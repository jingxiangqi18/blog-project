# 留白手记：前端设计与阅读体验

## 设计方向

以阅读和写作为中心。采用 Apple 风格的中性页面与系统字体，结合有边缘与受光层次的控件、连续移动的选中状态和有节奏的内容进入。石墨主按钮、银灰工具槽、白色内容与少量暖色图标分工明确，不再把所有操作都做成贴在背景上的裸文字。只有顶部固定导航和模态遮罩使用局部模糊，内容区不依赖玻璃、背景粒子或视差。

首页保留仓库已有的 `public/studio-editorial-banner.webp`，不新增远程字体、图片请求或动画依赖。首页文章、分区和系列来自真实接口；没有精选标记的数据，不伪造“精选”。

## 设计变量

源文件：`src/styles/tokens.css`。

| 项目 | 规范 |
| --- | --- |
| 页面 / 内容 / 次级底色 | `#f4f5f7` / `#ffffff` / `#eff1f4` |
| 主文字 / 次文字 / 辅助文字 | `#202329` / `#565d67` / `#747c88` |
| 分隔线 / 强分隔线 | `#e0e3e8` / `#cdd2da` |
| 强调色 | `#49667f` 仅用于链接、选中位置和焦点；主要操作使用石墨色，少量暖色用于标记和图标 |
| 字体 | 系统无衬线字体，中文回退 PingFang SC / Microsoft YaHei；代码使用系统等宽字体 |
| 标题 | 首页 54px、移动端 38px；页面标题 30–38px；阅读标题 34px、移动端 28px |
| 正文 | 16px，1.95 行高；移动端 15px；段落间距 1.3em |
| Markdown 标题 | H2 1.5em，上间距 2.8em；H3 1.2em；H4 1.05em |
| 间距 | 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64px |
| 圆角 | 内容条目和控件 4–8px；头像单独使用圆形 |
| 动画 | 快速 160ms、普通 280ms、位置切换 420ms；基础 `cubic-bezier(.22,1,.36,1)`，控件轻回弹 `.22,1.18,.36,1` |

固定字号随断点切换，不使用随视口无限缩放的文字。移动触控设备增加点击区域。全站支持 `prefers-reduced-motion`，JavaScript 平滑滚动也读取该设置。

## 信息架构

- `/`：品牌介绍、动态分区入口、最近更新、已有系列。
- `/articles`：全部手记；`sectionSlug` 选择分区，分类索引、标签、类型和关键词进一步筛选。
- `sectionSlug=life`：使用偏随笔的标题风格；生活文章详情不强制展示复杂技术目录。
- `/articles/:id`：技术或未分区文章使用文档导航和居中正文；原始路由继续可用。
- `/collections`：系列、分区和标签管理；分区、系列结构来自接口。
- `/knowledge`：知识列表与阅读详情组合；手机上转换为上下结构。
- `/me`、`/categories`、`/users`：保留功能，统一表单、列表、权限和空状态。
- `/about`：静态站点介绍，不伪造作者履历和社交信息。

导航里的分区来自 `/api/sections`。主导航只保留首页、文章与知识库；Technology/Life 等属于“文章”下的分区菜单，不再与全站入口混放在同一个分段控件里。系列与分类归入独立的“目录”菜单。手机菜单同样按浏览、文章分区、内容目录、账户分组，并显式比较分区查询参数，避免多个分区同时高亮。不会硬编码 Java、AI、数据库等内容领域。

## 组件与样式边界

| 文件 | 职责 |
| --- | --- |
| `components/SiteShell.vue` | 固定顶栏、动态分区、移动菜单、页脚与跳过导航入口 |
| `components/FluidTabs.vue` | 分区、个人中心、编辑视图与引用类型共享的滑动选中控件，支持键盘、缩放测量和减少动态效果 |
| `components/ArticleSearch.vue` | 全站文章搜索，使用现有文章接口；防抖、过期响应隔离、失败重试与搜索结果跳转 |
| `components/ReferencePicker.vue` | 原文中 `@` 触发的引用浮层；独立搜索、文章/卡片类型、结果区和底部分页 |
| `components/MarkdownMentionEditor.vue` | CodeMirror 编辑、原文引用装饰与浮层桥接；定位和视口避让交给 CodeMirror Tooltip |
| `components/TechBadge.vue` / `utils/tags.js` | 彩色技术标签；图标块和浅色名称块分离，同名标签跨页面保持一致 |
| `components/DocumentNavigation.vue` | 系列选择、轻量目录读取、上下篇、移动目录面板 |
| `components/DocumentTree.vue` | 递归章节分组与当前文章内部标题 |
| `components/useArticleOutline.js` | 正文解析结果、观察当前标题、路由定位与清理 |
| `components/MarkdownContent.vue` | 缓存知识卡片正文与预览渲染，拖动浮窗不重新执行 Markdown 解析和高亮 |
| `components/SeriesChapterManager.vue` | 分组创建、改名、移动、删除、同级排序和文章归组 |
| `utils/series.js` | 将后端平铺目录转换为树，并按显示顺序获取文章 |
| `utils/markdown.js` | 安全 Markdown、稳定标题 ID、源行标记、代码高亮与复制工具栏 |
| `styles/base.css` | 基础控件、Shell、通用状态、过渡与响应式规则 |
| `styles/pages.css` | 首页、内容发现、目录、个人中心、知识库和用户管理 |
| `styles/reading.css` | 文档目录、Markdown 排版、评论与连续阅读 |
| `styles/editor.css` | 写作界面、分屏、草稿和版本界面 |
| `styles/overlays.css` | 保留并统一知识浮窗、预览与 CodeMirror 补全浮层 |

