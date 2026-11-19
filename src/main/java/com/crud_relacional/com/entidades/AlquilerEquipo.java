package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "alquileres_equipo")
public class AlquilerEquipo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alquiler")
    private Long id_alquiler;

    @NotBlank
    @Column(name = "codigo_alquiler", unique = true, nullable = false, length = 60)
    private String codigo_alquiler;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fecha_inicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fecha_fin;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "deposito_garantia", precision = 10, scale = 2, nullable = false)
    private BigDecimal deposito_garantia;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "monto_alquiler", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto_alquiler;

    @NotBlank
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "RESERVADO"; // RESERVADO, EN_USO, DEVUELTO, CON_NOVEDAD

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private ProductoMusica equipo;

    public AlquilerEquipo() {
    }

    public AlquilerEquipo(String codigo_alquiler, LocalDate fecha_inicio, LocalDate fecha_fin, BigDecimal deposito_garantia, BigDecimal monto_alquiler, String estado, Usuario cliente, ProductoMusica equipo) {
        this.codigo_alquiler = codigo_alquiler;
        this.fecha_inicio = fecha_inicio;
        this.fecha_fin = fecha_fin;
        this.deposito_garantia = deposito_garantia;
        this.monto_alquiler = monto_alquiler;
        this.estado = estado;
        this.cliente = cliente;
        this.equipo = equipo;
    }

    public Long getId_alquiler() {
        return id_alquiler;
    }

    public void setId_alquiler(Long id_alquiler) {
        this.id_alquiler = id_alquiler;
    }

    public String getCodigo_alquiler() {
        return codigo_alquiler;
    }

    public void setCodigo_alquiler(String codigo_alquiler) {
        this.codigo_alquiler = codigo_alquiler;
    }

    public LocalDate getFecha_inicio() {
        return fecha_inicio;
    }

    public void setFecha_inicio(LocalDate fecha_inicio) {
        this.fecha_inicio = fecha_inicio;
    }

    public LocalDate getFecha_fin() {
        return fecha_fin;
    }

    public void setFecha_fin(LocalDate fecha_fin) {
        this.fecha_fin = fecha_fin;
    }

    public BigDecimal getDeposito_garantia() {
        return deposito_garantia;
    }

    public void setDeposito_garantia(BigDecimal deposito_garantia) {
        this.deposito_garantia = deposito_garantia;
    }

    public BigDecimal getMonto_alquiler() {
        return monto_alquiler;
    }

    public void setMonto_alquiler(BigDecimal monto_alquiler) {
        this.monto_alquiler = monto_alquiler;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public ProductoMusica getEquipo() {
        return equipo;
    }

    public void setEquipo(ProductoMusica equipo) {
        this.equipo = equipo;
    }
}
