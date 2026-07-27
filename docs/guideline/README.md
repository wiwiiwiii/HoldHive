# HoldHive Guideline Docs

本目录只存放项目说明、课程参考、文档生成工具和最终交付物，不包含应用源代码。设计稿和图片素材已移至 `../design/`。

## 目录结构

```text
docs/guideline/
├── README.md                  # 本索引
├── project/                   # HoldHive 项目中文技术文档源文件
├── references/                # 课程要求、项目原始说明和 GenAI 使用规范
├── output/                    # 最终导出的 PDF
└── tools/                     # 文档生成工具

docs/design/
├── lanhu/                     # 蓝湖协作设计图、SVG/PNG 和生成脚本
└── assets/                    # 文档引用的图片素材
```

## 常用入口

- 最终 PDF：`output/HoldHive_项目完整设计与执行指南.pdf`
- 项目执行指南：`project/team_project_guideline_zh.md`
- 成员目录分工速查：`project/member_directory_map_zh.md`
- 技术栈定案：`project/technology_stack_zh.md`
- Git 分支与 CI：`project/git_branching_ci_zh.md`
- 实时股价 API：`project/market_data_api_zh.md`
- 数据库设计：`project/database_design_zh.md`
- REST API 文档：`project/api_documentation_zh.md`
- 蓝湖设计图说明：`../design/lanhu/README.md`

## 重新生成 PDF

在仓库根目录运行：

```bash
python3 docs/guideline/tools/build_holdhive_pdf.py
```

生成结果会覆盖：

```text
docs/guideline/output/HoldHive_项目完整设计与执行指南.pdf
```

## 维护规则

- 新增项目正文文档放入 `project/`。
- 新增图片、截图、Logo 引用素材放入 `../design/assets/`。
- 蓝湖导入、导出和源 SVG/PNG 统一放入 `../design/lanhu/`。
- 课程原始资料和外部规则放入 `references/`，不要混入项目正文。
- 最终交付物只放入 `output/`，不要在根目录堆放导出文件。
