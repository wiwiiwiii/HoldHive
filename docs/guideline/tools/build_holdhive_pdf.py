from __future__ import annotations

import re
from datetime import date
from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image as RLImage,
    KeepTogether,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
    XPreformatted,
)
from reportlab.platypus.tableofcontents import TableOfContents


ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / "docs" / "guideline" / "output" / "HoldHive_项目完整设计与执行指南.pdf"

SOURCES = [
    ("docs/guideline/project/team_project_guideline_zh.md", "chapter-guideline"),
    ("docs/guideline/project/member_directory_map_zh.md", "chapter-member-map"),
    ("docs/design/lanhu/README.md", "chapter-lanhu"),
    ("docs/guideline/project/technology_stack_zh.md", "chapter-tech"),
    ("docs/guideline/project/git_branching_ci_zh.md", "chapter-git-ci"),
    ("docs/guideline/project/market_data_api_zh.md", "chapter-market-data"),
    ("docs/guideline/project/database_design_zh.md", "chapter-database"),
    ("docs/guideline/project/api_documentation_zh.md", "chapter-api"),
]

LOCAL_LINKS = {
    "./technology_stack_zh.md": "chapter-tech",
    "./member_directory_map_zh.md": "chapter-member-map",
    "./git_branching_ci_zh.md": "chapter-git-ci",
    "./market_data_api_zh.md": "chapter-market-data",
    "./database_design_zh.md": "chapter-database",
    "./api_documentation_zh.md": "chapter-api",
}

DESIGN_BOARDS = [
    ("01-gateway-dark.png", "Gateway 夜间主题 - 单个六边形门户，六个顶点入口和 hover 示意"),
    ("01-gateway-light.png", "Gateway 日间主题 - 同结构主题适配"),
    ("02-dashboard-dark.png", "Dashboard 夜间主题 - KPI、配置图、趋势和持仓摘要"),
    ("02-dashboard-light.png", "Dashboard 日间主题 - 首屏组合总览"),
    ("03-holdings-dark.png", "Holdings 夜间主题 - 持仓表和筛选"),
    ("03-holdings-light.png", "Holdings 日间主题 - 记录维护"),
    ("04-performance-dark.png", "Performance 夜间主题 - 价值趋势和贡献提示"),
    ("04-performance-light.png", "Performance 日间主题 - 表现视图"),
    ("05-analysis-dark.png", "Analysis 夜间主题 - 配置和集中度检查"),
    ("05-analysis-light.png", "Analysis 日间主题 - X-Ray 分析"),
    ("06-add-holding-dark.png", "Add Holding 夜间主题 - 表单和实时预览"),
    ("06-add-holding-light.png", "Add Holding 日间主题 - 新增持仓流程"),
    ("07-settings-dark.png", "Settings 夜间主题 - 主题、数据模式和动效偏好"),
    ("07-settings-light.png", "Settings 日间主题 - 主题切换配置"),
    ("08-states-dark.png", "States 夜间主题 - 空态、部分估值、失败和确认"),
    ("08-states-light.png", "States 日间主题 - 用户友好状态"),
]

pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))

PAGE_WIDTH, PAGE_HEIGHT = A4
MARGIN_X = 18 * mm
MARGIN_TOP = 18 * mm
MARGIN_BOTTOM = 17 * mm
CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_X

BRAND = colors.HexColor("#F2B134")
INK = colors.HexColor("#18212B")
MUTED = colors.HexColor("#667085")
LIGHT = colors.HexColor("#F6F8FA")
LINE = colors.HexColor("#D9E0E7")
GREEN = colors.HexColor("#1F7A5A")


