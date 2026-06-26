package io.github.mikhno351.internal;

import io.github.mikhno351.model.SourceEntry;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;

/**
 * Callback implementation for 7-Zip JBinding used to provide metadata
 * and data streams when creating an archive.
 */
public class ArchiveCreateCallback implements IOutCreateCallback<IOutItemAllFormats> {

    private final List<SourceEntry> sourceEntries;

    /**
     * Creates a new callback instance with the specified list of entries to archive.
     *
     * @param sourceEntries the list of files and folders to be packed.
     */
    public ArchiveCreateCallback(List<SourceEntry> sourceEntries) {
        this.sourceEntries = sourceEntries;
    }

    /**
     * Handles the final operation result status. (Currently a no-op).
     */
    @Override
    public void setOperationResult(boolean operationResultOk) {
    }

    /**
     * Configures metadata (path, size, modification time) for an archive item at the given index.
     *
     * @param index          the index of the current item.
     * @param outItemFactory the factory used to create the metadata object.
     * @return the configured archive item metadata.
     */
    @Override
    public IOutItemAllFormats getItemInformation(int index, OutItemFactory<IOutItemAllFormats> outItemFactory) {
        SourceEntry sourceEntry = sourceEntries.get(index);
        IOutItemAllFormats outItemAllFormats = outItemFactory.createOutItem();

        if (sourceEntry.file().isDirectory()) {
            outItemAllFormats.setPropertyIsDir(true);
        } else {
            outItemAllFormats.setDataSize(sourceEntry.file().length());
        }

        outItemAllFormats.setPropertyLastModificationTime(new Date(sourceEntry.file().lastModified()));
        outItemAllFormats.setPropertyPath(sourceEntry.archivePath().replace('\\', '/'));

        return outItemAllFormats;
    }

    /**
     * Opens and returns a readable sequential stream for the file data at the given index.
     *
     * @param index the index of the current item.
     * @return the input stream for the file, or {@code null} if the item is a directory.
     * @throws SevenZipException if the file cannot be found or opened.
     */
    @Override
    public ISequentialInStream getStream(int index) throws SevenZipException {
        SourceEntry sourceEntry = sourceEntries.get(index);

        if (sourceEntry.file().isDirectory()) {
            return null;
        }

        try {
            return new RandomAccessFileInStream(new RandomAccessFile(sourceEntry.file(), "r"));
        } catch (FileNotFoundException e) {
            throw new SevenZipException("Couldn't open the file for reading: " + sourceEntry.file().getAbsolutePath(), e);
        }
    }

    /**
     * Receives the total number of bytes or items to be processed. (Currently a no-op).
     */
    @Override
    public void setTotal(long total) {
    }

    /**
     * Receives progress updates on the number of bytes or items completed. (Currently a no-op).
     */
    @Override
    public void setCompleted(long complete) {
    }
}
