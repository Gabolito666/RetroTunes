package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "reservas_espacio")
public class ReservaEspacio implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id_reserva;

    @NotNull
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fecha_reserva;

    @NotBlank
    @Column(name = "bloque_horario", nullable = false, length = 50)
    private String bloque_horario; // 09:00 - 12:00, 14:00 - 17:00, 18:00 - 21:00

    @Column(name = "google_event_id")
    private String google_event_id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario cliente;

    public ReservaEspacio() {
    }

    public ReservaEspacio(LocalDate fecha_reserva, String bloque_horario, String google_event_id, Usuario cliente) {
        this.fecha_reserva = fecha_reserva;
        this.bloque_horario = bloque_horario;
        this.google_event_id = google_event_id;
        this.cliente = cliente;
    }

    public Long getId_reserva() {
        return id_reserva;
    }

    public void setId_reserva(Long id_reserva) {
        this.id_reserva = id_reserva;
    }

    public LocalDate getFecha_reserva() {
        return fecha_reserva;
    }

    public void setFecha_reserva(LocalDate fecha_reserva) {
        this.fecha_reserva = fecha_reserva;
    }

    public String getBloque_horario() {
        return bloque_horario;
    }

    public void setBloque_horario(String bloque_horario) {
        this.bloque_horario = bloque_horario;
    }

    public String getGoogle_event_id() {
        return google_event_id;
    }

    public void setGoogle_event_id(String google_event_id) {
        this.google_event_id = google_event_id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }
}
