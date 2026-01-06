package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ChooseAnimationScreen extends Screen {

    protected Consumer<String> callback;
    protected String selectedAnimationName = null;
    protected IAnimationRenderer selectedAnimation = null;

    protected ScrollArea animationListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ChooseAnimationScreen(@Nullable String preSelectedAnimation, @NotNull Consumer<String> callback) {
        super(Component.translatable("fancymenu.animation.choose"));
    }

    @Override
    protected void init() {}

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {}

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {}

    protected void setSelectedAnimation(@Nullable AnimationScrollEntry entry) {}

    protected void updateAnimationScrollAreaContent() {}

    public static class AnimationScrollEntry extends TextListScrollAreaEntry {

        public String animation;

        public AnimationScrollEntry(ScrollArea parent, @NotNull String animation, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(animation).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), UIBase.getUIColorTheme().listing_dot_color_1, onClick);
            this.animation = animation;
        }

    }

}
