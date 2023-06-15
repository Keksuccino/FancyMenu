package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElementBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IElementFactory {

    /**
     * Constructs element instances for {@link ScreenCustomizationLayer}s and {@link LayoutEditorScreen}s.
     *
     * @param menuIdentifier        The target menu the elements get constructed for. If this is NULL, no {@link AbstractDeepElement} instances will get constructed.
     * @param vanillaWidgetMetaList The vanilla buttons of the target menu. If this is NULL, no {@link VanillaButtonElement}s will get constructed.
     * @param layouts               The source layouts to construct the elements from.
     * @param normalElements        All normal {@link AbstractElement} instances will get added to this {@link Layout.OrderedElementCollection}.
     * @param vanillaButtonElements All {@link VanillaButtonElement} instances will get added to this list. If this is NULL, no {@link VanillaButtonElement}s will get constructed.
     * @param deepElements          All {@link AbstractDeepElement} instances will get added to this list. If this is NULL, no {@link AbstractDeepElement}s will get constructed.
     */
    default void constructElementInstances(@Nullable String menuIdentifier, @Nullable List<WidgetMeta> vanillaWidgetMetaList, @NotNull List<Layout> layouts, @NotNull Layout.OrderedElementCollection normalElements, @Nullable List<VanillaButtonElement> vanillaButtonElements, @Nullable List<AbstractDeepElement> deepElements) {

        Map<WidgetMeta, List<VanillaButtonElement>> unstackedVanillaButtonElements = new HashMap<>();
        Map<DeepElementBuilder<?, ?, ?>, List<AbstractDeepElement>> unstackedDeepElements = new HashMap<>();
        for (Layout layout : layouts) {
            //Construct element instances
            Layout.OrderedElementCollection layoutElements = layout.buildElementInstances();
            normalElements.backgroundElements.addAll(layoutElements.backgroundElements);
            normalElements.foregroundElements.addAll(layoutElements.foregroundElements);
            if (deepElements != null) {
                //Construct deep element instances
                for (AbstractDeepElement element : layout.buildDeepElementInstances()) {
                    if (!unstackedDeepElements.containsKey((DeepElementBuilder<?, ?, ?>) element.builder)) {
                        unstackedDeepElements.put((DeepElementBuilder<?, ?, ?>) element.builder, new ArrayList<>());
                    }
                    unstackedDeepElements.get(element.builder).add(element);
                }
            }
            if (vanillaButtonElements != null) {
                //Construct vanilla button element instances
                for (VanillaButtonElement element : layout.buildVanillaButtonElementInstances()) {
                    WidgetMeta d = findWidgetMeta(element.getInstanceIdentifier(), vanillaWidgetMetaList);
                    if (d != null) {
                        element.setVanillaButton(d);
                        if (!unstackedVanillaButtonElements.containsKey(d)) {
                            unstackedVanillaButtonElements.put(d, new ArrayList<>());
                        }
                        unstackedVanillaButtonElements.get(d).add(element);
                    }
                }
            }
        }

        if (deepElements != null) {
            //Add missing deep element instances
            DeepScreenCustomizationLayer deepScreenCustomizationLayer = (menuIdentifier != null) ? DeepScreenCustomizationLayerRegistry.getLayer(menuIdentifier) : null;
            if (deepScreenCustomizationLayer != null) {
                for (DeepElementBuilder<?, ?, ?> builder : deepScreenCustomizationLayer.getBuilders()) {
                    if (!unstackedDeepElements.containsKey(builder)) {
                        AbstractDeepElement element = builder.buildDefaultInstance();
                        unstackedDeepElements.put(builder, new ArrayList<>());
                        unstackedDeepElements.get(builder).add(element);
                    }
                }
            }
            //Stack collected deep elements, so only one element per element type is active at the same time
            for (Map.Entry<DeepElementBuilder<?, ?, ?>, List<AbstractDeepElement>> m : unstackedDeepElements.entrySet()) {
                if (!m.getValue().isEmpty()) {
                    if (m.getValue().size() > 1) {
                        AbstractDeepElement stacked = m.getKey().stackElementsInternal(m.getKey().buildDefaultInstance(), m.getValue().toArray(new AbstractDeepElement[0]));
                        if (stacked != null) {
                            deepElements.add(stacked);
                        }
                    } else {
                        deepElements.add(m.getValue().get(0));
                    }
                }
            }
            //Order deep elements by builder order
            if (deepScreenCustomizationLayer != null) {
                List<AbstractDeepElement> deepElementsOrdered = new ArrayList<>();
                for (DeepElementBuilder<?,?,?> b : deepScreenCustomizationLayer.getBuilders()) {
                    for (AbstractDeepElement e : deepElements) {
                        if (e.builder == b) {
                            deepElementsOrdered.add(e);
                            break;
                        }
                    }
                }
                deepElements.clear();
                deepElements.addAll(deepElementsOrdered);
            }
        }

        if ((vanillaWidgetMetaList != null) && (vanillaButtonElements != null)) {
            //Add missing vanilla button element instances
            for (WidgetMeta d : vanillaWidgetMetaList) {
                if (!unstackedVanillaButtonElements.containsKey(d)) {
                    VanillaButtonElement element = VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance();
                    element.setVanillaButton(d);
                    unstackedVanillaButtonElements.put(d, new ArrayList<>());
                    unstackedVanillaButtonElements.get(d).add(element);
                }
            }
            //Stack collected vanilla button elements, so only one element per button is active at the same time
            for (Map.Entry<WidgetMeta, List<VanillaButtonElement>> m : unstackedVanillaButtonElements.entrySet()) {
                if (!m.getValue().isEmpty()) {
                    if (m.getValue().size() > 1) {
                        VanillaButtonElement stacked = VanillaButtonElementBuilder.INSTANCE.stackElementsInternal(VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance(), m.getValue().toArray(new VanillaButtonElement[0]));
                        if (stacked != null) {
                            vanillaButtonElements.add(stacked);
                        }
                    } else {
                        vanillaButtonElements.add(m.getValue().get(0));
                    }
                }
            }
        }

    }

    /**
     * Constructs element instances for {@link ScreenCustomizationLayer}s and {@link LayoutEditorScreen}s.
     *
     * @param menuIdentifier The target menu the elements get constructed for. If this is NULL, no {@link AbstractDeepElement} instances will get constructed.
     * @param vanillaWidgetMetaList The vanilla buttons of the target menu. If this is NULL, no {@link VanillaButtonElement}s will get constructed.
     * @param layout The source layout to construct the elements from.
     * @param normalElements All normal {@link AbstractElement} instances will get added to this {@link Layout.OrderedElementCollection}.
     * @param vanillaButtonElements All {@link VanillaButtonElement} instances will get added to this list. If this is NULL, no {@link VanillaButtonElement}s will get constructed.
     * @param deepElements All {@link AbstractDeepElement} instances will get added to this list. If this is NULL, no {@link AbstractDeepElement}s will get constructed.
     */
    default void constructElementInstances(@Nullable String menuIdentifier, @Nullable List<WidgetMeta> vanillaWidgetMetaList, @NotNull Layout layout, @NotNull Layout.OrderedElementCollection normalElements, @Nullable List<VanillaButtonElement> vanillaButtonElements, @Nullable List<AbstractDeepElement> deepElements) {
        this.constructElementInstances(menuIdentifier, vanillaWidgetMetaList, ListUtils.build(layout), normalElements, vanillaButtonElements, deepElements);
    }

    /**
     * Returns the widget with the given identifier or NULL if no widget for the given identifier was found in the list.
     */
    @Nullable
    private static WidgetMeta findWidgetMeta(String identifier, List<WidgetMeta> metas) {
        identifier = identifier.replace("vanillabtn:", "");
        WidgetMeta data = findWidgetMetaForCompatibilityId(identifier, metas);
        if ((data == null) && MathUtils.isLong(identifier)) {
            data = findWidgetMetaForId(Long.parseLong(identifier), metas);
        }
        return data;
    }

    @Nullable
    private static WidgetMeta findWidgetMetaForId(long id, List<WidgetMeta> metas) {
        for (WidgetMeta m : metas) {
            if (m.getLongIdentifier() == id) return m;
        }
        return null;
    }

    @Nullable
    private static WidgetMeta findWidgetMetaForCompatibilityId(String compId, List<WidgetMeta> metas) {
        for (WidgetMeta m : metas) {
            if (m.getCompatibilityIdentifier() != null) {
                if (m.getCompatibilityIdentifier().equals(compId)) {
                    return m;
                }
            }
        }
        return null;
    }

}
