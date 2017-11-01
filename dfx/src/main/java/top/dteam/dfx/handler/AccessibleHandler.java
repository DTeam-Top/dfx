package top.dteam.dfx.handler;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dteam.dfx.plugin.Accessible;
import top.dteam.dfx.utils.Utils;

import java.util.Map;

public class AccessibleHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(AccessibleHandler.class);

    private String url;
    private Accessible accessible;

    public AccessibleHandler(String url, Accessible accessible) {
        this.url = url;
        this.accessible = accessible;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (routingContext.request().isEnded()) {
            processRequest(routingContext, routingContext.getBody());
        } else {
            routingContext.request().bodyHandler(totalBuffer -> processRequest(routingContext, totalBuffer));
        }
    }

    private void processRequest(RoutingContext routingContext, Buffer buffer) {
        JsonObject body = getBodyFromBuffer(buffer);
        mergeRequestParams(body, routingContext.request().params());
        processRequestBody(routingContext.response(), body);
    }

    private JsonObject getBodyFromBuffer(Buffer buffer) {
        logger.debug("Body is {}", buffer);

        if (buffer.toString().trim().length() == 0) {
            return new JsonObject();
        } else {
            return buffer.toJsonObject();
        }
    }

    private void mergeRequestParams(JsonObject body, MultiMap params) {
        if (params == null) {
            return;
        }

        params.forEach(entry -> body.put(entry.getKey(), entry.getValue()));

        logger.debug("Merged paramaters {}: ", body);
    }

    private void processRequestBody(HttpServerResponse response, JsonObject body) {
        try {
            Map result = accessible.invoke(body.getMap());
            Utils.fireJsonResponse(response, 200, result);
        } catch (NullPointerException ne) {
            logger.error("Could not find an Accessible for url: {}", url);
            Utils.fireSingleMessageResponse(response, 500, "Could not find an Accessible!");
        } catch (Exception e) {
            logger.error("Caught an exception: {}", e);
            Utils.fireSingleMessageResponse(response, 500, e.getMessage());
        }
    }
}
