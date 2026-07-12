package de.keksuccino.fancymenu.customization.gameintro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Starts the configured startup intro while leaving consumption to the overlay lifecycle.
 *
 * <p>A resource reload queued during initial loading can immediately displace the created overlay. The intro must
 * remain unconsumed until playback finishes or is skipped so the final loading overlay can retry it.</p>
 */
public final class GameIntroStartupController {

    private GameIntroStartupController() {}

    /**
     * Attempts to replace the completed loading overlay with the configured intro.
     *
     * @return whether an intro overlay was installed and the wrapped screen initialization must be deferred
     */
    public static boolean tryStartIntro(@NotNull Screen fadeTo) {
        return tryStartIntro(GameIntroHandler.introPlayed, GameIntroHandler::getIntro, intro -> Minecraft.getInstance().setOverlay(new GameIntroOverlay(fadeTo, intro)));
    }

    static <T> boolean tryStartIntro(boolean introPlayed, Supplier<? extends T> introSupplier, Consumer<? super T> introStarter) {
        if (introPlayed) return false;
        T intro = introSupplier.get();
        if (intro == null) return false;
        introStarter.accept(intro);
        return true;
    }

}
