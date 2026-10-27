package de.keksuccino.fancymenu.util.rendering;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shader source of the vanilla fallback pipeline cache, extended to also resolve FancyMenu's own shaders and includes.
 * <p>
 * Until the {@link net.minecraft.client.renderer.ShaderManager} applied its first resource reload, {@link com.mojang.blaze3d.systems.RenderSystem#getCompiledPipeline}
 * compiles every pipeline through the fallback pipeline cache, and vanilla's source for that cache only reads the vanilla pack.
 * FancyMenu's GUI renderers can draw before that point (for example in loading screens customized by Drippy Loading Screen), so this source
 * reads FancyMenu's shaders from the client resource manager instead, which already contains all mod and resource packs once the initial reload started.
 * <p>
 * Pipelines compiled by the fallback cache stay cached in it for the whole session, but they are only used while the pipeline cache of the
 * {@link net.minecraft.client.renderer.ShaderManager} is unavailable or fails to compile them.
 */
public final class EarlyShaderSource implements ShaderSource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FANCYMENU_NAMESPACE = "fancymenu";
    private static final String FANCYMENU_SHADER_PREFIX = "fancymenu_";
    private static final String INCLUDE_DIRECTORY = "shaders/include/";

    private final ShaderSource vanillaSource;
    private final Supplier<? extends ResourceProvider> resources;
    // Shaderc only borrows include results, so they have to stay alive until this source gets closed.
    private final Map<Identifier, CachedIncludeSource> includes = new ConcurrentHashMap<>();

    public EarlyShaderSource(@NotNull ShaderSource vanillaSource, @NotNull Supplier<? extends ResourceProvider> resources) {
        this.vanillaSource = Objects.requireNonNull(vanillaSource);
        this.resources = Objects.requireNonNull(resources);
    }

    @NotNull
    public static EarlyShaderSource forClientResources(@NotNull ShaderSource vanillaSource) {
        return new EarlyShaderSource(vanillaSource, () -> Minecraft.getInstance().getResourceManager());
    }

    @Override
    public @Nullable String getShader(@NotNull Identifier id, @NotNull ShaderType type) {
        // FancyMenu shaders skip the vanilla source, because it logs an error for every shader that is not part of the vanilla pack.
        if (!isFancyMenuShader(id)) return this.vanillaSource.getShader(id, type);
        return this.read(type.idConverter().idToFile(id));
    }

    @Override
    public @Nullable CachedIncludeSource getInclude(@NotNull Identifier id) {
        if (!isFancyMenuShader(id)) return this.vanillaSource.getInclude(id);
        return this.includes.computeIfAbsent(id, includeId -> {
            String contents = this.read(includeId.withPrefix(INCLUDE_DIRECTORY));
            return (contents != null) ? CachedIncludeSource.create(includeId, contents) : null;
        });
    }

    @Override
    public void close() {
        this.includes.values().forEach(CachedIncludeSource::close);
        this.includes.clear();
        this.vanillaSource.close();
    }

    /**
     * Returns if the given shader or include ID belongs to FancyMenu.<br>
     * FancyMenu's shaders live in the {@code minecraft} namespace with a {@code fancymenu_} file name prefix, its includes live in the {@code fancymenu} namespace.
     */
    static boolean isFancyMenuShader(@NotNull Identifier id) {
        if (FANCYMENU_NAMESPACE.equals(id.getNamespace())) return true;
        String path = id.getPath();
        return path.startsWith(FANCYMENU_SHADER_PREFIX, path.lastIndexOf('/') + 1);
    }

    private @Nullable String read(@NotNull Identifier location) {
        Optional<Resource> resource = this.resources.get().getResource(location);
        if (resource.isEmpty()) {
            LOGGER.error("[FANCYMENU] Couldn't find shader source: {}", location);
            return null;
        }
        try {
            return resource.get().readAllAsString();
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Couldn't read shader source: {}", location, ex);
            return null;
        }
    }

}
