package top.dteam.dfx.config

import spock.lang.Specification
import spock.lang.Unroll

class DfxConfigSpec extends Specification {

    def "Configuration should be parsed correctly."() {
        setup:
        String conf = """
                port = 7000
                host = "127.0.0.1"
                
                watchCycle = 5001
                
                circuitBreaker {
                    maxFailures = 5
                    timeout = 10000
                    resetTimeout = 30000
                }
                
                "/method1" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin1"
                }
                
                "/method2" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin2"
                }
        """

        when:
        DfxConfig config = DfxConfig.build(conf)

        then:
        config.port == 7000
        config.host == "127.0.0.1"
        config.watchCycle == 5001
        config.circuitBreakerOptions.maxFailures == 5
        config.circuitBreakerOptions.timeout == 10000
        config.circuitBreakerOptions.resetTimeout == 30000
        config.mappings == ["/method1"  : "top.dteam.dfx.plugin.implment.Plugin1"
                            , "/method2": "top.dteam.dfx.plugin.implment.Plugin2"]
    }

    def "These properties have default values: host, port, watchCycle, circuitBreakerOptions."() {
        setup:
        String conf = """
                "/method1" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin1"
                }
                
                "/method2" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin2"
                }
        """

        when:
        DfxConfig config = DfxConfig.build(conf)

        then:
        config.port == 8080
        config.host == "0.0.0.0"
        config.watchCycle == 5000
        config.circuitBreakerOptions.maxFailures == 3
        config.circuitBreakerOptions.timeout == 5000
        config.circuitBreakerOptions.resetTimeout == 10000
    }

    @Unroll
    def "InvalidConfiguriationException should be thrown when Configuration is not correct."() {
        when:
        DfxConfig.build(conf)

        then:
        thrown(InvalidConfiguriationException)

        where:
        conf << ["""
                port = 'test'
                "/method1" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin1"
                }
        """, """
                watchCycle = '123e'
                "/method1" {
                    plugin = "top.dteam.dfx.plugin.implment.Plugin1"
                }
        ""","""
                port = 7000
                host = "127.0.0.1"
                watchCycle = 5001
        """]
    }

}
