# 极限编程作业——通讯录 Address Book

本仓库是《软件工程（FZU–MU）》极限编程（Extreme Programming）作业的完整交付物：一个支持**收藏联系人**、**一个联系人多条联系方式**、**Excel 导入导出**的 Web 通讯录。

> 作业要求原文：<https://bbs.csdn.net/topics/620593436>

**提交信息**：学号 832402228　姓名 周祺粲　GitHub 用户 `guiyu8799-pixel`

**仓库地址**：<https://github.com/guiyu8799-pixel/832402228_addressbook>

**代码规范**：<https://github.com/guiyu8799-pixel/832402228_addressbook/blob/main/codestyle.md>

---

## 一、作业要求达成情况

| 要求 | 分值 | 实现情况 |
| --- | --- | --- |
| 1.1 收藏联系人 | 25 | 已实现。`contacts.bookmarked` 字段 + `PATCH /api/contacts/{id}/bookmark`；星标按钮、收藏筛选、收藏优先排序 |
| 1.2 一个联系人多条联系方式 | 10 | 已实现。`contact_methods` 一对多表，支持电话 / 邮箱 / 社交 / 地址四类，每类可带自定义标签、可无限增删 |
| 1.3 Excel 导入导出 | 25 | 已实现。导出每行一个联系人、每类信息一列；导入支持中英文表头、逐行校验、错误行号提示；另提供模板下载 |
| 1.4 Web 云部署 | +15（选做） | 未做。本地 `java -jar` 一键运行，部署说明见第四节 |
| 页面样式 | 10 | 卡片 + 表格双视图、响应式布局、弹窗表单、Toast 提示 |
| 博客与仓库 | 30 | 见 `BLOG.md` 与 GitHub 仓库 |

---

## 二、技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、Java 8、Spring Data JPA、Bean Validation |
| 数据库 | SQLite（`sqlite-jdbc` + `com.github.gwenn:sqlite-dialect`） |
| Excel | Apache POI 5.2.5（XSSF） |
| 前端 | 原生 HTML5 + CSS3 + ES6，无框架、无构建步骤，由 Spring Boot 直接托管 |
| 测试 | JUnit 5 + MockMvc + AssertJ，共 **31** 个测试 |

选择「单仓库 + 后端托管前端」的原因：一次 `java -jar` 即可同时提供页面与 API，避免跨域与两个进程的启动顺序问题；同时仍把前端代码独立放在 `src/main/resources/static`，职责边界清晰。

---

## 三、目录结构

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/fzu/addressbook/` | 后端源码：`controller / service / repository / entity / dto / exception / config` |
| `src/main/resources/static/` | 前端页面：`index.html`、`css/app.css`、`js/app.js` |
| `src/main/resources/application.yml` | 端口、SQLite 路径、上传大小等配置 |
| `src/test/` | 31 个自动化测试 |
| `docs/screenshots/` | 14 张演示截图 + `manifest.json` |
| `docs/address-book-demo.mp4` | 完整运行演示视频（24 秒） |
| `docs/demo-contacts.xlsx` | 用于演示导入的 10 条示例数据 |
| `docs/api.http` | 可直接执行的接口请求集合 |
| `scripts/` | 截图 / 录屏 / 示例数据生成脚本 |
| `codestyle.md` | 代码规范 |
| `start.bat` / `start.sh` | 一键启动并打开浏览器 |

---

## 四、快速开始

### 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 8 或更高（本次在 1.8.0_231 上验证） |
| Maven | 3.6 或更高 |
| 浏览器 | Chrome / Edge 90+、Firefox 88+、Safari 14+ |

### 运行

```bash
# 1. 构建
mvn clean package

# 2. 启动（二选一）
java -jar target/addressbook-1.0.0.jar
# 或双击 start.bat（Windows）/ ./start.sh（Linux、macOS）

