-- ==============================================================================
-- RETROTUNES - SPRINT 1: DDL, ÍNDICES DE OPTIMIZACIÓN Y SEED DATA EN SUPABASE
-- ==============================================================================

-- 1. Tabla: roles
CREATE TABLE IF NOT EXISTS public.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Tabla: usuarios
CREATE TABLE IF NOT EXISTS public.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    clave_usuario VARCHAR(255) NOT NULL,
    telefono VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    id_rol BIGINT NOT NULL,
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol)
);

-- 3. Tabla: artistas_empresas
CREATE TABLE IF NOT EXISTS public.artistas_empresas (
    id_entidad BIGSERIAL PRIMARY KEY,
    nombre_comercial VARCHAR(255) NOT NULL,
    tipo_entidad VARCHAR(50) NOT NULL,
    biografia_descripcion TEXT,
    sitio_web VARCHAR(255),
    id_usuario BIGINT UNIQUE,
    CONSTRAINT fk_artistas_empresas_usuarios FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario)
);

-- 4. Tabla: productos_musica
CREATE TABLE IF NOT EXISTS public.productos_musica (
    id_producto BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    titulo VARCHAR(255) NOT NULL,
    formato VARCHAR(50) NOT NULL,
    precio_venta NUMERIC(10, 2) NOT NULL,
    precio_alquiler_dia NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL CHECK (stock >= 0),
    spotify_album_id VARCHAR(255)
);

-- ==============================================================================
-- ÍNDICES B-TREE Y PARCIALES DE ALTO RENDIMIENTO
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON public.usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_id_rol ON public.usuarios(id_rol);
CREATE INDEX IF NOT EXISTS idx_productos_formato ON public.productos_musica(formato);
CREATE INDEX IF NOT EXISTS idx_productos_precio_venta ON public.productos_musica(precio_venta);
CREATE INDEX IF NOT EXISTS idx_productos_stock_disponible ON public.productos_musica(id_producto) WHERE stock > 0;
CREATE INDEX IF NOT EXISTS idx_artistas_empresas_tipo ON public.artistas_empresas(tipo_entidad);

-- ==============================================================================
-- POBLACIÓN INICIAL (SEED DATA SPRINT 1)
-- ==============================================================================

INSERT INTO public.roles (nombre_rol) VALUES
    ('ADMIN'),
    ('CLIENTE'),
    ('ARTISTA'),
    ('DISCOGRAFICA'),
    ('TECNICO_OPERACIONES')
ON CONFLICT (nombre_rol) DO NOTHING;

INSERT INTO public.usuarios (nombre_completo, correo, clave_usuario, telefono, activo, id_rol)
SELECT 
    'Administrador RetroTunes', 
    'admin@retrotunes.com', 
    'admin123', 
    '+57 300 000 0000', 
    TRUE, 
    id_rol
FROM public.roles 
WHERE nombre_rol = 'ADMIN'
ON CONFLICT (correo) DO NOTHING;

INSERT INTO public.productos_musica (sku, titulo, formato, precio_venta, precio_alquiler_dia, stock, spotify_album_id) VALUES
    ('VIN-ABBEY-001', 'The Beatles - Abbey Road (Edición Especial 180g)', 'VINILO', 39.99, 4.50, 15, '0ETFjACtuP2ADo6LFhL6HN'),
    ('CAS-THRILLER-002', 'Michael Jackson - Thriller (Cassette Vintage Remastered)', 'CASSETTE', 19.50, 2.50, 10, '2ANVost0y2y52ema1E9xAZ'),
    ('CD-DARK-003', 'Pink Floyd - The Dark Side of the Moon (Deluxe CD)', 'CD', 15.90, 1.75, 25, '4LH4d3cOWNNXdJwFd4G1kc'),
    ('EQ-CROSLEY-004', 'Tornamesa Portátil Retro Cruiser Deluxe Bluetooth', 'EQUIPO_SONIDO', 129.99, 15.00, 6, NULL)
ON CONFLICT (sku) DO NOTHING;


-- ==============================================================================
-- RETROTUNES - SPRINT 2: VENTAS, DETALLES, ALQUILERES Y RESERVAS DE ESPACIO
-- ==============================================================================

-- 5. Tabla: ventas
CREATE TABLE IF NOT EXISTS public.ventas (
    id_venta BIGSERIAL PRIMARY KEY,
    codigo_factura VARCHAR(60) NOT NULL UNIQUE,
    fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    monto_total NUMERIC(10, 2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    id_usuario BIGINT NOT NULL,
    CONSTRAINT fk_ventas_usuarios FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario)
);

-- 6. Tabla: detalles_venta
CREATE TABLE IF NOT EXISTS public.detalles_venta (
    id_detalle BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_detalles_ventas FOREIGN KEY (id_venta) REFERENCES public.ventas(id_venta) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_productos FOREIGN KEY (id_producto) REFERENCES public.productos_musica(id_producto)
);

-- 7. Tabla: alquileres_equipo
CREATE TABLE IF NOT EXISTS public.alquileres_equipo (
    id_alquiler BIGSERIAL PRIMARY KEY,
    codigo_alquiler VARCHAR(60) NOT NULL UNIQUE,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    deposito_garantia NUMERIC(10, 2) NOT NULL,
    monto_alquiler NUMERIC(10, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'RESERVADO',
    id_usuario BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    CONSTRAINT fk_alquileres_usuarios FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario),
    CONSTRAINT fk_alquileres_productos FOREIGN KEY (id_producto) REFERENCES public.productos_musica(id_producto)
);

-- 8. Tabla: reservas_espacio
CREATE TABLE IF NOT EXISTS public.reservas_espacio (
    id_reserva BIGSERIAL PRIMARY KEY,
    fecha_reserva DATE NOT NULL,
    bloque_horario VARCHAR(50) NOT NULL,
    google_event_id VARCHAR(255),
    id_usuario BIGINT NOT NULL,
    CONSTRAINT fk_reservas_usuarios FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario)
);

-- Índices B-Tree Sprint 2
CREATE INDEX IF NOT EXISTS idx_ventas_cliente ON public.ventas(id_usuario);
CREATE INDEX IF NOT EXISTS idx_ventas_codigo ON public.ventas(codigo_factura);
CREATE INDEX IF NOT EXISTS idx_detalles_venta ON public.detalles_venta(id_venta);
CREATE INDEX IF NOT EXISTS idx_alquileres_cliente ON public.alquileres_equipo(id_usuario);
CREATE INDEX IF NOT EXISTS idx_alquileres_codigo ON public.alquileres_equipo(codigo_alquiler);
CREATE INDEX IF NOT EXISTS idx_reservas_cliente ON public.reservas_espacio(id_usuario);
CREATE INDEX IF NOT EXISTS idx_reservas_fecha ON public.reservas_espacio(fecha_reserva);
