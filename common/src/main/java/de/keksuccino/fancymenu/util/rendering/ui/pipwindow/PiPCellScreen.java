package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public abstract class PiPCellScreen extends CellScreen implements PipableScreen {

    @Nullable
    private PiPWindow window;

    public PiPCellScreen(Component title) {
        super(title);
    }

    public PiPCellScreen() {
        super(Component.empty());
    }

    public void closeWindow() {
        if (this.window == null) throw new NullPointerException("The window can't be NULL when closing it.");
        this.window.setScreen(null);
        this.window.close();
    }

    public @Nullable PiPWindow getWindow() {
        return window;
    }

    @ApiStatus.Internal
    public void setWindow(@Nullable PiPWindow window) {
        this.window = window;
    }

    @Override
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public final void onClose() {
    }

    @Override
    public final void removed() {
    }

}
