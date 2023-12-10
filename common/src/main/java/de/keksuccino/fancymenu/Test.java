package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.resources.video.clip.MP4VideoClip;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    protected MP4VideoClip video = null;

    @EventListener
    public void onRenderPost(RenderedScreenBackgroundEvent e) {

        try {

            if (e.getScreen() instanceof OptionsScreen) {

                this.renderVideo(e.getPoseStack(), 0, 0, e.getScreen().width, e.getScreen().height);

            } else if (video != null) {
                video.pause();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void renderVideo(@NotNull PoseStack pose, int x, int y, int width, int height) throws Exception {

        if (video == null) {

            video = new MP4VideoClip(new File(GameDirectoryUtils.getGameDirectory(), "/horimiya.mp4"));
            video.play();

        } else if (video.isReady()) {

            if (video.isPaused()) video.resume();

            ResourceLocation loc = video.getRenderResourceLocation();
            if (loc != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderingUtils.resetShaderColor();
                RenderingUtils.bindTexture(loc);
                GuiComponent.blit(pose, x, y, 0.0F, 0.0F, width, height, width, height);
                RenderingUtils.resetShaderColor();
            }

        }

    }

}
