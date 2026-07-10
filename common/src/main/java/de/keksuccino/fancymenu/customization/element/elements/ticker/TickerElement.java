package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TickerElement extends AbstractElement implements ExecutableElement {

    @NotNull
    public volatile GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    public final Property.LongProperty tickDelayMs = putProperty(Property.longProperty("tick_delay", 0L, "fancymenu.elements.ticker.tick_delay"));
    public volatile boolean isAsync = false;
    public volatile TickMode tickMode = TickMode.NORMAL;
    protected volatile boolean ready = false;
    protected volatile boolean ticked = false;
    protected volatile long lastTick = -1;
    /**
     * A zero-delay NORMAL ticker that synchronously replaced its own screen must stay suspended for the lifetime of
     * the replacement. Clearing this after one tick would only reduce the replacement loop to every second frame.
     * Genuine later initialization creates a fresh ticker and intentionally restores the configured behavior.
     */
    protected volatile boolean suspendedAfterImmediateSameScreenReplacement = false;
    protected volatile TickerElementThreadController asyncThreadController = null;

    public TickerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    protected void tickerElementTick() {
        if (this.ready && this.shouldRender()) {
            if (this.suspendedAfterImmediateSameScreenReplacement) {
                return;
            }
            if (this.ticked && (this.tickMode == TickMode.ON_MENU_LOAD)) {
                return;
            }
            if (this.tickMode == TickMode.ONCE_PER_SESSION) {
                if (!TickerElementBuilder.tryMarkOncePerSessionItem(this.getInstanceIdentifier())) {
                    return;
                }
            } else {
                TickerElementBuilder.removeOncePerSessionItem(this.getInstanceIdentifier());
            }
            long now = System.currentTimeMillis();
            long delayMs = Math.max(0L, this.tickDelayMs.getLong());
            if ((delayMs <= 0) || ((this.lastTick + delayMs) <= now)) {
                this.lastTick = now;
                this.ticked = true;
                Screen sourceScreen = ScreenUtils.getScreen();
                String sourceScreenIdentifier = (sourceScreen != null) ? ScreenIdentifierHandler.getIdentifierOfScreen(sourceScreen) : null;
                ScreenCustomizationLayer sourceLayer = (sourceScreen != null) ? ScreenCustomizationLayerHandler.getLayerOfScreen(sourceScreen) : null;
                try (TickerRuntimeStateTransfer.ExecutionScope ignored = TickerRuntimeStateTransfer.begin(sourceScreenIdentifier, sourceLayer, this)) {
                    this.actionExecutor.execute();
                }
            }
        }
    }

    @Nullable
    TickerRuntimeStateTransfer.RuntimeKey createRuntimeStateKey() {
        if (this.getParentLayout() == null) {
            return null;
        }
        String rawTickDelay = Objects.requireNonNullElse(this.tickDelayMs.getRawInputOrFormattedValue(), "");
        return new TickerRuntimeStateTransfer.RuntimeKey(this.getParentLayout().runtimeLayoutIdentifier, this.getInstanceIdentifier(), rawTickDelay, this.tickMode.name, this.isAsync, this.actionExecutor.identifier);
    }

    @NotNull
    TickerRuntimeStateTransfer.RuntimeState createRuntimeState(boolean executionSource) {
        boolean suspend = this.suspendedAfterImmediateSameScreenReplacement || (executionSource && this.isImmediateNormalExecutionSource());
        return new TickerRuntimeStateTransfer.RuntimeState(this.ticked, this.lastTick, suspend);
    }

    boolean isImmediateNormalExecutionSource() {
        return (this.tickMode == TickMode.NORMAL) && (Math.max(0L, this.tickDelayMs.getLong()) == 0L);
    }

    void suspendAfterImmediateSameScreenReplacement() {
        this.suspendedAfterImmediateSameScreenReplacement = true;
        if (this.asyncThreadController != null) {
            this.asyncThreadController.running = false;
        }
    }

    boolean shouldStartAsyncThread() {
        return this.isAsync && !this.suspendedAfterImmediateSameScreenReplacement && ((this.asyncThreadController == null) || !this.asyncThreadController.running);
    }

    void restoreRuntimeState(@NotNull Object targetScreen) {
        TickerRuntimeStateTransfer.RuntimeKey key = this.createRuntimeStateKey();
        if (key == null) {
            return;
        }
        TickerRuntimeStateTransfer.RuntimeState state = TickerRuntimeStateTransfer.take(targetScreen, key);
        if (state != null) {
            this.ticked = state.ticked();
            this.lastTick = state.lastTick();
            this.suspendedAfterImmediateSameScreenReplacement = state.suspended();
            if (state.suspended()) {
                if (this.asyncThreadController != null) {
                    this.asyncThreadController.running = false;
                }
            }
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.ready = true;

        if (isEditor()) {
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            graphics.fill(x, y, x + w, y + h, this.inEditorColor.getDrawable().getColorInt());
            graphics.enableScissor(x, y, x + w, y + h);
            graphics.centeredText(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
            graphics.disableScissor();
            RenderingUtils.resetShaderColor(graphics);
        } else if (!this.isAsync) {
            this.tickerElementTick();
        }

        //Start thread if not in editor and isAsync
        if (this.shouldStartAsyncThread()) {
            if (!isEditor()) {
                TickerElementThreadController controller = new TickerElementThreadController();
                this.asyncThreadController = controller;
                TickerElementBuilder.cachedThreadControllers.add(controller);
                FancyMenuThreads.startDaemonThread(() -> {
                    while (controller.running && this.isAsync) {
                        this.tickerElementTick();
                        try {
                            //Sleep 50ms to tick 20 times per second (like normal MC menus)
                            Thread.sleep(50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, "TickerElement-AsyncTicker");
            }
        }

        //Stop thread if !isAsync
        if (!this.isAsync && (this.asyncThreadController != null)) {
            this.asyncThreadController.running = false;
        }

        de.keksuccino.fancymenu.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, 1.0F);

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
