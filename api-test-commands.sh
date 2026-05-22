#!/usr/bin/env bash

BASE_URL="${BASE_URL:-http://localhost:8080}"
ARTICLE_ID="${ARTICLE_ID:-1}"

cat <<EOF
Blog API 手动测试命令

说明：
- 这个脚本只打印命令，不会真正执行请求。
- 默认接口地址：${BASE_URL}
- 示例文章 ID：${ARTICLE_ID}
- 如果你的端口或 ID 不一样，可以这样运行：
  BASE_URL=http://localhost:8080 ARTICLE_ID=2 ./api-test-commands.sh

----------------------------------------
功能：健康检查
命令：
curl -i ${BASE_URL}/api/health

----------------------------------------
功能：创建一篇文章
命令：
curl -i -X POST ${BASE_URL}/api/articles \\
  -H "Content-Type: application/json" \\
  -d '{"title":"第一篇文章","content":"这是文章内容"}'

----------------------------------------
功能：查看文章列表
命令：
curl -i ${BASE_URL}/api/articles

----------------------------------------
功能：查看文章详情
状态：等 GET /api/articles/{id} 实现后使用
命令：
curl -i ${BASE_URL}/api/articles/${ARTICLE_ID}

----------------------------------------
功能：修改文章
状态：等 PUT /api/articles/{id} 实现后使用
命令：
curl -i -X PUT ${BASE_URL}/api/articles/${ARTICLE_ID} \\
  -H "Content-Type: application/json" \\
  -d '{"title":"修改后的标题","content":"修改后的文章内容"}'

----------------------------------------
功能：删除文章
状态：等 DELETE /api/articles/{id} 实现后使用
命令：
curl -i -X DELETE ${BASE_URL}/api/articles/${ARTICLE_ID}

EOF
