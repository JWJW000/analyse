# 工程教育思政融合平台运行与使用说明

本文档用于项目交付、演示和本地开发启动，覆盖技术栈、环境依赖、启动步骤、账号数据和核心功能使用流程。

## 1. 项目定位

本项目是一个面向工程教育课程的“需求分析 + 文献证据 + 工程伦理思政融合 + 课程任务提交”平台。

系统希望解决的问题：

- 学生写需求文档时缺少结构化流程，需求、文献、用例图和伦理映射容易割裂。
- 教师发布课程任务后，难以查看学生是否完成需求正文、文献证据、伦理模块和提交状态。
- 文献调研、工程伦理思政库、课程任务和报告生成原本像独立模块，平台通过“课程任务工作台”和“报告生成”把成果闭环串起来。

推荐主流程：

```text
教师创建课程与任务
→ 学生进入课程任务工作台
→ 完成需求正文
→ 关联文献证据
→ 生成/编辑用例图
→ 匹配工程伦理思政模块
→ 提交教师审核
→ 教师批改与统计
→ 生成课程项目/需求报告
```

## 2. 技术栈

### 2.1 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Ant Design Vue
- Axios
- AntV X6，用于用例图画布
- ECharts，用于统计图表
- docx / file-saver，用于前端导出 Word 内容
- Vitest，用于前端单元测试

前端目录：

```text
frontend/
```

核心页面：

- `frontend/src/views/CoursesView.vue`：课程任务、学生分配、任务统计入口
- `frontend/src/views/TaskWorkspaceView.vue`：课程任务工作台
- `frontend/src/views/RequirementEditorView.vue`：需求编辑、AI 续写、用例图、思政匹配、提交
- `frontend/src/views/LiteratureView.vue`：文献调研与附件上传下载
- `frontend/src/views/EthicsModulesView.vue`：工程伦理思政库
- `frontend/src/views/ReportGenerateView.vue`：报告生成与下载

### 2.2 后端

- Java 17
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- Spring Security
- JWT 登录认证
- MySQL 8
- Flyway 依赖已引入，但当前配置中 `spring.flyway.enabled=false`
- MinIO/S3 SDK，用于可选对象存储
- Maven

后端目录：

```text
backend/
```

核心服务：

- `AuthService`：登录注册与 JWT
- `CourseService`：课程、任务、学生加入
- `TaskWorkspaceService`：课程任务工作台进度与提交检查
- `RequirementService`：需求保存、提交、审核
- `RequirementAssistService`：AI 一键续写、规格向导初稿
- `DiagramGenerationService`：根据正文生成用例图结构
- `LiteratureService`：文献与附件
- `AiMatchService`：思政模块匹配与本地兜底
- `ReportService`：报告生成与下载

### 2.3 数据与外部服务

- MySQL：默认库名 `ethics_sra`
- Redis：当前 Docker 环境中存在，但主业务不强依赖
- MinIO：附件对象存储可选；本地默认也支持文件系统存储
- DashScope / 通义千问：用于 AI 续写等大模型能力
- 本地 AI 匹配服务：`AI_SERVICE_URL=http://localhost:8001`，不可用时后端有本地匹配兜底

## 3. 本地环境要求

建议版本：

- JDK 17+
- Maven 3.9+
- Node.js 20.19+ 或 22.12+
- npm
- Docker Desktop
- MySQL 8，推荐用项目 Docker 容器

当前默认端口：

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| 前端 Vite | `5174` 或 Vite 自动分配端口 | 当前开发访问常用 `http://localhost:5174` |
| 后端 Spring Boot | `8081` | 配置在 `backend/src/main/resources/application.yml` |
| MySQL | `3306` | Docker 容器名通常为 `mysql` |
| MinIO API | `9000` | 可选 |
| MinIO 控制台 | `9001` | 可选 |

## 4. 数据库与依赖服务启动

### 4.1 启动 Docker 依赖

项目提供了 Docker Compose 文件：

```bash
cd /Users/jiangyinhe/workSpace/analyse/deploy
docker compose up -d mysql minio
```

如果你已经有容器，可以查看状态：

```bash
docker ps
```

当前项目常用数据库配置：

```text
host: localhost
port: 3306
database: ethics_sra
username: root
password: root
```

连接数据库：

```bash
docker exec -it mysql mysql -uroot -proot ethics_sra
```

### 4.2 数据库表结构

