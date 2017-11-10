package top.dteam.dfx.config

import groovy.transform.ToString
import io.vertx.circuitbreaker.CircuitBreakerOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ToString
class DfxConfig {

    private static final Logger logger = LoggerFactory.getLogger(DfxConfig.class);

    int port
    String host = '0.0.0.0'
    Map<String, String> mappings

    int watchCycle

    CircuitBreakerOptions circuitBreakerOptions

    static DfxConfig load(String file) {
        if (!file) {
            throw new IllegalArgumentException("Please set config file first!")
        }

        File f = new File(file)
        if (f.isFile()) {
            build(f.text)
        } else {
            throw new FileNotFoundException("No such file or directory: ${file}")
        }
    }

    static DfxConfig build(String config) {
        ConfigSlurper slurper = new ConfigSlurper()
        ConfigObject configObject = slurper.parse(config)

        DfxConfig dfxConfig = new DfxConfig()

        try {
            dfxConfig.port = configObject.port ?: 8080
            dfxConfig.host = configObject.host ?: '0.0.0.0'
            dfxConfig.watchCycle = configObject.watchCycle ?: 5000
            dfxConfig.mappings = [:]
            dfxConfig.circuitBreakerOptions = new CircuitBreakerOptions()
                    .setMaxFailures(configObject.circuitBreaker.maxFailures ?: 3)
                    .setTimeout(configObject.circuitBreaker.timeout ?: 5000)
                    .setResetTimeout(configObject.circuitBreaker.resetTimeout ?: 10000)
            (configObject.keySet() - ['port', 'host', 'watchCycle', 'circuitBreaker']).each {
                dfxConfig.mappings[it] = configObject[it].plugin
            }
        } catch (Exception e) {
            throw new InvalidConfiguriationException(e.message)
        }

        if (!dfxConfig.mappings) {
            throw new InvalidConfiguriationException('No url mappings.')
        }

        logger.debug("dfx Configuration: {}", dfxConfig.toString())

        dfxConfig
    }

}
