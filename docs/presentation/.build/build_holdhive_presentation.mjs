import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../../..");
const OUT_DIR = path.join(ROOT, "docs/presentation");
const PREVIEW_DIR = path.join(OUT_DIR, "previews");
const FINAL_PPTX = path.join(OUT_DIR, "HoldHive_Final_Project_Presentation.pptx");
const MONTAGE = path.join(PREVIEW_DIR, "deck-montage.webp");

const W = 1280;
const H = 720;
const C = {
  bg: "#090A0D",
  bg2: "#11141B",
  panel: "#151922",
  panel2: "#1A1710",
  cream: "#090A0D",
  cream2: "#F7E6BD",
  ink: "#F7E6BD",
  muted: "#B9A77A",
  gold: "#F5B83B",
  gold2: "#D9971A",
  blue: "#C98A1B",
  green: "#E6B24D",
  red: "#A96A12",
  purple: "#8C621D",
  slate: "#F2D89B",
  border: "#5A4219",
  white: "#FFF4D3",
  soft: "#D9C690",
  dim: "#8F7A4D",
  line: "#3E3218",
  well: "#221B0B",
};

const ASSETS = {
  logoDark: path.join(ROOT, "static/img/FullLogoBlack.png"),
  boardOverview: path.join(ROOT, "docs/design/lanhu/HoldHive_Lanhu_Design_Overview.png"),
  gateway: path.join(ROOT, "docs/design/lanhu/png/01-gateway-dark.png"),
  dashboard: path.join(ROOT, "docs/design/lanhu/png/02-dashboard-dark.png"),
  holdings: path.join(ROOT, "docs/design/lanhu/png/03-holdings-dark.png"),
  analysis: path.join(ROOT, "docs/design/lanhu/png/05-analysis-dark.png"),
  states: path.join(ROOT, "docs/design/lanhu/png/08-states-dark.png"),
};

async function writeBlob(filePath, blob) {
  await fs.writeFile(filePath, new Uint8Array(await blob.arrayBuffer()));
}

async function imageBytes(filePath) {
  return fs.readFile(filePath);
}

function addText(slide, text, x, y, w, h, opts = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    name: opts.name,
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    typeface: opts.typeface ?? "Aptos",
    fontSize: opts.size ?? 24,
    bold: opts.bold ?? false,
    color: opts.color ?? C.ink,
    alignment: opts.align ?? "left",
    lineSpacing: opts.lineSpacing ?? 1.08,
  };
  return shape;
}

function addRect(slide, x, y, w, h, opts = {}) {
  return slide.shapes.add({
    geometry: opts.geometry ?? "roundRect",
    name: opts.name,
    position: { left: x, top: y, width: w, height: h },
    fill: opts.fill ?? C.panel,
    line: opts.line ?? { style: "solid", fill: opts.stroke ?? C.border, width: opts.strokeWidth ?? 1 },
    borderRadius: opts.radius ?? "rounded-xl",
    shadow: opts.shadow ?? "shadow-none",
  });
}

function addPill(slide, text, x, y, w, color = C.gold, dark = true) {
  addRect(slide, x, y, w, 32, {
    fill: C.well,
    stroke: color,
    strokeWidth: 1,
    radius: "rounded-full",
  });
  addText(slide, text, x + 12, y + 6, w - 24, 20, {
    size: 12,
    bold: true,
    color: dark ? C.gold : "#9A6400",
    align: "center",
  });
}

function hex(slide, x, y, size, opts = {}) {
  return slide.shapes.add({
    geometry: "hexagon",
    position: { left: x, top: y, width: size, height: size },
    fill: opts.fill ?? "none",
    line: { style: "solid", fill: opts.stroke ?? C.gold, width: opts.width ?? 2 },
  });
}

function addSlideTitle(slide, title, kicker, opts = {}) {
  const dark = opts.dark ?? false;
  addText(slide, kicker ?? "HOLDHIVE FINAL PROJECT", 64, 38, 520, 24, {
    size: 13,
    bold: true,
    color: dark ? C.gold : C.gold2,
  });
  addText(slide, title, 64, 70, 1000, 58, {
    size: 42,
    bold: true,
    color: dark ? C.white : C.ink,
  });
}

function addFooter(slide, page, dark = false) {
  addText(slide, "HoldHive | Final Project Presentation", 64, 674, 400, 20, {
    size: 11,
    color: C.dim,
  });
  addText(slide, String(page).padStart(2, "0"), 1186, 672, 32, 20, {
    size: 12,
    bold: true,
    color: dark ? C.gold : C.gold2,
    align: "right",
  });
}

