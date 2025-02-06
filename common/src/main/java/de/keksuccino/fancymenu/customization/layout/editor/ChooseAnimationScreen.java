package de.keksuccino.fancymenu.customization.layout.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
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
        super(Components.translatable("fancymenu.animation.choose"));
    }

    @Override
    protected void init() {}

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {}

    protected void setSelectedAnimation(@Nullable AnimationScrollEntry entry) {}

    protected void updateAnimationScrollAreaContent() {}

    public static class AnimationScrollEntry extends TextListScrollAreaEntry {

        public String animation;

        public AnimationScrollEntry(ScrollArea parent, @NotNull String animation, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Components.literal(animation).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.animation = animation;
        }

    }

}