# 3. 打开
http://localhost:8080/
```

数据库文件位于 `./data/addressbook.db`，首次启动时自动创建目录与表结构；删除该文件即可重置数据。

---

## 五、接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/contacts?q=&group=&bookmarked=` | 列表，支持关键词、分组、收藏过滤 |
| GET | `/api/contacts/summary` | 总数、收藏数、分组计数 |
| GET | `/api/contacts/{id}` | 单个联系人 |
| POST | `/api/contacts` | 新建（201） |
| PUT | `/api/contacts/{id}` | 整体更新 |
| DELETE | `/api/contacts/{id}` | 删除（204） |
| PATCH | `/api/contacts/{id}/bookmark` | 收藏开关 |
| GET | `/api/excel/export` | 导出全部联系人 |
| GET | `/api/excel/template` | 下载导入模板 |
| POST | `/api/excel/import` | 导入（multipart，字段名 `file`） |
| GET | `/api/health` | 健康检查 |

错误统一返回：

```json
{ "code": "VALIDATION_ERROR", "message": "请求参数不合法", "errors": ["姓名不能为空"] }
```

---

## 六、Excel 格式约定

导出与导入共用同一套表头：

| 姓名 | 分组 | 公司 | 职位 | 收藏 | 电话 | 邮箱 | 社交 | 地址 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

- **一行一个联系人，一列一类信息**，符合作业要求。
- 同一类联系方式有多条时，写在同一格内，用分号分隔，格式为 `标签: 内容`：
  `手机: 13800138000; 宿舍: 0591-22861234`
- 没有标签时只写内容；`https://...`、`mailto:...` 这类值不会被误拆成「标签: 值」。
- 导入时逐行处理：某一行失败只记录行号并跳过，不影响其他行。
- 导出的文件可以再次导入，联系方式与标签不丢失（有自动化测试覆盖）。

---

## 七、测试

```bash
mvn test
```

| 测试类 | 数量 | 覆盖内容 |
| --- | --- | --- |
| `ContactServiceTest` | 11 | 新建/更新/删除、级联、收藏排序、搜索、分组过滤、统计、404 |
| `ExcelServiceTest` | 9 | 导出表头与行、导出再导入的往返一致性、模板、错误行、英文表头、URL 解析、非法文件 |
| `ContactApiIntegrationTest` | 11 | 各接口的状态码与响应体、过滤参数、导出响应头、导入、健康检查 |

结果：**Tests run: 31, Failures: 0, Errors: 0**。

---

## 八、设计要点

1. **收藏是一条数据，不是一个前端状态**：`bookmarked` 落在数据库并建索引，列表查询 `ORDER BY bookmarked DESC, name ASC`，所以刷新、换设备、导入导出后收藏关系都不会丢。
2. **多联系方式用一对多表，而不是「电话 1 / 电话 2」这种列**：`contact_methods(contact_id, type, label, value, sort_order)` 真正支持任意数量；编辑时整体替换并依赖 `orphanRemoval` 清理旧行。
3. **Excel 是数据的另一种视图**：导出把多值压进单元格（`标签: 值; ...`），导入用同一规则还原，并用 `parseMethods` 处理分号、全角冒号与 URL 的边界情况。
4. **启动前创建数据库目录**：SQLite 不会自动建父目录，`DatabaseDirectoryPostProcessor` 在数据源初始化前完成建目录，`java -jar` 与测试环境行为一致。

---

## 九、演示材料

`docs/screenshots/manifest.json` 记录每张截图的文件名与说明，共 14 张：

| 文件 | 内容 |
| --- | --- |
| `01-empty-state.png` | 空状态 |
| `02-add-dialog.png` | 新增联系人（5 条联系方式） |
| `03-created-contact.png` | 保存后的卡片 |
| `04-bookmarked.png` | 收藏后高亮与排序 |
| `05-favorites-filter.png` | 收藏筛选 |
| `06-import-result.png` | 导入 10 条后共 11 位 |
| `07-export-toast.png` | 导出 Excel |
| `08-search-by-phone.png` | 用手机号搜索 |
| `09-edit-dialog.png` | 编辑弹窗回填 |
| `10-delete-confirm.png` | 删除二次确认 |
| `11-table-view.png` | 表格视图 |
| `12-persistence-after-reload.png` | 刷新后数据仍在 |
| `13-mobile-layout.png` | 移动端布局 |
| `14-excel-content.png` | 导出文件内容（读取真实 xlsx 渲染） |

运行视频：`docs/address-book-demo.mp4`。

---

## 十、已知限制

- 未做云服务器部署（选做项），本地运行方式见第四节。
- 单用户使用，没有登录与多用户隔离。
- 导入不去重，同一文件重复导入会产生重复联系人（行为与「追加导入」一致）。
