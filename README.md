# Graceful Response for Spring Boot 4

基于 [feiniaojin/graceful-response](https://github.com/feiniaojin/graceful-response) 的公开 Fork，提供 Spring Boot **4.1.3** 专用版本，保留原 Java 包名、注解和配置项。原作者为 **feiniaojin**，本分支由 **wangyongchuannb** 维护，使用 [MIT License](LICENSE)。

[![Boot 4 CI](https://github.com/wangyongchuannb/graceful-response/actions/workflows/boot4-ci.yml/badge.svg?branch=boot4)](https://github.com/wangyongchuannb/graceful-response/actions/workflows/boot4-ci.yml)

版本：`6.0.0-boot4`。支持 Servlet / Spring MVC；以 Spring Boot `4.1.3` 的 BOM 管理依赖，使用 Jackson 3。Java 17 起，推荐 JDK 21。

## 本次适配

- 使用 Boot 4 的 `spring-boot-starter-webmvc`，移除 Boot 3、旧 Jakarta 和 SLF4J 的独立版本锁定。
- 识别 Jackson 3 JSON 消息转换器，恢复正常对象、空值和 void 的统一响应封装。
- 异常放行使用应用的 MVC 消息转换器，保留自定义 JSON 配置、ResponseEntity 状态和响应头、文本响应。
- 异常处理复用 Spring MVC 7 的解析器；外部 advice 按顺序查找，并使用 Spring 选择同一 advice 中更具体的异常处理方法。
- 支持 MVC 方法参数校验产生的 `HandlerMethodValidationException`，应用参数校验错误码和 HTTP 状态配置。返回值约束失败返回 HTTP 500 和通用错误消息。

这是 Boot 4 专用版本。Boot 3 用户请使用[上游项目](https://github.com/feiniaojin/graceful-response)。上游说明保存在 [README.upstream.md](README.upstream.md)。

## 获取与安装

[GitHub Releases](https://github.com/wangyongchuannb/graceful-response/releases) 提供普通 JAR、源码 JAR、POM 和 SHA-256 校验和。**本版本未发布到 Maven Central**；仅添加下面的依赖坐标不会自动从中央仓库下载。

方式一：克隆并安装到本地 Maven 仓库（推荐）：

```shell
git clone --branch boot4 https://github.com/wangyongchuannb/graceful-response.git
cd graceful-response
mvn clean install
```

方式二：从 Release 下载 JAR 与 POM，放在同一目录，校验后安装：

```shell
mvn install:install-file -Dfile=graceful-response-6.0.0-boot4.jar -DpomFile=graceful-response-6.0.0-boot4.pom
```

务必同时使用 POM，以保留 Web MVC、Validation 等传递依赖。团队使用时可将 JAR 和 POM 上传到自己的 Maven 私服。

完成安装后，在 **Spring Boot 4.1.3** 项目中添加：

```xml
<dependency>
    <groupId>com.github.wangyongchuannb</groupId>
    <artifactId>graceful-response</artifactId>
    <version>6.0.0-boot4</version>
</dependency>
```

移除原来的 `com.feiniaojin:graceful-response` 依赖，避免相同 Java 类重复出现在类路径中。

## 启用

```java
import com.feiniaojin.gracefulresponse.EnableGracefulResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableGracefulResponse
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

控制器直接返回业务对象即可，默认响应风格保持为 `{code, msg, data}`：

```json
{"code":"0","msg":"ok","data":{"name":"boot4"}}
```

配置项仍使用 `graceful-response` 前缀，例如：

```yaml
graceful-response:
  response-style: 1
  default-success-code: "0"
  default-success-msg: ok
  default-error-code: "1"
  default-error-msg: error
  default-validate-error-code: "VALIDATION"
  default-http-status-code-on-validation-error: 400
  exclude-urls:
    - /actuator/**
    - /v3/api-docs/**
```

`response-style: 0` 可使用 `{status: {code, msg}, payload}`。`@ExcludeFromGracefulResponse`、`@ExceptionMapper`、`@ValidationStatusCode` 和 `ResponseFactory` 的包名保持不变。

## 构建与验证

```shell
mvn clean verify
```

默认执行测试；没有测试时构建失败。CI 在 JDK 17、21 上运行相同命令。测试覆盖 Boot 4.1.3 应用上下文、MockMvc 请求和嵌入式 Tomcat 的真实 HTTP 请求，包括响应封装、排除规则、错误码、参数校验、响应风格、自定义 JSON 命名策略和异常放行。

未验证 WebFlux、GraalVM 原生镜像，以及所有 Swagger、springdoc、Fastjson、Actuator 版本组合。本仓库验证通过后，仍需在实际业务项目中做接入验收。

发布步骤见 [docs/RELEASE.md](docs/RELEASE.md)。
