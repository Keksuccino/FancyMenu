package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiScissorUtil {

    private GuiScissorUtil() {
    }

    @Nullable
    public static ScreenRectangle getActiveScissor(@NotNull GuiGraphics graphics) {
        return ((IMixinGuiGraphics)graphics).get_scissorStack_FancyMenu().peek();
    }

}