`style.css` 只负责导入，原有多轮覆盖规则已整体整理。路由页面按需加载，编辑器不再进入首页首屏包。语法高亮使用 highlight.js 的常见语言集合，未知语言安全降级为纯文本代码。

## 引用与标签

引用浮层是独立的 Vue 组件，不再把“文章、知识卡片、下一页”都塞进 CodeMirror 的一条补全列表。浮层使用局部半透明表面、清晰边缘、细阴影和短促进入动画，正文与作者信息不再继承编辑器的等宽字体。所有控件都有独立的命中区域，翻页固定在底部，结果区独立滚动。CodeMirror 负责锚点、窗口边界及上下避让；弹层关闭和编辑器销毁时会卸载 Vue 实例并清理请求、监听和 ARIA 状态。

- 文章沿用 `GET /api/articles?page=...&size=5&keyword=...` 的后端分页，排除正在编辑的文章。
- 卡片沿用 `GET /api/knowledge-cards?keyword=...` 返回的数组，在前端以每页 5 张分页，不虚构服务端分页能力。
- 查询防抖 220ms；切换查询、类型或页码后，旧请求不得覆盖新结果。
- 可以在原文处继续输入关键词，也可以在浮层搜索框中查找。搜索、切换和翻页都不改原文，只有确认结果才插入现有的 `[标题](article:ID)` 或 `[标题](card:ID)`。
- 原文里的方向键、Enter、PageUp/PageDown、Escape 与 Tab 搜索聚焦可用；浮层输入框保留正常 Tab 焦点移动与中文输入法确认行为。

技术标签采用紧凑的双色块结构：左侧为深色语义图标，右侧为浅色名称，不用远程徽标图片或伪造品牌 Logo。Java、Spring、Redis、数据库等有固定配色；其他标签用 slug/name 的稳定散列选色，不依赖 ID、列表位置或随机数。颜色仅影响前端，不新增后端颜色字段。文章预览、详情、筛选选项、编辑器标签选项和标签目录复用同一组件；文字对比度至少 4.5:1，图标至少 3:1，已有自动化测试验证。

## 文档阅读

读取顺序：文章详情 → `/api/articles/{id}/series` → 用户选择或 URL 明确指定 `seriesId` → `/api/series/{id}/directory`。

不擅自选择文章所属的第一个系列。系列目录只加载文章摘要和位置，正文按文章单独请求。分组先按同级 `sortOrder`、ID 排序，再显示该分组文章；无分组文章放在根层。上一篇、下一篇按最终目录树的深度优先显示顺序计算。

Markdown 只解析一次即可得到 HTML 与标题集合。标题 ID 支持中文、清除不安全字符并处理重复标题与后缀冲突。H2–H4 进入文章目录；原文不被改写。`IntersectionObserver` 配合动画帧计算活动标题；侧栏只滚动自身，不把整页拉走。

阅读正文若以同文章标题完全相同的 H1 开头，仅给该渲染节点添加隐藏样式，避免标题重复；不同标题、后续 H1、编辑器预览、源行与原文均保留。代码块采用独立深色工具面与适配的高亮颜色，复制状态沿用现有机制。

URL 示例：`/#/articles/18?seriesId=1&heading=请求流程`。使用查询参数避免 Hash History 与正文锚点冲突。标题预留固定顶栏的滚动间距；在移动端，点击标题后自动关闭目录面板。

知识卡片保留悬停预览、多窗口、拖动、缩放、置顶、最大化和关闭；滚动时预览位置同步更新。代码复制优先使用 Clipboard API；非安全上下文尝试浏览器兼容复制，浏览器拒绝时显示手动复制提示。复制在阅读器、知识库和编辑器预览中共享处理。

## 兼容与边界

