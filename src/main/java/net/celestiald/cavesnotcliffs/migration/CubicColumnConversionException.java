package net.celestiald.cavesnotcliffs.migration;

/** Raised when a CubicChunks column cannot be represented losslessly as one finite Anvil chunk. */
public final class CubicColumnConversionException extends Exception {
    public CubicColumnConversionException(String message) {
        super(message);
    }

    public CubicColumnConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
