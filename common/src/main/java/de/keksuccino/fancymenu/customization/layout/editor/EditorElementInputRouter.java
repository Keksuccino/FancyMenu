package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Takes the input of the layout editor for what elements have in it besides themselves, like the scroll bars and the handles
 * of a scrollable area ({@link AbstractEditorElement#getEditorInputListeners()}).<br>
 * It is the one listener of the editor for all of them, and asks the elements the editor has when the input comes in,
 * so nothing of an element is left in the editor when the element is gone.<br>
 * It is a {@link FancyMenuUiComponent}, so it gets a click before the editor does. A click one of the listeners takes
 * never reaches the editor, which would select or move something with it otherwise.<br>
 * Elements get asked from the last one to the first one. Children come behind their container in the order of the elements,
 * so what is in a container gets its input before the container does.
 */
public class EditorElementInputRouter implements GuiEventListener, NarratableEntry, FancyMenuUiComponent {

    @NotNull
    protected final LayoutEditorScreen editor;
    /** The listener that took the last click, which holds the mouse until its button gets released. */
    @Nullable
    protected GuiEventListener clicked;

    public EditorElementInputRouter(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.clicked = null;
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            AbstractEditorElement<?, ?> element = elements.get(i);
            if (!this.takesInput(element)) continue;
            for (GuiEventListener listener : element.getEditorInputListeners()) {
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    this.clicked = listener;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Every listener gets told, like every FancyMenu UI component of a screen does. A listener that holds on to the mouse
     * has to let go of it, also when its element stopped taking input in the meantime.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.clicked = null;
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            for (GuiEventListener listener : elements.get(i).getEditorInputListeners()) {
                listener.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    /**
     * Returns if one of the listeners took the last click and the mouse button is still held. Whatever the mouse does in that time
     * is for that listener, which follows the mouse on its own, so the editor must not move or select anything with it.
     */
    public boolean isHoldingMouse() {
        return this.clicked != null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            AbstractEditorElement<?, ?> element = elements.get(i);
            if (!this.takesInput(element)) continue;
            for (GuiEventListener listener : element.getEditorInputListeners()) {
                if (listener.mouseScrolled(mouseX, mouseY, scrollY)) return true;
            }
        }
        return false;
    }

    /**
     * Returns if the element is there for the user: not hidden as a layer, and shown by the container it is in.
     */
    protected boolean takesInput(@NotNull AbstractEditorElement<?, ?> element) {
        return !element.element.layerHiddenInEditor && element.element.isShownByContainer();
    }

    /**
     * Always FALSE: a hovered listener would take clicks away from everything in the editor.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {
    }

}
