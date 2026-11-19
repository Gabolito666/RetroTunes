package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_venta")
public class DetalleVenta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id_detalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private ProductoMusica producto;

    @NotNull
    @Min(1)
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "precio_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio_unitario;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    public DetalleVenta() {
    }

    public DetalleVenta(Venta venta, ProductoMusica producto, Integer cantidad, BigDecimal precio_unitario, BigDecimal subtotal) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precio_unitario = precio_unitario;
        this.subtotal = subtotal;
    }

    public Long getId_detalle() {
        return id_detalle;
    }

    public void setId_detalle(Long id_detalle) {
        this.id_detalle = id_detalle;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public ProductoMusica getProducto() {
        return producto;
    }

    public void setProducto(ProductoMusica producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecio_unitario() {
        return precio_unitario;
    }

    public void setPrecio_unitario(BigDecimal precio_unitario) {
        this.precio_unitario = precio_unitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
