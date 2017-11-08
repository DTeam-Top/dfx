package top.dteam.dfx.handler;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dteam.dfx.plugin.Accessible;
import top.dteam.dfx.utils.Utils;

public class AccessibleHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(AccessibleHandler.class);

    private Vertx vertx;
    private String url;
    private Accessible accessible;
    private CircuitBreaker circuitBreaker;

    public AccessibleHandler(String url, Accessible accessible, CircuitBreaker circuitBreaker, Vertx vertx) {
        this.vertx = vertx;
        this.url = url;
        this.accessible = accessible;
        this.circuitBreaker = circuitBreaker;
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
            logger.debug("RequestURL is {} , RequestParams are {}", this.url, body);
            Utils.withCircuitBreaker(vertx, circuitBreaker, accessible, body.getMap()
                    , result -> Utils.fireJsonResponse(response, 200, result)
                    , throwable -> Utils.fireSingleMessageResponse(response, 500, throwable.getMessage()));
        } catch (Exception e) {
            logger.error("Caught an exception: {}", e);
            Utils.fireSingleMessageResponse(response, 500, e.getMessage());
        }
    }
}
