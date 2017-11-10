package top.dteam.dfx

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import spock.lang.Specification
import spock.lang.Stepwise

import java.nio.file.Files
import java.nio.file.Paths

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Stepwise
class IntegrationSpec extends Specification {

    static Vertx vertx
    static HttpClient httpClient

    void setupSpec() {
        vertx = Vertx.vertx()
        httpClient = vertx.createHttpClient()
        createTestEnv()
        kickOffPluginManagerVerticle()
    }

    void cleanupSpec() {
        new File('build/tmp/test/plugins').deleteDir()
        vertx.close()
    }

    def "should load plugins and accept request"() {
        setup:
        Map result1 = [:]
        Map result2 = [:]

        when:
        post(7000, '/method1', [plugin1: 'plugin1'], result1)
        post(7000, '/method2', [plugin2: 'plugin2'], result2)
        sleep(2000)

        then:
        result1 == [statusCode: 200, plugin1: 'plugin1']
        result2 == [statusCode: 200, plugin2: 'plugin2']
    }

    def "should reload when a plugin is delete"() {
        setup:
        Map result1 = [:]
        Map result2 = [:]
        Map result3 = [:]
        Map result4 = [:]

        when:
        Files.delete(Paths.get('build/tmp/test/plugins/dfx-plugin2-0.0.1.zip'))
        new File('build/tmp/test/plugins/dfx-plugin2-0.0.1').deleteDir()
        Files.copy(Paths.get('src/test/resources/anotherConf')
                , Paths.get('build/tmp/test/plugins/conf'), REPLACE_EXISTING)
        sleep 10000
        post(7000, '/method1', [plugin1: 'plugin1'], result1)
        post(7000, '/method2', [plugin2: 'plugin2'], result2)
        post(7001, '/method1', [plugin1: 'plugin1'], result3)
        post(7001, '/method2', [plugin2: 'plugin2'], result4)
        sleep(2000)

        then:
        !result1
        !result2
        result3 == [statusCode: 200, plugin1: 'plugin1']
        result4.statusCode == 404
    }

    def "should reload when a plugin is added"() {
        setup:
        Map result = [:]

        when:
        Files.copy(Paths.get('src/test/resources/conf')
                , Paths.get('build/tmp/test/plugins/conf'), REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/resources/dfx-plugin2-0.0.1.zip')
                , Paths.get('build/tmp/test/plugins/dfx-plugin2-0.0.1.zip'), REPLACE_EXISTING)
        sleep 10000
        post(7000, '/method2', [plugin2: 'plugin2'], result)
        sleep(2000)

        then:
        result == [statusCode: 200, plugin2: 'plugin2']
    }

    private void createTestEnv() {
        new File('build/tmp/test/plugins').mkdirs()
        Files.copy(Paths.get('src/test/resources/conf')
                , Paths.get('build/tmp/test/plugins/conf'), REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/resources/dfx-plugin1-0.0.1.zip')
                , Paths.get('build/tmp/test/plugins/dfx-plugin1-0.0.1.zip'), REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/resources/dfx-plugin2-0.0.1.zip')
                , Paths.get('build/tmp/test/plugins/dfx-plugin2-0.0.1.zip'), REPLACE_EXISTING)
    }

    private void kickOffPluginManagerVerticle() {
        MainVerticle.conf = 'build/tmp/test/plugins/conf'
        MainVerticle.pluginDir = 'build/tmp/test/plugins'
        PluginManagerVerticle.start(vertx)
    }

    private Handler<HttpClientResponse> handler(Map result) {
        { response ->
            result.statusCode = response.statusCode()
            response.bodyHandler { totalBuffer ->
                if (response.statusCode() == 200 && totalBuffer.length() > 0) {
                    result.putAll(totalBuffer.toJsonObject().map)
                }
            }
        }
    }

    private void post(int port, String uri, Map data, Map result) {
        httpClient.post(port, 'localhost', uri, handler(result))
                .setChunked(true)
                .putHeader("content-type", "application/json")
                .end(new JsonObject(data).toString())
    }

}
