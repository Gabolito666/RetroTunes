package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.DetalleVenta;
import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.entidades.Venta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReporteExcelHelper {

    public static byte[] generarReporteInventarioYVentas(List<ProductoMusica> productos, List<Venta> ventas) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            crearPestanaInventario(workbook, "Inventario General", productos, headerStyle, currencyStyle);
            crearPestanaInventario(workbook, "Vinilos", productos.stream().filter(p -> "VINILO".equalsIgnoreCase(p.getFormato())).toList(), headerStyle, currencyStyle);
            crearPestanaInventario(workbook, "Cassettes", productos.stream().filter(p -> "CASSETTE".equalsIgnoreCase(p.getFormato())).toList(), headerStyle, currencyStyle);
            crearPestanaInventario(workbook, "CDs", productos.stream().filter(p -> "CD".equalsIgnoreCase(p.getFormato())).toList(), headerStyle, currencyStyle);
            crearPestanaInventario(workbook, "Equipos de Sonido", productos.stream().filter(p -> "EQUIPO_SONIDO".equalsIgnoreCase(p.getFormato())).toList(), headerStyle, currencyStyle);

            crearPestanaHistoricoVentas(workbook, ventas, headerStyle, currencyStyle);

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarReporteVentasPorTipoExcel(List<AdminController.VentaPorTipoDto> datos) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            crearPestanaVentasPorTipo(workbook, datos, headerStyle, currencyStyle);
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarReporteVentasPorUsuarioExcel(List<AdminController.VentaPorUsuarioDto> datos) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            crearPestanaVentasPorUsuario(workbook, datos, headerStyle, currencyStyle);
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarReporteComprasIndividualesPorClienteExcel(List<AdminController.ClienteReporteComprasDto> datos) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            Sheet sheet = workbook.createSheet("Compras Detalladas por Cliente");
            String[] headers = {
                    "Cliente", "Correo", "Teléfono", 
                    "Código Factura", "Fecha y Hora", "Método Pago",
                    "SKU", "Título Producto", "Formato", "Cantidad", "Precio Unitario", "Subtotal Ítem", "Total Factura"
            };

            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rIdx = 1;
            for (AdminController.ClienteReporteComprasDto cliente : datos) {
                if (cliente.compras() == null || cliente.compras().isEmpty()) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue(cliente.nombreCliente());
                    r.createCell(1).setCellValue(cliente.correo());
                    r.createCell(2).setCellValue(cliente.telefono());
                    r.createCell(3).setCellValue("Sin compras");
                    continue;
                }

                for (AdminController.CompraIndividualClienteDto compra : cliente.compras()) {
                    if (compra.items() == null || compra.items().isEmpty()) {
                        Row r = sheet.createRow(rIdx++);
                        r.createCell(0).setCellValue(cliente.nombreCliente());
                        r.createCell(1).setCellValue(cliente.correo());
                        r.createCell(2).setCellValue(cliente.telefono());
                        r.createCell(3).setCellValue(compra.codigoFactura());
                        r.createCell(4).setCellValue(compra.fechaFormateada());
                        r.createCell(5).setCellValue(compra.metodoPago());
                        Cell cTot = r.createCell(12);
                        cTot.setCellValue(compra.total().doubleValue());
                        cTot.setCellStyle(currencyStyle);
                        continue;
                    }

                    for (AdminController.DetalleCompraItemDto item : compra.items()) {
                        Row r = sheet.createRow(rIdx++);
                        r.createCell(0).setCellValue(cliente.nombreCliente());
                        r.createCell(1).setCellValue(cliente.correo());
                        r.createCell(2).setCellValue(cliente.telefono());
                        r.createCell(3).setCellValue(compra.codigoFactura());
                        r.createCell(4).setCellValue(compra.fechaFormateada());
                        r.createCell(5).setCellValue(compra.metodoPago());
                        r.createCell(6).setCellValue(item.sku());
                        r.createCell(7).setCellValue(item.titulo());
                        r.createCell(8).setCellValue(item.formato());
                        r.createCell(9).setCellValue(item.cantidad());

                        Cell cPrecio = r.createCell(10);
                        cPrecio.setCellValue(item.precioUnitario().doubleValue());
                        cPrecio.setCellStyle(currencyStyle);

                        Cell cSub = r.createCell(11);
                        cSub.setCellValue(item.subtotal().doubleValue());
                        cSub.setCellStyle(currencyStyle);

                        Cell cTot = r.createCell(12);
                        cTot.setCellValue(compra.total().doubleValue());
                        cTot.setCellStyle(currencyStyle);
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarReporteAlquileresExcel(List<AdminController.AlquilerReporteDto> datos) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            crearPestanaAlquileres(workbook, datos, headerStyle, currencyStyle);
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarReporteConsolidadoCompleto(
            List<AdminController.VentaPorTipoDto> ventasTipo,
            List<AdminController.VentaPorUsuarioDto> ventasUsuario,
            List<AdminController.AlquilerReporteDto> alquileres,
            List<ProductoMusica> productos) throws Exception {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle currencyStyle = crearEstiloMoneda(workbook);

            crearPestanaVentasPorTipo(workbook, ventasTipo, headerStyle, currencyStyle);
            crearPestanaVentasPorUsuario(workbook, ventasUsuario, headerStyle, currencyStyle);
            crearPestanaAlquileres(workbook, alquileres, headerStyle, currencyStyle);
            crearPestanaInventario(workbook, "Inventario General", productos, headerStyle, currencyStyle);

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private static void crearPestanaVentasPorTipo(Workbook wb, List<AdminController.VentaPorTipoDto> datos, CellStyle hStyle, CellStyle curStyle) {
        Sheet sheet = wb.createSheet("Ventas por Tipo de Producto");
        String[] headers = {"Formato Físico", "Ítems Vendidos", "Total Recaudado ($)", "% Aporte Estimado"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hStyle);
        }

        BigDecimal suma = datos.stream().map(AdminController.VentaPorTipoDto::totalRecaudado).reduce(BigDecimal.ZERO, BigDecimal::add);

        int rowIdx = 1;
        for (AdminController.VentaPorTipoDto d : datos) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(d.formato());
            r.createCell(1).setCellValue(d.cantidadVendida());

            Cell cTot = r.createCell(2);
            cTot.setCellValue(d.totalRecaudado().doubleValue());
            cTot.setCellStyle(curStyle);

            double pct = suma.compareTo(BigDecimal.ZERO) > 0 ? (d.totalRecaudado().doubleValue() / suma.doubleValue()) * 100 : 0.0;
            r.createCell(3).setCellValue(String.format("%.1f%%", pct));
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private static void crearPestanaVentasPorUsuario(Workbook wb, List<AdminController.VentaPorUsuarioDto> datos, CellStyle hStyle, CellStyle curStyle) {
        Sheet sheet = wb.createSheet("Ventas por Usuario");
        String[] headers = {"ID", "Cliente", "Correo Electrónico", "Teléfono", "Órdenes", "Ítems Comprados", "Total Facturado ($)", "Última Compra"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hStyle);
        }

        int rowIdx = 1;
        for (AdminController.VentaPorUsuarioDto u : datos) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(u.idUsuario());
            r.createCell(1).setCellValue(u.nombreCliente());
            r.createCell(2).setCellValue(u.correo());
            r.createCell(3).setCellValue(u.telefono());
            r.createCell(4).setCellValue(u.totalFacturas());
            r.createCell(5).setCellValue(u.totalItems());

            Cell cTot = r.createCell(6);
            cTot.setCellValue(u.totalGastado().doubleValue());
            cTot.setCellStyle(curStyle);

            r.createCell(7).setCellValue(u.ultimaCompra());
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private static void crearPestanaAlquileres(Workbook wb, List<AdminController.AlquilerReporteDto> datos, CellStyle hStyle, CellStyle curStyle) {
        Sheet sheet = wb.createSheet("Reporte de Alquileres");
        String[] headers = {"Código Reserva", "Equipo Rentado", "SKU", "Cliente", "Correo", "Fecha Inicio", "Fecha Fin", "Días", "Monto Alquiler ($)", "Depósito Custodia ($)", "Estado"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hStyle);
        }

        int rowIdx = 1;
        for (AdminController.AlquilerReporteDto a : datos) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(a.codigo());
            r.createCell(1).setCellValue(a.equipoTitulo());
            r.createCell(2).setCellValue(a.equipoSku());
            r.createCell(3).setCellValue(a.clienteNombre());
            r.createCell(4).setCellValue(a.clienteCorreo());
            r.createCell(5).setCellValue(a.fechaInicio().toString());
            r.createCell(6).setCellValue(a.fechaFin().toString());
            r.createCell(7).setCellValue(a.dias());

            Cell cMonto = r.createCell(8);
            cMonto.setCellValue(a.montoAlquiler().doubleValue());
            cMonto.setCellStyle(curStyle);

            Cell cDep = r.createCell(9);
            cDep.setCellValue(a.depositoGarantia().doubleValue());
            cDep.setCellStyle(curStyle);

            r.createCell(10).setCellValue(a.estado());
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private static void crearPestanaHistoricoVentas(Workbook wb, List<Venta> ventas, CellStyle hStyle, CellStyle curStyle) {
        Sheet sheet = wb.createSheet("Histórico de Ventas");
        String[] colsVentas = {"Código Factura", "Fecha y Hora", "Cliente", "Formato", "Ítem", "Cant", "P. Unitario", "Subtotal", "Total Factura", "Método Pago"};
        Row hVentas = sheet.createRow(0);
        for (int i = 0; i < colsVentas.length; i++) {
            Cell cell = hVentas.createCell(i);
            cell.setCellValue(colsVentas[i]);
            cell.setCellStyle(hStyle);
        }

        int rowIdx = 1;
        for (Venta v : ventas) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(v.getCodigo_factura());
                r.createCell(1).setCellValue(v.getFecha_hora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                r.createCell(2).setCellValue(v.getCliente() != null ? v.getCliente().getNombre_completo() : "N/A");
                r.createCell(3).setCellValue(d.getProducto() != null ? d.getProducto().getFormato() : "N/A");
                r.createCell(4).setCellValue(d.getProducto() != null ? d.getProducto().getTitulo() : "N/A");
                r.createCell(5).setCellValue(d.getCantidad());

                Cell cPrecio = r.createCell(6);
                cPrecio.setCellValue(d.getPrecio_unitario() != null ? d.getPrecio_unitario().doubleValue() : 0.0);
                cPrecio.setCellStyle(curStyle);

                Cell cSub = r.createCell(7);
                cSub.setCellValue(d.getSubtotal() != null ? d.getSubtotal().doubleValue() : 0.0);
                cSub.setCellStyle(curStyle);

                Cell cTot = r.createCell(8);
                cTot.setCellValue(v.getMonto_total() != null ? v.getMonto_total().doubleValue() : 0.0);
                cTot.setCellStyle(curStyle);

                r.createCell(9).setCellValue(v.getMetodo_pago());
            }
        }

        for (int i = 0; i < colsVentas.length; i++) sheet.autoSizeColumn(i);
    }

    private static void crearPestanaInventario(Workbook workbook, String nombre, List<ProductoMusica> lista, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet(nombre);
        String[] cols = {"ID", "SKU", "Título", "Formato", "Precio Venta", "Alquiler / Día", "Stock Actual", "Spotify Album ID"};

        Row h = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }

        int rIndex = 1;
        for (ProductoMusica p : lista) {
            Row r = sheet.createRow(rIndex++);
            r.createCell(0).setCellValue(p.getId_producto());
            r.createCell(1).setCellValue(p.getSku());
            r.createCell(2).setCellValue(p.getTitulo());
            r.createCell(3).setCellValue(p.getFormato());

            Cell cVenta = r.createCell(4);
            cVenta.setCellValue(p.getPrecio_venta().doubleValue());
            cVenta.setCellStyle(currencyStyle);

            Cell cAlq = r.createCell(5);
            cAlq.setCellValue(p.getPrecio_alquiler_dia().doubleValue());
            cAlq.setCellStyle(currencyStyle);

            r.createCell(6).setCellValue(p.getStock());
            r.createCell(7).setCellValue(p.getSpotify_album_id() != null ? p.getSpotify_album_id() : "N/A");
        }

        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
    }

    private static CellStyle crearEstiloCabecera(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font fontHeader = workbook.createFont();
        fontHeader.setBold(true);
        fontHeader.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(fontHeader);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private static CellStyle crearEstiloMoneda(Workbook workbook) {
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat df = workbook.createDataFormat();
        currencyStyle.setDataFormat(df.getFormat("$#,##0.00"));
        return currencyStyle;
    }
}
