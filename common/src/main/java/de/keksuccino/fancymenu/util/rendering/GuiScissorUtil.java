package de.keksuccino.fancymenu.util.rendering;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphicsExtractor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiScissorUtil {

    private GuiScissorUtil() {
    }

    @Nullable
    public static ScreenRectangle getActiveScissor(@NotNull GuiGraphicsExtractor graphics) {
        return ((IMixinGuiGraphicsExtractor)graphics).get_scissorStack_FancyMenu().peek();
    }

}
