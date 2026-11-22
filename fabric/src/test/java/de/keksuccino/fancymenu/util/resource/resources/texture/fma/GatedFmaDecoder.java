package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import de.keksuccino.fancymenu.util.resource.resources.texture.GatedZipChannel;
import de.keksuccino.fancymenu.util.resource.resources.texture.ReadGate;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The real decoder, which spools streams into a test directory, can hold its loading at a gate, and reads its archive through a {@link GatedZipChannel}.
 */
final class GatedFmaDecoder extends FmaDecoder {

    @NotNull
    private final File tempDirectory;
    /** Holds the decoder after it opened its archive and before it reads anything from it, like a loading thread that is busy with the archive. **/
    @Nullable
    private final ReadGate loadingGate;
    @Nullable
    private volatile GatedZipChannel channel;

    GatedFmaDecoder(@NotNull File tempDirectory, @Nullable ReadGate loadingGate) {
        this.tempDirectory = tempDirectory;
        this.loadingGate = loadingGate;
    }

    GatedFmaDecoder(@NotNull File tempDirectory) {
        this(tempDirectory, null);
    }

    /**
     * The channel of the archive the decoder opened, or NULL if it never opened one.
     */
    @Nullable
    GatedZipChannel openedChannel() {
        return this.channel;
    }

    @NotNull
    GatedZipChannel requireOpenedChannel() {
        GatedZipChannel opened = this.channel;
        assertNotNull(opened, "The decoder didn't open its archive");
        return opened;
    }

    @Override
    protected @NotNull File resolveTempDirectory() {
        return this.tempDirectory;
    }

    @Override
    protected @NotNull ZipFile openZipFile(@NotNull File archiveFile) throws IOException {
        GatedZipChannel opened = new GatedZipChannel(archiveFile.toPath());
        this.channel = opened;
        return opened.openZipFile();
    }

    @Override
    protected void initializeArchiveState() throws IOException {
        if (this.loadingGate != null) this.loadingGate.pass();
        super.initializeArchiveState();
    }

}
