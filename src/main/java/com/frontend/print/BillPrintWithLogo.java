package com.frontend.print;

import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.Transaction;
import com.frontend.service.CustomerService;
import com.frontend.service.EmployeesService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.frontend.util.QRCodeGenerator;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;

/**
 * Bill Print with Logo class for thermal printer using iTextPDF
 * Generates PDF bill with logo in header and prints to thermal printer.
 * Replicates BillPrint functionality with bill logo image support.
 */
@Component
public class BillPrintWithLogo {

    private static final Logger LOG = LoggerFactory.getLogger(BillPrintWithLogo.class);

    // Default PDF output directory (fallback if not configured in settings)
    private static final String DEFAULT_BILL_PDF_DIR = "D:" + File.separator + "Hotel Software";

    // 80mm paper roll (8cm), 72mm actual printable area (7.2cm)
    // 72mm = (72 / 25.4) * 72 ≈ 204 points
    private static final float PAPER_WIDTH = 204f;

    // Setting key for bill logo image
    private static final String BILL_LOGO_SETTING = "bill_logo_image";

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private CustomerService customerService;

    // Font path - will be loaded from settings
    private String fontPath;
    private BaseFont baseFont;
    private Font fontLarge;
    private Font fontMedium;
    private Font fontSmall;
    private Font fontEnglishSmall;
    private Font fontEnglishMedium;

    /**
     * Get the PDF output directory from application settings
     * Falls back to default directory if not configured
     */
    private String getPdfOutputDirectory() {
        String documentDir = SessionService.getDocumentDirectory();
        if (documentDir != null && !documentDir.trim().isEmpty()) {
            File dir = new File(documentDir);
            if (dir.exists() && dir.isDirectory()) {
                return documentDir;
            }
            LOG.warn("Configured document directory does not exist: {}, using default", documentDir);
        }
        return DEFAULT_BILL_PDF_DIR;
    }

    /**
     * Get a writable output directory with multiple fallback options
     * Tries: configured directory -> user home -> temp directory
     */
    private String getWritableOutputDirectory() {
        // Try configured document directory first
        String documentDir = SessionService.getDocumentDirectory();
        if (documentDir != null && !documentDir.trim().isEmpty()) {
            File dir = new File(documentDir);
            if (tryCreateDirectory(dir)) {
                LOG.info("Using configured document directory: {}", documentDir);
                return documentDir;
            }
        }

        // Try default directory
        File defaultDir = new File(DEFAULT_BILL_PDF_DIR);
        if (tryCreateDirectory(defaultDir)) {
            LOG.info("Using default directory: {}", DEFAULT_BILL_PDF_DIR);
            return DEFAULT_BILL_PDF_DIR;
        }

        // Fallback to user home directory
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File homeDir = new File(userHome + File.separator + "HotelBills");
            if (tryCreateDirectory(homeDir)) {
                LOG.info("Using user home directory: {}", homeDir.getAbsolutePath());
                return homeDir.getAbsolutePath();
            }
        }

