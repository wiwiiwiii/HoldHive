#!/usr/bin/env python3
"""Generate the HoldHive Git history diagram for presentation assets."""

from __future__ import annotations

import argparse
import html
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


try:
    from PIL import Image, ImageDraw, ImageFont
except ModuleNotFoundError:
    bundled_python = (
        Path.home()
        / ".cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3"
    )
    if bundled_python.exists() and Path(sys.executable).resolve() != bundled_python.resolve():
        os.execv(str(bundled_python), [str(bundled_python), *sys.argv])
    raise SystemExit(
        "Pillow is required to generate git-history.png. "
        "Install Pillow or run with the Codex bundled Python runtime."
    )


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[2]
DEFAULT_OUTPUT_DIR = REPO_ROOT / "docs/presentation/assets"

WIDTH = 1920
HEIGHT = 1080
LEFT = 88
RIGHT = 1832
MAIN_Y = 395
MAX_EVENTS = 18
MIN_BRANCH_NODES = 2

COLORS = {
    "bg": "#f7f7ff",
    "dark": "#3f3f42",
    "text": "#333333",
    "muted": "#66666b",
    "main": "#aee1fb",
    "backend": "#52cf9d",
    "frontend": "#f59ac8",
    "analysis": "#a983e6",
    "fix": "#ff7b5f",
    "qa": "#4ec8cc",
    "note_border": "#bdbdc2",
}

LANE_ORDER = ["main", "backend", "frontend", "analysis", "fix", "qa"]
LANE_LABELS = {
    "main": "Main",
    "backend": "Backend",
    "frontend": "Frontend",
    "analysis": "Analysis",
    "fix": "Fix",
    "qa": "QA",
}
LANE_Y = {
    "backend": 535,
    "frontend": 690,
    "analysis": 930,
    "fix": 930,
    "qa": 930,
}
PR_RE = re.compile(r"#(\d+)")


@dataclass(frozen=True)
class Commit:
    full: str
    short: str
    parents: list[str]
    subject: str


@dataclass(frozen=True)
class Event:
    short: str
    subject: str
    category: str
    pr: str | None
    count: int


@dataclass(frozen=True)
class BranchSegment:
    start: int
    end: int
    category: str
    pr: str | None
    count: int


def run_git(repo_root: Path, *args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=repo_root, text=True).strip()


def git_summary(repo_root: Path) -> dict[str, str]:
    return {
        "branch": run_git(repo_root, "branch", "--show-current") or "HEAD",
        "head": run_git(repo_root, "rev-parse", "--short", "HEAD"),
        "total": run_git(repo_root, "rev-list", "--count", "--all"),
        "first_parent": run_git(repo_root, "rev-list", "--first-parent", "--count", "HEAD"),
    }


def load_first_parent(repo_root: Path) -> list[Commit]:
    output = run_git(
        repo_root,
        "log",
        "--first-parent",
        "--reverse",
        "--format=%H%x1f%h%x1f%P%x1f%s",
        "HEAD",
    )
    commits: list[Commit] = []
    for line in output.splitlines():
        full, short, parents, subject = line.split("\x1f", 3)
        commits.append(Commit(full=full, short=short, parents=parents.split(), subject=subject))
    return commits


def branch_subjects(repo_root: Path, commit: Commit) -> list[str]:
    if len(commit.parents) < 2:
        return []
    output = run_git(
        repo_root,
        "log",
        "--reverse",
        "--no-merges",
        "--format=%s",
        f"{commit.parents[0]}..{commit.parents[1]}",
    )
    return [line for line in output.splitlines() if line.strip()]


def select_indices(total: int, max_events: int = MAX_EVENTS) -> list[int]:
    if total <= max_events:
        return list(range(total))

    selected = {0, total - 1}
    for step in range(1, max_events - 1):
        selected.add(round(step * (total - 1) / (max_events - 1)))

    candidate = 0
    while len(selected) < max_events:
        selected.add(candidate)
        candidate += 1
    return sorted(selected)


