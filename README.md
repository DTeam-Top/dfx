# dfx：Distribution F(x)

dfx是一个基于插件架构（使用[pf4j](https://github.com/decebals/pf4j)）的远程服务容器，它可以快速地帮助开发者基于现有的Java类创建远程restful服务。它的运行命令如下：

~~~
java -jar dfx-0.1-fat.jar -Dconf=conf -Dpf4j.pluginsDir=build/libs/plugins
~~~

其中：
- conf中定义了插件和暴露的url的映射
- pf4j.pluginsDir是pf4j的需要用到的参数，指明插件所在的目录。若不指明，插件目录则为当前目录下的**plugins**子目录。

## 配置文件

下面是conf的一个示例：
~~~
port = 7000

"/method1" {
    plugin = "top.dteam.dfx.plugin.implment.Plugin1"
}

"/method2" {
    plugin = "top.dteam.dfx.plugin.implment.Plugin2"
}
~~~

文件本身的内容已经说明白了配置的语法，这里不再赘述。开发者需要注意的是，plugin所对应的是插件的全限定名。

## 插件开发

dfx的理念是：每个插件对应一个restful url。通过实现对应的接口（[Accessible](dfx/src/main/java/top/dteam/dfx/plugin/Accessible.java)），完成restful service的开发。

为了保持简单和通用性，Accessible接口如下：

~~~
public interface Accessible extends ExtensionPoint{
    Map invoke(Map parameters);
}
~~~

其中：
- ExtensionPoint来自pf4j，请参见其文档。
- 每个远程方法的入参和返回参数均为Map，可视为JSON的对等物。

插件工程的例子和模板可以参见[这里](dfx-plugin1)，其依赖[dfx-i](dfx-i)，它仅包含Accessible接口。

插件开发完之后，运行下面的gradle命令来将其打包：

~~~
gradle plugin
~~~

这样就得到了一个plugin压缩包，在打包前请检查插件工程的build.gradle中manifest部分：

~~~
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
~~~

其中的**Plugin-Class**和**Plugin-Id**请留意不要与其他插件发生冲突。

## 插件部署

插件开发完毕之后，将其复制到相应的插件目录之下就完成了部署。启动dfx之后，应该能出现以下字样：

~~~
2017-11-01 07:04:59,382 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - top.dteam.dfx.plugin.implment.Plugin2 is loaded ...
2017-11-01 07:04:59,382 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - top.dteam.dfx.plugin.implment.Plugin1 is loaded ...
2017-11-01 07:05:00,130 [vert.x-eventloop-thread-0] INFO  top.dteam.dfx.MainVerticle - dfx is listening at 0.0.0.0:7000 ...
~~~

此时，通过向暴露出的url发送请求即可：

~~~
curl -d '{"name":"name1"}' http://localhost:7000/method1
~~~

请注意，发送json作为请求体。

## 开发指南

- git clone
- gradle shadowJar，生成dfx的fatjar
- gradle test，运行测试代码

在发起Pull Request时，请同时提交测试代码，并保证现有测试代码【对于测试，我们推荐[Spock](http://spockframework.org/)】能全部通过，;)。
