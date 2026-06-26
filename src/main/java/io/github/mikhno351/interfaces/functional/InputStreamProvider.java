package io.github.mikhno351.interfaces.functional;

import java.io.IOException;
import java.io.InputStream;

/**
 * A functional interface designed to lazily provide or open an {@link InputStream}.
 */
@FunctionalInterface
public interface InputStreamProvider {

    /**
     * Opens or creates a new input stream for reading data.
     *
     * @return the opened {@link InputStream}.
     * @throws IOException if an I/O error occurs while preparing the stream.
     */
    InputStream getStream() throws IOException;
}
