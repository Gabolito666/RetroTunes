package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.ProductoMusica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoMusicaRepository extends JpaRepository<ProductoMusica, Long> {

    @Query("SELECT p FROM ProductoMusica p WHERE " +
           "((:formato IS NULL OR :formato = '' OR :formato = 'TODOS') AND p.formato IN ('VINILO', 'CASSETTE', 'CD') OR p.formato = :formato) AND " +
           "(:artista IS NULL OR :artista = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :artista, '%'))) AND " +
           "(:precioMin IS NULL OR p.precio_venta >= :precioMin) AND " +
           "(:precioMax IS NULL OR p.precio_venta <= :precioMax) " +
           "ORDER BY p.id_producto ASC")
    List<ProductoMusica> buscarDinamico(
            @Param("formato") String formato,
            @Param("artista") String artista,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax
    );

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

    List<ProductoMusica> findByArtista(com.crud_relacional.com.entidades.ArtistaEmpresa artista);

    @Query("SELECT p FROM ProductoMusica p LEFT JOIN p.artista a WHERE " +
           "(:formato IS NULL OR :formato = '' OR :formato = 'TODOS' OR UPPER(p.formato) = UPPER(:formato) OR (UPPER(:formato) = 'EQUIPO' AND UPPER(p.formato) = 'EQUIPO_SONIDO')) AND " +
           "(:artista IS NULL OR :artista = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :artista, '%')) OR (a IS NOT NULL AND LOWER(a.nombre_comercial) LIKE LOWER(CONCAT('%', :artista, '%')))) AND " +
           "(:discografica IS NULL OR :discografica = '' OR (a IS NOT NULL AND LOWER(a.nombre_comercial) LIKE LOWER(CONCAT('%', :discografica, '%')))) AND " +
           "(:stockMin IS NULL OR p.stock >= :stockMin) AND " +
           "(:stockMax IS NULL OR p.stock <= :stockMax) " +
           "ORDER BY p.id_producto ASC")
    List<ProductoMusica> filtrarInventarioReporte(
            @Param("formato") String formato,
            @Param("artista") String artista,
            @Param("discografica") String discografica,
            @Param("stockMin") Integer stockMin,
            @Param("stockMax") Integer stockMax
    );
}
