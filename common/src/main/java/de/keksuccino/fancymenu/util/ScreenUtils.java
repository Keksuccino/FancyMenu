package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ScreenUtils {

    private static int setScreenBlockDepth = 0;

    public static void blockSetScreenCalls(boolean blocked) {
        if (blocked) {
            setScreenBlockDepth++;
        } else if (setScreenBlockDepth > 0) {
            setScreenBlockDepth--;
        }
    }

    public static boolean areSetScreenCallsBlocked() {
        return setScreenBlockDepth > 0;
    }

    @Nullable
    public static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * Opens a live screen and restores the previously valid screen if synchronous initialization fails. Vanilla assigns
     * the new screen before calling its init method, so callers otherwise leave a partially initialized screen active.
     */
    public static void setScreenWithRollback(@Nullable Screen screen) {
        setScreenWithRollback(screen, ScreenUtils::getScreen, ScreenUtils::setScreen);
    }

    static <T> void setScreenWithRollback(@Nullable T screen, Supplier<T> currentScreenSupplier, Consumer<T> rawScreenSetter) {
        T previousScreen = currentScreenSupplier.get();
        try {
            rawScreenSetter.accept(screen);
        } catch (RuntimeException | Error openingFailure) {
            if (currentScreenSupplier.get() != previousScreen) {
                try {
                    // Call the raw setter so a rollback failure cannot recursively enter this recovery method.
                    rawScreenSetter.accept(previousScreen);
                } catch (RuntimeException | Error rollbackFailure) {
                    openingFailure.addSuppressed(rollbackFailure);
                }
            }
            throw openingFailure;
        }
    }

    public static int getScreenWidth() {
        Screen s = getScreen();
        return (s != null) ? s.width : 0;
    }

    public static int getScreenHeight() {
        Screen s = getScreen();
        return (s != null) ? s.height : 0;
    }

    public static int getScreenCenterX() {
        return Math.max(1, getScreenWidth()) / 2;
    }

    public static int getScreenCenterY() {
        return Math.max(1, getScreenHeight()) / 2;
    }

}
