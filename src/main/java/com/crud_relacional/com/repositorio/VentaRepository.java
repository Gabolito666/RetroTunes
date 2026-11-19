package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.entidades.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByCliente(Usuario cliente);

    @Query("SELECT v FROM Venta v WHERE v.codigo_factura = :codigoFactura")
    Optional<Venta> findByCodigoFactura(@Param("codigoFactura") String codigoFactura);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Venta v WHERE v.codigo_factura = :codigoFactura")
    boolean existsByCodigoFactura(@Param("codigoFactura") String codigoFactura);
}
