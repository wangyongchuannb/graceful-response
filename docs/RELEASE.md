# GitHub Release 发布流程

本 Fork 发布至 GitHub，不使用上游的 Sonatype/GPG release profile，也不发布到原作者的 Maven 命名空间。

1. 确认当前是 `boot4`，更新版本和变更说明，执行 `mvn clean verify`。检查测试数量非零、失败和错误均为零。
2. 检查依赖树中没有意外的 Boot 3、Spring 6、Jackson 2 databind/core 依赖。Jackson 3 继续使用 `com.fasterxml.jackson.annotation` 注解是正常现象。
3. 提交并推送，等待 GitHub Actions 的 JDK 17、21 验证全部通过。
4. 为通过验证的提交建立标签，例如 `v6.0.0-boot4`。
5. 同一构建中收集 `graceful-response-6.0.0-boot4.jar`、`graceful-response-6.0.0-boot4-sources.jar`，并将项目 `pom.xml` 复制为 `graceful-response-6.0.0-boot4.pom`。
6. 为以上三个附件生成 `SHA256SUMS`，记录文件名和 SHA-256 值。
7. 发布 GitHub Release，上传三个附件和 `SHA256SUMS`。说明验证版本、测试范围、安装方法以及未发布 Maven Central 的事实。
8. 检查 Release 的标签提交、附件下载和校验和。

本地安装已下载的二进制包时：

```shell
mvn install:install-file -Dfile=graceful-response-6.0.0-boot4.jar -DpomFile=graceful-response-6.0.0-boot4.pom
```

不要省略 `-DpomFile`，否则传递依赖可能丢失。