def build_styles():
    base = getSampleStyleSheet()
    return {
        "body": ParagraphStyle(
            "BodyCN",
            parent=base["BodyText"],
            fontName="STSong-Light",
            fontSize=9.2,
            leading=14.5,
            textColor=INK,
            spaceAfter=5,
            wordWrap="CJK",
        ),
        "h1": ParagraphStyle(
            "Heading1",
            parent=base["Heading1"],
            fontName="STSong-Light",
            fontSize=20,
            leading=27,
            textColor=INK,
            spaceBefore=4,
            spaceAfter=12,
            keepWithNext=True,
            wordWrap="CJK",
        ),
        "h2": ParagraphStyle(
            "Heading2",
            parent=base["Heading2"],
            fontName="STSong-Light",
            fontSize=14,
            leading=20,
            textColor=colors.HexColor("#25364A"),
            spaceBefore=12,
            spaceAfter=7,
            keepWithNext=True,
            wordWrap="CJK",
        ),
        "h3": ParagraphStyle(
            "Heading3",
            parent=base["Heading3"],
            fontName="STSong-Light",
            fontSize=11.2,
            leading=17,
            textColor=GREEN,
            spaceBefore=9,
            spaceAfter=5,
            keepWithNext=True,
            wordWrap="CJK",
        ),
        "h4": ParagraphStyle(
            "Heading4",
            parent=base["Heading4"],
            fontName="STSong-Light",
            fontSize=10,
            leading=15,
            textColor=colors.HexColor("#334A60"),
            spaceBefore=7,
            spaceAfter=4,
            keepWithNext=True,
            wordWrap="CJK",
        ),
        "bullet": ParagraphStyle(
            "BulletCN",
            fontName="STSong-Light",
            fontSize=9,
            leading=14,
            leftIndent=13,
            firstLineIndent=-9,
            textColor=INK,
            spaceAfter=2.5,
            wordWrap="CJK",
        ),
        "quote": ParagraphStyle(
            "QuoteCN",
            fontName="STSong-Light",
            fontSize=9.2,
            leading=14.5,
            leftIndent=10,
            rightIndent=8,
            borderColor=BRAND,
            borderWidth=2,
            borderPadding=(3, 7, 3, 8),
            backColor=colors.HexColor("#FFF9E8"),
            textColor=colors.HexColor("#4B5563"),
            spaceAfter=7,
            wordWrap="CJK",
        ),
        "code": ParagraphStyle(
            "CodeCN",
            fontName="STSong-Light",
            fontSize=7.4,
            leading=10.5,
            leftIndent=7,
            rightIndent=7,
            borderColor=LINE,
            borderWidth=0.5,
            borderPadding=7,
            backColor=LIGHT,
            textColor=colors.HexColor("#25364A"),
            spaceBefore=3,
            spaceAfter=7,
            wordWrap="CJK",
        ),
        "table": ParagraphStyle(
            "TableCN",
            fontName="STSong-Light",
            fontSize=7.2,
            leading=10.2,
            textColor=INK,
            wordWrap="CJK",
        ),
        "table_header": ParagraphStyle(
            "TableHeaderCN",
            fontName="STSong-Light",
            fontSize=7.4,
            leading=10.5,
            textColor=colors.white,
            wordWrap="CJK",
        ),
        "toc_title": ParagraphStyle(
            "TocTitleCN",
            fontName="STSong-Light",
            fontSize=22,
            leading=28,
            textColor=INK,
            spaceAfter=14,
        ),
        "cover_title": ParagraphStyle(
            "CoverTitleCN",
            fontName="STSong-Light",
            fontSize=31,
            leading=42,
            alignment=TA_CENTER,
            textColor=INK,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitleCN",
            fontName="STSong-Light",
            fontSize=14,
            leading=22,
            alignment=TA_CENTER,
            textColor=MUTED,
        ),
        "cover_meta": ParagraphStyle(
            "CoverMetaCN",
            fontName="STSong-Light",
            fontSize=10,
            leading=16,
            alignment=TA_CENTER,
            textColor=MUTED,
        ),
    }


STYLES = build_styles()


def inline_markup(text: str) -> str:
    pattern = re.compile(r"\[([^\]]+)\]\(([^)]+)\)|`([^`]+)`|\*\*([^*]+)\*\*")
    out = []
    pos = 0
    for match in pattern.finditer(text):
        out.append(escape(text[pos : match.start()]))
        label, href, code, bold = match.groups()
        if label is not None:
            safe_label = escape(label)
            if href in LOCAL_LINKS:
                out.append(f'<link href="#{LOCAL_LINKS[href]}" color="#1F6F8B">{safe_label}</link>')
            else:
                out.append(f'<a href="{escape(href)}" color="#1F6F8B">{safe_label}</a>')
        elif code is not None:
            out.append(f'<font name="STSong-Light" color="#8B3A3A">{escape(code)}</font>')
        else:
            out.append(f"<b>{escape(bold)}</b>")
        pos = match.end()
    out.append(escape(text[pos:]))
    return "".join(out)