def classify_text(text: str) -> str:
    lower = text.lower()
    if any(word in lower for word in ("frontend", "react", "layout", "addholdings", "dashboard")):
        return "frontend"
    if any(word in lower for word in ("analysis", "insight", "sse", "eastmoney", "narrative")):
        return "analysis"
    if any(word in lower for word in ("fix", "revert", "perf", "conflict", "hotfix")):
        return "fix"
    if any(
        word in lower
        for word in (
            "backend",
            "api",
            "market",
            "holding",
            "portfolio",
            "pricing",
            "database",
            "flyway",
            "seed",
            "quote",
            "instrument",
            "cash",
            "fund",
        )
    ):
        return "backend"
    if any(word in lower for word in ("test", "qa", "ci", "docs", "build", "presentation", "chore")):
        return "qa"
    return "main"


def classify_subjects(subjects: list[str]) -> str:
    counts = {key: 0 for key in ("backend", "frontend", "analysis", "fix", "qa")}
    for subject in subjects:
        category = classify_text(subject)
        if category in counts:
            counts[category] += 1

    if not any(counts.values()):
        return "main"

    priority = {"backend": 4, "frontend": 4, "analysis": 3, "fix": 2, "qa": 1}
    return max(counts, key=lambda category: (counts[category], priority[category]))


def pr_label(subjects: list[str]) -> str | None:
    for subject in reversed(subjects):
        match = PR_RE.search(subject)
        if match:
            return f"#{match.group(1)}"
    return None


def build_events(repo_root: Path, first_parent: list[Commit]) -> list[Event]:
    indices = select_indices(len(first_parent))
    events: list[Event] = []
    previous_index = indices[0]

    first = first_parent[previous_index]
    events.append(
        Event(short=first.short, subject=first.subject, category="main", pr=None, count=1)
    )

    for index in indices[1:]:
        chunk = first_parent[previous_index + 1 : index + 1]
        subjects = [commit.subject for commit in chunk]
        branch_count = 0
        for commit in chunk:
            merged_subjects = branch_subjects(repo_root, commit)
            branch_count += len(merged_subjects)
            subjects.extend(merged_subjects)

        current = first_parent[index]
        events.append(
            Event(
                short=current.short,
                subject=current.subject,
                category=classify_subjects(subjects),
                pr=pr_label(subjects),
                count=max(1, len(chunk) + branch_count),
            )
        )
        previous_index = index

    return events


def chunk_intervals(interval_count: int) -> list[tuple[int, int]]:
    segment_count = min(5, max(3, round(interval_count / 3.5)))
    base = interval_count // segment_count
    remainder = interval_count % segment_count

    chunks: list[tuple[int, int]] = []
    start = 1
    for index in range(segment_count):
        size = base + (1 if index < remainder else 0)
        end = start + size - 1
        chunks.append((start, end))
        start = end + 1
    return chunks


def segment_category(events: list[Event], start: int, end: int, previous: str | None) -> str:
    counts = {key: 0 for key in ("backend", "frontend", "analysis", "fix", "qa")}
    for event in events[start : end + 1]:
        if event.category in counts:
            counts[event.category] += max(1, event.count)

    ranked = sorted(
        counts,
        key=lambda category: (counts[category], category != previous, category != "qa"),
        reverse=True,
    )
    for category in ranked:
        if counts[category] and category != previous:
            return category
    for category in ranked:
        if counts[category]:
            return category
    return "qa"


def build_branch_segments(events: list[Event]) -> list[BranchSegment]:
    segments: list[BranchSegment] = []
    previous: str | None = None
    for start, end in chunk_intervals(len(events) - 1):
        category = segment_category(events, start, end, previous)
        chunk = events[start : end + 1]
        label = next((event.pr for event in reversed(chunk) if event.pr), None)
        count = sum(event.count for event in chunk)
        segments.append(
            BranchSegment(start=start, end=end, category=category, pr=label, count=count)
        )
        previous = category
    return segments


def crossing_segment_indexes(segments: list[BranchSegment]) -> set[int]:
    if len(segments) < 3:
        return set()
    indexes = {len(segments) - 1}
    if len(segments) >= 5:
        indexes.add(2)
    return indexes


