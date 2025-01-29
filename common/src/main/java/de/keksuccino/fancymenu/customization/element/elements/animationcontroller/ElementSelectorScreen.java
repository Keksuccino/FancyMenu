package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ElementSelectorScreen extends CellScreen {

    protected final LayoutEditorScreen layoutEditor;
    protected final Screen parent;
    protected final Consumer<AbstractEditorElement> callback;
    protected final Predicate<AbstractEditorElement> filter;

    public ElementSelectorScreen(@NotNull LayoutEditorScreen layoutEditor, @NotNull Screen parent, @NotNull Consumer<AbstractEditorElement> callback) {
        this(layoutEditor, parent, callback, element -> true);
    }

    public ElementSelectorScreen(@NotNull LayoutEditorScreen layoutEditor, @NotNull Screen parent, @NotNull Consumer<AbstractEditorElement> callback, @NotNull Predicate<AbstractEditorElement> filter) {
        super(Component.translatable("fancymenu.elements.animation_controller.select_element"));
        this.layoutEditor = layoutEditor;
        this.parent = parent;
        this.callback = callback;
        this.filter = filter;
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

        // Create selectable cells for each element
        for (AbstractEditorElement element : elements) {
            this.addCell(new ElementCell(element)).setSelectable(true);
        }

        this.addStartEndSpacerCell();

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

            super(Component.empty());

            this.element = element;

            MutableComponent label = element.element.getDisplayName().copy();
            label = label.setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
            label = label.append(Component.literal(" [" + element.element.getInstanceIdentifier() + "]").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().generic_text_base_color.getColorInt())));
            this.setText(label);

        }

    }
}