def split_table_row(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def is_table_separator(line: str) -> bool:
    cells = split_table_row(line)
    return bool(cells) and all(re.fullmatch(r":?-{3,}:?", cell) for cell in cells)


def make_table(rows: list[list[str]]):
    col_count = max(len(row) for row in rows)
    normalized = [row + [""] * (col_count - len(row)) for row in rows]
    data = []
    for row_index, row in enumerate(normalized):
        style = STYLES["table_header"] if row_index == 0 else STYLES["table"]
        data.append([Paragraph(inline_markup(cell), style) for cell in row])

    if col_count == 2:
        widths = [CONTENT_WIDTH * 0.28, CONTENT_WIDTH * 0.72]
    elif col_count == 3:
        widths = [CONTENT_WIDTH * 0.20, CONTENT_WIDTH * 0.35, CONTENT_WIDTH * 0.45]
    elif col_count == 4:
        widths = [CONTENT_WIDTH * 0.15, CONTENT_WIDTH * 0.24, CONTENT_WIDTH * 0.31, CONTENT_WIDTH * 0.30]
    else:
        widths = [CONTENT_WIDTH / col_count] * col_count

    table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT", splitByRow=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#334A60")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("GRID", (0, 0), (-1, -1), 0.35, LINE),
                ("BACKGROUND", (0, 1), (-1, -1), colors.white),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    return table


def markdown_to_flowables(path: Path, root_anchor: str) -> list:
    lines = path.read_text(encoding="utf-8").splitlines()
    story = []
    paragraph_lines: list[str] = []
    in_code = False
    code_lines: list[str] = []
    heading_index = 0

    def flush_paragraph():
        nonlocal paragraph_lines
        if paragraph_lines:
            text = " ".join(part.strip() for part in paragraph_lines)
            story.append(Paragraph(inline_markup(text), STYLES["body"]))
            paragraph_lines = []

    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("```"):
            flush_paragraph()
            if in_code:
                code_text = "\n".join(code_lines) or " "
                story.append(Preformatted(code_text, STYLES["code"], maxLineLength=96))
                code_lines = []
                in_code = False
            else:
                in_code = True
            i += 1
            continue

        if in_code:
            code_lines.append(line)
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < len(lines) and is_table_separator(lines[i + 1]):
            flush_paragraph()
            table_rows = [split_table_row(line)]
            i += 2
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_rows.append(split_table_row(lines[i]))
                i += 1
            story.append(make_table(table_rows))
            story.append(Spacer(1, 6))
            continue

        heading = re.match(r"^(#{1,4})\s+(.+)$", stripped)
        if heading:
            flush_paragraph()
            level = len(heading.group(1))
            title = heading.group(2)
            heading_index += 1
            anchor = root_anchor if level == 1 else f"{root_anchor}-h{heading_index}"
            p = Paragraph(inline_markup(title), STYLES[f"h{level}"])
            p._bookmarkName = anchor
            story.append(p)
            i += 1
            continue

        if stripped.startswith("> "):
            flush_paragraph()
            story.append(Paragraph(inline_markup(stripped[2:]), STYLES["quote"]))
            i += 1
            continue

        bullet = re.match(r"^[-*]\s+(.+)$", stripped)
        numbered = re.match(r"^(\d+)\.\s+(.+)$", stripped)
        if bullet or numbered:
            flush_paragraph()
            if bullet:
                marker, text = "•", bullet.group(1)
            else:
                marker, text = f"{numbered.group(1)}.", numbered.group(2)
            story.append(Paragraph(f"{marker} {inline_markup(text)}", STYLES["bullet"]))
            i += 1
            continue

        if not stripped:
            flush_paragraph()
            i += 1
            continue

        paragraph_lines.append(stripped)
        i += 1

    flush_paragraph()
    return story


def design_board_flowables() -> list:
    story = []
    png_dir = ROOT / "docs" / "design" / "lanhu" / "png"
    for index, (filename, caption) in enumerate(DESIGN_BOARDS, start=1):
        board_path = png_dir / filename
        if not board_path.exists():
            continue
        if index != 1:
            story.append(PageBreak())
        title = f"蓝湖画板 {index:02d}：{caption}"
        heading = Paragraph(inline_markup(title), STYLES["h2"])
        heading._bookmarkName = f"chapter-lanhu-board-{index:02d}"
        story.append(heading)
        story.append(Paragraph(inline_markup(f"文件：`docs/design/lanhu/png/{filename}`。上传蓝湖时保留文件名前缀排序。"), STYLES["body"]))
        image = RLImage(str(board_path))
        image.drawWidth = CONTENT_WIDTH
        image.drawHeight = CONTENT_WIDTH * 1024 / 1440
        story.append(image)
        story.append(Spacer(1, 6))
    return story


