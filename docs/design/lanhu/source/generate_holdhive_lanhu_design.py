from __future__ import annotations

import base64
import json
import math
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "docs" / "design" / "lanhu"
SVG_DIR = OUT / "svg"
PNG_DIR = OUT / "png"
IMG_DIR = ROOT / "static" / "img"

W, H = 1440, 1024


def b64(path: Path) -> str:
    return base64.b64encode(path.read_bytes()).decode("ascii")


LOGOS = {
    "light_full": b64(IMG_DIR / "FullLogoWhite.png"),
    "dark_full": b64(IMG_DIR / "FullLogoBlack.png"),
    "light_mark": b64(IMG_DIR / "LogoWhite.png"),
    "dark_mark": b64(IMG_DIR / "LogoBlack.png"),
}


THEMES = {
    "light": {
        "name": "Day Mode",
        "bg": "#F6F7FB",
        "surface": "#FFFFFF",
        "surface2": "#FFF7E5",
        "text": "#172033",
        "muted": "#687086",
        "line": "#E1E5EE",
        "nav": "#FFFFFF",
        "navText": "#58627A",
        "honey": "#F6B33B",
        "honey2": "#FFD66B",
        "amber": "#D98B00",
        "green": "#0E8F67",
        "red": "#D94A56",
        "blue": "#2B7DE9",
        "purple": "#7557D6",
        "shadow": "rgba(23,32,51,.10)",
        "logo_full": LOGOS["light_full"],
        "logo_mark": LOGOS["light_mark"],
    },
    "dark": {
        "name": "Night Mode",
        "bg": "#08090C",
        "surface": "#12151D",
        "surface2": "#1B1710",
        "text": "#F7E8C8",
        "muted": "#A49C89",
        "line": "#28251F",
        "nav": "#0E1016",
        "navText": "#B8AD94",
        "honey": "#E9BD63",
        "honey2": "#FFD98A",
        "amber": "#F2A923",
        "green": "#4ED19A",
        "red": "#FF6F7B",
        "blue": "#6DA8FF",
        "purple": "#A78BFA",
        "shadow": "rgba(0,0,0,.40)",
        "logo_full": LOGOS["dark_full"],
        "logo_mark": LOGOS["dark_mark"],
    },
}


HOLDINGS = [
    ("AAPL", "Stock · Apple Inc.", "35", "$210.25", "$7,358.75", "+$1,216.25", "33.1%"),
    ("BTC", "Crypto · Bitcoin", "0.080", "$66,250.00", "$5,300.00", "+$620.00", "23.8%"),
    ("CASH", "Cash · USD Reserve", "-", "$1.00", "$4,500.00", "$0.00", "20.2%"),
    ("VOO", "ETF · Vanguard S&P 500", "6", "$510.40", "$3,062.40", "+$312.40", "13.8%"),
    ("TSLA", "Stock · Tesla Inc.", "8", "$248.90", "$1,991.20", "-$104.80", "9.0%"),
]

ALLOC = [
    ("Stocks", 42, "#2B7DE9"),
    ("Crypto", 24, "#4ED19A"),
    ("Cash", 20, "#F6B33B"),
    ("ETF", 14, "#7557D6"),
]