def load_font(name: str, size: int) -> ImageFont.ImageFont:
    candidates = [
        f"/System/Library/Fonts/Supplemental/{name}.ttf",
        f"/Library/Fonts/{name}.ttf",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


class Diagram:
    def __init__(self) -> None:
        self.image = Image.new("RGB", (WIDTH, HEIGHT), COLORS["bg"])
        self.draw = ImageDraw.Draw(self.image)
        self.svg: list[str] = [
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">',
            f'<rect width="{WIDTH}" height="{HEIGHT}" fill="{COLORS["bg"]}"/>',
        ]
        self.font_box = load_font("Arial", 34)
        self.font_tag = load_font("Arial", 23)
        self.font_note = load_font("Arial", 18)
        self.font_footer = load_font("Arial", 17)

    def finish(self) -> str:
        self.svg.append("</svg>")
        return "\n".join(self.svg)

    def text_center(self, cx: float, cy: float, text: str, font: ImageFont.ImageFont, fill: str) -> None:
        bbox = self.draw.textbbox((0, 0), text, font=font)
        width = bbox[2] - bbox[0]
        height = bbox[3] - bbox[1]
        self.draw.text((cx - width / 2, cy - height / 2 - 2), text, font=font, fill=fill)

    def svg_text(self, cx: float, cy: float, text: str, size: int, fill: str) -> None:
        self.svg.append(
            f'<text x="{cx:.1f}" y="{cy:.1f}" text-anchor="middle" dominant-baseline="middle" '
            f'font-family="Arial" font-size="{size}" fill="{fill}">{html.escape(text)}</text>'
        )

    def lane_box(self, x: float, y: float, w: float, h: float, text: str, fill: str) -> None:
        self.draw.rectangle((x, y, x + w, y + h), fill=fill, outline=COLORS["dark"], width=7)
        self.text_center(x + w / 2, y + h / 2, text, self.font_box, COLORS["text"])
        self.svg.append(
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" '
            f'fill="{fill}" stroke="{COLORS["dark"]}" stroke-width="7"/>'
        )
        self.svg_text(x + w / 2, y + h / 2 + 1, text, 34, COLORS["text"])

    def cubic(
        self,
        p0: tuple[float, float],
        p1: tuple[float, float],
        p2: tuple[float, float],
        p3: tuple[float, float],
        steps: int = 36,
    ) -> list[tuple[float, float]]:
        points: list[tuple[float, float]] = []
        for i in range(steps + 1):
            t = i / steps
            x = (
                (1 - t) ** 3 * p0[0]
                + 3 * (1 - t) ** 2 * t * p1[0]
                + 3 * (1 - t) * t * t * p2[0]
                + t**3 * p3[0]
            )
            y = (
                (1 - t) ** 3 * p0[1]
                + 3 * (1 - t) ** 2 * t * p1[1]
                + 3 * (1 - t) * t * t * p2[1]
                + t**3 * p3[1]
            )
            points.append((x, y))
        return points

    def path(self, segments: list[tuple], width: int = 8, color: str = COLORS["dark"]) -> None:
        points: list[tuple[float, float]] = []
        svg_path: list[str] = []
        for segment in segments:
            command = segment[0]
            if command == "M":
                point = segment[1]
                points = [point]
                svg_path.append(f"M {point[0]:.1f} {point[1]:.1f}")
            elif command == "L":
                point = segment[1]
                points.append(point)
                svg_path.append(f"L {point[0]:.1f} {point[1]:.1f}")
            elif command == "C":
                c1, c2, point = segment[1], segment[2], segment[3]
                points.extend(self.cubic(points[-1], c1, c2, point)[1:])
                svg_path.append(
                    f"C {c1[0]:.1f} {c1[1]:.1f}, {c2[0]:.1f} {c2[1]:.1f}, {point[0]:.1f} {point[1]:.1f}"
                )

        self.draw.line(points, fill=color, width=width, joint="curve")
        radius = width / 2
        for x, y in (points[0], points[-1]):
            self.draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)
        self.svg.append(
            f'<path d="{" ".join(svg_path)}" fill="none" stroke="{color}" '
            f'stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"/>'
        )

    def circle(self, x: float, y: float, fill: str, radius: int, stroke_width: int = 6) -> None:
        self.draw.ellipse(
            (x - radius, y - radius, x + radius, y + radius),
            fill=fill,
            outline=COLORS["dark"],
            width=stroke_width,
        )
        self.svg.append(
            f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{radius}" fill="{fill}" '
            f'stroke="{COLORS["dark"]}" stroke-width="{stroke_width}"/>'
        )

    def tag(self, cx: float, y: float, text: str) -> None:
        bbox = self.draw.textbbox((0, 0), text, font=self.font_tag)
        w = max(70, bbox[2] - bbox[0] + 30)
        h = 44
        x = max(28, min(WIDTH - 28 - w, cx - w / 2))
        tag_cx = x + w / 2
        self.draw.rectangle((x, y, x + w, y + h), fill=COLORS["main"], outline=COLORS["dark"], width=6)
        self.text_center(tag_cx, y + h / 2, text, self.font_tag, COLORS["text"])
        arrow_top = y + h + 16
        arrow_tip = MAIN_Y - 30
        self.draw.line((tag_cx, arrow_top, tag_cx, arrow_tip), fill=COLORS["dark"], width=6)
        self.draw.line((tag_cx, arrow_tip, tag_cx - 15, arrow_tip - 18), fill=COLORS["dark"], width=6)
        self.draw.line((tag_cx, arrow_tip, tag_cx + 15, arrow_tip - 18), fill=COLORS["dark"], width=6)
        self.svg.append(
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h}" fill="{COLORS["main"]}" '
            f'stroke="{COLORS["dark"]}" stroke-width="6"/>'
        )
        self.svg_text(tag_cx, y + h / 2 + 1, text, 23, COLORS["text"])
        self.svg.append(
            f'<path d="M {tag_cx:.1f} {arrow_top:.1f} L {tag_cx:.1f} {arrow_tip:.1f} '
            f'M {tag_cx:.1f} {arrow_tip:.1f} L {tag_cx - 15:.1f} {arrow_tip - 18:.1f} '
            f'M {tag_cx:.1f} {arrow_tip:.1f} L {tag_cx + 15:.1f} {arrow_tip - 18:.1f}" '
            f'fill="none" stroke="{COLORS["dark"]}" stroke-width="6" stroke-linecap="round"/>'
        )

    def segment_label(self, cx: float, cy: float, text: str) -> None:
        bbox = self.draw.textbbox((0, 0), text, font=self.font_note)
        w = bbox[2] - bbox[0] + 24
        h = 34
        x = max(28, min(WIDTH - 28 - w, cx - w / 2))
        y = cy - h / 2
        label_cx = x + w / 2
        self.draw.rounded_rectangle(
            (x, y, x + w, y + h),
            radius=6,
            fill=COLORS["bg"],
            outline=COLORS["note_border"],
            width=2,
        )
        self.text_center(label_cx, cy, text, self.font_note, COLORS["text"])
        self.svg.append(
            f'<g data-segment-label="{html.escape(text)}">'
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h}" rx="6" '
            f'fill="{COLORS["bg"]}" stroke="{COLORS["note_border"]}" stroke-width="2"/>'
        )
        self.svg_text(label_cx, cy + 1, text, 18, COLORS["text"])
        self.svg.append("</g>")

    def footer(self, text: str) -> None:
        bbox = self.draw.textbbox((0, 0), text, font=self.font_footer)
        x = WIDTH - (bbox[2] - bbox[0]) - 34
        y = HEIGHT - 39
        self.draw.text((x, y), text, font=self.font_footer, fill=COLORS["muted"])
        self.svg.append(
            f'<text x="{WIDTH - 34}" y="{HEIGHT - 25}" text-anchor="end" font-family="Arial" '
            f'font-size="17" fill="{COLORS["muted"]}">{html.escape(text)}</text>'
        )


