package com.frontend.print;

import com.frontend.entity.BillPayment;
import com.frontend.entity.PurchaseBill;
import com.frontend.entity.Supplier;
import com.frontend.repository.PurchaseBillRepository;
import com.frontend.service.SessionService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Compact Professional Payment Receipt Print
 * - Half A4 page size
 * - Table-based layout
 * - Custom font throughout
 */
@Component
public class PayReceiptPrint {

    private static final Logger LOG = LoggerFactory.getLogger(PayReceiptPrint.class);

    private static final String PDF_DIR = "D:" + File.separator + "Hotel Software";

    // Half A4 page with room for 20px fonts - A4 width, ~60% height
    private static final Rectangle HALF_A4 = new Rectangle(595f, 500f); // A4 width, adjusted height

    // Colors
    private static final BaseColor BLACK = BaseColor.BLACK;
    private static final BaseColor GRAY = new BaseColor(80, 80, 80);
    private static final BaseColor LIGHT_GRAY = new BaseColor(180, 180, 180);
    private static final BaseColor BORDER_COLOR = new BaseColor(200, 200, 200);

    @Autowired
    private PurchaseBillRepository purchaseBillRepository;

    // Fonts
    private BaseFont customBaseFont;
    private Font fontHeader;
    private Font fontTitle;
    private Font fontLabel;
    private Font fontValue;
    private Font fontBold;
    private Font fontAmount;
    private Font fontSmall;
    private Font englishFont;        // English font 14px for dates, numbers, amounts, symbols
    private Font englishFontBold;    // English font 14px bold
    private Font englishFontLarge;   // English font 18px bold for amounts

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    public boolean printPaymentReceipt(BillPayment payment, Supplier supplier) {
        if (payment == null) return false;
        try {
            loadFonts();
            ensureOutputDir();
            String pdfPath = generateCompactReceipt(payment, supplier);
            if (pdfPath != null) {
                openPdf(pdfPath);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error generating receipt: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean printPaymentReceiptWithDialog(BillPayment payment, Supplier supplier, Window ownerWindow) {
        return printPaymentReceipt(payment, supplier);
    }

    /**
     * Print a combined receipt for multiple bill payments
     */
    public boolean printMultiPaymentReceipt(List<BillPayment> payments, Supplier supplier) {
        if (payments == null || payments.isEmpty()) return false;
        try {
            loadFonts();
            ensureOutputDir();
            String pdfPath = generateMultiBillReceipt(payments, supplier);
            if (pdfPath != null) {
                openPdf(pdfPath);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error generating multi-bill receipt: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean printMultiPaymentReceiptWithDialog(List<BillPayment> payments, Supplier supplier, Window ownerWindow) {
        return printMultiPaymentReceipt(payments, supplier);
    }

    private void ensureOutputDir() {
        File dir = new File(PDF_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private void loadFonts() {
        try {
            String fontPath = SessionService.getApplicationSetting("input_font_path");
            if (fontPath != null && !fontPath.trim().isEmpty() && new File(fontPath).exists()) {
                customBaseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                try {
                    customBaseFont = BaseFont.createFont("/fonts/kiran.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception e) {
                    customBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
                }
            }

            // Font sizes - Custom font 20px for Marathi, English 14px for dates/numbers/amounts/symbols
            fontHeader = new Font(customBaseFont, 24f, Font.BOLD, BLACK);      // Header - larger
            fontTitle = new Font(customBaseFont, 20f, Font.BOLD, BLACK);       // Marathi titles - 20px
            fontLabel = new Font(customBaseFont, 20f, Font.NORMAL, GRAY);      // Marathi labels - 20px
            fontValue = new Font(customBaseFont, 20f, Font.NORMAL, BLACK);     // Marathi values - 20px
            fontBold = new Font(customBaseFont, 20f, Font.BOLD, BLACK);        // Marathi bold - 20px
            fontAmount = new Font(customBaseFont, 22f, Font.BOLD, BLACK);      // Amount highlight (Marathi)
            fontSmall = new Font(customBaseFont, 14f, Font.NORMAL, GRAY);      // Small text

            // English fonts for dates, numbers, amounts, symbols
            englishFont = new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, BLACK);
            englishFontBold = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BLACK);
            englishFontLarge = new Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BLACK);
        } catch (Exception e) {
            LOG.error("Font error: {}", e.getMessage());
            fontHeader = new Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, BLACK);
            fontTitle = new Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BLACK);
            fontLabel = new Font(Font.FontFamily.HELVETICA, 20f, Font.NORMAL, GRAY);
            fontValue = new Font(Font.FontFamily.HELVETICA, 20f, Font.NORMAL, BLACK);
            fontBold = new Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BLACK);
            fontAmount = new Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD, BLACK);
            fontSmall = new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, GRAY);
            englishFont = new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, BLACK);
            englishFontBold = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BLACK);
            englishFontLarge = new Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BLACK);
        }
    }

    private String generateCompactReceipt(BillPayment payment, Supplier supplier) {
        try {
            // Half A4 with small margins
            Document doc = new Document(HALF_A4, 25f, 25f, 15f, 15f);
            String pdfPath = PDF_DIR + File.separator + "PayReceipt.pdf";
            PdfWriter.getInstance(doc, new FileOutputStream(pdfPath));
            doc.open();

            PurchaseBill bill = getBillDetails(payment.getBillNo());

            // Main container table - single column
            PdfPTable mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(100);

            // === HEADER SECTION ===
            mainTable.addCell(createHeaderCell());

            // === RECEIPT INFO (Receipt No + Date) ===
            mainTable.addCell(createReceiptInfoCell(payment));

            // === TWO COLUMN LAYOUT: Supplier | Payment Details ===
            mainTable.addCell(createTwoColumnSection(payment, supplier));

            // === BILL SUMMARY TABLE ===
            mainTable.addCell(createBillSummaryCell(payment, bill, supplier));

            // === SIGNATURE + FOOTER ===
            mainTable.addCell(createFooterCell());

            doc.add(mainTable);
            doc.close();

            LOG.info("Compact receipt generated: {}", pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("PDF generation error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate a combined receipt for multiple bill payments
     */
    private String generateMultiBillReceipt(List<BillPayment> payments, Supplier supplier) {
        try {
            // Use taller page for multiple bills
            Rectangle pageSize = new Rectangle(595f, 550f + (payments.size() * 30f));
            Document doc = new Document(pageSize, 25f, 25f, 15f, 15f);
            String pdfPath = PDF_DIR + File.separator + "PayReceipt.pdf";
            PdfWriter.getInstance(doc, new FileOutputStream(pdfPath));
            doc.open();

            // Calculate totals
            double totalPaid = 0.0;
            for (BillPayment p : payments) {
                totalPaid += (p.getPaymentAmount() != null ? p.getPaymentAmount() : 0.0);
            }

            // Main container table
            PdfPTable mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(100);

            // === HEADER SECTION ===
            mainTable.addCell(createHeaderCell());

            // === RECEIPT INFO (Receipt Nos + Date) ===
            mainTable.addCell(createMultiReceiptInfoCell(payments));

            // === SUPPLIER INFO ===
            mainTable.addCell(createMultiSupplierCell(supplier, totalPaid));

            // === BILL PAYMENTS TABLE ===
            mainTable.addCell(createBillPaymentsTableCell(payments));

            // === SUMMARY SECTION ===
            mainTable.addCell(createMultiBillSummaryCell(payments, supplier, totalPaid));

            // === SIGNATURE + FOOTER ===
            mainTable.addCell(createFooterCell());

            doc.add(mainTable);
            doc.close();

            LOG.info("Multi-bill receipt generated: {} bills, Total: Rs. {}", payments.size(), totalPaid);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Multi-bill PDF generation error: {}", e.getMessage(), e);
            return null;
        }
    }

    private PdfPCell createMultiReceiptInfoCell(List<BillPayment> payments) {
        PdfPTable infoTable = new PdfPTable(4);
        try {
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{22, 28, 22, 28});
        } catch (Exception ignored) {}

        // Receipt Nos - show first and last if multiple
        String receiptNos;
        if (payments.size() == 1) {
            receiptNos = "#" + payments.get(0).getId();
        } else {
            receiptNos = "#" + payments.get(0).getId() + " - #" + payments.get(payments.size() - 1).getId();
        }
        addLabelValuePair(infoTable, "pavataI k`. :", receiptNos);

        // Date
        String date = payments.get(0).getPaymentDate() != null ?
                payments.get(0).getPaymentDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        addLabelValuePair(infoTable, "idnaaMk :", date);

        PdfPCell wrapper = new PdfPCell(infoTable);
        wrapper.setBorder(Rectangle.BOTTOM);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(5f);
        return wrapper;
    }

    private PdfPCell createMultiSupplierCell(Supplier supplier, double totalPaid) {
        PdfPTable twoCol = new PdfPTable(2);
        try {
            twoCol.setWidthPercentage(100);
            twoCol.setWidths(new float[]{50, 50});
        } catch (Exception ignored) {}

        // LEFT: Supplier Info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.RIGHT);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setPadding(8f);

        Paragraph title = new Paragraph("paurvazadaracaO maaihtaI", fontTitle);
        title.setSpacingAfter(5f);
        leftCell.addElement(title);

        PdfPTable details = new PdfPTable(2);
        try {
            details.setWidths(new float[]{35, 65});
        } catch (Exception ignored) {}

        addDetailRow(details, "naava :", supplier != null ? supplier.getName() : "-");
        if (supplier != null && supplier.getContact() != null && !supplier.getContact().isEmpty()) {
            addDetailRow(details, "saMpak- :", supplier.getContact());
        }
        leftCell.addElement(details);
        twoCol.addCell(leftCell);

        // RIGHT: Total Payment Amount
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(8f);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph amtTitle = new Paragraph("ekUNa Barlaolao", fontTitle);
        amtTitle.setSpacingAfter(5f);
        rightCell.addElement(amtTitle);

        Paragraph amtPara = new Paragraph();
        amtPara.add(new Chunk("Rs. " + String.format("%,.2f", totalPaid), englishFontLarge));
        rightCell.addElement(amtPara);
        twoCol.addCell(rightCell);

        PdfPCell wrapper = new PdfPCell(twoCol);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        return wrapper;
    }

    private PdfPCell createBillPaymentsTableCell(List<BillPayment> payments) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(8f);

        // Title
        Paragraph title = new Paragraph("Barlaolao ibala tpSaIla", fontTitle);
        title.setSpacingAfter(5f);
        wrapper.addElement(title);

        // Table header: Bill No | Bill Date | Bill Amount | Paid Amount | Status
        PdfPTable billTable = new PdfPTable(5);
        try {
            billTable.setWidthPercentage(100);
            billTable.setWidths(new float[]{15, 20, 22, 22, 21});
        } catch (Exception ignored) {}

        // Header row
        addTableHeaderCell(billTable, "ibala k`.");
        addTableHeaderCell(billTable, "idnaaMk");
        addTableHeaderCell(billTable, "ibala r@kma");
        addTableHeaderCell(billTable, "Barlaolao");
        addTableHeaderCellEnglish(billTable, "STATUS");

        // Data rows
        for (BillPayment payment : payments) {
            PurchaseBill bill = getBillDetails(payment.getBillNo());

            // Bill No
            addTableDataCell(billTable, "#" + payment.getBillNo(), false);

            // Bill Date
            String billDate = bill != null && bill.getBillDate() != null ?
                    bill.getBillDate().format(DATE_FMT) : "-";
            addTableDataCell(billTable, billDate, false);

            // Bill Amount
            double billAmt = bill != null && bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
            addTableDataCell(billTable, "Rs. " + String.format("%,.2f", billAmt), false);

            // Paid Amount
            double paidAmt = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
            addTableDataCell(billTable, "Rs. " + String.format("%,.2f", paidAmt), true);

            // Status - use English font
            String status = bill != null ? bill.getStatus() : "-";
            addTableDataCellEnglish(billTable, status);
        }

        wrapper.addElement(billTable);
        return wrapper;
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontBold));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(new BaseColor(230, 230, 230));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableHeaderCellEnglish(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, englishFontBold));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(new BaseColor(230, 230, 230));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableDataCell(PdfPTable table, String text, boolean highlight) {
        Font font = text.startsWith("Rs.") || text.startsWith("#") ||
                text.matches(".*\\d.*") ? englishFont : fontValue;
        if (highlight && text.startsWith("Rs.")) {
            font = englishFontBold;
        }
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (highlight) {
            cell.setBackgroundColor(new BaseColor(245, 255, 245));
        }
        table.addCell(cell);
    }

    private void addTableDataCellEnglish(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, englishFontBold));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell createMultiBillSummaryCell(List<BillPayment> payments, Supplier supplier, double totalPaid) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(8f);

        // Summary table - 4 columns
        PdfPTable summaryTable = new PdfPTable(4);
        try {
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{25, 25, 25, 25});
        } catch (Exception ignored) {}

        // Bills Paid | Total Paid | Supplier Total Pending
        addSummaryCell(summaryTable, "Barlaolao ibala", String.valueOf(payments.size()), false);
        addSummaryCell(summaryTable, "ekUNa Barlaolao", "Rs. " + String.format("%,.2f", totalPaid), false);

        Double totalPending = getTotalPendingForSupplier(supplier != null ? supplier.getId() : null);
        addSummaryCell(summaryTable, "paurvazadar baakI", "Rs. " + String.format("%,.2f", totalPending), true);
        addEmptyCell(summaryTable, 1);

        wrapper.addElement(summaryTable);
        return wrapper;
    }

    private PdfPCell createHeaderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8f);

        // Company name centered - Marathi 20px
        Paragraph header = new Paragraph();
        header.setAlignment(Element.ALIGN_CENTER);
        header.add(new Chunk("AMjanaI k^fo", fontHeader));
        
        cell.addElement(header);
        header.add(new Chunk(" - f^imalaI rosTa^rMT", new Font(customBaseFont, 20f, Font.NORMAL, GRAY)));
        cell.addElement(header);

        // Receipt title - English 14px, Marathi 20px
        Paragraph title = new Paragraph();
        title.setAlignment(Element.ALIGN_CENTER);
       // title.add(new Chunk("PAYMENT RECEIPT", fontSmall));
        title.add(new Chunk("paOsao BarlyacaI paavataI", fontTitle));
        title.setSpacingBefore(3f);
        cell.addElement(title);

        return cell;
    }

    private PdfPCell createReceiptInfoCell(BillPayment payment) {
        PdfPTable infoTable = new PdfPTable(4);
        try {
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{22, 28, 22, 28});
        } catch (Exception ignored) {}

        // Receipt No
        addLabelValuePair(infoTable, "pavataI k`. :", "#" + payment.getId());
        // Date
        String date = payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        addLabelValuePair(infoTable, "idnaaMk :", date);

        PdfPCell wrapper = new PdfPCell(infoTable);
        wrapper.setBorder(Rectangle.BOTTOM);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(5f);
        return wrapper;
    }

    private void addLabelValuePair(PdfPTable table, String label, String value) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, fontLabel));
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lbl.setPaddingRight(3f);
        table.addCell(lbl);

        // Use English font for numbers, dates, symbols
        PdfPCell val = new PdfPCell(new Phrase(value, englishFontBold));
        val.setBorder(Rectangle.NO_BORDER);
        val.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(val);
    }

    private PdfPCell createTwoColumnSection(BillPayment payment, Supplier supplier) {
        PdfPTable twoCol = new PdfPTable(2);
        try {
            twoCol.setWidthPercentage(100);
            twoCol.setWidths(new float[]{50, 50});
        } catch (Exception ignored) {}

        // LEFT: Supplier Info
        twoCol.addCell(createSupplierInfoCell(supplier));

        // RIGHT: Payment Details
        twoCol.addCell(createPaymentDetailsCell(payment));

        PdfPCell wrapper = new PdfPCell(twoCol);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        return wrapper;
    }

    private PdfPCell createSupplierInfoCell(Supplier supplier) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.RIGHT);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(8f);

        Paragraph title = new Paragraph("paurvazadaracaO maaihtaI", fontTitle);
        title.setSpacingAfter(5f);
        cell.addElement(title);

        PdfPTable details = new PdfPTable(2);
        try {
            details.setWidths(new float[]{35, 65});
        } catch (Exception ignored) {}

        addDetailRow(details, "naava :", supplier != null ? supplier.getName() : "-");
        if (supplier != null && supplier.getContact() != null && !supplier.getContact().isEmpty()) {
            addDetailRow(details, "saMpak- :", supplier.getContact());
        }
        if (supplier != null && supplier.getAddress() != null && !supplier.getAddress().isEmpty()) {
            String addr = supplier.getAddress();
            if (addr.length() > 30) addr = addr.substring(0, 30) + "...";
            addDetailRow(details, "pattaa :", addr);
        }

        cell.addElement(details);
        return cell;
    }

    private PdfPCell createPaymentDetailsCell(BillPayment payment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8f);

        Paragraph title = new Paragraph("Barlayaacao tpSaIla", fontTitle);
        title.setSpacingAfter(5f);
        cell.addElement(title);

        PdfPTable details = new PdfPTable(2);
        try {
            details.setWidths(new float[]{40, 60});
        } catch (Exception ignored) {}

        addDetailRow(details, "ibala k`. :", "" + payment.getBillNo());
        addDetailRow(details, "pa`kar :", payment.getPaymentMode() != null ? payment.getPaymentMode() : "-");

        if (payment.getChequeNo() != null && !payment.getChequeNo().trim().isEmpty()) {
            addDetailRow(details, "caok k`. :", payment.getChequeNo());
        }
        if (payment.getReferenceNo() != null && !payment.getReferenceNo().trim().isEmpty()) {
            addDetailRow(details, "saMdBa- k`. :", payment.getReferenceNo());
        }

        cell.addElement(details);

        // Payment Amount - Highlighted with English font for amount
        Double amt = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
        Paragraph amtPara = new Paragraph();
        amtPara.add(new Chunk("Barlaolao : ", fontLabel));
        amtPara.add(new Chunk("Rs. " + String.format("%,.2f", amt), englishFontLarge));
        amtPara.setSpacingBefore(5f);
        cell.addElement(amtPara);

        return cell;
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, fontLabel));
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setPadding(2f);
        table.addCell(lbl);

        PdfPCell val = new PdfPCell(new Phrase(value, fontValue));
        val.setBorder(Rectangle.NO_BORDER);
        val.setPadding(2f);
        table.addCell(val);
    }

    private PdfPCell createBillSummaryCell(BillPayment payment, PurchaseBill bill, Supplier supplier) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.TOP);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(8f);

        // Summary table - 4 columns for compact display
        PdfPTable summaryTable = new PdfPTable(4);
        try {
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{25, 25, 25, 25});
        } catch (Exception ignored) {}

        if (bill != null) {
            Double netAmt = bill.getNetAmount() != null ? bill.getNetAmount() : 0.0;
            Double paidAmt = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
            Double balance = bill.getBalanceAmount();

            // Row 1: Bill Amount | Total Paid
            addSummaryCell(summaryTable, "ibala r@kma", "Rs. " + String.format("%,.2f", netAmt), false);
            addSummaryCell(summaryTable, "ekuNa Barlaolao", "Rs. " + String.format("%,.2f", paidAmt), false);

            // Row 2: Balance | Supplier Total Pending
            addSummaryCell(summaryTable, "baakI r@kma", "Rs. " + String.format("%,.2f", balance), true);

            Double totalPending = getTotalPendingForSupplier(supplier != null ? supplier.getId() : null);
            addSummaryCell(summaryTable, "ekUNa baakI (paurvazadar)", "Rs. " + String.format("%,.2f", totalPending), true);

        } else {
            Double amt = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
            addSummaryCell(summaryTable, "Barlaolao rkkma", "Rs. " + String.format("%,.2f", amt), false);
            addEmptyCell(summaryTable, 3);
        }

        wrapper.addElement(summaryTable);
        return wrapper;
    }

    private void addSummaryCell(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontLabel));
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(4f);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (highlight) {
            labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        }
        table.addCell(labelCell);

        // Use English font for amount values (Rs. and numbers)
        PdfPCell valueCell = new PdfPCell(new Phrase(value, highlight ? englishFontBold : englishFont));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(BORDER_COLOR);
        valueCell.setPadding(4f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (highlight) {
            valueCell.setBackgroundColor(new BaseColor(245, 245, 245));
        }
        table.addCell(valueCell);
    }

    private void addEmptyCell(PdfPTable table, int count) {
        for (int i = 0; i < count; i++) {
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);
        }
    }

    private PdfPCell createFooterCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);

        // Signature section - compact
        PdfPTable signTable = new PdfPTable(2);
        try {
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50, 50});
        } catch (Exception ignored) {}

        // Receiver - English font for symbols, Marathi 20px for text
        PdfPCell recvCell = new PdfPCell();
        recvCell.setBorder(Rectangle.NO_BORDER);
        recvCell.setPadding(5f);
        Paragraph recv = new Paragraph();
        recv.setAlignment(Element.ALIGN_CENTER);
        recv.add(new Chunk("_________________\n", englishFont));
        recv.add(new Chunk("paOsao GaoNaar", fontValue));
        recvCell.addElement(recv);
        signTable.addCell(recvCell);

        // Authorized - English font for symbols, Marathi 20px for text
        PdfPCell authCell = new PdfPCell();
        authCell.setBorder(Rectangle.NO_BORDER);
        authCell.setPadding(5f);
        Paragraph auth = new Paragraph();
        auth.setAlignment(Element.ALIGN_CENTER);
        auth.add(new Chunk("_________________\n", englishFont));
        auth.add(new Chunk("paOsao doNaar", fontValue));
        authCell.addElement(auth);
        signTable.addCell(authCell);

        cell.addElement(signTable);

        // Footer line - English 14px, Marathi 20px
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk("Generated: " + LocalDateTime.now().format(DATETIME_FMT) + " | ", englishFont));
        footer.add(new Chunk("Computer Generated Receipt - ", englishFont));
        footer.add(new Chunk("QanyavaadÑ", fontValue));
        footer.setSpacingBefore(5f);
        cell.addElement(footer);

        return cell;
    }

    private PurchaseBill getBillDetails(Integer billNo) {
        if (billNo == null) return null;
        try {
            Optional<PurchaseBill> opt = purchaseBillRepository.findById(billNo);
            return opt.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Double getTotalPendingForSupplier(Integer supplierId) {
        if (supplierId == null) return 0.0;
        try {
            Double total = purchaseBillRepository.getTotalPayableAmountBySupplier(supplierId);
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void openPdf(String pdfPath) {
        try {
            File file = new File(pdfPath);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            LOG.error("Error opening PDF: {}", e.getMessage());
        }
    }
}
