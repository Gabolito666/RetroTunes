package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    @Query("SELECT r FROM Rol r WHERE r.nombre_rol = :nombreRol")
    Optional<Rol> findByNombreRol(@Param("nombreRol") String nombreRol);

    @Query("SELECT r FROM Rol r WHERE r.nombre_rol = :nombreRol")
    Optional<Rol> findByNombre_rol(@Param("nombreRol") String nombreRol);
}
