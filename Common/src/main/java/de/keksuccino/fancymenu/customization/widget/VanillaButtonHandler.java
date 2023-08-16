package de.keksuccino.fancymenu.customization.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.events.widget.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.widget.RenderWidgetBackgroundEvent;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class VanillaButtonHandler {

    private static final Map<AbstractWidget, ResourceLocation> BACKGROUND_TEXTURES = new HashMap<>();
    private static final Map<AbstractWidget, VanillaBackgroundAnimation> BACKGROUND_ANIMATIONS = new HashMap<>();
    private static final Map<AbstractWidget, String> CLICK_SOUNDS = new HashMap<>();

    public static void init() {
        EventHandler.INSTANCE.registerListenersOf(new VanillaButtonHandler());
    }

    public static void setRenderTickBackgroundTexture(AbstractWidget widget, ResourceLocation background) {
        if (background != null) {
            BACKGROUND_TEXTURES.put(widget, background);
        } else {
            BACKGROUND_TEXTURES.remove(widget);
        }
    }

    public static void setRenderTickBackgroundAnimation(AbstractWidget widget, IAnimationRenderer background, boolean loop, float opacity) {
        if (background != null) {
            BACKGROUND_ANIMATIONS.put(widget, new VanillaBackgroundAnimation(background, loop, opacity));
        } else {
            BACKGROUND_ANIMATIONS.remove(widget);
        }
    }

    public static void setRenderTickClickSound(AbstractWidget widget, String clickSound) {
        if (clickSound != null) {
            CLICK_SOUNDS.put(widget, clickSound);
        } else {
            CLICK_SOUNDS.remove(widget);
        }
    }

    @EventListener
    public void onButtonClickSoundPre(PlayWidgetClickSoundEvent.Pre e) {

        String clickSound = CLICK_SOUNDS.get(e.getWidget());

        if (clickSound != null) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(clickSound));
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
                SoundHandler.registerSound(f.getPath(), f.getPath());
                SoundHandler.resetSound(f.getPath());
                SoundHandler.playSound(f.getPath());
                e.setCanceled(true);
            }
        }

    }

    @EventListener
    public void onRenderButtonBackgroundPre(RenderWidgetBackgroundEvent.Pre e) {

        AbstractWidget w = e.getWidget();
        ResourceLocation backgroundTexture = BACKGROUND_TEXTURES.get(w);
        VanillaBackgroundAnimation backgroundAnimation = BACKGROUND_ANIMATIONS.get(w);

        RenderSystem.enableBlend();

        if (backgroundTexture != null) {
            RenderUtils.bindTexture(backgroundTexture);
            GuiComponent.blit(e.getPoseStack(), w.getX(), w.getY(), 0.0F, 0.0F, w.getWidth(), w.getHeight(), w.getWidth(), w.getHeight());
        }
        if ((backgroundTexture == null) && (backgroundAnimation != null)) {
            boolean loop = backgroundAnimation.animationRenderer.isGettingLooped();
            int aw = backgroundAnimation.animationRenderer.getWidth();
            int ah = backgroundAnimation.animationRenderer.getHeight();
            int ax = backgroundAnimation.animationRenderer.getPosX();
            int ay = backgroundAnimation.animationRenderer.getPosY();
            backgroundAnimation.animationRenderer.setWidth(w.getWidth());
            backgroundAnimation.animationRenderer.setHeight(w.getHeight());
            backgroundAnimation.animationRenderer.setPosX(w.x);
            backgroundAnimation.animationRenderer.setPosY(w.y);
            backgroundAnimation.animationRenderer.setOpacity(backgroundAnimation.opacity);
            backgroundAnimation.animationRenderer.setLooped(backgroundAnimation.loop);
            backgroundAnimation.animationRenderer.render(e.getPoseStack());
            backgroundAnimation.animationRenderer.setWidth(aw);
            backgroundAnimation.animationRenderer.setHeight(ah);
            backgroundAnimation.animationRenderer.setPosX(ax);
            backgroundAnimation.animationRenderer.setPosY(ay);
            backgroundAnimation.animationRenderer.setOpacity(1.0F);
            backgroundAnimation.animationRenderer.setLooped(loop);
        }

        if ((backgroundTexture != null) || (backgroundAnimation != null)) {
            if (w instanceof ImageButton) {
                Component msg = w.getMessage();
                int j = w.active ? 16777215 : 10526880;
                GuiComponent.drawCenteredString(e.getPoseStack(), Minecraft.getInstance().font, msg, w.x + w.getWidth() / 2, w.y + (w.getHeight() - 8) / 2, j | Mth.ceil(e.getAlpha() * 255.0F) << 24);
            }
            e.setCanceled(true);
        }

    }

    @EventListener(priority = -100)
    public void onRenderScreenPost(RenderScreenEvent.Post e) {
        BACKGROUND_TEXTURES.clear();
        BACKGROUND_ANIMATIONS.clear();
        CLICK_SOUNDS.clear();
    }

    protected static class VanillaBackgroundAnimation {

        protected IAnimationRenderer animationRenderer;
        protected boolean loop;
        protected float opacity;

        protected VanillaBackgroundAnimation(IAnimationRenderer animationRenderer, boolean loop, float opacity) {
            this.animationRenderer = animationRenderer;
            this.loop = loop;
            this.opacity = opacity;
        }

    }

}
