package top.dteam.dfx;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dteam.dfx.monitor.CircuitBreakerMonitor;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
        vertx.deployVerticle(new CircuitBreakerMonitor());
        PluginManagerVerticle.start(vertx);
    }

}
