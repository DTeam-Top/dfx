package top.dteam.dfx.utils

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import spock.lang.Specification
import top.dteam.dfx.plugin.Accessible

class UtilsSpec extends Specification {

    Vertx vertx
    CircuitBreaker circuitBreaker

    void setup() {
        vertx = Vertx.vertx()
        circuitBreaker = CircuitBreaker.create("circuitbreaker1", vertx, new CircuitBreakerOptions()
                .setMaxFailures(2)
                .setTimeout(500)
                .setResetTimeout(500))
    }

    void cleanup() {
        vertx.close()
    }

    def "withCircuitBreaker should work for a normal invocation."() {
        setup:
        String result

        when:
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                [result: 'result']
            }
        }, [:], { map -> result = map.result }, null)

        sleep 100

        then:
        result == 'result'
    }

    def "withCircuitBreaker should work for an exceptional invocation."() {
        setup:
        String result

        when:
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                throw new Exception('exception')
            }
        }, [:], null, { throwable -> result = throwable.message })

        sleep 100

        then:
        result == 'exception'
    }

    def "withCircuitBreaker should work: timeout n times --> open --> close after a while."() {
        setup:
        String result

        when:
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                sleep 550
                throw new Exception('exception')
            }
        }, [:], null, { throwable -> result = throwable.message })
        sleep 600

        then:
        result == 'operation timeout'

        when:
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                sleep 550
                [result: 'result']
            }
        }, [:], null, { throwable -> result = throwable.message })
        sleep 600

        then:
        result == 'operation timeout'

        when:
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                [result: 'result']
            }
        }, [:], null, { throwable -> result = throwable.message })
        sleep 100

        then:
        result == 'open circuit'

        when:
        sleep 600
        Utils.withCircuitBreaker(vertx, circuitBreaker, new Accessible() {
            @Override
            Map invoke(Map parameters) {
                [result: 'result']
            }
        }, [:], { map -> result = map.result }, null)
        sleep 100

        then:
        result == 'result'
    }

}
