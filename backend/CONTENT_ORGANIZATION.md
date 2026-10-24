# 内容组织 API 与数据库升级

这次升级继续使用 Controller → Service → Repository → JdbcTemplate。分区、分类、标签和系列分别解决不同的问题：分区区分内容空间，分类表示主题，标签支持跨领域检索，系列提供有顺序的阅读目录。文章仍然只有一张表。

## 数据模型

| 表 | 增加或保留的关系 |
| --- | --- |
| `sections` | 数据库配置的分区，唯一 `slug`，支持排序、启用和禁用 |
| `articles` | 增加可空 `section_id`、可空 `summary`、默认 `general` 的 `content_type` |
| `categories` | 增加可空 `section_id`；空值表示全局分类 |
| `tags` | 全局标签，唯一 `slug`，不绑定分区 |
| `article_tags` | 文章与标签多对多，复合主键防止重复关联 |
| `series` | 系列归属一个分区，分类可空，创建人由 JWT 确定 |
| `series_articles` | 系列与文章多对多，`sort_order` 表示章节位置 |

`technology`、`life` 由 SQL 初始化；以后可以通过管理员 API 增加分区，无须修改 Java。旧文章和旧分类的分区保持为空，不猜测内容归属。

## 迁移顺序

SQL 不会随 Spring Boot 启动自动执行。已有数据库应先备份，在选择正确数据库后执行 **尚未执行过的** 迁移：

1. `database/20260808_article_drafts_and_revisions.sql`
2. `database/20260808_social_interactions.sql`
3. `database/20260812_knowledge_cards.sql`
4. `database/20261023_content_organization.sql`
5. `database/20261024_series_reading.sql`

如果前三项已经执行，依次运行第 4、5 项；如果第 4 项也已经完成，只运行第 5 项。MySQL 客户端示例（在仓库根目录执行，密码使用交互输入）：

```bash
mysql -h YOUR_HOST -P YOUR_PORT -u YOUR_USER -p YOUR_DATABASE \
  < backend/database/20261023_content_organization.sql
```

TiDB Cloud 可在对应集群的 SQL Editor 中选择数据库，再按顺序运行尚未执行的文件。生产连接的 TLS 设置继续使用该集群要求的配置。

新数据库只执行更新后的 `database/schema.sql`，**不再执行这些增量迁移**。`CREATE TABLE IF NOT EXISTS` 不会替已有表补列，因此不能用初始化 schema 替代迁移。

本次迁移使用普通 `ADD COLUMN`、`CREATE INDEX` 和 `ADD CONSTRAINT`，不依赖 MySQL 不支持的 `ADD COLUMN IF NOT EXISTS`/`ADD INDEX IF NOT EXISTS`。旧的 social 迁移中存在后两种 TiDB 扩展语法；若要在原生 MySQL 执行尚未执行过的旧迁移，应先用 `SHOW COLUMNS FROM comments`、`SHOW INDEX FROM comments` 确认缺失项，再对缺失项使用去掉 `IF NOT EXISTS` 的对应语句。

新迁移中的 ALTER 语句按**执行一次**设计。MySQL/TiDB DDL 不随业务事务整体回滚；中断后应检查已执行的列、索引和约束，从未完成的语句继续，不要直接重复整个文件。

迁移完成后可以检查：

```sql
SELECT slug, name, enabled, sort_order FROM sections ORDER BY sort_order, id;
SELECT COUNT(*) AS all_articles,
       SUM(section_id IS NULL) AS unassigned_articles
FROM articles;
SHOW COLUMNS FROM articles;
SHOW COLUMNS FROM categories;
```

应先迁移数据库，再启动升级后的后端。迁移没有删除文章、用户、分类、草稿、历史版本、知识卡片或互动记录。

## 请求与兼容规则

原有接口路径、分页响应字段和文章字段保留；新字段以增量方式返回。分页仍从 `page=1` 开始，`size` 默认 10、最大 50。

文章新增响应字段为 `sectionId`、`sectionSlug`、`sectionName`、`summary`、`contentType`、`tagIds`、`tags`。分类增加分区 ID、slug 和名称。