def esc(s: str) -> str:
    return (
        str(s)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def hexagon_points(cx: float, cy: float, r: float) -> str:
    pts = []
    for i in range(6):
        a = math.radians(60 * i - 30)
        pts.append(f"{cx + r * math.cos(a):.1f},{cy + r * math.sin(a):.1f}")
    return " ".join(pts)


def text(x, y, value, size=18, fill=None, weight=500, anchor="start", extra=""):
    return f'<text x="{x}" y="{y}" font-family="Inter, Arial, sans-serif" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" {extra}>{esc(value)}</text>'


def rect(x, y, w, h, fill, stroke="none", rx=24, shadow=False):
    flt = ' filter="url(#shadow)"' if shadow else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"{flt}/>'


def pill(x, y, w, h, label, theme, fill=None, stroke=None, color=None):
    fill = fill or theme["surface2"]
    stroke = stroke or theme["line"]
    color = color or theme["text"]
    return (
        rect(x, y, w, h, fill, stroke, rx=h / 2)
        + text(x + w / 2, y + h / 2 + 5, label, 14, color, 500, "middle")
    )


def honey_bg(theme):
    parts = []
    for row in range(-1, 10):
        for col in range(0, 14):
            x = 225 + col * 88 + (row % 2) * 44
            y = 78 + row * 76
            opacity = 0.07 if (row + col) % 3 == 0 else 0.035
            parts.append(
                f'<polygon points="{hexagon_points(x,y,32)}" fill="none" stroke="{theme["honey"]}" stroke-width="1.2" opacity="{opacity}"/>'
            )
    return "\n".join(parts)


def defs(theme):
    return f"""
<defs>
  <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
    <feDropShadow dx="0" dy="18" stdDeviation="20" flood-color="{theme['shadow']}" flood-opacity="1"/>
  </filter>
  <linearGradient id="honeyGrad" x1="0" x2="1" y1="0" y2="1">
    <stop offset="0" stop-color="{theme['honey2']}"/>
    <stop offset="1" stop-color="{theme['amber']}"/>
  </linearGradient>
  <linearGradient id="areaGrad" x1="0" x2="0" y1="0" y2="1">
    <stop offset="0" stop-color="{theme['blue']}" stop-opacity=".38"/>
    <stop offset="1" stop-color="{theme['blue']}" stop-opacity=".02"/>
  </linearGradient>
  <style>
    .portal-node {{ transition: transform .18s ease, opacity .18s ease; transform-box: fill-box; transform-origin: center; }}
    .portal:hover .portal-node {{ transform: scale(1.12); opacity: 1; }}
    .pulse-ring {{ animation: hivePulse 1.6s ease-out infinite; transform-box: fill-box; transform-origin: center; }}
    .pulse-ring.delay {{ animation-delay: .35s; }}
    @keyframes hivePulse {{
      0% {{ opacity: .62; transform: scale(.86); }}
      70% {{ opacity: .08; transform: scale(1.22); }}
      100% {{ opacity: 0; transform: scale(1.28); }}
    }}
  </style>
</defs>
"""


def icon_hex(x, y, theme, active=False):
    fill = "url(#honeyGrad)" if active else "transparent"
    stroke = theme["honey"] if active else theme["line"]
    inner = theme["surface"] if active else theme["navText"]
    return (
        f'<polygon points="{hexagon_points(x,y,18)}" fill="{fill}" stroke="{stroke}" stroke-width="2"/>'
        f'<circle cx="{x}" cy="{y}" r="4" fill="{inner}"/>'
    )


def shell(theme, page, subtitle="Portfolio workspace"):
    nav_items = ["Gateway", "Dashboard", "Holdings", "Performance", "Analysis", "Settings"]
    s = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        defs(theme),
        rect(0, 0, W, H, theme["bg"], rx=0),
        honey_bg(theme),
        rect(32, 28, 232, 968, theme["nav"], theme["line"], rx=34, shadow=True),
        f'<image x="58" y="46" width="152" height="64" href="data:image/png;base64,{theme["logo_full"]}" preserveAspectRatio="xMidYMid meet"/>',
        text(66, 138, subtitle, 13, theme["muted"], 500),
    ]
    y = 192
    for item in nav_items:
        active = item == page
        if active:
            s.append(rect(56, y - 24, 184, 52, theme["surface2"], theme["honey"], rx=18))
        s.append(icon_hex(82, y + 2, theme, active))
        s.append(text(112, y + 8, item, 16, theme["text"] if active else theme["navText"], 500))
        y += 66
    s.extend(
        [
            rect(56, 848, 184, 110, theme["surface2"], theme["line"], rx=24),
            text(76, 884, "Hive health", 14, theme["muted"], 500),
            text(76, 918, "API connected", 18, theme["green"], 500),
            text(76, 944, "Demo · 09:30", 12, theme["muted"], 400),
        ]
    )
    return s


def topbar(s, theme, title, caption, right="Day / Night", action="+ Add"):
    s += [
        text(300, 74, title, 34, theme["text"], 500),
        text(302, 104, caption, 15, theme["muted"], 400),
        pill(1150, 46, 122, 40, right, theme),
    ]
    if action:
        s += [
            rect(1290, 42, 92, 48, "url(#honeyGrad)", "none", rx=24),
            text(1336, 72, action, 15, theme["surface"], 500, "middle"),
        ]


def kpi_card(x, y, w, h, label, value, change, theme, color=None, hexmark=True):
    color = color or theme["green"]
    s = [rect(x, y, w, h, theme["surface"], theme["line"], rx=26, shadow=True)]
    if hexmark:
        s.append(f'<polygon points="{hexagon_points(x+34,y+34,18)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="1.5"/>')
    s += [
        text(x + 64, y + 38, label, 14, theme["muted"], 500),
        text(x + 24, y + 92, value, 30, theme["text"], 500),
        text(x + 24, y + 126, change, 14, color, 500),
    ]
    return "".join(s)


