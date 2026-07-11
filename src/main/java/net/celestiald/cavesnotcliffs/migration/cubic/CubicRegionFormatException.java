package net.celestiald.cavesnotcliffs.migration.cubic;

import java.io.IOException;

/**
 * Signals that a legacy CubicChunks region cannot be read without guessing.
 */
public final class CubicRegionFormatException extends IOException {
    public CubicRegionFormatException(String message) {
        super(message);
    }

    public CubicRegionFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
