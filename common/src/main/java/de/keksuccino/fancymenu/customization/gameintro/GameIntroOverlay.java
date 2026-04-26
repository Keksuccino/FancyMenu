package de.keksuccino.fancymenu.customization.gameintro;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.resources.audio.PlayableResourceWithAudio;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class GameIntroOverlay extends Overlay {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU = DrawableColor.of(180, 0, 0);
    private static final String WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia/files/all?page=1&pageSize=20&showAlphaFiles=show";
    private static final String WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia-binaries/files/all?page=1&pageSize=20&showAlphaFiles=show";

    protected Font font = Minecraft.getInstance().font;
    @NotNull
    protected Screen fadeTo;
    protected PlayableResource intro;
    protected float opacity = 1.0F;
    protected long start = -1;
    protected boolean fadeToInitialized = false;
    protected int width;
    protected int height;
    protected float cachedActualVolume = -10000F;
    protected float lastCachedActualVolume = -11000F;
    protected float watermediaDownloadX_FancyMenu = Float.NaN;
    protected float watermediaDownloadY_FancyMenu = Float.NaN;
    protected float watermediaDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaDownloadHeight_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadX_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadY_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    protected boolean watermediaLeftMouseWasDown_FancyMenu = false;

    public GameIntroOverlay(@NotNull Screen fadeTo, @NotNull PlayableResource intro) {
        super();
        this.fadeTo = Objects.requireNonNull(fadeTo);
        this.intro = Objects.requireNonNull(intro);
        this.intro.waitForReady(5000);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.resetWatermediaDownloadLinkBounds_FancyMenu();

        //----------------------

        boolean startIntro = (this.start == -1);
        if (startIntro) {
            this.start = System.currentTimeMillis();
            this.intro.stop();
            if (this.intro instanceof IVideo videoIntro) {
                videoIntro.setPlayTime(0.0F);
            }
        }

        this.updateIntroVolume();

        if (startIntro) {
            this.intro.play();
        }

        if (this.endOfIntroReached() && !this.fadeToInitialized) {
            this.initFadeToScreen();
        }

        this.tickFadeOut();

        //Close screen after finished playing
        if (this.endOfIntroReached() && (!this.fadeOutIntro() || (this.opacity < 0.1F))) {
            this.close();
            return;
        }

        if (this.endOfIntroReached()) {
            RenderingUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(this.fadeTo, graphics, mouseX, mouseY, partial));
            this.fadeTo.render(graphics, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(this.fadeTo, graphics, mouseX, mouseY, partial));
            RenderingUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
        } else {
            graphics.fill(0, 0, this.width, this.height, DrawableColor.BLACK.getColorInt());
        }

        RenderingUtils.resetShaderColor(graphics);

        this.renderIntro(graphics, mouseX, mouseY, partial);
        double resolvedMouseX_FancyMenu = MouseUtil.getGuiScaledMouseX();
        double resolvedMouseY_FancyMenu = MouseUtil.getGuiScaledMouseY();
        boolean showWatermediaWarning_FancyMenu = this.shouldRenderWatermediaMissingOverlay_FancyMenu();
        this.tickWatermediaMissingOverlayMouseClick_FancyMenu(showWatermediaWarning_FancyMenu, resolvedMouseX_FancyMenu, resolvedMouseY_FancyMenu);
        if (showWatermediaWarning_FancyMenu) {
            this.renderWatermediaMissingOverlay_FancyMenu(graphics, resolvedMouseX_FancyMenu, resolvedMouseY_FancyMenu);
        }

        this.renderSkipText(graphics, mouseX, mouseY, partial);

    }

    protected void renderIntro(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.intro instanceof RenderableResource r) {

            AspectRatio ratio = r.getAspectRatio();
            int[] size = ratio.getAspectRatioSizeByMinimumSize(this.width, this.height);
            int aspectWidth = size[0];
            int aspectHeight = size[1];
            int x = (this.width / 2) - (aspectWidth / 2);
            int y = (this.height / 2) - (aspectHeight / 2);

            Identifier location = r.getResourceLocation();
            if (location != null) {
                com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
                de.keksuccino.fancymenu.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.opacity);
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, location, x, y, 0.0F, 0.0F, aspectWidth, aspectHeight, aspectWidth, aspectHeight);
            }

            RenderingUtils.resetShaderColor(graphics);

        }

    }

    protected void renderSkipText(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (FancyMenu.getOptions().gameIntroAllowSkip.getValue()) {
            float scale = 1.3F;
            String customSkipText = FancyMenu.getOptions().gameIntroCustomSkipText.getValue();
            if (!customSkipText.isEmpty() && LocalizationUtils.isLocalizationKey(customSkipText)) {
                customSkipText = I18n.get(customSkipText);
            }
            Component skipComp = customSkipText.isEmpty() ? Component.translatable("fancymenu.game_intro.press_any_key") : Component.literal(customSkipText);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
            RenderingUtils.resetShaderColor(graphics);
            int normalizedWidth = (int)(this.width / scale);
            int normalizedHeight = (int)(this.height / scale);
            int textX = (normalizedWidth / 2) - (this.font.width(skipComp) / 2);
            int textY = normalizedHeight - 40;
            graphics.drawString(this.font, skipComp, textX, textY, RenderingUtils.replaceAlphaInColor(DrawableColor.WHITE.getColorInt(), Math.max(0.1F, 0.6F * this.opacity)), false);
            graphics.pose().popMatrix();
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    protected boolean fadeOutIntro() {
        return FancyMenu.getOptions().gameIntroFadeOut.getValue();
    }

    protected void updateIntroVolume() {
        if (!(this.intro instanceof PlayableResourceWithAudio)) return;
        float volume = GameIntroHandler.getIntroVolume();
        this.setIntroVolume(volume, false);
        if ((this.lastCachedActualVolume == -11000F) || (this.cachedActualVolume != this.lastCachedActualVolume)) {
            this.setIntroVolume(volume, true);
        }
        this.lastCachedActualVolume = this.cachedActualVolume;
    }

    protected void setIntroVolume(float volume, boolean updateIntro) {
        if (this.intro instanceof PlayableResourceWithAudio introAudio) {
            float actualVolume = Math.max(0.0F, Math.min(1.0F, volume));
            float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            SoundSource soundSource = GameIntroHandler.getIntroSoundSource();
            float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(soundSource);
            if (soundSource != SoundSource.MASTER) {
                soundSourceVolume *= masterVolume;
            }
            actualVolume *= soundSourceVolume;
            this.cachedActualVolume = actualVolume;
            if (updateIntro) {
                introAudio.setVolume(Math.max(0.0F, Math.min(1.0F, actualVolume)));
            }
        }
    }

    protected boolean endOfIntroReached() {
        if (this.start == -1) return false;
        long now = System.currentTimeMillis();
        //If not playing (anymore) 2 seconds after starting to play it, consider the playable resource finished
        return ((this.start + 2000) < now) && !this.intro.isPlaying();
    }

    protected void tickFadeOut() {
        if (this.endOfIntroReached() && this.fadeOutIntro()) {
            this.opacity -= 0.02F;
        }
    }

    protected void initFadeToScreen() {

        ScreenCustomization.setIsNewMenu(true);

        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this.fadeTo);
        if (layer != null) layer.resetLayer();

        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));

        this.fadeTo.init(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());

        ScrollScreenNormalizer.normalizeScrollableScreen(this.fadeTo);

        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));

        this.fadeToInitialized = true;

    }

    protected void close() {
        this.intro.stop();
        if (!this.fadeToInitialized) this.initFadeToScreen();
        Minecraft.getInstance().setOverlay(null);
    }

    public void keyPressed(int keycode, int scancode, int modifiers) {
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().gameIntroAllowSkip.getValue()) {
            this.close();
        }
    }

    public void mouseClicked(int button) {
        if (MouseUtil.MouseButton.fromGlfwButton(button) == MouseUtil.MouseButton.LEFT || MouseUtil.isLeftMouseDown()) {
            boolean handled = this.handleWatermediaMissingOverlayClick_FancyMenu(MouseUtil.getGuiScaledMouseX(), MouseUtil.getGuiScaledMouseY());
            if (handled) {
                this.watermediaLeftMouseWasDown_FancyMenu = true;
                return;
            }
        }
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().gameIntroAllowSkip.getValue()) {
            this.close();
        }
    }

    protected boolean shouldRenderWatermediaMissingOverlay_FancyMenu() {
        return (this.intro instanceof IVideo) && !WatermediaUtil.isWatermediaVideoPlaybackAvailable();
    }

    protected void renderWatermediaMissingOverlay_FancyMenu(@NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, this.width, this.height, WATERMEDIA_MISSING_BACKGROUND_COLOR_FANCYMENU.getColorIntWithAlpha(this.opacity));

        Component infoText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.info");
        Component downloadText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download");
        Component downloadBinariesText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download_binaries");

        float normalTextSize = UIBase.getUITextSizeNormal();
        float largeTextSize = UIBase.getUITextSizeLarge();
        float infoTextWidth = UIBase.getUITextWidth(infoText, normalTextSize);
        float infoTextHeight = UIBase.getUITextHeight(normalTextSize);
        float downloadTextWidth = UIBase.getUITextWidth(downloadText, largeTextSize);
        float downloadTextHeight = UIBase.getUITextHeight(largeTextSize);
        float downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, largeTextSize);
        float downloadBinariesTextHeight = UIBase.getUITextHeight(largeTextSize);
        float spacing = Math.max(4.0F, UIBase.getUITextHeightSmall());
        float totalHeight = infoTextHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;

        float infoX = (this.width / 2.0F) - (infoTextWidth / 2.0F);
        float infoY = (this.height / 2.0F) - (totalHeight / 2.0F);
        float downloadX = (this.width / 2.0F) - (downloadTextWidth / 2.0F);
        float downloadY = infoY + infoTextHeight + spacing;
        float downloadBinariesX = (this.width / 2.0F) - (downloadBinariesTextWidth / 2.0F);
        float downloadBinariesY = downloadY + downloadTextHeight + spacing;

        this.watermediaDownloadX_FancyMenu = downloadX;
        this.watermediaDownloadY_FancyMenu = downloadY;
        this.watermediaDownloadWidth_FancyMenu = downloadTextWidth;
        this.watermediaDownloadHeight_FancyMenu = downloadTextHeight;
        this.watermediaBinariesDownloadX_FancyMenu = downloadBinariesX;
        this.watermediaBinariesDownloadY_FancyMenu = downloadBinariesY;
        this.watermediaBinariesDownloadWidth_FancyMenu = downloadBinariesTextWidth;
        this.watermediaBinariesDownloadHeight_FancyMenu = downloadBinariesTextHeight;

        boolean hoveredMain = this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY);
        boolean hoveredBinaries = this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY);
        if (hoveredMain || hoveredBinaries) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }
        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withBold(true).withUnderlined(hoveredBinaries));

        int textColor = DrawableColor.WHITE.getColorIntWithAlpha(this.opacity);
        UIBase.renderText(graphics, infoText, infoX, infoY, textColor, normalTextSize);
        UIBase.renderText(graphics, renderedDownloadText, downloadX, downloadY, textColor, largeTextSize);
        UIBase.renderText(graphics, renderedDownloadBinariesText, downloadBinariesX, downloadBinariesY, textColor, largeTextSize);
    }

    protected boolean handleWatermediaMissingOverlayClick_FancyMenu(double mouseX, double mouseY) {
        if (!this.shouldRenderWatermediaMissingOverlay_FancyMenu()) return false;
        if (this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        if (this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        return false;
    }

    protected void tickWatermediaMissingOverlayMouseClick_FancyMenu(boolean showWarning, double mouseX, double mouseY) {
        boolean leftDown = MouseUtil.isLeftMouseDown();
        if (showWarning && leftDown && !this.watermediaLeftMouseWasDown_FancyMenu) {
            this.handleWatermediaMissingOverlayClick_FancyMenu(mouseX, mouseY);
        }
        this.watermediaLeftMouseWasDown_FancyMenu = leftDown;
    }

    protected boolean isMouseOverWatermediaDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaDownloadX_FancyMenu + this.watermediaDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaDownloadY_FancyMenu + this.watermediaDownloadHeight_FancyMenu));
    }

    protected boolean isMouseOverWatermediaBinariesDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaBinariesDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaBinariesDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaBinariesDownloadX_FancyMenu + this.watermediaBinariesDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaBinariesDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaBinariesDownloadY_FancyMenu + this.watermediaBinariesDownloadHeight_FancyMenu));
    }

    protected void resetWatermediaDownloadLinkBounds_FancyMenu() {
        this.watermediaDownloadX_FancyMenu = Float.NaN;
        this.watermediaDownloadY_FancyMenu = Float.NaN;
        this.watermediaDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaDownloadHeight_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadX_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadY_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    }

}