        // Final fallback to temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir != null) {
            File tempBillDir = new File(tempDir + File.separator + "HotelBills");
            if (tryCreateDirectory(tempBillDir)) {
                LOG.info("Using temp directory: {}", tempBillDir.getAbsolutePath());
                return tempBillDir.getAbsolutePath();
            }
        }

        LOG.error("Could not find any writable directory for PDF output");
        return null;
    }

    /**
     * Try to create directory if it doesn't exist
     * @return true if directory exists or was created successfully and is writable
     */
    private boolean tryCreateDirectory(File dir) {
        try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    LOG.warn("Failed to create directory: {}", dir.getAbsolutePath());
                    return false;
                }
            }
            // Check if directory is writable by creating a test file
            File testFile = new File(dir, ".write_test_" + System.currentTimeMillis());
            if (testFile.createNewFile()) {
                testFile.delete();
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.warn("Directory not accessible: {} - {}", dir.getAbsolutePath(), e.getMessage());
            return false;
        }
    }

    /**
     * Generate Bill PDF and save to configured document directory, then print automatically
     */
    public boolean printBill(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        try {
            LOG.info("Starting Bill PDF (with logo) generation for Bill #{}", bill.getBillNo());

            // Load fonts
            loadFonts();

            // Get output directory from settings
            String pdfDir = getPdfOutputDirectory();

            // Ensure output directory exists
            File outputDir = new File(pdfDir);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Generate PDF
            String pdfPath = generateBillPdf(bill, tableName);
            if (pdfPath == null) {
                LOG.error("Failed to generate bill PDF with logo");
                return false;
            }

            LOG.info("Bill #{} PDF (with logo) saved successfully at: {}", bill.getBillNo(), pdfPath);

            // Auto-print the PDF to configured printer
            printPdfToConfiguredPrinter(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating Bill #{} with logo: {}", bill.getBillNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate Bill PDF and save to configured document directory (same as printBill)
     */
    public boolean printBillWithDialog(Bill bill, String tableName) {
        // Simply delegate to printBill - no longer showing printer dialog
        return printBill(bill, tableName);
    }

    /**
     * Generate Bill PDF with optional QR code for UPI payment
     */
    public boolean printBillWithQR(Bill bill, String tableName, boolean printQR, String upiId, String bankName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        // If no QR requested, use standard print
        if (!printQR || upiId == null || upiId.trim().isEmpty()) {
            return printBill(bill, tableName);
        }

        try {
            LOG.info("Starting Bill PDF (with logo + QR) generation for Bill #{}", bill.getBillNo());

            // Load fonts
            loadFonts();

            // Get output directory from settings
            String pdfDir = getPdfOutputDirectory();

            // Ensure output directory exists
            File outputDir = new File(pdfDir);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Generate PDF with QR code
            String pdfPath = generateBillPdfWithQR(bill, tableName, upiId, bankName);
            if (pdfPath == null) {
                LOG.error("Failed to generate bill PDF with logo + QR");
                return false;
            }

            LOG.info("Bill #{} PDF (with logo + QR) saved successfully at: {}", bill.getBillNo(), pdfPath);

            // Auto-print the PDF to configured printer
            printPdfToConfiguredPrinter(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating Bill #{} with logo + QR: {}", bill.getBillNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate Bill PDF with QR code
     */
    private String generateBillPdfWithQR(Bill bill, String tableName, String upiId, String bankName) {
        try {
            String waitorName = getWaitorName(bill.getWaitorId());

            // Calculate net amount for QR code
            double netAmount = bill.getNetAmount() != null ? bill.getNetAmount() :
                    (bill.getTransactions() != null ?
                            bill.getTransactions().stream().mapToDouble(t -> t.getAmt()).sum() : 0);

            // Generate QR code image
            byte[] qrCodeImage = null;
            try {
                qrCodeImage = QRCodeGenerator.generateUPIQRCode(upiId, bankName, netAmount);
                LOG.info("Generated UPI QR code for amount: {}", netAmount);
            } catch (Exception e) {
                LOG.error("Failed to generate QR code: {}", e.getMessage());
                // Continue without QR code
            }

            // Create items table with QR code
            PdfPTable itemsTable = createItemsTableWithQR(bill.getTransactions(), tableName, waitorName, qrCodeImage, upiId);

            // Create header table with logo
            PdfPTable headerTable = createHeaderTable(bill, tableName, waitorName, itemsTable);

            // Calculate document height based on content (add extra for QR code)
            float height = headerTable.getTotalHeight() + 20f;
            if (qrCodeImage != null) {
                height += 100f; // Extra height for QR code section
            }
            // Minimum height must be greater than PAPER_WIDTH to ensure portrait orientation
            if (height < PAPER_WIDTH + 74f) height = PAPER_WIDTH + 74f;

            // Create page size for thermal printer - portrait orientation
            Rectangle pageSize = new Rectangle(0, 0, PAPER_WIDTH, height);
            pageSize.setRotation(0);

            // Left/right margins to keep content centered and away from edges
            Document document = new Document(pageSize, 12f, 12f, 0f, 2f);

            // Create PDF file path
            String pdfDir = getPdfOutputDirectory();
            String pdfPath = pdfDir + File.separator + "bill.pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Add header table (which includes items table with QR)
            document.add(headerTable);

            document.close();

            LOG.info("Bill PDF with logo + QR generated at: {}", pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Error generating bill PDF with logo + QR: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create items table with QR code - adds QR code before footer
     */
    private PdfPTable createItemsTableWithQR(List<Transaction> transactions, String tableName, String waitorName, byte[] qrCodeImage, String upiId) throws Exception {
        // Use full content width (PAPER_WIDTH - left margin - right margin = 204 - 12 - 12 = 180)
        float contentWidth = PAPER_WIDTH - 24f;

        // Items table with 4 columns: Item, Qty, Rate, Amount
        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{78, 27, 27, 48});
        table.setLockedWidth(true);

        // Row height for proper text display
        float rowHeight = 16f;

        // Table header
        PdfPCell cell = new PdfPCell(new Phrase("tapiSala", fontMedium));
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

        cell = new PdfPCell(new Phrase("dr", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("r@kma", fontMedium));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        // Add items with English font for numbers
        if (transactions != null) {
            for (Transaction trans : transactions) {
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                c1.setPaddingTop(1f);
                c1.setPaddingBottom(2f);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontEnglishMedium));
                c2.setBorder(Rectangle.NO_BORDER);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPaddingTop(1f);
                c2.setPaddingBottom(2f);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontEnglishMedium));
                c3.setBorder(Rectangle.NO_BORDER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setPaddingTop(1f);
                c3.setPaddingBottom(2f);
                table.addCell(c3);

                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", trans.getAmt()), fontEnglishMedium));
                c4.setBorder(Rectangle.NO_BORDER);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                c4.setPaddingTop(1f);
                c4.setPaddingBottom(2f);
                table.addCell(c4);
            }
        }

        // Table number and Total row combined
        Phrase tablePhrase = new Phrase();
        tablePhrase.add(new Chunk("To naM. ", fontMedium));
        tablePhrase.add(new Chunk(tableName, fontEnglishMedium));
        PdfPCell cellTableNo = new PdfPCell(tablePhrase);
        cellTableNo.setFixedHeight(rowHeight);
        cellTableNo.setBorder(Rectangle.TOP);
        cellTableNo.setPaddingTop(1f);
        cellTableNo.setPaddingBottom(1f);
        cellTableNo.setColspan(2);
        table.addCell(cellTableNo);

        PdfPCell cellTotalLabel = new PdfPCell(new Phrase("ekuNa", fontMedium));
        cellTotalLabel.setFixedHeight(rowHeight);
        cellTotalLabel.setBorder(Rectangle.TOP);
        cellTotalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalLabel.setPaddingTop(1f);
        cellTotalLabel.setPaddingBottom(1f);
        table.addCell(cellTotalLabel);

        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell cellTotalAmt = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontEnglishMedium));
        cellTotalAmt.setFixedHeight(rowHeight);
        cellTotalAmt.setBorder(Rectangle.TOP);
        cellTotalAmt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalAmt.setPaddingTop(1f);
        cellTotalAmt.setPaddingBottom(1f);
        table.addCell(cellTotalAmt);

        // Waiter row
        PdfPCell cellWaiter = new PdfPCell(new Phrase("vaoTr :" + waitorName, fontMedium));
        cellWaiter.setFixedHeight(rowHeight);
        cellWaiter.setBorder(Rectangle.NO_BORDER);
        cellWaiter.setPaddingTop(1f);
        cellWaiter.setPaddingBottom(1f);
        cellWaiter.setColspan(4);
        table.addCell(cellWaiter);

        // Add QR code section if available
        if (qrCodeImage != null && qrCodeImage.length > 0) {
            Font scanFont = new Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD, BaseColor.BLACK);
            PdfPCell scanLabel = new PdfPCell(new Phrase("Scan to Pay", scanFont));
            scanLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
            scanLabel.setBorder(Rectangle.NO_BORDER);
            scanLabel.setColspan(4);
            scanLabel.setPaddingTop(2f);
            scanLabel.setPaddingBottom(1f);
            table.addCell(scanLabel);

            try {
                Image qrImage = Image.getInstance(qrCodeImage);
                qrImage.scaleToFit(100f, 100f);
                PdfPCell qrCell = new PdfPCell(qrImage, false);
                qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qrCell.setBorder(Rectangle.NO_BORDER);
                qrCell.setColspan(4);
                qrCell.setPaddingTop(1f);
                qrCell.setPaddingBottom(0f);
                table.addCell(qrCell);

                if (upiId != null && !upiId.trim().isEmpty()) {
                    Font upiFont = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.BLACK);
                    PdfPCell upiCell = new PdfPCell(new Phrase("UPI: " + upiId, upiFont));
                    upiCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    upiCell.setBorder(Rectangle.NO_BORDER);
                    upiCell.setColspan(4);
                    upiCell.setPaddingTop(0f);
                    upiCell.setPaddingBottom(3f);
                    table.addCell(upiCell);
                }
            } catch (Exception e) {
                LOG.error("Error adding QR image to PDF: {}", e.getMessage());
            }
        }

        // Thank you footer
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 7);
        PdfPCell cellFooter = new PdfPCell(new Phrase(
                "Thanks for visit.....HAVE A NICE DAY\n________________________________________\nSoftware developed by Ankush Supnar (8329394603)",
                smallFont));
        cellFooter.setFixedHeight(32f);
        cellFooter.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellFooter.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellFooter.setBorder(Rectangle.NO_BORDER);
        cellFooter.setColspan(4);
        cellFooter.setPaddingTop(3f);
        table.addCell(cellFooter);

        return table;
    }

    /**
     * Load fonts for PDF generation
     */
    private void loadFonts() {
        try {
            fontPath = SessionService.getApplicationSetting("input_font_path");

            if (fontPath != null && !fontPath.trim().isEmpty() && new File(fontPath).exists()) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                LOG.info("Custom font loaded from: {}", fontPath);
            } else {
                String bundledFontPath = getClass().getResource("/fonts/kiran.ttf") != null
                        ? getClass().getResource("/fonts/kiran.ttf").getPath()
                        : null;

                if (bundledFontPath != null) {
                    baseFont = BaseFont.createFont("/fonts/kiran.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    LOG.info("Bundled font loaded from resources");
                } else {
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
                    LOG.warn("Using fallback font - custom font not available");
                }
            }

            fontLarge = new Font(baseFont, 20f, Font.BOLD, BaseColor.BLACK);
            fontMedium = new Font(baseFont, 12f, Font.NORMAL, BaseColor.BLACK);
            fontSmall = new Font(baseFont, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);

        } catch (Exception e) {
            LOG.error("Error loading fonts: {}", e.getMessage(), e);
            fontLarge = new Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor.BLACK);
            fontMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
            fontSmall = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);
        }
    }

    /**
     * Generate Bill PDF
     */
    private String generateBillPdf(Bill bill, String tableName) {
        try {
            String waitorName = getWaitorName(bill.getWaitorId());

            // Create items table
            PdfPTable itemsTable = createItemsTable(bill.getTransactions(), tableName, waitorName);

            // Create header table with logo
            PdfPTable headerTable = createHeaderTable(bill, tableName, waitorName, itemsTable);

            // Calculate document height based on content
            float height = headerTable.getTotalHeight() + 20f;
            if (height < PAPER_WIDTH + 74f) height = PAPER_WIDTH + 74f;

            Rectangle pageSize = new Rectangle(0, 0, PAPER_WIDTH, height);
            pageSize.setRotation(0);

            Document document = new Document(pageSize, 12f, 12f, 0f, 2f);

            String pdfDir = getPdfOutputDirectory();
            String pdfPath = pdfDir + File.separator + "bill.pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            document.add(headerTable);

            document.close();

            LOG.info("Bill PDF with logo generated at: {}", pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Error generating bill PDF with logo: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate Bill PDF as byte array (for API responses).
     */
    public byte[] generateBillPdfBytes(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to generate PDF bytes for");
            return null;
        }
        try {
            loadFonts();

            String waitorName = getWaitorName(bill.getWaitorId());

            PdfPTable itemsTable = createItemsTable(bill.getTransactions(), tableName, waitorName);
            PdfPTable headerTable = createHeaderTable(bill, tableName, waitorName, itemsTable);

            float height = headerTable.getTotalHeight() + 20f;
            if (height < PAPER_WIDTH + 74f) height = PAPER_WIDTH + 74f;

            Rectangle pageSize = new Rectangle(0, 0, PAPER_WIDTH, height);
            pageSize.setRotation(0);

            Document document = new Document(pageSize, 12f, 12f, 0f, 2f);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();
            document.add(headerTable);
            document.close();

            LOG.info("Bill #{} PDF bytes with logo generated ({} bytes)", bill.getBillNo(), baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.error("Error generating bill PDF bytes with logo: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate Bill PDF as byte array with optional QR code for UPI payment.
     */
    public byte[] generateBillPdfBytesWithQR(Bill bill, String tableName, String upiId, String bankName) {
        if (bill == null) {
            LOG.warn("No bill to generate PDF bytes for");
            return null;
        }

        if (upiId == null || upiId.trim().isEmpty()) {
            return generateBillPdfBytes(bill, tableName);
        }

        try {
            loadFonts();

            String waitorName = getWaitorName(bill.getWaitorId());

            double netAmount = bill.getNetAmount() != null ? bill.getNetAmount() :
                    (bill.getTransactions() != null ?
                            bill.getTransactions().stream().mapToDouble(t -> t.getAmt()).sum() : 0);

            byte[] qrCodeImage = null;
            try {
                qrCodeImage = QRCodeGenerator.generateUPIQRCode(upiId, bankName, netAmount);
                LOG.info("Generated UPI QR code for amount: {}", netAmount);
            } catch (Exception e) {
                LOG.error("Failed to generate QR code: {}", e.getMessage());
            }

            PdfPTable itemsTable = createItemsTableWithQR(bill.getTransactions(), tableName, waitorName, qrCodeImage, upiId);
            PdfPTable headerTable = createHeaderTable(bill, tableName, waitorName, itemsTable);

            float height = headerTable.getTotalHeight() + 20f;
            if (qrCodeImage != null) {
                height += 100f;
            }
            if (height < PAPER_WIDTH + 74f) height = PAPER_WIDTH + 74f;

            Rectangle pageSize = new Rectangle(0, 0, PAPER_WIDTH, height);
            pageSize.setRotation(0);

            Document document = new Document(pageSize, 12f, 12f, 0f, 2f);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();
            document.add(headerTable);
            document.close();

            LOG.info("Bill #{} PDF bytes with logo + QR generated ({} bytes)", bill.getBillNo(), baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            LOG.error("Error generating bill PDF bytes with logo + QR: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create header table with hotel info and LOGO at the top
     */
    private PdfPTable createHeaderTable(Bill bill, String tableName, String waitorName, PdfPTable itemsTable) throws Exception {
        float contentWidth = PAPER_WIDTH - 24f;

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setTotalWidth(new float[]{contentWidth});
        headerTable.setLockedWidth(true);
        headerTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        // ===== HEADER: LOGO (LEFT) + RESTAURANT INFO (RIGHT) =====
        PdfPCell cellHead;
        Image logoImage = loadBillLogoImage();
        if (logoImage != null) {
            // Two-column layout: logo on left, restaurant info on right
            float logoColWidth = contentWidth * 0.40f;
            float infoColWidth = contentWidth * 0.60f;
            PdfPTable twoColHeader = new PdfPTable(2);
            twoColHeader.setTotalWidth(new float[]{logoColWidth, infoColWidth});
            twoColHeader.setLockedWidth(true);

            // Left column: Logo image
            logoImage.scaleToFit(logoColWidth - 4f, 70f);
            PdfPCell logoCell = new PdfPCell(logoImage, false);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(2f);
            twoColHeader.addCell(logoCell);

            // Right column: Restaurant details
            PdfPTable infoTable = new PdfPTable(1);
            infoTable.setTotalWidth(new float[]{infoColWidth});
            infoTable.setLockedWidth(true);

            PdfPCell cellInfo = new PdfPCell(new Phrase(getRestaurantName(), fontLarge));
            cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellInfo.setBorder(Rectangle.NO_BORDER);
            cellInfo.setPaddingTop(0f);
            cellInfo.setPaddingBottom(0f);
            infoTable.addCell(cellInfo);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(subTitle, fontMedium));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(address, fontSmall));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
                phonePhrase.add(new Chunk(contacts, fontEnglishSmall));
                cellInfo = new PdfPCell(phonePhrase);
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase("GSTIN:- " + gstin, fontEnglishSmall));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPadding(0f);
            twoColHeader.addCell(infoCell);

            // Add 2-column header to main table with bottom border separator
            PdfPCell headerCell = new PdfPCell(twoColHeader);
            headerCell.setBorder(Rectangle.BOTTOM);
            headerCell.setPadding(0f);
            headerCell.setPaddingBottom(2f);
            headerTable.addCell(headerCell);
        } else {
            // No logo - original single-column centered layout
            cellHead = new PdfPCell(new Phrase(getRestaurantName(), fontLarge));
            cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellHead.setBorder(Rectangle.NO_BORDER);
            cellHead.setPaddingTop(0f);
            cellHead.setPaddingBottom(0f);
            headerTable.addCell(cellHead);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cellHead = new PdfPCell(new Phrase(subTitle, fontMedium));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cellHead = new PdfPCell(new Phrase(address, fontSmall));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
                phonePhrase.add(new Chunk(contacts, fontEnglishSmall));
                cellHead = new PdfPCell(phonePhrase);
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cellHead = new PdfPCell(new Phrase("GSTIN:- " + gstin, fontEnglishSmall));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.BOTTOM);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(2f);
                headerTable.addCell(cellHead);
            }
        }

        // Bill type (Cash/Credit)
        String mode = "";
        if ("Cash".equalsIgnoreCase(bill.getPaymode()) || "PENDING".equalsIgnoreCase(bill.getPaymode())) {
            mode = "k^Sa ibala";
        } else if ("Credit".equalsIgnoreCase(bill.getPaymode()) || "CREDIT".equalsIgnoreCase(bill.getStatus())) {
            mode = "k`oiDT ibala";
        } else if ("CLOSE".equalsIgnoreCase(bill.getStatus())) {
            mode = "k^Sa ibala";
        }

        cellHead = new PdfPCell(new Phrase(mode, fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
        cellHead.setPaddingTop(2f);
        cellHead.setPaddingBottom(2f);
        headerTable.addCell(cellHead);

        // Bill number and date in nested table
        PdfPTable nestedTable = new PdfPTable(2);
        nestedTable.setTotalWidth(contentWidth);
        nestedTable.setLockedWidth(true);
        nestedTable.setWidths(new float[]{50, 50});

        Phrase billPhrase = new Phrase();
        billPhrase.add(new Chunk("ibala naM.", fontMedium));
        billPhrase.add(new Chunk(String.valueOf(bill.getBillNo()), fontEnglishMedium));
        PdfPCell cellBill = new PdfPCell(billPhrase);
        cellBill.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellBill.setBorder(Rectangle.NO_BORDER);
        cellBill.setPaddingTop(2f);
        cellBill.setPaddingBottom(2f);
        cellBill.setPaddingLeft(3f);
        nestedTable.addCell(cellBill);

        Phrase datePhrase = new Phrase();
        datePhrase.add(new Chunk("idnaaMk ", fontMedium));
        datePhrase.add(new Chunk(bill.getBillDate(), fontEnglishMedium));
        PdfPCell cellDate = new PdfPCell(datePhrase);
        cellDate.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.setPaddingTop(2f);
        cellDate.setPaddingBottom(2f);
        cellDate.setPaddingRight(3f);
        nestedTable.addCell(cellDate);

        cellHead = new PdfPCell(nestedTable);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPadding(0f);
        headerTable.addCell(cellHead);

        // Add customer name for CREDIT bills
        if ("CREDIT".equalsIgnoreCase(bill.getStatus()) || "CREDIT".equalsIgnoreCase(bill.getPaymode())) {
            String customerName = getCustomerName(bill.getCustomerId());
            if (customerName != null && !customerName.isEmpty()) {
                Phrase customerPhrase = new Phrase();
                customerPhrase.add(new Chunk("ga`ahk : ", fontMedium));
                customerPhrase.add(new Chunk(customerName, fontMedium));
                PdfPCell cellCustomer = new PdfPCell(customerPhrase);
                cellCustomer.setHorizontalAlignment(Element.ALIGN_LEFT);
                cellCustomer.setBorder(Rectangle.NO_BORDER);
                cellCustomer.setPaddingTop(2f);
                cellCustomer.setPaddingBottom(2f);
                cellCustomer.setPaddingLeft(5f);
                headerTable.addCell(cellCustomer);
            }
        }

        // Add items table
        cellHead = new PdfPCell(itemsTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPadding(0f);
        cellHead.setPaddingBottom(5f);
        headerTable.addCell(cellHead);

        return headerTable;
    }

    /**
     * Load bill logo image from application settings.
     * @return Image object or null if logo is not configured or file doesn't exist
     */
    private Image loadBillLogoImage() {
        try {
            String billLogoPath = SessionService.getApplicationSetting(BILL_LOGO_SETTING);
            if (billLogoPath == null || billLogoPath.trim().isEmpty()) {
                LOG.debug("No bill logo configured");
                return null;
            }

            File logoFile = new File(billLogoPath);
            if (!logoFile.exists()) {
                LOG.warn("Bill logo file does not exist: {}", billLogoPath);
                return null;
            }

            Image logoImage = Image.getInstance(billLogoPath);
            LOG.info("Bill logo loaded from: {}", billLogoPath);
            return logoImage;

        } catch (Exception e) {
            LOG.warn("Could not load bill logo image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create items table - Compact and professional layout
     */
    private PdfPTable createItemsTable(List<Transaction> transactions, String tableName, String waitorName) throws Exception {
        float contentWidth = PAPER_WIDTH - 24f;

        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{78, 27, 27, 48});
        table.setLockedWidth(true);

        float rowHeight = 16f;

        // Table header
        PdfPCell cell = new PdfPCell(new Phrase("tapiSala", fontMedium));
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

        cell = new PdfPCell(new Phrase("dr", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("r@kma", fontMedium));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        // Add items
        if (transactions != null) {
            for (Transaction trans : transactions) {
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                c1.setPaddingTop(1f);
                c1.setPaddingBottom(2f);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontEnglishMedium));
                c2.setBorder(Rectangle.NO_BORDER);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPaddingTop(1f);
                c2.setPaddingBottom(2f);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontEnglishMedium));
                c3.setBorder(Rectangle.NO_BORDER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setPaddingTop(1f);
                c3.setPaddingBottom(2f);
                table.addCell(c3);

                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", trans.getAmt()), fontEnglishMedium));
                c4.setBorder(Rectangle.NO_BORDER);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                c4.setPaddingTop(1f);
                c4.setPaddingBottom(2f);
                table.addCell(c4);
            }
        }

        // Table number and Total row
        Phrase tablePhrase = new Phrase();
        tablePhrase.add(new Chunk("To naM. ", fontMedium));
        tablePhrase.add(new Chunk(tableName, fontEnglishMedium));
        PdfPCell cellTableNo = new PdfPCell(tablePhrase);
        cellTableNo.setFixedHeight(rowHeight);
        cellTableNo.setBorder(Rectangle.TOP);
        cellTableNo.setPaddingTop(1f);
        cellTableNo.setPaddingBottom(1f);
        cellTableNo.setColspan(2);
        table.addCell(cellTableNo);

        PdfPCell cellTotalLabel = new PdfPCell(new Phrase("ekuNa", fontMedium));
        cellTotalLabel.setFixedHeight(rowHeight);
        cellTotalLabel.setBorder(Rectangle.TOP);
        cellTotalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalLabel.setPaddingTop(1f);
        cellTotalLabel.setPaddingBottom(1f);
        table.addCell(cellTotalLabel);

        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell cellTotalAmt = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontEnglishMedium));
        cellTotalAmt.setFixedHeight(rowHeight);
        cellTotalAmt.setBorder(Rectangle.TOP);
        cellTotalAmt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalAmt.setPaddingTop(1f);
        cellTotalAmt.setPaddingBottom(1f);
        table.addCell(cellTotalAmt);

        // Waiter row
        PdfPCell cellWaiter = new PdfPCell(new Phrase("vaoTr :" + waitorName, fontMedium));
        cellWaiter.setFixedHeight(rowHeight);
        cellWaiter.setBorder(Rectangle.NO_BORDER);
        cellWaiter.setPaddingTop(1f);
        cellWaiter.setPaddingBottom(1f);
        cellWaiter.setColspan(4);
        table.addCell(cellWaiter);

        // Thank you footer
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 7);
        PdfPCell cellFooter = new PdfPCell(new Phrase(
                "Thanks for visit.....HAVE A NICE DAY\n________________________________________\nSoftware developed by Ankush Supnar (8329394603)",
                smallFont));
        cellFooter.setFixedHeight(32f);
        cellFooter.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellFooter.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellFooter.setBorder(Rectangle.NO_BORDER);
        cellFooter.setColspan(4);
        cellFooter.setPaddingTop(3f);
        table.addCell(cellFooter);

        return table;
    }

    // ============== Dynamic Restaurant Info Helper Methods ==============

    private String getRestaurantName() {
        String name = SessionService.getCurrentRestaurantName();
        return (name != null && !name.trim().isEmpty()) ? name : "Restaurant";
    }

    private String getRestaurantSubTitle() {
        String subTitle = SessionService.getCurrentRestaurantSubTitle();
        return (subTitle != null && !subTitle.trim().isEmpty()) ? subTitle : "";
    }

    private String getRestaurantAddress() {
        String address = SessionService.getCurrentRestaurantAddress();
        return (address != null && !address.trim().isEmpty()) ? address : "";
    }

    private String getRestaurantContacts() {
        String contact1 = SessionService.getCurrentRestaurantContact();
        String contact2 = SessionService.getCurrentRestaurantContact2();

        StringBuilder contacts = new StringBuilder();
        if (contact1 != null && !contact1.trim().isEmpty()) {
            contacts.append(contact1.trim());
        }
        if (contact2 != null && !contact2.trim().isEmpty()) {
            if (contacts.length() > 0) {
                contacts.append("   ");
            }
            contacts.append(contact2.trim());
        }
        return contacts.toString();
    }

    private String getRestaurantGstin() {
        String gstin = SessionService.getCurrentRestaurantGstin();
        return (gstin != null && !gstin.trim().isEmpty()) ? gstin : "";
    }

    // ============== End Dynamic Restaurant Info Helper Methods ==============

    private String getWaitorName(Integer waitorId) {
        if (waitorId == null) {
            return "-";
        }
        try {
            var waitor = employeesService.getEmployeeById(waitorId);
            return waitor != null ? waitor.getFirstName() : "-";
        } catch (Exception e) {
            LOG.warn("Could not get waiter name for ID {}", waitorId);
            return "-";
        }
    }

    private String getCustomerName(Integer customerId) {
        if (customerId == null) {
            return null;
        }
        try {
            Customer customer = customerService.getCustomerById(customerId);
            return customer != null ? customer.getFullName() : null;
        } catch (Exception e) {
            LOG.warn("Could not get customer name for ID {}", customerId);
            return null;
        }
    }

    /**
     * Print single bill in professional A4 format with logo and open in default PDF viewer
     */
    public boolean printBillA4(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        try {
            LOG.info("Generating A4 PDF with logo for bill #{}", bill.getBillNo());
            loadFonts();

            String pdfDir = getWritableOutputDirectory();
            if (pdfDir == null) {
                LOG.error("Could not find a writable directory for PDF output");
                return false;
            }

            String pdfPath = pdfDir + File.separator + "billA4.pdf";

            Document document = new Document(PageSize.A4, 40f, 40f, 30f, 30f);
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            createA4BillContent(document, bill, tableName);

            document.close();

            LOG.info("A4 Bill PDF with logo generated at: {}", pdfPath);

            openPdfInDefaultViewer(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating A4 bill PDF with logo: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create professional A4 bill content with logo
     */
    private void createA4BillContent(Document document, Bill bill, String tableName) throws Exception {
        String waitorName = getWaitorName(bill.getWaitorId());
        String customerName = getCustomerName(bill.getCustomerId());

        // Fonts for A4
        Font a4TitleFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 28f, Font.BOLD, BaseColor.BLACK);
        Font a4SubtitleFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 16f, Font.NORMAL, BaseColor.BLACK);
        Font a4AddressFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 12f, Font.NORMAL, BaseColor.DARK_GRAY);
        Font a4MarathiMedium = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 14f, Font.NORMAL, BaseColor.BLACK);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, BaseColor.BLACK);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.BLACK);
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD, BaseColor.WHITE);
        Font tableDataFont = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK);
        Font tableDataMarathiFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 12f, Font.NORMAL, BaseColor.BLACK);
        Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.BLACK);
        Font totalValueFont = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, new BaseColor(0, 100, 0));
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9f, Font.ITALIC, BaseColor.GRAY);

        // ========== HEADER SECTION: LOGO (LEFT) + RESTAURANT INFO (RIGHT) ==========
        PdfPCell cell;
        Image logoImage = loadBillLogoImage();
        if (logoImage != null) {
            // Two-column layout: logo on left, restaurant info on right
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{35, 65});

            // Left column: Logo image
            logoImage.scaleToFit(200f, 100f);
            PdfPCell logoCell = new PdfPCell(logoImage, false);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(5f);
            headerTable.addCell(logoCell);

            // Right column: Restaurant details
            PdfPTable infoTable = new PdfPTable(1);
            infoTable.setWidthPercentage(100);

            PdfPCell cellInfo = new PdfPCell(new Phrase(getRestaurantName(), a4TitleFont));
            cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellInfo.setBorder(Rectangle.NO_BORDER);
            cellInfo.setPaddingBottom(5f);
            infoTable.addCell(cellInfo);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(subTitle, a4SubtitleFont));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingBottom(5f);
                infoTable.addCell(cellInfo);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(address, a4AddressFont));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingBottom(3f);
                infoTable.addCell(cellInfo);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM. ", a4AddressFont));
                phonePhrase.add(new Chunk(contacts.replace("   ", " | "), new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.DARK_GRAY)));
                cellInfo = new PdfPCell(phonePhrase);
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingBottom(3f);
                infoTable.addCell(cellInfo);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase("GSTIN: " + gstin, new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingBottom(10f);
                infoTable.addCell(cellInfo);
            }

            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPadding(0f);
            headerTable.addCell(infoCell);

            document.add(headerTable);
        } else {
            // No logo - original single-column centered layout
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            cell = new PdfPCell(new Phrase(getRestaurantName(), a4TitleFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingBottom(5f);
            headerTable.addCell(cell);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cell = new PdfPCell(new Phrase(subTitle, a4SubtitleFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingBottom(5f);
                headerTable.addCell(cell);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cell = new PdfPCell(new Phrase(address, a4AddressFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingBottom(3f);
                headerTable.addCell(cell);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM. ", a4AddressFont));
                phonePhrase.add(new Chunk(contacts.replace("   ", " | "), new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.DARK_GRAY)));
                cell = new PdfPCell(phonePhrase);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingBottom(3f);
                headerTable.addCell(cell);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cell = new PdfPCell(new Phrase("GSTIN: " + gstin, new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingBottom(10f);
                headerTable.addCell(cell);
            }

            document.add(headerTable);
        }

        // ========== BILL TITLE ==========
        String billType = "k^Sa ibala";
        if ("Credit".equalsIgnoreCase(bill.getPaymode()) || "CREDIT".equalsIgnoreCase(bill.getStatus())) {
            billType = "k`oiDT ibala";
        }

        PdfPTable billTitleTable = new PdfPTable(1);
        billTitleTable.setWidthPercentage(100);
        cell = new PdfPCell(new Phrase(billType, new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 18f, Font.BOLD, BaseColor.WHITE)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new BaseColor(102, 126, 234));
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        billTitleTable.addCell(cell);
        document.add(billTitleTable);

        // ========== BILL INFO SECTION ==========
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{15, 35, 15, 35});
        infoTable.setSpacingBefore(15f);
        infoTable.setSpacingAfter(15f);

        addInfoCellMarathi(infoTable, "ibala naM.", a4MarathiMedium);
        addInfoCell(infoTable, String.valueOf(bill.getBillNo()), valueFont);
        addInfoCellMarathi(infoTable, "idnaaMk", a4MarathiMedium);
        addInfoCell(infoTable, bill.getBillDate() != null ? bill.getBillDate() : "-", valueFont);

        addInfoCellMarathi(infoTable, "To naM.", a4MarathiMedium);
        addInfoCell(infoTable, tableName, valueFont);
        addInfoCellMarathi(infoTable, "vaoL", a4MarathiMedium);
        addInfoCell(infoTable, bill.getBillTime() != null ? bill.getBillTime() : "-", valueFont);

        addInfoCellMarathi(infoTable, "vaoTr", a4MarathiMedium);
        addInfoCellMarathi(infoTable, waitorName, a4MarathiMedium);
        if (customerName != null && !customerName.isEmpty()) {
            addInfoCellMarathi(infoTable, "ga`ahk", a4MarathiMedium);
            addInfoCellMarathi(infoTable, customerName, a4MarathiMedium);
        } else {
            addInfoCell(infoTable, "", labelFont);
            addInfoCell(infoTable, "", valueFont);
        }

        document.add(infoTable);

        // ========== ITEMS TABLE ==========
        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{8, 44, 12, 16, 20});

        BaseColor headerBg = new BaseColor(102, 126, 234);
        Font tableHeaderMarathiFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 12f, Font.BOLD, BaseColor.WHITE);
        addItemHeaderCell(itemsTable, "k`.", tableHeaderMarathiFont, headerBg);
        addItemHeaderCell(itemsTable, "tapiSala", tableHeaderMarathiFont, headerBg);
        addItemHeaderCell(itemsTable, "naga", tableHeaderMarathiFont, headerBg);
        addItemHeaderCell(itemsTable, "dr", tableHeaderMarathiFont, headerBg);
        addItemHeaderCell(itemsTable, "r@kma", tableHeaderMarathiFont, headerBg);

        List<Transaction> transactions = bill.getTransactions();
        int srNo = 1;
        float totalAmount = 0f;

        if (transactions != null && !transactions.isEmpty()) {
            boolean alternate = false;
            for (Transaction trans : transactions) {
                BaseColor rowBg = alternate ? new BaseColor(245, 245, 250) : BaseColor.WHITE;

                addItemDataCell(itemsTable, String.valueOf(srNo++), tableDataFont, rowBg, Element.ALIGN_CENTER);
                addItemDataCell(itemsTable, trans.getItemName() != null ? trans.getItemName() : "-", tableDataMarathiFont, rowBg, Element.ALIGN_LEFT);
                addItemDataCell(itemsTable, String.valueOf(trans.getQty().intValue()), tableDataFont, rowBg, Element.ALIGN_CENTER);
                addItemDataCell(itemsTable, String.format("%.2f", trans.getRate()), tableDataFont, rowBg, Element.ALIGN_RIGHT);
                addItemDataCell(itemsTable, String.format("%.2f", trans.getAmt()), tableDataFont, rowBg, Element.ALIGN_RIGHT);

                totalAmount += trans.getAmt();
                alternate = !alternate;
            }
        }

        document.add(itemsTable);

        // ========== TOTALS SECTION ==========
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(15f);
        totalsTable.setWidths(new float[]{60, 40});

        addTotalRowA4Marathi(totalsTable, "ekuNa r@kma", String.format("%.2f", bill.getBillAmt() != null ? bill.getBillAmt() : totalAmount), a4MarathiMedium, valueFont);

        if (bill.getDiscount() != null && bill.getDiscount() > 0) {
            addTotalRowA4Marathi(totalsTable, "savalaT", String.format("%.2f", bill.getDiscount()), a4MarathiMedium, valueFont);
        }

        Font a4MarathiBold = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 16f, Font.BOLD, BaseColor.BLACK);
        addTotalRowA4Marathi(totalsTable, "eMkuNa", String.format("%.2f", bill.getNetAmount() != null ? bill.getNetAmount() : totalAmount), a4MarathiBold, totalValueFont);

        document.add(totalsTable);

        // ========== FOOTER ==========
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(40f);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk("Thank you for visiting! Have a nice day.", footerFont));
        document.add(footer);

        Paragraph devFooter = new Paragraph();
        devFooter.setSpacingBefore(20f);
        devFooter.setAlignment(Element.ALIGN_CENTER);
        devFooter.add(new Chunk("_______________________________________", footerFont));
        devFooter.add(Chunk.NEWLINE);
        devFooter.add(new Chunk("Software developed by Ankush Supnar (8329394603)", footerFont));
        document.add(devFooter);
    }

    // ============== A4 Helper Methods ==============

    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addInfoCellMarathi(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addItemHeaderCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        cell.setBorderColor(BaseColor.WHITE);
        table.addCell(cell);
    }

    private void addItemDataCell(PdfPTable table, String text, Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        table.addCell(cell);
    }

    private void addTotalRowA4Marathi(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    /**
     * Print multiple bills in A4 format PDF with logo and open in default PDF viewer
     */
    public boolean printMultipleBillsA4(Set<Bill> bills, Map<Integer, String> tableNameMap) {
        if (bills == null || bills.isEmpty()) {
            LOG.warn("No bills to print");
            return false;
        }

        try {
            LOG.info("Generating A4 PDF with logo for {} bills", bills.size());
            loadFonts();

            String pdfDir = getWritableOutputDirectory();
            if (pdfDir == null) {
                LOG.error("Could not find a writable directory for PDF output");
                return false;
            }

            String pdfPath = pdfDir + File.separator + "multipleBillsA4.pdf";

            Document document = new Document(PageSize.A4, 15f, 15f, 15f, 15f);
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);

            for (Bill bill : bills) {
                String tableName = tableNameMap.getOrDefault(bill.getTableNo(), "-");
                PdfPCell billCell = createBillCell(bill, tableName);
                mainTable.addCell(billCell);
            }

            if (bills.size() % 2 != 0) {
                PdfPCell emptyCell = new PdfPCell();
                emptyCell.setBorder(Rectangle.NO_BORDER);
                mainTable.addCell(emptyCell);
            }

            document.add(mainTable);
            document.close();

            LOG.info("Multiple bills A4 PDF with logo generated at: {}", pdfPath);

            openPdfInDefaultViewer(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating multiple bills PDF with logo: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a cell containing a single bill with logo in thermal print format
     */
    private PdfPCell createBillCell(Bill bill, String tableName) throws Exception {
        String waitorName = getWaitorName(bill.getWaitorId());

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setTotalWidth(new float[]{260});
        headerTable.setLockedWidth(true);

        // ===== HEADER: LOGO (LEFT) + RESTAURANT INFO (RIGHT) =====
        PdfPCell cellHead;
        Image logoImage = loadBillLogoImage();
        if (logoImage != null) {
            // Two-column layout: logo on left, restaurant info on right
            float logoColWidth = 260f * 0.40f;
            float infoColWidth = 260f * 0.60f;
            PdfPTable twoColHeader = new PdfPTable(2);
            twoColHeader.setTotalWidth(new float[]{logoColWidth, infoColWidth});
            twoColHeader.setLockedWidth(true);

            // Left column: Logo image
            logoImage.scaleToFit(logoColWidth - 4f, 70f);
            PdfPCell logoCell = new PdfPCell(logoImage, false);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(2f);
            twoColHeader.addCell(logoCell);

            // Right column: Restaurant details
            PdfPTable infoTable = new PdfPTable(1);
            infoTable.setTotalWidth(new float[]{infoColWidth});
            infoTable.setLockedWidth(true);

            PdfPCell cellInfo = new PdfPCell(new Phrase(getRestaurantName(), fontLarge));
            cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellInfo.setBorder(Rectangle.NO_BORDER);
            cellInfo.setPaddingTop(0f);
            cellInfo.setPaddingBottom(0f);
            infoTable.addCell(cellInfo);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(subTitle, fontMedium));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase(address, fontSmall));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
                phonePhrase.add(new Chunk(contacts, fontEnglishSmall));
                cellInfo = new PdfPCell(phonePhrase);
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cellInfo = new PdfPCell(new Phrase("GSTIN:- " + gstin, fontEnglishSmall));
                cellInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellInfo.setBorder(Rectangle.NO_BORDER);
                cellInfo.setPaddingTop(0f);
                cellInfo.setPaddingBottom(0f);
                infoTable.addCell(cellInfo);
            }

            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPadding(0f);
            twoColHeader.addCell(infoCell);

            // Add 2-column header to main table with bottom border separator
            PdfPCell headerCell = new PdfPCell(twoColHeader);
            headerCell.setBorder(Rectangle.BOTTOM);
            headerCell.setPadding(0f);
            headerCell.setPaddingBottom(2f);
            headerTable.addCell(headerCell);
        } else {
            // No logo - original single-column centered layout
            cellHead = new PdfPCell(new Phrase(getRestaurantName(), fontLarge));
            cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellHead.setBorder(Rectangle.NO_BORDER);
            cellHead.setPaddingTop(0f);
            cellHead.setPaddingBottom(0f);
            headerTable.addCell(cellHead);

            String subTitle = getRestaurantSubTitle();
            if (!subTitle.isEmpty()) {
                cellHead = new PdfPCell(new Phrase(subTitle, fontMedium));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String address = getRestaurantAddress();
            if (!address.isEmpty()) {
                cellHead = new PdfPCell(new Phrase(address, fontSmall));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String contacts = getRestaurantContacts();
            if (!contacts.isEmpty()) {
                Phrase phonePhrase = new Phrase();
                phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
                phonePhrase.add(new Chunk(contacts, fontEnglishSmall));
                cellHead = new PdfPCell(phonePhrase);
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.NO_BORDER);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(0f);
                headerTable.addCell(cellHead);
            }

            String gstin = getRestaurantGstin();
            if (!gstin.isEmpty()) {
                cellHead = new PdfPCell(new Phrase("GSTIN:- " + gstin, fontEnglishSmall));
                cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellHead.setBorder(Rectangle.BOTTOM);
                cellHead.setPaddingTop(0f);
                cellHead.setPaddingBottom(2f);
                headerTable.addCell(cellHead);
            }
        }

        // Bill type
        String mode = "";
        if ("Cash".equalsIgnoreCase(bill.getPaymode()) || "PENDING".equalsIgnoreCase(bill.getPaymode())) {
            mode = "k^Sa ibala";
        } else if ("Credit".equalsIgnoreCase(bill.getPaymode()) || "CREDIT".equalsIgnoreCase(bill.getStatus())) {
            mode = "k`oiDT ibala";
        } else if ("CLOSE".equalsIgnoreCase(bill.getStatus())) {
            mode = "k^Sa ibala";
        }

        cellHead = new PdfPCell(new Phrase(mode, fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
        cellHead.setPaddingTop(2f);
        cellHead.setPaddingBottom(2f);
        headerTable.addCell(cellHead);

        // Bill number and date
        PdfPTable nestedTable = new PdfPTable(2);
        nestedTable.setWidths(new float[]{50, 50});

        Phrase billPhrase = new Phrase();
        billPhrase.add(new Chunk("ibala naM.", fontMedium));
        billPhrase.add(new Chunk(String.valueOf(bill.getBillNo()), fontEnglishMedium));
        PdfPCell cellBill = new PdfPCell(billPhrase);
        cellBill.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellBill.setBorder(Rectangle.NO_BORDER);
        cellBill.setPaddingTop(2f);
        cellBill.setPaddingBottom(2f);
        nestedTable.addCell(cellBill);

        Phrase datePhrase = new Phrase();
        datePhrase.add(new Chunk("idnaaMk ", fontMedium));
        datePhrase.add(new Chunk(bill.getBillDate(), fontEnglishMedium));
        PdfPCell cellDate = new PdfPCell(datePhrase);
        cellDate.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.setPaddingTop(2f);
        cellDate.setPaddingBottom(2f);
        nestedTable.addCell(cellDate);

        cellHead = new PdfPCell(nestedTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Customer name for CREDIT bills
        if ("CREDIT".equalsIgnoreCase(bill.getStatus()) || "CREDIT".equalsIgnoreCase(bill.getPaymode())) {
            String customerName = getCustomerName(bill.getCustomerId());
            if (customerName != null && !customerName.isEmpty()) {
                Phrase customerPhrase = new Phrase();
                customerPhrase.add(new Chunk("ga`ahk : ", fontMedium));
                customerPhrase.add(new Chunk(customerName, fontMedium));
                PdfPCell cellCustomer = new PdfPCell(customerPhrase);
                cellCustomer.setHorizontalAlignment(Element.ALIGN_LEFT);
                cellCustomer.setBorder(Rectangle.NO_BORDER);
                cellCustomer.setPaddingTop(2f);
                cellCustomer.setPaddingBottom(2f);
                cellCustomer.setPaddingLeft(5f);
                headerTable.addCell(cellCustomer);
            }
        }

        // Items table
        PdfPTable itemsTable = createItemsTableForA4(bill.getTransactions(), tableName, waitorName);
        cellHead = new PdfPCell(itemsTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(5f);
        headerTable.addCell(cellHead);

        // Wrap in outer cell with border
        PdfPCell outerCell = new PdfPCell(headerTable);
        outerCell.setBorder(Rectangle.BOX);
        outerCell.setBorderColor(new BaseColor(180, 180, 180));
        outerCell.setPadding(5f);

        return outerCell;
    }

    /**
     * Create items table for A4 format
     */
    private PdfPTable createItemsTableForA4(List<Transaction> transactions, String tableName, String waitorName) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{120, 40, 40, 60});
        table.setLockedWidth(true);

        float rowHeight = 16f;

        PdfPCell cell = new PdfPCell(new Phrase("tapiSala", fontMedium));
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

        cell = new PdfPCell(new Phrase("dr", fontMedium));
        cell.setFixedHeight(rowHeight);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("r@kma", fontMedium));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setFixedHeight(rowHeight);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(1f);
        table.addCell(cell);

        if (transactions != null) {
            for (Transaction trans : transactions) {
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                c1.setPaddingTop(1f);
                c1.setPaddingBottom(2f);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontEnglishMedium));
                c2.setBorder(Rectangle.NO_BORDER);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPaddingTop(1f);
                c2.setPaddingBottom(2f);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontEnglishMedium));
                c3.setBorder(Rectangle.NO_BORDER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setPaddingTop(1f);
                c3.setPaddingBottom(2f);
                table.addCell(c3);

                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", trans.getAmt()), fontEnglishMedium));
                c4.setBorder(Rectangle.NO_BORDER);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                c4.setPaddingTop(1f);
                c4.setPaddingBottom(2f);
                table.addCell(c4);
            }
        }

        // Table number and Total row
        Phrase tablePhrase = new Phrase();
        tablePhrase.add(new Chunk("To naM. ", fontMedium));
        tablePhrase.add(new Chunk(tableName, fontEnglishMedium));
        PdfPCell cellTableNo = new PdfPCell(tablePhrase);
        cellTableNo.setFixedHeight(rowHeight);
        cellTableNo.setBorder(Rectangle.TOP);
        cellTableNo.setPaddingTop(1f);
        cellTableNo.setPaddingBottom(1f);
        cellTableNo.setColspan(2);
        table.addCell(cellTableNo);

        PdfPCell cellTotalLabel = new PdfPCell(new Phrase("ekuNa", fontMedium));
        cellTotalLabel.setFixedHeight(rowHeight);
        cellTotalLabel.setBorder(Rectangle.TOP);
        cellTotalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalLabel.setPaddingTop(1f);
        cellTotalLabel.setPaddingBottom(1f);
        table.addCell(cellTotalLabel);

        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell cellTotalAmt = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontEnglishMedium));
        cellTotalAmt.setFixedHeight(rowHeight);
        cellTotalAmt.setBorder(Rectangle.TOP);
        cellTotalAmt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalAmt.setPaddingTop(1f);
        cellTotalAmt.setPaddingBottom(1f);
        table.addCell(cellTotalAmt);

        // Waiter row
        PdfPCell cellWaiter = new PdfPCell(new Phrase("vaoTr :" + waitorName, fontMedium));
        cellWaiter.setFixedHeight(rowHeight);
        cellWaiter.setBorder(Rectangle.NO_BORDER);
        cellWaiter.setPaddingTop(1f);
        cellWaiter.setPaddingBottom(1f);
        cellWaiter.setColspan(4);
        table.addCell(cellWaiter);

        return table;
    }

    /**
     * Open PDF file in default system PDF viewer
     */
    private void openPdfInDefaultViewer(String pdfPath) {
        try {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(pdfFile);
                        LOG.info("Opened PDF in default viewer: {}", pdfPath);
                    } else {
                        LOG.warn("Desktop OPEN action not supported");
                    }
                } else {
                    String os = System.getProperty("os.name").toLowerCase();
                    ProcessBuilder pb;
                    if (os.contains("win")) {
                        pb = new ProcessBuilder("cmd", "/c", "start", "", pdfPath);
                    } else if (os.contains("mac")) {
                        pb = new ProcessBuilder("open", pdfPath);
                    } else {
                        pb = new ProcessBuilder("xdg-open", pdfPath);
                    }
                    pb.start();
                    LOG.info("Opened PDF using system command: {}", pdfPath);
                }
            } else {
                LOG.error("PDF file does not exist: {}", pdfPath);
            }
        } catch (Exception e) {
            LOG.error("Error opening PDF in default viewer: {}", e.getMessage(), e);
        }
    }

    /**
     * Print PDF to configured printer silently (no dialog)
     */
    private void printPdfToConfiguredPrinter(String pdfPath) {
        try {
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                LOG.error("PDF file does not exist for printing: {}", pdfPath);
                return;
            }

            String configuredPrinter = SessionService.getApplicationSetting("billing_printer");
            LOG.info("Configured billing printer: {}", configuredPrinter);

            PrintService printService = null;

            if (configuredPrinter != null && !configuredPrinter.trim().isEmpty()) {
                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
                for (PrintService service : printServices) {
                    if (service.getName().equalsIgnoreCase(configuredPrinter.trim())) {
                        printService = service;
                        LOG.info("Found configured printer: {}", service.getName());
                        break;
                    }
                }

                if (printService == null) {
                    LOG.warn("Configured printer '{}' not found, using default printer", configuredPrinter);
                }
            }

            if (printService == null) {
                printService = PrintServiceLookup.lookupDefaultPrintService();
                if (printService != null) {
                    LOG.info("Using default printer: {}", printService.getName());
                } else {
                    LOG.error("No default printer available");
                    return;
                }
            }

            try (PDDocument document = PDDocument.load(pdfFile)) {
                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPrintService(printService);
                printerJob.setPageable(new PDFPageable(document, Orientation.PORTRAIT));

                printerJob.print();
                LOG.info("Bill PDF with logo sent to printer: {}", printService.getName());
            }

        } catch (Exception e) {
            LOG.error("Error printing PDF to printer: {}", e.getMessage(), e);
        }
    }

    /**
     * Get list of available printers
     */
    public static String[] getAvailablePrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        String[] printerNames = new String[printServices.length];
        for (int i = 0; i < printServices.length; i++) {
            printerNames[i] = printServices[i].getName();
        }
        return printerNames;
    }
}
