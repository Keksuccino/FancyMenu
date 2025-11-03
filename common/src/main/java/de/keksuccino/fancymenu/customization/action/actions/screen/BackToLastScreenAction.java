package de.keksuccino.fancymenu.customization.action.actions.screen;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.screen.CloseScreenEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BackToLastScreenAction extends Action {

    @Nullable
    protected Screen lastScreen = null;

    public BackToLastScreenAction() {
        super("back_to_last_screen");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(@Nullable String value) {
        if (Minecraft.getInstance().screen instanceof CustomGuiBaseScreen c) {
            if (c.getParentScreen() != null) {
                Minecraft.getInstance().setScreen(c.getParentScreen());
                return;
            }
        }
        if (this.lastScreen instanceof LayoutEditorScreen) {
            this.lastScreen = null;
        }
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.back_to_last_screen");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.back_to_last_screen.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValueExample() {
        return null;
    }

    @EventListener
    public void onCloseScreen(CloseScreenEvent e) {
        if (e.getNewScreen() instanceof LayoutEditorScreen) return;
        if (this.lastScreen == e.getNewScreen()) return;
        this.lastScreen = e.getClosedScreen();
        if (e.getNewScreen() == null) this.lastScreen = null;
    }

}
