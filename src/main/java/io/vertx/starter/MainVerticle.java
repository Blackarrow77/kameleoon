package io.vertx.starter;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() throws IOException {

    FileInputStream serviceAccount = new FileInputStream("kameleoon-firebase-adminsdk-4zr2m-f2951b9096.json");
    FirebaseOptions options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://kameleoon.firebaseio.com")
      .build();
    FirebaseApp.initializeApp(options);

    Firestore db = FirestoreClient.getFirestore();


    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    Route app = router.route(HttpMethod.POST, "/request");
    app.handler(routingContext -> {
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
