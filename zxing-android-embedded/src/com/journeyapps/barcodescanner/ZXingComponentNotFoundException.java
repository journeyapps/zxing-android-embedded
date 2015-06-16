package com.journeyapps.barcodescanner;

/**
 * Thrown when an ZXing component is not found on scanner layout.
 *
 * Components: "zxing_barcode_surface" as @code BarcodeView
 *             "zxing_viewfinder_view" as @code ViewfinderView
 *             "zxing_status_view" as @code TextView
 */
public class ZXingComponentNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ZXingComponentNotFoundException} that includes the current
     * stack trace.
     */
    public ZXingComponentNotFoundException() {
    }

    /**
     * Constructs a new {@code ZXingComponentNotFoundException} with the current stack
     * trace and the specified detail message.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    public ZXingComponentNotFoundException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new {@code ZXingComponentNotFoundException} with the current stack
     * trace, the specified detail message and the specified cause.
     *
     * @param message
     *            the detail message for this exception.
     * @param cause
     *            the cause of this exception.
     * @since 1.5
     */
    public ZXingComponentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ZXingComponentNotFoundException} with the current stack
     * trace and the specified cause.
     *
     * @param cause
     *            the cause of this exception, may be {@code null}.
     * @since 1.5
     */
    public ZXingComponentNotFoundException(Throwable cause) {
        super((cause == null ? null : cause.toString()), cause);
    }

}