def donut(cx, cy, theme):
    # Simplified segmented ring using circles with dash arrays.
    circumference = 2 * math.pi * 86
    offset = 0
    s = [f'<circle cx="{cx}" cy="{cy}" r="86" fill="none" stroke="{theme["line"]}" stroke-width="38"/>']
    for name, pct, color in ALLOC:
        dash = circumference * pct / 100
        gap = circumference - dash
        s.append(
            f'<circle cx="{cx}" cy="{cy}" r="86" fill="none" stroke="{color}" stroke-width="38" stroke-dasharray="{dash:.1f} {gap:.1f}" stroke-dashoffset="{-offset:.1f}" transform="rotate(-90 {cx} {cy})" stroke-linecap="round"/>'
        )
        offset += dash
    s += [
        text(cx, cy - 4, "$22.2k", 26, theme["text"], 500, "middle"),
        text(cx, cy + 24, "priced value", 13, theme["muted"], 400, "middle"),
    ]
    return "".join(s)


def legend(x, y, theme):
    s = []
    for i, (name, pct, color) in enumerate(ALLOC):
        yy = y + i * 34
        s.append(f'<polygon points="{hexagon_points(x,yy-4,9)}" fill="{color}"/>')
        s.append(text(x + 22, yy + 1, name, 14, theme["text"], 500))
        s.append(text(x + 156, yy + 1, f"{pct}%", 14, theme["muted"], 500, "end"))
    return "".join(s)


def line_chart(x, y, w, h, theme, accent=None):
    accent = accent or theme["blue"]
    raw = [(0, 125), (70, 98), (140, 104), (210, 78), (280, 89), (350, 54), (420, 62), (490, 30), (560, 42)]
    sx = w / 560
    sy = h / 142
    pts = [(px * sx, py * sy) for px, py in raw]
    d = " ".join(f"{x+px},{y+py}" for px, py in pts)
    area = f"M{x},{y+h} L" + " L".join(f"{x+px},{y+py}" for px, py in pts) + f" L{x+w},{y+h} Z"
    s = [f'<path d="{area}" fill="url(#areaGrad)"/>']
    for gy in [0, h * .33, h * .66, h]:
        s.append(f'<line x1="{x}" y1="{y+gy}" x2="{x+w}" y2="{y+gy}" stroke="{theme["line"]}" stroke-width="1"/>')
    s.append(f'<polyline points="{d}" fill="none" stroke="{accent}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>')
    for px, py in pts[-3:]:
        s.append(f'<circle cx="{x+px}" cy="{y+py}" r="5" fill="{accent}" stroke="{theme["surface"]}" stroke-width="3"/>')
    return "".join(s)


def holdings_table(x, y, w, theme, compact=False):
    headers = ["Symbol", "Company", "Qty", "Price", "Market Value", "P/L", "Allocation"]
    col = [0, 132, 360, 480, 620, 800, 945]
    s = [rect(x, y, w, 48, theme["surface2"], theme["line"], rx=16)]
    for i, h in enumerate(headers):
        s.append(text(x + col[i] + 18, y + 31, h, 13, theme["muted"], 500))
    for r, row in enumerate(HOLDINGS[: 3 if compact else 5]):
        yy = y + 56 + r * 58
        s.append(rect(x, yy, w, 48, theme["surface"], theme["line"], rx=16))
        for i, cell in enumerate(row):
            color = theme["green"] if i == 5 and cell.startswith("+") else theme["red"] if i == 5 and cell.startswith("-") else theme["text"]
            s.append(text(x + col[i] + 18, yy + 31, cell, 14, color, 500 if i in [0, 5] else 400))
    return "".join(s)


def portal_vertex(cx: float, cy: float, r: float, index: int) -> tuple[float, float]:
    angle = math.radians(60 * index - 30)
    return cx + r * math.cos(angle), cy + r * math.sin(angle)


def portal_label_position(cx: float, cy: float, px: float, py: float) -> tuple[float, float, str]:
    dx = px - cx
    dy = py - cy
    x = px + (34 if dx >= 0 else -34)
    y = py + (12 if dy >= 0 else -12)
    anchor = "start" if dx >= 0 else "end"
    return x, y, anchor