class HoldHiveDocTemplate(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph):
            return
        style_name = flowable.style.name
        if style_name not in {"Heading1", "Heading2", "Heading3", "Heading4"}:
            return
        level = {"Heading1": 0, "Heading2": 1, "Heading3": 2, "Heading4": 2}[style_name]
        text = flowable.getPlainText()
        key = getattr(flowable, "_bookmarkName", f"heading-{self.page}-{id(flowable)}")
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(text, key, level=level, closed=level > 0)
        self.notify("TOCEntry", (level, text, self.page, key))


def draw_content_page(canvas, doc):
    canvas.saveState()
    canvas.setTitle("HoldHive 项目完整设计与执行指南")
    canvas.setAuthor("HoldHive Team")
    canvas.setFont("STSong-Light", 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(MARGIN_X, PAGE_HEIGHT - 10 * mm, "HoldHive · 项目完整设计与执行指南")
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.4)
    canvas.line(MARGIN_X, PAGE_HEIGHT - 12 * mm, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 12 * mm)
    canvas.drawCentredString(PAGE_WIDTH / 2, 9 * mm, str(doc.page))
    canvas.restoreState()


def draw_cover(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(BRAND)
    canvas.rect(0, PAGE_HEIGHT - 12 * mm, PAGE_WIDTH, 12 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.HexColor("#FFF9E8"))
    canvas.circle(PAGE_WIDTH / 2, PAGE_HEIGHT * 0.70, 22 * mm, stroke=0, fill=1)
    canvas.setFillColor(BRAND)
    canvas.circle(PAGE_WIDTH / 2, PAGE_HEIGHT * 0.70, 9 * mm, stroke=0, fill=1)
    canvas.restoreState()


def build_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    cover_frame = Frame(MARGIN_X, MARGIN_BOTTOM, CONTENT_WIDTH, PAGE_HEIGHT - MARGIN_BOTTOM - 14 * mm, id="cover")
    content_frame = Frame(
        MARGIN_X,
        MARGIN_BOTTOM,
        CONTENT_WIDTH,
        PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM,
        id="content",
        topPadding=2 * mm,
    )

    doc = HoldHiveDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        leftMargin=MARGIN_X,
        rightMargin=MARGIN_X,
        topMargin=MARGIN_TOP,
        bottomMargin=MARGIN_BOTTOM,
        title="HoldHive 项目完整设计与执行指南",
        author="HoldHive Team",
    )
    doc.addPageTemplates(
        [
            PageTemplate(id="cover", frames=[cover_frame], onPage=draw_cover),
            PageTemplate(id="content", frames=[content_frame], onPage=draw_content_page),
        ]
    )

    story = [
        Spacer(1, 68 * mm),
        Paragraph("HoldHive", STYLES["cover_title"]),
        Spacer(1, 5 * mm),
        Paragraph("项目完整设计与执行指南", STYLES["cover_subtitle"]),
        Spacer(1, 24 * mm),
        Paragraph("产品故事 · 竞品研究 · 技术栈 · 数据库 · REST API · 四人执行计划", STYLES["cover_meta"]),
        Spacer(1, 6 * mm),
        Paragraph("两天规划 + 两天编码", STYLES["cover_meta"]),
        Spacer(1, 30 * mm),
        Paragraph(f"文档日期：{date.today().isoformat()}", STYLES["cover_meta"]),
        NextPageTemplate("content"),
        PageBreak(),
        Paragraph("目录", STYLES["toc_title"]),
    ]

    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            "TOC1",
            fontName="STSong-Light",
            fontSize=10.5,
            leading=16,
            leftIndent=0,
            firstLineIndent=0,
            textColor=INK,
            spaceBefore=4,
        ),
        ParagraphStyle(
            "TOC2",
            fontName="STSong-Light",
            fontSize=8.7,
            leading=13,
            leftIndent=12,
            firstLineIndent=0,
            textColor=colors.HexColor("#334A60"),
        ),
        ParagraphStyle(
            "TOC3",
            fontName="STSong-Light",
            fontSize=7.8,
            leading=11.5,
            leftIndent=25,
            firstLineIndent=0,
            textColor=MUTED,
        ),
    ]
    story.extend([toc, PageBreak()])

    for index, (relative_path, anchor) in enumerate(SOURCES):
        if index:
            story.append(PageBreak())
        story.extend(markdown_to_flowables(ROOT / relative_path, anchor))
        if relative_path == "docs/design/lanhu/README.md":
            story.append(PageBreak())
            story.extend(design_board_flowables())

    doc.multiBuild(story)
    print(OUTPUT)


if __name__ == "__main__":
    build_pdf()
