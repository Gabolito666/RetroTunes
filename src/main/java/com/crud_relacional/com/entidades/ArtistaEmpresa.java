package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Table(name = "artistas_empresas")
public class ArtistaEmpresa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Long id_entidad;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Column(name = "nombre_comercial", nullable = false)
    private String nombre_comercial;

    @Column(name = "tipo_entidad", nullable = false, length = 50)
    private String tipo_entidad; // ARTISTA / DISCOGRAFICA

    @Column(name = "biografia_descripcion", columnDefinition = "TEXT")
    private String biografia_descripcion;

    @Column(name = "sitio_web")
    private String sitio_web;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    public ArtistaEmpresa() {
    }

    public ArtistaEmpresa(String nombre_comercial, String tipo_entidad, String biografia_descripcion, String sitio_web, Usuario usuario) {
        this.nombre_comercial = nombre_comercial;
        this.tipo_entidad = tipo_entidad;
        this.biografia_descripcion = biografia_descripcion;
        this.sitio_web = sitio_web;
        this.usuario = usuario;
    }

    public Long getId_entidad() {
        return id_entidad;
    }

    public void setId_entidad(Long id_entidad) {
        this.id_entidad = id_entidad;
    }

    public String getNombre_comercial() {
        return nombre_comercial;
    }

    public void setNombre_comercial(String nombre_comercial) {
        this.nombre_comercial = nombre_comercial;
    }

    public String getTipo_entidad() {
        return tipo_entidad;
    }

    public void setTipo_entidad(String tipo_entidad) {
        this.tipo_entidad = tipo_entidad;
    }

    public String getBiografia_descripcion() {
        return biografia_descripcion;
    }

    public void setBiografia_descripcion(String biografia_descripcion) {
        this.biografia_descripcion = biografia_descripcion;
    }

    public String getSitio_web() {
        return sitio_web;
    }

    public void setSitio_web(String sitio_web) {
        this.sitio_web = sitio_web;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