def event_x_positions(count: int) -> list[float]:
    if count == 1:
        return [(LEFT + RIGHT) / 2]
    step = (RIGHT - LEFT) / (count - 1)
    return [LEFT + i * step for i in range(count)]


def branch_path(x0: float, x1: float, y: float) -> list[tuple]:
    dx = x1 - x0
    enter = x0 + dx * 0.28
    leave = x0 + dx * 0.72
    return [
        ("M", (x0, MAIN_Y)),
        ("C", (x0 + dx * 0.14, MAIN_Y), (x0 + dx * 0.14, y), (enter, y)),
        ("L", (leave, y)),
        ("C", (x0 + dx * 0.86, y), (x0 + dx * 0.86, MAIN_Y), (x1, MAIN_Y)),
    ]


def branch_node_positions(x0: float, x1: float, y: float, count: int) -> list[tuple[float, float]]:
    dx = x1 - x0
    if count <= 4 or dx < 220:
        factors = [0.42, 0.58]
    elif count <= 8 or dx < 320:
        factors = [0.36, 0.5, 0.64]
    else:
        factors = [0.34, 0.45, 0.56, 0.67]
    return [(x0 + dx * factor, y) for factor in factors]


def choose_tags(segments: list[BranchSegment], xs: list[float]) -> list[tuple[float, str]]:
    return [(xs[segment.end], segment.pr) for segment in segments if segment.pr]


