package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaAlignment;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.layout.ScrollAreaLayoutMode;
import de.keksuccino.fancymenu.customization.element.style.ElementStyleMenus;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollDirection;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollGeometry;
import de.keksuccino.fancymenu.customization.element.scrolling.ScrollSettingsMenus;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScrollAreaEditorElement extends AbstractEditorElement<ScrollAreaEditorElement, ScrollAreaElement> implements ContainerEditorElement {

    private static final int DROP_MARKER_THICKNESS = 2;
    private static final int DROP_MARKER_INSET = 2;

    /** The children of the area as what they are in the editor, by the element they wrap. */
    protected final Map<AbstractElement, AbstractEditorElement<?, ?>> childEditorElements = new IdentityHashMap<>();
    /** The same children, in the order they render in. */
    protected final List<AbstractEditorElement<?, ?>> orderedChildEditorElements = new ArrayList<>();
    protected final ScrollAreaEmptyGuide emptyGuide = new ScrollAreaEmptyGuide();
    protected final ScrollAreaContentEndGrabber contentWidthGrabber = new ScrollAreaContentEndGrabber(this, true);
    protected final ScrollAreaContentEndGrabber contentHeightGrabber = new ScrollAreaContentEndGrabber(this, false);
    /** The handles come first: they lie on top of what the area shows, where a click would start dragging the content otherwise. */
    protected final List<GuiEventListener> editorInputListeners;

    public ScrollAreaEditorElement(@NotNull ScrollAreaElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setParallaxAllowed(false);
        this.element.setChildRenderer(this::renderChild);
        this.element.setEmptyContentRenderer(this.emptyGuide::render);
        this.editorInputListeners = List.of(this.contentWidthGrabber, this.contentHeightGrabber, this.element.getScrollInput());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        if ((this.renderPass != RenderPass.ELEMENT) && !this.element.layerHiddenInEditor) {
            this.contentWidthGrabber.render(graphics, mouseX, mouseY, partial);
            this.contentHeightGrabber.render(graphics, mouseX, mouseY, partial);
        }

    }

    @Override
    public @NotNull List<GuiEventListener> getEditorInputListeners() {
        return this.editorInputListeners;
    }

    @Override
    public void setChildEditorElements(@NotNull List<AbstractEditorElement<?, ?>> children) {
        this.childEditorElements.clear();
        this.orderedChildEditorElements.clear();
        this.orderedChildEditorElements.addAll(children);
        for (AbstractEditorElement<?, ?> child : children) {
            this.childEditorElements.put(child.element, child);
        }
    }

    @Override
    public @NotNull List<AbstractEditorElement<?, ?>> getChildEditorElements() {
        return this.orderedChildEditorElements;
    }

    /**
     * Arranged children read like text: along the line first, then line by line. Dropped elements go in front of the first child
     * the mouse is in front of in that order. A stack is one line along the direction the area scrolls in.
     */
    @Override
    public int getDropIndex(int mouseX, int mouseY, @NotNull List<AbstractEditorElement<?, ?>> dragged) {
        if (!this.element.arrangesChildren()) return -1;
        boolean horizontal = this.element.getScrollDirection() == ScrollDirection.HORIZONTAL;
        boolean oneLine = this.element.getLayoutMode() == ScrollAreaLayoutMode.STACK;
        int mouseMain = horizontal ? mouseX : mouseY;
        int mouseCross = horizontal ? mouseY : mouseX;
        int index = 0;
        for (int i = 0; i < this.orderedChildEditorElements.size(); i++) {
            AbstractEditorElement<?, ?> child = this.orderedChildEditorElements.get(i);
            if (dragged.contains(child)) continue;
            // Children that do not show have no place the mouse could be in front of
            if (!child.element.layerHiddenInEditor) {
                int mainStart = horizontal ? child.getX() : child.getY();
                int mainSize = horizontal ? child.getWidth() : child.getHeight();
                int crossStart = horizontal ? child.getY() : child.getX();
                int crossSize = horizontal ? child.getHeight() : child.getWidth();
                boolean mouseInFront = oneLine ? (mouseMain < (mainStart + (mainSize / 2))) : ((mouseMain < mainStart) || ((mouseMain < (mainStart + mainSize)) && (mouseCross < (crossStart + (crossSize / 2)))));
                if (mouseInFront) return index;
            }
            index++;
        }
        return index;
    }

    @Override
    public boolean getDropMarker(int dropIndex, @NotNull List<AbstractEditorElement<?, ?>> dragged, int @NotNull [] marker) {

        // The line sits in front of the child at the place, or behind the last child that shows
        AbstractEditorElement<?, ?> reference = null;
        boolean behindReference = true;
        int index = 0;
        for (int i = 0; i < this.orderedChildEditorElements.size(); i++) {
            AbstractEditorElement<?, ?> child = this.orderedChildEditorElements.get(i);
            if (dragged.contains(child)) continue;
            if (!child.element.layerHiddenInEditor) {
                reference = child;
                if (index >= dropIndex) {
                    behindReference = false;
                    break;
                }
            }
            index++;
        }
        if (reference == null) return false;

        ScrollGeometry geometry = this.element.getScrollGeometry();
        boolean horizontal = this.element.getScrollDirection() == ScrollDirection.HORIZONTAL;
        boolean oneLine = this.element.getLayoutMode() == ScrollAreaLayoutMode.STACK;
        // Half of the room between two children, so the line sits in the middle of it
        int gap = Math.round(this.element.childSpacing.getInteger() / 2.0F) + (DROP_MARKER_THICKNESS / 2);
        // A stack gets a line across the whole area. Everything else gets one next to the child, where the dropped elements would go.
        boolean lineRunsAcrossX = oneLine != horizontal;
        if (lineRunsAcrossX) {
            int y = behindReference ? (reference.getY() + reference.getHeight() + gap - DROP_MARKER_THICKNESS) : (reference.getY() - gap);
            marker[0] = oneLine ? (geometry.getViewportX() + DROP_MARKER_INSET) : reference.getX();
            marker[1] = y;
            marker[2] = oneLine ? (geometry.getViewportWidth() - (DROP_MARKER_INSET * 2)) : reference.getWidth();
            marker[3] = DROP_MARKER_THICKNESS;
        } else {
            int x = behindReference ? (reference.getX() + reference.getWidth() + gap - DROP_MARKER_THICKNESS) : (reference.getX() - gap);
            marker[0] = x;
            marker[1] = oneLine ? (geometry.getViewportY() + DROP_MARKER_INSET) : reference.getY();
            marker[2] = DROP_MARKER_THICKNESS;
            marker[3] = oneLine ? (geometry.getViewportHeight() - (DROP_MARKER_INSET * 2)) : reference.getHeight();
        }
        return true;

    }

    /**
     * Changes how the area places its children, in a way that leaves the children where they are as far as the new mode allows it:<br>
     * - A mode that leaves the place of children to the user puts every child where it was on the screen.<br>
     * - A mode that arranges the children puts them in the order they were in on the screen.
     */
    public void changeLayoutMode(@NotNull ScrollAreaLayoutMode mode) {

        ScrollAreaElement area = this.element;
        ScrollAreaLayoutMode oldMode = area.getLayoutMode();
        if (mode == oldMode) return;

        int count = this.orderedChildEditorElements.size();
        int[] screenBoxes = new int[count * 4];
        for (int i = 0; i < count; i++) {
            AbstractEditorElement<?, ?> child = this.orderedChildEditorElements.get(i);
            screenBoxes[i * 4] = child.getX();
            screenBoxes[(i * 4) + 1] = child.getY();
            screenBoxes[(i * 4) + 2] = child.getWidth();
            screenBoxes[(i * 4) + 3] = child.getHeight();
        }
        ScrollGeometry geometry = area.getScrollGeometry();
        int oldContentWidth = geometry.getContentWidth();
        int oldContentHeight = geometry.getContentHeight();

        area.layoutMode.set(mode);
        // What the new mode has no use for becomes what the area does in its place, so the settings of the area show what it does
        if (area.scrollSettings.scrollDirection.tryGetNonNullElse(ScrollDirection.VERTICAL) != area.getScrollDirection()) area.scrollSettings.scrollDirection.set(area.getScrollDirection());
        if (area.childAlignment.tryGetNonNullElse(ScrollAreaAlignment.START) != area.getChildAlignment()) area.childAlignment.set(area.getChildAlignment());
        // Arranged content can turn into content of the user's size, which should hold everything it held before
        if (mode.hasUserContentSize() && !oldMode.hasUserContentSize()) {
            if (!ScrollAreaElement.isAdvancedSize(area.contentWidth)) area.contentWidth.set(oldContentWidth);
            if (!ScrollAreaElement.isAdvancedSize(area.contentHeight)) area.contentHeight.set(oldContentHeight);
        }

        if (mode.arrangesChildren()) {
            if (!oldMode.arrangesChildren()) this.orderChildrenByPosition(screenBoxes);
        } else {
            // The boxes of the mode before are of no use anymore, and the new geometry is what the children get placed in
            area.refreshFrame();
            for (int i = 0; i < count; i++) {
                AbstractEditorElement<?, ?> child = this.orderedChildEditorElements.get(i);
                child.placeFreelyInContainer(screenBoxes[i * 4], screenBoxes[(i * 4) + 1], screenBoxes[(i * 4) + 2], screenBoxes[(i * 4) + 3]);
            }
        }

        area.refreshFrame();
        this.editor.updateContainerHierarchy();

    }

    /**
     * Puts the children into the order they have on the screen: line by line along the direction the area does not scroll in,
     * like text. This is the order an area that arranges its children shows them in, so they stay next to what they were next to.
     *
     * @param screenBoxes X, Y, width and height of every child, in the order of the children.
     */
    protected void orderChildrenByPosition(int @NotNull [] screenBoxes) {
        boolean horizontal = this.element.getScrollDirection() == ScrollDirection.HORIZONTAL;
        int[] order = orderByPosition(screenBoxes, horizontal);
        List<AbstractEditorElement<?, ?>> children = new ArrayList<>(order.length);
        for (int index : order) {
            children.add(this.orderedChildEditorElements.get(index));
        }
        // Only the order among the children counts. The hierarchy of the editor puts them behind the area in that order.
        List<AbstractEditorElement<?, ?>> elements = this.editor.normalEditorElements;
        elements.removeAll(children);
        elements.addAll(children);
    }

    /**
     * Returns the order boxes come in when they get read like text: line by line along the direction the area scrolls in,
     * and across that direction in every line.<br>
     * Every box gets a line before the boxes get compared: a box is in the line of the box that started it while its middle lies in the range
     * of that box. Comparing two boxes by whether they share a line would not put them in an order, because sharing a line
     * with two boxes does not mean those two share one.
     *
     * @param screenBoxes X, Y, width and height of every box.
     * @param horizontal If the area scrolls sideways, which makes the lines columns.
     * @return The indexes of the boxes, in the order they come in.
     */
    protected static int @NotNull [] orderByPosition(int @NotNull [] screenBoxes, boolean horizontal) {
        int count = screenBoxes.length / 4;
        int mainOffset = horizontal ? 0 : 1;
        int crossOffset = horizontal ? 1 : 0;
        int sizeOffset = horizontal ? 2 : 3;
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt(index -> screenBoxes[(index * 4) + mainOffset]));
        int[] lines = new int[count];
        int line = -1;
        int lineEnd = 0;
        for (int index : order) {
            int start = screenBoxes[(index * 4) + mainOffset];
            int size = screenBoxes[(index * 4) + sizeOffset];
            if ((line < 0) || ((start + (size / 2)) >= lineEnd)) {
                line++;
                lineEnd = start + size;
            }
            lines[index] = line;
        }
        Arrays.sort(order, Comparator.<Integer>comparingInt(index -> lines[index]).thenComparingInt(index -> screenBoxes[(index * 4) + crossOffset]).thenComparingInt(index -> index));
        int[] ordered = new int[count];
        for (int i = 0; i < count; i++) {
            ordered[i] = order[i];
        }
        return ordered;
    }

    @Override
    public boolean canHoldChild(@NotNull AbstractEditorElement<?, ?> element) {
        // Vanilla elements belong to the screen, which renders them itself, so the area can neither move nor cut them
        if (element instanceof VanillaWidgetEditorElement) return false;
        // An area in itself, in one of its own children or in something it is anchored to could never work out where it is
        return (element != this) && !ElementContainerHierarchy.dependsOn(this.element, element.element);
    }

    @Override
    public int getDropAreaX() {
        return this.element.getScrollGeometry().getViewportX();
    }

    @Override
    public int getDropAreaY() {
        return this.element.getScrollGeometry().getViewportY();
    }

    @Override
    public int getDropAreaWidth() {
        return this.element.getScrollGeometry().getViewportWidth();
    }

    @Override
    public int getDropAreaHeight() {
        return this.element.getScrollGeometry().getViewportHeight();
    }

    /**
     * Renders a child of the area the way the editor shows elements, in place of the child rendering itself.
     */
    protected void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        AbstractEditorElement<?, ?> editorElement = this.childEditorElements.get(child);
        if (editorElement != null) {
            editorElement.renderInContainer(graphics, mouseX, mouseY, partial);
        } else {
            child.renderInternal(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public void init() {

        super.init();

        this.addArrangementMenu();

        ScrollSettingsMenus.addScrollDirectionEntry(this.rightClickMenu, this, this.element.scrollSettings);

        ContextMenu contentSizeMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("content_size", Component.translatable("fancymenu.elements.scroll_area.content_size"), contentSizeMenu)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.hasUserContentSize())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.content_size.desc")))
                .setIcon(MaterialIcons.STRAIGHTEN);

        this.element.contentWidth.buildContextMenuEntryAndAddTo(contentSizeMenu, this)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> this.element.getScrollDirection().scrollsX())
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.element.contentHeight.buildContextMenuEntryAndAddTo(contentSizeMenu, this)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> this.element.getScrollDirection().scrollsY())
                .setIcon(MaterialIcons.SWAP_VERT);

        contentSizeMenu.addSeparatorEntry("separator_before_fit_content").setStackable(true);

        contentSizeMenu.addClickableEntry("fit_content", Component.translatable("fancymenu.elements.scroll_area.content_size.fit"), (menu, entry) -> {
                    if (!entry.getStackMeta().isFirstInStack()) return;
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement<?, ?> e : this.editor.getSelectedElements()) {
                        if (e instanceof ScrollAreaEditorElement area) area.element.fitContentToChildren();
                    }
                    menu.closeMenuChain();
                })
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.content_size.fit.desc")))
                .setIcon(MaterialIcons.FIT_PAGE_HEIGHT);

        this.rightClickMenu.addSeparatorEntry("separator_after_content_size").setStackable(true);

        ScrollSettingsMenus.addScrollingMenu(this.rightClickMenu, this, this.element.scrollSettings);

        ScrollSettingsMenus.addAutoScrollMenu(this.rightClickMenu, this, this.element.scrollSettings);

        this.rightClickMenu.addSeparatorEntry("separator_before_looks").setStackable(true);

        ElementStyleMenus.addPanelBackgroundMenu(this.rightClickMenu, this, this.element.background);

        ContextMenu scrollBarsMenu = ScrollSettingsMenus.addScrollBarsMenu(this.rightClickMenu, this, this.element.scrollSettings);

        ElementStyleMenus.addScrollGrabberMenus(scrollBarsMenu, this, this.element.grabberStyle);

        ElementStyleMenus.addScrollBarBackgroundMenu(scrollBarsMenu, this, this.element.scrollBarBackground);

    }

    private void addArrangementMenu() {

        ContextMenu arrangementMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("arrangement", Component.translatable("fancymenu.elements.scroll_area.arrangement"), arrangementMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.arrangement.desc")))
                .setIcon(MaterialIcons.DASHBOARD);

        // Every mode has an entry of its own. Changing the mode gives the children new places, which nothing should do to them
        // on the way to another mode, like an entry that goes through the modes one by one would.
        ContextMenu layoutModeMenu = new ContextMenu();
        arrangementMenu.addSubMenuEntry("layout_mode", Component.empty(), layoutModeMenu)
                .setStackable(true)
                .setLabelSupplier((menu, entry) -> Component.translatable("fancymenu.elements.scroll_area.layout_mode", Component.translatable(this.element.getLayoutMode().getLocalizationKey()).setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get())))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.layout_mode.desc")))
                .setIcon(MaterialIcons.DASHBOARD_CUSTOMIZE);

        for (ScrollAreaLayoutMode mode : ScrollAreaLayoutMode.values()) {
            layoutModeMenu.addClickableEntry("layout_mode_" + mode.name().toLowerCase(Locale.ROOT), Component.translatable(mode.getLocalizationKey()), (menu, entry) -> {
                        if (!entry.getStackMeta().isFirstInStack()) return;
                        this.editor.history.saveSnapshot();
                        for (AbstractEditorElement<?, ?> e : this.editor.getSelectedElements()) {
                            if (e instanceof ScrollAreaEditorElement area) area.changeLayoutMode(mode);
                        }
                        menu.closeMenuChain();
                    })
                    .setStackable(true)
                    // The mode the area is in already is nothing to change to
                    .addIsActiveSupplier((menu, entry) -> this.element.getLayoutMode() != mode)
                    .setIcon(getLayoutModeIcon(mode));
        }

        arrangementMenu.addSeparatorEntry("separator_after_layout_mode").setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.getLayoutMode() != ScrollAreaLayoutMode.ANCHORS);

        this.element.childAlignment.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.arrangesChildren())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.alignment.desc")))
                .setIcon(MaterialIcons.ALIGN_HORIZONTAL_LEFT);

        this.element.childSpacing.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.arrangesChildren())
                .setIcon(MaterialIcons.FORMAT_LINE_SPACING);

        this.element.contentPadding.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.arrangesChildren())
                .setIcon(MaterialIcons.PADDING_ICON);

        arrangementMenu.addSeparatorEntry("separator_before_grid").setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.getLayoutMode() == ScrollAreaLayoutMode.GRID);

        this.element.gridAutoFit.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.getLayoutMode() == ScrollAreaLayoutMode.GRID)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.grid_auto_fit.desc")))
                .setIcon(MaterialIcons.FIT_WIDTH);

        this.element.gridLines.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.getLayoutMode() == ScrollAreaLayoutMode.GRID)
                .addIsActiveSupplier((menu, entry) -> !this.element.gridAutoFit.getBoolean())
                .setIcon(MaterialIcons.VIEW_COLUMN);

        this.element.gridMinCellSize.buildContextMenuEntryAndAddTo(arrangementMenu, this)
                .setStackable(true)
                .addIsVisibleSupplier((menu, entry) -> this.element.getLayoutMode() == ScrollAreaLayoutMode.GRID)
                .addIsActiveSupplier((menu, entry) -> this.element.gridAutoFit.getBoolean())
                .setIcon(MaterialIcons.WIDTH);

    }

    @NotNull
    private static MaterialIcon getLayoutModeIcon(@NotNull ScrollAreaLayoutMode mode) {
        return switch (mode) {
            case ANCHORS -> MaterialIcons.ANCHOR;
            case STACK -> MaterialIcons.VIEW_AGENDA;
            case GRID -> MaterialIcons.GRID_VIEW;
            case FLOW -> MaterialIcons.WRAP_TEXT;
        };
    }

}
