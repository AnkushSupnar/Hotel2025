package com.frontend.print;

import com.frontend.entity.TempTransaction;
import com.frontend.service.EmployeeService;
import com.frontend.service.SessionService;
import com.frontend.service.TableMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.print.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * KOT (Kitchen Order Ticket) Print class for thermal printer
 * Prints order items to kitchen for food preparation
 */
@Component
public class KOTOrderPrint {

    private static final Logger LOG = LoggerFactory.getLogger(KOTOrderPrint.class);

    // Thermal printer paper width in cm (58mm = 5.8cm, 80mm = 8cm)
    private static final double PAPER_WIDTH_CM = 8.0;

    // Font sizes
    private static final int HOTEL_NAME_FONT_SIZE = 30;
    private static final int HEADER_FONT_SIZE = 14;
    private static final int ITEM_FONT_SIZE = 24;
    private static final int LABEL_FONT_SIZE = 24;
    private static final int ENGLISH_FONT_SIZE = 14;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TableMasterService tableMasterService;

    private Font marathiFontLarge;      // For hotel name
    private Font marathiFontMedium;     // For items
    private Font marathiFontLabel;      // For labels
    private Font englishFont;           // For values (table no, date, qty)
    private Font englishFontBold;       // For bold values
    private Font monospacedFont;        // For separators

