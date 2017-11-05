package top.dteam.dfx;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

public class WatcherVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(WatcherVerticle.class);

    private int watchCycle;

    private WatchService watchService;
    private WatchKey key;

    public WatcherVerticle(int watchCycle) throws IOException {
        this.watchCycle = watchCycle;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    @Override
    public void start() {
        try {
            kickOffWatchService();
        } catch (IOException e) {
            logger.error("Something wrong happened during start: {}", e);
        }
    }

    @Override
    public void stop() {
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error("Something wrong happened during stopping: {}", e);
        }
    }

    private void kickOffWatchService() throws IOException {
        Path path = FileSystems.getDefault().getPath(System.getProperty("pf4j.pluginsDir", "plugins")).toAbsolutePath();
        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        Path conf = FileSystems.getDefault().getPath(System.getProperty("conf")).toAbsolutePath();
        conf.getParent().register(watchService, ENTRY_MODIFY);

        vertx.setPeriodic(watchCycle, tid -> {
            try {
                key = watchService.poll(100, TimeUnit.MILLISECONDS);

                if (key == null) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path file = ev.context();

                    if (file.toString().endsWith(".zip") || file.endsWith(System.getProperty("conf"))) {
                        if (kind == ENTRY_CREATE) {
                            logger.info("An entry was created: {}", file.getFileName());
                        } else if (kind == ENTRY_DELETE) {
                            logger.info("An entry was deleted: {}", file.getFileName());
                        } else {
                            logger.info("An entry was modified: {}", file.getFileName());
                        }

                        // consumer will undeploy this verticle
//                        vertx.eventBus().send(PluginManagerVerticle.PLUGINS_CHANGED, this.deploymentID());
                    }
                }

                key.reset();
            } catch (InterruptedException e) {
                return;
            }
        });
    }
}
