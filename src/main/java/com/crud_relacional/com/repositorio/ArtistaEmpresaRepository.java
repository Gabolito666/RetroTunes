package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.ArtistaEmpresa;
import com.crud_relacional.com.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtistaEmpresaRepository extends JpaRepository<ArtistaEmpresa, Long> {
    Optional<ArtistaEmpresa> findByUsuario(Usuario usuario);
}
