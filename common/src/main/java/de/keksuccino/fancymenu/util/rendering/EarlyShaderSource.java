package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shader source that reads shaders straight from the client resource manager, for the time the {@link net.minecraft.client.renderer.ShaderManager} has no shader sources yet.
 * <p>
 * The GPU device compiles pipelines with the shader sources of the {@link net.minecraft.client.renderer.ShaderManager}, which stay empty until the first resource reload applied it.
 * FancyMenu's GUI renderers can draw before that point (for example in loading screens customized by Drippy Loading Screen), and without sources their pipelines
 * get compiled invalid, which leaves them invisible until the reload finished. The client resource manager already contains all mod and resource packs
 * once the initial reload started, so this source fills that gap. Imports get resolved like the shader manager resolves them.
 */
public final class EarlyShaderSource implements ShaderSource {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String INCLUDE_DIRECTORY = "shaders/include/";

    public static final EarlyShaderSource CLIENT_RESOURCES = new EarlyShaderSource(() -> Minecraft.getInstance().getResourceManager());

    private final Supplier<? extends ResourceProvider> resources;

    public EarlyShaderSource(@NotNull Supplier<? extends ResourceProvider> resources) {
        this.resources = Objects.requireNonNull(resources);
    }

    @Override
    public @Nullable String get(@NotNull Identifier id, @NotNull ShaderType type) {
        Identifier location = type.idConverter().idToFile(id);
        // Missing shaders stay silent here, because the GPU device already logs every shader it can't find a source for.
        String source = this.read(location);
        return (source != null) ? String.join("", this.createPreprocessor(location).process(source)) : null;
    }

    @NotNull
    private GlslPreprocessor createPreprocessor(@NotNull Identifier location) {
        Identifier parentLocation = location.withPath(FileUtil::getFullResourcePath);
        Set<Identifier> importedLocations = new HashSet<>();
        return new GlslPreprocessor() {
            @Override
            public @Nullable String applyImport(boolean isRelative, @NotNull String path) {
                Identifier importLocation;
                try {
                    importLocation = isRelative ? parentLocation.withPath(parentPath -> FileUtil.normalizeResourcePath(parentPath + path)) : Identifier.parse(path).withPrefix(INCLUDE_DIRECTORY);
                } catch (IdentifierException ex) {
                    LOGGER.error("[FANCYMENU] Malformed GLSL import in {}: {}", location, path, ex);
                    return "#error " + ex.getMessage();
                }
                if (!importedLocations.add(importLocation)) return null;
                String importSource = EarlyShaderSource.this.read(importLocation);
                if (importSource == null) {
                    LOGGER.error("[FANCYMENU] Couldn't find GLSL import {} of {}", importLocation, location);
                    return "#error Missing GLSL import " + importLocation;
                }
                return importSource;
            }
        };
    }

    private @Nullable String read(@NotNull Identifier location) {
        Optional<Resource> resource = this.resources.get().getResource(location);
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            return IOUtils.toString(reader);
        } catch (IOException ex) {
            LOGGER.error("[FANCYMENU] Couldn't read shader source: {}", location, ex);
            return null;
        }
    }

}
