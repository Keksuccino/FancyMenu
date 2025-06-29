package de.keksuccino.fancymenu.customization.gameintro;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class GameIntroOverlay extends Overlay {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Font font = Minecraft.getInstance().font;
    @NotNull
    protected Screen fadeTo;
    protected PlayableResource intro;
    protected float opacity = 1.0F;
    protected long start = -1;
    protected boolean fadeToInitialized = false;
    protected int width;
    protected int height;

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

        //----------------------

        if (this.start == -1) {
            this.start = System.currentTimeMillis();
            this.intro.stop();
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
            ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(this.fadeTo, graphics, mouseX, mouseY, partial));
            this.fadeTo.render(graphics, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(this.fadeTo, graphics, mouseX, mouseY, partial));
            ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
        } else {
            graphics.fill(RenderType.guiOverlay(), 0, 0, this.width, this.height, DrawableColor.BLACK.getColorInt());
        }

        this.renderIntro(graphics, mouseX, mouseY, partial);

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

            ResourceLocation location = r.getResourceLocation();
            if (location != null) {
                RenderSystem.enableBlend();
                graphics.blit(RenderType::guiTextured, location, x, y, 0.0F, 0.0F, aspectWidth, aspectHeight, aspectWidth, aspectHeight, DrawableColor.WHITE.getColorIntWithAlpha(this.opacity));
            }

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
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, scale);
            RenderSystem.enableBlend();
            int normalizedWidth = (int)(this.width / scale);
            int normalizedHeight = (int)(this.height / scale);
            int textX = (normalizedWidth / 2) - (this.font.width(skipComp) / 2);
            int textY = normalizedHeight - 40;
            graphics.drawString(this.font, skipComp, textX, textY, DrawableColor.WHITE.getColorIntWithAlpha(Math.max(0.1F, 0.6F * this.opacity)), false);
            graphics.pose().popPose();
        }
    }

    protected boolean fadeOutIntro() {
        return FancyMenu.getOptions().gameIntroFadeOut.getValue();
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

        this.fadeTo.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());

        ScrollScreenNormalizer.normalizeScrollableScreen(this.fadeTo);

        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.fadeTo, InitOrResizeScreenEvent.InitializationPhase.INIT));

        this.fadeToInitialized = true;

    }

    protected void close() {
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
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().gameIntroAllowSkip.getValue()) {
            this.close();
        }
    }

}
