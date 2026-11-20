package com.crud_relacional.com.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id_usuario;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Column(name = "nombre_completo", nullable = false)
    private String nombre_completo;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Column(name = "correo", unique = true, nullable = false)
    private String correo;

    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 6, message = "La clave debe tener al menos 6 caracteres")
    @Column(name = "clave_usuario", nullable = false)
    private String clave_usuario;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "razon_social")
    private String razon_social;

    @Column(name = "nit")
    private String nit;

    @Column(name = "codigo_empleado")
    private String codigo_empleado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    public Usuario() {
    }

    public Usuario(String nombre_completo, String correo, String clave_usuario, String telefono, Boolean activo, Rol rol) {
        this.nombre_completo = nombre_completo;
        this.correo = correo;
        this.clave_usuario = clave_usuario;
        this.telefono = telefono;
        this.activo = activo;
        this.rol = rol;
    }

    public Usuario(String nombre_completo, String correo, String clave_usuario, String telefono, Boolean activo, Rol rol, String razon_social, String nit, String codigo_empleado) {
        this(nombre_completo, correo, clave_usuario, telefono, activo, rol);
        this.razon_social = razon_social;
        this.nit = nit;
        this.codigo_empleado = codigo_empleado;
    }

    public Long getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(Long id_usuario) {
        this.id_usuario = id_usuario;
    }

    public Long getIdUsuario() {
        return id_usuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.id_usuario = idUsuario;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public String getNombreCompleto() {
        return nombre_completo;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombre_completo = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getClave_usuario() {
        return clave_usuario;
    }

    public void setClave_usuario(String clave_usuario) {
        this.clave_usuario = clave_usuario;
    }

    public String getClaveUsuario() {
        return clave_usuario;
    }

    public void setClaveUsuario(String claveUsuario) {
        this.clave_usuario = claveUsuario;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getRazon_social() {
        return razon_social;
    }

    public void setRazon_social(String razon_social) {
        this.razon_social = razon_social;
    }

    public String getRazonSocial() {
        return razon_social;
    }

    public void setRazonSocial(String razonSocial) {
        this.razon_social = razonSocial;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getCodigo_empleado() {
        return codigo_empleado;
    }

    public void setCodigo_empleado(String codigo_empleado) {
        this.codigo_empleado = codigo_empleado;
    }

    public String getCodigoEmpleado() {
        return codigo_empleado;
    }

    public void setCodigoEmpleado(String codigoEmpleado) {
        this.codigo_empleado = codigoEmpleado;
    }
}
