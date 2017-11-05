package top.dteam.dfx;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import top.dteam.dfx.config.DfxConfig;
import top.dteam.dfx.handler.AccessibleHandler;
import top.dteam.dfx.plugin.Accessible;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginManagerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerVerticle.class);

    public static final String PLUGINS_CHANGED = "plugins.changed";

    private DfxConfig config;
    private PluginManager pluginManager;
    private Map<String, CircuitBreaker> circuitBreakerMap;

    public PluginManagerVerticle(DfxConfig config) {
        this.config = config;
        this.circuitBreakerMap = new HashMap<>();
    }

    @Override
    public void start() {
        Map<String, Accessible> accessibleMap = loadExtensions();
        Router router = buildRouter(config, accessibleMap);
        buildHttpServer(router, config.getPort(), config.getHost());

        vertx.eventBus().consumer(PLUGINS_CHANGED, message -> {
            logger.info("Something was changed, reload plugins ...");
            vertx.undeploy(this.deploymentID(), empty1 -> vertx.undeploy((String) message.body(),
                    empty2 -> PluginManagerVerticle.start(vertx)));
        });
    }

    @Override
    public void stop() {
        pluginManager.stopPlugins();
    }

    private Map<String, Accessible> loadExtensions() {
        Map<String, Accessible> accessibleMap = new HashMap<>();

        pluginManager = new DefaultPluginManager();
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
                    CircuitBreaker circuitBreaker = CircuitBreaker.create(key, vertx, new CircuitBreakerOptions()
                            .setMaxFailures(3)
                            .setTimeout(5000)
                            .setResetTimeout(10000));
                    if (accessibleMap.get(value) != null) {
                        router.route(key).handler(new AccessibleHandler(key, accessibleMap.get(value), circuitBreaker, vertx));
                    }
                }
        );

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

    public static void start(Vertx vertx) {
        DfxConfig config = DfxConfig.load();
        vertx.deployVerticle(new PluginManagerVerticle(config), result -> {
            try {
                vertx.deployVerticle(new WatcherVerticle(config.getWatchCycle()), new DeploymentOptions().setWorker(true));
            } catch (IOException e) {
                logger.error("An error happened during start process: {}", e);
                vertx.close();
                System.exit(-1);
            }
        });
    }

}
