package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import de.keksuccino.fancymenu.customization.element.elements.image.ImageEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ElementSelectorScreen extends CellScreen {

    protected final LayoutEditorScreen layoutEditor;
    protected final Screen parent;
    protected final Consumer<AbstractEditorElement> callback;
    protected final Predicate<AbstractEditorElement> filter;
    @Nullable
    protected final List<String> elementIds;

    public ElementSelectorScreen(@NotNull LayoutEditorScreen layoutEditor, @NotNull Screen parent, @Nullable List<String> elementIds, @NotNull Consumer<AbstractEditorElement> callback) {
        this(layoutEditor, parent, elementIds, callback, element -> true);
    }

    public ElementSelectorScreen(@NotNull LayoutEditorScreen layoutEditor, @NotNull Screen parent, @Nullable List<String> elementIds, @NotNull Consumer<AbstractEditorElement> callback, @NotNull Predicate<AbstractEditorElement> filter) {
        super(Components.translatable("fancymenu.elements.animation_controller.select_element"));
        this.layoutEditor = layoutEditor;
        this.parent = parent;
        this.callback = callback;
        this.filter = filter;
        this.elementIds = elementIds;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        // Get all available elements
        List<AbstractEditorElement> elements = new ArrayList<>();
        elements.addAll(this.layoutEditor.normalEditorElements);
        elements.addAll(this.layoutEditor.vanillaWidgetEditorElements);

        // Filter elements
        elements.removeIf(element -> !this.filter.test(element));

        // Add dummy elements in case some elements don't exist anymore
        if (this.elementIds != null) {
            this.elementIds.forEach(s -> {
                if (!this.containsElement(elements, s)) {
                    ImageEditorElement dummy = Elements.IMAGE.wrapIntoEditorElement(Elements.IMAGE.buildDefaultInstance(), this.layoutEditor);
                    dummy.getElement().customElementLayerName = "---";
                    dummy.getElement().setInstanceIdentifier(s);
                    elements.add(dummy);
                }
            });
        }

        // Create selectable cells for each element
        for (AbstractEditorElement element : elements) {
            this.addCell(new ElementCell(element)).setSelectable(true);
        }

        this.addStartEndSpacerCell();

    }

    protected boolean containsElement(@NotNull List<AbstractEditorElement> elements, @NotNull String elementId) {
        for (AbstractEditorElement e : elements) {
            if (e.element.getInstanceIdentifier().equals(elementId)) return true;
        }
        return false;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        RenderCell cell = this.getSelectedCell();
        this.callback.accept(cell instanceof ElementCell ? ((ElementCell) cell).element : null);
    }

    @Override
    public boolean allowDone() {
        return (this.getSelectedCell() != null) && (this.getSelectedCell() instanceof ElementCell);
    }

    public class ElementCell extends LabelCell {

        protected final AbstractEditorElement element;

        public ElementCell(@NotNull AbstractEditorElement element) {

            super(Components.empty());

            this.element = element;

            MutableComponent label = element.element.getDisplayName().copy();
            label = label.setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
            label = label.append(Components.literal(" [" + element.element.getInstanceIdentifier() + "]").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().generic_text_base_color.getColorInt())));
            this.setText(label);

        }

    }
}