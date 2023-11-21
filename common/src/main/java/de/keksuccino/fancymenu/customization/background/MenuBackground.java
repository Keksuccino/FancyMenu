package de.keksuccino.fancymenu.customization.background;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MenuBackground extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

    public final MenuBackgroundBuilder<?> builder;
    /** This gets set by the {@link ScreenCustomizationLayer} when screens fade in or out and should only get used as getter. **/
    public float opacity = 1.0F;
    /** This gets set by the {@link ScreenCustomizationLayer} and should only be used as getter. **/
    public boolean keepBackgroundAspectRatio = false;

    public MenuBackground(MenuBackgroundBuilder<?> builder) {
        this.builder = builder;
    }

    @Override
    public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

    /** Gets called every {@link Screen} tick, after {@link Screen#tick()} got called. **/
    public void tick() {
    }

    /**
     * Gets called before a {@link Screen} gets closed.<br>
     * A screen gets closed when a new {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.
     */
    public void onCloseScreen() {
    }

    /**
     * Gets called after a {@link Screen} got opened via {@link Minecraft#setScreen(Screen)}.<br>
     * The {@link Screen} is already initialized at the time this method gets called.
     */
    public void onOpenScreen() {
    }

    /**
     * Gets called before the current {@link Screen} gets resized.<br>
     * Does NOT get called on initial resize (when opening the screen). Use {@link MenuBackground#onOpenScreen()} for that instead.
     */
    public void onBeforeResizeScreen() {
    }

    /**
     * Gets called after the current {@link Screen} got resized.<br>
     * Does NOT get called on initial resize (when opening the screen). Use {@link MenuBackground#onOpenScreen()} for that instead.
     */
    public void onAfterResizeScreen() {
    }

    @Nullable
    public MenuBackground copy() {
        try {
            return this.builder.deserializeBackgroundInternal(this.builder.serializedBackgroundInternal(this));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isEditor() {
        return (Minecraft.getInstance().screen instanceof LayoutEditorScreen);
    }

    public static int getScreenWidth() {
        return AbstractElement.getScreenWidth();
    }

    public static int getScreenHeight() {
        return AbstractElement.getScreenHeight();
    }

    @Override
    public void setFocused(boolean var1) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("MenuBackgrounds are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("MenuBackgrounds are not navigatable!");
    }

}
