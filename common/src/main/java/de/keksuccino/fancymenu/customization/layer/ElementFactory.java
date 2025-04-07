package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.util.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ElementFactory {

    /**
     * Constructs element instances for {@link ScreenCustomizationLayer}s and {@link LayoutEditorScreen}s.
     *
     * @param screenIdentifier The target menu the elements get constructed for.
     * @param vanillaWidgetMetaList The vanilla buttons of the target menu. If this is NULL, no {@link VanillaWidgetElement}s will get constructed.
     * @param layouts The source layouts to construct the elements from.
     * @param normalElements All normal {@link AbstractElement} instances will get added to this {@link Layout.OrderedElementCollection}.
     * @param vanillaWidgetElements All {@link VanillaWidgetElement} instances will get added to this list. If this is NULL, no {@link VanillaWidgetElement}s will get constructed.
     */
    default void constructElementInstances(@Nullable String screenIdentifier, @Nullable List<WidgetMeta> vanillaWidgetMetaList, @NotNull List<Layout> layouts, @NotNull Layout.OrderedElementCollection normalElements, @Nullable List<VanillaWidgetElement> vanillaWidgetElements) {

        Map<WidgetMeta, List<VanillaWidgetElement>> unstackedVanillaButtonElements = new HashMap<>();
        for (Layout layout : layouts) {
            //Construct element instances
            Layout.OrderedElementCollection layoutElements = layout.buildElementInstances();
            normalElements.backgroundElements.addAll(layoutElements.backgroundElements);
            normalElements.foregroundElements.addAll(layoutElements.foregroundElements);
            if (vanillaWidgetElements != null) {
                //Construct vanilla button element instances
                for (VanillaWidgetElement element : layout.buildVanillaButtonElementInstances()) {
                    WidgetMeta d = (vanillaWidgetMetaList != null) ? findWidgetMeta(element.getInstanceIdentifier(), vanillaWidgetMetaList) : null;
                    if (d != null) {
                        element.setVanillaWidget(d, (element.anchorPoint == ElementAnchorPoints.VANILLA));
                        if (!unstackedVanillaButtonElements.containsKey(d)) {
                            unstackedVanillaButtonElements.put(d, new ArrayList<>());
                        }
                        unstackedVanillaButtonElements.get(d).add(element);
                    }
                }
            }
        }

        if ((vanillaWidgetMetaList != null) && (vanillaWidgetElements != null)) {
            //Add missing vanilla button element instances
            for (WidgetMeta d : vanillaWidgetMetaList) {
                if (!unstackedVanillaButtonElements.containsKey(d)) {
                    VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
                    element.setVanillaWidget(d, true);
                    unstackedVanillaButtonElements.put(d, new ArrayList<>());
                    unstackedVanillaButtonElements.get(d).add(element);
                }
            }
            //Stack collected vanilla button elements, so only one element per button is active at the same time
            for (Map.Entry<WidgetMeta, List<VanillaWidgetElement>> m : unstackedVanillaButtonElements.entrySet()) {
                if (!m.getValue().isEmpty()) {
                    if (m.getValue().size() > 1) {
                        VanillaWidgetElement stacked = VanillaWidgetElementBuilder.INSTANCE.stackElementsInternal(VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance(), m.getValue().toArray(new VanillaWidgetElement[0]));
                        if (stacked != null) {
                            if (stacked.anchorPoint == ElementAnchorPoints.VANILLA) {
                                stacked.mirrorVanillaWidgetSizeAndPosition();
                            }
                            vanillaWidgetElements.add(stacked);
                        }
                    } else {
                        if (m.getValue().get(0).anchorPoint == ElementAnchorPoints.VANILLA) {
                            m.getValue().get(0).mirrorVanillaWidgetSizeAndPosition();
                        }
                        vanillaWidgetElements.add(m.getValue().get(0));
                    }
                }
            }
        }

    }

    /**
     * Constructs element instances for {@link ScreenCustomizationLayer}s and {@link LayoutEditorScreen}s.
     *
     * @param menuIdentifier The target menu the elements get constructed for.
     * @param vanillaWidgetMetaList The vanilla buttons of the target menu. If this is NULL, no {@link VanillaWidgetElement}s will get constructed.
     * @param layout The source layout to construct the elements from.
     * @param normalElements All normal {@link AbstractElement} instances will get added to this {@link Layout.OrderedElementCollection}.
     * @param vanillaWidgetElements All {@link VanillaWidgetElement} instances will get added to this list. If this is NULL, no {@link VanillaWidgetElement}s will get constructed.
     */
    default void constructElementInstances(@Nullable String menuIdentifier, @Nullable List<WidgetMeta> vanillaWidgetMetaList, @NotNull Layout layout, @NotNull Layout.OrderedElementCollection normalElements, @Nullable List<VanillaWidgetElement> vanillaWidgetElements) {
        this.constructElementInstances(menuIdentifier, vanillaWidgetMetaList, ListUtils.of(layout), normalElements, vanillaWidgetElements);
    }

    /**
     * Returns the widget with the given identifier or NULL if no widget for the given identifier was found in the list.
     */
    @Nullable
    private static WidgetMeta findWidgetMeta(@NotNull String identifier, @NotNull List<WidgetMeta> metas) {
        identifier = identifier.replace("vanillabtn:", "");
        for (WidgetMeta meta : metas) {
            if (WidgetIdentifierHandler.isIdentifierOfWidget(identifier, meta)) return meta;
        }
        return null;
    }

}
