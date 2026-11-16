# CSDN 发布指南

本文件说明如何把 `BLOG.md` 发布到 CSDN 并完成作业提交。按顺序做即可。

---

## 一、发布前必须知道的 4 个坑

| 坑 | 现象 | 正确做法 |
| --- | --- | --- |
| **图片不会自动带过去** | 正文里写的是 `docs/screenshots/xx.png`，粘贴到 CSDN 后全是裂图 | 在 CSDN 编辑器里逐张重新上传（见第三节） |
| **粘贴方式** | 直接 Ctrl+V 可能把表格、代码块粘坏 | 先把编辑器切到「**Markdown**」模式，再粘贴 Markdown 源码 |
| **目录锚点** | 手写的 `[xxx](#anchor)` 锚点可能失效 | 用 CSDN 自带的目录功能，或保留标题让平台自动生成 |
| **审核时间** | 发布后帖子可能短暂 404（审核中） | 老师明确要求提前发布，别卡在截止时间前 |

---

## 二、创建博客并粘贴正文

1. 打开 <https://mp.csdn.net/mp_blog/creation/editor>
2. 右上角把编辑器切到「**Markdown**」模式
3. 打开本地的 `BLOG.md`，**全选复制**，粘贴进编辑器
4. 标题建议：

```
极限编程作业——通讯录 Address Book（学号 832402228 周祺粲）
```

5. 按第三节处理图片，然后发布

---

## 三、上传 18 张图片

图片都在 `docs/screenshots/` 下，正文中每处 `![说明](docs/screenshots/xx.png)` 都要替换成 CSDN 上传后的图片。

| 正文位置 | 文件 | 图中内容 |
| --- | --- | --- |
| 2. 提交记录 | `15-github-commit-history.png` | GitHub 10 次提交历史 |
| 2. 贡献统计 | `16-github-contributors.png` | Contributors 图：9 commits，100% |
| 2. PR | `17-github-pull-request.png` | Pull Request #1 已合并 |
| 2. Release | `18-github-release.png` | Release v1.0.0 |
| 4.1 空状态 | `01-empty-state.png` | 首次打开为空 |
| 4.1 新增 | `02-add-dialog.png` | 5 条联系方式的弹窗 |
| 4.1 保存后 | `03-created-contact.png` | 卡片出现 |
| 4.2 收藏 | `04-bookmarked.png` | 星标高亮并置顶 |
| 4.2 收藏筛选 | `05-favorites-filter.png` | 只看收藏 |
| 4.3 导入 | `06-import-result.png` | 导入 10 条后共 11 位 |
| 4.3 导出 | `07-export-toast.png` | 导出触发下载 |
| 4.3 导出内容 | `14-excel-content.png` | 导出文件内容 |
| 4.4 搜索 | `08-search-by-phone.png` | 手机号搜索命中 |
| 4.4 编辑 | `09-edit-dialog.png` | 回填全部联系方式 |
| 4.4 删除 | `10-delete-confirm.png` | 二次确认 |
| 4.5 表格 | `11-table-view.png` | 表格视图 |
| 4.5 持久化 | `12-persistence-after-reload.png` | 刷新后仍在 |
| 4.5 移动端 | `13-mobile-layout.png` | 窄屏布局 |

> 正文里的 `docs/address-book-demo.mp4` 链接指向仓库文件，CSDN 无法直接播放 mp4。
> 如果要展示视频，可以在 CSDN 编辑器里用「插入视频」上传该文件，或改成 Gitee/GitHub 上的链接说明。

---

## 四、发布与提交

1. 检查正文 4 个链接是否都能点开：
   - 仓库 `https://github.com/guiyu8799-pixel/832402228_addressbook`
   - 代码规范 `.../blob/main/codestyle.md`
   - Release `.../releases/tag/v1.0.0`
   - PR `.../pull/1`
2. 确认 18 张图都显示正常、没有裂图
3. 点「发布文章」
4. 复制发布后的网址，形如 `https://blog.csdn.net/你的用户名/article/details/数字`
5. 回到作业帖 <https://bbs.csdn.net/topics/620593436>，点「**提交任务**」，把博客网址填进去

> 提交框提示「可重复提交，以最后提交记录为准」，发布后再改博客可以重新提交覆盖。

---

## 五、提交前自查清单

- [ ] 博客开头有**课程信息说明表格**（课程、团队名称、作业要求、作业目标、其他参考）
- [ ] 有**目录**，且能跳转
- [ ] 有**项目地址**：GitHub 仓库、代码规范、Release、PR
- [ ] 有 **GitHub 提交历史截图**（≥ 3 次提交，实际 10 次）
- [ ] 有**贡献者统计截图**
- [ ] 有**功能实现思路**：收藏、多联系方式、Excel 导入导出、页面样式
- [ ] 有**程序截图 + 完整运行视频**
- [ ] 有 **PSP 表**（估计时间 + 实际时间）
- [ ] 有**困难与解决**记录
- [ ] 仓库里 `mvn test` 能跑通（31 个测试）

---

## 六、验收信息（回复助教时可用）

| 项目 | 地址 |
| --- | --- |
| 仓库 | <https://github.com/guiyu8799-pixel/832402228_addressbook> |
| 代码规范 | <https://github.com/guiyu8799-pixel/832402228_addressbook/blob/main/codestyle.md> |
| Release | <https://github.com/guiyu8799-pixel/832402228_addressbook/releases/tag/v1.0.0> |
| PR | <https://github.com/guiyu8799-pixel/832402228_addressbook/pull/1> |

本地运行（助教若要求现场演示）：

```text
mvn clean package
java -jar target/addressbook-1.0.0.jar
访问 http://localhost:8080/
```

测试：

```text
mvn test
Tests run: 31, Failures: 0, Errors: 0
```
