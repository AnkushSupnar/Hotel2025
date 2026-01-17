package com.frontend.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR codes for UPI payments.
 */
public class QRCodeGenerator {

    private static final int DEFAULT_QR_SIZE = 150;

    /**
     * Generates a UPI QR code image as a byte array.
     *
     * @param upiId     The UPI ID (e.g., "merchant@upi")
     * @param payeeName The name of the payee/merchant
     * @param amount    The payment amount
     * @return byte array containing PNG image data
     * @throws WriterException if QR code generation fails
     * @throws IOException     if image conversion fails
     */
    public static byte[] generateUPIQRCode(String upiId, String payeeName, double amount)
            throws WriterException, IOException {
        return generateUPIQRCode(upiId, payeeName, amount, DEFAULT_QR_SIZE);
    }

    /**
     * Generates a UPI QR code image as a byte array with custom size.
     *
     * @param upiId     The UPI ID (e.g., "merchant@upi")
     * @param payeeName The name of the payee/merchant
     * @param amount    The payment amount
     * @param size      The size of the QR code in pixels
     * @return byte array containing PNG image data
     * @throws WriterException if QR code generation fails
     * @throws IOException     if image conversion fails
     */
    public static byte[] generateUPIQRCode(String upiId, String payeeName, double amount, int size)
            throws WriterException, IOException {

        String upiString = buildUPIString(upiId, payeeName, amount);
        return generateQRCode(upiString, size);
    }

    /**
     * Builds a UPI payment string.
     *
     * @param upiId     The UPI ID
     * @param payeeName The payee name
     * @param amount    The payment amount
     * @return UPI payment string
     */
    public static String buildUPIString(String upiId, String payeeName, double amount) {
        StringBuilder upiBuilder = new StringBuilder("upi://pay?");

        // UPI ID (pa = payee address)
        upiBuilder.append("pa=").append(encodeURIComponent(upiId));

        // Payee Name (pn)
        if (payeeName != null && !payeeName.trim().isEmpty()) {
            upiBuilder.append("&pn=").append(encodeURIComponent(payeeName));
        }

        // Amount (am)
        if (amount > 0) {
            upiBuilder.append("&am=").append(String.format("%.2f", amount));
        }

        // Currency (cu)
        upiBuilder.append("&cu=INR");

        return upiBuilder.toString();
    }

    /**
     * Generates a QR code image from the given content.
     *
     * @param content The content to encode in the QR code
     * @param size    The size of the QR code in pixels
     * @return byte array containing PNG image data
     * @throws WriterException if QR code generation fails
     * @throws IOException     if image conversion fails
     */
    public static byte[] generateQRCode(String content, int size) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }

    /**
     * URL encodes a string for use in UPI payment string.
     */
    private static String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
