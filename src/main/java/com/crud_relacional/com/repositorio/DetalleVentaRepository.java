package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Query("SELECT d FROM DetalleVenta d " +
           "JOIN FETCH d.venta v " +
           "JOIN FETCH v.cliente c " +
           "JOIN FETCH d.producto p " +
           "LEFT JOIN FETCH p.artista a " +
           "WHERE (:usuario IS NULL OR :usuario = '' OR LOWER(c.nombre_completo) LIKE LOWER(CONCAT('%', :usuario, '%')) OR LOWER(c.correo) LIKE LOWER(CONCAT('%', :usuario, '%'))) " +
           "AND (:producto IS NULL OR :producto = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :producto, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :producto, '%'))) " +
           "AND (:discografica IS NULL OR :discografica = '' OR (a IS NOT NULL AND LOWER(a.nombre_comercial) LIKE LOWER(CONCAT('%', :discografica, '%')))) " +
           "AND (:artista IS NULL OR :artista = '' OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :artista, '%')) OR (a IS NOT NULL AND LOWER(a.nombre_comercial) LIKE LOWER(CONCAT('%', :artista, '%')))) " +
           "AND (:cantMin IS NULL OR d.cantidad >= :cantMin) " +
           "AND (:cantMax IS NULL OR d.cantidad <= :cantMax) " +
           "ORDER BY v.fecha_hora DESC, d.id_detalle DESC")
    List<DetalleVenta> filtrarComprasDetalladas(
            @Param("usuario") String usuario,
            @Param("producto") String producto,
            @Param("discografica") String discografica,
            @Param("artista") String artista,
            @Param("cantMin") Integer cantMin,
            @Param("cantMax") Integer cantMax
    );
}
