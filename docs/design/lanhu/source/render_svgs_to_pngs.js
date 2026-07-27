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
