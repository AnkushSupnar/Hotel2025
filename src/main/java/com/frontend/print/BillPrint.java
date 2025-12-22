package com.frontend.print;

import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.Transaction;
import com.frontend.service.CustomerService;
import com.frontend.service.EmployeeService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Bill Print class for thermal printer using iTextPDF
 * Generates PDF bill and prints to thermal printer
 */
@Component
public class BillPrint {

    private static final Logger LOG = LoggerFactory.getLogger(BillPrint.class);

    // PDF output path
    private static final String BILL_PDF_PATH = System.getProperty("user.home") + File.separator + "bill.pdf";

    // Paper width for 80mm thermal printer (in points, 1 inch = 72 points, 80mm ≈ 3.15 inches)
    private static final float PAPER_WIDTH = 226f;

    @Autowired
    private EmployeeService employeeService;

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
     * Print Bill to thermal printer
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

            // Generate PDF
            String pdfPath = generateBillPdf(bill, tableName);
            if (pdfPath == null) {
                LOG.error("Failed to generate bill PDF");
                return false;
            }

            // Print PDF
            boolean printed = printPdf(pdfPath);
            if (printed) {
                LOG.info("Bill #{} printed successfully", bill.getBillNo());
            }

            return printed;

        } catch (Exception e) {
            LOG.error("Error printing Bill #{}: {}", bill.getBillNo(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Print Bill with print dialog (for selecting printer)
     */
    public boolean printBillWithDialog(Bill bill, String tableName) {
        if (bill == null) {
            LOG.warn("No bill to print");
            return false;
        }

        try {
            LOG.info("Starting Bill PDF generation (with dialog) for Bill #{}", bill.getBillNo());

            // Load fonts
            loadFonts();

            // Generate PDF
            String pdfPath = generateBillPdf(bill, tableName);
            if (pdfPath == null) {
                LOG.error("Failed to generate bill PDF");
                return false;
            }

            // Print PDF with dialog
            boolean printed = printPdfWithDialog(pdfPath);
            if (printed) {
                LOG.info("Bill #{} printed successfully", bill.getBillNo());
            }

            return printed;

        } catch (Exception e) {
            LOG.error("Error printing Bill #{}: {}", bill.getBillNo(), e.getMessage(), e);
            return false;
        }
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

            Document document = new Document(pageSize, 3f, 3f, 5f, 5f);

            // Create PDF file
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(BILL_PDF_PATH));
            document.open();

            // Add header table (which includes items table)
            document.add(headerTable);

            document.close();

            LOG.info("Bill PDF generated at: {}", BILL_PDF_PATH);
            return BILL_PDF_PATH;

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
        cellHead.setPaddingTop(2f);
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
                "Thanks for visit.....HAVE A NICE DAY\n________________________________________\nDeveloped by Ankush(8329394603)",
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
            var waitor = employeeService.getEmployeeById(waitorId);
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
     * Print PDF using PDFBox
     */
    private boolean printPdf(String pdfPath) {
        try {
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                LOG.error("PDF file not found: {}", pdfPath);
                return false;
            }

            PDDocument document = PDDocument.load(pdfFile);
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));
            job.print();
            document.close();

            LOG.info("PDF printed successfully");
            return true;

        } catch (Exception e) {
            LOG.error("Error printing PDF: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Print PDF with dialog
     */
    private boolean printPdfWithDialog(String pdfPath) {
        try {
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                LOG.error("PDF file not found: {}", pdfPath);
                return false;
            }

            PDDocument document = PDDocument.load(pdfFile);
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));

            if (job.printDialog()) {
                job.print();
                document.close();
                LOG.info("PDF printed successfully");
                return true;
            } else {
                document.close();
                LOG.info("Print cancelled by user");
                return false;
            }

        } catch (Exception e) {
            LOG.error("Error printing PDF: {}", e.getMessage(), e);
            return false;
        }
    }
}
