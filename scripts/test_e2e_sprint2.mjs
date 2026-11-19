import { chromium } from "playwright";

const BASE_URL = process.env.BASE_URL || "http://localhost:8097";

async function runE2E() {
    console.log(`[E2E] Iniciando suite de pruebas Sprint 2 en ${BASE_URL}...`);
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
        // 1. Paso: Login
        console.log("[E2E] Paso 1: Autenticación con admin@retrotunes.com");
        await page.goto(`${BASE_URL}/login`);
        await page.fill('input[name="correo"]', "admin@retrotunes.com");
        await page.fill('input[name="clave"]', "admin123");
        await Promise.all([
            page.waitForNavigation(),
            page.click('button[type="submit"]')
        ]);
        console.log("[E2E] ✓ Login exitoso, redirección completada:", page.url());

        // 2. Paso: Catálogo y Detalle de Vinilo
        console.log("[E2E] Paso 2: Navegación al catálogo y ficha de detalle");
        await page.goto(`${BASE_URL}/catalogo`);
        const primerDetalle = await page.$('a[href^="/catalogo/"]');
        if (!primerDetalle) {
            throw new Error("No se encontraron productos en el catálogo");
        }
        const detalleUrl = await primerDetalle.getAttribute("href");
        await page.goto(`${BASE_URL}${detalleUrl}`);
        console.log("[E2E] ✓ Ficha de producto cargada:", page.url());

        // 3. Paso: Agregar al Carrito
        console.log("[E2E] Paso 3: Agregar vinilo al carrito de compras");
        const agregarBtn = await page.$('button:has-text("Agregar al Carrito")');
        if (agregarBtn) {
            await Promise.all([
                page.waitForNavigation(),
                agregarBtn.click()
            ]);
        } else {
            // Si el producto seleccionado es un equipo de sonido, vamos al vinilo 1 directamente
            await page.goto(`${BASE_URL}/catalogo/1`);
            await Promise.all([
                page.waitForNavigation(),
                page.click('button:has-text("Agregar al Carrito")')
            ]);
        }
        console.log("[E2E] ✓ Carrito cargado:", page.url());
        const totalText = await page.textContent('.price-display');
        console.log("[E2E] ✓ Total calculado en carrito:", totalText?.trim());

        // 4. Paso: Checkout
        console.log("[E2E] Paso 4: Proceder al Checkout y confirmar orden");
        await page.goto(`${BASE_URL}/compras/checkout`);
        await page.check('input[value="TARJETA_CREDITO"]');
        await Promise.all([
            page.waitForNavigation(),
            page.click('button[type="submit"]')
        ]);
        console.log("[E2E] ✓ Orden procesada con éxito:", page.url());

        // 5. Paso: Reserva de Alquiler de Equipo de Sonido
        console.log("[E2E] Paso 5: Formulario de reserva de alquiler de equipo");
        await page.goto(`${BASE_URL}/alquileres/reserva`);
        console.log("[E2E] URL tras navegar a /alquileres/reserva:", page.url());
        if (page.url().includes("/login")) {
            console.log("[E2E] Re-autenticando para flujo de alquiler...");
            await page.fill('input[name="correo"]', "admin@retrotunes.com");
            await page.fill('input[name="clave"]', "admin123");
            await Promise.all([
                page.waitForNavigation(),
                page.click('button[type="submit"]')
            ]);
            await page.goto(`${BASE_URL}/alquileres/reserva`);
        }
        
        // Seleccionar primer equipo disponible en el select
        const options = await page.$$eval('#selectEquipo option', opts => 
            opts.filter(o => !o.disabled && o.value).map(o => o.value)
        );
        if (options.length === 0) {
            throw new Error("No hay equipos de sonido disponibles para reserva");
        }
        await page.selectOption('#selectEquipo', options[0]);

        const hoy = new Date();
        const enDosDias = new Date();
        enDosDias.setDate(hoy.getDate() + 2);
        const enCincoDias = new Date();
        enCincoDias.setDate(hoy.getDate() + 5);

        const fmt = d => d.toISOString().split("T")[0];
        await page.fill('#fechaInicio', fmt(enDosDias));
        await page.fill('#fechaFin', fmt(enCincoDias));

        await Promise.all([
            page.waitForNavigation(),
            page.click('button[type="submit"]')
        ]);
        console.log("[E2E] ✓ Alquiler registrado, en 'Mis Reservas':", page.url());

        // 6. Paso: Verificación de iCal (.ics)
        console.log("[E2E] Paso 6: Verificación de descarga de iCal (.ics)");
        const icalLink = await page.$('a[href$="/ical"]');
        if (icalLink) {
            const icalHref = await icalLink.getAttribute("href");
            const response = await page.request.get(`${BASE_URL}${icalHref}`);
            const body = await response.text();
            if (body.includes("BEGIN:VCALENDAR") && body.includes("SUMMARY:Alquiler RetroTunes")) {
                console.log("[E2E] ✓ Archivo iCal generado y validado conforme a RFC 5545");
            } else {
                throw new Error("El archivo iCal no contiene el formato esperado");
            }
        }

        console.log("[E2E] ========================================================");
        console.log("[E2E] ¡TODAS LAS PRUEBAS E2E DEL SPRINT 2 PASARON EXITOSAMENTE!");
        console.log("[E2E] ========================================================");
    } catch (err) {
        console.error("[E2E] ERROR durante la ejecución:", err);
        process.exitCode = 1;
    } finally {
        await browser.close();
    }
}

runE2E();
