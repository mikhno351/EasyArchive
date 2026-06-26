# EasyArchive
EasyArchive is a lightweight and intuitive Java library (a wrapper over [**7-Zip JBinding**](https://github.com/borisbrodski/sevenzipjbinding)) that greatly simplifies working with archives.

## Features
* **Versatility:** Supports ZIP, 7z, TAR, RAR, GZIP and other formats.
* **Streaming processing (Stream API):** Easy navigation and filtering of archive contents using standard Java Streams.
* **Smart memory management:** Small files (up to 10 MB) are extracted directly into RAM, and large files are automatically dumped into temporary files, preventing `OutOfMemoryError`.
* **Password support:** Create and read encrypted archives "out of the box".
* **Flexible settings:** Control of the compression level and the number of streams used (for supported formats).

## Usage
### 1. Add in your project
Use gradle:
```gradle
implementation("io.github.mikhno351:easy-archive:{version}")
```
Use maven:
```xml
<dependency>
    <groupId>io.github.mikhno351</groupId>
    <artifactId>easy-archive</artifactId>
    <version>{version}</version>
</dependency>
```
Use local method (download actual version from Releases):
```xml
<dependency>
    <groupId>io.github.mikhno351</groupId>
    <artifactId>easy-archive</artifactId>
    <version>{version}</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/easy-archive-{version}.jar</systemPath>
</dependency>
```

### 2. Creating an archive
You can quickly pack an entire directory or transfer a custom list of files.

```java
import io.github.mikhno351.EasyArchive;

import net.sf.sevenzipjbinding.ArchiveFormat;

import java.io.File;

EasyArchive archive = EasyArchive.builder()
        // Archive format (ZIP by default)
        .forcedFormat(ArchiveFormat.SEVEN_ZIP)
        // Password (optional)
        .forcedPassword("mySecretPassword")
        // Compression level (optional)
        .forcedCompressionLevel(EasyArchive.CompressionLevel.ULTRA)
        // Number of streams to compress (optional)
        .forcedThreadCount(4)
        .build();

File sourceDirectory = new File("/path/to/source");
File outputFile = new File("/path/to/output.7z");
```
Creating an archive from a folder:
```java
archive.create(sourceDirectory, outputFile);
```
Creating an archive from a custom list:
```java
archive.create(List.of(
        new SourceEntry("path/inside/archive/file.txt", "/path/to/file.txt")
),outputFile);
```
Creating an archive from a list (with your filtering):
```java
archive.create(EasyArchive.SourcePreparer.prepare(sourceDirectory), outputFile);
```

### 3. Reading and extracting the archive (Complete)

```java
import io.github.mikhno351.EasyArchive;

import java.io.File;

EasyArchive archive = EasyArchive.builder()
        // Archive format (optional)
        .forcedFormat(ArchiveFormat.SEVEN_ZIP)
        // We transmit the password if the archive is encrypted
        .forcedPassword("mySecretPassword")
        .build();

File archiveFile = new File("/path/to/archive.7z");
File outputDirectory = new File("/path/to/extracted");
```
A quick way to extract the entire contents of an archive to a target folder:
```java
// Using try-with-resources is mandatory to close file descriptors
try (ArchiveContext context = archive.open(archiveFile)) {
        context.extractAll(outputDirectory);
} catch (Exception e) {
        e.printStackTrace();
}
```
Selective extraction and filtering (Stream API). **Stream API** makes it easy to find and process specific files inside an archive without unpacking it entirely:
```java
try (ArchiveContext context = archive.open(archiveFile)) {
    context.streamEntries()
            .filter(entry -> !entry.isDirectory())
            .filter(entry -> entry.path().endsWith(".txt"))
            .forEach(entry -> {
                try {
                    entry.extract(outputDirectory);
                } catch (IOException e) {
                    System.err.println("Error: " + entry.path());
                }
            });
} catch (Exception e) {
    e.printStackTrace();
}
```
Reading content directly into the InputStream. You can read the contents of any file inside the archive on the fly without saving it to disk manually:
```java
try (ArchiveContext context = archive.open(archiveFile)) {
    context.streamEntries()
            .filter(entry -> entry.path().equals("config.properties"))
            .findFirst()
            .ifPresent(entry -> {
                try (InputStream inputStream = entry.getStream()) {
                    Properties props = new Properties();
                    props.load(inputStream);
                    System.out.println("Property loaded: " + props.getProperty("app.version"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
} catch (Exception e) {
    e.printStackTrace();
}
```

## Compression levels
When configuring the builder via `.forcedCompressionLevel(...)` use constants from the `EasyArchive.CompressionLevel.*` class:
* `CompressionLevel.NONE` (0) — No compression (fast copy)
* `CompressionLevel.FASTEST` (1)
* `CompressionLevel.FAST` (3)
* `CompressionLevel.NORMAL` (5) — Base value
* `CompressionLevel.MAXIMUM` (7)
* `CompressionLevel.ULTRA` (9) — Maximum compression (requires more time and RAM)

## Resource and RAM security
* Native resources: The `ArchiveContext` context extends `AutoCloseable` and manages `RandomAccessFile` descriptors and `IInArchive` references. Always wrap the `.open()` call in `try-with-resources`.
