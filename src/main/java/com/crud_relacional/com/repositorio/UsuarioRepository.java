package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE u.correo = :correo")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    @Query("SELECT u FROM Usuario u WHERE u.correo = :correo AND u.clave_usuario = :clave")
    Optional<Usuario> findByCorreoAndClaveUsuario(@Param("correo") String correo, @Param("clave") String clave);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Usuario u WHERE u.correo = :correo")
    boolean existsByCorreo(@Param("correo") String correo);
}