文章创建或更新仍要求原来的 `title`、`content`、`categoryId`。创建请求省略新字段时，分区和摘要为空，类型为 `general`，标签为空列表。更新请求省略新字段时，**保留原值**，防止旧前端保存正文时擦掉新元数据。

明确传入 `sectionId: null` 取消分区；`summary: null` 清空摘要；`tagIds: []` 清空标签。`tagIds: null` 返回 400。重复标签 ID 会去重，无效标签 ID 返回 400，不会部分保存。

`summary` 最长 500 字符，`contentType` 最长 50 字符，必须符合小写英文标识格式，例如 `general`、`interview_notes`、`reading_notes`；没有固定枚举列表。显式传入空值、空白或非法类型会返回 400。标签最多 50 个。

分区、标签和系列的 `slug` 最长 100 字符，格式为小写字母起头、小写字母数字组成的短横线分隔单词，例如 `java-backend`。重复 slug 返回 409。

分区禁用后从公开分区列表、公开 slug 详情中隐藏，并禁止新内容分配到该分区。已有文章仍公开可读，也可以保留原分区编辑正文。系列中新加文章同样要求分区启用。分区不提供物理删除 API，数据库外键也限制删除被引用的分区。

文章有分区、分类也有分区时，两者必须一致；全局分类可用于任意分区。未分区历史文章仍可使用任意已有分类。更新分类的分区时，会检查现有文章和系列，防止制造跨分区冲突；旧客户端只修改分类名时保留分区。

## 文章与分类接口

| 方法与路径 | 用途 |
| --- | --- |
| `GET /api/articles` | 保留 `page`、`size`、`keyword`、`categoryId`；新增 `sectionSlug`、`contentType`、`tagSlug`、`seriesId` |
| `POST /api/articles` | 原有字段基础上可传分区、摘要、类型、标签 ID |
| `PUT /api/articles/{id}` | 修改内容及元数据；权限仍为作者或管理员 |
| `GET /api/articles/{id}` | 返回原有字段及新增元数据、标签 |
| `GET /api/categories` | 可用 `sectionId` 或 `sectionSlug` 筛选；结果包含分区专属分类和全局分类 |
| `POST /api/categories`、`PUT /api/categories/{id}` | 管理员可传 `sectionId`；其余分类能力保留 |

分类筛选同时传 ID 和 slug 时必须指向同一分区，否则返回 400。未知分区 slug 返回 404。文章筛选使用匹配查询，未知 slug 得到空分页结果。

创建文章示例（ID 使用查询接口返回的真实值）：

```json
{
  "title": "理解 JVM 内存模型",
  "content": "Markdown 正文",
  "categoryId": 1,
  "sectionId": 1,
  "summary": "这篇文章讨论 JVM 内存模型与并发可见性。",
  "contentType": "interview_notes",
  "tagIds": [1, 2]
}
```

组合查询：

```text
GET /api/articles?page=1&size=10&sectionSlug=technology&contentType=interview_notes&tagSlug=jvm&seriesId=1
```

标签与系列通过 SQL `EXISTS` 筛选，不把多个关联行直接 JOIN 到文章分页中；总数和列表使用相同条件。返回标签时对整页文章一次批量查询，避免每篇文章再查一次标签。

## 分区接口

| 方法与路径 | 权限与用途 |
| --- | --- |
| `GET /api/sections` | 公开；启用分区，按 `sort_order`、ID 排序 |
| `GET /api/sections/{slug}` | 公开；启用分区详情 |
| `GET /api/sections/admin/all` | 管理员；含禁用分区 |
| `POST /api/sections` | 管理员；`slug`、`name`，可选 `description`、`sortOrder`、`enabled` |
| `PUT /api/sections/{id}` | 管理员；编辑分区配置 |
| `PATCH /api/sections/{id}/enabled` | 管理员；请求体 `{"enabled": false}` 或 `true` |

## 标签接口

| 方法与路径 | 权限与用途 |
| --- | --- |
| `GET /api/tags?keyword=jvm` | 公开；按名称或 slug 搜索 |
| `GET /api/tags/{id}` | 公开；标签详情 |
| `GET /api/tags/slug/{slug}` | 公开；通过 slug 查询 |
| `POST /api/tags` | 管理员；`name`、`slug`，可选 `description` |
| `PUT /api/tags/{id}` | 管理员；编辑标签 |
| `DELETE /api/tags/{id}` | 管理员；删除标签和关联，不删除文章 |

