package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.entidades.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByCliente(Usuario cliente);

    @Query("SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.cliente = :cliente ORDER BY v.fecha_hora DESC")
    List<Venta> findByClienteConDetallesOrderByFechaHoraDesc(@Param("cliente") Usuario cliente);

    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.id_venta = :idVenta")
    Optional<Venta> findByIdConDetalles(@Param("idVenta") Long idVenta);

    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.codigo_factura = :codigoFactura")
    Optional<Venta> findByCodigoFactura(@Param("codigoFactura") String codigoFactura);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Venta v WHERE v.codigo_factura = :codigoFactura")
    boolean existsByCodigoFactura(@Param("codigoFactura") String codigoFactura);

    @Query("SELECT COALESCE(SUM(v.monto_total), 0) FROM Venta v")
    BigDecimal sumTotalVentas();

    @Query("SELECT d.producto.formato, SUM(d.cantidad), SUM(d.subtotal) FROM DetalleVenta d GROUP BY d.producto.formato ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> findVentasPorFormato();
}
