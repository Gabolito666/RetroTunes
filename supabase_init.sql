-- ==============================================================================
-- RETROTUNES - SPRINT 1: DDL & POBLACIÓN INICIAL PARA SUPABASE POSTGRESQL
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

-- Índices recomendados para optimización de consultas
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON public.usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_productos_formato ON public.productos_musica(formato);
CREATE INDEX IF NOT EXISTS idx_productos_sku ON public.productos_musica(sku);

-- ==============================================================================
-- POBLACIÓN INICIAL (SEED DATA SPRINT 1)
-- ==============================================================================

-- Insertar los 5 roles si no existen
INSERT INTO public.roles (nombre_rol) VALUES
    ('ADMIN'),
    ('CLIENTE'),
    ('ARTISTA'),
    ('DISCOGRAFICA'),
    ('TECNICO_OPERACIONES')
ON CONFLICT (nombre_rol) DO NOTHING;

-- Insertar Usuario Administrador por defecto
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

-- Insertar los 4 productos semilla
INSERT INTO public.productos_musica (sku, titulo, formato, precio_venta, precio_alquiler_dia, stock, spotify_album_id) VALUES
    ('VIN-ABBEY-001', 'The Beatles - Abbey Road (Edición Especial 180g)', 'VINILO', 39.99, 4.50, 15, '0ETFjACtuP2ADo6LFhL6HN'),
    ('CAS-THRILLER-002', 'Michael Jackson - Thriller (Cassette Vintage Remastered)', 'CASSETTE', 19.50, 2.50, 10, '2ANVost0y2y52ema1E9xAZ'),
    ('CD-DARK-003', 'Pink Floyd - The Dark Side of the Moon (Deluxe CD)', 'CD', 15.90, 1.75, 25, '4LH4d3cOWNNXdJwFd4G1kc'),
    ('EQ-CROSLEY-004', 'Tornamesa Portátil Retro Cruiser Deluxe Bluetooth', 'EQUIPO_SONIDO', 129.99, 15.00, 6, NULL)
ON CONFLICT (sku) DO NOTHING;
