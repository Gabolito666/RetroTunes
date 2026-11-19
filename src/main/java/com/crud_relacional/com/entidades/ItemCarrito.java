package com.crud_relacional.com.entidades;

import java.io.Serializable;
import java.math.BigDecimal;

public class ItemCarrito implements Serializable {

    private Long idProducto;
    private String sku;
    private String titulo;
    private String formato;
    private BigDecimal precioUnitario;
    private Integer cantidad;

    public ItemCarrito() {
    }

    public ItemCarrito(Long idProducto, String sku, String titulo, String formato, BigDecimal precioUnitario, Integer cantidad) {
        this.idProducto = idProducto;
        this.sku = sku;
        this.titulo = titulo;
        this.formato = formato;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
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

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