function addNotes(slide, speaker, sources) {
  const sourceBlock = sources.map((s) => `- ${s}`).join("\n");
  slide.speakerNotes.textFrame.setText(`${speaker}\n\n[Sources]\n${sourceBlock}`);
  slide.speakerNotes.setVisible(true);
}

function sourcePath(rel) {
  return rel;
}

function agendaLine(slide, idx, label, detail, y) {
  addRect(slide, 92, y, 54, 54, { fill: C.well, stroke: C.gold, radius: "rounded-lg" });
  addText(slide, idx, 108, y + 12, 22, 24, { size: 18, bold: true, color: C.gold2, align: "center" });
  addText(slide, label, 170, y + 2, 330, 30, { size: 25, bold: true, color: C.ink });
  addText(slide, detail, 170, y + 36, 850, 24, { size: 17, color: C.muted });
}

function bullet(slide, text, x, y, w, opts = {}) {
  hex(slide, x, y + 5, 18, { stroke: opts.hexColor ?? C.gold, width: 2 });
  addText(slide, text, x + 32, y, w, opts.h ?? 48, {
    size: opts.size ?? 20,
    bold: opts.bold ?? false,
    color: opts.color ?? C.ink,
    lineSpacing: 1.12,
  });
}

function metricCard(slide, label, value, detail, x, y, w, opts = {}) {
  addRect(slide, x, y, w, 142, {
    fill: opts.fill ?? C.panel,
    stroke: opts.stroke ?? C.border,
    shadow: "shadow-md",
  });
  hex(slide, x + 24, y + 24, 34, { stroke: opts.accent ?? C.gold, width: 2 });
  addText(slide, label, x + 72, y + 20, w - 90, 28, { size: 16, color: opts.labelColor ?? C.muted });
  addText(slide, value, x + 24, y + 62, w - 48, 38, { size: 31, bold: true, color: opts.valueColor ?? C.ink });
  addText(slide, detail, x + 24, y + 104, w - 48, 24, { size: 15, bold: true, color: opts.detailColor ?? C.green });
}

async function addImage(slide, filePath, x, y, w, h, opts = {}) {
  const bytes = await imageBytes(filePath);
  slide.images.add({
    blob: bytes,
    contentType: "image/png",
    alt: opts.alt ?? path.basename(filePath),
    fit: opts.fit ?? "cover",
    position: { left: x, top: y, width: w, height: h },
    geometry: opts.geometry ?? "rect",
    borderRadius: opts.radius,
    crop: opts.crop,
  });
}

function addLine(slide, x1, y1, x2, y2, opts = {}) {
  return slide.shapes.add({
    geometry: "line",
    position: {
      left: Math.min(x1, x2),
      top: Math.min(y1, y2),
      width: Math.abs(x2 - x1),
      height: Math.abs(y2 - y1),
    },
    fill: "none",
    line: {
      style: opts.style ?? "solid",
      fill: opts.color ?? C.gold,
      width: opts.width ?? 2,
    },
  });
}

