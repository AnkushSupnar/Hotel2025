package com.frontend.print;

import com.frontend.entity.Bill;
import com.frontend.entity.Customer;
import com.frontend.entity.SalesBillPayment;
import com.frontend.entity.SalesPaymentReceipt;
import com.frontend.repository.BillRepository;
import com.frontend.service.SessionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Compact Professional Sales Payment Receipt Print
 * For receiving payments from customers (credit sales)
 * - Half A4 page size
 * - Table-based layout
 * - Custom font throughout
 */
@Component
public class ReceivePaymentPrint {

    private static final Logger LOG = LoggerFactory.getLogger(ReceivePaymentPrint.class);

    private static final String PDF_DIR = "D:" + File.separator + "Hotel Software";

    // Colors
    private static final BaseColor BLACK = BaseColor.BLACK;
    private static final BaseColor GRAY = new BaseColor(80, 80, 80);
    private static final BaseColor LIGHT_GRAY = new BaseColor(180, 180, 180);
    private static final BaseColor BORDER_COLOR = new BaseColor(200, 200, 200);

    @Autowired
    private BillRepository billRepository;

    // Fonts
    private BaseFont customBaseFont;
    private Font fontHeader;
    private Font fontTitle;
    private Font fontLabel;
    private Font fontValue;
    private Font fontBold;
    private Font englishFont;
    private Font englishFontBold;
    private Font englishFontLarge;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");

