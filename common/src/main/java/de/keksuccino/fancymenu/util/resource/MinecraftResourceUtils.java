package de.keksuccino.fancymenu.util.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@SuppressWarnings("all")
public class MinecraftResourceUtils {

    public static Optional<Resource> get(@NotNull ResourceLocation location) {
        try {
            return Optional.of(Minecraft.getInstance().getResourceManager().getResourceOrThrow(location));
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    @NotNull
    public static InputStream open(@NotNull ResourceLocation location) throws IOException {
        Optional<Resource> resource = get(location);
        if (resource.isPresent()) {
            try {
                InputStream in = resource.get().open();
                if (in != null) return in;
            } catch (Exception ex) {
                throw new IOException("Error while trying to open resource!", ex);
            }
        }
        throw new IOException("Failed to open resource!");
    }

}
