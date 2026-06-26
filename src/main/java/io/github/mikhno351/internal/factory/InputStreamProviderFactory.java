package io.github.mikhno351.internal.factory;

import io.github.mikhno351.interfaces.functional.InputStreamProvider;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import java.io.*;
import java.nio.file.Files;

/**
 * Factory class that creates {@link InputStreamProvider} instances for archive items.
 * Decides whether to extract content into memory or onto the disk based on file size.
 */
public final class InputStreamProviderFactory {

    /**
     * The maximum file size (10 MB) allowed to be extracted entirely into memory.
     */
    private static final long MEMORY_THRESHOLD = 10485760;

    private InputStreamProviderFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates an {@link InputStreamProvider} for the given archive item.
     * Small files are routed to memory, while larger ones use a temporary file.
     *
     * @param simpleInArchiveItem the archive item to extract.
     * @param forcedPassword      the password for decryption, or {@code null} if none.
     * @return the provider to get the item's stream, or {@code null} if it is a folder.
     * @throws SevenZipException if an error occurs within the 7-Zip engine.
     */
    public static InputStreamProvider create(ISimpleInArchiveItem simpleInArchiveItem, String forcedPassword) throws SevenZipException {
        if (simpleInArchiveItem.isFolder()) {
            return null;
        }

        long fileSize = simpleInArchiveItem.getSize();
        return () -> fileSize <= MEMORY_THRESHOLD ? readToMemory(simpleInArchiveItem, fileSize, forcedPassword) : readToTempFile(simpleInArchiveItem, forcedPassword);
    }

    /**
     * Extracts the archive item directly into a byte array in memory.
     */
    private static InputStream readToMemory(ISimpleInArchiveItem item, long fileSize, String forcedPassword) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(fileSize > 0 ? (int) fileSize : 32);
            item.extractSlow(data -> {
                try {
                    byteArrayOutputStream.write(data);
                    return data.length;
                } catch (IOException e) {
                    return 0;
                }
            }, forcedPassword);
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            throw new IOException("Failed to extract file to memory: " + item.getPath(), e);
        }
    }

    /**
     * Extracts the archive item to a temporary file that deletes itself upon stream closure.
     */
    private static InputStream readToTempFile(ISimpleInArchiveItem item, String forcedPassword) throws IOException {
        File tempFile = File.createTempFile("easyarchive_", ".tmp");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            item.extractSlow(data -> {
                try {
                    fileOutputStream.write(data);
                    return data.length;
                } catch (IOException e) {
                    return 0;
                }
            }, forcedPassword);
        } catch (Exception e) {
            Files.deleteIfExists(tempFile.toPath());
            throw new IOException("Failed to extract file to temp storage: " + item.getPath(), e);
        }

        return new FileInputStream(tempFile) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(tempFile.toPath());
                }
            }
        };
    }
}