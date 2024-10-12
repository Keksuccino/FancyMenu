package de.keksuccino.fancymenu.customization.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * This screen basically does nothing but execute a {@link Runnable} in its {@link Screen#render(GuiGraphics, int, int, float)} method.<br>
 * Please don't ask.
 */
public class ExecuteOnRenderScreen extends Screen {

    @NotNull
    protected Runnable action;
    protected boolean executeOnlyOnce;
    protected boolean executed = false;

    /**
     * @param action The {@link Runnable} to execute in the screen's {@link Screen#render(GuiGraphics, int, int, float)} method.
     * @param executeOnlyOnce If the action should get executed only one render tick.
     */
    protected ExecuteOnRenderScreen(@NotNull Runnable action, boolean executeOnlyOnce) {
        super(Component.empty());
        this.action = action;
        this.executeOnlyOnce = executeOnlyOnce;
    }

    @Override
    public void render(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
        if (this.executeOnlyOnce && this.executed) return;
        this.executed = true;
        this.action.run();
    }

}
