package com.crud_relacional.com.repositorio;

import com.crud_relacional.com.entidades.ReservaEspacio;
import com.crud_relacional.com.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaEspacioRepository extends JpaRepository<ReservaEspacio, Long> {

    List<ReservaEspacio> findByCliente(Usuario cliente);

    @Query("SELECT r FROM ReservaEspacio r WHERE r.fecha_reserva = :fecha")
    List<ReservaEspacio> findByFechaReserva(@Param("fecha") LocalDate fecha);
}
