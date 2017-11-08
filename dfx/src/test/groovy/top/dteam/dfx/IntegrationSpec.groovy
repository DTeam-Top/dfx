package top.dteam.dfx

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import spock.lang.Specification
import spock.lang.Stepwise

import static java.nio.file.StandardCopyOption.*
import java.nio.file.Files
import java.nio.file.Paths

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
        vertx.close()
    }

    def "should load plugins and accept request"() {
        setup:
        Map result1 = [:]
        Map result2 = [:]

        when:
        httpClient.post(7000, 'localhost', '/method1', handler(result1))
                .setChunked(true)
                .putHeader("content-type", "application/json")
                .end(new JsonObject([plugin1: 'plugin1']).toString())

        httpClient.post(7000, 'localhost', '/method2', handler(result2))
                .setChunked(true)
                .putHeader("content-type", "application/json")
                .end(new JsonObject([plugin2: 'plugin2']).toString())

        sleep(2000)

        then:
        result1 == [plugin1: 'plugin1']
        result2 == [plugin2: 'plugin2']
    }

    private void createTestEnv() {
        File dir = new File('build/tmp/test/plugins')
        dir.mkdirs()

        Files.copy(Paths.get('src/test/resources/conf')
                , Paths.get('build/tmp/test/plugins/conf'), REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/resources/dfx-plugin1-0.0.1.zip')
                , Paths.get('build/tmp/test/plugins/dfx-plugin1-0.0.1.zip'), REPLACE_EXISTING)
        Files.copy(Paths.get('src/test/resources/dfx-plugin2-0.0.1.zip')
                , Paths.get('build/tmp/test/plugins/dfx-plugin2-0.0.1.zip'), REPLACE_EXISTING)
        dir.deleteOnExit()
    }

    private void kickOffPluginManagerVerticle() {
        MainVerticle.conf = 'build/tmp/test/plugins/conf'
        MainVerticle.pluginDir = 'build/tmp/test/plugins'
        PluginManagerVerticle.start(vertx)
    }

    private Handler<HttpClientResponse> handler(Map result) {
        { response ->
            response.bodyHandler { totalBuffer ->
                if (totalBuffer.length() > 0) {
                    result.putAll(totalBuffer.toJsonObject().map)
                }
            }
        }
    }

}
