package top.dteam.dfx.utils;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dteam.dfx.plugin.Accessible;

import java.util.Map;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void fireSingleMessageResponse(HttpServerResponse response, int statusCode) {
        response.setStatusCode(statusCode).end();
    }

    public static void fireSingleMessageResponse(HttpServerResponse response, int statusCode, String message) {
        response.setStatusCode(statusCode).end(message);
    }

    public static void fireJsonResponse(HttpServerResponse response, int statusCode, Map payload) {
        response.setStatusCode(statusCode);
        JsonObject jsonObject = new JsonObject(payload);
        response.putHeader("content-type", "application/json; charset=utf-8").end(jsonObject.toString());
    }

    public static void withCircuitBreaker(Vertx vertx, CircuitBreaker circuitBreaker
            , Accessible accessible, Map params, Handler<Map> successHandler, Handler<Throwable> failureHandler) {
        circuitBreaker.execute(future ->
                vertx.executeBlocking(f -> f.complete(accessible.invoke(params))
                        , result -> future.complete(result.result()))
        ).setHandler(result -> {
            if (result.succeeded()) {
                successHandler.handle((Map) result.result());
            } else {
                logger.error("CB[{}] execution failed, cause: ", circuitBreaker.name(), result.cause());
                failureHandler.handle(result.cause());
            }
        });
    }
}
