package de.keksuccino.fancymenu.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.jarjar.nio.layzip.LayeredZipFileSystemProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PackResourcesRootEnumerationTest {

    @Test
    void removesOnlyTheExtraSlashProducedForArchiveRootEnumeration() {
        assertEquals("assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example//", ""));
        assertEquals("overlays/modern/assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("overlays/modern/assets/example//", ""));
    }

    @Test
    void leavesNonRootAndAlreadyValidArchivePrefixesUnchanged() {
        assertEquals("assets/example/textures/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example/textures/", "textures"));
        assertEquals("assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example/", ""));
    }

    @Test
    void representsTheNamespaceRootWithNoPathSegments() {
        DataResult<List<String>> result = PackResourcesRootEnumeration.decomposeDirectory("", args -> fail("Vanilla decomposition must not receive its invalid empty path"));

        assertEquals(List.of(), result.result().orElseThrow());
    }

    @Test
    void delegatesNonRootDirectoriesUnchanged() {
        DataResult<List<String>> expected = DataResult.success(List.of("textures", "gui"));
        Operation<DataResult<List<String>>> original = args -> {
            assertEquals(List.of("textures/gui"), List.of(args));
            return expected;
        };

        assertSame(expected, PackResourcesRootEnumeration.decomposeDirectory("textures/gui", original));
    }

    @Test
    void enumeratesRootAndNestedFilesFromAPathBackedNamespace(@TempDir Path packRoot) throws Exception {
        Path namespaceRoot = packRoot.resolve("assets/example");
        Files.createDirectories(namespaceRoot.resolve("textures/gui"));
        Files.writeString(namespaceRoot.resolve("root.txt"), "root");
        Files.writeString(namespaceRoot.resolve("textures/gui/button.png"), "image");
        Set<ResourceLocation> locations = new LinkedHashSet<>();

        PackResourcesRootEnumeration.listPathNamespaceRoot(packRoot, PackType.CLIENT_RESOURCES, "example", (location, streamSupplier) -> locations.add(location));

        assertEquals(Set.of(new ResourceLocation("example", "root.txt"), new ResourceLocation("example", "textures/gui/button.png")), locations);
    }

    @Test
    void enumeratesValidRootAndNestedFilesFromAnArchivedNamespace(@TempDir Path tempDirectory) throws Exception {
        Path archive = tempDirectory.resolve("resources.jar");
        try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(archive))) {
            writeArchiveEntry(outputStream, "assets/example/root.txt");
            writeArchiveEntry(outputStream, "assets/example/textures/gui/button.png");
            writeArchiveEntry(outputStream, "assets/example/lang/README.txt");
            writeArchiveEntry(outputStream, "assets/other/ignored.txt");
        }
        Set<ResourceLocation> locations = new LinkedHashSet<>();
        List<IoSupplier<InputStream>> suppliers = new ArrayList<>();

        try (FilePackResources pack = new FilePackResources("archive", archive.toFile(), false)) {
            PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "example", (location, streamSupplier) -> {
                locations.add(location);
                suppliers.add(streamSupplier);
            });

            assertEquals(List.of("content", "content"), suppliers.stream().map(PackResourcesRootEnumerationTest::readAll).toList());
        }

        assertEquals(Set.of(new ResourceLocation("example", "root.txt"), new ResourceLocation("example", "textures/gui/button.png")), locations);
    }

    @Test
    void enumeratesAForgeJarInJarRootWithoutConvertingItToADiskFile(@TempDir Path tempDirectory) throws Exception {
        Path outerArchive = createNestedArchive(tempDirectory);
        try (FileSystem outerFileSystem = FileSystems.newFileSystem(outerArchive)) {
            Path nestedArchive = outerFileSystem.getPath("/META-INF/jarjar/resources.jar");
            URI uri = URI.create("jij:" + nestedArchive.toUri().getRawSchemeSpecificPart());
            try (FileSystem jarInJarFileSystem = new LayeredZipFileSystemProvider().newFileSystem(uri, Map.of("packagePath", nestedArchive))) {
                Path archive = jarInJarFileSystem.getPath("/");
                // This is the Forge path that triggered #1783: it represents existing archive bytes, despite having an empty display path.
                assertTrue(archive.toString().isEmpty());
                assertTrue(Files.isRegularFile(archive));
                assertReadableNestedArchive(archive);
                assertTrue(jarInJarFileSystem.isOpen());
            }
            assertTrue(outerFileSystem.isOpen());
        }
    }

    @Test
    void enumeratesAnArchiveEntryFromAnotherFileSystem(@TempDir Path tempDirectory) throws Exception {
        Path outerArchive = createNestedArchive(tempDirectory);
        try (FileSystem outerFileSystem = FileSystems.newFileSystem(outerArchive)) {
            assertReadableNestedArchive(outerFileSystem.getPath("/META-INF/jarjar/resources.jar"));
            assertTrue(outerFileSystem.isOpen());
        }
    }

    @Test
    void ignoresAbsentNamespacesAndResourcesRejectedByTheOwningPack(@TempDir Path tempDirectory) throws Exception {
        Path archive = tempDirectory.resolve("resources.jar");
        try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(archive))) {
            writeArchiveEntry(outputStream, "assets/example/root.txt");
        }
        try (FilePackResources pack = new FilePackResources("filtered", archive.toFile(), false) {
            @Override
            public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
                return null;
            }
        }) {
            PackResources.ResourceOutput output = (location, supplier) -> fail("Unexpected resource");
            PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "absent", output);
            PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "example", output);
        }
    }

    @Test
    void reportsUnreadableArchives(@TempDir Path tempDirectory) {
        Path archive = tempDirectory.resolve("missing.jar");
        try (FilePackResources pack = new FilePackResources("missing", archive.toFile(), false)) {
            UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "example", (location, supplier) -> fail("Unexpected resource")));
            assertTrue(failure.getCause() instanceof IOException);
        }
    }

    @Test
    void enumeratesOnlyDelegatingChildrenThatOwnTheRequestedNamespace(@TempDir Path packRoot) throws Exception {
        Path alphaRoot = packRoot.resolve("alpha-pack");
        Path betaRoot = packRoot.resolve("beta-pack");
        Files.createDirectories(alphaRoot.resolve("assets/alpha"));
        Files.createDirectories(betaRoot.resolve("assets/beta"));
        Files.writeString(alphaRoot.resolve("assets/alpha/alpha.txt"), "alpha");
        Files.writeString(betaRoot.resolve("assets/beta/beta.txt"), "beta");
        PackResources alpha = new PathPackResources("alpha", alphaRoot, false);
        PackResources beta = new PathPackResources("beta", betaRoot, false);
        List<String> enumeratedChildren = new ArrayList<>();
        Set<ResourceLocation> locations = new LinkedHashSet<>();

        PackResourcesRootEnumeration.listMatchingChildNamespaceRoots(List.of(alpha, beta), PackType.CLIENT_RESOURCES, "alpha", (location, streamSupplier) -> locations.add(location), (child, type, namespace, output) -> {
            enumeratedChildren.add(child.packId());
            PackResourcesRootEnumeration.listPathNamespaceRoot(child == alpha ? alphaRoot : betaRoot, type, namespace, output);
        });

        assertEquals(List.of("alpha"), enumeratedChildren);
        assertEquals(Set.of(new ResourceLocation("alpha", "alpha.txt")), locations);
    }

    @Test
    void keepsPartialResultsAndContinuesAfterChildEnumerationFailures(@TempDir Path packRoot) {
        PackResources first = namespacePack("first", packRoot);
        PackResources second = namespacePack("second", packRoot);
        PackResources healthy = namespacePack("healthy", packRoot);
        RuntimeException firstFailure = new IllegalStateException();
        RuntimeException secondFailure = new IllegalArgumentException();
        Set<ResourceLocation> locations = new LinkedHashSet<>();

        RuntimeException failure = assertThrows(RuntimeException.class, () -> PackResourcesRootEnumeration.listMatchingChildNamespaceRoots(List.of(first, second, healthy), PackType.CLIENT_RESOURCES, "example", (location, supplier) -> locations.add(location), (child, type, namespace, output) -> {
            output.accept(new ResourceLocation(namespace, child.packId() + ".txt"), () -> fail("Enumeration must not open resources"));
            if (child == first) throw firstFailure;
            if (child == second) throw secondFailure;
        }));

        assertEquals(Set.of(new ResourceLocation("example", "first.txt"), new ResourceLocation("example", "second.txt"), new ResourceLocation("example", "healthy.txt")), locations);
        assertEquals(List.of(firstFailure, secondFailure), List.of(failure.getSuppressed()));
    }

    @Test
    void continuesAfterAChildFailsToReportItsNamespaces(@TempDir Path packRoot) {
        RuntimeException namespaceFailure = new IllegalStateException();
        PackResources broken = new PathPackResources("broken", packRoot, false) {
            @Override
            public Set<String> getNamespaces(PackType type) {
                throw namespaceFailure;
            }
        };
        PackResources healthy = namespacePack("healthy", packRoot);
        List<PackResources> enumeratedChildren = new ArrayList<>();

        RuntimeException failure = assertThrows(RuntimeException.class, () -> PackResourcesRootEnumeration.listMatchingChildNamespaceRoots(List.of(broken, healthy), PackType.CLIENT_RESOURCES, "example", (location, supplier) -> fail("Unexpected resource"), (child, type, namespace, output) -> enumeratedChildren.add(child)));

        assertEquals(List.of(healthy), enumeratedChildren);
        assertEquals(List.of(namespaceFailure), List.of(failure.getSuppressed()));
    }

    private static Path createNestedArchive(Path tempDirectory) throws IOException {
        ByteArrayOutputStream nestedBytes = new ByteArrayOutputStream();
        try (ZipOutputStream outputStream = new ZipOutputStream(nestedBytes)) {
            writeArchiveEntry(outputStream, "assets/example/root.txt");
            writeArchiveEntry(outputStream, "assets/example/textures/gui/button.png");
            writeArchiveEntry(outputStream, "assets/other/ignored.txt");
            writeArchiveEntry(outputStream, "data/example/ignored.json");
        }
        Path archive = tempDirectory.resolve("outer.jar");
        try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(archive))) {
            outputStream.putNextEntry(new ZipEntry("META-INF/jarjar/resources.jar"));
            outputStream.write(nestedBytes.toByteArray());
            outputStream.closeEntry();
        }
        return archive;
    }

    private static void assertReadableNestedArchive(Path archive) throws IOException {
        try (FileSystem packFileSystem = FileSystems.newFileSystem(archive); PackResources pack = new PathPackResources("nested", packFileSystem.getPath("/"), false)) {
            Set<ResourceLocation> locations = new LinkedHashSet<>();
            List<IoSupplier<InputStream>> suppliers = new ArrayList<>();

            PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "example", (location, supplier) -> {
                locations.add(location);
                suppliers.add(supplier);
            });

            assertEquals(Set.of(new ResourceLocation("example", "root.txt"), new ResourceLocation("example", "textures/gui/button.png")), locations);
            assertTrue(packFileSystem.isOpen());
            assertEquals(List.of("content", "content"), suppliers.stream().map(PackResourcesRootEnumerationTest::readAll).toList());
        }
    }

    private static PackResources namespacePack(String name, Path packRoot) {
        return new PathPackResources(name, packRoot, false) {
            @Override
            public Set<String> getNamespaces(PackType type) {
                return Set.of("example");
            }
        };
    }

    private static void writeArchiveEntry(ZipOutputStream outputStream, String name) throws IOException {
        outputStream.putNextEntry(new ZipEntry(name));
        outputStream.write("content".getBytes(StandardCharsets.UTF_8));
        outputStream.closeEntry();
    }

    private static String readAll(IoSupplier<InputStream> streamSupplier) {
        try (InputStream inputStream = streamSupplier.get()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
