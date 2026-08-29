import { expect, test } from "@playwright/test";

const importedReplayKey = "depotreplay.replay.import";

test("downloads, verifies, and steps through a browser replay", async ({ page }) => {
  const browserErrors = [];
  page.on("pageerror", error => browserErrors.push(error.message));
  page.on("console", message => {
    if (message.type() === "error") browserErrors.push(message.text());
  });

  await page.goto("/");
  const canvas = page.getByLabel("DepotReplay interactive game canvas");
  await expect(canvas).toBeVisible();
  await expect(canvas).toHaveAttribute("data-game-ready", "true");

  const downloadPromise = page.waitForEvent("download");
  await canvas.press("e");
  const download = await downloadPromise;
  const replayBytes = await download.createReadStream().then(async stream => {
    const chunks = [];
    for await (const chunk of stream) chunks.push(chunk);
    return Buffer.concat(chunks);
  });
  const replay = JSON.parse(replayBytes.toString("utf8"));
  expect(download.suggestedFilename()).toMatch(/^depotreplay-.*\.replay\.json$/);
  expect(replay.schemaVersion).toBe(1);
  expect(replay.tickHashes.length).toBeGreaterThan(1);
  expect(replay.finalStateHash).toMatch(/^[0-9a-f]{64}$/);

  await page.getByLabel("Choose replay file").setInputFiles({
    name: "round-trip.replay.json",
    mimeType: "application/json",
    buffer: replayBytes
  });
  await expect(page.getByRole("status")).toContainText("Selected round-trip.replay.json");
  await canvas.press("l");
  await expect(page.getByRole("status")).toContainText("Verified selected file");

  const beforeStep = await canvas.screenshot();
  await canvas.press("Space");
  const afterStep = await canvas.screenshot();
  expect(afterStep.equals(beforeStep)).toBe(false);
  expect(browserErrors).toEqual([]);
});

test("rejects a corrupted replay and an oversized import", async ({ page }) => {
  await page.goto("/");
  const canvas = page.getByLabel("DepotReplay interactive game canvas");
  await expect(canvas).toHaveAttribute("data-game-ready", "true");
  const replay = await exportReplay(page, canvas);
  replay.tickHashes[0].stateHash = "0".repeat(64);

  await page.getByLabel("Choose replay file").setInputFiles({
    name: "corrupted.replay.json",
    mimeType: "application/json",
    buffer: Buffer.from(JSON.stringify(replay))
  });
  await canvas.press("l");
  await expect(page.getByRole("status")).toContainText("Replay rejected");
  await expect(page.getByRole("status")).toContainText("tick 0");

  await page.getByLabel("Choose replay file").setInputFiles({
    name: "oversized.replay.json",
    mimeType: "application/json",
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1)
  });
  await expect(page.getByRole("status")).toContainText("5 MB or smaller");
  expect(await page.evaluate(key => localStorage.getItem(key), importedReplayKey)).not.toBeNull();
});

async function exportReplay(page, canvas) {
  const downloadPromise = page.waitForEvent("download");
  await canvas.press("e");
  const download = await downloadPromise;
  const stream = await download.createReadStream();
  const chunks = [];
  for await (const chunk of stream) chunks.push(chunk);
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}
