package io.github.mikhno351.model;

import io.github.mikhno351.EasyArchive;
import io.github.mikhno351.interfaces.functional.InputStreamProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Represents an entry extracted or read from an existing archive.
 *
 * @param index               the index of the item inside the archive.
 * @param path                the relative path of the item.
 * @param comment             the comment associated with the item, if any.
 * @param creationDate        the date and time when the item was created.
 * @param lastAccessDate      the date and time when the item was last accessed.
 * @param lastModifiedDate    the date and time when the item was last modified.
 * @param size                the uncompressed size of the file in bytes.
 * @param packedSize          the compressed size of the file in bytes.
 * @param isDirectory         true if this entry represents a directory, false otherwise.
 * @param isEncrypted         true if the entry is password-protected.
 * @param inputStreamProvider the provider used to open an input stream for the file content.
 */
public record ArchiveEntry(int index, String path, String comment, LocalDateTime creationDate, LocalDateTime lastAccessDate, LocalDateTime lastModifiedDate, Long size, Long packedSize, boolean isDirectory, boolean isEncrypted, InputStreamProvider inputStreamProvider) {

    /**
     * Opens an input stream to read the content of this archive entry.
     *
     * @return the {@link InputStream} for the file content, or {@code null} if it is a directory.
     * @throws IOException if an I/O error occurs while opening the stream.
     */
    public InputStream getStream() throws IOException {
        if (isDirectory || inputStreamProvider == null) {
            return null;
        }
        return inputStreamProvider.getStream();
    }

    /**
     * Extracts this specific entry to the specified target directory.
     *
     * @param outputDirectory the target directory where the file or folder should be extracted.
     * @throws IOException if an I/O error occurs during extraction.
     */
    public void extract(File outputDirectory) throws IOException {
        EasyArchive.Extractor.extract(this, outputDirectory);
    }
}