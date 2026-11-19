package com.crud_relacional.com.controlador;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportePdfHelper {

    private static final Color PRIMARY_DARK = new Color(24, 24, 27);
    private static final Color BORDER_COLOR = new Color(228, 228, 231);
    private static final Color ALT_ROW_BG = new Color(248, 250, 252);
    private static final Color BRAND_BLUE = new Color(14, 116, 144);

    public static byte[] generarReporteVentasPorTipo(List<AdminController.VentaPorTipoDto> datos) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        agregarEncabezado(doc, "REPORTE DE VENTAS POR TIPO DE PRODUCTO", "Consolidado por formatos físicos: Vinilos, Cassettes, CDs y Equipos de Sonido");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 2.0f, 1.5f});

        String[] headers = {"FORMATO FÍSICO", "ITEMS VENDIDOS", "TOTAL RECAUDADO", "% APORTE"};
        agregarEncabezadosTabla(table, headers);

        BigDecimal sumaTotal = datos.stream()
                .map(AdminController.VentaPorTipoDto::totalRecaudado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean alt = false;
        long totalItems = 0;
        for (AdminController.VentaPorTipoDto d : datos) {
            Color bg = alt ? ALT_ROW_BG : Color.WHITE;
            alt = !alt;
            totalItems += d.cantidadVendida();

            double pct = sumaTotal.compareTo(BigDecimal.ZERO) > 0 
                    ? d.totalRecaudado().divide(sumaTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100 
                    : 0.0;

            table.addCell(crearCelda(d.formato(), Element.ALIGN_LEFT, bg, true));
            table.addCell(crearCelda(String.valueOf(d.cantidadVendida()), Element.ALIGN_CENTER, bg, false));
            table.addCell(crearCelda("$" + d.totalRecaudado().setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, bg, false));
            table.addCell(crearCelda(String.format("%.1f%%", pct), Element.ALIGN_RIGHT, bg, false));
        }

        // Fila Total
        table.addCell(crearCelda("TOTAL CONSOLIDADO", Element.ALIGN_LEFT, new Color(241, 245, 249), true));
        table.addCell(crearCelda(String.valueOf(totalItems), Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("$" + sumaTotal.setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("100.0%", Element.ALIGN_RIGHT, new Color(241, 245, 249), true));

        doc.add(table);
        agregarPie(doc);
        doc.close();
        return baos.toByteArray();
    }

    public static byte[] generarReporteVentasPorUsuario(List<AdminController.VentaPorUsuarioDto> datos) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        agregarEncabezado(doc, "REPORTE CONSOLIDADO DE VENTAS POR USUARIO", "Historial acumulado de compras, frecuencia y facturación por cliente");

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2.5f, 1.0f, 1.0f, 1.8f, 1.8f});

        String[] headers = {"CLIENTE", "CORREO ELECTRÓNICO", "ÓRDENES", "ITEMS", "TOTAL COMPRADO", "ÚLTIMA COMPRA"};
        agregarEncabezadosTabla(table, headers);

        boolean alt = false;
        BigDecimal sumaTotal = BigDecimal.ZERO;
        for (AdminController.VentaPorUsuarioDto u : datos) {
            Color bg = alt ? ALT_ROW_BG : Color.WHITE;
            alt = !alt;
            sumaTotal = sumaTotal.add(u.totalGastado());

            table.addCell(crearCelda(u.nombreCliente(), Element.ALIGN_LEFT, bg, true));
            table.addCell(crearCelda(u.correo(), Element.ALIGN_LEFT, bg, false));
            table.addCell(crearCelda(String.valueOf(u.totalFacturas()), Element.ALIGN_CENTER, bg, false));
            table.addCell(crearCelda(String.valueOf(u.totalItems()), Element.ALIGN_CENTER, bg, false));
            table.addCell(crearCelda("$" + u.totalGastado().setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, bg, true));
            table.addCell(crearCelda(u.ultimaCompra(), Element.ALIGN_CENTER, bg, false));
        }

        // Fila Total
        table.addCell(crearCelda("TOTAL AUDITORÍA CLIENTES", Element.ALIGN_LEFT, new Color(241, 245, 249), true));
        table.addCell(crearCelda(datos.size() + " clientes", Element.ALIGN_LEFT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("", Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("", Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("$" + sumaTotal.setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("-", Element.ALIGN_CENTER, new Color(241, 245, 249), true));

        doc.add(table);
        agregarPie(doc);
        doc.close();
        return baos.toByteArray();
    }

    public static byte[] generarReporteAlquileres(List<AdminController.AlquilerReporteDto> datos) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36); // Horizontal para mostrar columnas completas
        PdfWriter.getInstance(doc, baos);
        doc.open();

        agregarEncabezado(doc, "REPORTE COMPLETO DE ALQUILERES DE SONIDO", "Auditoría de equipos rentados, períodos contractuales, clientes y depósitos en custodia");

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2.5f, 2.0f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f});

        String[] headers = {"CÓDIGO", "EQUIPO DE AUDIO", "CLIENTE", "INICIO", "FIN", "MONTO RENTA", "DEPÓSITO (30%)", "ESTADO"};
        agregarEncabezadosTabla(table, headers);

        boolean alt = false;
        BigDecimal sumaRenta = BigDecimal.ZERO;
        BigDecimal sumaDepositos = BigDecimal.ZERO;

        for (AdminController.AlquilerReporteDto a : datos) {
            Color bg = alt ? ALT_ROW_BG : Color.WHITE;
            alt = !alt;
            sumaRenta = sumaRenta.add(a.montoAlquiler());
            sumaDepositos = sumaDepositos.add(a.depositoGarantia());

            table.addCell(crearCelda(a.codigo(), Element.ALIGN_LEFT, bg, true));
            table.addCell(crearCelda(a.equipoTitulo(), Element.ALIGN_LEFT, bg, false));
            table.addCell(crearCelda(a.clienteNombre(), Element.ALIGN_LEFT, bg, false));
            table.addCell(crearCelda(a.fechaInicio().toString(), Element.ALIGN_CENTER, bg, false));
            table.addCell(crearCelda(a.fechaFin().toString(), Element.ALIGN_CENTER, bg, false));
            table.addCell(crearCelda("$" + a.montoAlquiler().setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, bg, false));
            table.addCell(crearCelda("$" + a.depositoGarantia().setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, bg, false));
            table.addCell(crearCelda(a.estado(), Element.ALIGN_CENTER, bg, true));
        }

        // Fila Total
        table.addCell(crearCelda("TOTAL CONSOLIDADO", Element.ALIGN_LEFT, new Color(241, 245, 249), true));
        table.addCell(crearCelda(datos.size() + " contratos", Element.ALIGN_LEFT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("-", Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("-", Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("-", Element.ALIGN_CENTER, new Color(241, 245, 249), true));
        table.addCell(crearCelda("$" + sumaRenta.setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("$" + sumaDepositos.setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT, new Color(241, 245, 249), true));
        table.addCell(crearCelda("-", Element.ALIGN_CENTER, new Color(241, 245, 249), true));

        doc.add(table);
        agregarPie(doc);
        doc.close();
        return baos.toByteArray();
    }

    private static void agregarEncabezado(Document doc, String titulo, String subtitulo) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_DARK);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(100, 116, 139));
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND_BLUE);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.0f, 2.0f});

        PdfPCell cLeft = new PdfPCell();
        cLeft.setBorder(Rectangle.NO_BORDER);
        cLeft.addElement(new Paragraph("RETROTUNES AUDITORÍA & GESTIÓN", brandFont));
        cLeft.addElement(new Paragraph("NIT: 900.876.543-1 • Bucaramanga, Santander, Colombia", subFont));
        cLeft.addElement(new Paragraph(titulo, titleFont));
        cLeft.addElement(new Paragraph(subtitulo, subFont));

        PdfPCell cRight = new PdfPCell();
        cRight.setBorder(Rectangle.NO_BORDER);
        cRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pFecha = new Paragraph("Emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), subFont);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pEstado = new Paragraph("ESTADO: DATOS OFICIALES SUPABASE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(22, 101, 52)));
        pEstado.setAlignment(Element.ALIGN_RIGHT);
        cRight.addElement(pFecha);
        cRight.addElement(pEstado);

        header.addCell(cLeft);
        header.addCell(cRight);
        header.setSpacingAfter(12);
        doc.add(header);
    }

    private static void agregarEncabezadosTabla(PdfPTable table, String[] headers) {
        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, hFont));
            c.setBackgroundColor(PRIMARY_DARK);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(c);
        }
    }

    private static PdfPCell crearCelda(String texto, int align, Color bg, boolean bold) {
        Font font = bold 
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, PRIMARY_DARK)
                : FontFactory.getFont(FontFactory.HELVETICA, 8.0f, new Color(39, 39, 42));

        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_COLOR);
        c.setPadding(4);
        return c;
    }

    private static void agregarPie(Document doc) throws Exception {
        Paragraph footer = new Paragraph("\nDocumento generado electrónicamente por el sistema contable y de auditoría de RetroTunes. Bucaramanga, Colombia.",
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(148, 163, 184)));
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }
}
