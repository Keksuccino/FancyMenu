package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads image files as textures for the profile UIs and keeps them until the UI closes.<br>
 * The icons don't go through FancyMenu's resource handlers, because those cache textures by path, and icons get replaced at the same path.
 */
final class ProfileIconCache {

    private final Map<Path, Entry> textures = new HashMap<>();

    /**
     * @return the texture of the image file, or NULL if the file does not exist.
     */
    @Nullable
    ITexture get(@NotNull Path imageFile) {
        Entry entry = this.textures.get(imageFile);
        if (entry != null) return entry.texture;
        PngTexture texture = null;
        if (Files.isRegularFile(imageFile)) {
            texture = PngTexture.local(imageFile.toFile());
        }
        this.textures.put(imageFile, new Entry(texture));
        return texture;
    }

    /**
     * Forgets a file, so it gets loaded again the next time, for example after its image changed.
     */
    void invalidate(@NotNull Path imageFile) {
        Entry entry = this.textures.remove(imageFile);
        if ((entry != null) && (entry.texture != null)) entry.texture.close();
    }

    void clear() {
        for (Entry entry : this.textures.values()) {
            if (entry.texture != null) entry.texture.close();
        }
        this.textures.clear();
    }

    private record Entry(@Nullable PngTexture texture) {
    }

}