## 系列接口

| 方法与路径 | 权限与用途 |
| --- | --- |
| `GET /api/series` | 公开分页；支持 `page`、`size`、`sectionId`、`sectionSlug`、`keyword` |
| `GET /api/series/{id}` | 公开详情，`articles` 按章节顺序返回 |
| `POST /api/series` | 已登录且启用的用户；`slug`、`title`、`sectionId`，可选 `description`、`categoryId` |
| `PUT /api/series/{id}` | 创建者或管理员；编辑系列配置 |
| `DELETE /api/series/{id}` | 创建者或管理员；只删系列及其关联 |
| `GET /api/series/{id}/articles` | 公开；返回 `[{"sortOrder": 1, "article": {...}}]` |
| `POST /api/series/{id}/articles/{articleId}` | 创建者或管理员；追加到末尾，重复添加不产生重复行 |
| `DELETE /api/series/{id}/articles/{articleId}` | 创建者或管理员；移除关联 |
| `PUT /api/series/{id}/articles/order` | 创建者或管理员；完整重排章节 |

排序请求示例：

```json
{"articleIds": [12, 8, 20]}
```

必须传入当前系列的全部文章且每篇恰好出现一次，最多 500 篇；不能借排序接口偷偷新增、删除或引入其他系列的文章。Service 在事务内先验证整个列表，再按数组顺序赋值 `1..N`。同一系列写入会锁定系列记录，避免两个追加请求产生相同的末尾位置。

系列可以引用公开文章，不修改文章本身；系列所属分区必须与已经分区的文章一致，未分区的历史文章允许关联，且不会因此自动获得分区。修改文章分区、系列分区时都会检查既有关联。

所有写接口沿用 `Authorization: Bearer ...`，通过 `CurrentUserService` 查用户当前启用状态和数据库角色。请求体里的 `createdBy`、`authorId` 不作为授权依据。缺失或失效凭证返回 401，无资源操作权限返回 403；业务校验失败返回 400，资源不存在返回 404，重复标识返回 409。错误继续使用项目现有的 `ResponseStatusException` 机制。

## 测试

```bash
cd backend
./mvnw test
./mvnw package
```

测试专用依赖 H2 只在 test scope 中，生产仍然使用 MySQL Connector/J。测试使用 `test` profile 和内存数据库，不连接本地或线上数据库。迁移测试读取固定的升级前 schema，以及实际的新迁移和初始化 SQL，比较表、字段、索引和外键，并检查历史记录未丢失。

HTTP 回归测试通过 MockMvc、真实 Service/Repository、JWT 和 SQL 验证旧请求兼容、新增字段、权限、分区关联、标签去重、组合分页及系列章节操作。另有数据库约束故障测试，检查文章/标签和批量排序在中途失败后能完整回滚。

H2 的 MySQL 模式不能替代真实 MySQL/TiDB。上线前仍需在备份数据库或测试分支执行迁移，确认对应数据库版本的 DDL 和实际数据规模下的查询行为。

## 当前边界

多级系列章节、轻量阅读目录和文章所属系列查询已进一步补齐，见 [系列阅读 API](SERIES_READING.md)。

- `frontend/` 保持原样，因此新分区、标签和系列暂时通过 REST API 管理。
- 草稿继续保存原有的标题、正文和分类。新组织字段由正式文章接口保存，不会被旧前端发布或保存正文时清空。
- 历史版本仍记录原有标题、正文和分类；恢复时保留当前分区、摘要、类型和标签。若历史分类与当前分区冲突，返回 400，避免生成无效关联。
- 分区关闭不等于文章私有；这次没有改变文章、知识卡片或互动功能的公开读取规则。
- 名称关键词查询沿用 `LIKE`，已有名字索引不保证任意包含查询都能走索引，暂不引入全文检索。
- 本次不自动运行线上迁移、不替用户给历史文章分区，也不引入自动迁移框架。
