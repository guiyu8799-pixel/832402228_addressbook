# 代码规范 / Code Standard

> **规范来源 / Source of this standard**
> 1. [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)（Java 主要依据）
> 2. [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)（前端 JavaScript 主要依据）
> 3. [Conventional Commits 1.0.0](https://www.conventionalcommits.org/zh-hans/)（提交信息规范）
>
> 若上述规范与本文件冲突，以本文件为准；本文件未覆盖的内容遵循上述规范。

本项目只有一个仓库（后端 + 前端静态资源），下列规则对全部代码生效。

---

## 1. 通用

| 项目 | 规则 |
| --- | --- |
| 文件编码 | UTF-8（无 BOM） |
| 换行符 | LF，仓库内通过 `.gitattributes` 固化 |
| 缩进 | 4 个空格（Java / HTML / CSS / JS 统一），不使用 Tab |
| 行宽 | 上限 120 字符；注释可适当放宽 |
| 行尾空格 | 不允许 |
| 文件末尾 | 保留一个换行 |
| 命名语言 | 代码标识符用英文；注释、界面文案用中文 |
| 注释 | 解释「为什么」，不复述「是什么」；公共类与公共方法必须有 Javadoc |

## 2. Java

- 包名全小写：`com.fzu.addressbook.<层>`，分层为 `controller / service / repository / entity / dto / exception / config`。
- 类名 `UpperCamelCase`，方法名与变量名 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`。
- 每个类只承担一个职责；Controller 不写业务逻辑，Service 不拼 SQL，Repository 不返回 DTO。
- 依赖注入一律使用构造器注入，字段声明为 `private final`。
- 校验放在边界：请求体用 Bean Validation 注解，业务规则在 Service 中显式判断并抛 `ApiException`。
- 异常必须带稳定的错误码（`ErrorCode`），不允许把 `Exception.getMessage()` 直接透给前端。
- 事务边界放在 Service 的 public 方法上；只读方法标注 `@Transactional(readOnly = true)`。
- 禁止使用 `System.out.println`，日志统一用 SLF4J。
- 魔法值必须提取为常量或枚举；状态值使用枚举而不是字符串字面量。
- 集合返回空集合而不是 `null`；方法参数可空时在 Javadoc 中写明。

## 3. JavaScript（前端）

- 使用严格模式（`'use strict';`），不使用 `var`，不使用 `eval` / `new Function`。
- 模块内按「常量 → 状态 → 工具函数 → 渲染 → 事件」的顺序组织，便于从上往下阅读。
- DOM 查询结果统一缓存在 `els` 对象中，避免重复查询。
- 所有来自接口或用户输入的文本在插入 `innerHTML` 前必须经过 `escapeHtml`，防止 XSS。
- 布尔与空值判断使用显式比较，不使用隐式类型转换。
- 事件监听使用事件委托，避免为每个卡片单独绑定。
- 不允许把业务数据写死在前端：页面展示的数据一律来自 REST API。

## 4. HTML / CSS

- HTML 使用小写标签与属性，属性值统一用双引号；语义化标签优先（`header / nav / main / section / article`）。
- 交互元素必须可键盘操作：按钮用 `<button>`，输入框配 `<label>`。
- CSS 使用 BEM 风格的类名（`card__head` 允许简写为 `card-head`），禁止用标签选择器做主要布局。
- 颜色、圆角、阴影统一使用 `:root` 中的 CSS 变量，不在样式里散落魔法色值。
- 响应式断点固定为 `1080px` 与 `720px`。
- 不使用 `!important`，除非覆盖第三方样式。

## 5. REST API

- 资源名用复数名词：`/api/contacts`。
- 方法语义：`GET` 查询、`POST` 新建、`PUT` 整体更新、`PATCH` 局部更新、`DELETE` 删除。
- 状态码：创建返回 `201`，删除成功返回 `204`，参数错误 `400`，不存在 `404`。
- 错误响应统一为 `{ "code": "...", "message": "...", "errors": [...] }`。
- 时间字段使用 ISO-8601（`2026-11-17T10:20:30`），由前端负责本地化展示。

## 6. 数据库

- 表名与列名使用 `snake_case`；`group` 是 SQL 关键字，因此列名使用 `grp`。
- 主键统一 `id INTEGER PRIMARY KEY AUTOINCREMENT`。
- 外键必须声明；一对多关系由 JPA 的 `cascade` + `orphanRemoval` 维护一致性。
- 所有 `NOT NULL` 列在实体上显式声明 `nullable = false`。
- 建索引的列：`bookmarked`、`grp`。

## 7. 提交信息（Conventional Commits）

格式：

```
<type>(<scope>): <subject>
```

| type | 用途 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `test` | 测试 |
| `docs` | 文档 |
| `style` | 只改格式 |
| `refactor` | 重构 |
| `chore` | 构建、依赖、脚本 |

示例：

```
feat(contact): 支持一个联系人多条联系方式
feat(excel): 实现 Excel 导出与导入
fix(excel): 网址不再被误拆成「标签: 值」
```

- 一次提交只做一件事，禁止 `update`、`fix bug` 这类无信息量的说明。
- 提交信息用中文描述「做了什么」，不超过 50 字。

## 8. 分支与合并

- `main` 始终保持可构建、可运行。
- 功能开发在 `feat/<name>` 分支进行，通过 Pull Request 合入 `main`。
- PR 描述必须写清：改动内容、验证方式、涉及的功能点。

## 9. 检查方式

```bash
# 编译并运行全部测试
mvn test

# 检查行尾空格与 Tab（PowerShell）
Select-String -Path (Get-ChildItem -Recurse src -Include *.java,*.js,*.css,*.html) -Pattern "`t| +$"
```
