package io.github.mikhno351;

import io.github.mikhno351.interfaces.ArchiveContext;
import io.github.mikhno351.internal.ArchiveCreateCallback;
import io.github.mikhno351.internal.EncryptedArchiveCreateCallback;
import io.github.mikhno351.internal.factory.InputStreamProviderFactory;
import io.github.mikhno351.model.ArchiveEntry;

import io.github.mikhno351.model.SourceEntry;
import lombok.Builder;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main entry point for the EasyArchive library. Provides a simple API
 * to open, read, and create compressed archives using 7-Zip JBinding.
 */
@Builder
public final class EasyArchive {

    private static volatile boolean isEngineInitialized = false;

    private final ArchiveFormat forcedFormat;
    private final String forcedPassword;

    private final Integer forcedCompressionLevel;
    private final Integer forcedThreadCount;

    /**
     * Opens an existing archive file for reading and extracting contents.
     *
     * @param file the archive file to open.
     * @return an {@link ArchiveContext} instance representing the open session.
     * @throws IOException if the file does not exist, cannot be read, or is corrupted.
     */
    public ArchiveContext open(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Expected a file, but found a directory: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IOException("File cannot be read: " + file.getAbsolutePath());
        }

        ensureEngineInitialized();

        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        final IInArchive inArchive;

        try {
            inArchive = SevenZip.openInArchive(forcedFormat, new RandomAccessFileInStream(randomAccessFile), forcedPassword);
        } catch (Exception e) {
            randomAccessFile.close();
            throw new IOException("Failed to open archive: " + file.getName() + ". Unsupported format or corrupted file", e);
        }

