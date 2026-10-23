## 自动 Changelog 启用前的 Reader 变更

### 2026/10/23 · 上游同步（PR #9）
- 同步 Legado 阅读记录数据库 version 112 → 113 的迁移兼容修复，保留已发布的两种 version 112 数据结构。
- 同步 WebDAV 本地书籍自动上传能力，可配合恢复缺失本地书实现多设备阅读；该功能默认关闭。
- 同步 WebDAV 旧备份自动清理与保留数量设置；默认不自动清理。
- 已取消的书源请求头解析不再作为普通错误写入日志。

### Reader 1.0.0-beta.2 · 2026/10/23（PR #8）
- Cronet 自定义 Hosts 改为通过 HostResolverRules 解析地址，不再把请求 URL 的域名直接替换成 IP，保留正确的 Host 与 TLS SNI。
- 单地址 `dnsIp` 可继续使用 Cronet；多地址 `dnsIp` 仍保留 OkHttp 的多地址重试语义。
- `timeout` 与 `followRedirects` 不再无条件让请求退出 Cronet，并修复 Cronet 重定向取消时可能连带取消协程的问题。

### Reader 1.0.0-beta.1 · 2026/10/23（PR #2–#7）
- Reader 启用独立应用身份与包名，可与原版应用共存；应用内更新源切换为 Reader GitHub Releases，并在关于页标明基于 Legado 二次开发。
- 建立 Reader 自己的 `1.0.0-beta.N` / Stable 版本体系、独立 versionCode 迁移规则以及 Stable / Latest 更新通道。
- 目录加载时增加分页与章节数量进度反馈，长目录加载过程更直观。
- 下载与缓存遇到 HTTP 429 时增加智能退避重试，并提供失败章节恢复入口。
- 改进 WebDAV 初始化及异常处理，降低初始化时序导致的访问失败。
- 改进崩溃处理流程，避免异常线程进入不合适的消息循环，并加强异常清理过程的保护。
