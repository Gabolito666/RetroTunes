package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "productos_musica")
public class ProductoMusica implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id_producto;

    @NotBlank(message = "El SKU es obligatorio")
    @Column(name = "sku", unique = true, nullable = false, length = 50)
    private String sku;

    @NotBlank(message = "El título es obligatorio")
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @NotBlank(message = "El formato es obligatorio")
    @Column(name = "formato", nullable = false, length = 50)
    private String formato; // VINILO, CASSETTE, CD, EQUIPO_SONIDO

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta no puede ser negativo")
    @Column(name = "precio_venta", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio_venta;

    @NotNull(message = "El precio de alquiler por día es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de alquiler no puede ser negativo")
    @Column(name = "precio_alquiler_dia", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio_alquiler_dia;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "spotify_album_id")
    private String spotify_album_id;

    public ProductoMusica() {
    }

    public ProductoMusica(String sku, String titulo, String formato, BigDecimal precio_venta, BigDecimal precio_alquiler_dia, Integer stock, String spotify_album_id) {
        this.sku = sku;
        this.titulo = titulo;
        this.formato = formato;
        this.precio_venta = precio_venta;
        this.precio_alquiler_dia = precio_alquiler_dia;
        this.stock = stock;
        this.spotify_album_id = spotify_album_id;
    }

    public Long getId_producto() {
        return id_producto;
    }

    public void setId_producto(Long id_producto) {
        this.id_producto = id_producto;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public BigDecimal getPrecio_venta() {
        return precio_venta;
    }

    public void setPrecio_venta(BigDecimal precio_venta) {
        this.precio_venta = precio_venta;
    }

    public BigDecimal getPrecio_alquiler_dia() {
        return precio_alquiler_dia;
    }

    public void setPrecio_alquiler_dia(BigDecimal precio_alquiler_dia) {
        this.precio_alquiler_dia = precio_alquiler_dia;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSpotify_album_id() {
        return spotify_album_id;
    }

    public void setSpotify_album_id(String spotify_album_id) {
        this.spotify_album_id = spotify_album_id;
    }
}
