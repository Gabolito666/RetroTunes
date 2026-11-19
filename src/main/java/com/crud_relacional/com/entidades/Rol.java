package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Table(name = "roles")
public class Rol implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id_rol;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Column(name = "nombre_rol", unique = true, nullable = false, length = 50)
    private String nombre_rol;

    public Rol() {
    }

    public Rol(String nombre_rol) {
        this.nombre_rol = nombre_rol;
    }

    public Rol(Long id_rol, String nombre_rol) {
        this.id_rol = id_rol;
        this.nombre_rol = nombre_rol;
    }

    public Long getId_rol() {
        return id_rol;
    }

    public void setId_rol(Long id_rol) {
        this.id_rol = id_rol;
    }

    public String getNombre_rol() {
        return nombre_rol;
    }

    public void setNombre_rol(String nombre_rol) {
        this.nombre_rol = nombre_rol;
    }

    // Alias camelCase para compatibilidad SpEL / Thymeleaf (sessionScope.usuario.rol.nombreRol)
    public String getNombreRol() {
        return nombre_rol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombre_rol = nombreRol;
    }
}
