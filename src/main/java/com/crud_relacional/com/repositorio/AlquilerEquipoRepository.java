package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.AlquilerEquipo;
import com.crud_relacional.com.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlquilerEquipoRepository extends JpaRepository<AlquilerEquipo, Long> {

    List<AlquilerEquipo> findByCliente(Usuario cliente);

    @Query("SELECT a FROM AlquilerEquipo a WHERE a.codigo_alquiler = :codigoAlquiler")
    Optional<AlquilerEquipo> findByCodigoAlquiler(@Param("codigoAlquiler") String codigoAlquiler);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AlquilerEquipo a WHERE a.codigo_alquiler = :codigoAlquiler")
    boolean existsByCodigoAlquiler(@Param("codigoAlquiler") String codigoAlquiler);
}
