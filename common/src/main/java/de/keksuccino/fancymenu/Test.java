package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.texture.ApngTexture;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;

public class Test {

    ResourceFile file = ResourceFile.asset("config/fancymenu/assets/elephant.apng");
    ApngTexture apng = null;

    @EventListener
    public void onRenderPost(RenderScreenEvent.Post e) {

        if (this.apng == null) {
            apng = ApngTexture.local(file.getFile());
        }

        ResourceLocation loc = apng.getResourceLocation();
        if (loc != null) {
            RenderSystem.enableBlend();
            RenderingUtils.resetShaderColor();
            RenderingUtils.bindTexture(loc);
            GuiComponent.blit(e.getPoseStack(), 20, 20, 0.0F, 0.0F, apng.getWidth(), apng.getHeight(), apng.getWidth(), apng.getHeight());
            RenderingUtils.resetShaderColor();
        }

    }

}
