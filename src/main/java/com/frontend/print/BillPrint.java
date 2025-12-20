package com.frontend.print;

import com.frontend.entity.Bill;
import com.frontend.entity.Transaction;
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

            // Create document with dynamic height
            Rectangle pageSize = new Rectangle(PAPER_WIDTH, height);
            Document document = new Document(pageSize, 3f, 3f, 5f, 5f);

            // Create PDF file
            PdfWriter.getInstance(document, new FileOutputStream(BILL_PDF_PATH));
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

        // Hotel name - "AMjanaI k^fo" or "ha^Tola AMjanaI"
        PdfPCell cellHead = new PdfPCell(new Phrase("AMjanaI k^fo", fontLarge));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setVerticalAlignment(Element.ALIGN_TOP);
        cellHead.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(cellHead);

        // Sub-title - "f^imalaI rosTa^rMT"
        cellHead = new PdfPCell(new Phrase("f^imalaI rosTa^rMT", fontMedium));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setVerticalAlignment(Element.ALIGN_TOP);
        cellHead.setPaddingTop(-2f);
        cellHead.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(cellHead);

        // Address
        cellHead = new PdfPCell(new Phrase("mau.paosT.saaonaJ-.ta.naovaasaa.ija.Ahmadnagar", fontSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(-2f);
        headerTable.addCell(cellHead);

        // Phone numbers
        cellHead = new PdfPCell(new Phrase("maaobaa[la naM.9860419230   8552803030", fontSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setPaddingTop(-2f);
        cellHead.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(cellHead);

        // GSTIN
        cellHead = new PdfPCell(new Phrase("GSTIN:- 27AGKPL2419AIZR", fontEnglishSmall));
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.BOTTOM);
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
        cellHead.setPaddingTop(-2f);
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
        cellBill.setPaddingTop(-2f);
        nestedTable.addCell(cellBill);

        // Date: Marathi label + English value
        Phrase datePhrase = new Phrase();
        datePhrase.add(new Chunk("idnaaMk ", fontMedium));
        datePhrase.add(new Chunk(bill.getBillDate(), fontEnglishMedium));
        PdfPCell cellDate = new PdfPCell(datePhrase);
        cellDate.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.setPaddingTop(-2f);
        nestedTable.addCell(cellDate);

        cellHead = new PdfPCell(nestedTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingTop(-2f);
        headerTable.addCell(cellHead);

        // Add items table
        cellHead = new PdfPCell(itemsTable);
        cellHead.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead.setBorder(Rectangle.NO_BORDER);
        cellHead.setPaddingBottom(10f);
        headerTable.addCell(cellHead);

        return headerTable;
    }

    /**
     * Create items table
     */
    private PdfPTable createItemsTable(List<Transaction> transactions, String tableName, String waitorName) throws Exception {
        // Items table with 4 columns: Item, Qty, Rate, Amount
        PdfPTable table = new PdfPTable(4);
        table.setTotalWidth(new float[]{100, 30, 30, 50});
        table.setLockedWidth(true);

        // Table header
        PdfPCell cell = new PdfPCell(new Phrase("tapiSala", fontMedium));
        cell.setFixedHeight(20);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("naga", fontMedium));
        cell.setFixedHeight(20);
        cell.setVerticalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("dr", fontMedium));
        cell.setFixedHeight(20);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("r@kma", fontMedium));
        cell.setVerticalAlignment(Element.ALIGN_LEFT);
        cell.setFixedHeight(20);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        table.addCell(cell);

        // Add items
        if (transactions != null) {
            for (Transaction trans : transactions) {
                PdfPCell c1 = new PdfPCell(new Phrase(trans.getItemName(), fontMedium));
                c1.setBorder(Rectangle.NO_BORDER);
                c1.setNoWrap(false);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(trans.getQty().intValue()), fontMedium));
                c2.setFixedHeight(20);
                c2.setBorder(Rectangle.NO_BORDER);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.0f", trans.getRate()), fontMedium));
                c3.setFixedHeight(20);
                c3.setBorder(Rectangle.NO_BORDER);
                table.addCell(c3);

                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", trans.getAmt()), fontMedium));
                c4.setFixedHeight(20);
                c4.setBorder(Rectangle.NO_BORDER);
                table.addCell(c4);
            }
        }

        // Table number row: Marathi label + English value
        PdfPTable nestedTable1 = new PdfPTable(2);
        nestedTable1.setWidths(new float[]{50, 50});

        PdfPCell c1 = new PdfPCell(new Phrase("To naM.", fontMedium));
        c1.setFixedHeight(20);
        c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c1.setBorder(Rectangle.NO_BORDER);
        nestedTable1.addCell(c1);

        // Table name in English font
        PdfPCell cellTable = new PdfPCell(new Phrase(tableName, fontEnglishMedium));
        cellTable.setFixedHeight(20);
        cellTable.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellTable.setBorder(Rectangle.NO_BORDER);
        nestedTable1.addCell(cellTable);

        PdfPCell cellHead1 = new PdfPCell(nestedTable1);
        cellHead1.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellHead1.setBorder(Rectangle.TOP);
        cellHead1.setPaddingTop(-2f);
        table.addCell(cellHead1);

        PdfPCell empty = new PdfPCell();
        empty.setHorizontalAlignment(Element.ALIGN_CENTER);
        empty.setBorder(Rectangle.TOP);
        empty.setPaddingTop(-2f);
        table.addCell(empty);

        // Total label and amount
        PdfPCell c3 = new PdfPCell(new Phrase("ekuNa", fontMedium));
        c3.setFixedHeight(20);
        c3.setBorder(Rectangle.TOP);
        c3.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(c3);

        float totalAmt = transactions != null ?
                (float) transactions.stream().mapToDouble(t -> t.getAmt()).sum() : 0f;
        PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.0f", totalAmt), fontMedium));
        c4.setFixedHeight(20);
        c4.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c4.setBorder(Rectangle.TOP);
        table.addCell(c4);

        // Waiter row
        PdfPCell c5 = new PdfPCell(new Phrase("vaoTr :" + waitorName, fontMedium));
        c5.setFixedHeight(20);
        c5.setVerticalAlignment(Element.ALIGN_RIGHT);
        c5.setBorder(Rectangle.NO_BORDER);
        table.addCell(c5);

        // Empty cells
        for (int i = 0; i < 3; i++) {
            PdfPCell emptyCell = new PdfPCell(new Phrase(""));
            emptyCell.setFixedHeight(20);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(emptyCell);
        }

        // Thank you footer
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 7);
        PdfPCell c9 = new PdfPCell(new Phrase(
                "Thanks for visit.....HAVE A NICE DAY\n___________________________________________________\nDeveloped by Ankush(8329394603)",
                smallFont));
        c9.setFixedHeight(50);
        c9.setVerticalAlignment(Element.ALIGN_TOP);
        c9.setHorizontalAlignment(Element.ALIGN_CENTER);
        c9.setBorder(Rectangle.NO_BORDER);
        c9.setColspan(4);
        table.addCell(c9);

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
