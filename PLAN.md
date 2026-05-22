# 博客文章 CRUD MVP 开发计划

## Summary
当前阶段只实现文章 CRUD，目标是跑通最小完整闭环：

`HTTP 请求 -> ArticleController -> ArticleService -> ArticleRepository -> JdbcTemplate SQL -> MySQL articles 表`

不引入 common 层、统一响应、统一异常、错误码、权限、DTO/VO、Converter、复杂工具类。  
数据库部分明确纳入主流程：自己写建表 SQL，Repository 里自己写 CRUD SQL。

## Key Changes

### 1. 数据库与依赖
新增或修改：

- `pom.xml`
  - 新增 `spring-boot-starter-jdbc`
  - 保留 `spring-boot-starter-webmvc`
  - 保留 `mysql-connector-j`

- `src/main/resources/application.properties`
  - 配置 MySQL 连接：
    - `spring.datasource.url`
    - `spring.datasource.username`
    - `spring.datasource.password`
    - `spring.datasource.driver-class-name`

- `src/main/resources/schema.sql`
  - 新增 `articles` 表建表 SQL

作用：

- 让项目能连接 MySQL。
- 让 Repository 可以通过 `JdbcTemplate` 执行 SQL。
- 让你练习真实数据库表设计和 SQL CRUD。

为什么当前阶段需要：

- 当前目标是完整最小闭环，数据库不能只靠框架自动生成。
- `JdbcTemplate` 足够简单，能直接看到 SQL，不会引入 MyBatis/JPA 的额外概念。
- `schema.sql` 是当前唯一必要的数据库初始化文件，不引入 Flyway/Liquibase。

### 2. 新增文章模型
新增：

- `com.qijx.blog.entity.Article`

作用：

- 表示一篇文章。
- 字段保持最小：
  - `Long id`
  - `String title`
  - `String content`
  - `LocalDateTime createdAt`
  - `LocalDateTime updatedAt`

为什么当前阶段需要：

- Controller、Service、Repository 之间需要传递文章数据。
- 这是 Java 代码和数据库 `articles` 表之间的最小数据结构。
- 暂不加入分类、标签、作者、发布状态、浏览量、slug、软删除等字段。

### 3. 新增 Repository 层
新增：

- `com.qijx.blog.repository.ArticleRepository`

作用：

- 使用 `JdbcTemplate` 手写 SQL 操作 `articles` 表。
- 提供方法：
  - `Article save(Article article)`
  - `List<Article> findAll()`
  - `Optional<Article> findById(Long id)`
  - `int update(Long id, Article article)`
  - `int deleteById(Long id)`

为什么当前阶段需要：

- Repository 是连接 Service 和 Database 的层。
- 这里能练习真实 SQL：
  - `INSERT`
  - `SELECT`
  - `UPDATE`
  - `DELETE`
- `findById` 返回 `Optional<Article>`，方便 Service 判断文章是否存在。
- 不单独创建 RowMapper 类，结果映射逻辑直接放在 Repository 私有方法里，避免过度拆分。

### 4. 新增 Service 层
新增：

- `com.qijx.blog.service.ArticleService`

作用：

- 封装文章 CRUD 主流程：
  - 添加文章
  - 查询文章列表
  - 查询文章详情
  - 修改文章
  - 删除文章

为什么当前阶段需要：

- 让 Controller 不直接操作数据库。
- 让项目形成清晰的三层结构。
- 当前只处理必要业务逻辑：文章不存在时返回 404。

兜底逻辑：

- 查询、修改、删除文章时，如果 ID 不存在，抛出：
  - `ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found")`

为什么这样处理：

- 404 是 CRUD 当前阶段真实需要的行为。
- 但暂不创建全局异常处理类，因为目前错误处理还没有重复复杂到需要抽象。

### 5. 新增 Controller 层
新增：

- `com.qijx.blog.controller.ArticleController`

作用：

- 提供文章 CRUD 接口：
  - `POST /api/articles` 添加文章
  - `GET /api/articles` 查看文章列表
  - `GET /api/articles/{id}` 查看文章详情
  - `PUT /api/articles/{id}` 修改文章
  - `DELETE /api/articles/{id}` 删除文章

为什么当前阶段需要：

- Controller 是 HTTP 请求入口。
- 直接调用 Service。
- 直接接收和返回 `Article`，不引入 DTO/VO。
- 删除成功返回 `204 No Content`。

### 6. 保留健康检查
保留：

- `com.qijx.blog.controller.HealthController`

作用：

- 通过 `GET /api/health` 确认应用启动成功。

为什么当前阶段需要：

- 对调试很有用。
- 不影响文章 CRUD 主流程。

## Database Design
`articles` 表保持最小：

```sql
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

当前不做：

- 软删除字段
- 发布状态字段
- 分类字段
- 标签表
- 用户表
- 索引优化
- 数据库迁移版本管理

## API Behavior
请求示例：

```json
{
  "title": "第一篇博客",
  "content": "这是文章内容"
}
```

接口行为：

- `POST /api/articles`：插入数据库，返回新增后的文章。
- `GET /api/articles`：查询全部文章，按 `id DESC` 或 `created_at DESC` 排序。
- `GET /api/articles/{id}`：查询单篇文章，不存在返回 404。
- `PUT /api/articles/{id}`：更新标题和内容，不存在返回 404。
- `DELETE /api/articles/{id}`：物理删除文章，不存在返回 404，成功返回 204。

## Test Plan
手动测试优先：

1. 创建 MySQL 数据库，例如 `blog_api`。
2. 启动 Spring Boot 项目，确认能连接数据库。
3. 访问 `GET /api/health`，确认服务运行。
4. 调用 `POST /api/articles`，确认数据库插入记录。
5. 调用 `GET /api/articles`，确认能查到列表。
6. 调用 `GET /api/articles/{id}`，确认能查到详情。
7. 调用 `PUT /api/articles/{id}`，确认数据库记录被更新。
8. 调用 `DELETE /api/articles/{id}`，确认数据库记录被删除。
9. 再次查询已删除 ID，确认返回 404。

## Assumptions
- 当前阶段使用 MySQL。
- 当前阶段使用 `JdbcTemplate`，不使用 JPA，不使用 MyBatis。
- 当前阶段自己写 SQL，Repository 负责 SQL 执行。
- 当前阶段 Controller 可以直接接收和返回 `Article`。
- 当前阶段不做统一响应体和统一异常处理。
- 当前阶段删除文章使用物理删除。

## TODO Later
- 数据量变多后再加分页。
- 请求和返回结构复杂后再引入 DTO/VO。
- 错误处理重复后再加统一异常处理。
- 数据库结构稳定后再引入 Flyway/Liquibase。
- 需要用户身份后再加登录和权限。