当前后端配置是：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: false
```

这表示后端启动时只校验表结构，不会自动建表或迁移。

如果是新库，需要先导入已有 SQL 或手动执行 `backend/src/main/resources/db/migration/` 下的迁移脚本。当前本机 Docker MySQL 已清理为小规模演示数据。

### 4.3 当前演示数据

当前库中保留了少量演示数据：

- 用户：4 个
- 课程：1 门，`软件需求工程`
- 课程任务：1 个，`课程任务：校园服务系统需求分析`
- 选课学生：2 个
- 需求：2 条
- 文献：3 篇
- 项目：1 个
- 工程伦理思政模块：4 条

已确认演示账号：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 系统管理员 |
| 教师 | `teacher_demo` | `admin123` | 演示教师 |

学生账号可以在注册页自助注册，也可以由教师在课程学生管理中创建并加入课程。

## 5. 后端启动

进入后端目录：

```bash
cd /Users/jiangyinhe/workSpace/analyse/backend
```

启动后端：

```bash
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8081
```

健康检查：

```bash
curl http://localhost:8081/api/health
```

如果需要覆盖数据库或 AI 配置，可用环境变量：

```bash
MYSQL_HOST=localhost \
MYSQL_PORT=3306 \
MYSQL_DATABASE=ethics_sra \
DASHSCOPE_API_KEY=your-api-key \
mvn spring-boot:run
```

注意：

- 如果 `DASHSCOPE_API_KEY` 未配置，部分 AI 能力会走规则兜底。
- `/api/ai/match` 的外部匹配服务不可用时，后端会自动使用本地相似度兜底，不会因为 `localhost:8001` 拒绝连接而中断主流程。

## 6. 前端启动

进入前端目录：

```bash
cd /Users/jiangyinhe/workSpace/analyse/frontend
```

安装依赖：

```bash
npm install
```

启动前端：

```bash
npm run dev
```

访问地址以终端输出为准，当前常见为：

```text
http://localhost:5174
```

前端代理配置在 `frontend/vite.config.ts`：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true,
    },
  },
}
```

因此本地开发推荐：

```text
浏览器 → Vite 前端 → /api 代理 → Spring Boot 8081
```

## 7. 常用构建与测试命令

### 7.1 后端

编译：

```bash
cd /Users/jiangyinhe/workSpace/analyse/backend
mvn -DskipTests compile
```

运行指定测试：

```bash
mvn -Dtest=ReportServiceTest test
```

打包：

```bash
mvn clean package -DskipTests
```

### 7.2 前端

构建：

```bash
cd /Users/jiangyinhe/workSpace/analyse/frontend
npm run build-only
```

单元测试：

```bash
npm run test:unit
```

类型检查：

```bash
npm run type-check
```

当前说明：

- `npm run build-only` 可通过。
- `npm run type-check` 目前仍有项目既有类型问题，主要集中在旧的 AI composable、项目详情页和 Vite 配置类型，不影响当前前端构建。

## 8. 功能使用说明

### 8.1 登录与注册

访问：

```text
/login
/register
```

功能：

- 用户登录后获取 JWT。
- 学生可自助注册。
- 管理员可创建教师、学生和管理员账号。
- 教师也可以在课程管理中直接创建学生并加入课程。

### 8.2 首页

路径：

```text
/app
```

功能：

- 查看当前角色的快捷入口。
- 学生侧突出课程任务、我的需求、文献证据、伦理素材。
- 教师/管理员侧保留课程任务、全局搜索、资源维护、报告生成和统计入口。

### 8.3 课程任务

路径：

```text
/app/courses
```

教师功能：

- 创建课程。
- 创建课程任务。
- 搜索已有学生并加入课程。
- 当系统无学生时，可以直接创建学生账号并加入课程。
- 查看任务统计。
- 进入作业批改页面。

学生功能：

- 查看已加入课程。
- 查看课程任务。
- 进入任务工作台完成需求文档。

### 8.4 课程任务工作台

路径：

```text
/app/tasks/:assignmentId
```

这是当前学生主流程页面。

功能：

- 查看任务说明、截止时间和完成检查。
- 管理该任务下的需求文档。
- 将需求和文献证据关联。
- 将需求和工程伦理思政模块关联。
- 提交前检查是否具备正文、文献证据和伦理映射。

建议使用方式：

