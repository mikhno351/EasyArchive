package io.github.mikhno351.interfaces;

import io.github.mikhno351.EasyArchive;
import io.github.mikhno351.model.ArchiveEntry;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Represents an open archive session.
 * <p>
 * This context is {@link AutoCloseable} and must be closed (typically using a
 * try-with-resources block) to free native resources and close file handles.
 */
public interface ArchiveContext extends AutoCloseable {

    /**
     * Returns a sequential {@link Stream} of all entries inside the archive.
     *
     * @return a stream of {@link ArchiveEntry} elements.
     * @throws IOException if an I/O error occurs while reading the archive.
     */
    Stream<ArchiveEntry> streamEntries() throws IOException;

    /**
     * Extracts all entries from the archive into the specified target directory.
     *
     * @param outputDirectory the target directory where files should be extracted.
     * @throws IOException if an I/O error occurs during extraction.
     */
    default void extractAll(File outputDirectory) throws IOException {
        EasyArchive.Extractor.extract(this.streamEntries()::iterator, outputDirectory);
    }
}