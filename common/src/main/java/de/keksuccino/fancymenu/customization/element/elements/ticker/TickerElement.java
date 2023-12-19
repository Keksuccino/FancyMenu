package de.keksuccino.fancymenu.customization.element.elements.ticker;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

public class TickerElement extends AbstractElement implements ExecutableElement {

    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(Color.ORANGE);

    @NotNull
    public volatile GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    public volatile long tickDelayMs = 0;
    public volatile boolean isAsync = false;
    public volatile TickMode tickMode = TickMode.NORMAL;
    protected volatile boolean ready = false;
    protected volatile boolean ticked = false;
    protected volatile long lastTick = -1;
    protected volatile TickerElementThreadController asyncThreadController = null;

    public TickerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    protected void tickerElementTick() {
        if (this.ready && this.shouldRender()) {
            if (this.ticked && (this.tickMode == TickMode.ON_MENU_LOAD)) {
                return;
            }
            if ((this.tickMode == TickMode.ONCE_PER_SESSION) && TickerElementBuilder.cachedOncePerSessionItems.contains(this.getInstanceIdentifier())) {
                return;
            }
            if (this.tickMode == TickMode.ONCE_PER_SESSION) {
                TickerElementBuilder.cachedOncePerSessionItems.add(this.getInstanceIdentifier());
            } else {
                TickerElementBuilder.cachedOncePerSessionItems.remove(this.getInstanceIdentifier());
            }
            long now = System.currentTimeMillis();
            if ((this.tickDelayMs <= 0) || ((this.lastTick + this.tickDelayMs) <= now)) {
                this.lastTick = now;
                this.ticked = true;
                this.actionExecutor.execute();
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.ready = true;

        if (isEditor()) {
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            RenderSystem.enableBlend();
            graphics.fill(x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
            graphics.enableScissor(x, y, x + w, y + h);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
            graphics.disableScissor();
            RenderingUtils.resetShaderColor(graphics);
        } else if (!this.isAsync) {
            this.tickerElementTick();
        }

        //Start thread if not in editor and isAsync
        if (this.isAsync && ((this.asyncThreadController == null) || !this.asyncThreadController.running)) {
            if (!isEditor()) {
                this.asyncThreadController = new TickerElementThreadController();
                TickerElementBuilder.cachedThreadControllers.add(this.asyncThreadController);
                new Thread(() -> {
                    while ((this.asyncThreadController != null) && this.asyncThreadController.running && this.isAsync) {
                        this.tickerElementTick();
                        try {
                            //Sleep 50ms to tick 20 times per second (like normal MC menus)
                            Thread.sleep(50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }

        //Stop thread if !isAsync
        if (!this.isAsync && (this.asyncThreadController != null)) {
            this.asyncThreadController.running = false;
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    
    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

    public static class TickerElementThreadController {

        public volatile boolean running = true;

    }

    public enum TickMode {

        NORMAL("normal"),
        ONCE_PER_SESSION("once_per_session"),
        ON_MENU_LOAD("on_menu_load");

        public final String name;

        TickMode(String name) {
            this.name = name;
        }

        @Nullable
        public static TickMode getByName(String name) {
            for (TickMode t : TickMode.values()) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
            return null;
        }

    }

}