    /**
     * Print KOT to thermal printer
     */
    public boolean printKOT(String tableName, Integer tableId, List<TempTransaction> items, Integer waitorId) {
        if (items == null || items.isEmpty()) {
            LOG.warn("No items to print for table {}", tableName);
            return false;
        }

        try {
            LOG.info("Starting KOT print for table {} with {} items", tableName, items.size());

            loadFonts();
            String waitorName = getWaitorName(waitorId);

            PrinterJob printerJob = PrinterJob.getPrinterJob();

            // Calculate dynamic height based on items
            int totalLines = calculateTotalLines(items);
            PageFormat pageFormat = getPageFormat(printerJob, totalLines);

            printerJob.setPrintable(new KOTPrintable(tableName, items, waitorName), pageFormat);
            printerJob.print();

            LOG.info("KOT printed successfully for table {}", tableName);
            return true;

        } catch (PrinterException e) {
            LOG.error("Error printing KOT for table {}: {}", tableName, e.getMessage(), e);
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
            LOG.info("Starting KOT print (with dialog) for table {} with {} items", tableName, items.size());

            loadFonts();
            String waitorName = getWaitorName(waitorId);

            PrinterJob printerJob = PrinterJob.getPrinterJob();

            int totalLines = calculateTotalLines(items);
            PageFormat pageFormat = getPageFormat(printerJob, totalLines);

            printerJob.setPrintable(new KOTPrintable(tableName, items, waitorName), pageFormat);

            if (printerJob.printDialog()) {
                printerJob.print();
                LOG.info("KOT printed successfully for table {}", tableName);
                return true;
            } else {
                LOG.info("Print cancelled by user for table {}", tableName);
                return false;
            }

        } catch (PrinterException e) {
            LOG.error("Error printing KOT for table {}: {}", tableName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Calculate total lines needed including wrapped text
     */
    private int calculateTotalLines(List<TempTransaction> items) {
        int lines = 0;
        for (TempTransaction item : items) {
            // Estimate lines needed for each item (max chars per line ~15 for Marathi)
            String itemName = item.getItemName();
            int itemLines = (int) Math.ceil(itemName.length() / 12.0);
            lines += Math.max(1, itemLines);
        }
        return lines + 8; // Add extra for header and footer
    }

    /**
     * Load custom fonts for printing
     * Uses SessionService to get the custom font family name (same as BillingController)
     */
    private void loadFonts() {
        try {
            // Get custom font family from SessionService (same as used in BillingController)
            String customFontFamily = SessionService.getCustomFontFamily();

            if (customFontFamily != null && SessionService.isCustomFontLoaded()) {
                // Create AWT fonts using the font family from SessionService
                marathiFontLarge = new Font(customFontFamily, Font.BOLD, HOTEL_NAME_FONT_SIZE);
                marathiFontMedium = new Font(customFontFamily, Font.PLAIN, ITEM_FONT_SIZE);
                marathiFontLabel = new Font(customFontFamily, Font.PLAIN, LABEL_FONT_SIZE);
                LOG.info("Custom font '{}' loaded from SessionService for printing", customFontFamily);
            } else {
                // Fallback to system font
                marathiFontLarge = new Font("SansSerif", Font.BOLD, HOTEL_NAME_FONT_SIZE);
                marathiFontMedium = new Font("SansSerif", Font.PLAIN, ITEM_FONT_SIZE);
                marathiFontLabel = new Font("SansSerif", Font.PLAIN, LABEL_FONT_SIZE);
                LOG.warn("Custom font not available from SessionService, using fallback font");
            }

            // English fonts
            englishFont = new Font("Arial", Font.PLAIN, ENGLISH_FONT_SIZE);
            englishFontBold = new Font("Arial", Font.BOLD, ENGLISH_FONT_SIZE);
            monospacedFont = new Font("Monospaced", Font.PLAIN, 10);

        } catch (Exception e) {
            LOG.error("Error loading fonts: {}", e.getMessage());
            marathiFontLarge = new Font("SansSerif", Font.BOLD, HOTEL_NAME_FONT_SIZE);
            marathiFontMedium = new Font("SansSerif", Font.PLAIN, ITEM_FONT_SIZE);
            marathiFontLabel = new Font("SansSerif", Font.PLAIN, LABEL_FONT_SIZE);
            englishFont = new Font("Arial", Font.PLAIN, ENGLISH_FONT_SIZE);
            englishFontBold = new Font("Arial", Font.BOLD, ENGLISH_FONT_SIZE);
            monospacedFont = new Font("Monospaced", Font.PLAIN, 10);
        }
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
     * Create page format for thermal printer
     */
    private PageFormat getPageFormat(PrinterJob printerJob, int totalLines) {
        PageFormat pageFormat = printerJob.defaultPage();
        Paper paper = pageFormat.getPaper();

        // Calculate height based on content
        double headerHeight = 3.5;  // cm for header
        double lineHeight = 0.55;   // cm per line (increased for larger font)
        double footerHeight = 2.0;  // cm for footer
        double totalHeight = headerHeight + (totalLines * lineHeight) + footerHeight;

        if (totalHeight < 8.0) {
            totalHeight = 8.0;
        }

        double width = convertCmToPPI(PAPER_WIDTH_CM);
        double height = convertCmToPPI(totalHeight);

        paper.setSize(width, height);
        paper.setImageableArea(
            3,
            8,
            width - 6,
            height - convertCmToPPI(0.3)
        );

        pageFormat.setOrientation(PageFormat.PORTRAIT);
        pageFormat.setPaper(paper);

        return pageFormat;
    }

    private static double convertCmToPPI(double cm) {
        return cm * 0.393700787 * 72;
    }

    /**
     * Inner class implementing Printable for KOT content
     */
    private class KOTPrintable implements Printable {

        private final String tableName;
        private final List<TempTransaction> items;
        private final String waitorName;

        public KOTPrintable(String tableName, List<TempTransaction> items, String waitorName) {
            this.tableName = tableName;
            this.items = items;
            this.waitorName = waitorName;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

            // Enable antialiasing for better text rendering
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int y = 18;
            int lineHeight = 16;
            int itemLineHeight = 18;
            double width = pageFormat.getImageableWidth();
            int pageWidth = (int) width;

            // ========== HEADER - HOTEL NAME (Large, Bold, Dark) ==========
            g2d.setFont(marathiFontLarge);
            g2d.setColor(Color.BLACK);

            String hotelName = "ha^Tola AMjanaI";  // Hotel Anjani in Marathi
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(hotelName);
            g2d.drawString(hotelName, (pageWidth - textWidth) / 2, y);
            y += 22;

            // "Order" text (centered)
            g2d.setFont(marathiFontMedium);
            String orderText = "Aa^Dr";  // Order in Marathi
            fm = g2d.getFontMetrics();
            textWidth = fm.stringWidth(orderText);
            g2d.drawString(orderText, (pageWidth - textWidth) / 2, y);
            y += 20;

            // ========== TABLE NO (Label in Marathi, Value in English) ==========
            g2d.setFont(marathiFontLabel);
            String tableLabel = "TobalanaM. : ";  // Table No. in Marathi
            g2d.drawString(tableLabel, 5, y);

            // Table number in English font (bold)
            int labelWidth = g2d.getFontMetrics().stringWidth(tableLabel);
            g2d.setFont(englishFontBold);
            g2d.drawString(tableName, 5 + labelWidth, y);
            y += lineHeight;

            // ========== DATE TIME (English font) ==========
            g2d.setFont(englishFont);
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm"));
            g2d.drawString(dateTime, 5, y);
            y += lineHeight + 4;

            // ========== SEPARATOR LINE ==========
            g2d.setFont(monospacedFont);
            g2d.drawString("================================", 0, y);
            y += lineHeight;

            // ========== TABLE HEADER (Marathi labels) ==========
            g2d.setFont(marathiFontMedium);

            // Column positions
            int srNoCol = 5;
            int itemCol = 30;
            int qtyCol = pageWidth - 35;

            g2d.drawString("k`.", srNoCol, y);        // Sr. in Marathi
            g2d.drawString("tapaSaIla", itemCol, y);    // Item in Marathi
            g2d.drawString("naga", qtyCol, y);        // Qty in Marathi
            y += lineHeight;

            // Separator
            g2d.setFont(monospacedFont);
            g2d.drawString("--------------------------------", 0, y);
            y += lineHeight;

            // ========== ITEMS TABLE ==========
            int srNo = 1;
            int itemColWidth = qtyCol - itemCol - 10;  // Available width for item name

            for (TempTransaction item : items) {
                // Serial number (English font)
                g2d.setFont(englishFontBold);
                g2d.drawString(String.valueOf(srNo++), srNoCol, y);

                // Item name (Marathi font with word wrap)
                g2d.setFont(marathiFontMedium);
                String itemName = item.getItemName();
                List<String> wrappedLines = wrapText(itemName, g2d.getFontMetrics(), itemColWidth);

                // Draw first line of item name
                if (!wrappedLines.isEmpty()) {
                    g2d.drawString(wrappedLines.get(0), itemCol, y);
                }

                // Quantity (English font) - aligned with first line
                g2d.setFont(englishFontBold);
                String qty = String.valueOf(item.getPrintQty().intValue());
                g2d.drawString(qty, qtyCol, y);

                y += itemLineHeight;

                // Draw remaining wrapped lines (if any)
                g2d.setFont(marathiFontMedium);
                for (int i = 1; i < wrappedLines.size(); i++) {
                    g2d.drawString(wrappedLines.get(i), itemCol, y);
                    y += itemLineHeight;
                }
            }

            // ========== FOOTER ==========
            y += 4;

            // Separator
            g2d.setFont(monospacedFont);
            g2d.drawString("================================", 0, y);
            y += lineHeight;

            // Waiter name (Label in Marathi, Value in English/Marathi)
            g2d.setFont(marathiFontLabel);
            String waiterLabel = "vaoTr : ";  // Waiter in Marathi
            g2d.drawString(waiterLabel, 5, y);

            labelWidth = g2d.getFontMetrics().stringWidth(waiterLabel);
            g2d.setFont(marathiFontMedium);
            g2d.drawString(waitorName, 5 + labelWidth, y);
            y += lineHeight;

            // Total items (Label in Marathi, Value in English)
            g2d.setFont(marathiFontLabel);
            String totalLabel = "ekUNa Aa[Tma : ";  // Total Items in Marathi
            g2d.drawString(totalLabel, 5, y);

            labelWidth = g2d.getFontMetrics().stringWidth(totalLabel);
            g2d.setFont(englishFontBold);
            g2d.drawString(String.valueOf(items.size()), 5 + labelWidth, y);

            return PAGE_EXISTS;
        }

        /**
         * Wrap text to fit within specified width
         */
        private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
            List<String> lines = new ArrayList<>();

            if (text == null || text.isEmpty()) {
                lines.add("");
                return lines;
            }

            // If text fits in one line, return as is
            if (fm.stringWidth(text) <= maxWidth) {
                lines.add(text);
                return lines;
            }

            // Word wrap
            StringBuilder currentLine = new StringBuilder();
            String[] words = text.split(" ");

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

                if (fm.stringWidth(testLine) <= maxWidth) {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                } else {
                    // Current line is full
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        // Word itself is too long, need to break it
                        String remaining = word;
                        while (fm.stringWidth(remaining) > maxWidth) {
                            // Find how many characters fit
                            int charsFit = 0;
                            for (int i = 1; i <= remaining.length(); i++) {
                                if (fm.stringWidth(remaining.substring(0, i)) > maxWidth) {
                                    charsFit = i - 1;
                                    break;
                                }
                                charsFit = i;
                            }
                            if (charsFit == 0) charsFit = 1; // At least one character
                            lines.add(remaining.substring(0, charsFit));
                            remaining = remaining.substring(charsFit);
                        }
                        currentLine = new StringBuilder(remaining);
                    }
                }
            }

            // Add remaining text
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            return lines;
        }
    }
}
