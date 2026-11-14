package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.container.ContainerEditorElement;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ScrollAreaEditorElement extends AbstractEditorElement<ScrollAreaEditorElement, ScrollAreaElement> implements ContainerEditorElement {

    /** The children of the area as what they are in the editor, by the element they wrap. */
    protected final Map<AbstractElement, AbstractEditorElement<?, ?>> childEditorElements = new IdentityHashMap<>();
    protected final ScrollAreaEmptyGuide emptyGuide = new ScrollAreaEmptyGuide();
    protected final ScrollAreaContentEndGrabber contentWidthGrabber = new ScrollAreaContentEndGrabber(this, true);
    protected final ScrollAreaContentEndGrabber contentHeightGrabber = new ScrollAreaContentEndGrabber(this, false);

    public ScrollAreaEditorElement(@NotNull ScrollAreaElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setParallaxAllowed(false);
        this.element.setChildRenderer(this::renderChild);
        this.element.setEmptyContentRenderer(this.emptyGuide::render);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        // The editor is a screen like any other for what takes the input of the area: its wheel, its scroll bars and its handles
        this.registerEditorListener(this.contentWidthGrabber);
        this.registerEditorListener(this.contentHeightGrabber);
        this.registerEditorListener(this.element.getInput());

        super.extractRenderState(graphics, mouseX, mouseY, partial);

        if ((this.renderPass != RenderPass.ELEMENT) && !this.element.layerHiddenInEditor) {
            this.contentWidthGrabber.extractRenderState(graphics, mouseX, mouseY, partial);
            this.contentHeightGrabber.extractRenderState(graphics, mouseX, mouseY, partial);
        }

    }

    private <T extends GuiEventListener & NarratableEntry> void registerEditorListener(@NotNull T listener) {
        List<GuiEventListener> children = ((IMixinScreen) this.editor).getChildrenFancyMenu();
        if (!children.contains(listener)) children.add(listener);
    }

    @Override
    public void setChildEditorElements(@NotNull List<AbstractEditorElement<?, ?>> children) {
        this.childEditorElements.clear();
        for (AbstractEditorElement<?, ?> child : children) {
            this.childEditorElements.put(child.element, child);
        }
    }

    @Override
    public boolean canHoldChild(@NotNull AbstractEditorElement<?, ?> element) {
        // Vanilla elements belong to the screen, which renders them itself, so the area can neither move nor cut them
        if (element instanceof VanillaWidgetEditorElement) return false;
        // An area in one of its own children would hold itself
        if (element == this) return false;
        return !(element.element instanceof ElementContainer container) || !ElementContainerHierarchy.isInContainer(this.element, container);
    }

    @Override
    public void extractChildRenderStateUncut(@NotNull AbstractEditorElement<?, ?> child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.element.renderChild(child.element, graphics, mouseX, mouseY, partial, (element, childGraphics, childMouseX, childMouseY, childPartial) -> child.extractElementRenderState(childGraphics, childMouseX, childMouseY, childPartial));
    }

    @Override
    public int getDropAreaX() {
        return this.element.getGeometry().getViewportX();
    }

    @Override
    public int getDropAreaY() {
        return this.element.getGeometry().getViewportY();
    }

    @Override
    public int getDropAreaWidth() {
        return this.element.getGeometry().getViewportWidth();
    }

    @Override
    public int getDropAreaHeight() {
        return this.element.getGeometry().getViewportHeight();
    }

    /**
     * Renders a child of the area the way the editor shows elements, in place of the child rendering itself.
     */
    protected void renderChild(@NotNull AbstractElement child, @NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        AbstractEditorElement<?, ?> editorElement = this.childEditorElements.get(child);
        if (editorElement != null) {
            editorElement.extractRenderStateInContainer(graphics, mouseX, mouseY, partial);
        } else {
            child.renderInternal(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public void init() {

        super.init();

        this.element.scrollDirection.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.scroll_direction.desc")))
                .setIcon(MaterialIcons.SWAP_VERT);

        ContextMenu contentSizeMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("content_size", Component.translatable("fancymenu.elements.scroll_area.content_size"), contentSizeMenu)
                .setStackable(true)
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

        this.element.scrollSpeed.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_area.scroll_speed.desc")))
                .setIcon(MaterialIcons.SPEED);

    }

}
