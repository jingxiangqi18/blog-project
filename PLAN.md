# Spring Boot Blog API 从零开发计划

## Summary
- 新建独立项目 `blog-api`，不复用、不读取旧代码；目标是完成一个可本地运行的 REST 后端 blog。
- 技术路线：Java 17+、Maven、Spring Boot 4.0.6、MySQL 8、MyBatis-Plus、Spring Security + JWT、Flyway、springdoc-openapi。
- 第一版功能：管理员登录、文章 CRUD、分类、标签、Markdown 正文、匿名评论提交与审核、AI 总结接口预留。
- 版本依据：Spring Boot 当前文档显示 4.0.6，且要求 Java SDK 17+；MyBatis-Plus 官方文档提供 Spring Boot 4 starter；springdoc v3 支持 Spring Boot 4。

## Key Architecture
- 项目包名使用 `com.qijx.blog`，分层为 `controller`、`service`、`mapper`、`entity`、`dto`、`security`、`config`、`common`、`ai`。
- API 统一返回 `ApiResponse<T>`，分页统一返回 `PageResponse<T>`，异常统一由 `GlobalExceptionHandler` 转换为 JSON。
- 数据库变更全部用 Flyway 管理，禁止手工改表后不写迁移脚本。
- 认证采用管理员账号 + BCrypt 密码哈希 + JWT Bearer Token；`/api/admin/**` 需要登录，公开接口不需要登录。
- AI 只做接口缝合：定义 `ArticleSummaryService` 接口和 `NoopArticleSummaryService` 默认实现，暂不接真实模型、不保存密钥、不引入 Spring AI。

## Data And APIs
| 模块 | 第一版设计 |
| --- | --- |
| 文章 | `articles`：标题、slug、Markdown 正文、人工摘要、AI 摘要、状态 `DRAFT/PUBLISHED`、分类、发布时间、浏览量、逻辑删除 |
| 分类 | `categories`：名称、slug、描述、排序 |
| 标签 | `tags` + `article_tags`：文章和标签多对多 |
| 评论 | `comments`：文章 ID、昵称、邮箱、内容、状态 `PENDING/APPROVED/REJECTED`、审核时间；第一版不做楼中楼 |
| 管理员 | `admin_users`：用户名、密码哈希、角色、启用状态 |
| AI 预留 | 文章表保留 `ai_summary`、`ai_summary_status`、`ai_summary_updated_at`，接口先返回 `AI_PROVIDER_NOT_CONFIGURED` |

| API | 说明 |
| --- | --- |
| `POST /api/auth/login` | 管理员登录，返回 JWT |
| `GET /api/articles` | 公开分页查询已发布文章 |
| `GET /api/articles/{slug}` | 公开查看文章详情 |
| `GET /api/categories`、`GET /api/tags` | 公开查询分类和标签 |
| `GET /api/articles/{id}/comments` | 公开查询已审核评论 |
| `POST /api/articles/{id}/comments` | 匿名提交评论，默认待审核 |
| `POST /api/admin/articles`、`PUT /api/admin/articles/{id}`、`DELETE /api/admin/articles/{id}` | 管理文章 |
| `POST /api/admin/articles/{id}/publish`、`POST /api/admin/articles/{id}/unpublish` | 发布和撤回文章 |
| `POST /api/admin/comments/{id}/approve`、`POST /api/admin/comments/{id}/reject` | 审核评论 |
| `POST /api/admin/articles/{id}/ai-summary` | AI 总结预留接口，第一版不实际生成 |

## Development Phases
1. 环境与脚手架：用 Spring Initializr 创建 Maven 项目，添加 Web MVC、Validation、Security、MySQL、MyBatis-Plus Boot4 starter、Flyway、springdoc；先跑通 `/actuator` 或简单健康接口。
2. Spring Boot 基础学习：理解 `@SpringBootApplication`、Controller、Service、依赖注入、配置文件、profile、请求参数校验。
3. 数据库初始化：建立 `blog` 数据库，编写 `V1__init_schema.sql`，创建核心表和索引，编写 `V2__seed_admin.sql` 初始化管理员。
4. 通用基础设施：实现统一响应、统一异常、参数校验错误、分页模型、枚举状态、时间字段处理。
5. 文章模块：先完成文章的 admin CRUD，再完成公开列表和详情；只允许公开接口读取 `PUBLISHED` 状态文章。
6. 分类与标签：完成分类 CRUD、标签 CRUD、文章绑定标签；文章列表支持按分类、标签、关键词、发布时间筛选。
7. 登录与权限：实现登录、JWT 生成与校验、Spring Security filter、管理员接口鉴权、公开接口放行。
8. 评论模块：实现匿名评论提交、内容长度校验、邮箱不公开、管理员审核、公开接口只展示 `APPROVED` 评论。
9. AI 预留层：增加 `ai` 包、`ArticleSummaryService` 接口、默认 noop 实现、AI 预留 endpoint、错误码和 OpenAPI 文档说明。
10. 文档与手动验收：生成 Swagger UI，维护 README，写清楚本地 MySQL 配置、启动命令、默认管理员、主要 API 调用顺序。
11. 测试补齐：补 controller、service、mapper 的关键测试；本地使用独立 `blog_test` schema，避免污染开发数据。

## Test Plan
- 启动测试：应用能在 `local` profile 下连接 MySQL 并自动执行 Flyway。
- 认证测试：错误密码失败，正确密码返回 token，无 token 访问 admin API 返回 401。
- 文章测试：草稿不会出现在公开列表，发布后可通过 slug 访问，删除后不可访问。
- 评论测试：匿名提交后状态为 `PENDING`，审核通过后才会公开展示，拒绝后不展示。
- AI 预留测试：调用 AI 总结接口返回稳定错误码 `AI_PROVIDER_NOT_CONFIGURED`，不破坏文章数据。
- API 文档测试：Swagger UI 可打开，公开和管理员接口分组清晰，管理员接口标明 Bearer Token。

## Assumptions
- 第一版只做后端 REST API，不做前端页面。
- 第一版使用 MyBatis-Plus，不使用 JPA；SQL 复杂时允许写 XML mapper。
- 第一版文章正文保存 Markdown 原文，不在后端渲染 HTML。
- 第一版评论不做回复、点赞、垃圾评论识别和验证码。
- 第一版本地开发优先，不做 Docker、云服务器、对象存储和真实 AI 接入。
- 参考链接：[Spring Boot 4.0.6](https://spring.io/spring-boot)、[Spring Boot starters](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html)、[MyBatis-Plus install](https://baomidou.com/en/getting-started/install/)、[springdoc v3](https://springdoc.org/v4/)、[JJWT Maven Central](https://central.sonatype.com/artifact/io.jsonwebtoken/jjwt-api/versions)。
