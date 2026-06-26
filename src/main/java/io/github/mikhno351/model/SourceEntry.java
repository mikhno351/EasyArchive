package io.github.mikhno351.model;

import java.io.File;

/**
 * Represents a file or folder that is prepared to be added to an archive.
 *
 * @param archivePath the relative path inside the archive (e.g., "folder/file.txt").
 * @param file        the actual physical file on the disk.
 */
public record SourceEntry(String archivePath, File file) {
}