package de.keksuccino.fancymenu.customization.element.elements.video.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.video.MCEFVideoPlayer;
import de.keksuccino.fancymenu.util.rendering.video.VideoManager;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class McefVideoElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public ResourceSource rawVideoUrlSource = null;
    @NotNull
    public DrawableColor imageTint = DrawableColor.of("#FFFFFF");
    public boolean repeat = false;
    public boolean nineSlice = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;

    protected boolean initialized = false;
    protected final VideoManager videoManager = VideoManager.getInstance();
    protected MCEFVideoPlayer videoPlayer = null;
    protected String playerId = null;

    protected String lastFinalUrl = null;
    protected int lastAbsoluteWidth = -10000;
    protected int lastAbsoluteHeight = -10000;
    protected int lastAbsoluteX = -10000;
    protected int lastAbsoluteY = -10000;

    public McefVideoElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            if (!this.initialized) {
                this.initialized = true;
                playerId = videoManager.createPlayer(0, 0, 10, 10);
                if (playerId != null) {
                    videoPlayer = videoManager.getPlayer(playerId);
                    if (videoPlayer != null) {
                        LOGGER.info("[FANCYMENU] Created video player");

                        // Set player options
                        videoPlayer.setVolume(0.1f);
                        videoPlayer.setLooping(true);
                        videoPlayer.setFillScreen(true);
                        videoPlayer.play();

                        // Don't load a video immediately
                        LOGGER.info("[FANCYMENU] Ready to load videos - press 1, 2, or 3 to try different loading methods");
                    }
                }
            }

            if (this.videoPlayer == null) return;

            String finalVideoUrl = null;
            if (this.rawVideoUrlSource != null) {
                finalVideoUrl = PlaceholderParser.replacePlaceholders(this.rawVideoUrlSource.getSourceWithoutPrefix());
            }
            if (!Objects.equals(finalVideoUrl, this.lastFinalUrl)) {
                if (finalVideoUrl != null) {
                    this.videoPlayer.loadVideo(finalVideoUrl);
                } else {
                    this.videoPlayer.stop();
                }
            }
            this.lastFinalUrl = finalVideoUrl;

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            if (finalVideoUrl != null) {
                if ((this.lastAbsoluteX != x) || (this.lastAbsoluteY != y)) {
                    this.videoPlayer.setPosition(x, y);
                }
                this.lastAbsoluteX = x;
                this.lastAbsoluteY = y;
                if ((this.lastAbsoluteWidth != w) || (this.lastAbsoluteHeight != h)) {
                    this.videoPlayer.resizeToFill(w, h);
                }
                this.lastAbsoluteWidth = w;
                this.lastAbsoluteHeight = h;
            }

            RenderSystem.enableBlend();

            if (finalVideoUrl != null) {
                this.videoPlayer.render(graphics, mouseX, mouseY, partial);
            } else {
                graphics.fill(x, y, x + w, y + h, DrawableColor.BLACK.getColorInt());
            }

            RenderSystem.disableBlend();

        }

    }

    @Override
    public void onDestroyElement() {
        if ((this.videoManager != null) && (this.playerId != null)) {
            this.videoManager.removePlayer(this.playerId);
        }
    }

    public void restoreAspectRatio() {
//        ITexture t = this.getTextureResource();
//        AspectRatio ratio = (t != null) ? t.getAspectRatio() : new AspectRatio(10, 10);
//        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

}
