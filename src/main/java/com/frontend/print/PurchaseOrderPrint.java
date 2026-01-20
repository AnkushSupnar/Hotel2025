package com.frontend.print;

import com.frontend.entity.PurchaseOrder;
import com.frontend.entity.PurchaseOrderTransaction;
import com.frontend.entity.Supplier;
import com.frontend.service.SessionService;
import com.frontend.service.SupplierService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Purchase Order Print class for A4 page PDF generation
 * Generates professional purchase order PDF and opens for printing
 */
@Component
public class PurchaseOrderPrint {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderPrint.class);

    // PDF output directory
    private static final String PDF_DIR = "D:" + File.separator + "Hotel Software";

    // A4 page size
    private static final Rectangle A4_PAGE = PageSize.A4;

    // Colors
    private static final BaseColor HEADER_BG = BaseColor.WHITE;
    private static final BaseColor HEADER_TEXT = BaseColor.BLACK;
    private static final BaseColor TABLE_HEADER_BG = new BaseColor(224, 242, 241); // Light teal
    private static final BaseColor BORDER_COLOR = BaseColor.BLACK;

    @Autowired
    private SupplierService supplierService;

    // Fonts
    private BaseFont baseFont;
    private Font fontTitle;
    private Font fontSubtitle;
    private Font fontHeader;
    private Font fontNormal;
    private Font fontBold;
    private Font fontSmall;
    private Font fontEnglishNormal;
    private Font fontEnglishBold;
    private Font fontEnglishSmall;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Generate Purchase Order PDF and open for printing
     */
    public boolean printPurchaseOrder(PurchaseOrder order) {
        if (order == null) {
            LOG.warn("No purchase order to print");
            return false;
        }

        try {
            LOG.info("Starting Purchase Order PDF generation for Order #{}", order.getOrderNo());

            // Load fonts
            loadFonts();

            // Ensure output directory exists
            File outputDir = new File(PDF_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Generate PDF
            String pdfPath = generatePurchaseOrderPdf(order);
            if (pdfPath == null) {
                LOG.error("Failed to generate purchase order PDF");
                return false;
            }

            LOG.info("Purchase Order #{} PDF saved at: {}", order.getOrderNo(), pdfPath);

            // Open PDF for viewing/printing
            openPdf(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating Purchase Order #{}: {}", order.getOrderNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Load fonts for PDF generation
     */
    private void loadFonts() {
        try {
            // Get font path from settings
            String fontPath = SessionService.getApplicationSetting("input_font_path");

            if (fontPath != null && !fontPath.trim().isEmpty() && new File(fontPath).exists()) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                LOG.info("Custom font loaded from: {}", fontPath);
            } else {
                // Try bundled font
                try {
                    baseFont = BaseFont.createFont("/fonts/kiran.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    LOG.info("Bundled font loaded from resources");
                } catch (Exception e) {
                    // Fallback to system font
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
                    LOG.warn("Using fallback font - custom font not available");
                }
            }

            // Create font instances for Marathi text
            fontTitle = new Font(baseFont, 24f, Font.BOLD, BaseColor.BLACK);
            fontSubtitle = new Font(baseFont, 16f, Font.NORMAL, BaseColor.BLACK);
            fontHeader = new Font(baseFont, 14f, Font.BOLD, BaseColor.BLACK);
            fontNormal = new Font(baseFont, 20f, Font.NORMAL, BaseColor.BLACK);
            fontBold = new Font(baseFont, 12f, Font.BOLD, BaseColor.BLACK);
            fontSmall = new Font(baseFont, 10f, Font.NORMAL, BaseColor.BLACK);

            // English fonts
            fontEnglishNormal = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishBold = new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);

        } catch (Exception e) {
            LOG.error("Error loading fonts: {}", e.getMessage(), e);
            // Create fallback fonts
            fontTitle = new Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, BaseColor.BLACK);
            fontSubtitle = new Font(Font.FontFamily.HELVETICA, 16f, Font.NORMAL, BaseColor.BLACK);
            fontHeader = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor.BLACK);
            fontNormal = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontBold = new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK);
            fontSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishNormal = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishBold = new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
        }
    }

    /**
     * Generate Purchase Order PDF
     */
    private String generatePurchaseOrderPdf(PurchaseOrder order) {
        try {
            // Create document with A4 page size - reduced margins
            Document document = new Document(A4_PAGE, 25f, 25f, 20f, 20f);

            // Create PDF file path with timestamp for unique filename
           // String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String pdfPath = PDF_DIR + File.separator + "PurchaseOrder"+ ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Get supplier details
            Supplier supplier = getSupplier(order.getPartyId());

            // Add header
            addHeader(document, order);

            // Add order info section
            addOrderInfo(document, order, supplier);

            // Add items table
            addItemsTable(document, order.getTransactions());

            // Add summary section
            addSummary(document, order);

            // Add footer
            addFooter(document);

            document.close();

            LOG.info("Purchase Order PDF generated at: {}", pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Error generating purchase order PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Add header section
     */
    private void addHeader(Document document, PurchaseOrder order) throws DocumentException {
        // Header table
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(8f);

        // Title cell
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(HEADER_BG);
        titleCell.setPadding(5f);
        titleCell.setBorder(Rectangle.NO_BORDER);

        // Title text
        Paragraph title = new Paragraph();
        title.setAlignment(Element.ALIGN_CENTER);

        // Company name in Marathi
        Chunk companyName = new Chunk("AMjanaI k^fo\n", new Font(baseFont, 24f, Font.BOLD, HEADER_TEXT));
        title.add(companyName);

        // Subtitle
        Chunk subtitle = new Chunk("f^imalaI rosTa^rMT\n", new Font(baseFont, 14f, Font.NORMAL, HEADER_TEXT));
        title.add(subtitle);

        // Document title
        Chunk docTitle = new Chunk("PURCHASE ORDER", new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, HEADER_TEXT));
        title.add(docTitle);

        titleCell.addElement(title);
        headerTable.addCell(titleCell);

        document.add(headerTable);
    }

    /**
     * Add order info section
     */
    private void addOrderInfo(Document document, PurchaseOrder order, Supplier supplier) throws DocumentException {
        // Two column layout for order info
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});
        infoTable.setSpacingAfter(8f);

        // Left column - Order details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOX);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setPadding(5f);

        Paragraph orderDetails = new Paragraph();
        orderDetails.add(new Chunk("Order No: ", fontEnglishBold));
        orderDetails.add(new Chunk(String.valueOf(order.getOrderNo()) + "  ", fontEnglishNormal));
        orderDetails.add(new Chunk("Date: ", fontEnglishBold));
        String dateStr = order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : "-";
        orderDetails.add(new Chunk(dateStr, fontEnglishNormal));

        leftCell.addElement(orderDetails);
        infoTable.addCell(leftCell);

        // Right column - Supplier details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setBorderColor(BORDER_COLOR);
        rightCell.setPadding(5f);

        Paragraph supplierDetails = new Paragraph();
        supplierDetails.add(new Chunk("paurvazadaracao naava : ", fontBold));

        if (supplier != null) {
            supplierDetails.add(new Chunk(supplier.getName(), fontNormal));
            if (supplier.getContact() != null && !supplier.getContact().isEmpty()) {
                supplierDetails.add(new Chunk(" (" + supplier.getContact() + ")", fontEnglishSmall));
            }
        } else {
            supplierDetails.add(new Chunk("-", fontNormal));
        }

        rightCell.addElement(supplierDetails);
        infoTable.addCell(rightCell);

        document.add(infoTable);
    }

    /**
     * Add items table
     */
    private void addItemsTable(Document document, List<PurchaseOrderTransaction> transactions) throws DocumentException {
        // Items table with 6 columns: Sr.No, Item Name, Unit, Quantity, Rate (empty), Amount (empty)
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 44, 10, 10, 15, 15});
        table.setSpacingAfter(8f);

        // Table header
        addTableHeaderCell(table, "A.k`.", fontHeader); // Sr. No.
        addTableHeaderCell(table, "maalaacao naava", fontHeader); // Item Name
        addTableHeaderCell(table, "yaunaIT", fontHeader); // Unit
        addTableHeaderCell(table, "naga", fontHeader); // Quantity
        addTableHeaderCell(table, "dr", fontHeader); // Rate (empty for manual entry)
        addTableHeaderCell(table, "rkkma", fontHeader); // Amount (empty for manual entry)

        // Add items
        int srNo = 1;
        if (transactions != null && !transactions.isEmpty()) {
            for (PurchaseOrderTransaction trans : transactions) {
                // Sr. No.
                PdfPCell cellSr = new PdfPCell(new Phrase(String.valueOf(srNo++), fontEnglishNormal));
                cellSr.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellSr.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellSr.setPadding(4f);
                cellSr.setBorderColor(BORDER_COLOR);
                table.addCell(cellSr);

                // Item Name
                PdfPCell cellItem = new PdfPCell(new Phrase(trans.getItemName(), fontNormal));
                cellItem.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellItem.setPadding(4f);
                cellItem.setBorderColor(BORDER_COLOR);
                table.addCell(cellItem);

                // Unit
                PdfPCell cellUnit = new PdfPCell(new Phrase(
                        trans.getUnit() != null ? trans.getUnit() : "KG", fontEnglishNormal));
                cellUnit.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellUnit.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellUnit.setPadding(4f);
                cellUnit.setBorderColor(BORDER_COLOR);
                table.addCell(cellUnit);

                // Quantity
                PdfPCell cellQty = new PdfPCell(new Phrase(String.format("%.0f", trans.getQty()), fontEnglishNormal));
                cellQty.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellQty.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellQty.setPadding(4f);
                cellQty.setBorderColor(BORDER_COLOR);
                table.addCell(cellQty);

                // Rate (empty cell for manual entry)
                PdfPCell cellRate = new PdfPCell(new Phrase(" ", fontEnglishNormal));
                cellRate.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellRate.setPadding(4f);
                cellRate.setBorderColor(BORDER_COLOR);
                table.addCell(cellRate);

                // Amount (empty cell for manual entry)
                PdfPCell cellAmount = new PdfPCell(new Phrase(" ", fontEnglishNormal));
                cellAmount.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellAmount.setPadding(4f);
                cellAmount.setBorderColor(BORDER_COLOR);
                table.addCell(cellAmount);
            }
        } else {
            // No items row
            PdfPCell noItemsCell = new PdfPCell(new Phrase("No items in this order", fontEnglishNormal));
            noItemsCell.setColspan(6);
            noItemsCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            noItemsCell.setPadding(8f);
            noItemsCell.setBorderColor(BORDER_COLOR);
            table.addCell(noItemsCell);
        }

        document.add(table);
    }

    /**
     * Add table header cell
     */
    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(TABLE_HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    /**
     * Add summary section
     */
    private void addSummary(Document document, PurchaseOrder order) throws DocumentException {
        // Summary table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(45);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{60, 40});

        // Total Items
        PdfPCell labelCell = new PdfPCell(new Phrase("ekuNa vastau :", fontBold));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                String.valueOf(order.getTotalItems() != null ? order.getTotalItems() : 0), fontEnglishBold));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.addCell(valueCell);

        // Total Quantity
        labelCell = new PdfPCell(new Phrase("ekuNa saMKyaa :", fontBold));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(labelCell);

        valueCell = new PdfPCell(new Phrase(
                String.format("%.0f", order.getTotalQty() != null ? order.getTotalQty() : 0), fontEnglishBold));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.addCell(valueCell);

        // Total Amount (empty for manual entry)
        labelCell = new PdfPCell(new Phrase("ekuNa rkkma :", fontBold));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(labelCell);

        // Empty box for total amount - manual entry
        valueCell = new PdfPCell(new Phrase(" ", fontEnglishBold));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(BORDER_COLOR);
        valueCell.setPadding(4f);
        valueCell.setMinimumHeight(20f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.addCell(valueCell);

        document.add(summaryTable);

        // Remarks section if present
        if (order.getRemarks() != null && !order.getRemarks().trim().isEmpty()) {
            PdfPTable remarksTable = new PdfPTable(1);
            remarksTable.setWidthPercentage(100);
            remarksTable.setSpacingBefore(5f);

            PdfPCell remarksCell = new PdfPCell();
            remarksCell.setBorder(Rectangle.BOX);
            remarksCell.setBorderColor(BORDER_COLOR);
            remarksCell.setPadding(5f);

            Paragraph remarks = new Paragraph();
            remarks.add(new Chunk("SaorI : ", fontBold));
            remarks.add(new Chunk(order.getRemarks(), fontNormal));
            remarksCell.addElement(remarks);

            remarksTable.addCell(remarksCell);
            document.add(remarksTable);
        }
    }

    /**
     * Add footer section
     */
    private void addFooter(Document document) throws DocumentException {
        // Signature section
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{50, 50});
        signTable.setSpacingBefore(15f);

        // Prepared By
        PdfPCell prepCell = new PdfPCell();
        prepCell.setBorder(Rectangle.NO_BORDER);
        prepCell.setPadding(5f);

        Paragraph prep = new Paragraph();
        prep.add(new Chunk("\n\n________________________\n", fontEnglishNormal));
        prep.add(new Chunk("Prepared By", fontEnglishSmall));
        prep.setAlignment(Element.ALIGN_CENTER);
        prepCell.addElement(prep);
        signTable.addCell(prepCell);

        // Authorized By
        PdfPCell authCell = new PdfPCell();
        authCell.setBorder(Rectangle.NO_BORDER);
        authCell.setPadding(5f);

        Paragraph auth = new Paragraph();
        auth.add(new Chunk("\n\n________________________\n", fontEnglishNormal));
        auth.add(new Chunk("Authorized Signature", fontEnglishSmall));
        auth.setAlignment(Element.ALIGN_CENTER);
        authCell.addElement(auth);
        signTable.addCell(authCell);

        document.add(signTable);
    }

    /**
     * Get supplier by ID
     */
    private Supplier getSupplier(Integer supplierId) {
        if (supplierId == null) return null;
        try {
            return supplierService.getSupplierById(supplierId);
        } catch (Exception e) {
            LOG.warn("Could not get supplier for ID {}", supplierId);
            return null;
        }
    }

    /**
     * Open PDF file for viewing/printing
     */
    private void openPdf(String pdfPath) {
        try {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
                LOG.info("PDF opened for viewing: {}", pdfPath);
            }
        } catch (Exception e) {
            LOG.error("Error opening PDF: {}", e.getMessage(), e);
        }
    }
}
