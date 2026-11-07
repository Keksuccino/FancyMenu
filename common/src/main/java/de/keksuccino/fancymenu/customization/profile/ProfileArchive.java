package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Reads and writes FancyMenu profile archives (.fmp). An archive is a ZIP file with the content of a profile directory at its root,
 * including the profile's metadata file. The custom extension keeps users from opening and editing archives by accident.
 */
public final class ProfileArchive {

    public static final String FILE_EXTENSION = "fmp";
    public static final String ARCHIVE_COMMENT = "FancyMenu Profile";
    /** Upper limit for metadata files inside archives, so a broken archive can't make FancyMenu read huge files into memory. */
    private static final long MAX_METADATA_SIZE = 1024L * 1024L;
    /** Files of these types are already compressed, so they get stored without wasting time on compressing them again. */
    private static final Set<String> COMPRESSED_FILE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "apng", "webp", "fma", "afma",
            "ogg", "mp3", "flac", "opus", "m4a", "aac",
            "mp4", "webm", "mkv", "mov", "avi",
            "zip", "jar", "gz", "7z", "rar", FILE_EXTENSION, "ttf", "otf", "woff", "woff2");

    private ProfileArchive() {
    }

    public static boolean hasArchiveExtension(@NotNull Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + FILE_EXTENSION);
    }

    /**
     * Writes the content of a profile directory into an archive. The archive gets written to a temporary file first,
     * so a failed or cancelled export never leaves a broken archive at the target path.
     *
     * @param filter decides which entries of the profile directory get exported.
     */
    public static void write(@NotNull Path profileDirectory, @NotNull Predicate<Path> filter, @NotNull Path archiveFile, @NotNull ProfileTaskProgress progress) throws IOException {
        List<Path> files = new ArrayList<>();
        List<Path> directories = new ArrayList<>();
        Files.walkFileTree(profileDirectory, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                if (dir.equals(profileDirectory)) return FileVisitResult.CONTINUE;
                if (!filter.test(dir)) return FileVisitResult.SKIP_SUBTREE;
                directories.add(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && filter.test(file)) {
                    files.add(file);
                    progress.addTotalBytes(attrs.size());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        // The metadata comes first, so reading it from an archive later never needs to search the whole archive
        Path metadataFile = profileDirectory.resolve(ProfileStorage.METADATA_FILE_NAME);
        if (files.remove(metadataFile)) files.add(0, metadataFile);

        Path parent = archiveFile.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tempFile = archiveFile.resolveSibling(archiveFile.getFileName().toString() + ".part");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile), ProfileFiles.BUFFER_SIZE), StandardCharsets.UTF_8)) {
                zip.setComment(ARCHIVE_COMMENT);
                for (Path directory : directories) {
                    progress.throwIfCancelled();
                    ZipEntry entry = new ZipEntry(toEntryName(profileDirectory, directory) + "/");
                    zip.putNextEntry(entry);
                    zip.closeEntry();
                }
                byte[] buffer = new byte[ProfileFiles.BUFFER_SIZE];
                for (Path file : files) {
                    progress.throwIfCancelled();
                    ZipEntry entry = new ZipEntry(toEntryName(profileDirectory, file));
                    entry.setLastModifiedTime(Files.getLastModifiedTime(file));
                    zip.setLevel(isAlreadyCompressed(file) ? Deflater.NO_COMPRESSION : Deflater.DEFAULT_COMPRESSION);
                    zip.putNextEntry(entry);
                    try (InputStream in = Files.newInputStream(file)) {
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            progress.throwIfCancelled();
                            zip.write(buffer, 0, read);
                            progress.addProcessedBytes(read);
                        }
                    }
                    zip.closeEntry();
                }
            }
            ProfileStorage.moveReplacing(tempFile, archiveFile);
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(tempFile);
            throw ex;
        }
    }

    /**
     * Reads the metadata of an archive without extracting it.
     *
     * @throws InvalidProfileArchiveException if the file is no valid profile archive.
     */
    @NotNull
    public static ProfileMetadata readMetadata(@NotNull Path archiveFile, @NotNull String fallbackId) throws IOException {
        try (ZipFile zip = openZip(archiveFile)) {
            ZipEntry metadataEntry = zip.getEntry(ProfileStorage.METADATA_FILE_NAME);
            if ((metadataEntry == null) || metadataEntry.isDirectory()) throw new InvalidProfileArchiveException("The archive has no profile metadata: " + archiveFile);
            if (metadataEntry.getSize() > MAX_METADATA_SIZE) throw new InvalidProfileArchiveException("The metadata of the archive is too big: " + archiveFile);
            try (InputStream in = zip.getInputStream(metadataEntry)) {
                byte[] bytes = in.readNBytes((int) MAX_METADATA_SIZE + 1);
                if (bytes.length > MAX_METADATA_SIZE) throw new InvalidProfileArchiveException("The metadata of the archive is too big: " + archiveFile);
                try {
                    return ProfileMetadata.fromJson(new String(bytes, StandardCharsets.UTF_8), fallbackId);
                } catch (IllegalArgumentException ex) {
                    throw new InvalidProfileArchiveException("The metadata of the archive is broken: " + archiveFile, ex);
                }
            }
        }
    }

    /**
     * Extracts an archive into a directory.<br>
     * Entries that would end up outside the target directory get rejected, and entries for the profile store directory get skipped,
     * because profiles can't contain other profiles.
     *
     * @throws InvalidProfileArchiveException if the archive is no valid profile archive.
     * @throws NotEnoughSpaceException if the target drive has not enough free space for the extracted content.
     */
    public static void extract(@NotNull Path archiveFile, @NotNull Path targetDirectory, @NotNull ProfileTaskProgress progress) throws IOException {
        Path normalizedTarget = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTarget);
        try (ZipFile zip = openZip(archiveFile)) {
            if (zip.getEntry(ProfileStorage.METADATA_FILE_NAME) == null) throw new InvalidProfileArchiveException("The archive has no profile metadata: " + archiveFile);
            List<ZipEntry> entries = new ArrayList<>();
            long totalSize = 0L;
            Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (isSkippedEntry(entry.getName())) continue;
                resolveEntryPath(normalizedTarget, entry.getName());
                entries.add(entry);
                if (!entry.isDirectory()) totalSize += Math.max(0L, entry.getSize());
            }
            long usableSpace = Files.getFileStore(normalizedTarget).getUsableSpace();
            if (totalSize > usableSpace) throw new NotEnoughSpaceException(totalSize, usableSpace);
            progress.setTotalBytes(totalSize);
            byte[] buffer = new byte[ProfileFiles.BUFFER_SIZE];
            for (ZipEntry entry : entries) {
                progress.throwIfCancelled();
                Path target = resolveEntryPath(normalizedTarget, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(target)) {
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        progress.throwIfCancelled();
                        out.write(buffer, 0, read);
                        progress.addProcessedBytes(read);
                    }
                }
                FileTime modified = entry.getLastModifiedTime();
                if (modified != null) {
                    try {
                        Files.setLastModifiedTime(target, modified);
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    @NotNull
    private static ZipFile openZip(@NotNull Path archiveFile) throws IOException {
        try {
            return new ZipFile(archiveFile.toFile(), StandardCharsets.UTF_8);
        } catch (java.util.zip.ZipException ex) {
            throw new InvalidProfileArchiveException("The file is no valid profile archive: " + archiveFile, ex);
        }
    }

    private static boolean isSkippedEntry(@NotNull String entryName) {
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        return normalized.equals(ProfileStorage.STORE_DIRECTORY_NAME) || normalized.startsWith(ProfileStorage.STORE_DIRECTORY_NAME + "/");
    }

    /**
     * Resolves the target path of an entry and makes sure it stays inside the target directory ("Zip Slip" protection).
     */
    @NotNull
    static Path resolveEntryPath(@NotNull Path normalizedTargetDirectory, @NotNull String entryName) throws InvalidProfileArchiveException {
        String normalized = entryName.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":") || normalized.contains("\0")) throw new InvalidProfileArchiveException("The archive contains an invalid entry: " + entryName);
        Path resolved = normalizedTargetDirectory.resolve(normalized).normalize();
        if (!resolved.startsWith(normalizedTargetDirectory) || resolved.equals(normalizedTargetDirectory)) {
            throw new InvalidProfileArchiveException("The archive contains an entry outside of the profile: " + entryName);
        }
        return resolved;
    }

    @NotNull
    private static String toEntryName(@NotNull Path root, @NotNull Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static boolean isAlreadyCompressed(@NotNull Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return (dot >= 0) && COMPRESSED_FILE_EXTENSIONS.contains(name.substring(dot + 1));
    }

    /**
     * Thrown when a file is no valid profile archive.
     */
    public static class InvalidProfileArchiveException extends IOException {

        public InvalidProfileArchiveException(@NotNull String message) {
            super(message);
        }

        public InvalidProfileArchiveException(@NotNull String message, @Nullable Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Thrown when the target drive has not enough free space for an operation.
     */
    public static class NotEnoughSpaceException extends IOException {

        private final long requiredBytes;
        private final long usableBytes;

        public NotEnoughSpaceException(long requiredBytes, long usableBytes) {
            super("Not enough free disk space! Required: " + requiredBytes + " bytes, usable: " + usableBytes + " bytes");
            this.requiredBytes = requiredBytes;
            this.usableBytes = usableBytes;
        }

        public long getRequiredBytes() {
            return this.requiredBytes;
        }

        public long getUsableBytes() {
            return this.usableBytes;
        }

    }

}
