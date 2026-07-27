# HoldHive 蓝湖协作设计稿

## 文件说明

- `png/`：可直接上传蓝湖的页面设计图，尺寸均为 `1440 x 1024`。
- `svg/`：同名可编辑矢量源稿，适合后续增量修改。
- `source/generate_holdhive_lanhu_design.py`：生成脚本，主题色、页面数据、组件均集中在代码中。
- `HoldHive_Lanhu_Design_Overview.png`：全部画板总览图。

## 主题与 Logo

- 日间主题使用浅色页面、蜂蜜金强调色，并配合 `FullLogoWhite.png` / `LogoWhite.png`。
- 夜间主题使用黑金页面、低亮度蜂巢背景，并配合 `FullLogoBlack.png` / `LogoBlack.png`。
- 两套主题只切换颜色变量与 logo 资源，不改变页面结构，便于前端用 CSS variables 实现。

## 页面清单

1. `01-gateway`：单个蜂巢六边形门户页，六个入口固定在六边形六个顶点上。
2. `02-dashboard`：组合总览，参考 Empower/Yahoo Finance 的首屏 KPI 和总览信息。
3. `03-holdings`：持仓列表和维护页，参考 Yahoo Finance/TradingView 的 holdings 视图。
4. `04-performance`：组合表现页，参考 Sharesight/TradingView 的趋势视图。
5. `05-analysis`：Hive X-Ray 分析页，参考 Morningstar X-Ray 的风险拆解方式。
6. `06-add-holding`：添加持仓表单页。
7. `07-settings`：日/夜主题、数据模式和动效设置页。
8. `08-states`：空状态、部分估值、价格失败、删除确认等友好状态页。

## Gateway 动效标注

- 主入口不是列表，而是一个中心六边形：Dashboard、Holdings、Performance、Analysis、Add Holding、Settings 分别落在六个顶点。
- 默认态：顶点为描边六边形，中心点表示可点击入口。
- Hover 态：目标顶点放大到约 110%，出现蜂蜜金填充、外圈 halo 和 tooltip。
- Click 态：沿当前入口导航到对应页面；前端实现时使用 `transition: transform 160ms ease, opacity 160ms ease`。
- PNG 是静态交付图，SVG 中保留了 `:hover` 与 `hivePulse` CSS 标注，方便设计和开发理解动效。

## 蓝湖上传建议

- 首次上传：上传 `png/` 下全部 16 张页面图，并用页面名前缀排序。
- 标注交互：Gateway 页面使用静态 hover 示意条补充鼠标悬停、点击导航和 reduce-motion 行为。
- 增量修改：只重新上传改动页面对应的 PNG；SVG 和源脚本保留在仓库中作为可追溯源稿。
- 切图资产：优先复用 `static/img` 的四个 logo，不从设计图里二次截取低清图片。

## 增量修改方式

新增页面或改页面数据时，优先修改 `source/generate_holdhive_lanhu_design.py`：

- 改颜色：修改 `THEMES`。
- 改持仓示例：修改 `HOLDINGS`。
- 改图表分类：修改 `ALLOC`。
- 改单个页面：修改对应函数，例如 `dashboard()` 或 `analysis()`。

修改后运行：

```bash
python3 docs/design/lanhu/source/generate_holdhive_lanhu_design.py
```

脚本会覆盖生成 `svg/` 和 `png/` 中的设计稿，方便重新上传蓝湖。
