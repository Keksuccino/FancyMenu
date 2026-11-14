package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves elements into and out of containers when the user drags them in the layout editor.<br>
 * An element that gets dropped on a container becomes its child, and a child that gets dropped anywhere else
 * becomes an element of what it got dropped on: another container or the main layout.
 * While the user drags, the container that would take the elements is marked.
 */
public class ContainerDragDropHandler {

    private static final int DROP_TARGET_FILL_ALPHA = 40;
    /** How long the mark of an element that can't go into a container stays after the last frame it got set in. Longer than a frame, so it never flickers. */
    private static final long NOT_ALLOWED_MARK_MS = 150L;

    @NotNull
    protected final LayoutEditorScreen editor;
    protected final List<AbstractEditorElement<?, ?>> refused = new ArrayList<>();

    public ContainerDragDropHandler(@NotNull LayoutEditorScreen editor) {
        this.editor = editor;
    }

    /**
     * Marks the container the dragged elements would go into if the user dropped them right now, and the dragged elements it would not take.
     */
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        List<AbstractEditorElement<?, ?>> dragged = this.editor.getCurrentlyDraggedElements();
        if (!this.isMoving(dragged)) return;

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);
        if (target == null) return;
        ContainerEditorElement container = (ContainerEditorElement) target;

        this.refused.clear();
        boolean takesAny = false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (this.movesWithDraggedContainer(e, dragged)) continue;
            if (container.canHoldChild(e)) {
                if (e.element.getParentContainer() != target.element) takesAny = true;
            } else {
                this.refused.add(e);
            }
        }

        if (!this.refused.isEmpty()) {
            long until = System.currentTimeMillis() + NOT_ALLOWED_MARK_MS;
            for (AbstractEditorElement<?, ?> e : this.refused) {
                e.renderMovingNotAllowedTime = Math.max(e.renderMovingNotAllowedTime, until);
            }
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.containers.drop.not_allowed")), () -> true);
        }

        if (takesAny) {
            int x = container.getDropAreaX();
            int y = container.getDropAreaY();
            int width = container.getDropAreaWidth();
            int height = container.getDropAreaHeight();
            int color = UIBase.getUITheme().layout_editor_element_border_color_selected.getColorInt();
            graphics.fill(x, y, x + width, y + height, RenderingUtils.replaceAlphaInColor(color, DROP_TARGET_FILL_ALPHA));
            UIBase.renderBorder(graphics, x, y, x + width, y + height, 1, UIBase.getUITheme().layout_editor_element_border_color_selected.getColor(), true, true, true, true);
            RenderingUtils.resetShaderColor(graphics);
        }

    }

    /**
     * Puts the elements the user just dropped into what they got dropped on.
     *
     * @param dragged The elements the user dragged until now.
     */
    public void onElementsDropped(@NotNull List<AbstractEditorElement<?, ?>> dragged, int mouseX, int mouseY) {

        if (!this.isMoving(dragged)) return;

        AbstractEditorElement<?, ?> target = this.findDropTarget(dragged, mouseX, mouseY);
        ElementContainer targetContainer = (target != null) ? (ElementContainer) target.element : null;

        List<AbstractEditorElement<?, ?>> moved = new ArrayList<>();
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (this.movesWithDraggedContainer(e, dragged)) continue;
            if (e.element.getParentContainer() == targetContainer) continue;
            if ((target != null) && !((ContainerEditorElement) target).canHoldChild(e)) continue;
            e.moveToContainer(targetContainer);
            moved.add(e);
        }
        if (moved.isEmpty()) return;

        // What got dropped lies on top of what it got dropped on, and the hierarchy puts it behind its new container from there
        this.editor.normalEditorElements.removeAll(moved);
        this.editor.normalEditorElements.addAll(moved);
        this.editor.updateContainerHierarchy();
        this.editor.updateLayerGroupElementOrder();
        for (AbstractLayoutEditorWidget widget : this.editor.layoutEditorWidgets) {
            widget.refresh();
        }

    }

    /**
     * Returns the container the mouse is over that could take the dragged elements, or NULL if they would go into the main layout.<br>
     * Children come behind their container in the order of the elements, so a container in another container gets found before the outer one.
     */
    @Nullable
    protected AbstractEditorElement<?, ?> findDropTarget(@NotNull List<AbstractEditorElement<?, ?>> dragged, int mouseX, int mouseY) {
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        for (int i = elements.size() - 1; i >= 0; i--) {
            AbstractEditorElement<?, ?> e = elements.get(i);
            if (!(e instanceof ContainerEditorElement) || !(e.element instanceof ElementContainer container)) continue;
            if (e.element.layerHiddenInEditor) continue;
            if (dragged.contains(e) || this.movesWithDraggedContainer(e, dragged)) continue;
            if (container.isShowingChildren() && container.isScreenPointVisible(mouseX, mouseY)) return e;
        }
        return null;
    }

    /**
     * Returns if the element is in a container the user drags along with it. Such an element moves with its container and stays in it.
     */
    public boolean movesWithDraggedContainer(@NotNull AbstractEditorElement<?, ?> element, @NotNull List<AbstractEditorElement<?, ?>> dragged) {
        if (element.element.getParentContainer() == null) return false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if ((e != element) && (e.element instanceof ElementContainer container) && ElementContainerHierarchy.isInContainer(element.element, container)) return true;
        }
        return false;
    }

    /**
     * Returns if the user moves the elements around, which is the only kind of dragging that drops them somewhere.
     */
    protected boolean isMoving(@NotNull List<AbstractEditorElement<?, ?>> dragged) {
        if (dragged.isEmpty()) return false;
        for (AbstractEditorElement<?, ?> e : dragged) {
            if (e.isGettingResized() || e.isGettingRotated() || e.isGettingTilted()) return false;
        }
        return true;
    }

}