- 登录仍通过 `/auth/me` 校验旧凭证；公开内容可匿名阅读，写操作按原有用户权限显示。
- CodeMirror、引用语法、编辑/分屏/预览、草稿自动保存和版本历史继续使用现有逻辑。
- 正式文章保存新增元数据；系列关联走现有系列关联接口，不给文章接口发送不存在的 `seriesIds` 字段。
- 草稿接口仍只保存标题、正文和分类。组织信息显示待保存提示，离页时确认。自动保存全部组织字段需要后端扩展草稿 DTO、存储及返回字段，本次不修改后端。
- 系列关联若部分失败，文章已保存的结果保留，并提示到目录中重试。
- 多级目录依赖后端新增接口及 `20261024_series_reading.sql`；本次不执行迁移。
- 首页精选、置顶及作者社交资料没有独立接口，不伪造数据。后续如需后台配置，应提供精选排序字段或独立首页配置接口。
- 深色主题、数学公式渲染、拖拽目录排序属于可选后续，不增加无实际需求的依赖。

## 实际参考

- [Apple UI Design Dos and Don'ts](https://developer.apple.com/design/tips/)：文字对比、内容对齐、空间组织和响应式可读性。
- [Apple HIG Motion](https://developer.apple.com/design/human-interface-guidelines/motion)、[WebKit Responsive Design for Motion](https://webkit.org/blog/7551/responsive-design-for-motion/)：状态变化的连贯性与减少动态效果。
- [Component Gallery Navigation](https://component.gallery/components/navigation/)：区分全站导航与文章内部导航。
- [Radix Navigation Menu](https://www.radix-ui.com/primitives/docs/components/navigation-menu)：菜单状态、活动指示、键盘和焦点处理的参考；未引入 React 组件。
- [Minimal Gallery](https://minimal.gallery/)：参考内容型网站的排版节奏和有真实图片的个人品牌表达，未复制模板与素材。
- [shadcn/vue Button](https://www.shadcn-vue.com/docs/components/button)：参考主次控件、图标工具和统一状态；继续复用 Element Plus，不混装第二套 UI 系统。
- [Reka Tabs](https://reka-ui.com/docs/components/tabs)：参考受控选择、方向键、Home/End 与链接导航的区分。这里是筛选或导航组，不冒用没有对应面板的 ARIA tab 语义。
- [Motion for Vue Layout](https://motion.dev/docs/vue-layout-animations)：参考共享选中态的连续位置变化。本次用 Vue、ResizeObserver 和 CSS 过渡完成有限的位移需求，不为这些效果引入完整动画运行时。
- [Apple HIG Popovers](https://developer.apple.com/design/human-interface-guidelines/popovers/)：参考临时浮层与当前编辑任务之间的关系，避免无关操作挤入结果列表。
- [GitHub Primer Label](https://primer.github.io/brand/components/Label/react/)：参考用于内容元数据的紧凑色块标签，只借鉴视觉与状态，不引入 React 依赖。
- [CodeMirror Tooltip API](https://codemirror.net/docs/ref/#view.showTooltip)：使用已有 Tooltip 定位和生命周期接口，不手写新的浮层定位引擎。

Motion Primitives 网站在上轮调研中返回 403，未复制其实现，也未安装相关依赖。React 专属资源仅作视觉参考，不引入 React 或未经审核的远程安装脚本。

## 验证

```bash
cd frontend
npm test
npm run build
npm run dev -- --port 5178
```

浏览器回归脚本：`tests/visual.mjs`。运行环境需能导入 Playwright，并安装对应 Chromium；不将测试数据加入应用。

```bash
PLAYWRIGHT_MODULE=/absolute/path/to/playwright/index.mjs \
BLOG_TEST_URL=http://127.0.0.1:5178 node tests/visual.mjs
```

实际页面使用本地后端的公开只读接口。系列、多卡片、写入回归使用浏览器请求拦截，避免向数据库写入测试记录。检查 1440 / 1024 / 768 / 390px 的首页、文章、阅读器、编辑器、知识库、个人中心和管理界面；截图输出到 `/tmp/blog-redesign`。

构建仍有 Element Plus 上游 PURE 注释与大包提示；它们不阻断构建。完整 Element Plus 包的进一步按组件导入可以后续单独评估，避免本次增加新的自动导入工具链。

回归覆盖四档视口截图宽度检查、真实公开页面加载，以及隔离测试下的导航层级与分区键盘切换、标签色彩一致性/筛选/跳转、引用浮层四档视口边界/类型切换/分页/键盘/空结果/失败重试/乱序响应、搜索跳转、编辑视图切换、多级目录和锚点、代码复制、多卡片拖拽缩放、文章保存、草稿自动保存、历史恢复、点赞/评论/收藏、分组创建/排序、移动菜单键盘访问、登录后按钮切换及过期会话清理。引用、系列、卡片等测试数据仅存在于浏览器隔离接口中，实际写库没有执行。
