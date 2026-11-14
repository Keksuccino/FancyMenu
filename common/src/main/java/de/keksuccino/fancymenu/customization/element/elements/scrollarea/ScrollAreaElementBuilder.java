package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScrollAreaElementBuilder extends ElementBuilder<ScrollAreaElement, ScrollAreaEditorElement> {

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 120;
    private static final ScrollAreaKeyboard KEYBOARD = new ScrollAreaKeyboard();

    static {
        EventHandler.INSTANCE.registerListenersOf(KEYBOARD);
        // Every release of the mouse comes in here, also the ones a screen keeps to itself, so areas always find out that the mouse let go of their content
        MouseUtil.addReleaseListener((button, mouseX, mouseY) -> {
            if (button == MouseUtil.MouseButton.LEFT) ScrollAreaScrolling.onLeftMouseReleasedAnywhere();
        });
        // The same goes for presses, which something in an area can take before the area hears of them
        MouseUtil.addClickListener((button, mouseX, mouseY) -> ScrollAreaScrolling.onMousePressedAnywhere());
    }

    public ScrollAreaElementBuilder() {
        super("scroll_area");
    }

    @Override
    public @NotNull ScrollAreaElement buildDefaultInstance() {
        ScrollAreaElement element = new ScrollAreaElement(this);
        element.baseWidth = DEFAULT_WIDTH;
        element.baseHeight = DEFAULT_HEIGHT;
        // A new area has something to scroll right away, which shows what it is for
        element.contentHeight.set(DEFAULT_HEIGHT * 2);
        element.background.applyDefaultsOfNewElements();
        return element;
    }

    @Override
    public ScrollAreaElement deserializeElement(@NotNull SerializedElement serialized) {
        // Everything the area saves is a property, which elements load on their own
        return new ScrollAreaElement(this);
    }

    @Override
    protected SerializedElement serializeElement(@NotNull ScrollAreaElement element, @NotNull SerializedElement serializeTo) {
        return serializeTo;
    }

    @Override
    public @NotNull ScrollAreaEditorElement wrapIntoEditorElement(@NotNull ScrollAreaElement element, @NotNull LayoutEditorScreen editor) {
        return new ScrollAreaEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.scroll_area");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.desc");
    }

}
