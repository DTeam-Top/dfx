package top.dteam.dfx.utils;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

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

}
