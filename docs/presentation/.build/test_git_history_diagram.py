import subprocess
import sys
import tempfile
import unittest
import struct
import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
SCRIPT = REPO_ROOT / "docs" / "presentation" / ".build" / "generate_git_history_diagram.py"


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as image:
        signature = image.read(8)
        if signature != b"\x89PNG\r\n\x1a\n":
            raise AssertionError(f"{path} is not a PNG")
        length, chunk_type = struct.unpack(">I4s", image.read(8))
        if length != 13 or chunk_type != b"IHDR":
            raise AssertionError(f"{path} is missing an IHDR chunk")
        width, height = struct.unpack(">II", image.read(8))
    return width, height


class GitHistoryDiagramTest(unittest.TestCase):
    def test_generates_svg_and_png_from_current_git_history(self):
        head = subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=REPO_ROOT,
            text=True,
        ).strip()
        total = subprocess.check_output(
            ["git", "rev-list", "--count", "--all"],
            cwd=REPO_ROOT,
            text=True,
        ).strip()
        first_parent = subprocess.check_output(
            ["git", "rev-list", "--first-parent", "--count", "HEAD"],
            cwd=REPO_ROOT,
            text=True,
        ).strip()

        with tempfile.TemporaryDirectory() as tmp:
            out_dir = Path(tmp)
            subprocess.check_call(
                [sys.executable, str(SCRIPT), "--output-dir", str(out_dir)],
                cwd=REPO_ROOT,
            )

            png = out_dir / "git-history.png"
            svg = out_dir / "git-history.svg"

            self.assertTrue(png.exists())
            self.assertTrue(svg.exists())

            self.assertEqual(png_size(png), (1920, 1080))

            svg_text = svg.read_text(encoding="utf-8")
            self.assertIn(f"HEAD {head}", svg_text)
            self.assertIn(f"{total} commits", svg_text)
            self.assertIn(f"{first_parent} first-parent", svg_text)
            self.assertNotIn("Gitflow Workflow", svg_text)
            self.assertNotIn("Atlassian Git Tutorial", svg_text)
            self.assertIn(">Backend<", svg_text)
            self.assertIn("#52cf9d", svg_text)
            self.assertIn(">Frontend<", svg_text)
            self.assertIn("#f59ac8", svg_text)
            self.assertNotIn("#c9c9c9", svg_text)
            self.assertGreaterEqual(svg_text.count("data-branch-category="), 3)
            self.assertIn('data-crossing="allowed"', svg_text)
            self.assertGreaterEqual(svg_text.count("data-segment-label="), 3)
            self.assertRegex(svg_text, r'data-segment-label="[^"]+ #[0-9]+ / [0-9]+ commits"')
            self.assertNotIn('data-node-count="1"', svg_text)
            node_counts = [int(value) for value in re.findall(r'data-node-count="(\d+)"', svg_text)]
            self.assertTrue(node_counts)
            self.assertTrue(all(count >= 2 for count in node_counts))
            self.assertLessEqual(max(node_counts) - min(node_counts), 2)


if __name__ == "__main__":
    unittest.main()