def segment_info(segment: BranchSegment) -> str:
    label = segment.pr or "no PR"
    return f"{LANE_LABELS[segment.category]} {label} / {segment.count} commits"


def segment_label_y(y: float) -> float:
    if y >= 850:
        return y - 58
    return y + 56


def render(repo_root: Path, output_dir: Path) -> None:
    summary = git_summary(repo_root)
    events = build_events(repo_root, load_first_parent(repo_root))
    segments = build_branch_segments(events)
    xs = event_x_positions(len(events))
    diagram = Diagram()

    box_y = 30
    box_w = 220
    box_h = 92
    gap = (WIDTH - 2 * 58 - box_w * len(LANE_ORDER)) / (len(LANE_ORDER) - 1)
    for i, lane in enumerate(LANE_ORDER):
        x = 58 + i * (box_w + gap)
        diagram.lane_box(x, box_y, box_w, box_h, LANE_LABELS[lane], COLORS[lane])

    diagram.path([("M", (xs[0], MAIN_Y)), ("L", (xs[-1], MAIN_Y))], width=9)

    crossing_indexes = crossing_segment_indexes(segments)
    for index, segment in enumerate(segments):
        crosses_previous = index in crossing_indexes and index > 0
        x0_index = max(0, segment.start - 2) if crosses_previous else segment.start - 1
        x0 = xs[x0_index]
        x1 = xs[segment.end]
        y = LANE_Y[segment.category]
        node_positions = branch_node_positions(x0, x1, y, segment.count)
        crossing_attr = ' data-crossing="allowed"' if crosses_previous else ""
        diagram.svg.append(
            f'<g data-branch-category="{segment.category}" data-start-index="{segment.start}" '
            f'data-draw-start-index="{x0_index}" data-end-index="{segment.end}" '
            f'data-node-count="{len(node_positions)}"{crossing_attr}>'
        )
        diagram.path(branch_path(x0, x1, y), width=8)
        for x, node_y in node_positions:
            diagram.circle(x, node_y, COLORS[segment.category], radius=23, stroke_width=6)
        diagram.svg.append("</g>")
        diagram.segment_label((x0 + x1) / 2, segment_label_y(y), segment_info(segment))

    tags = choose_tags(segments, xs)
    tag_xs = {round(x) for x, _ in tags}
    for i, (x, event) in enumerate(zip(xs, events)):
        radius = 25 if i in {0, len(events) - 1} or round(x) in tag_xs else 18
        diagram.circle(x, MAIN_Y, COLORS["main"], radius=radius, stroke_width=7 if radius > 20 else 5)

    for x, label in tags:
        diagram.tag(x, 184, label)

    metadata = (
        f"HEAD {summary['head']} | {summary['total']} commits / "
        f"{summary['first_parent']} first-parent / {summary['branch']}"
    )
    diagram.svg.insert(2, f"<metadata>{html.escape(metadata)}</metadata>")
    diagram.footer(metadata)

    output_dir.mkdir(parents=True, exist_ok=True)
    for existing in output_dir.glob("git-history*"):
        if existing.name not in {"git-history.png", "git-history.svg"}:
            existing.unlink()
    (output_dir / "git-history.svg").write_text(diagram.finish(), encoding="utf-8")
    diagram.image.save(output_dir / "git-history.png", quality=96)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=REPO_ROOT)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_dir = args.output_dir.resolve()
    render(args.repo_root.resolve(), output_dir)
    print(f"Generated {output_dir / 'git-history.png'}")
    print(f"Generated {output_dir / 'git-history.svg'}")


if __name__ == "__main__":
    main()
