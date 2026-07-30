# HoldHive Docs

项目文档按用途拆分为四个入口：

```text
docs/
├── guideline/   # 项目计划、技术栈、API、数据库、Git/CI、课程参考和最终 PDF
├── design/      # 蓝湖设计图、界面素材和设计生成脚本
├── demo/        # 演示脚本和本地 walkthrough
├── presentation/# 终期展示素材和来源说明
└── qa/          # 验收清单、API 测试用例、缺陷记录和前端交接说明
```

当前交付状态：

- `qa` 是日常集成分支；文档、修复和后续功能仍从 `qa` 开分支并通过 PR 合回 `qa`。
- `main` 已同步 `qa` 并作为 `1.0.0` 发布快照。
- `1.0.0` 标签指向当前正式交付版本；不要为普通文档修订移动该标签。

常用入口：

- 技术文档索引：`guideline/README.md`
- 最终 PDF：`guideline/output/HoldHive_项目完整设计与执行指南.pdf`
- 蓝湖设计说明：`design/lanhu/README.md`
- QA 验收与接口测试：`qa/README.md`
- 本地演示脚本：`demo/demo-script.md`
