package de.keksuccino.fancymenu.customization.screen;

import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the runtime context a constructed screen is expected to represent.
 * Screen constructors frequently use otherwise ambiguous primitive parameters, so context-sensitive providers should
 * consume this instead of relying on {@link ScreenInstanceFactory}'s legacy reflective defaults.
 */
public record ScreenConstructionContext(@Nullable Screen parentScreen, boolean inWorld) {

    @NotNull
    public static ScreenConstructionContext live() {
        return new ScreenConstructionContext(ScreenUtils.getScreen(), Minecraft.getInstance().level != null);
    }

    /**
     * Creates a best-effort preview context using the live client's world state. Call the record constructor directly
     * when the target screen's origin is known to differ from the active client state.
     */
    @NotNull
    public static ScreenConstructionContext preview(@Nullable Screen parentScreen) {
        return new ScreenConstructionContext(parentScreen, Minecraft.getInstance().level != null);
    }

    @NotNull
    public static ScreenConstructionContext postDisconnect(@Nullable Screen parentScreen) {
        return new ScreenConstructionContext(parentScreen, false);
    }

}
