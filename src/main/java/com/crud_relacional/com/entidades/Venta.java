package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id_venta;

    @NotBlank
    @Column(name = "codigo_factura", unique = true, nullable = false, length = 60)
    private String codigo_factura;

    @NotNull
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fecha_hora;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "monto_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto_total;

    @NotBlank
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodo_pago;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario cliente;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
    }

    public Venta(String codigo_factura, LocalDateTime fecha_hora, BigDecimal monto_total, String metodo_pago, Usuario cliente) {
        this.codigo_factura = codigo_factura;
        this.fecha_hora = fecha_hora;
        this.monto_total = monto_total;
        this.metodo_pago = metodo_pago;
        this.cliente = cliente;
    }

    public Long getId_venta() {
        return id_venta;
    }

    public void setId_venta(Long id_venta) {
        this.id_venta = id_venta;
    }

    public String getCodigo_factura() {
        return codigo_factura;
    }

    public void setCodigo_factura(String codigo_factura) {
        this.codigo_factura = codigo_factura;
    }

    public LocalDateTime getFecha_hora() {
        return fecha_hora;
    }

    public void setFecha_hora(LocalDateTime fecha_hora) {
        this.fecha_hora = fecha_hora;
    }

    public BigDecimal getMonto_total() {
        return monto_total;
    }

    public void setMonto_total(BigDecimal monto_total) {
        this.monto_total = monto_total;
    }

    public String getMetodo_pago() {
        return metodo_pago;
    }

    public void setMetodo_pago(String metodo_pago) {
        this.metodo_pago = metodo_pago;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public void agregarDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }
}
