package de.keksuccino.fancymenu.customization.gameintro;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class GameIntroOverlay extends Overlay {

    //TODO fadeTo wird von Minecraft#resize() initialisiert, wenn screen resized wird
    // - Eventuell richtiges InitEvent nicht firen, wenn overlay == GameIntroOverlay ?????

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
    protected int lastWidth;
    protected int lastHeight;

    public GameIntroOverlay(@NotNull Screen fadeTo, @NotNull PlayableResource intro) {
        super();
        this.fadeTo = Objects.requireNonNull(fadeTo);
        this.intro = Objects.requireNonNull(intro);
        this.intro.waitForReady(5000);
    }

    protected void resize() {
        if (this.endOfIntroReached()) {
            LOGGER.info("############# RESIZE");
            this.initNextScreen();
        }
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if ((this.lastWidth != this.width) || (this.lastHeight != this.height)) {
            this.resize();
        }

        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.lastWidth = this.width;
        this.lastHeight = this.height;

        //----------------------

        if (this.start == -1) {
            this.start = System.currentTimeMillis();
            this.intro.stop();
            this.intro.play();
        }

        if (this.endOfIntroReached() && !this.fadeToInitialized) {
            this.initNextScreen();
        }

        this.tickFadeOut();

        //Close screen after finished playing
        if (this.endOfIntroReached() && (this.opacity < 0.1F)) {
            this.close();
            return;
        }

        if (this.endOfIntroReached()) {
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(this.fadeTo, pose, mouseX, mouseY, partial));
            this.fadeTo.render(pose, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(this.fadeTo, pose, mouseX, mouseY, partial));
        } else {
            fill(pose, 0, 0, this.width, this.height, DrawableColor.BLACK.getColorInt());
        }

        RenderingUtils.resetShaderColor();

        this.renderAnimation(pose, mouseX, mouseY, partial);

        this.renderSkipText(pose, mouseX, mouseY, partial);

    }

    protected boolean endOfIntroReached() {
        if (this.start == -1) return false;
        long now = System.currentTimeMillis();
        //If not playing (anymore) 2 seconds after starting to play it, consider the playable resource finished
        return ((this.start + 2000) < now) && !this.intro.isPlaying();
    }

    protected void initNextScreen() {

        LOGGER.info("############################# init", new Throwable());

        boolean firstInit = !this.fadeToInitialized;

        if (firstInit) EventHandler.INSTANCE.postEvent(new OpenScreenEvent(this.fadeTo));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(this.fadeTo, firstInit ? InitOrResizeScreenEvent.InitializationPhase.INIT : InitOrResizeScreenEvent.InitializationPhase.RESIZE));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.fadeTo, firstInit ? InitOrResizeScreenEvent.InitializationPhase.INIT : InitOrResizeScreenEvent.InitializationPhase.RESIZE));

        this.fadeTo.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());

        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.fadeTo, firstInit ? InitOrResizeScreenEvent.InitializationPhase.INIT : InitOrResizeScreenEvent.InitializationPhase.RESIZE));
        EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.fadeTo, firstInit ? InitOrResizeScreenEvent.InitializationPhase.INIT : InitOrResizeScreenEvent.InitializationPhase.RESIZE));
        if (firstInit) EventHandler.INSTANCE.postEvent(new OpenScreenPostInitEvent(this.fadeTo));

        this.fadeToInitialized = true;

    }

    protected void tickFadeOut() {
        if (this.endOfIntroReached()) {
            this.opacity -= 0.02F;
        }
    }

    protected void renderAnimation(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

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
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
                RenderingUtils.bindTexture(location);
                blit(pose, x, y, 0.0F, 0.0F, aspectWidth, aspectHeight, aspectWidth, aspectHeight);
            }

            RenderingUtils.resetShaderColor();

        }

    }

    protected void renderSkipText(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (FancyMenu.getOptions().allowGameIntroSkip.getValue()) {
            float scale = 1.3F;
            String customSkipText = FancyMenu.getOptions().customGameIntroSkipText.getValue();
            if (!customSkipText.isEmpty() && LocalizationUtils.isLocalizationKey(customSkipText)) {
                customSkipText = I18n.get(customSkipText);
            }
            Component skipComp = customSkipText.isEmpty() ? Component.translatable("fancymenu.game_intro.press_any_key") : Component.literal(customSkipText);
            pose.pushPose();
            pose.scale(scale, scale, scale);
            RenderSystem.enableBlend();
            RenderingUtils.resetShaderColor();
            int normalizedWidth = (int)(this.width / scale);
            int normalizedHeight = (int)(this.height / scale);
            int textX = (normalizedWidth / 2) - (this.font.width(skipComp) / 2);
            int textY = normalizedHeight - 40;
            this.font.draw(pose, skipComp, textX, textY, RenderingUtils.replaceAlphaInColor(DrawableColor.WHITE.getColorInt(), Math.max(0.1F, 0.6F * this.opacity)));
            pose.popPose();
            RenderingUtils.resetShaderColor();
        }
    }

    public void keyPressed(int keycode, int scancode, int modifiers) {
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().allowGameIntroSkip.getValue()) {
            this.close();
        }
    }

    public void mouseClicked(int button) {
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().allowGameIntroSkip.getValue()) {
            this.close();
        }
    }

    protected void close() {
        if (!this.fadeToInitialized) this.initNextScreen();
        Minecraft.getInstance().setOverlay(null);
    }

}