1. 从课程任务进入工作台。
2. 新建或打开需求文档。
3. 完成需求正文。
4. 回到任务工作台关联文献证据。
5. 在需求编辑页匹配并加入思政模块。
6. 满足检查后提交。

### 8.5 需求分析

路径：

```text
/app/requirements
/app/requirements/:id
```

核心能力：

- 需求标题与正文编辑。
- 模板与典型场景起步。
- AI 一键续写。
- 根据标题与正文生成规格向导初稿。
- 根据正文生成用例图。
- 规则完整性评分。
- 多语言与逻辑一致性分析。
- 工程伦理思政模块匹配。
- 提交教师审核。

AI 一键续写：

- 接口：`POST /api/requirements/assist/continue-text`
- 有真实 DashScope API Key 时调用大模型。
- 未配置 API Key 时走本地规则兜底。
- 返回字段 `source` 可判断来源。

用例图生成：

- 接口：`POST /api/ai/generate-use-case`
- 后端从需求正文提取参与者、用例和关系。
- 前端转换为 AntV X6 画布 JSON 后展示。
- 生成后可继续拖拽编辑并保存。

### 8.6 文献调研

路径：

```text
/app/literature
```

功能：

- 新建、编辑、删除文献条目。
- 维护标题、作者、来源、摘要、关键词。
- 上传附件。
- 下载附件。
- 运行 AI 分析，生成摘要与研究提示。

附件存储：

- 本地默认路径：`backend/data/uploads`
- 也支持 MinIO，配置项位于 `app.storage`

### 8.7 工程伦理思政库

路径：

```text
/app/ethics
```

功能：

- 浏览工程伦理与思政模块。
- 教师/管理员可维护模块内容。
- 需求编辑页可调用 AI 匹配，将模块加入需求的伦理映射。

当前保留示例模块：

- 工程社会责任
- 诚信与职业操守
- 数据隐私与合规
- 工程师职业责任

### 8.8 作业批改

路径：

```text
/app/courses/:courseId/assignments/:assignmentId/review
```

教师功能：

- 查看学生提交情况。
- 打开学生需求文档。
- 给出审核状态和评语。
- 批量审核。

状态说明：

- `DRAFT`：草稿
- `SUBMITTED`：已提交
- `APPROVED`：已通过
- `REJECTED`：已退回

### 8.9 统计分析

路径：

```text
/app/stats/course/:courseId
/app/stats/course/:courseId/student/:studentId
/app/stats/global
```

功能：

- 教师查看课程维度提交统计。
- 教师查看学生画像。
- 管理员查看全站统计。
- 支持导出部分统计 CSV。

### 8.10 报告生成

路径：

```text
/app/reports
```

功能：

- 选择项目生成报告。
- 如果没有项目，可选择“我的需求报告”。
- 自动汇总项目关联的文献、需求和思政模块。
- 支持 Word/PDF 选项。
- 点击下载时走后端下载接口，前端以 blob 方式下载，能携带 JWT 登录态。

相关接口：

- `POST /api/reports/generate`
- `GET /api/reports/download/{id}`

说明：

- 当前后端生成的是文本内容字节并按文件名返回，适合演示下载链路。
- 若需要真正的 `.docx` 富格式或 PDF 渲染，可继续接入 Apache POI、docx4j、LibreOffice 或后端 PDF 渲染库。

### 8.11 项目工作台

路径：

```text
/app/projects
/app/projects/:id
```

功能：

- 创建课程项目。
- 关联文献、需求和思政模块。
- 查看项目阶段进度。
- 作为报告生成的数据来源之一。

当前产品主线更偏“课程任务工作台”，项目工作台可作为教师组织课程成果或综合报告的聚合视图。

### 8.12 管理员功能

路径：

```text
/app/admin/users
/app/admin/ops
```

功能：

- 用户管理。
- 创建教师、学生、管理员账号。
- 重置密码。
- 系统运维、日志和备份相关入口。

## 9. 角色权限

| 角色 | 主要功能 |
| --- | --- |
| 学生 | 课程任务、任务工作台、我的需求、文献调研、思政库、报告生成、个人中心 |
| 教师 | 课程管理、任务发布、学生分配、作业批改、课程统计、文献/思政资源维护、报告生成 |
| 管理员 | 用户管理、全站统计、系统运维、全部基础资源维护 |

## 10. 关键接口速查

