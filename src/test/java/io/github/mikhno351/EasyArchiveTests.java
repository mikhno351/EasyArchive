package io.github.mikhno351;

import io.github.mikhno351.interfaces.ArchiveContext;

import java.io.File;

import net.sf.sevenzipjbinding.ArchiveFormat;
import org.junit.jupiter.api.Test;

public class EasyArchiveTests {

    @Test
    public void zipOpenArchive() throws Exception {
        EasyArchive archive = EasyArchive.builder().build();

        try (ArchiveContext context = archive.open(new File("C:/Users/Alex/Downloads/Telegram Desktop/GM_V48_00_EN.zip"))) {
            System.out.println(context.streamEntries().toList());
        }
    }

    @Test
    public void zipExtractArchive() throws Exception {
        EasyArchive archive = EasyArchive.builder().build();

        try (ArchiveContext context = archive.open(new File("C:/Users/Alex/Downloads/Telegram Desktop/GM_V48_00_EN.zip"))) {
            context.extractAll(new File("C:/Users/Alex/Downloads/Telegram Desktop/GM_V48_00_EN"));
        }
    }

    @Test
    public void zipCreateArchive() throws Exception {
        EasyArchive archive = EasyArchive.builder().forcedFormat(ArchiveFormat.ZIP).build();

        archive.create(new File("C:/Users/Alex/Downloads/Telegram Desktop/GM_V48_00_EN"), new File("C:/Users/Alex/Downloads/Output/archive-" + System.currentTimeMillis() + ".zip"));
    }
}