async function buildDeck() {
  await fs.mkdir(OUT_DIR, { recursive: true });
  await fs.rm(PREVIEW_DIR, { recursive: true, force: true });
  await fs.mkdir(PREVIEW_DIR, { recursive: true });

  const p = Presentation.create({ slideSize: { width: W, height: H } });

  // 1. Title
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    hex(slide, -78, 88, 220, { stroke: C.line, width: 2 });
    hex(slide, 1120, 472, 190, { stroke: C.line, width: 2 });
    await addImage(slide, ASSETS.logoDark, 91, 78, 490, 208, { fit: "contain", alt: "HoldHive logo" });
    addText(slide, "One hive for scattered holdings", 96, 344, 940, 88, {
      size: 44,
      bold: true,
      color: C.white,
    });
    addText(slide, "A product and process story, not just a feature list", 101, 454, 780, 34, {
      size: 22,
      color: C.soft,
    });
    addPill(slide, "Final Project Presentation", 98, 512, 220, C.gold, true);
    addPill(slide, "15-20 min team story", 336, 512, 202, C.gold, true);
    addText(slide, "2026-07-28", 102, 625, 210, 24, { size: 14, color: C.dim });
    addFooter(slide, 1, true);
    addNotes(
      slide,
      "Opening beat: set the expectation that this deck tells how the team turned a broad portfolio-management assignment into a focused, explainable MVP. Do not start by listing features.",
      [
        sourcePath("README.md"),
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
      ],
    );
  }

  // 2. Presentation contract
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "The rules changed what we chose to show", "PRESENTATION CONTRACT");
    const rules = [
      ["One idea", "per slide"],
      ["Story", "over feature list"],
      ["Git history", "as evidence"],
      ["Mistakes", "included"],
    ];
    rules.forEach(([a, b], i) => {
      const x = 92 + i * 270;
      addRect(slide, x, 170, 214, 148, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      hex(slide, x + 76, 196, 60, { stroke: C.gold2, width: 3 });
      addText(slide, a, x + 28, 272, 158, 26, { size: 25, bold: true, color: C.ink, align: "center" });
      addText(slide, b, x + 28, 304, 158, 20, { size: 16, color: C.muted, align: "center" });
    });
    addRect(slide, 158, 432, 820, 84, { fill: C.panel, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "We are being judged on the process as much as the result.", 206, 456, 724, 30, {
      size: 28,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addText(slide, "So this deck follows the build story: product decisions -> implementation -> recovery -> learning.", 222, 496, 690, 20, {
      size: 15,
      color: C.soft,
      align: "center",
    });
    addFooter(slide, 2);
    addNotes(
      slide,
      "This slide explicitly ties the deck to the required presentation style: light slides, one key idea, visuals over dense text, and a story that includes mistakes. It also sets up why Git, bugs, and team learning are included.",
      [
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
        sourcePath("docs/guideline/references/portfolio_manager.md"),
      ],
    );
  }

  // 3. Customer problem
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "Alex had prices, but not a portfolio view", "CUSTOMER PROBLEM", { dark: true });
    addText(slide, "Our customer story starts with a beginner investor holding assets across several apps.", 76, 158, 640, 72, {
      size: 25,
      color: C.white,
      lineSpacing: 1.18,
    });
    const labels = [
      ["Stocks", "single prices"],
      ["ETF", "overlap risk"],
      ["Mutual fund", "lagging data"],
      ["Crypto", "demo/cache"],
      ["Cash", "fixed value"],
      ["Bank deposit", "principal view"],
    ];
    const positions = [
      [760, 150],
      [930, 180],
      [1030, 310],
      [900, 460],
      [700, 445],
      [650, 275],
    ];
    hex(slide, 820, 285, 170, { stroke: C.gold, width: 3 });
    addText(slide, "?", 878, 330, 52, 62, { size: 54, bold: true, color: C.gold, align: "center" });
    labels.forEach(([a, b], i) => {
      const [x, y] = positions[i];
      addRect(slide, x, y, 150, 70, { fill: C.panel, stroke: C.line, radius: "rounded-lg" });
      addText(slide, a, x + 14, y + 13, 124, 22, { size: 17, bold: true, color: C.white, align: "center" });
      addText(slide, b, x + 14, y + 40, 124, 18, { size: 12, color: C.soft, align: "center" });
    });
    bullet(slide, "He could see individual prices.", 88, 270, 590, { color: C.soft });
    bullet(slide, "He could not answer the whole-portfolio questions.", 88, 356, 590, { color: C.soft });
    bullet(slide, "HoldHive became the one-minute snapshot.", 88, 442, 590, { color: C.white, bold: true });
    addFooter(slide, 3, true);
    addNotes(
      slide,
      "Tell Alex's story: scattered apps provide price facts, but not the combined view. The user need becomes: what do I own, what is it worth, and where am I concentrated?",
      [
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/guideline/references/portfolio_manager.md"),
      ],
    );
  }

  // 4. User stories
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "User stories kept the scope honest", "USER STORY MAP");
    addText(slide, "We treated each story as a promise we could demonstrate and explain.", 82, 144, 920, 32, {
      size: 25,
      color: C.ink,
      bold: true,
    });
    const stories = [
      ["HH-01", "Browse holdings", "As Alex, I want one list of assets so I can stop switching apps."],
      ["HH-02", "Add a holding", "As Alex, I want a simple form so my first portfolio takes minutes."],
      ["HH-04/05", "Understand value", "As Alex, I want summary and allocation so I can see the whole picture."],
      ["HH-06/10", "Trust the data", "As Alex, I want price status and fund warnings so I know the limits."],
    ];
    stories.forEach(([id, label, detail], i) => {
      const x = 92 + (i % 2) * 522;
      const y = 212 + Math.floor(i / 2) * 158;
      addRect(slide, x, y, 448, 116, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      addRect(slide, x + 24, y + 16, 96, 28, { fill: C.well, stroke: C.gold2, radius: "rounded-full" });
      addText(slide, id, x + 34, y + 22, 76, 14, { size: 12, bold: true, color: C.gold2, align: "center" });
      addText(slide, label, x + 142, y + 17, 260, 26, { size: 23, bold: true, color: C.ink });
      addText(slide, detail, x + 26, y + 56, 386, 42, { size: 16, color: C.soft, lineSpacing: 1.12 });
    });
    addRect(slide, 222, 552, 722, 48, { fill: C.well, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "Stories set the MVP; stretch ideas waited.", 258, 566, 650, 20, {
      size: 20,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 4);
    addNotes(
      slide,
      "Make User Stories central. Explain that HH-01 to HH-06 formed the P0 backbone: browse, add, delete, summary, allocation, and data trust. HH-10 made fund warnings visible without forcing full lookthrough into P0.",
      [
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/guideline/project/api_documentation_zh.md"),
        sourcePath("docs/qa/acceptance-checklist.md"),
      ],
    );
  }

  // 5. Product design choices
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "Product design translated stories into screens", "PRODUCT DESIGN");
    await addImage(slide, ASSETS.dashboard, 76, 150, 556, 395, {
      fit: "cover",
      geometry: "roundRect",
      radius: "rounded-xl",
      alt: "HoldHive dashboard design",
    });
    const choices = [
      ["Dashboard first", "summary before detail"],
      ["Visible trust", "demo / fixed / unavailable"],
      ["Fund warning", "lookthrough stays separate"],
      ["Recovery states", "failure is explainable"],
    ];
    choices.forEach(([a, b], i) => {
      const y = 160 + i * 92;
      addRect(slide, 692, y, 380, 64, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      hex(slide, 718, y + 14, 34, { stroke: [C.gold, C.green, C.purple, C.red][i], width: 2 });
      addText(slide, a, 770, y + 12, 210, 22, { size: 20, bold: true, color: C.ink });
      addText(slide, b, 770, y + 38, 210, 16, { size: 14, color: C.muted });
    });
    addRect(slide, 214, 582, 748, 46, { fill: C.panel, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "The UI made financial trust visible, not hidden in assumptions.", 250, 595, 676, 20, {
      size: 20,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 5);
    addNotes(
      slide,
      "This slide connects product design to the user stories. The dashboard screenshot should carry most of the page. Explain that the design borrowed mature portfolio-product patterns but intentionally kept the first experience narrow.",
      [
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/design/lanhu/README.md"),
        sourcePath("docs/design/lanhu/png/02-dashboard-dark.png"),
      ],
    );
  }

  // 6. Technology choices
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "We chose tools the team could explain", "TECH SELECTION", { dark: true });
    const tech = [
      ["React + Vite", "fast UI loop"],
      ["Spring Boot", "clear REST boundary"],
      ["MySQL + Flyway", "repeatable data setup"],
      ["JUnit / Vitest", "evidence, not hope"],
    ];
    tech.forEach(([a, b], i) => {
      const x = 92 + i * 270;
      addRect(slide, x, 170, 216, 162, { fill: C.panel, stroke: C.line });
      hex(slide, x + 78, 198, 58, { stroke: [C.blue, C.green, C.gold, C.purple][i], width: 3 });
      addText(slide, a, x + 26, 270, 164, 24, { size: 22, bold: true, color: C.white, align: "center" });
      addText(slide, b, x + 26, 304, 164, 18, { size: 15, color: C.soft, align: "center" });
    });
    addRect(slide, 146, 430, 870, 78, { fill: C.panel, stroke: C.line, radius: "rounded-lg" });
    addText(slide, "The rule was simple: use boring tools, then document why.", 206, 456, 748, 28, {
      size: 26,
      bold: true,
      color: C.white,
      align: "center",
    });
    addFooter(slide, 6, true);
    addNotes(
      slide,
      "This slide covers technology selection without drowning the audience in details. Explain that the team chose a stack two or more members could learn and explain, and wrote the decision into ADR-001.",
      [
        sourcePath("docs/guideline/project/technology_stack_zh.md"),
        sourcePath("docs/adr/ADR-001-technology-stack.md"),
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
      ],
    );
  }

  // 7. Architecture
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "Ownership boundaries kept the work explainable", "APPLICATION ARCHITECTURE", { dark: true });
    const boxes = [
      ["React UI", "display only", 84, 206, C.blue],
      ["API client", "typed contract", 308, 206, C.gold],
      ["Spring REST", "HTTP boundary", 532, 206, C.gold2],
      ["Domain", "valuation logic", 756, 206, C.green],
      ["Adapters", "MySQL + pricing", 980, 206, C.purple],
    ];
    boxes.forEach(([a, b, x, y, accent], i) => {
      addRect(slide, x, y, 156, 104, { fill: C.panel, stroke: C.line, radius: "rounded-lg" });
      hex(slide, x + 52, y + 16, 48, { stroke: accent, width: 2 });
      addText(slide, a, x + 18, y + 70, 120, 20, { size: 18, bold: true, color: C.white, align: "center" });
      addText(slide, b, x + 18, y + 92, 120, 16, { size: 12, color: C.soft, align: "center" });
      if (i < boxes.length - 1) addText(slide, "->", x + 166, y + 38, 48, 30, { size: 26, bold: true, color: C.gold, align: "center" });
    });
    const claims = [
      "Frontend never owns valuation formulas",
      "Domain has no Spring or database dependency",
      "Pricing failures do not corrupt holdings",
    ];
    claims.forEach((c, i) => bullet(slide, c, 236, 410 + i * 54, 760, { color: C.soft, size: 22, h: 34 }));
    addFooter(slide, 7, true);
    addNotes(
      slide,
      "This is the high-level architecture required by the project presentation. Keep it simple: user interface, REST contract, domain calculation, persistence, and pricing adapter. Mention that this is a modular monolith, not microservices.",
      [
        sourcePath("docs/architecture/c4-context.md"),
        sourcePath("docs/guideline/project/technology_stack_zh.md"),
        sourcePath("frontend/src/api/README.md"),
        sourcePath("backend/src/main/java/com/holdhive/portfolio/domain/PortfolioCalculator.java"),
      ],
    );
  }

  // 8. Iteration timeline
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "We iterated in four distinct beats", "ITERATIVE DEVELOPMENT");
    const phases = [
      ["Day 1", "Scope", "stories, ADR, skeleton"],
      ["Day 2", "Design", "API, data, screens"],
      ["Day 3", "Build", "summary, quotes, UI"],
      ["Day 4", "Stabilise", "tests, docs, rehearsal"],
    ];
    phases.forEach(([day, title, desc], i) => {
      const x = 110 + i * 272;
      addLine(slide, x + 92, 300, x + 272, 300, { color: i === 3 ? "none" : C.gold, width: 3 });
      addRect(slide, x, 226, 184, 148, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      addText(slide, day, x + 34, 250, 116, 22, { size: 18, bold: true, color: C.gold2, align: "center" });
      addText(slide, title, x + 28, 286, 128, 30, { size: 27, bold: true, color: C.ink, align: "center" });
      addText(slide, desc, x + 24, 332, 136, 24, { size: 15, color: C.muted, align: "center" });
    });
    addRect(slide, 214, 506, 748, 54, { fill: C.well, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "The point was not to finish first; it was to make progress visible and explainable.", 246, 521, 684, 20, {
      size: 18,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 8);
    addNotes(
      slide,
      "This slide tells the planning-to-delivery story. Tie it back to the course guidance: do not jump into code on day one, surface blockers early, and leave time for rehearsal.",
      [
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
        sourcePath("docs/guideline/project/git_branching_ci_zh.md"),
      ],
    );
  }

  // 9. Git graph
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "The Git graph shows the real teamwork", "GIT COLLABORATION", { dark: true });
    const lanes = [
      ["main", 168, C.gold],
      ["qa", 286, C.blue],
      ["feature work", 404, C.green],
    ];
    lanes.forEach(([label, y, color]) => {
      addText(slide, label, 76, y - 12, 120, 20, { size: 16, bold: true, color });
      addLine(slide, 190, y, 1126, y, { color: C.line, width: 2 });
    });
    function commit(x, y, color, label, msg, w = 104) {
      slide.shapes.add({ geometry: "ellipse", position: { left: x, top: y - 8, width: 16, height: 16 }, fill: color, line: { style: "solid", fill: color, width: 1 } });
      addRect(slide, x - 18, y + 18, w, 46, { fill: C.panel, stroke: C.line, radius: "rounded-lg" });
      addText(slide, label, x - 4, y + 24, w - 28, 14, { size: 10, bold: true, color });
      addText(slide, msg, x - 4, y + 39, w - 28, 15, { size: 10, color: C.soft });
    }
    const mainXs = [220, 360, 500, 640, 780];
    ["baseline", "backend", "frontend", "CI", "docs"].forEach((m, i) => commit(mainXs[i], 168, C.gold, ["c36d917","8e4126d","d1190d4","36a3d68","bbc6cd7"][i], m, 110));
    commit(300, 286, C.blue, "104802f", "pricing base", 108);
    commit(430, 286, C.blue, "ba2f5f6", "summary API", 108);
    commit(640, 404, C.green, "4ec90f4", "quotes API", 120);
    commit(780, 404, C.green, "da61953", "multi-asset", 120);
    commit(920, 404, C.green, "bf1fec4", "Flyway test", 120);
    commit(890, 286, C.blue, "3d79a2a", "merge fix", 104);
    commit(995, 286, C.red, "1f8b727", "revert", 86);
    commit(1080, 286, C.gold, "e024ae8", "reapply", 92);
    commit(1160, 286, C.gold, "6cb6d8c", "CRUD", 86);
    commit(1160, 350, C.gold2, "2a1d243", "QA docs", 86);
    addLine(slide, 780, 168, 300, 286, { color: C.dim, width: 2 });
    addLine(slide, 430, 286, 640, 404, { color: C.dim, width: 2 });
    addLine(slide, 920, 404, 890, 286, { color: C.dim, width: 2 });
    addLine(slide, 995, 286, 1080, 286, { color: C.gold, width: 3 });
    addLine(slide, 1080, 286, 1160, 286, { color: C.gold, width: 3 });
    addLine(slide, 1160, 286, 1160, 350, { color: C.gold2, width: 2 });
    addRect(slide, 238, 548, 740, 48, { fill: C.well, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "History matters: conflict, reapply, CRUD, and QA hardening are visible.", 276, 562, 664, 18, {
      size: 17,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 9, true);
    addNotes(
      slide,
      "This is a visual Git graph based on the actual repository history. Explain the main and qa branches, feature work, the conflict-resolution merge, the revert, and the reapply. This directly follows the course PDF's instruction to bring process visuals such as a git graph.",
      [
        sourcePath("git log --oneline --decorate --graph --all --max-count=24"),
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
        sourcePath("docs/guideline/project/git_branching_ci_zh.md"),
      ],
    );
  }

  // 10. What we built
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "The demo follows the customer journey", "WHAT WE BUILT");
    const frames = [
      [ASSETS.gateway, "Enter"],
      [ASSETS.dashboard, "Understand"],
      [ASSETS.holdings, "Maintain"],
      [ASSETS.states, "Recover"],
    ];
    for (let i = 0; i < frames.length; i++) {
      const [img, label] = frames[i];
      const x = 74 + i * 286;
      addRect(slide, x, 166, 246, 260, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      await addImage(slide, img, x + 18, 186, 210, 150, { fit: "cover", geometry: "roundRect", radius: "rounded-lg", alt: label });
      addText(slide, label, x + 30, 358, 186, 28, { size: 26, bold: true, color: C.ink, align: "center" });
    }
    addRect(slide, 214, 510, 772, 58, { fill: C.well, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "Live route: Gateway -> Dashboard -> add/list/delete holdings -> recoverable states.", 248, 527, 704, 20, {
      size: 18,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 10);
    addNotes(
      slide,
      "Use this slide as the bridge into live demo. It keeps the screen uncluttered and product-styled. The current qa branch includes holdings list, create, and delete endpoints; the UI/demo route should show the journey rather than API details.",
      [
        sourcePath("docs/demo/demo-script.md"),
        sourcePath("docs/design/lanhu/README.md"),
        sourcePath("frontend/src/App.tsx"),
        sourcePath("frontend/src/components/DashboardPage.tsx"),
      ],
    );
  }

  // 11. Calculation slice
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "Small formulas made the product trustworthy", "VALUATION SLICE", { dark: true });
    const formulas = [
      ["Cost", "quantity x average price"],
      ["Value", "quantity x current price"],
      ["P/L", "value - cost"],
      ["Allocation", "value / priced value"],
    ];
    formulas.forEach(([a, b], i) => {
      const x = 94 + i * 268;
      addRect(slide, x, 178, 216, 134, { fill: C.panel, stroke: C.line });
      addText(slide, a, x + 24, 210, 168, 30, { size: 29, bold: true, color: C.gold, align: "center" });
      addText(slide, b, x + 24, 258, 168, 22, { size: 15, color: C.soft, align: "center" });
    });
    addText(slide, "Price statuses are product language.", 300, 396, 580, 34, {
      size: 29,
      bold: true,
      color: C.white,
      align: "center",
    });
    addText(slide, "LIVE | CACHED | DEMO | FIXED | UNAVAILABLE", 270, 452, 640, 28, {
      size: 22,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addText(slide, "Unknown price is never treated as zero.", 376, 500, 420, 24, {
      size: 19,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 11, true);
    addNotes(
      slide,
      "Keep this technical slide short. Explain why BigDecimal, price statuses, and partial valuation matter to the product. This is the point where technical implementation reinforces customer trust.",
      [
        sourcePath("docs/guideline/project/api_documentation_zh.md"),
        sourcePath("backend/src/main/java/com/holdhive/portfolio/domain/PortfolioCalculator.java"),
        sourcePath("backend/src/test/java/com/holdhive/portfolio/domain/PortfolioCalculatorTest.java"),
      ],
    );
  }

  // 12. Bugs and recovery
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "We made mistakes, then made them visible", "BUGS AND RECOVERY");
    const recoveries = [
      ["Merge conflict", "resolved -> reverted -> reapplied"],
      ["Contract drift", "DTOs and docs moved together"],
      ["Price failure", "partial valuation, not fake zero"],
      ["Demo risk", "screenshots and script as backup"],
    ];
    recoveries.forEach(([a, b], i) => {
      const x = 82 + (i % 2) * 538;
      const y = 166 + Math.floor(i / 2) * 166;
      addRect(slide, x, y, 460, 110, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      addText(slide, a, x + 30, y + 24, 230, 28, { size: 24, bold: true, color: [C.red, C.gold2, C.green, C.blue][i] });
      addText(slide, b, x + 30, y + 64, 360, 24, { size: 19, color: C.muted });
    });
    addRect(slide, 180, 548, 806, 56, { fill: C.panel, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "The recovery is the project story, not an embarrassment.", 228, 564, 710, 22, {
      size: 21,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 12);
    addNotes(
      slide,
      "This slide follows the rule PDF: mistakes included. Use the actual git history as evidence for merge conflict recovery. Then explain bugs as learning moments: unknown prices, API contract drift, and live-demo risk were handled through clearer states and documentation.",
      [
        sourcePath("git log --oneline --decorate --graph --all --max-count=24"),
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
        sourcePath("docs/guideline/project/technology_stack_zh.md"),
        sourcePath("docs/demo/demo-script.md"),
      ],
    );
  }

  // 13. Quality gates
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "Done meant checked, not just compiled", "QUALITY GATES");
    metricCard(slide, "Backend gate", "70%+", "line coverage target", 92, 178, 262, { accent: C.green });
    metricCard(slide, "PR rule", "Human", "review before merge", 390, 178, 262, { accent: C.gold2 });
    metricCard(slide, "Acceptance", "Manual", "checklist-driven", 688, 178, 262, { accent: C.blue });
    const checks = [
      "Tests cover edge cases, not only happy path",
      "API cases and defect log keep QA visible",
      "Every member can explain their code",
    ];
    checks.forEach((c, i) => bullet(slide, c, 152, 408 + i * 54, 820, { size: 22, h: 36 }));
    addFooter(slide, 13);
    addNotes(
      slide,
      "This slide links the project to the rules PDF and GenAI guidance. Verification mattered more than simply generating code. Mention backend unit tests, frontend build/test, acceptance checklist, PR review, and human explainability.",
      [
        sourcePath("docs/guideline/references/team-project-rules-process.pdf"),
        sourcePath("docs/guideline/references/use-of-genai.md"),
        sourcePath("docs/qa/acceptance-checklist.md"),
        sourcePath("docs/qa/api-test-cases.md"),
        sourcePath("docs/qa/defect-log.md"),
        sourcePath("backend/pom.xml"),
      ],
    );
  }

  // 14. Learning
  {
    const slide = p.slides.add();
    slide.background.fill = C.bg;
    addSlideTitle(slide, "The main output was what the team learned", "NEW KNOWLEDGE", { dark: true });
    const learn = [
      ["Product", "scope is a design choice"],
      ["Backend", "BigDecimal + adapter boundaries"],
      ["Frontend", "stateful dashboards and charts"],
      ["Git", "conflict recovery is a skill"],
      ["AI", "assistant output must be explainable"],
    ];
    learn.forEach(([a, b], i) => {
      const x = 92 + i * 218;
      addRect(slide, x, 190, 170, 172, { fill: C.panel, stroke: C.line });
      hex(slide, x + 55, 218, 60, { stroke: [C.gold, C.green, C.blue, C.purple, C.red][i], width: 3 });
      addText(slide, a, x + 20, 296, 130, 24, { size: 23, bold: true, color: C.white, align: "center" });
      addText(slide, b, x + 18, 330, 134, 34, { size: 14, color: C.soft, align: "center", lineSpacing: 1.1 });
    });
    addText(slide, "We can defend the choices because we documented them.", 230, 468, 720, 32, {
      size: 29,
      bold: true,
      color: C.gold,
      align: "center",
    });
    addFooter(slide, 14, true);
    addNotes(
      slide,
      "This slide answers the user's request to include the learning process. Keep it reflective: what the team learned about product design, technical boundaries, Git collaboration, debugging, and disciplined AI use.",
      [
        sourcePath("docs/guideline/references/use-of-genai.md"),
        sourcePath("docs/guideline/project/technology_stack_zh.md"),
        sourcePath("docs/guideline/project/git_branching_ci_zh.md"),
        sourcePath("docs/adr/ADR-001-technology-stack.md"),
      ],
    );
  }

  // 15. Next steps
  {
    const slide = p.slides.add();
    slide.background.fill = C.cream;
    addSlideTitle(slide, "Next step: prove P0 before adding ambition", "NEXT STEPS");
    const roadmap = [
      ["Prove P0", "run CRUD demo end to end"],
      ["Harden demo", "full rehearsal + backup"],
      ["Sync QA", "test cases + defect log"],
      ["Expand insight", "fund exposure, then AI brief"],
    ];
    roadmap.forEach(([title, desc], i) => {
      const x = 100 + i * 265;
      addRect(slide, x, 188, 208, 142, { fill: C.panel, stroke: C.border, shadow: "shadow-sm" });
      addText(slide, title, x + 24, 224, 160, 28, { size: 25, bold: true, color: C.ink, align: "center" });
      addText(slide, desc, x + 24, 270, 160, 22, { size: 16, color: C.muted, align: "center" });
    });
    addRect(slide, 255, 472, 670, 64, { fill: C.panel, stroke: C.gold, radius: "rounded-lg" });
    addText(slide, "Questions?", 390, 482, 400, 42, { size: 38, bold: true, color: C.gold, align: "center" });
    addFooter(slide, 15);
    addNotes(
      slide,
      "Close by returning to the core discipline: prove P0 before adding P1/P2. Current qa has holdings CRUD endpoints, so the next work is end-to-end demo rehearsal, QA documentation sync, then fund exposure and AI brief expansion.",
      [
        sourcePath("docs/guideline/project/team_project_guideline_zh.md"),
        sourcePath("docs/guideline/project/api_documentation_zh.md"),
        sourcePath("docs/qa/acceptance-checklist.md"),
        sourcePath("docs/qa/api-test-cases.md"),
        sourcePath("backend/src/main/java/com/holdhive/portfolio/api/HoldingController.java"),
      ],
    );
  }

  for (const [i, slide] of p.slides.items.entries()) {
    const stem = `slide-${String(i + 1).padStart(2, "0")}`;
    await writeBlob(path.join(PREVIEW_DIR, `${stem}.png`), await p.export({ slide, format: "png", scale: 1 }));
    await fs.writeFile(path.join(PREVIEW_DIR, `${stem}.layout.json`), await (await slide.export({ format: "layout" })).text());
  }
  await writeBlob(MONTAGE, await p.export({ format: "webp", montage: true, scale: 1 }));

  const pptx = await PresentationFile.exportPptx(p);
  await pptx.save(FINAL_PPTX);

  const sourceNotes = [
    "HoldHive final presentation source notes",
    "",
    "Communication job:",
    "By the end, instructors and project stakeholders should understand how the team turned a broad portfolio-management assignment into a scoped product, built a demo-ready slice, recovered from integration issues, and learned enough to explain the code and process.",
    "",
    "Primary reviewed sources:",
    "- README.md",
    "- CONTRIBUTING.md",
    "- docs/README.md",
    "- docs/guideline/README.md",
    "- docs/guideline/output/HoldHive final project guide PDF",
    "- docs/guideline/references/portfolio_manager.md",
    "- docs/guideline/references/team-project-rules-process.pdf",
    "- docs/guideline/references/use-of-genai.md",
    "- docs/guideline/project/team_project_guideline_zh.md",
    "- docs/guideline/project/technology_stack_zh.md",
    "- docs/guideline/project/database_design_zh.md",
    "- docs/guideline/project/api_documentation_zh.md",
    "- docs/guideline/project/market_data_api_zh.md",
    "- docs/guideline/project/git_branching_ci_zh.md",
    "- docs/guideline/project/member_directory_map_zh.md",
    "- docs/architecture/c4-context.md",
    "- docs/design/lanhu/README.md",
    "- docs/demo/demo-script.md",
    "- docs/qa/acceptance-checklist.md",
    "- docs/qa/api-test-cases.md",
    "- docs/qa/defect-log.md",
    "- docs/adr/ADR-001-technology-stack.md",
    "- backend/src/main/java/com/holdhive/portfolio/api/HoldingController.java",
    "- backend/src/main/java/com/holdhive",
    "- backend/src/main/resources/db/migration",
    "- backend/src/test/java/com/holdhive",
    "- frontend/src",
    "- git log --oneline --decorate --graph --all --max-count=24",
    "",
    "Final output:",
    `- ${FINAL_PPTX}`,
    `- ${path.join(OUT_DIR, ".build/build_holdhive_presentation.mjs")}`,
    `- ${path.join(OUT_DIR, "source-notes.txt")}`,
    "",
    "Local QA artifacts are regenerated under docs/presentation/previews/ and ignored by Git.",
  ].join("\n");
  await fs.writeFile(path.join(OUT_DIR, "source-notes.txt"), sourceNotes);

  return { finalPptx: FINAL_PPTX, montage: MONTAGE, slides: p.slides.items.length };
}

buildDeck()
  .then((result) => {
    console.log(JSON.stringify(result, null, 2));
  })
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
