package top.dteam.dfx;

import io.vertx.core.AbstractVerticle;
import top.dteam.dfx.monitor.CircuitBreakerMonitor;

public class MainVerticle extends AbstractVerticle {

    public static String conf;
    public static String pluginDir;

    @Override
    public void start() {
        conf = System.getProperty("conf");
        pluginDir = System.getProperty("pf4j.pluginsDir", "plugins");

        vertx.deployVerticle(new CircuitBreakerMonitor());
        PluginManagerVerticle.start(vertx);
    }

}
