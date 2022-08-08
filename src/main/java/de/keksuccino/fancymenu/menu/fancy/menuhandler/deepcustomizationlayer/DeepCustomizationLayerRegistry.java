//---
package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepCustomizationLayerRegistry {

    protected static Map<String, DeepCustomizationLayer> layers = new HashMap<>();

    public static void registerLayer(DeepCustomizationLayer layer) {
        layers.put(layer.getParentMenuIdentifier(), layer);
    }

    public static void unregisterLayer(DeepCustomizationLayer layer) {
        layers.remove(layer.getParentMenuIdentifier());
    }

    public static void unregisterLayer(String menuIdentifier) {
        layers.remove(menuIdentifier);
    }

    public static Map<String, DeepCustomizationLayer> getLayersMap() {
        return layers;
    }

    public static List<DeepCustomizationLayer> getLayersList() {
        List<DeepCustomizationLayer> l = new ArrayList<>();
        l.addAll(layers.values());
        return l;
    }

    @Nullable
    public static DeepCustomizationLayer getLayerByMenuIdentifier(String menuIdentifier) {
        return layers.get(menuIdentifier);
    }

    public boolean hasLayerForMenuIdentifier(String menuIdentifier) {
        return (getLayerByMenuIdentifier(menuIdentifier) != null);
    }

}
