package io.vertx.starter;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.server.UID;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() throws IOException {

    FileInputStream serviceAccount = new FileInputStream("kameleoon-firebase-adminsdk-4zr2m-99cd7aaa10.json");
    FirebaseOptions options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://kameleoon.firebaseio.com")
      .build();
    FirebaseApp.initializeApp(options);

    Firestore db = FirestoreClient.getFirestore();


    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    Route vote = router.route(HttpMethod.POST, "/vote");
    vote.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      //Get values
      String idToken = routingContext.getBodyAsJson().getString("idToken");
      String id = routingContext.getBodyAsJson().getString("id");
      String v = routingContext.getBodyAsJson().getString("vote");
      String uid = null;
      try {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        uid  = decodedToken.getUid();
      } catch (FirebaseAuthException e) {
        e.printStackTrace();
      }
      int score = 9;
      DocumentReference docRef = db.collection(uid).document(id);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      try {
        DocumentSnapshot document = future.get();
        Map<String, Object> docData = document.getData();
        score = (int) docData.get("score");
        if(v.equals("up")){
          score++;
        }else{
          if(score>0){
            score--;
            response.end("false");
          }
        }
        score = 2;
        System.out.println(score);
        ApiFuture<WriteResult> futureWrite = docRef.update("score", score);
        WriteResult result = futureWrite.get();
        System.out.println("Write result: " + result);
      } catch (InterruptedException e) {
        e.printStackTrace();
        response.end("false");
      } catch (ExecutionException e) {
        e.printStackTrace();
        response.end("false");
      } finally {
        response.end(String.valueOf(score));
      }
  });


    Route list = router.route(HttpMethod.POST, "/list");
    list.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      //Get values
      String idToken = routingContext.getBodyAsJson().getString("idToken");
      try {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid   = decodedToken.getUid();
        ApiFuture<QuerySnapshot> future = db.collection(uid).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        ArrayList<Map<String, Object>> quoteList = new ArrayList<Map<String, Object>>();
        for (QueryDocumentSnapshot document : documents) {
          quoteList.add(document.getData());
        }
        response.end(Json.encode(quoteList));
      } catch (FirebaseAuthException e) {
        e.printStackTrace();
        response.end("false");
      } catch (InterruptedException e) {
        e.printStackTrace();
        response.end("false");
      } catch (ExecutionException e) {
        e.printStackTrace();
        response.end("false");
      }


      response.end("true");
    });


    Route add = router.route(HttpMethod.POST, "/add");
    add.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      //Get values
      String idToken = routingContext.getBodyAsJson().getString("idToken");
      String title = routingContext.getBodyAsJson().getString("title");
      String uid = null;
      String name = null;
      String id = UUID.randomUUID().toString();
      try {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        uid  = decodedToken.getUid();
        name = FirebaseAuth.getInstance().getUser(uid).getDisplayName();
      } catch (FirebaseAuthException e) {
        e.printStackTrace();
      }
      DocumentReference docRef = db.collection(uid).document(id);
      Map<String, Object> data = new HashMap<>();
      data.put("title", title);
      data.put("id", id);
      data.put("author", name);
      data.put("score", 0);
      //asynchronously write data
      ApiFuture<WriteResult> result = docRef.set(data);
      try {
        System.out.println("Update time : " + result.get().getUpdateTime());
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
      response.end("true");
    });

    Route home = router.route("/");
    home.handler(routingContext -> {
      // This handler will be called for every request
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");
      // Write to the response and end it
      response.sendFile("src/main/html/index.html");
    });

    Route application = router.route("/app");
    application.handler(routingContext -> {
      // This handler will be called for every request
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");
      // Write to the response and end it
      response.sendFile("src/main/html/app.html");
    });

    Route css = router.route("/css/bootstrap.css");
    css.handler(routingContext -> {
      // This handler will be called for every request
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/css");
      // Write to the response and end it
      response.sendFile("src/main/html/css/bootstrap.css");
    });

    server.requestHandler(router::accept).listen(80);


  }

}
