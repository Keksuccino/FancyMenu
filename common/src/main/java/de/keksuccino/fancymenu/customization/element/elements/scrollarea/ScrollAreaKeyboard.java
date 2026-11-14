package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;

/**
 * Scrolls the scrollable area the mouse is over with the keyboard: a page at a time with Page Up and Page Down,
 * and to the start and the end with Home and End.<br>
 * Screens only hand keys to the widget that has their focus, which an area is not, so this listens to the keys of all screens instead.
 */
public final class ScrollAreaKeyboard {

    @EventListener
    public void onKeyPressed(ScreenKeyPressedEvent event) {

        int key = event.getKeycode();
        if ((key != InputConstants.KEY_PAGEUP) && (key != InputConstants.KEY_PAGEDOWN) && (key != InputConstants.KEY_HOME) && (key != InputConstants.KEY_END)) return;
        // The layout editor and everything it opens use these keys for their own things
        if (LayoutEditorScreen.getCurrentInstance() != null) return;
        Screen screen = event.getScreen();
        if (screen == null) return;
        // In a text field, these keys move the cursor. Screens know the text field of a child of a container as the listener that maps its input.
        GuiEventListener focused = screen.getFocused();
        if ((focused != null) && (ElementSpaceInputListener.unwrap(focused) instanceof EditBox)) return;
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
        if (layer == null) return;

        // Children come behind their area in the order of the elements, so an area in another area gets asked first,
        // and hands the key on to the outer one when it has nowhere to scroll to anymore
        List<AbstractElement> elements = layer.allElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            if (!(elements.get(i) instanceof ScrollAreaElement area)) continue;
            if (!area.keyboardScrolling.getBoolean() || !area.isShowingChildren() || !area.isPointerOverContent()) continue;
            ScrollAreaScrolling scrolling = area.getScrolling();
            boolean scrolled = switch (key) {
                case InputConstants.KEY_PAGEUP -> scrolling.scrollByPage(false);
                case InputConstants.KEY_PAGEDOWN -> scrolling.scrollByPage(true);
                case InputConstants.KEY_HOME -> area.userScrolling.getBoolean() && scrolling.scrollToEdge(false, true);
                default -> area.userScrolling.getBoolean() && scrolling.scrollToEdge(true, true);
            };
            if (scrolled) return;
        }

    }

}
