package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

//TODO übernehmen
@SuppressWarnings("all")
public class RenderScreenEvent extends EventBase {

    private Screen screen;
    private GuiGraphics graphics;
    private int mouseX;
    private int mouseY;
    private float renderTicks;

    public RenderScreenEvent(@NotNull Screen screen, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float renderPartialTicks) {
        this.screen = Objects.requireNonNull(screen);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.renderTicks = renderPartialTicks;
        this.graphics = Objects.requireNonNull(graphics);
    }

    @Override
    public void setCanceled(boolean b) {
        throw new RuntimeException("[FANCYMENU] Tried to cancel non-cancelable RenderScreenEvent!");
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Deprecated
    public Screen getGui() {
        return this.screen;
    }

    public GuiGraphics getGuiGraphics() {
        return this.graphics;
    }

    public PoseStack getPoseStack() {
        return this.graphics.pose();
    }

    @Deprecated
    public PoseStack getMatrixStack() {
        return this.graphics.pose();
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public float getRenderPartialTicks() {
        return this.renderTicks;
    }

    public static class Pre extends RenderScreenEvent {

        public Pre(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float renderPartialTicks) {
            super(screen, graphics, mouseX, mouseY, renderPartialTicks);
        }

    }

    public static class Post extends RenderScreenEvent {

        public Post(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float renderPartialTicks) {
            super(screen, graphics, mouseX, mouseY, renderPartialTicks);
        }

    }

}
