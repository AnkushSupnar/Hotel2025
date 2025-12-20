package com.frontend.print;

import com.frontend.entity.TempTransaction;
import com.frontend.service.EmployeeService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KOT (Kitchen Order Ticket) Print class for thermal printer using iTextPDF
 * Generates PDF KOT and prints to thermal printer
 */
@Component
public class KOTOrderPrint {

    private static final Logger LOG = LoggerFactory.getLogger(KOTOrderPrint.class);

    // PDF output path
    private static final String KOT_PDF_PATH = System.getProperty("user.home") + File.separator + "kot.pdf";

    // Paper width for 80mm thermal printer (in points, 1 inch = 72 points, 80mm = 3.15 inches)
    private static final float PAPER_WIDTH = 226f;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TableMasterService tableMasterService;

    // Font path - will be loaded from settings
    private String fontPath;
    private BaseFont baseFont;
    private Font fontLarge;
    private Font fontMedium;
    private Font fontSmall;
    private Font fontEnglishSmall;
    private Font fontEnglishMedium;
    private Font fontEnglishBold;

    // Store last print error for displaying to user
    private String lastPrintError = null;

    /**
     * Print KOT to thermal printer
     */
    public boolean printKOT(String tableName, Integer tableId, List<TempTransaction> items, Integer waitorId) {
        if (items == null || items.isEmpty()) {
            LOG.warn("No items to print for table {}", tableName);
            return false;
        }

        try {
            LOG.info("Starting KOT PDF generation for table {}", tableName);

            // Load fonts
            loadFonts();

            // Generate PDF
            String waitorName = getWaitorName(waitorId);
            String pdfPath = generateKOTPdf(tableName, items, waitorName);
            if (pdfPath == null) {
                LOG.error("Failed to generate KOT PDF");
                return false;
            }

            // Print PDF
            boolean printed = printPdf(pdfPath);
            if (printed) {
                LOG.info("KOT printed successfully for table {}", tableName);
            }

            return printed;

        } catch (Exception e) {
            LOG.error("Error printing KOT for table {}: {}", tableName, e.getMessage(), e);
            lastPrintError = e.getMessage();
            return false;
        }
    }

    /**
     * Print KOT with print dialog (for selecting printer)
     */
    public boolean printKOTWithDialog(String tableName, Integer tableId, List<TempTransaction> items, Integer waitorId) {
        if (items == null || items.isEmpty()) {
            LOG.warn("No items to print for table {}", tableName);
            return false;
        }

        try {
            LOG.info("Starting KOT PDF generation (with dialog) for table {} with {} items", tableName, items.size());

            // Load fonts
            loadFonts();

            // Generate PDF
            String waitorName = getWaitorName(waitorId);
            String pdfPath = generateKOTPdf(tableName, items, waitorName);
            if (pdfPath == null) {
                LOG.error("Failed to generate KOT PDF");
                lastPrintError = "Failed to generate KOT PDF";
                return false;
            }

            // Print PDF with dialog
            boolean printed = printPdfWithDialog(pdfPath);
            if (printed) {
                LOG.info("KOT printed successfully for table {}", tableName);
            }

            return printed;

        } catch (Exception e) {
            LOG.error("Error printing KOT for table {}: {}", tableName, e.getMessage(), e);
            lastPrintError = e.getMessage();
            return false;
        }
    }

    /**
     * Get the last print error message
     */
    public String getLastPrintError() {
        return lastPrintError;
    }

    /**
     * Clear the last print error
     */
    public void clearLastPrintError() {
        lastPrintError = null;
    }

