package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepCustomizationLayer {

    protected String parentMenuIdentifier;
    public Map<String, DeepCustomizationElement> elements = new HashMap<>();

    public DeepCustomizationLayer(String parentMenuIdentifier) {
        this.parentMenuIdentifier = parentMenuIdentifier;
    }

    public void registerElement(DeepCustomizationElement element) {
        this.elements.put(element.getIdentifier(), element);
    }

    public void unregisterElement(DeepCustomizationElement element) {
        this.elements.remove(element.getIdentifier());
    }

    public void unregisterElement(String identifier) {
        this.elements.remove(identifier);
    }

    public Map<String, DeepCustomizationElement> getElementsMap() {
        return this.elements;
    }

    public List<DeepCustomizationElement> getElementsList() {
        List<DeepCustomizationElement> l = new ArrayList<>();
        l.addAll(this.elements.values());
        return l;
    }

    @Nullable
    public DeepCustomizationElement getElementByIdentifier(String identifier) {
        return this.elements.get(identifier);
    }

    public String getParentMenuIdentifier() {
        return this.parentMenuIdentifier;
    }

}
