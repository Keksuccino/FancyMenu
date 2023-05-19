package de.keksuccino.fancymenu.customization.element.elements.ticker;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.action.ActionExecutor.ActionContainer;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IActionExecutorElement;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TickerElement extends AbstractElement implements IActionExecutorElement {

    public volatile List<ActionContainer> actions = new ArrayList<>();
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

    public void tick() {

        if (this.ready && this.shouldRender()) {

            if (this.ticked && (this.tickMode == TickMode.ON_MENU_LOAD)) {
                return;
            }
            if ((this.tickMode == TickMode.ONCE_PER_SESSION) && TickerElementBuilder.cachedOncePerSessionItems.contains(this.instanceIdentifier)) {
                return;
            }
            if (this.tickMode == TickMode.ONCE_PER_SESSION) {
                TickerElementBuilder.cachedOncePerSessionItems.add(this.instanceIdentifier);
            } else {
                TickerElementBuilder.cachedOncePerSessionItems.remove(this.instanceIdentifier);
            }
            long now = System.currentTimeMillis();
            if ((this.tickDelayMs <= 0) || ((this.lastTick + this.tickDelayMs) <= now)) {
                this.lastTick = now;
                this.ticked = true;
                for (ActionContainer a : this.actions) {
                    a.execute();
                }
            }

        }

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.ready = true;

        if (isEditor()) {
            RenderSystem.enableBlend();
            fill(pose, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), Color.ORANGE.getRGB());
            drawCenteredString(pose, Minecraft.getInstance().font, "§l" + I18n.get("fancymenu.customization.items.ticker"), this.getX() + (this.getWidth() / 2), this.getY() + (this.getHeight() / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
        } else if (!this.isAsync) {
            this.tick();
        }

        //Start thread if not in editor and isAsync
        if (this.isAsync && ((this.asyncThreadController == null) || !this.asyncThreadController.running)) {
            if (!isEditor()) {
                this.asyncThreadController = new TickerElementThreadController();
                TickerElementBuilder.cachedThreadControllers.add(this.asyncThreadController);
                new Thread(() -> {
                    while ((this.asyncThreadController != null) && this.asyncThreadController.running && this.isAsync) {
                        this.tick();
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

    }

    
    @Override
    public List<ActionContainer> getActionList() {
        return this.actions;
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
