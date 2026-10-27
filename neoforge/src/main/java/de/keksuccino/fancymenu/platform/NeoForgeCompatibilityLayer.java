package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.textures.GpuTexture;
import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import de.keksuccino.fancymenu.platform.services.TitleScreenBrandingLineCollector;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;
import net.neoforged.neoforge.internal.BrandingControl;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class NeoForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public List<Component> getTitleScreenBrandingLines() {
        return TitleScreenBrandingLineCollector.collectTopToBottom(BrandingControl::forEachLine, Component::literal);
    }

    @Override
    public @Nullable GpuTexture getBackendTexture(@Nullable GpuTexture texture) {
        // NeoForge's "enableB3DValidationLayer" client option (on by default in development) wraps every texture the GpuDevice creates
        return (texture instanceof ValidationGpuTexture validationTexture) ? validationTexture.getRealTexture() : texture;
    }

}