    /**
     * Print a SalesPaymentReceipt (grouped payment)
     * Shows single receipt with bill allocations breakdown
     */
    public boolean printPaymentReceipt(SalesPaymentReceipt receipt, Customer customer) {
        if (receipt == null) return false;
        try {
            loadFonts();
            ensureOutputDir();
            String pdfPath = generatePaymentReceiptPdf(receipt, customer);
            if (pdfPath != null) {
                openPdf(pdfPath);
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error generating sales payment receipt: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Generate PDF for SalesPaymentReceipt (Master-Detail pattern)
     * Compact single-page layout
     */
    private String generatePaymentReceiptPdf(SalesPaymentReceipt receipt, Customer customer) {
        try {
            int billsCount = receipt.getBillsCount() != null ? receipt.getBillsCount() : 1;
            float pageHeight = 380f + (billsCount > 1 ? (billsCount * 22f) : 0f);
            Rectangle pageSize = new Rectangle(595f, Math.min(pageHeight, 550f));

            Document doc = new Document(pageSize, 20f, 20f, 10f, 10f);
            String pdfPath = PDF_DIR + File.separator + "ReceivePayment.pdf";
            PdfWriter.getInstance(doc, new FileOutputStream(pdfPath));
            doc.open();

            PdfPTable mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(100);

            // === COMPACT HEADER ===
            mainTable.addCell(createCompactHeaderCell());

            // === RECEIPT INFO + CUSTOMER + PAYMENT (Combined compact row) ===
            mainTable.addCell(createCompactInfoCell(receipt, customer));

            // === BILL ALLOCATIONS TABLE (if multiple bills) ===
            if (billsCount > 1 && receipt.getBillPayments() != null && !receipt.getBillPayments().isEmpty()) {
                mainTable.addCell(createCompactBillTableCell(receipt));
            }

            // === COMPACT SUMMARY + FOOTER ===
            mainTable.addCell(createCompactSummaryFooterCell(receipt, customer));

            doc.add(mainTable);
            doc.close();

            LOG.info("Sales Payment Receipt PDF generated: ReceiptNo={}, Bills={}, Path={}",
                    receipt.getReceiptNo(), billsCount, pdfPath);
            return pdfPath;

        } catch (Exception e) {
            LOG.error("Sales Payment Receipt PDF generation error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Compact header - single line
     */
    private PdfPCell createCompactHeaderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(3f);
        cell.setPaddingBottom(5f);

        // Company name + Receipt title in one line
        Paragraph header = new Paragraph();
        header.setAlignment(Element.ALIGN_CENTER);
        header.add(new Chunk("AMjanaI k^fo", fontHeader));
        header.add(new Chunk("  |  ", new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, LIGHT_GRAY)));
        header.add(new Chunk("paOsao imaLalyacaI paavataI", fontTitle));  // Receipt for money received
        cell.addElement(header);

        return cell;
    }

    /**
     * Combined compact info cell - Receipt#, Date, Customer, Payment Mode, Amount
     */
    private PdfPCell createCompactInfoCell(SalesPaymentReceipt receipt, Customer customer) {
        PdfPTable mainInfo = new PdfPTable(3);
        try {
            mainInfo.setWidthPercentage(100);
            mainInfo.setWidths(new float[]{35, 35, 30});
        } catch (Exception ignored) {}

        // Column 1: Receipt Info
        PdfPCell col1 = new PdfPCell();
        col1.setBorder(Rectangle.RIGHT);
        col1.setBorderColor(BORDER_COLOR);
        col1.setPadding(5f);

        Paragraph receiptInfo = new Paragraph();
        receiptInfo.add(new Chunk("pavataI k`. : ", fontLabel));
        receiptInfo.add(new Chunk("#" + receipt.getReceiptNo(), englishFontBold));
        col1.addElement(receiptInfo);

        String date = receipt.getPaymentDate() != null ?
                receipt.getPaymentDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
        Paragraph dateInfo = new Paragraph();
        dateInfo.add(new Chunk("idnaaMk : ", fontLabel));
        dateInfo.add(new Chunk(date, englishFontBold));
        col1.addElement(dateInfo);

        Paragraph billsInfo = new Paragraph();
        billsInfo.add(new Chunk("ibala : ", fontLabel));
        billsInfo.add(new Chunk(getBillNumbersSummary(receipt), englishFontBold));
        col1.addElement(billsInfo);

        mainInfo.addCell(col1);

        // Column 2: Customer + Payment Mode
        PdfPCell col2 = new PdfPCell();
        col2.setBorder(Rectangle.RIGHT);
        col2.setBorderColor(BORDER_COLOR);
        col2.setPadding(5f);

        Paragraph customerInfo = new Paragraph();
        customerInfo.add(new Chunk("ga`ahk : ", fontLabel));  // Customer
        customerInfo.add(new Chunk(customer != null ? customer.getFullName() : "-", fontValue));
        col2.addElement(customerInfo);

        Paragraph modeInfo = new Paragraph();
        modeInfo.add(new Chunk("pa`kar : ", fontLabel));
        modeInfo.add(new Chunk(receipt.getPaymentMode() != null ? receipt.getPaymentMode() : "-", fontValue));
        col2.addElement(modeInfo);

        if (receipt.getChequeNo() != null && !receipt.getChequeNo().trim().isEmpty()) {
            Paragraph chequeInfo = new Paragraph();
            chequeInfo.add(new Chunk("caok k`. : ", fontLabel));
            chequeInfo.add(new Chunk(receipt.getChequeNo(), englishFontBold));
            col2.addElement(chequeInfo);
        }

        mainInfo.addCell(col2);

        // Column 3: Total Amount (Highlighted - green for receiving)
        Double totalAmt = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : 0.0;

        PdfPCell col3 = new PdfPCell();
        col3.setBorder(Rectangle.NO_BORDER);
        col3.setBackgroundColor(new BaseColor(232, 245, 233)); // Light green
        col3.setPadding(8f);
        col3.setVerticalAlignment(Element.ALIGN_MIDDLE);
        col3.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph amtLabel = new Paragraph("ekUNa imaLalao", fontLabel);  // Total Received
        amtLabel.setAlignment(Element.ALIGN_CENTER);
        col3.addElement(amtLabel);

        Paragraph amtValue = new Paragraph("Rs. " + String.format("%,.2f", totalAmt), englishFontLarge);
        amtValue.setAlignment(Element.ALIGN_CENTER);
        col3.addElement(amtValue);

        mainInfo.addCell(col3);

        PdfPCell wrapper = new PdfPCell(mainInfo);
        wrapper.setBorder(Rectangle.BOTTOM);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(0f);
        return wrapper;
    }

    /**
     * Compact bill allocations table
     */
    private PdfPCell createCompactBillTableCell(SalesPaymentReceipt receipt) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.BOTTOM);
        wrapper.setBorderColor(BORDER_COLOR);
        wrapper.setPadding(5f);

        // Table: Bill No | Bill Date | Bill Amount | Received Amount | Status
        PdfPTable billTable = new PdfPTable(5);
        try {
            billTable.setWidthPercentage(100);
            billTable.setWidths(new float[]{15, 20, 22, 22, 21});
        } catch (Exception ignored) {}

        // Compact header row
        addCompactTableHeader(billTable, "ibala k`.");
        addCompactTableHeader(billTable, "idnaaMk");
        addCompactTableHeader(billTable, "ibala r@kma");
        addCompactTableHeader(billTable, "imaLalao");  // Received
        addCompactTableHeaderEng(billTable, "STATUS");

        // Data rows
        for (SalesBillPayment bp : receipt.getBillPayments()) {
            Bill bill = getBillDetails(bp.getBillNo());

            addCompactTableData(billTable, "#" + bp.getBillNo());

            String billDate = bill != null && bill.getBillDate() != null ?
                    bill.getBillDate() : "-";
            addCompactTableData(billTable, billDate);

            float billAmt = bill != null && bill.getNetAmount() != null ? bill.getNetAmount() : 0f;
            addCompactTableData(billTable, "Rs. " + String.format("%,.2f", billAmt));

            double receivedAmt = bp.getPaymentAmount() != null ? bp.getPaymentAmount() : 0.0;
            addCompactTableDataHighlight(billTable, "Rs. " + String.format("%,.2f", receivedAmt));

            String status = bill != null ? bill.getStatus() : "-";
            addCompactTableDataEng(billTable, status);
        }

        wrapper.addElement(billTable);
        return wrapper;
    }

    private void addCompactTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(customBaseFont, 14f, Font.BOLD, BLACK)));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(new BaseColor(240, 240, 240));
        cell.setPadding(3f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCompactTableHeaderEng(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BLACK)));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(new BaseColor(240, 240, 240));
        cell.setPadding(3f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCompactTableData(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCompactTableDataHighlight(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(new BaseColor(245, 255, 245));
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCompactTableDataEng(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Compact summary + signature + footer combined
     */
    private PdfPCell createCompactSummaryFooterCell(SalesPaymentReceipt receipt, Customer customer) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(5f);

        // Summary row: Bills Count | Total Received | Customer Pending
        PdfPTable summaryTable = new PdfPTable(6);
        try {
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{18, 15, 18, 17, 17, 15});
        } catch (Exception ignored) {}

        // Labels and values inline
        addCompactSummaryLabel(summaryTable, "imaLalao ibala :");  // Received bills
        addCompactSummaryValue(summaryTable, String.valueOf(receipt.getBillsCount()));

        addCompactSummaryLabel(summaryTable, "ekUNa imaLalao :");  // Total received
        addCompactSummaryValue(summaryTable, "Rs. " + String.format("%,.2f", receipt.getTotalAmount()));

        Double totalPending = getTotalPendingForCustomer(customer != null ? customer.getId() : null);
        addCompactSummaryLabel(summaryTable, "ga`ahk baakI :");  // Customer pending
        addCompactSummaryValueHighlight(summaryTable, "Rs. " + String.format("%,.2f", totalPending));

        wrapper.addElement(summaryTable);

        // Compact signature section
        PdfPTable signTable = new PdfPTable(2);
        try {
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50, 50});
            signTable.setSpacingBefore(10f);
        } catch (Exception ignored) {}

        PdfPCell recvCell = new PdfPCell();
        recvCell.setBorder(Rectangle.NO_BORDER);
        recvCell.setPadding(2f);
        Paragraph recv = new Paragraph();
        recv.setAlignment(Element.ALIGN_CENTER);
        recv.add(new Chunk("____________\n", englishFont));
        recv.add(new Chunk("ga`ahk sahI", new Font(customBaseFont, 14f, Font.NORMAL, GRAY)));  // Customer signature
        recvCell.addElement(recv);
        signTable.addCell(recvCell);

        PdfPCell authCell = new PdfPCell();
        authCell.setBorder(Rectangle.NO_BORDER);
        authCell.setPadding(2f);
        Paragraph auth = new Paragraph();
        auth.setAlignment(Element.ALIGN_CENTER);
        auth.add(new Chunk("____________\n", englishFont));
        auth.add(new Chunk("AiQakarI", new Font(customBaseFont, 14f, Font.NORMAL, GRAY)));  // Authorized
        authCell.addElement(auth);
        signTable.addCell(authCell);

        wrapper.addElement(signTable);

        // Compact footer
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk(LocalDateTime.now().format(DATETIME_FMT) + " | Computer Generated",
                new Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, LIGHT_GRAY)));
        footer.setSpacingBefore(3f);
        wrapper.addElement(footer);

        return wrapper;
    }

    private void addCompactSummaryLabel(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(customBaseFont, 14f, Font.NORMAL, GRAY)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void addCompactSummaryValue(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BLACK)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addCompactSummaryValueHighlight(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, new BaseColor(211, 47, 47))));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    /**
     * Get summary of bill numbers paid in this receipt
     */
    private String getBillNumbersSummary(SalesPaymentReceipt receipt) {
        if (receipt.getBillPayments() == null || receipt.getBillPayments().isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SalesBillPayment bp : receipt.getBillPayments()) {
            if (count > 0) sb.append(", ");
            sb.append("#").append(bp.getBillNo());
            count++;
            if (count >= 4 && receipt.getBillPayments().size() > 4) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
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

            fontHeader = new Font(customBaseFont, 24f, Font.BOLD, BLACK);
            fontTitle = new Font(customBaseFont, 20f, Font.BOLD, BLACK);
            fontLabel = new Font(customBaseFont, 20f, Font.NORMAL, GRAY);
            fontValue = new Font(customBaseFont, 20f, Font.NORMAL, BLACK);
            fontBold = new Font(customBaseFont, 20f, Font.BOLD, BLACK);

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
            englishFont = new Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL, BLACK);
            englishFontBold = new Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BLACK);
            englishFontLarge = new Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BLACK);
        }
    }

    private Bill getBillDetails(Integer billNo) {
        if (billNo == null) return null;
        try {
            Optional<Bill> opt = billRepository.findById(billNo);
            return opt.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Double getTotalPendingForCustomer(Integer customerId) {
        if (customerId == null) return 0.0;
        try {
            Double total = billRepository.getTotalPendingAmountByCustomerId(customerId);
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
