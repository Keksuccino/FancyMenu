package de.keksuccino.fancymenu.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        List<String> contents = new ArrayList<>();

        try (FilePackResources pack = new FilePackResources("archive", archive.toFile(), false)) {
            PackResourcesRootEnumeration.listArchiveNamespaceRoot(archive, pack, PackType.CLIENT_RESOURCES, "example", (location, streamSupplier) -> {
                locations.add(location);
                contents.add(readAll(streamSupplier));
            });
        }

        assertEquals(Set.of(new ResourceLocation("example", "root.txt"), new ResourceLocation("example", "textures/gui/button.png")), locations);
        assertEquals(List.of("content", "content"), contents);
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
