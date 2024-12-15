package de.keksuccino.fancymenu.util.resource.resources.texture;

import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.file.Counters;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public interface ITexture extends RenderableResource {

    public static final Counters.Counter TEX_REGISTRY_COUNTER = Counters.longCounter();

    @NotNull
    default ResourceLocation registerAbstractTexture(@NotNull AbstractTexture texture) {
        TEX_REGISTRY_COUNTER.increment();
        return RenderUtils.register("fancymenu_texture_" + TEX_REGISTRY_COUNTER.get(), Objects.requireNonNull(texture));
    }

}
