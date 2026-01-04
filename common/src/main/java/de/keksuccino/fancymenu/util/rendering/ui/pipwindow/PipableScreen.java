package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface PipableScreen {

    /**
     * Closes this screen's parent {@link PiPWindow} and itself.
     */
    void closeWindow();

    /**
     * The {@link PiPWindow} that is currently the parent of this screen.
     */
    @Nullable PiPWindow getWindow();

    /**
     * Gets set automatically by the parent {@link PiPWindow}. This should not get set manually.
     */
    @ApiStatus.Internal
    void setWindow(@Nullable PiPWindow window);

    /**
     * Gets called when the screen gets closed in any way, which means when it gets replaced by a new screen or when the screen's parent window gets closed.
     */
    void onScreenClosed();

    /**
     * Gets called when the parent {@link PiPWindow} gets closed by anything but the screen itself.
     */
    void onWindowClosedExternally();

}
