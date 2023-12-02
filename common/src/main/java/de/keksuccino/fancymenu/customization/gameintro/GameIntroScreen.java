package de.keksuccino.fancymenu.customization.gameintro;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

@SuppressWarnings("all")
public class GameIntroScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Screen fadeTo;
    protected PlayableResource intro;
    protected float opacity = 1.0F;
    protected long start = -1;

    public GameIntroScreen(@NotNull Screen fadeTo, @NotNull PlayableResource intro) {
        super(Component.empty());
        this.fadeTo = Objects.requireNonNull(fadeTo);
        this.intro = Objects.requireNonNull(intro);
        this.intro.waitForReady(5000);
    }

    protected boolean endOfIntroReached() {
        if (this.start == -1) return false;
        long now = System.currentTimeMillis();
        //If not playing (anymore) 2 seconds after starting to play it, consider the playable resource finished
        return ((this.start + 2000) < now) && !this.intro.isPlaying();
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.start == -1) {
            this.start = System.currentTimeMillis();
            this.intro.stop();
            this.intro.play();
        }

        //Close screen after finished playing
        if (this.endOfIntroReached()) {
            this.onClose();
            return;
        }

        this.tickFadeOut();

        fill(pose, 0, 0, this.width, this.height, DrawableColor.BLACK.getColorInt());
        RenderingUtils.resetShaderColor();

        this.renderAnimation(pose, mouseX, mouseY, partial);

        this.renderSkipText(pose, mouseX, mouseY, partial);

    }

    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)
    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)
    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)
    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)
    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)
    //TODO APNG game intro fixen (scheint NULL zu sein, deshalb startet es nicht?? vllt wegen resource reload??)

    //TODO fix fade-out
    protected void tickFadeOut() {
//        int fps = this.animationRenderer.getFPS();
//        int framesLeft = this.animationRenderer.animationFrames() - this.animationRenderer.currentFrame();
//        int secondsLeft = (framesLeft > 0) ? Math.max(0, framesLeft / fps) : 0;
//        if (secondsLeft <= 1) {
//            this.opacity -= 1.0F / (fps * 4F);
//            if (this.opacity < 0F) this.opacity = 0F;
//        }
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

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.fadeTo);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return FancyMenu.getOptions().allowGameIntroSkip.getValue();
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().allowGameIntroSkip.getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //Handle "Press Any Key to Skip" if enabled
        if (FancyMenu.getOptions().allowGameIntroSkip.getValue()) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}
