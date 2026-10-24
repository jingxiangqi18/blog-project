# 系列阅读与多级章节 API

系列现在可以同时包含有序文章和多级章节分组。分组只组织目录，不替代文章分类，也不改变正文 Markdown 内的标题。文章内目录、滚动高亮、锚点定位仍由前端解析 Markdown 实现。

## 从阅读请求到数据库

打开阅读侧栏时，前端请求 `GET /api/series/{id}/directory`。Controller 调用 SeriesService 检查系列，再由 SeriesRepository 分别查询系列元信息、分组、文章标题与位置。目录查询不读取文章正文，也不批量加载标签。

点击某篇文章时，仍调用原来的 `GET /api/articles/{id}` 加载正文。从文章详情进入时，可先请求 `GET /api/articles/{id}/series` 让读者选择系列。文章可属于多个系列，前端路由应保留当前选定的 `seriesId`，不要默认把第一个系列作为唯一归属。

## 数据库升级

- 新增 `series_chapters`：`id`、`series_id`、`parent_id`、`title`、`sort_order`、`created_at`、`updated_at`。
- `parent_id` 为空表示顶层分组，否则指向同一系列的父分组。
- `series_articles` 新增可空 `chapter_id`，为空表示文章暂未分组。
- 同一文章可以在不同系列中归入不同分组；分组位置保存在系列文章关联表，不修改文章本身。

已有数据库先确认 `20261023_content_organization.sql` 已执行，再执行 **一次**：

```bash
# 在仓库根目录执行，密码交互输入。
mysql -h YOUR_HOST -P YOUR_PORT -u YOUR_USER -p YOUR_DATABASE \
  < backend/database/20261024_series_reading.sql
```

也可在连接目标 MySQL/TiDB 的 SQL 编辑器中选择数据库，执行这份文件全文。该迁移保留原来的系列、文章关联及 `sort_order`；历史关联的 `chapter_id` 默认为空。新数据库只执行完整的 `database/schema.sql`。

ALTER 语句按执行一次设计，中断后应先检查已完成的列、索引、约束，再继续剩余语句。应用不会启动时自动迁移，也不会把现有文章自动分组。

## 公开读取接口

| 方法与路径 | 返回内容 |
| --- | --- |
| `GET /api/series/{id}/directory` | 系列信息、多级分组关系、文章标题/摘要/归属/顺序，均不带正文 |
| `GET /api/articles/{id}/series` | 该文章所属系列的元信息列表，不带各系列正文 |

轻量目录示例（ID 仅为示例，实际使用接口返回的值）：

```json
{
  "id": 1,
  "slug": "java-basics",
  "title": "Java 基础",
  "description": "基础概念与常见问题",
  "sectionId": 1,
  "sectionSlug": "technology",
  "sectionName": "Technology",
  "categoryId": null,
  "chapters": [
    {"id": 10, "seriesId": 1, "parentId": null, "title": "概念", "sortOrder": 1},
    {"id": 11, "seriesId": 1, "parentId": 10, "title": "数据类型", "sortOrder": 1}
  ],
  "articles": [
    {"id": 20, "title": "自动装箱与拆箱", "summary": null, "chapterId": 11, "sortOrder": 1}
  ]
}
```

分组还返回创建和修改时间，示例省略。`chapters` 使用平铺关系，前端通过 `parentId` 构建树，不需要再逐层请求服务器。每组同级分组按 `sortOrder`、ID 排序；文章保留整个系列原有的排序值，在同一分组内按该顺序展示。未分组文章通过 `chapterId: null` 识别。分组与文章分别有顺序，没有混合节点排序。

上一篇、下一篇可从前端最终展示的目录顺序计算。旧的整个系列文章顺序仍由 `/articles/order` 管理；调整分组位置不会悄悄重写该顺序。

文章存在但未加入系列时，所属系列接口返回 `[]`；文章或系列不存在返回 404。公开读取行为不变，分区禁用不等同于文章私有。

## 章节管理接口

以下写操作都要求 `Authorization: Bearer ...`。SeriesService 继续使用 CurrentUserService 校验用户当前启用状态，只允许系列创建者或管理员修改，并在事务内锁定系列后操作。

| 方法与路径 | 请求体与用途 |
| --- | --- |
| `POST /api/series/{id}/chapters` | `{"title":"概念","parentId":null}`，追加顶层分组 |
| `PUT /api/series/{id}/chapters/{chapterId}` | `{"title":"数据类型","parentId":10}`，改名或移动分组 |
| `DELETE /api/series/{id}/chapters/{chapterId}` | 删除没有子分组的分组，文章保留并回到未分组状态 |
| `PUT /api/series/{id}/chapters/order` | `{"parentId":null,"chapterIds":[12,10]}`，完整调整同一父分组下的分组顺序 |
| `PUT /api/series/{id}/articles/{articleId}/chapter` | `{"chapterId":11}`，移动已加入系列的文章；传 `null` 移回未分组 |

创建子分组时传父分组 ID；更新分组是完整的标题和父分组替换，省略或传空 `parentId` 都表示顶层。新建分组自动排到同级末尾，换父分组后追加到新父分组末尾，同级内改名保留原位置。

校验规则：

- 标题必填，最长 200 字符。
- 父分组和目标分组必须属于当前系列。
- 不能把分组挂到自己或自己的后代下。
- 最多 8 层、每系列最多 500 个分组；移动时也检查后代的新深度。
- 分组排序必须完整包含该父分组下全部直接子分组，每个 ID 恰好出现一次。
- 文章必须已经通过原有添加文章接口加入系列；分组接口不会隐式新增关联。
- 存在子分组时删除父分组返回 409，需先移动或删除子分组。

无效结构、排序或格式返回 400，未登录/账号禁用返回 401，无操作权限返回 403，当前系列内找不到目标分组或文章关联返回 404。沿用项目的 ResponseStatusException，不新增错误处理框架。

## 兼容性与事务

原有系列详情和 `/articles` 接口继续返回完整文章，并增量返回关联的 `chapterId`。原有添加文章行为不变：新关联不分组，重复添加不会清空已有分组。系列文章排序只更新位置，不修改分组；移出系列会删除该系列中的分组关联。

分组排序、移动文章、删除分组等多步写操作均在事务内执行。目录写操作使用 READ_COMMITTED 隔离级别，保证等待系列锁后读到其他请求已提交的最新分组状态，避免用旧快照校验循环或排序。删除系列会清理分组和关联，但不会删除文章。原始文章 API、草稿、版本历史、评论、点赞、收藏和知识卡片不受此次扩展影响。

测试覆盖轻量响应不含正文、文章反查多个系列、多级分组创建和移动、循环与深度校验、跨系列拒绝、权限、同级排序完整性、排序中途失败回滚，以及删除分组/系列保留文章。迁移测试验证升级前的系列关联和顺序保留，并比较迁移后与全新 schema 的列、索引和外键。

测试仍使用 H2 MySQL 模式；真实 MySQL/TiDB 的迁移需要在目标库中单独验证。