    /**
     * Load fonts for PDF generation
     */
    private void loadFonts() {
        try {
            // Get font path from settings
            fontPath = SessionService.getApplicationSetting("input_font_path");

            if (fontPath != null && !fontPath.trim().isEmpty() && new File(fontPath).exists()) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                LOG.info("Custom font loaded from: {}", fontPath);
            } else {
                // Try bundled font
                String bundledFontPath = getClass().getResource("/fonts/kiran.ttf") != null
                        ? getClass().getResource("/fonts/kiran.ttf").getPath()
                        : null;

                if (bundledFontPath != null) {
                    // Load from classpath
                    baseFont = BaseFont.createFont("/fonts/kiran.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    LOG.info("Bundled font loaded from resources");
                } else {
                    // Fallback to system font
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
                    LOG.warn("Using fallback font - custom font not available");
                }
            }

            // Create font instances
            fontLarge = new Font(baseFont, 22f, Font.BOLD, BaseColor.BLACK);
            fontMedium = new Font(baseFont, 14f, Font.NORMAL, BaseColor.BLACK);
            fontSmall = new Font(baseFont, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishBold = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor.BLACK);

        } catch (Exception e) {
            LOG.error("Error loading fonts: {}", e.getMessage(), e);
            // Create fallback fonts
            fontLarge = new Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, BaseColor.BLACK);
            fontMedium = new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, BaseColor.BLACK);
            fontSmall = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishBold = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor.BLACK);
        }
    }

    /**
     * Generate KOT PDF
     */
    private String generateKOTPdf(String tableName, List<TempTransaction> items, String waitorName) {
        try {
            // Calculate dynamic height based on content
            // Header: Hotel name (30) + Order text (20) + Table/Date row (20) = 70
            // Items header row: 20
            // Each item row: ~22 (with padding for text wrap)
            // Footer: Waiter + Total row (25)
            // Margins and padding: 30
            float headerHeight = 70f;
            float itemsHeaderHeight = 20f;
            float itemRowHeight = 22f;
            float footerHeight = 25f;
            float margins = 30f;

            float height = headerHeight + itemsHeaderHeight + (items.size() * itemRowHeight) + footerHeight + margins;

            // Ensure minimum height for proper display
            if (height < 250f) height = 250f;

            // Create document with default page size first
            Document document = new Document();

            // Create PDF file
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(KOT_PDF_PATH));

            // Set custom page size before opening - this is the correct way
            Rectangle pageSize = new Rectangle(PAPER_WIDTH, height);
            document.setPageSize(pageSize);
            document.setMargins(3f, 3f, 5f, 5f);

            document.open();

            // Create items table
            PdfPTable itemsTable = createItemsTable(items);

            // Create header table
            PdfPTable headerTable = createHeaderTable(tableName, waitorName, items, itemsTable);

            // Add header table (which includes items table)
            document.add(headerTable);

            document.close();

            LOG.info("KOT PDF generated at: {} with height: {}", KOT_PDF_PATH, height);
            return KOT_PDF_PATH;

        } catch (Exception e) {
            LOG.error("Error generating KOT PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create header table with hotel info and KOT details
     */
    private PdfPTable createHeaderTable(String tableName, String waitorName, List<TempTransaction> items, PdfPTable itemsTable) throws Exception {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setTotalWidth(new float[]{210});
        headerTable.setLockedWidth(true);

        // Hotel name - "ha^Tola AMjanaI"
        PdfPCell cellHead = new PdfPCell(new Phrase("ha^Tola AMjanaI", fontLarge));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(2f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // "Aa^Dr" (Order) text
        cellHead = new PdfPCell(new Phrase("Aa^Dr", fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(4f);
        headerTable.addCell(cellHead);

        // Table No and DateTime in nested table
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidths(new float[]{50, 50});

        // Table number: Marathi label + English value
        Phrase tablePhrase = new Phrase();
        tablePhrase.add(new Chunk("TobalanaM. ", fontSmall));
        tablePhrase.add(new Chunk(tableName, fontEnglishBold));
        PdfPCell cellTable = new PdfPCell(tablePhrase);
        cellTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.setBorder(Rectangle.NO_BORDER);
        cellTable.setPaddingTop(4f);
        cellTable.setPaddingBottom(2f);
        infoTable.addCell(cellTable);

        // Date Time in English
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"));
        PdfPCell cellDateTime = new PdfPCell(new Phrase(dateTime, fontEnglishSmall));
        cellDateTime.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDateTime.setBorder(Rectangle.NO_BORDER);
        cellDateTime.setPaddingTop(4f);
        cellDateTime.setPaddingBottom(2f);
        infoTable.addCell(cellDateTime);

        cellHead = new PdfPCell(infoTable);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Add items table
        cellHead = new PdfPCell(itemsTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(2f);
        headerTable.addCell(cellHead);

        // Footer with Waiter and Total items
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidths(new float[]{60, 40});

        // Waiter: Marathi label + Marathi value
        Phrase waiterPhrase = new Phrase();
        waiterPhrase.add(new Chunk("vaoTr : ", fontSmall));
        waiterPhrase.add(new Chunk(waitorName, fontMedium));
        PdfPCell cellWaiter = new PdfPCell(waiterPhrase);
        cellWaiter.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellWaiter.setBorder(Rectangle.TOP);
        cellWaiter.setPaddingTop(4f);
        cellWaiter.setPaddingBottom(2f);
        footerTable.addCell(cellWaiter);

        // Total items: Marathi label + English value
        Phrase totalPhrase = new Phrase();
        totalPhrase.add(new Chunk("ekUNa Aa[Tma : ", fontSmall));
        totalPhrase.add(new Chunk(String.valueOf(items.size()), fontEnglishBold));
        PdfPCell cellTotal = new PdfPCell(totalPhrase);
        cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotal.setBorder(Rectangle.TOP);
        cellTotal.setPaddingTop(4f);
        cellTotal.setPaddingBottom(2f);
        footerTable.addCell(cellTotal);

        cellHead = new PdfPCell(footerTable);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        return headerTable;
    }

    /**
     * Create items table for KOT - Sr.No, Item Name, Qty
     */
    private PdfPTable createItemsTable(List<TempTransaction> items) throws Exception {
        // Items table with 3 columns: Sr.No, Item, Qty
        PdfPTable table = new PdfPTable(3);
        table.setTotalWidth(new float[]{25, 145, 40});
        table.setLockedWidth(true);

        float rowHeight = 18f;

        // Table header
        PdfPCell cell = new PdfPCell(new Phrase("k`.", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("tapaSaIla", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("naga", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        // Add items
        int srNo = 1;
        for (TempTransaction item : items) {
            // Sr. No in English font
            PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(srNo++), fontEnglishBold));
            c1.setBorder(Rectangle.NO_BORDER);
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            c1.setPaddingTop(2f);
            c1.setPaddingBottom(3f);
            table.addCell(c1);

            // Item name in Marathi font
            PdfPCell c2 = new PdfPCell(new Phrase(item.getItemName(), fontMedium));
            c2.setBorder(Rectangle.NO_BORDER);
            c2.setNoWrap(false);
            c2.setPaddingTop(2f);
            c2.setPaddingBottom(3f);
            table.addCell(c2);

            // Qty in English font (using printQty)
            PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(item.getPrintQty().intValue()), fontEnglishBold));
            c3.setBorder(Rectangle.NO_BORDER);
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            c3.setPaddingTop(2f);
            c3.setPaddingBottom(3f);
            table.addCell(c3);
        }

        return table;
    }

    /**
     * Get waiter name by ID
     */
    private String getWaitorName(Integer waitorId) {
        if (waitorId == null) {
            return "-";
        }
        try {
            var waitor = employeeService.getEmployeeById(waitorId);
            return waitor != null ? waitor.getFirstName() : "-";
        } catch (Exception e) {
            LOG.warn("Could not get waiter name for ID {}", waitorId);
            return "-";
        }
    }

    /**
     * Print PDF using PDFBox
     */
    private boolean printPdf(String pdfPath) {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            LOG.error("PDF file not found: {}", pdfPath);
            lastPrintError = "PDF not found";
            return false;
        }

        // Try printing with retry logic - reload document each attempt
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            PDDocument document = null;
            try {
                // Small delay before loading to ensure file is released
                if (retryCount > 0) {
                    Thread.sleep(2000);
                }

                document = PDDocument.load(pdfFile);
                PrinterJob job = PrinterJob.getPrinterJob();
                // Use PORTRAIT orientation to prevent auto-rotation
                job.setPageable(new PDFPageable(document, Orientation.PORTRAIT));

                job.print();
                document.close();
                LOG.info("PDF printed successfully");
                return true;

            } catch (Exception pe) {
                retryCount++;
                String errorMsg = pe.getMessage() != null ? pe.getMessage().toLowerCase() : "";

                // Close document before retry
                if (document != null) {
                    try { document.close(); } catch (Exception ignored) {}
                }

                if ((errorMsg.contains("access") || errorMsg.contains("denied") || errorMsg.contains("busy"))
                        && retryCount < maxRetries) {
                    LOG.warn("Printer access issue (attempt {}/{}): {}. Retrying in 2 seconds...",
                            retryCount, maxRetries, pe.getMessage());
                } else {
                    LOG.error("Error printing PDF: {}", pe.getMessage(), pe);
                    lastPrintError = retryCount >= maxRetries
                            ? "Printer access denied after " + maxRetries + " attempts. Please check printer."
                            : pe.getMessage();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Print PDF with dialog
     */
    private boolean printPdfWithDialog(String pdfPath) {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            LOG.error("PDF file not found: {}", pdfPath);
            lastPrintError = "PDF file not found";
            return false;
        }

        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            PrinterJob job = PrinterJob.getPrinterJob();
            // Use PORTRAIT orientation to prevent auto-rotation
            job.setPageable(new PDFPageable(document, Orientation.PORTRAIT));

            if (!job.printDialog()) {
                document.close();
                LOG.info("Print cancelled by user");
                return false;
            }

            // User selected printer, now close document and use retry logic
            document.close();
            document = null;

            // Retry logic with document reload for each attempt
            int maxRetries = 3;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                PDDocument retryDoc = null;
                try {
                    // Delay before retry
                    if (retryCount > 0) {
                        Thread.sleep(2000);
                    }

                    retryDoc = PDDocument.load(pdfFile);
                    PrinterJob retryJob = PrinterJob.getPrinterJob();
                    retryJob.setPrintService(job.getPrintService()); // Use selected printer
                    retryJob.setPageable(new PDFPageable(retryDoc, Orientation.PORTRAIT));

                    retryJob.print();
                    retryDoc.close();
                    LOG.info("PDF printed successfully");
                    return true;

                } catch (Exception pe) {
                    retryCount++;
                    String errorMsg = pe.getMessage() != null ? pe.getMessage().toLowerCase() : "";

                    // Close document before retry
                    if (retryDoc != null) {
                        try { retryDoc.close(); } catch (Exception ignored) {}
                    }

                    if ((errorMsg.contains("access") || errorMsg.contains("denied") || errorMsg.contains("busy"))
                            && retryCount < maxRetries) {
                        LOG.warn("Printer access issue (attempt {}/{}): {}. Retrying in 2 seconds...",
                                retryCount, maxRetries, pe.getMessage());
                    } else {
                        LOG.error("Error printing PDF: {}", pe.getMessage(), pe);
                        lastPrintError = retryCount >= maxRetries
                                ? "Printer access denied after " + maxRetries + " attempts. Please check printer."
                                : pe.getMessage();
                        return false;
                    }
                }
            }
            return false;

        } catch (Exception e) {
            LOG.error("Error printing PDF: {}", e.getMessage(), e);
            lastPrintError = e.getMessage();
            if (document != null) {
                try { document.close(); } catch (Exception ignored) {}
            }
            return false;
        }
    }
}
