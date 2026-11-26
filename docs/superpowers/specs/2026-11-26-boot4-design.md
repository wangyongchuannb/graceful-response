# Spring Boot 4.1.3 专用版本设计

## 目标与范围

基于 feiniaojin/graceful-response 维护公开 Fork，使 Spring Boot 4.1.3 的 Servlet / Spring MVC 应用能够继续使用统一响应、异常处理、校验增强和排除规则。保留原 Java 包名、公开注解与配置项，保留 MIT LICENSE 和原作者署名。

用户已选择公开 Fork、维护 Boot 4 专用版本。仓库归属 wangyongchuannb。实际业务项目的配置和第三方依赖尚未提供，仓库测试通过不等于业务项目已验收。

## 方案比较

- Boot 4 专用版本（用户已选择）：使用 Boot 4.1.3 管理依赖并适配 Jackson 3、Spring Framework 7，迁移路径清楚，避免携带 Boot 3 依赖。
- 单一制品同时兼容 Boot 3/4：需要可选依赖、运行时探测与双版本测试，维护成本更高，本次不采用。

## 实现约束与工程决策

- 以 Spring Boot 4.1.3 BOM 管理依赖，使用对应 Web MVC starter，Java 编译基线为 17。
- 更新 JSON 转换器识别，适配 Boot 4 默认 Jackson 3；不为保留旧转换器而强制引入 Jackson 2。
- 检查异常放行解析器与 MVC 扩展点，优先复用框架和应用的消息转换器，避免单独创建的默认转换器丢失用户序列化配置。
- 保持响应结构和配置语义。发现与升级直接相关的兼容性问题时一并修复，用回归用例说明。
- 不承诺未测试的 WebFlux、原生镜像或所有第三方组件兼容性。

## 验证

开启默认测试。使用 Boot 4.1.3 的真实应用上下文和 MVC 请求验证：对象响应、空响应、既有 Response、防止重复封装、全局异常、参数校验、注解及 URL 排除、异常放行与自定义序列化。覆盖可实际验证的原有配置特性，补充持续集成。

补充验证 ResponseEntity 的 HTTP 状态码与响应头、非 JSON 响应，避免迁移消息转换器影响原语义。

以编译、测试和打包的实际结果为发布依据。若依赖版本无法从仓库解析，不用其他版本的测试冒充 4.1.3 验证。

## 发布与使用

创建或复用 wangyongchuannb/graceful-response 公开 Fork，保留上游历史。使用 boot4 维护分支，并将其设为 Fork 默认分支。拟用独立 Maven 坐标 com.github.wangyongchuannb:graceful-response，初始版本 6.0.0-boot4；具体坐标和版本是实施者建议。

更新 README，明确 Fork 身份、支持版本、构建方法、依赖接入和测试边界。发布 GitHub Release，附普通 JAR、源码 JAR、发布 POM 和校验和。提供本地 Maven 安装方式，指定 -DpomFile 保留传递依赖。不沿用上游 release profile 的 Sonatype/GPG 发布路径，改为独立源码 JAR 构建和 GitHub 附件流程。

本次 GitHub 发布不等于 Maven Central 发布；不使用原作者 Maven 命名空间发布。若文档提供 JitPack 路径，应清楚标注其构建状态，不能声称未验证的远程制品可用。

## 完成标准

适配代码、自动测试和文档已提交；本地 Boot 4.1.3 验证通过；公开 Fork 可访问，发布标签与 Release 对应已测试的提交，附件可下载。明确告知仍需在用户业务项目执行接入验收。
