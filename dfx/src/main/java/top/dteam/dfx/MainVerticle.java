package top.dteam.dfx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import top.dteam.dfx.config.DfxConfig;
import top.dteam.dfx.handler.AccessibleHandler;
import top.dteam.dfx.plugin.Accessible;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
//        vertx.deployVerticle(new CircuitBreakerMonitor());

        DfxConfig config = DfxConfig.load();
        Map<String, Accessible> accessibleMap = loadExtensions();
        Router router = buildRouter(config, accessibleMap);

        buildHttpServer(router, config.getPort(), config.getHost());
    }

    private static Map<String, Accessible> loadExtensions() {
        Map<String, Accessible> accessibleMap = new HashMap<>();

        PluginManager pluginManager = new DefaultPluginManager();
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        List<Accessible> accessibleList = pluginManager.getExtensions(Accessible.class);
        for (Accessible accessible : accessibleList) {
            String pluginName = accessible.getClass().getDeclaringClass().getName();

            logger.info("{} is loaded ...", pluginName);

            accessibleMap.put(pluginName, accessible);
        }

        return accessibleMap;
    }

    private Router buildRouter(DfxConfig config, Map<String, Accessible> accessibleMap) {
        Router router = Router.router(vertx);

        config.getMappings().forEach((key, value) -> {
            router.route(key).handler(new AccessibleHandler(key, accessibleMap.get(value)));
        });

        return router;
    }

    private void buildHttpServer(Router router, int port, String host) {
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router::accept).listen(port, host, result -> {
            if (result.succeeded()) {
                logger.info("dfx is listening at {}:{} ...", host, port);
            }
        });
    }
}
