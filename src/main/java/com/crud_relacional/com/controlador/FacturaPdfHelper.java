package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.DetalleVenta;
import com.crud_relacional.com.entidades.Venta;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class FacturaPdfHelper {

    public static byte[] generarFactura(Venta venta) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.addTitle("Factura RetroTunes - " + venta.getCodigo_factura());
        document.addSubject("Comprobante oficial de venta y facturación");
        document.addAuthor("RetroTunes - Bucaramanga, Colombia");
        document.addCreator("RetroTunes ERP");

        document.open();

        // Tipografías
        Font brandTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(24, 24, 27));
        Font brandSloganFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(14, 116, 144));
        Font metaTextFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(82, 82, 91));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(39, 39, 42));
        Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(24, 24, 27));
        Font highlightFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(14, 116, 144));

        // a) Encabezado institucional con logo y marca "RetroTunes" (NIT, Bucaramanga, Colombia)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3.2f, 2.0f});

        PdfPCell leftHeader = new PdfPCell();
        leftHeader.setBorder(Rectangle.NO_BORDER);
        leftHeader.addElement(new Paragraph("RETROTUNES", brandTitleFont));
        leftHeader.addElement(new Paragraph("ARCHIVO & TIENDA MUSICAL ANALÓGICA", brandSloganFont));
        leftHeader.addElement(new Paragraph("NIT: 900.876.543-1 • Bucaramanga, Colombia", metaTextFont));
        leftHeader.addElement(new Paragraph("Carrera 27 # 36-14, Bucaramanga, Santander\nContacto: compras@retrotunes.com • Tel: +57 (607) 634-9800", metaTextFont));

        PdfPCell rightHeader = new PdfPCell();
        rightHeader.setBorder(Rectangle.NO_BORDER);
        rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invTitle = new Paragraph("FACTURA OFICIAL DE VENTA", boldCellFont);
        invTitle.setAlignment(Element.ALIGN_RIGHT);
        Paragraph invCode = new Paragraph(venta.getCodigo_factura(), highlightFont);
        invCode.setAlignment(Element.ALIGN_RIGHT);
        Paragraph invDate = new Paragraph("Fecha: " + venta.getFecha_hora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), metaTextFont);
        invDate.setAlignment(Element.ALIGN_RIGHT);
        Paragraph invPay = new Paragraph("Método: " + venta.getMetodo_pago(), metaTextFont);
        invPay.setAlignment(Element.ALIGN_RIGHT);

        rightHeader.addElement(invTitle);
        rightHeader.addElement(invCode);
        rightHeader.addElement(invDate);
        rightHeader.addElement(invPay);

        headerTable.addCell(leftHeader);
        headerTable.addCell(rightHeader);
        document.add(headerTable);

        // Línea divisoria sutil
        Paragraph sep = new Paragraph("_______________________________________________________________________________________________", 
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(228, 228, 231)));
        sep.setSpacingAfter(10);
        document.add(sep);

        // b) Datos del cliente (Nombre, correo, fecha y código único de factura)
        PdfPTable clientBox = new PdfPTable(2);
        clientBox.setWidthPercentage(100);
        clientBox.setWidths(new float[]{1.0f, 1.0f});

        PdfPCell clientCell1 = new PdfPCell();
        clientCell1.setBackgroundColor(new Color(248, 250, 252));
        clientCell1.setBorderColor(new Color(226, 232, 240));
        clientCell1.setPadding(8);
        clientCell1.addElement(new Paragraph("INFORMACIÓN DEL CLIENTE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139))));
        clientCell1.addElement(new Paragraph("Nombre: " + (venta.getCliente() != null ? venta.getCliente().getNombre_completo() : "Cliente General"), boldCellFont));
        clientCell1.addElement(new Paragraph("Correo Electrónico: " + (venta.getCliente() != null ? venta.getCliente().getCorreo() : "N/A"), cellFont));
        clientCell1.addElement(new Paragraph("Teléfono: " + (venta.getCliente() != null && venta.getCliente().getTelefono() != null ? venta.getCliente().getTelefono() : "No registrado"), cellFont));

        PdfPCell clientCell2 = new PdfPCell();
        clientCell2.setBackgroundColor(new Color(248, 250, 252));
        clientCell2.setBorderColor(new Color(226, 232, 240));
        clientCell2.setPadding(8);
        clientCell2.addElement(new Paragraph("DETALLES FISCALES DE TRANSACCIÓN", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139))));
        clientCell2.addElement(new Paragraph("Código Único Factura: " + venta.getCodigo_factura(), boldCellFont));
        clientCell2.addElement(new Paragraph("Fecha y Hora Emisión: " + venta.getFecha_hora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), cellFont));
        clientCell2.addElement(new Paragraph("Estado de Pago: APROBADO Y REGISTRADO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(22, 101, 52))));

        clientBox.addCell(clientCell1);
        clientBox.addCell(clientCell2);
        document.add(clientBox);
        document.add(new Paragraph(" "));

        // c) Tabla detallada de items (Titulo, Formato: Vinilo/CD/Cassette/Equipo, Cantidad, Precio Unitario, Subtotal)
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.3f, 3.5f, 1.6f, 0.9f, 1.4f, 1.4f});

        Color tableHeaderBg = new Color(24, 24, 27);
        String[] headers = {"SKU", "TÍTULO / ARTÍCULO", "FORMATO", "CANT.", "PRECIO UNIT.", "SUBTOTAL"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            c.setBackgroundColor(tableHeaderBg);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        boolean alternate = false;
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta d : venta.getDetalles()) {
                Color rowBg = alternate ? new Color(248, 250, 252) : Color.WHITE;
                alternate = !alternate;

                String skuStr = d.getProducto() != null ? d.getProducto().getSku() : "SKU-N/A";
                String tituloStr = d.getProducto() != null ? d.getProducto().getTitulo() : "Producto";
                String formatoStr = d.getProducto() != null ? d.getProducto().getFormato() : "Estándar";

                PdfPCell cSku = createTableCell(skuStr, cellFont, Element.ALIGN_LEFT, rowBg);
                PdfPCell cTit = createTableCell(tituloStr, boldCellFont, Element.ALIGN_LEFT, rowBg);
                PdfPCell cFor = createTableCell(formatoStr, cellFont, Element.ALIGN_CENTER, rowBg);
                PdfPCell cCan = createTableCell(String.valueOf(d.getCantidad()), cellFont, Element.ALIGN_CENTER, rowBg);
                PdfPCell cPre = createTableCell("$" + (d.getPrecio_unitario() != null ? d.getPrecio_unitario().setScale(2, RoundingMode.HALF_UP) : "0.00"), cellFont, Element.ALIGN_RIGHT, rowBg);
                PdfPCell cSub = createTableCell("$" + (d.getSubtotal() != null ? d.getSubtotal().setScale(2, RoundingMode.HALF_UP) : "0.00"), boldCellFont, Element.ALIGN_RIGHT, rowBg);

                table.addCell(cSku);
                table.addCell(cTit);
                table.addCell(cFor);
                table.addCell(cCan);
                table.addCell(cPre);
                table.addCell(cSub);
            }
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // d) Desglose de IVA (19%), Subtotal y Monto Total pagado
        BigDecimal total = venta.getMonto_total() != null ? venta.getMonto_total() : BigDecimal.ZERO;
        BigDecimal base = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(base);

        PdfPTable totTable = new PdfPTable(2);
        totTable.setWidthPercentage(45);
        totTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totTable.setWidths(new float[]{1.4f, 1.2f});

        totTable.addCell(createRowCell("Subtotal Gravable:", cellFont, Element.ALIGN_RIGHT));
        totTable.addCell(createRowCell("$" + base.setScale(2, RoundingMode.HALF_UP), cellFont, Element.ALIGN_RIGHT));

        totTable.addCell(createRowCell("IVA Discriminado (19%):", cellFont, Element.ALIGN_RIGHT));
        totTable.addCell(createRowCell("$" + iva.setScale(2, RoundingMode.HALF_UP), cellFont, Element.ALIGN_RIGHT));

        totTable.addCell(createRowCell("TOTAL PAGADO:", highlightFont, Element.ALIGN_RIGHT));
        totTable.addCell(createRowCell("$" + total.setScale(2, RoundingMode.HALF_UP), highlightFont, Element.ALIGN_RIGHT));

        document.add(totTable);

        // Pie de Factura institucional
        Paragraph footerSpace = new Paragraph(" ");
        footerSpace.setSpacingBefore(15);
        document.add(footerSpace);

        Paragraph footerNote = new Paragraph(
                "DOCUMENTO EQUIVALENTE A FACTURA ELECTRÓNICA DE VENTA — RÉGIMEN COMÚN\n" +
                "Resolución DIAN No. 18764000001 de Habilitación. RetroTunes Bucaramanga, Colombia.\n" +
                "Garantía de originalidad y preservación acústica analógica bajo estándares internacionales.",
                FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(113, 113, 122)));
        footerNote.setAlignment(Element.ALIGN_CENTER);
        document.add(footerNote);

        document.close();
        return baos.toByteArray();
    }

    private static PdfPCell createTableCell(String text, Font font, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderColor(new Color(228, 228, 231));
        return c;
    }

    private static PdfPCell createRowCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(3);
        return c;
    }
}
