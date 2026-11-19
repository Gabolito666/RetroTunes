import { chromium } from "playwright";

const BASE_URL = process.env.BASE_URL || "http://localhost:8097";

async function runE2ESprint3() {
    console.log(`[E2E-SPRINT3] Iniciando suite de pruebas Sprint 3 y Cierre Final en ${BASE_URL}...`);
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
        // 1. Paso: Login de Administrador
        console.log("[E2E-SPRINT3] Paso 1: Autenticación de Administrador");
        await page.goto(`${BASE_URL}/login`);
        await page.fill('input[name="correo"]', "admin@retrotunes.com");
        await page.fill('input[name="clave"]', "admin123");
        await Promise.all([
            page.waitForNavigation(),
            page.click('button[type="submit"]')
        ]);
        console.log("[E2E-SPRINT3] ✓ Autenticación exitosa:", page.url());

        // 2. Paso: Consulta del Dashboard Gerencial
        console.log("[E2E-SPRINT3] Paso 2: Validación del Dashboard Gerencial (/admin/dashboard)");
        await page.goto(`${BASE_URL}/admin/dashboard`);
        const titleText = await page.textContent('h1');
        if (!titleText.includes("Panel de Control Gerencial")) {
            throw new Error("No se cargó el encabezado esperado en /admin/dashboard");
        }
        console.log("[E2E-SPRINT3] ✓ Dashboard cargado correctamente:", titleText.trim());

        // 3. Paso: Exportación y verificación binaria de Excel (.xlsx)
        console.log("[E2E-SPRINT3] Paso 3: Descarga y verificación de Reporte Excel (.xlsx)");
        const excelRes = await context.request.get(`${BASE_URL}/admin/reportes/excel`);
        if (excelRes.status() !== 200) {
            throw new Error(`Fallo en exportación Excel con status ${excelRes.status()}`);
        }
        const excelBuffer = await excelRes.body();
        // Verificación de firma ZIP/OOXML (PK\x03\x04)
        if (excelBuffer.length > 100 && excelBuffer[0] === 0x50 && excelBuffer[1] === 0x4B) {
            console.log(`[E2E-SPRINT3] ✓ Archivo Excel generado con éxito (${excelBuffer.length} bytes, firma PK ZIP válida)`);
        } else {
            throw new Error("El archivo Excel descargado no tiene una estructura válida de Office Open XML");
        }

        // 4. Paso: Descarga de Factura Oficial PDF
        console.log("[E2E-SPRINT3] Paso 4: Descarga y validación de Factura PDF");
        const facturaPdfLink = await page.$('a[href*="/compras/factura/"][href$="/pdf"]');
        let pdfRes;
        if (facturaPdfLink) {
            const pdfHref = await facturaPdfLink.getAttribute("href");
            console.log(`[E2E-SPRINT3] Descargando comprobante PDF desde: ${pdfHref}`);
            pdfRes = await context.request.get(`${BASE_URL}${pdfHref}`);
        } else {
            console.log("[E2E-SPRINT3] No se encontró link directo en tabla, probando endpoint /compras/factura/id/1/pdf");
            pdfRes = await context.request.get(`${BASE_URL}/compras/factura/id/1/pdf`);
        }

        if (pdfRes && pdfRes.status() === 200) {
            const pdfBuffer = await pdfRes.body();
            const pdfHeader = pdfBuffer.slice(0, 5).toString("ascii");
            if (pdfHeader.startsWith("%PDF-")) {
                console.log(`[E2E-SPRINT3] ✓ Factura PDF generada y validada (${pdfBuffer.length} bytes, cabecera %PDF- presente)`);
            } else {
                throw new Error("El archivo PDF generado no contiene la cabecera estándar %PDF-");
            }
        } else {
            console.log("[E2E-SPRINT3] Nota: No había factura previa o devolvió estado no-200. Creando una venta para validar PDF...");
            // Agregar producto al carrito y comprar
            await page.goto(`${BASE_URL}/catalogo`);
            const detalleBtn = await page.$('a[href^="/catalogo/"]');
            if (detalleBtn) {
                await page.goto(`${BASE_URL}${await detalleBtn.getAttribute("href")}`);
                const addCartBtn = await page.$('button:has-text("Agregar al Carrito")');
                if (addCartBtn) {
                    await Promise.all([page.waitForNavigation(), addCartBtn.click()]);
                    await page.goto(`${BASE_URL}/compras/checkout`);
                    await page.check('input[value="TARJETA_CREDITO"]');
                    await Promise.all([page.waitForNavigation(), page.click('button[type="submit"]')]);
                    
                    // Ahora descargamos la factura
                    await page.goto(`${BASE_URL}/admin/dashboard`);
                    const nuevoPdfLink = await page.$('a[href*="/compras/factura/"][href$="/pdf"]');
                    if (nuevoPdfLink) {
                        const href = await nuevoPdfLink.getAttribute("href");
                        const newPdfRes = await context.request.get(`${BASE_URL}${href}`);
                        const buf = await newPdfRes.body();
                        if (buf.slice(0, 5).toString("ascii").startsWith("%PDF-")) {
                            console.log(`[E2E-SPRINT3] ✓ Factura PDF generada tras venta (${buf.length} bytes, cabecera %PDF- válida)`);
                        }
                    }
                }
            }
        }

        // 5. Paso: Módulo de Operaciones e Inspección Técnica
        console.log("[E2E-SPRINT3] Paso 5: Módulo de Inspección Técnica de Alquileres (/operaciones/inspeccion)");
        await page.goto(`${BASE_URL}/operaciones/inspeccion`);
        const operacionTitle = await page.textContent('h1');
        if (!operacionTitle.includes("Módulo de Operaciones")) {
            throw new Error("No se cargó el encabezado esperado en /operaciones/inspeccion");
        }
        console.log("[E2E-SPRINT3] ✓ Vista de inspección técnica operativa:", operacionTitle.trim());

        // Si hay formularios de inspección pendientes, procesamos uno
        const formInspeccion = await page.$('form[action*="/operaciones/inspeccion/"]');
        if (formInspeccion) {
            console.log("[E2E-SPRINT3] Procesando formulario de peritaje técnico...");
            await page.fill('form[action*="/operaciones/inspeccion/"] input[name="novedadTecnica"]', "Revisión técnica automatizada Playwright: Todo en orden.");
            const btnLiberar = await page.$('form[action*="/operaciones/inspeccion/"] button[value="LIBERAR"]');
            if (btnLiberar) {
                await Promise.all([
                    page.waitForNavigation(),
                    btnLiberar.click()
                ]);
                console.log("[E2E-SPRINT3] ✓ Inspección completada y depósito liberado con éxito");
            }
        } else {
            console.log("[E2E-SPRINT3] No hay alquileres pendientes de inspección en este ciclo");
        }

        // 6. Paso: Panel de Regalías de Artistas y Descarga Word (.docx)
        console.log("[E2E-SPRINT3] Paso 6: Panel de Liquidación de Regalías y descarga de Acta Word (.docx)");
        await page.goto(`${BASE_URL}/artista/liquidacion`);
        const artistaTitle = await page.textContent('h1');
        if (!artistaTitle.includes("Liquidación de Regalías")) {
            throw new Error("No se cargó el encabezado esperado en /artista/liquidacion");
        }
        console.log("[E2E-SPRINT3] ✓ Panel de regalías cargado:", artistaTitle.trim());

        const wordRes = await context.request.get(`${BASE_URL}/artista/liquidacion/docx`);
        if (wordRes.status() !== 200) {
            throw new Error(`Fallo en descarga de documento Word con status ${wordRes.status()}`);
        }
        const wordBuffer = await wordRes.body();
        if (wordBuffer.length > 100 && wordBuffer[0] === 0x50 && wordBuffer[1] === 0x4B) {
            console.log(`[E2E-SPRINT3] ✓ Acta de liquidación en Word generada con éxito (${wordBuffer.length} bytes, firma PK ZIP válida)`);
        } else {
            throw new Error("El archivo Word descargado no tiene una estructura válida de Office Open XML");
        }

        // 7. Paso: Centro de Reportes
        console.log("[E2E-SPRINT3] Paso 7: Validación de Centro de Reportes (/admin/reportes)");
        await page.goto(`${BASE_URL}/admin/reportes`);
        const repTitle = await page.textContent('h1');
        if (!repTitle.includes("Centro de Reportes")) {
            throw new Error("No se cargó el centro de reportes");
        }
        console.log("[E2E-SPRINT3] ✓ Centro de reportes cargado:", repTitle.trim());

        console.log("[E2E-SPRINT3] ==================================================================");
        console.log("[E2E-SPRINT3] ¡TODAS LAS PRUEBAS E2E DEL SPRINT 3 Y CIERRE PASARON EXITOSAMENTE!");
        console.log("[E2E-SPRINT3] ==================================================================");

    } catch (error) {
        console.error("[E2E-SPRINT3] ERROR durante la ejecución:", error);
        process.exitCode = 1;
    } finally {
        await browser.close();
    }
}

runE2ESprint3();
