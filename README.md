# dfx：Distribution F(x)

dfx 是一个基于插件架构（使用[pf4j](https://github.com/decebals/pf4j)）的远程服务容器，它可以快速地帮助开发者基于现有的 Java 类创建远程 restful 服务。

## 使用

编译 dfx 主程序:

```sh
./gradlew :dfx:shadowjar
```

编译好的程序存放于`dfx/build/libs/`

它的运行命令如下：

```sh
java -jar dfx-0.1-fat.jar -Dconf=conf -Dpf4j.pluginsDir=build/libs/plugins
```

其中：

- conf 中定义了插件和暴露的 url 的映射
- pf4j.pluginsDir 是 pf4j 的需要用到的参数，指明插件所在的目录。若不指明，插件目录则为当前目录下的**plugins**子目录。

## 配置文件

配置文件的语法为 Groovy，下面是 conf 的一个示例：

```groovy
import io.vertx.core.http.HttpMethod

port = 7000
host = "127.0.0.1"

watchCycle = 5001

circuitBreaker {
    maxFailures = 5
    timeout = 10000
    resetTimeout = 30000
}

cors {
    allowedOriginPattern = '*'
    allowedMethods = [HttpMethod.POST]
    allowedHeaders = ['content-type']
}

mappings {
    "/method1" {
        plugin = "top.dteam.dfx.plugin.implment.Plugin1"
    }

    "/method2" {
        plugin = "top.dteam.dfx.plugin.implment.Plugin2"
    }
}
```

文件本身的内容已经说明白了配置的语法，这里不再赘述。开发者需要注意的是，plugin 所对应的是插件的全限定名。

`port`、`host`、`watchCycle`、`circuitBreaker` 的缺省值如下：

- port：8080
- host："0.0.0.0"
- watchCycle：5000 ms
- circuitBreakerOptions：
  - maxFailures：3 次
  - timeout：5000 ms
  - resetTimeout：10000 ms

如果要让 Web 前端能正常访问 dfx 中部署的服务，需要打开 `cors`。同时，这里请留意引入“import io.vertx.core.http.HttpMethod”。

CORS 的配置项如下：

- allowedOriginPattern
- allowedHeaders
- allowedMethods
- exposedHeaders
- maxAgeSeconds
- allowCredentials

同时请注意：当 `allowedOriginPattern` 为“\*”时，`allowCredentials` 不允许为“true”。

## 插件开发

dfx 的理念是：每个插件对应一个 restful url。通过实现对应的接口（[Accessible](dfx/src/main/java/top/dteam/dfx/plugin/Accessible.java)），完成 restful service 的开发。

为了保持简单和通用性，Accessible 接口如下：

```groovy
public interface Accessible extends ExtensionPoint{
    Map invoke(Map parameters);
}
```

其中：

- ExtensionPoint 来自 pf4j，请参见其文档。
- 每个远程方法的入参和返回参数均为 Map，可视为 JSON 的对等物。

插件工程的例子和模板可以参见[这里](dfx-plugin1)，其依赖[dfx-i](dfx-i)，它仅包含 `Accessible` 接口。

如果使用独立的 gradle 工程开发插件，需要删除模板工程的`compileOnly project(':dfx-i')`这一行，将`dfx-i-{ver}.jar`放在工程的`lib/`目录下进行编译。另外可以使用`gradle wrapper`命令生成 gradle wrapper，以便固定 gradle 版本号。

插件开发完之后，运行下面的 gradle 命令来将其打包：

```sh
./gradlew plugin
```

要编译本项目的 demo 工程，需要使用 gradle 子项目 task 编译:

```sh
# 在git项目根目录运行
./gradlew :dfx-plugin1:plugin  # 编译dfx-plugin1
./gradlew :dfx-plugin2:plugin  # 编译dfx-plugin2
```

编译好的 zip 包存放于`build/distributions/`目录下。

这样就得到了一个 plugin 压缩包，在打包前请检查插件工程的 build.gradle 中 manifest 部分：

```groovy
classes.doLast {
    jar.manifest {
        attributes("Manifest-Version": 1.0,
                "Archiver-Version": "Plexus Archiver",
                "Created-By": "Gradle",
                "Built-By": "Hu Jian",
                "Build-Jdk": JavaVersion.current(),
                "Plugin-Class": "top.dteam.dfx.plugin.implment.Plugin1",
                "Plugin-Id": "dfx-plugin1",
                "Plugin-Provider": "Hu Jian",
                "Plugin-Version": version)
        writeTo("$buildDir/classes/main/META-INF/MANIFEST.MF")
    }
}
```

其中的**Plugin-Class**和**Plugin-Id**请留意不要与其他插件发生冲突。

## 插件部署

插件开发完毕之后，将其复制到相应的插件目录之下就完成了部署。启动 dfx 之后，应该能出现以下字样：

```txt
2017-11-01 07:04:59,382 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - top.dteam.dfx.plugin.implment.Plugin2 is loaded ...
2017-11-01 07:04:59,382 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - top.dteam.dfx.plugin.implment.Plugin1 is loaded ...
2017-11-01 07:05:00,130 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - dfx is listening at 0.0.0.0:7000 ...
```

此时，通过向暴露出的 url 发送请求即可：

```sh
curl -d '{"name":"name1"}' http://localhost:7000/method1
```

请注意，发送 json 作为请求体。

## 热更新

dfx 支持热更新，它会监视 conf 文件和 plugins 目录的变化，监控周期由配置文件里的 watchCycle 指定。目前的 reload 方式简单粗暴：当发现任意一个变化时，会重新加载整个服务，即相当于重启。

更新 plugin 时请按照以下步骤进行：

- 更新 conf 文件配置（如有必要）
- 删除 pluygins 目录下对应 plugin 的目录和 zip 文件
- 复制新的 plugin zip 文件到 plugins 目录

提示：建议讲上述过程脚本化，避免更新过程中反复重启。

## 开发指南

- git clone
- gradle shadowJar，生成 dfx 的 fatjar
- gradle test，运行测试代码

在发起 Pull Request 时，请同时提交测试代码，并保证现有测试代码【对于测试，我们推荐[Spock](http://spockframework.org/)】能全部通过)。