        return new ArchiveContext() {

            @Override
            public Stream<ArchiveEntry> streamEntries() throws IOException {
                try {
                    return Arrays.stream(inArchive.getSimpleInterface().getArchiveItems()).map(item -> {
                        try {
                            return new ArchiveEntry(item.getItemIndex(), item.getPath(), item.getComment(), toLocalDateTime(item.getCreationTime()), toLocalDateTime(item.getLastAccessTime()), toLocalDateTime(item.getLastWriteTime()), item.getSize(), item.getPackedSize(), item.isFolder(), item.isEncrypted(), InputStreamProviderFactory.create(item, forcedPassword));
                        } catch (Exception e) {
                            throw new RuntimeException("Error reading properties for archive item: " + e.getMessage(), e);
                        }
                    });
                } catch (Exception e) {
                    throw new IOException("Error accessing archive items", e);
                }
            }

            @Override
            public void close() throws Exception {
                try {
                    inArchive.close();
                } finally {
                    randomAccessFile.close();
                }
            }
        };
    }

    /**
     * Creates an archive containing all files from the specified source directory.
     *
     * @param sourceDirectory the directory containing files to pack.
     * @param outputFile      the target archive file to be created.
     * @throws IOException if an I/O error occurs during compression.
     */
    public void create(File sourceDirectory, File outputFile) throws IOException {
        create(SourcePreparer.prepare(sourceDirectory), outputFile);
    }

    /**
     * Creates an archive from a predefined list of source entries.
     *
     * @param sourceEntries the list of files and folders to pack.
     * @param outputFile    the target archive file to be created.
     * @throws IOException if an I/O error occurs during compression.
     */
    public void create(List<SourceEntry> sourceEntries, File outputFile) throws IOException {
        ensureEngineInitialized();

        ArchiveFormat runtimeFormat = this.forcedFormat != null ? this.forcedFormat : ArchiveFormat.ZIP;

        File parentDir = outputFile.getParentFile();

        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directories for the output archive: " + parentDir.getAbsolutePath());
        }
        if (outputFile.exists()) {
            throw new IOException("File already exists: " + outputFile.getAbsolutePath());
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw"); IOutCreateArchive<IOutItemAllFormats> outCreateArchive = SevenZip.openOutArchive(runtimeFormat)) {
            RandomAccessFileOutStream outStream = new RandomAccessFileOutStream(randomAccessFile);

            if (outCreateArchive instanceof IOutFeatureSetMultithreading && forcedThreadCount != null) {
                ((IOutFeatureSetMultithreading) outCreateArchive).setThreadCount(forcedThreadCount);
            }
            if (outCreateArchive instanceof IOutFeatureSetLevel && forcedCompressionLevel != null) {
                ((IOutFeatureSetLevel) outCreateArchive).setLevel(forcedCompressionLevel);
            }

            boolean hasPassword = (forcedPassword != null && !forcedPassword.isEmpty());
            if (outCreateArchive instanceof IOutFeatureSetEncryptHeader && hasPassword) {
                ((IOutFeatureSetEncryptHeader) outCreateArchive).setHeaderEncryption(true);
            }

            outCreateArchive.createArchive(outStream, sourceEntries.size(), hasPassword ? new EncryptedArchiveCreateCallback(sourceEntries, forcedPassword) : new ArchiveCreateCallback(sourceEntries));
        } catch (Exception e) {
            if (outputFile.exists()) {
                Files.deleteIfExists(outputFile.toPath());
            }
            throw new IOException("Failed to create archive: " + e.getMessage(), e);
        }
    }

    /**
     * Lazily initializes the native 7-Zip engine from the platform JAR.
     */
    private static void ensureEngineInitialized() throws IOException {
        if (!isEngineInitialized) {
            synchronized (EasyArchive.class) {
                if (!isEngineInitialized) {
                    try {
                        SevenZip.initSevenZipFromPlatformJAR();
                        isEngineInitialized = true;
                    } catch (Exception e) {
                        throw new IOException("Failed to initialize SevenZip native engine from platform JAR", e);
                    }
                }
            }
        }
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Standard compression level constants used by the archive creator.
     */
    public static final class CompressionLevel {

        public static final int NONE = 0;
        public static final int FASTEST = 1;
        public static final int FAST = 3;
        public static final int NORMAL = 5;
        public static final int MAXIMUM = 7;
        public static final int ULTRA = 9;

        private CompressionLevel() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    /**
     * Utility class responsible for extracting archive entries to the file system.
     */
    public static final class Extractor {

        private Extractor() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        /**
         * Extracts a single archive entry to the target directory.
         *
         * @param archiveEntry    the entry to extract.
         * @param outputDirectory the target directory.
         * @throws IOException if an I/O error occurs.
         */
        public static void extract(ArchiveEntry archiveEntry, File outputDirectory) throws IOException {
            extract(Collections.singletonList(archiveEntry), outputDirectory);
        }

        /**
         * Extracts a collection of archive entries to the target directory.
         *
         * @param archiveEntries  the entries to extract.
         * @param outputDirectory the target directory.
         * @throws IOException if an I/O error occurs.
         */
        public static void extract(Iterable<ArchiveEntry> archiveEntries, File outputDirectory) throws IOException {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new IOException("Failed to create target directory: " + outputDirectory.getAbsolutePath());
            }

            byte[] buffer = new byte[8192];

            for (ArchiveEntry archiveEntry : archiveEntries) {
                File outputFile = new File(outputDirectory, archiveEntry.path());

                if (archiveEntry.isDirectory()) {
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + outputFile.getAbsolutePath());
                    }
                    continue;
                }

                File outputParentFile = outputFile.getParentFile();
                if (outputParentFile != null && !outputParentFile.exists() && !outputParentFile.mkdirs()) {
                    throw new IOException("Failed to create directory for file: " + outputParentFile.getAbsolutePath());
                }

                try (InputStream inputStream = archiveEntry.getStream();
                     FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {

                    if (inputStream != null) {
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility class used to scan folders and convert files into a list of packable {@link SourceEntry} items.
     */
    public static final class SourcePreparer {

        private SourcePreparer() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        /**
         * Recursively scans a directory and maps all its contents into relative source entries.
         *
         * @param sourceDirectory the directory to scan.
         * @return a list of prepared {@link SourceEntry} objects.
         * @throws IOException if an error occurs while walking the file tree.
         */
        public static List<SourceEntry> prepare(File sourceDirectory) throws IOException {
            if (sourceDirectory == null) {
                throw new IllegalArgumentException("Directory must not be NULL");
            }
            if (!sourceDirectory.exists()) {
                throw new IllegalArgumentException("Directory does not exist: " + sourceDirectory.getAbsolutePath());
            }
            if (!sourceDirectory.isDirectory()) {
                throw new IllegalArgumentException("The specified object is not a directory: " + sourceDirectory.getAbsolutePath());
            }

            Path rootPath = sourceDirectory.toPath();

            try (Stream<Path> stream = Files.walk(rootPath)) {
                return stream.filter(path -> !path.equals(rootPath)).map(path -> new SourceEntry(rootPath.relativize(path).toString().replace('\\', '/'), path.toFile())).collect(Collectors.toList());
            }
        }
    }
}