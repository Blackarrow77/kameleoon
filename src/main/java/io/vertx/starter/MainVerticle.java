package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.regex.Matcher;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    Route app = router.route(HttpMethod.POST, "/request");
    app.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.end("Test");
    });

    Route home = router.route("/");
    home.handler(routingContext -> {
      // This handler will be called for every request
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/html");
      // Write to the response and end it
      response.sendFile("src/main/html/index.html");
    });

    Route app = router.route("/app");
    app.handler(routingContext -> {
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
