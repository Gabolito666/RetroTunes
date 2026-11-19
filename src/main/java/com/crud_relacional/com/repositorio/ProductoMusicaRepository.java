package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.ProductoMusica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoMusicaRepository extends JpaRepository<ProductoMusica, Long> {

    @Query("SELECT p FROM ProductoMusica p WHERE p.formato = :formato ORDER BY p.id_producto ASC")
    List<ProductoMusica> findByFormato(@Param("formato") String formato);

    @Query("SELECT p FROM ProductoMusica p WHERE p.formato IN ('VINILO', 'CASSETTE', 'CD') ORDER BY p.id_producto ASC")
    List<ProductoMusica> findSoloMusica();

    @Query("SELECT p FROM ProductoMusica p WHERE p.formato = 'EQUIPO_SONIDO' ORDER BY p.id_producto ASC")
    List<ProductoMusica> findSoloEquipos();

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProductoMusica p WHERE p.sku = :sku")
    boolean existsBySku(@Param("sku") String sku);

    Optional<ProductoMusica> findBySku(String sku);

    Optional<ProductoMusica> findByTitulo(String titulo);

    List<ProductoMusica> findByTituloContainingIgnoreCase(String query);
}
