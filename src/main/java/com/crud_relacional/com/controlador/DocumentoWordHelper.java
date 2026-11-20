package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.AlquilerEquipo;
import com.crud_relacional.com.entidades.Usuario;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentoWordHelper {

    public static byte[] generarContratoAlquiler(AlquilerEquipo alq) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Título
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitle = title.createRun();
            rTitle.setText("CONTRATO DE ALQUILER Y COMODATO DE EQUIPO ACÚSTICO");
            rTitle.setBold(true);
            rTitle.setFontSize(14);
            rTitle.setFontFamily("Arial");

            XWPFParagraph code = doc.createParagraph();
            code.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rCode = code.createRun();
            rCode.setText("CÓDIGO DE RESERVA: " + alq.getCodigo_alquiler());
            rCode.setItalic(true);
            rCode.setFontSize(10);
            rCode.setColor("666666");

            // Cláusulas
            agregarParrafo(doc, "Entre los suscritos, a saber RETROTUNES ARCHIVO ANALÓGICO S.A.S. (en adelante EL ARRENDADOR) y el usuario " 
                    + alq.getCliente().getNombre_completo().toUpperCase() + " con correo " + alq.getCliente().getCorreo() 
                    + " (en adelante EL ARRENDATARIO), se celebra el presente contrato bajo las siguientes condiciones:");

            agregarParrafo(doc, "PRIMERA - OBJETO: EL ARRENDADOR entrega a título de alquiler el equipo de alta fidelidad: " 
                    + alq.getEquipo().getTitulo() + " (SKU: " + alq.getEquipo().getSku() + "), en perfecto estado funcional y cosmético.");

            agregarParrafo(doc, "SEGUNDA - VIGENCIA Y TARIFAS: El período pactado de uso rige desde el " 
                    + alq.getFecha_inicio().format(DateTimeFormatter.ISO_DATE) + " hasta el " 
                    + alq.getFecha_fin().format(DateTimeFormatter.ISO_DATE) + ". El canon total de alquiler asciende a $" 
                    + alq.getMonto_alquiler() + " COP/USD.");

            agregarParrafo(doc, "TERCERA - DEPÓSITO DE GARANTÍA (30%): EL ARRENDATARIO consigna la suma de $" 
                    + alq.getDeposito_garantia() + " correspondiente al 30% del valor comercial de reposición ($" 
                    + alq.getEquipo().getPrecio_venta() + "). Dicho depósito será liberado por el Técnico de Operaciones tras la devolución y verificación técnica sin novedades.");

            agregarParrafo(doc, "CUARTA - ESTADO Y NOVEDADES: El estado inicial registrado es: " + alq.getEstado() 
                    + ". Cualquier daño en aguja, cápsula, correas o circuito será deducido del depósito.");

            // Firmas
            XWPFParagraph pFirmas = doc.createParagraph();
            pFirmas.setSpacingBefore(800);
            XWPFRun rF = pFirmas.createRun();
            rF.setText("_____________________________                  _____________________________\n"
                     + "POR RETROTUNES ARCHIVO                         EL ARRENDATARIO / CLIENTE\n"
                     + "CC/NIT: 900.876.543-1                          CC/ID: " + alq.getCliente().getId_usuario());
            rF.setFontSize(10);

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarLiquidacionRegalias(Usuario artista, BigDecimal totalVentas, BigDecimal montoRegalia) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitle = title.createRun();
            rTitle.setText("ACTA DE LIQUIDACIÓN DE REGALÍAS POR CONSIGNACIÓN");
            rTitle.setBold(true);
            rTitle.setFontSize(14);
            rTitle.setFontFamily("Arial");

            agregarParrafo(doc, "FECHA DE LIQUIDACIÓN: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            agregarParrafo(doc, "BENEFICIARIO: " + artista.getNombre_completo() + " (" + artista.getCorreo() + ")");
            agregarParrafo(doc, "ROL DE COBERTURA: " + (artista.getRol() != null ? artista.getRol().getNombre_rol() : "ARTISTA"));

            agregarParrafo(doc, "INFORME FINANCIERO: Durante el ciclo actual de ventas analógicas en tienda y plataforma web, las piezas asociadas acumularon ventas brutas por $" 
                    + totalVentas.setScale(2) + " COP/USD.");

            agregarParrafo(doc, "DIVISIÓN PACTADA: Conforme al contrato marco de distribución independiente, corresponde el 70% neto a favor del Artista/Discográfica ($" 
                    + montoRegalia.setScale(2) + ") y el 30% a gastos de administración, curaduría y empaque analógico de RetroTunes.");

            agregarParrafo(doc, "ESTADO: Aprobado para giro bancario y transferencia inmediata.");

            XWPFParagraph pFirmas = doc.createParagraph();
            pFirmas.setSpacingBefore(600);
            XWPFRun rF = pFirmas.createRun();
            rF.setText("_____________________________                  _____________________________\n"
                     + "DIRECCIÓN FINANCIERA RETROTUNES                REPRESENTANTE / ARTISTA\n");
            rF.setFontSize(10);

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] generarEstadoDeCuentaWord(Usuario artista, 
                                                   java.util.List<com.crud_relacional.com.entidades.Venta> ventas, 
                                                   BigDecimal totalVentas, 
                                                   BigDecimal totalRegalias) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Membrete Oficial RetroTunes
            XWPFParagraph header = doc.createParagraph();
            header.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rHeader = header.createRun();
            rHeader.setText("RETROTUNES ARCHIVO ANALÓGICO & DISTRIBUCIÓN MUSICAL");
            rHeader.setBold(true);
            rHeader.setFontSize(16);
            rHeader.setFontFamily("Arial");
            rHeader.setColor("222222");

            XWPFParagraph subHeader = doc.createParagraph();
            subHeader.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rSub = subHeader.createRun();
            rSub.setText("ESTADO DE CUENTA OFICIAL DE VENTAS FÍSICAS Y REGALÍAS");
            rSub.setBold(true);
            rSub.setFontSize(12);
            rSub.setColor("555555");
            subHeader.setSpacingAfter(300);

            // Información del Beneficiario
            agregarParrafo(doc, "FECHA DE EMISIÓN: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            agregarParrafo(doc, "ARTISTA / TITULAR: " + artista.getNombre_completo() + " (" + artista.getCorreo() + ")");
            agregarParrafo(doc, "ENTIDAD REGISTRADA: " + (artista.getRol() != null ? artista.getRol().getNombre_rol() : "ARTISTA"));
            agregarParrafo(doc, "MODELO DE DISTRIBUCIÓN: 70% Regalía Neta Creador / 30% Custodia y Tienda RetroTunes");

            // Tabla de Desglose de Ventas Físicas
            XWPFTable table = doc.createTable();
            table.setWidth("100%");

            // Fila de encabezado
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("Fecha");
            headerRow.addNewTableCell().setText("Factura");
            headerRow.addNewTableCell().setText("Artículos / Formatos");
            headerRow.addNewTableCell().setText("Uds");
            headerRow.addNewTableCell().setText("Total Venta");
            headerRow.addNewTableCell().setText("Regalía (70%)");

            int totalUnidades = 0;
            if (ventas != null) {
                for (com.crud_relacional.com.entidades.Venta v : ventas) {
                    XWPFTableRow row = table.createRow();
                    row.getCell(0).setText(v.getFecha_hora() != null ? v.getFecha_hora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
                    row.getCell(1).setText(v.getCodigo_factura() != null ? v.getCodigo_factura() : "-");

                    StringBuilder articulos = new StringBuilder();
                    int cantVenta = 0;
                    if (v.getDetalles() != null) {
                        for (com.crud_relacional.com.entidades.DetalleVenta d : v.getDetalles()) {
                            if (d.getProducto() != null) {
                                articulos.append(d.getProducto().getTitulo()).append(" (").append(d.getProducto().getFormato()).append("), ");
                            }
                            cantVenta += d.getCantidad();
                        }
                    }
                    totalUnidades += cantVenta;

                    String artStr = articulos.toString();
                    if (artStr.endsWith(", ")) artStr = artStr.substring(0, artStr.length() - 2);
                    row.getCell(2).setText(artStr.isEmpty() ? "Música Analógica" : artStr);
                    row.getCell(3).setText(String.valueOf(cantVenta));

                    BigDecimal montoVenta = v.getMonto_total() != null ? v.getMonto_total() : BigDecimal.ZERO;
                    BigDecimal regaliaVenta = montoVenta.multiply(new BigDecimal("0.70")).setScale(2, java.math.RoundingMode.HALF_UP);

                    row.getCell(4).setText("$" + montoVenta.setScale(2));
                    row.getCell(5).setText("$" + regaliaVenta.setScale(2));
                }
            }

            // Resumen Financiero
            XWPFParagraph pResumen = doc.createParagraph();
            pResumen.setSpacingBefore(300);
            agregarParrafo(doc, "TOTAL DE PIEZAS FÍSICAS VENDIDAS: " + totalUnidades + " unidades");
            agregarParrafo(doc, "RECAUDACIÓN BRUTA TOTAL: $" + totalVentas.setScale(2) + " COP/USD");
            agregarParrafo(doc, "COMISIÓN ADMINISTRATIVA Y LOGÍSTICA (30%): $" + totalVentas.subtract(totalRegalias).setScale(2) + " COP/USD");
            agregarParrafo(doc, "TOTAL NETO A LIQUIDAR AL ARTISTA (70%): $" + totalRegalias.setScale(2) + " COP/USD");

            // Firmas Oficiales
            XWPFParagraph pFirmas = doc.createParagraph();
            pFirmas.setSpacingBefore(700);
            XWPFRun rF = pFirmas.createRun();
            rF.setText("_____________________________                  _____________________________\n"
                     + "GERENCIA COMERCIAL RETROTUNES                  FIRMA DEL ARTISTA / APODERADO\n"
                     + "NIT: 900.876.543-1                             ID: " + artista.getId_usuario());
            rF.setFontSize(10);

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    private static void agregarParrafo(XWPFDocument doc, String texto) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(200);
        XWPFRun r = p.createRun();
        r.setText(texto);
        r.setFontSize(10);
        r.setFontFamily("Arial");
    }
}
