package com.frontend.print;

import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.Transaction;
import com.frontend.service.CustomerService;
import com.frontend.service.EmployeesService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
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
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bill Print class for thermal printer using iTextPDF
 * Generates PDF bill and prints to thermal printer
 */
@Component
public class BillPrint {

    private static final Logger LOG = LoggerFactory.getLogger(BillPrint.class);

    // Default PDF output directory (fallback if not configured in settings)
    private static final String DEFAULT_BILL_PDF_DIR = "D:" + File.separator + "Hotel Software";

    // Paper width for 80mm thermal printer (in points, 1 inch = 72 points, 80mm ≈ 3.15 inches)
    private static final float PAPER_WIDTH = 226f;

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
     * Generate Bill PDF and save to configured document directory
     */
    public boolean printBill(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        try {
            LOG.info("Starting Bill PDF generation for Bill #{}", bill.getBillNo());

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
                LOG.error("Failed to generate bill PDF");
                return false;
            }

            LOG.info("Bill #{} PDF saved successfully at: {}", bill.getBillNo(), pdfPath);
            return true;

        } catch (Exception e) {
            LOG.error("Error generating Bill #{}: {}", bill.getBillNo(), e.getMessage(), e);
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
            fontLarge = new Font(baseFont, 20f, Font.BOLD, BaseColor.BLACK);
            fontMedium = new Font(baseFont, 12f, Font.NORMAL, BaseColor.BLACK);
            fontSmall = new Font(baseFont, 10f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishSmall = new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor.BLACK);
            fontEnglishMedium = new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK);

        } catch (Exception e) {
            LOG.error("Error loading fonts: {}", e.getMessage(), e);
            // Create fallback fonts
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

            // Create header table
            PdfPTable headerTable = createHeaderTable(bill, tableName, waitorName, itemsTable);

            // Calculate document height based on content
            float height = headerTable.getTotalHeight() + 20f;
            if (height < 200f) height = 200f;

            // Create page size for thermal printer - portrait orientation
            // For iText, we need to ensure width < height for portrait
            // Use lower-left corner at (0,0) and upper-right at (width, height)
            Rectangle pageSize = new Rectangle(0, 0, PAPER_WIDTH, height);
            pageSize.setRotation(0); // Ensure no rotation

            // Minimal margins - cut to cut from top (left, right, top, bottom)
            Document document = new Document(pageSize, 3f, 3f, 0f, 2f);

            // Create PDF file path using configured document directory
            String pdfDir = getPdfOutputDirectory();
            String pdfPath = pdfDir + File.separator + "bill.pdf";
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Add header table (which includes items table)
            document.add(headerTable);

            document.close();

            LOG.info("Bill PDF generated at: {}", pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Error generating bill PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create header table with hotel info
     */
    private PdfPTable createHeaderTable(Bill bill, String tableName, String waitorName, PdfPTable itemsTable) throws Exception {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setTotalWidth(new float[]{210});
        headerTable.setLockedWidth(true);

        // Hotel name - "AMjanaI k^fo"
        PdfPCell cellHead = new PdfPCell(new Phrase("AMjanaI k^fo", fontLarge));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f); // Cut to cut from top
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Sub-title - "f^imalaI rosTa^rMT"
        cellHead = new PdfPCell(new Phrase("f^imalaI rosTa^rMT", fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Address
        cellHead = new PdfPCell(new Phrase("mau.paosT.saaonaJ-.ta.naovaasaa.ija.Ahmadnagar", fontSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Phone numbers - Marathi label + English numbers
        Phrase phonePhrase = new Phrase();
        phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
        phonePhrase.add(new Chunk("9860419230   8552803030", fontEnglishSmall));
        cellHead = new PdfPCell(phonePhrase);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // GSTIN
        cellHead = new PdfPCell(new Phrase("GSTIN:- 27AGKPL2419AIZR", fontEnglishSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(2f);
        headerTable.addCell(cellHead);

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
        nestedTable.setWidths(new float[]{50, 50});

        // Bill number: Marathi label + English value
        Phrase billPhrase = new Phrase();
        billPhrase.add(new Chunk("ibala naM.", fontMedium));
        billPhrase.add(new Chunk(String.valueOf(bill.getBillNo()), fontEnglishMedium));
        PdfPCell cellBill = new PdfPCell(billPhrase);
        cellBill.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellBill.setBorder(Rectangle.NO_BORDER);
        cellBill.setPaddingTop(2f);
        cellBill.setPaddingBottom(2f);
        nestedTable.addCell(cellBill);

        // Date: Marathi label + English value
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

        // Add customer name for CREDIT bills
        if ("CREDIT".equalsIgnoreCase(bill.getStatus()) || "CREDIT".equalsIgnoreCase(bill.getPaymode())) {
            String customerName = getCustomerName(bill.getCustomerId());
            if (customerName != null && !customerName.isEmpty()) {
                // Customer name row: "ga`ahk : CustomerName" (left aligned)
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
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(5f);
        headerTable.addCell(cellHead);

        return headerTable;
    }

    /**
     * Create items table - Compact and professional layout
     */
    private PdfPTable createItemsTable(List<Transaction> transactions, String tableName, String waitorName) throws Exception {
        // Items table with 4 columns: Item, Qty, Rate, Amount
        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{100, 30, 30, 50});
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
                // Item name in Marathi font
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                c1.setPaddingTop(1f);
                c1.setPaddingBottom(2f);
                table.addCell(c1);

                // Qty in English font
                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontEnglishMedium));
                c2.setBorder(Rectangle.NO_BORDER);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPaddingTop(1f);
                c2.setPaddingBottom(2f);
                table.addCell(c2);

                // Rate in English font
                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontEnglishMedium));
                c3.setBorder(Rectangle.NO_BORDER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setPaddingTop(1f);
                c3.setPaddingBottom(2f);
                table.addCell(c3);

                // Amount in English font
                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", trans.getAmt()), fontEnglishMedium));
                c4.setBorder(Rectangle.NO_BORDER);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                c4.setPaddingTop(1f);
                c4.setPaddingBottom(2f);
                table.addCell(c4);
            }
        }

        // Table number and Total row combined
        // Table number: Marathi label + English value
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

        // Total label
        PdfPCell cellTotalLabel = new PdfPCell(new Phrase("ekuNa", fontMedium));
        cellTotalLabel.setFixedHeight(rowHeight);
        cellTotalLabel.setBorder(Rectangle.TOP);
        cellTotalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalLabel.setPaddingTop(1f);
        cellTotalLabel.setPaddingBottom(1f);
        table.addCell(cellTotalLabel);

        // Total amount in English font
        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell cellTotalAmt = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontEnglishMedium));
        cellTotalAmt.setFixedHeight(rowHeight);
        cellTotalAmt.setBorder(Rectangle.TOP);
        cellTotalAmt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalAmt.setPaddingTop(1f);
        cellTotalAmt.setPaddingBottom(1f);
        table.addCell(cellTotalAmt);

        // Waiter row - spans all columns
        PdfPCell cellWaiter = new PdfPCell(new Phrase("vaoTr :" + waitorName, fontMedium));
        cellWaiter.setFixedHeight(rowHeight);
        cellWaiter.setBorder(Rectangle.NO_BORDER);
        cellWaiter.setPaddingTop(1f);
        cellWaiter.setPaddingBottom(1f);
        cellWaiter.setColspan(4);
        table.addCell(cellWaiter);

        // Thank you footer - compact
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
     * Get waiter name by ID
     */
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

    /**
     * Get customer name by ID (for credit bills)
     */
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
     * Print single bill in professional A4 format and open in default PDF viewer
     * @param bill The bill to print
     * @param tableName The table name
     * @return true if successful, false otherwise
     */
    public boolean printBillA4(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        try {
            LOG.info("Generating A4 PDF for bill #{}", bill.getBillNo());
            loadFonts();

            // Get output directory
            String pdfDir = getWritableOutputDirectory();
            if (pdfDir == null) {
                LOG.error("Could not find a writable directory for PDF output");
                return false;
            }

            // Use fixed filename - override every time
            String pdfPath = pdfDir + File.separator + "billA4.pdf";

            // Create A4 document
            Document document = new Document(PageSize.A4, 40f, 40f, 30f, 30f);
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Create professional A4 bill
            createA4BillContent(document, bill, tableName);

            document.close();

            LOG.info("A4 Bill PDF generated at: {}", pdfPath);

            // Open PDF in default application
            openPdfInDefaultViewer(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating A4 bill PDF: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create professional A4 bill content
     */
    private void createA4BillContent(Document document, Bill bill, String tableName) throws Exception {
        String waitorName = getWaitorName(bill.getWaitorId());
        String customerName = getCustomerName(bill.getCustomerId());

        // Fonts for A4 - use custom Marathi fonts (scaled up for A4)
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

        // ========== HEADER SECTION ==========
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        // Hotel Name - Marathi font
        PdfPCell cell = new PdfPCell(new Phrase("AMjanaI k^fo", a4TitleFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5f);
        headerTable.addCell(cell);

        // Subtitle - Marathi font
        cell = new PdfPCell(new Phrase("f^imalaI rosTa^rMT", a4SubtitleFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5f);
        headerTable.addCell(cell);

        // Address - Marathi font
        cell = new PdfPCell(new Phrase("mau.paosT.saaonaJ-.ta.naovaasaa.ija.Ahmadnagar", a4AddressFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(3f);
        headerTable.addCell(cell);

        // Phone - Marathi label + English numbers
        Phrase phonePhrase = new Phrase();
        phonePhrase.add(new Chunk("maaobaa[la naM. ", a4AddressFont));
        phonePhrase.add(new Chunk("9860419230 | 8552803030", new Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.DARK_GRAY)));
        cell = new PdfPCell(phonePhrase);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(3f);
        headerTable.addCell(cell);

        // GSTIN - English font
        cell = new PdfPCell(new Phrase("GSTIN: 27AGKPL2419AIZR", new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.DARK_GRAY)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(10f);
        headerTable.addCell(cell);

        document.add(headerTable);

        // ========== BILL TITLE ========== (Marathi text for bill type)
        String billType = "k^Sa ibala";  // Cash Bill in Marathi
        if ("Credit".equalsIgnoreCase(bill.getPaymode()) || "CREDIT".equalsIgnoreCase(bill.getStatus())) {
            billType = "k`oiDT ibala";  // Credit Bill in Marathi
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

        // ========== BILL INFO SECTION ========== (Marathi labels)
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{15, 35, 15, 35});
        infoTable.setSpacingBefore(15f);
        infoTable.setSpacingAfter(15f);

        // Row 1: Bill No and Date - Marathi labels
        addInfoCellMarathi(infoTable, "ibala naM.", a4MarathiMedium);
        addInfoCell(infoTable, String.valueOf(bill.getBillNo()), valueFont);
        addInfoCellMarathi(infoTable, "idnaaMk", a4MarathiMedium);
        addInfoCell(infoTable, bill.getBillDate() != null ? bill.getBillDate() : "-", valueFont);

        // Row 2: Table and Time - Marathi labels
        addInfoCellMarathi(infoTable, "To naM.", a4MarathiMedium);
        addInfoCell(infoTable, tableName, valueFont);
        addInfoCellMarathi(infoTable, "vaoL", a4MarathiMedium);
        addInfoCell(infoTable, bill.getBillTime() != null ? bill.getBillTime() : "-", valueFont);

        // Row 3: Waiter and Customer (if credit) - Marathi labels and values
        addInfoCellMarathi(infoTable, "vaoTr", a4MarathiMedium);
        addInfoCellMarathi(infoTable, waitorName, a4MarathiMedium);  // Waiter name in Marathi font
        if (customerName != null && !customerName.isEmpty()) {
            addInfoCellMarathi(infoTable, "ga`ahk", a4MarathiMedium);
            addInfoCellMarathi(infoTable, customerName, a4MarathiMedium);  // Customer name in Marathi font
        } else {
            addInfoCell(infoTable, "", labelFont);
            addInfoCell(infoTable, "", valueFont);
        }

        document.add(infoTable);

        // ========== ITEMS TABLE ========== (Marathi headers)
        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{8, 44, 12, 16, 20});

        // Header row - Marathi headers
        BaseColor headerBg = new BaseColor(102, 126, 234);
        Font tableHeaderMarathiFont = new Font(baseFont != null ? baseFont : BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED), 12f, Font.BOLD, BaseColor.WHITE);
        addItemHeaderCell(itemsTable, "k`.", tableHeaderMarathiFont, headerBg);  // Sr.
        addItemHeaderCell(itemsTable, "tapiSala", tableHeaderMarathiFont, headerBg);  // Item Description
        addItemHeaderCell(itemsTable, "naga", tableHeaderMarathiFont, headerBg);  // Qty
        addItemHeaderCell(itemsTable, "dr", tableHeaderMarathiFont, headerBg);  // Rate
        addItemHeaderCell(itemsTable, "r@kma", tableHeaderMarathiFont, headerBg);  // Amount

        // Item rows
        List<Transaction> transactions = bill.getTransactions();
        int srNo = 1;
        float totalAmount = 0f;

        if (transactions != null && !transactions.isEmpty()) {
            boolean alternate = false;
            for (Transaction trans : transactions) {
                BaseColor rowBg = alternate ? new BaseColor(245, 245, 250) : BaseColor.WHITE;

                addItemDataCell(itemsTable, String.valueOf(srNo++), tableDataFont, rowBg, Element.ALIGN_CENTER);
                // Item name in Marathi font
                addItemDataCell(itemsTable, trans.getItemName() != null ? trans.getItemName() : "-", tableDataMarathiFont, rowBg, Element.ALIGN_LEFT);
                addItemDataCell(itemsTable, String.valueOf(trans.getQty().intValue()), tableDataFont, rowBg, Element.ALIGN_CENTER);
                addItemDataCell(itemsTable, String.format("%.2f", trans.getRate()), tableDataFont, rowBg, Element.ALIGN_RIGHT);
                addItemDataCell(itemsTable, String.format("%.2f", trans.getAmt()), tableDataFont, rowBg, Element.ALIGN_RIGHT);

                totalAmount += trans.getAmt();
                alternate = !alternate;
            }
        }

        document.add(itemsTable);

        // ========== TOTALS SECTION ========== (Marathi labels)
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(15f);
        totalsTable.setWidths(new float[]{60, 40});

        // Sub Total - Marathi label
        addTotalRowA4Marathi(totalsTable, "ekuNa r@kma", String.format("%.2f", bill.getBillAmt() != null ? bill.getBillAmt() : totalAmount), a4MarathiMedium, valueFont);

        // Discount (if any) - Marathi label
        if (bill.getDiscount() != null && bill.getDiscount() > 0) {
            addTotalRowA4Marathi(totalsTable, "savalaT", String.format("%.2f", bill.getDiscount()), a4MarathiMedium, valueFont);
        }

        // Grand Total - Marathi label
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

    /**
     * Helper: Add info cell to bill info table
     */
    private void addInfoCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    /**
     * Helper: Add info cell with Marathi font to bill info table
     */
    private void addInfoCellMarathi(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    /**
     * Helper: Add header cell to items table
     */
    private void addItemHeaderCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        cell.setBorderColor(BaseColor.WHITE);
        table.addCell(cell);
    }

    /**
     * Helper: Add data cell to items table
     */
    private void addItemDataCell(PdfPTable table, String text, Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        table.addCell(cell);
    }

    /**
     * Helper: Add row to totals table
     */
    private void addTotalRowA4(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
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
     * Helper: Add row to totals table with Marathi label
     */
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
     * Print multiple bills in A4 format PDF and open in default PDF viewer
     * @param bills Set of bills to print
     * @param tableNameMap Map of table IDs to table names
     * @return true if successful, false otherwise
     */
    public boolean printMultipleBillsA4(Set<Bill> bills, Map<Integer, String> tableNameMap) {
        if (bills == null || bills.isEmpty()) {
            LOG.warn("No bills to print");
            return false;
        }

        try {
            LOG.info("Generating A4 PDF for {} bills", bills.size());
            loadFonts();

            // Get output directory with fallback options
            String pdfDir = getWritableOutputDirectory();
            if (pdfDir == null) {
                LOG.error("Could not find a writable directory for PDF output");
                return false;
            }

            // Use fixed filename - override every time
            String pdfPath = pdfDir + File.separator + "multipleBillsA4.pdf";

            // Create A4 document
            Document document = new Document(PageSize.A4, 15f, 15f, 15f, 15f);
            PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
            document.open();

            // Create main table with 2 columns for bills (2 bills per row)
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);

            // Add each bill as a cell
            for (Bill bill : bills) {
                String tableName = tableNameMap.getOrDefault(bill.getTableNo(), "-");
                PdfPCell billCell = createBillCell(bill, tableName);
                mainTable.addCell(billCell);
            }

            // If odd number of bills, add empty cell
            if (bills.size() % 2 != 0) {
                PdfPCell emptyCell = new PdfPCell();
                emptyCell.setBorder(Rectangle.NO_BORDER);
                mainTable.addCell(emptyCell);
            }

            document.add(mainTable);
            document.close();

            LOG.info("Multiple bills A4 PDF generated at: {}", pdfPath);

            // Open PDF in default application
            openPdfInDefaultViewer(pdfPath);

            return true;

        } catch (Exception e) {
            LOG.error("Error generating multiple bills PDF: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a cell containing a single bill in thermal print format (same as BillPrint thermal format)
     */
    private PdfPCell createBillCell(Bill bill, String tableName) throws Exception {
        String waitorName = getWaitorName(bill.getWaitorId());

        // Create the bill table same as thermal printer format
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setTotalWidth(new float[]{260});
        headerTable.setLockedWidth(true);

        // Hotel name - "AMjanaI k^fo"
        PdfPCell cellHead = new PdfPCell(new Phrase("AMjanaI k^fo", fontLarge));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Sub-title - "f^imalaI rosTa^rMT"
        cellHead = new PdfPCell(new Phrase("f^imalaI rosTa^rMT", fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Address
        cellHead = new PdfPCell(new Phrase("mau.paosT.saaonaJ-.ta.naovaasaa.ija.Ahmadnagar", fontSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // Phone numbers - Marathi label + English numbers
        Phrase phonePhrase = new Phrase();
        phonePhrase.add(new Chunk("maaobaa[la naM.", fontSmall));
        phonePhrase.add(new Chunk("9860419230   8552803030", fontEnglishSmall));
        cellHead = new PdfPCell(phonePhrase);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(0f);
        headerTable.addCell(cellHead);

        // GSTIN
        cellHead = new PdfPCell(new Phrase("GSTIN:- 27AGKPL2419AIZR", fontEnglishSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
        cellHead.setPaddingTop(0f);
        cellHead.setPaddingBottom(2f);
        headerTable.addCell(cellHead);

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
        nestedTable.setWidths(new float[]{50, 50});

        // Bill number: Marathi label + English value
        Phrase billPhrase = new Phrase();
        billPhrase.add(new Chunk("ibala naM.", fontMedium));
        billPhrase.add(new Chunk(String.valueOf(bill.getBillNo()), fontEnglishMedium));
        PdfPCell cellBill = new PdfPCell(billPhrase);
        cellBill.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellBill.setBorder(Rectangle.NO_BORDER);
        cellBill.setPaddingTop(2f);
        cellBill.setPaddingBottom(2f);
        nestedTable.addCell(cellBill);

        // Date: Marathi label + English value
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

        // Create and add items table
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
     * Create items table for A4 format - same style as thermal printer
     */
    private PdfPTable createItemsTableForA4(List<Transaction> transactions, String tableName, String waitorName) throws Exception {
        // Items table with 4 columns: Item, Qty, Rate, Amount
        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{120, 40, 40, 60});
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

        // Add items with English font for numbers
        if (transactions != null) {
            for (Transaction trans : transactions) {
                // Item name in Marathi font
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                c1.setPaddingTop(1f);
                c1.setPaddingBottom(2f);
                table.addCell(c1);

                // Qty in English font
                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontEnglishMedium));
                c2.setBorder(Rectangle.NO_BORDER);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c2.setPaddingTop(1f);
                c2.setPaddingBottom(2f);
                table.addCell(c2);

                // Rate in English font
                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontEnglishMedium));
                c3.setBorder(Rectangle.NO_BORDER);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setPaddingTop(1f);
                c3.setPaddingBottom(2f);
                table.addCell(c3);

                // Amount in English font
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

        // Total label
        PdfPCell cellTotalLabel = new PdfPCell(new Phrase("ekuNa", fontMedium));
        cellTotalLabel.setFixedHeight(rowHeight);
        cellTotalLabel.setBorder(Rectangle.TOP);
        cellTotalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalLabel.setPaddingTop(1f);
        cellTotalLabel.setPaddingBottom(1f);
        table.addCell(cellTotalLabel);

        // Total amount in English font
        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell cellTotalAmt = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontEnglishMedium));
        cellTotalAmt.setFixedHeight(rowHeight);
        cellTotalAmt.setBorder(Rectangle.TOP);
        cellTotalAmt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotalAmt.setPaddingTop(1f);
        cellTotalAmt.setPaddingBottom(1f);
        table.addCell(cellTotalAmt);

        // Waiter row - spans all columns
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
                    // Fallback for systems where Desktop is not supported
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
}