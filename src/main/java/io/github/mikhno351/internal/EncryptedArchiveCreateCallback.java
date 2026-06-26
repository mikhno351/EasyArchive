package io.github.mikhno351.internal;

import io.github.mikhno351.model.SourceEntry;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;

import java.util.List;

/**
 * An extension of {@link ArchiveCreateCallback} that supports password-protected
 * archive creation by providing encryption keys to the 7-Zip engine.
 */
public class EncryptedArchiveCreateCallback extends ArchiveCreateCallback implements ICryptoGetTextPassword {

    private final String forcedPassword;

    /**
     * Creates a new encrypted callback instance with entries and a password.
     *
     * @param sourceEntries  the list of files and folders to be packed.
     * @param forcedPassword the password to encrypt the archive with.
     */
    public EncryptedArchiveCreateCallback(List<SourceEntry> sourceEntries, String forcedPassword) {
        super(sourceEntries);
        this.forcedPassword = forcedPassword;
    }

    /**
     * Retrieves the password used for encrypting the archive items.
     *
     * @return the encryption password string.
     */
    @Override
    public String cryptoGetTextPassword() {
        return forcedPassword;
    }
}