def portal_node(cx, cy, theme, active=False, color=None):
    color = color or theme["honey"]
    fill = "url(#honeyGrad)" if active else theme["surface2"]
    stroke = theme["honey"] if active else color
    r = 44 if active else 34
    parts = ['<g class="portal">']
    if active:
        parts.append(f'<polygon class="pulse-ring" points="{hexagon_points(cx,cy,60)}" fill="none" stroke="{theme["honey"]}" stroke-width="3" opacity=".45"/>')
        parts.append(f'<polygon class="pulse-ring delay" points="{hexagon_points(cx,cy,74)}" fill="none" stroke="{theme["honey"]}" stroke-width="2" opacity=".28"/>')
    parts.append(f'<polygon class="portal-node" points="{hexagon_points(cx,cy,r)}" fill="{fill}" stroke="{stroke}" stroke-width="{3 if active else 2.2}"/>')
    parts.append(f'<circle cx="{cx}" cy="{cy}" r="{5 if active else 4}" fill="{theme["surface"] if active else color}"/>')
    parts.append("</g>")
    return "".join(parts)


def gateway(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Gateway")
    topbar(s, theme, "Hive Gateway", "Choose a workspace from the hive map", "Theme", action=None)
    cx, cy, radius = 842, 530, 205
    portals = [
        ("Dashboard", "Overview", theme["honey"]),
        ("Holdings", "Positions", theme["blue"]),
        ("Performance", "Returns", theme["green"]),
        ("Analysis", "Risk view", theme["purple"]),
        ("Add Holding", "New record", theme["amber"]),
        ("Settings", "Theme", theme["muted"]),
    ]
    s += [
        rect(302, 145, 1080, 780, theme["surface"], theme["line"], rx=38, shadow=True),
        f'<image x="346" y="188" width="128" height="128" href="data:image/png;base64,{theme["logo_mark"]}" preserveAspectRatio="xMidYMid meet"/>',
        text(514, 224, "Pick a workspace", 34, theme["text"], 500),
        text(516, 262, "Every vertex opens a focused part of the same portfolio.", 16, theme["muted"], 400),
        text(1172, 224, "Hover preview", 16, theme["honey"], 500, "end"),
        text(1172, 250, "scale + halo + label", 13, theme["muted"], 400, "end"),
        f'<polygon points="{hexagon_points(cx,cy,radius)}" fill="none" stroke="{theme["honey"]}" stroke-width="5" opacity=".82"/>',
        f'<polygon points="{hexagon_points(cx,cy,radius-68)}" fill="{theme["surface2"]}" stroke="{theme["line"]}" stroke-width="1.5" opacity=".76"/>',
        f'<polygon points="{hexagon_points(cx,cy,96)}" fill="none" stroke="{theme["honey"]}" stroke-width="1.2" opacity=".22"/>',
        f'<image x="{cx-48}" y="{cy-58}" width="96" height="96" href="data:image/png;base64,{theme["logo_mark"]}" preserveAspectRatio="xMidYMid meet"/>',
        text(cx, cy + 66, "HoldHive", 25, theme["text"], 500, "middle"),
        text(cx, cy + 92, "single portfolio context", 13, theme["muted"], 400, "middle"),
    ]
    vertex_points = []
    for i, (_, _, _) in enumerate(portals):
        px, py = portal_vertex(cx, cy, radius, i)
        vertex_points.append((px, py))
        s.append(f'<line x1="{cx}" y1="{cy}" x2="{px}" y2="{py}" stroke="{theme["line"]}" stroke-width="1.1" opacity=".74"/>')
    label_specs = {
        0: (58, -14, "start"),
        1: (44, 20, "start"),
        2: (54, 14, "start"),
        3: (-44, 20, "end"),
        4: (-44, -14, "end"),
        5: (42, -10, "start"),
    }
    for i, (label, caption, color) in enumerate(portals):
        px, py = vertex_points[i]
        active = label == "Dashboard"
        s.append(portal_node(px, py, theme, active, color))
        dx, dy, anchor = label_specs[i]
        lx, ly = px + dx, py + dy
        if active:
            width = 224
            bx = lx - 12
            s.append(rect(bx, ly - 38, width, 66, theme["surface"], theme["honey"], rx=22, shadow=True))
            s.append(text(lx + 8, ly - 8, label, 18, theme["text"], 500, "start"))
            s.append(text(lx + 8, ly + 15, "Open portfolio overview", 13, theme["muted"], 400, "start"))
        else:
            s.append(text(lx, ly - 4, label, 16, theme["text"], 500, anchor))
            s.append(text(lx, ly + 18, caption, 12, theme["muted"], 400, anchor))
    s += [
        rect(535, 825, 614, 72, theme["bg"], theme["line"], rx=24),
        text(568, 856, "Idle", 13, theme["muted"], 500),
        f'<polygon points="{hexagon_points(624,868,18)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="2"/>',
        f'<line x1="662" y1="868" x2="724" y2="868" stroke="{theme["line"]}" stroke-width="2"/>',
        text(748, 856, "Hover", 13, theme["muted"], 500),
        f'<polygon points="{hexagon_points(812,868,26)}" fill="url(#honeyGrad)" stroke="{theme["honey"]}" stroke-width="3"/>',
        f'<polygon points="{hexagon_points(812,868,37)}" fill="none" stroke="{theme["honey"]}" stroke-width="2" opacity=".38"/>',
        f'<line x1="856" y1="868" x2="918" y2="868" stroke="{theme["line"]}" stroke-width="2"/>',
        text(944, 856, "Click", 13, theme["muted"], 500),
        f'<polygon points="{hexagon_points(1016,868,24)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="3"/>',
        text(1050, 873, "Navigate", 15, theme["text"], 500),
    ]
    s.append("</svg>")
    return "\n".join(s)


def dashboard(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Dashboard")
    topbar(s, theme, "Dashboard", "Value, allocation, and data status")
    cards = [
        ("Total Value", "$22,212.35", "+10.1% vs cost", theme["green"]),
        ("Cost Basis", "$20,168.50", "5 priced assets", theme["blue"]),
        ("Unrealized P/L", "+$2,043.85", "multi-asset snapshot", theme["green"]),
        ("Data Mode", "DEMO", "fixed demo prices", theme["amber"]),
    ]
    for i, c in enumerate(cards):
        label, value, change, color = c
        s.append(kpi_card(300 + i * 270, 145, 246, 150, label, value, change, theme, color))
    s += [
        rect(300, 326, 500, 312, theme["surface"], theme["line"], rx=30, shadow=True),
        text(332, 374, "Asset Allocation", 22, theme["text"], 500),
        text(332, 400, "Distribution by priced market value", 14, theme["muted"], 400),
        donut(492, 505, theme),
        legend(650, 450, theme),
        rect(830, 326, 552, 312, theme["surface"], theme["line"], rx=30, shadow=True),
        text(862, 374, "Portfolio Performance", 22, theme["text"], 500),
        text(862, 400, "Snapshot value trend", 14, theme["muted"], 400),
        line_chart(876, 442, 456, 142, theme),
        rect(300, 668, 760, 282, theme["surface"], theme["line"], rx=30, shadow=True),
        text(332, 716, "Your Holdings", 22, theme["text"], 500),
        holdings_table(326, 742, 708, theme, compact=True),
        rect(1090, 668, 292, 282, theme["surface"], theme["line"], rx=30, shadow=True),
        text(1122, 716, "Portfolio Notes", 22, theme["text"], 500),
        f'<polygon points="{hexagon_points(1146,780,35)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="2"/>',
        text(1196, 770, "Multi-asset mix", 17, theme["text"], 500),
        text(1196, 796, "Stocks, crypto, ETF, cash", 14, theme["muted"], 400),
        f'<polygon points="{hexagon_points(1146,866,35)}" fill="{theme["surface2"]}" stroke="{theme["amber"]}" stroke-width="2"/>',
        text(1196, 856, "Demo data", 17, theme["text"], 500),
        text(1196, 882, "Fixed values for training", 14, theme["muted"], 400),
    ]
    s.append("</svg>")
    return "\n".join(s)


def holdings(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Holdings")
    topbar(s, theme, "Holdings", "Review positions and maintain records")
    s += [
        rect(300, 145, 1082, 116, theme["surface"], theme["line"], rx=30, shadow=True),
        text(332, 196, "Quick filters", 21, theme["text"], 500),
        pill(332, 212, 106, 36, "All", theme, theme["surface2"], theme["honey"]),
        pill(452, 212, 116, 36, "Stock", theme),
        pill(582, 212, 124, 36, "Crypto", theme),
        pill(720, 212, 106, 36, "Cash", theme),
        pill(840, 212, 106, 36, "ETF", theme),
        rect(300, 292, 1082, 560, theme["surface"], theme["line"], rx=30, shadow=True),
        text(332, 340, "Holding Ledger", 24, theme["text"], 500),
        holdings_table(326, 370, 1030, theme),
        rect(300, 884, 1082, 78, theme["surface2"], theme["honey"], rx=28),
        text(336, 928, "Conflict rule", 19, theme["text"], 500),
        text(520, 928, "Duplicate tickers show a clear message. Cost basis is never changed silently.", 15, theme["muted"], 400),
    ]
    s.append("</svg>")
    return "\n".join(s)


def performance(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Performance")
    topbar(s, theme, "Performance", "Snapshot trend and contribution")
    s += [
        rect(300, 145, 1082, 450, theme["surface"], theme["line"], rx=34, shadow=True),
        text(338, 198, "Portfolio Value Trend", 26, theme["text"], 500),
        text(338, 228, "Current-value trend for demo data", 15, theme["muted"], 400),
        line_chart(360, 290, 920, 220, theme),
        rect(300, 630, 340, 250, theme["surface"], theme["line"], rx=30, shadow=True),
        text(332, 680, "Best contributor", 20, theme["text"], 500),
        text(332, 730, "AAPL", 44, theme["green"], 500),
        text(332, 766, "+$1,216.25 unrealized", 16, theme["muted"], 400),
        rect(670, 630, 340, 250, theme["surface"], theme["line"], rx=30, shadow=True),
        text(702, 680, "Needs review", 20, theme["text"], 500),
        text(702, 730, "AMZN", 44, theme["red"], 500),
        text(702, 766, "-$143.00 unrealized", 16, theme["muted"], 400),
        rect(1040, 630, 342, 250, theme["surface"], theme["line"], rx=30, shadow=True),
        text(1072, 680, "Return basis", 20, theme["text"], 500),
        text(1072, 724, "Snapshot P/L", 28, theme["text"], 500),
        text(1072, 758, "Not time-weighted return", 16, theme["muted"], 400),
    ]
    s.append("</svg>")
    return "\n".join(s)


def analysis(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Analysis")
    topbar(s, theme, "Hive X-Ray", "Allocation, concentration, and data quality")
    s += [
        rect(300, 145, 516, 342, theme["surface"], theme["line"], rx=32, shadow=True),
        text(332, 198, "Allocation X-Ray", 24, theme["text"], 500),
        donut(466, 325, theme),
        legend(628, 282, theme),
        rect(846, 145, 536, 342, theme["surface"], theme["line"], rx=32, shadow=True),
        text(878, 198, "Concentration Check", 24, theme["text"], 500),
        f'<polygon points="{hexagon_points(1008,328,86)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="8"/>',
        text(1008, 318, "33.8%", 38, theme["text"], 500, "middle"),
        text(1008, 354, "largest holding", 15, theme["muted"], 400, "middle"),
        text(1124, 304, "Below 40% alert line", 17, theme["green"], 500),
        text(1124, 334, "Keep monitoring after new holdings.", 14, theme["muted"], 400),
        rect(300, 520, 1082, 380, theme["surface"], theme["line"], rx=32, shadow=True),
        text(332, 574, "Review Notes", 24, theme["text"], 500),
    ]
    for i, (title, body, color) in enumerate(
        [
            ("Price transparency", "2 holdings use demo values.", theme["amber"]),
            ("Diversification", "Cash reserve is 28.2% of priced value.", theme["blue"]),
            ("No advice", "Signals are explanatory only.", theme["purple"]),
        ]
    ):
        yy = 630 + i * 78
        s.append(f'<polygon points="{hexagon_points(358,yy,27)}" fill="{theme["surface2"]}" stroke="{color}" stroke-width="2"/>')
        s.append(text(408, yy - 6, title, 18, theme["text"], 500))
        s.append(text(408, yy + 20, body, 15, theme["muted"], 400))
    s.append("</svg>")
    return "\n".join(s)


def add_holding(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Holdings")
    topbar(s, theme, "Add Holding", "Create a holding from three required fields")
    s += [
        rect(300, 145, 612, 760, theme["surface"], theme["line"], rx=34, shadow=True),
        text(344, 204, "New holding", 28, theme["text"], 500),
        text(344, 236, "Asset type, symbol, quantity, and average price.", 16, theme["muted"], 400),
    ]
    fields = [
        ("Asset type", "Stock / ETF / Crypto / Cash", "Cash uses fixed value in base currency"),
        ("Symbol", "AAPL", "Uppercase automatically"),
        ("Quantity", "35", "Must be greater than 0"),
        ("Average purchase price", "175.50", "Use the same currency as quote"),
    ]
    for i, (label, value, hint) in enumerate(fields):
        yy = 292 + i * 104
        s += [
            text(344, yy, label, 15, theme["muted"], 500),
            rect(344, yy + 18, 502, 58, theme["bg"], theme["line"], rx=18),
            text(368, yy + 55, value, 20, theme["text"], 500),
            text(344, yy + 98, hint, 13, theme["muted"], 400),
        ]
    s += [
        rect(344, 720, 210, 60, "url(#honeyGrad)", "none", rx=30),
        text(449, 757, "Save holding", 17, theme["surface"], 500, "middle"),
        rect(574, 720, 150, 60, "transparent", theme["line"], rx=30),
        text(649, 757, "Cancel", 17, theme["text"], 500, "middle"),
        rect(950, 145, 432, 760, theme["surface"], theme["line"], rx=34, shadow=True),
        text(994, 204, "Live preview", 28, theme["text"], 500),
        f'<polygon points="{hexagon_points(1166,378,122)}" fill="{theme["surface2"]}" stroke="{theme["honey"]}" stroke-width="6"/>',
        text(1166, 354, "AAPL", 44, theme["text"], 500, "middle"),
        text(1166, 392, "35 shares", 17, theme["muted"], 400, "middle"),
        text(1030, 584, "Estimated cost basis", 16, theme["muted"], 400),
        text(1030, 626, "$6,142.50", 36, theme["text"], 500),
        text(1030, 680, "Input stays visible after errors.", 15, theme["muted"], 400),
    ]
    s.append("</svg>")
    return "\n".join(s)


def settings(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Settings")
    topbar(s, theme, "Settings", "Theme and data preferences")
    s += [
        rect(300, 145, 1082, 250, theme["surface"], theme["line"], rx=34, shadow=True),
        text(338, 202, "Theme system", 26, theme["text"], 500),
        text(338, 236, "Logo variants switch with the theme.", 16, theme["muted"], 400),
        f'<image x="340" y="270" width="210" height="86" href="data:image/png;base64,{THEMES["light"]["logo_full"]}" preserveAspectRatio="xMidYMid meet"/>',
        f'<image x="600" y="270" width="210" height="86" href="data:image/png;base64,{THEMES["dark"]["logo_full"]}" preserveAspectRatio="xMidYMid meet"/>',
        pill(880, 272, 120, 42, "Day", theme, theme["surface2"], theme["honey"]),
        pill(1020, 272, 120, 42, "Night", theme),
        rect(300, 430, 520, 360, theme["surface"], theme["line"], rx=34, shadow=True),
        text(338, 486, "Data mode", 24, theme["text"], 500),
        pill(338, 530, 150, 42, "Demo", theme, theme["surface2"], theme["honey"]),
        pill(506, 530, 150, 42, "Live", theme),
        pill(674, 530, 118, 42, "Cached", theme),
        text(338, 624, "Demo mode is always labelled.", 16, theme["muted"], 400),
        rect(862, 430, 520, 360, theme["surface"], theme["line"], rx=34, shadow=True),
        text(900, 486, "Motion preferences", 24, theme["text"], 500),
        text(900, 538, "Cards: 160ms fade/slide", 17, theme["text"], 500),
        text(900, 582, "Charts: 600ms ease-out", 17, theme["text"], 500),
        text(900, 626, "Reduced motion: disable non-essential transitions", 17, theme["text"], 500),
    ]
    s.append("</svg>")
    return "\n".join(s)


def error_state(theme_key):
    theme = THEMES[theme_key]
    s = shell(theme, "Dashboard")
    topbar(s, theme, "Recoverable States", "Empty, partial, failed, and confirmation states")
    states = [
        ("Empty hive", "Add your first holding.", "Create a portfolio snapshot.", "Add first holding", theme["honey"]),
        ("Partial valuation", "Some holdings have no price.", "Totals use priced holdings only.", "Review unpriced", theme["amber"]),
        ("Price service failed", "Saved holdings are safe.", "Retry price refresh when ready.", "Retry", theme["red"]),
        ("Delete confirmation", "Remove TSLA from the hive?", "Totals update immediately.", "Confirm delete", theme["purple"]),
    ]
    for i, (title, body, detail, action, color) in enumerate(states):
        x = 300 + (i % 2) * 556
        y = 150 + (i // 2) * 360
        s += [
            rect(x, y, 526, 306, theme["surface"], theme["line"], rx=34, shadow=True),
            f'<polygon points="{hexagon_points(x+80,y+86,48)}" fill="{theme["surface2"]}" stroke="{color}" stroke-width="4"/>',
            text(x + 146, y + 76, title, 26, theme["text"], 500),
            text(x + 146, y + 112, body, 16, theme["muted"], 400),
            text(x + 146, y + 140, detail, 15, theme["muted"], 400),
            rect(x + 146, y + 202, 170, 52, "url(#honeyGrad)", "none", rx=26),
            text(x + 231, y + 235, action, 15, theme["surface"], 500, "middle"),
        ]
    s.append("</svg>")
    return "\n".join(s)


PAGES = {
    "01-gateway": gateway,
    "02-dashboard": dashboard,
    "03-holdings": holdings,
    "04-performance": performance,
    "05-analysis": analysis,
    "06-add-holding": add_holding,
    "07-settings": settings,
    "08-states": error_state,
}


def export_one(name: str, theme_key: str, svg: str):
    svg_path = SVG_DIR / f"{name}-{theme_key}.svg"
    png_path = PNG_DIR / f"{name}-{theme_key}.png"
    svg_path.write_text(svg, encoding="utf-8")
    return svg_path, png_path, W, H


def render_pngs(pairs: list[tuple[Path, Path, int, int]]):
    tasks = [
        {"svg": str(svg.resolve()), "png": str(png.resolve()), "width": width, "height": height}
        for svg, png, width, height in pairs
    ]
    manifest = OUT / "source" / "render_manifest.json"
    manifest.write_text(json.dumps(tasks, ensure_ascii=False, indent=2), encoding="utf-8")
    renderer = OUT / "source" / "render_svgs_to_pngs.js"
    renderer.write_text(
        """
const fs = require('fs');
const { chromium } = require('../../../tmp/lanhu-render/node_modules/playwright');

(async () => {
  const manifest = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1024 }, deviceScaleFactor: 1 });
  for (const task of manifest) {
    await page.setViewportSize({ width: task.width, height: task.height });
    await page.goto('file://' + task.svg);
    await page.screenshot({ path: task.png, fullPage: false });
  }
  await browser.close();
})();
""".strip()
        + "\n",
        encoding="utf-8",
    )
    subprocess.run(["node", str(renderer), str(manifest)], check=True, cwd=ROOT)


def contact_sheet():
    thumb_w, thumb_h = 360, 256
    gap = 28
    sheet_w = thumb_w * 4 + gap * 5
    sheet_h = thumb_h * 4 + gap * 5 + 80
    s = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{sheet_w}" height="{sheet_h}" viewBox="0 0 {sheet_w} {sheet_h}">',
        '<rect width="100%" height="100%" fill="#F4F1E8"/>',
        text(32, 52, "HoldHive Lanhu Design Boards", 30, "#172033", 500),
        text(32, 78, "8 pages x day/night theme · upload PNG files to Lanhu, keep SVG/source for incremental edits", 14, "#687086", 400),
    ]
    files = sorted(PNG_DIR.glob("*.png"))
    for idx, file in enumerate(files):
        col = idx % 4
        row = idx // 4
        x = gap + col * (thumb_w + gap)
        y = 112 + row * (thumb_h + gap)
        data = b64(file)
        s.append(rect(x - 2, y - 2, thumb_w + 4, thumb_h + 34, "#FFFFFF", "#E1E5EE", rx=18, shadow=False))
        s.append(f'<image x="{x}" y="{y}" width="{thumb_w}" height="{thumb_h}" href="data:image/png;base64,{data}" preserveAspectRatio="xMidYMid slice"/>')
        s.append(text(x + 12, y + thumb_h + 24, file.stem, 13, "#172033", 500))
    s.append("</svg>")
    svg = "\n".join(s)
    p = OUT / "HoldHive_Lanhu_Design_Overview.svg"
    p.write_text(svg, encoding="utf-8")
    return p, OUT / "HoldHive_Lanhu_Design_Overview.png", sheet_w, sheet_h


def write_readme():
    readme = """# HoldHive 蓝湖协作设计稿

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
"""
    (OUT / "README.md").write_text(readme, encoding="utf-8")


def main():
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    PNG_DIR.mkdir(parents=True, exist_ok=True)
    pairs = []
    for name, fn in PAGES.items():
        for theme_key in THEMES:
            pairs.append(export_one(name, theme_key, fn(theme_key)))
    render_pngs(pairs)
    overview_pair = contact_sheet()
    render_pngs([overview_pair])
    write_readme()
    print(f"Exported {len(PAGES) * len(THEMES)} design boards to {OUT}")


if __name__ == "__main__":
    main()