认证：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`

课程任务：

- `GET /api/courses`
- `POST /api/courses`
- `POST /api/courses/{courseId}/assignments`
- `POST /api/courses/{courseId}/enroll`
- `GET /api/tasks/{assignmentId}/workspace`
- `GET /api/tasks/{assignmentId}/checks`

需求：

- `GET /api/requirements/mine`
- `POST /api/requirements`
- `PUT /api/requirements/{id}`
- `POST /api/requirements/{id}/submit`
- `POST /api/requirements/{id}/review`
- `POST /api/requirements/assist/continue-text`
- `POST /api/requirements/assist/draft-spec`

文献：

- `GET /api/literature`
- `POST /api/literature`
- `POST /api/literature/{id}/file`
- `GET /api/literature/{id}/file`

思政匹配与用例图：

- `POST /api/ai/match`
- `POST /api/ai/embed-feedback`
- `POST /api/ai/generate-use-case`

报告：

- `POST /api/reports/generate`
- `GET /api/reports/download/{id}`

统计：

- `GET /api/stats/me`
- `GET /api/stats/class/{courseId}`
- `GET /api/stats/course/{courseId}/students`
- `GET /api/stats/global`

## 11. 常见问题

### 11.1 注册返回 400

检查用户名和密码长度：

- 用户名至少 3 位。
- 密码至少 6 位。

### 11.2 文献上传后跳到登录页

通常是后端文件存储路径或权限异常导致请求失败。当前已将本地存储改为绝对路径写入，仍需确认后端有权限写入：

```text
backend/data/uploads
```

### 11.3 AI 匹配服务连接 `localhost:8001` 失败

这是外部 AI 匹配服务不可用。当前后端已有本地兜底，不会影响思政模块推荐基本使用。

### 11.4 课程任务统计或附件下载跳登录

下载或统计接口必须携带 JWT。当前报告下载和文献附件下载都使用 axios blob 下载，会自动携带 token。

### 11.5 报告生成项目下拉为空

原因可能是当前账号没有项目。当前报告页已支持“我的需求报告”，不再强制依赖项目。

### 11.6 Docker Compose 全量启动注意事项

`deploy/docker-compose.yml` 中 api 服务映射仍偏旧：

```yaml
ports:
  - "8080:8080"
```

但当前后端配置端口是 `8081`，前端代理也指向 `8081`。如果要用 Compose 启动后端容器，需要统一端口，例如：

```yaml
ports:
  - "8081:8081"
```

或通过环境变量覆盖 Spring Boot 端口。

## 12. 推荐演示路径

教师演示：

1. 使用 `teacher_demo/admin123` 登录。
2. 进入“课程任务”。
3. 查看 `软件需求工程`。
4. 查看任务 `课程任务：校园服务系统需求分析`。
5. 在学生管理中搜索/创建学生。
6. 进入任务批改或课程统计。
7. 进入报告生成，选择项目或需求报告并下载。

学生演示：

1. 注册一个学生账号，或使用已有学生账号。
2. 进入“课程任务”。
3. 打开任务工作台。
4. 新建或编辑需求。
5. 使用 AI 一键续写。
6. 根据正文生成用例图。
7. 匹配并加入工程伦理思政模块。
8. 在任务工作台关联文献证据。
9. 提交教师审核。

管理员演示：

1. 使用 `admin/admin123` 登录。
2. 进入用户管理。
3. 创建或重置教师/学生账号。
4. 查看全站统计和系统运维入口。

## 13. 目录结构概览

```text
analyse/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/        # 业务代码
│   ├── src/main/resources/   # 配置与 SQL 迁移
│   ├── src/test/java/        # 后端测试
│   └── data/                 # 本地上传、备份等运行数据
├── frontend/                # Vue 前端
│   ├── src/api/              # API 封装
│   ├── src/views/            # 页面
│   ├── src/components/       # 组件
│   └── src/utils/            # 工具函数
├── deploy/                  # Docker Compose 与部署配置
├── docs/                    # 项目文档
└── specs/                   # 产品/功能设计文档
```

## 14. 当前验证状态

最近已验证通过的命令：

```bash
cd backend
mvn -DskipTests compile
mvn -Dtest=ReportServiceTest test
```

```bash
cd frontend
npm run build-only
```

构建说明：

- 前端生产构建可通过。
- 构建时可能出现 `:deep` CSS warning 和 chunk size warning，当前不阻断构建。
- 完整 `type-check` 仍有旧模块类型问题，后续可单独清理。
