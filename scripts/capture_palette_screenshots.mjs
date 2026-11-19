import { chromium } from "playwright";
import fs from "fs";
import path from "path";

const BASE_URL = process.env.BASE_URL || "http://localhost:8097";
const ARTIFACT_DIR = "C:/Users/gprad/.gemini/antigravity-cli/brain/faa229da-5997-4f11-af5f-e018d71ef560";
const OUTPUT_DIR = path.resolve("docs/screenshots");

const THEMES = [
    { id: "cobalt-dark", name: "Cobalt & Retro Tape (Oscuro)" },
    { id: "cobalt-light", name: "Cobalt & Retro Tape (Claro)" },
    { id: "warm-dark", name: "Warm Vinyl & Amber (Oscuro)" },
    { id: "warm-light", name: "Warm Vinyl & Amber (Claro)" },
    { id: "hifi-dark", name: "Hi-Fi Studio (Oscuro)" },
    { id: "hifi-light", name: "Hi-Fi Studio (Claro)" }
];

async function captureThemes() {
    console.log(`[PALETTE-AUDIT] Iniciando sesión Playwright para captura de las 6 paletas (Dark & Light) en ${BASE_URL}...`);
    
    if (!fs.existsSync(OUTPUT_DIR)) {
        fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    }
    if (!fs.existsSync(ARTIFACT_DIR)) {
        fs.mkdirSync(ARTIFACT_DIR, { recursive: true });
    }

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({
        viewport: { width: 1280, height: 800 },
        deviceScaleFactor: 1
    });
    const page = await context.newPage();

    for (const theme of THEMES) {
        console.log(`[PALETTE-AUDIT] Capturando: ${theme.name} (${theme.id})...`);

        // 1. Landing Page
        await page.goto(`${BASE_URL}/`, { waitUntil: "networkidle" });
        await page.evaluate((themeId) => {
            window.setRetroTheme(themeId);
        }, theme.id);
        await page.waitForTimeout(250);

        const landingFileName = `landing_${theme.id.replace("-", "_")}.png`;
        const landingLocalPath = path.join(OUTPUT_DIR, landingFileName);
        const landingArtifactPath = path.join(ARTIFACT_DIR, landingFileName);

        await page.screenshot({ path: landingLocalPath, fullPage: false });
        fs.copyFileSync(landingLocalPath, landingArtifactPath);

        // 2. Catálogo
        await page.goto(`${BASE_URL}/catalogo`, { waitUntil: "networkidle" });
        await page.evaluate((themeId) => {
            window.setRetroTheme(themeId);
        }, theme.id);
        await page.waitForTimeout(250);

        const catalogoFileName = `catalogo_${theme.id.replace("-", "_")}.png`;
        const catalogoLocalPath = path.join(OUTPUT_DIR, catalogoFileName);
        const catalogoArtifactPath = path.join(ARTIFACT_DIR, catalogoFileName);

        await page.screenshot({ path: catalogoLocalPath, fullPage: false });
        fs.copyFileSync(catalogoLocalPath, catalogoArtifactPath);
        console.log(`[PALETTE-AUDIT] ✓ ${theme.id}: Landing y Catálogo capturados con éxito`);
    }

    await browser.close();
    console.log("[PALETTE-AUDIT] Todas las 6 variantes (Oscuro y Claro) capturadas con éxito.");
}

captureThemes().catch((err) => {
    console.error("[PALETTE-AUDIT] Error durante la captura de pantallas:", err);
    process.exit(1);
});
