// Gestor de Temas Analógicos y Modos Claro / Oscuro — RetroTunes (Unslop Edition)

const VALID_THEMES = [
    'cobalt-dark', 'cobalt-light',
    'warm-dark', 'warm-light',
    'hifi-dark', 'hifi-light'
];

function normalizeTheme(theme) {
    if (!theme) return 'cobalt-dark';
    if (theme === 'cobalt-retro') return 'cobalt-dark';
    if (theme === 'warm-vinyl') return 'warm-dark';
    return VALID_THEMES.includes(theme) ? theme : 'cobalt-dark';
}

function setRetroTheme(theme) {
    const canonicalTheme = normalizeTheme(theme);
    document.documentElement.setAttribute('data-theme', canonicalTheme);
    localStorage.setItem('retrotunes-theme', canonicalTheme);
    updateThemeUI(canonicalTheme);
}

function updateThemeUI(theme) {
    const labels = {
        'cobalt-dark': 'Cobalt (Oscuro)',
        'cobalt-light': 'Cobalt (Claro)',
        'warm-dark': 'Warm Vinyl (Oscuro)',
        'warm-light': 'Warm Vinyl (Claro)',
        'hifi-dark': 'Hi-Fi (Oscuro)',
        'hifi-light': 'Hi-Fi (Claro)'
    };

    const lbl = document.getElementById('currentThemeLabel');
    if (lbl && labels[theme]) {
        lbl.textContent = labels[theme];
    }

    VALID_THEMES.forEach(t => {
        const check = document.getElementById('check-' + t);
        if (check) {
            check.style.display = (t === theme) ? 'inline-block' : 'none';
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // Sincronizar tema activo desde localStorage (Default: Cobalt Dark)
    const savedTheme = normalizeTheme(localStorage.getItem('retrotunes-theme'));
    setRetroTheme(savedTheme);

    // Inicializar tooltips de Bootstrap 5
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

    // Desvanecer alertas automáticamente después de 5 segundos
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 5000);
    });

    // Delegación de eventos para adición rápida al carrito por AJAX
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-add-cart-ajax');
        if (btn) {
            e.preventDefault();
            const idProducto = btn.getAttribute('data-id');
            const cantidad = btn.getAttribute('data-cantidad') || 1;
            if (idProducto) {
                agregarAlCarritoAjax(idProducto, cantidad, btn);
            }
        }
    });
});

// Adición asíncrona al carrito con Fetch API
function agregarAlCarritoAjax(idProducto, cantidad, btnTrigger) {
    let originalHtml = null;
    if (btnTrigger) {
        originalHtml = btnTrigger.innerHTML;
        btnTrigger.disabled = true;
        btnTrigger.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>...';
    }

    const params = new URLSearchParams();
    params.append('idProducto', idProducto);
    params.append('cantidad', cantidad);

    fetch('/compras/carrito/agregar-ajax', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: params.toString()
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Error en la comunicación con el servidor.');
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            // Actualizar badge del carrito en el navbar
            actualizarBadgeCarrito(data.totalItems);
            mostrarToastCarrito('Agregado al Carrito', data.mensaje, false);
        } else {
            mostrarToastCarrito('No se pudo añadir', data.mensaje, true);
        }
    })
    .catch(err => {
        mostrarToastCarrito('Error de Conexión', 'No se pudo comunicar con el servidor.', true);
    })
    .finally(() => {
        if (btnTrigger) {
            btnTrigger.disabled = false;
            btnTrigger.innerHTML = originalHtml;
        }
    });
}

// Actualizar indicador visual del carrito
function actualizarBadgeCarrito(totalItems) {
    let badge = document.getElementById('cartBadgeCount');
    if (badge) {
        badge.textContent = totalItems;
        badge.style.display = totalItems > 0 ? 'inline-block' : 'none';
        badge.classList.remove('bg-danger');
        badge.classList.add('bg-warning');
        setTimeout(() => {
            badge.classList.remove('bg-warning');
            badge.classList.add('bg-danger');
        }, 600);
    }
}

// Notificación Flotante Toast Bootstrap 5
function mostrarToastCarrito(titulo, mensaje, esError) {
    const toastEl = document.getElementById('cartToast');
    if (!toastEl) return;

    const toastTitle = document.getElementById('cartToastTitle');
    const toastBody = document.getElementById('cartToastBody');
    const toastIcon = document.getElementById('cartToastIcon');

    if (toastTitle) toastTitle.textContent = titulo;
    if (toastBody) toastBody.textContent = mensaje;

    if (toastIcon) {
        if (esError) {
            toastIcon.className = 'bi bi-exclamation-triangle-fill text-danger fs-5';
        } else {
            toastIcon.className = 'bi bi-check-circle-fill text-success fs-5';
        }
    }

    const toastInstance = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 3500 });
    toastInstance.show();
}
