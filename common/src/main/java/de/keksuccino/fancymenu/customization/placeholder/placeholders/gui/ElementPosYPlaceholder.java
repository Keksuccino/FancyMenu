package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElementPosYPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public ElementPosYPlaceholder() {
        super("elementposy");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            AbstractElement element = findElement(id);
            if (element != null) {
                return "" + element.getAbsoluteY();
            } else {
                LOGGER.error("[FANCYMENU] Unable to get Y-position of element via placeholder! Element not found: " + id);
            }
        }
        return "1";
    }

    private AbstractElement findElement(String id) {
        if (Minecraft.getInstance().screen != null) {
            if (!(Minecraft.getInstance().screen instanceof LayoutEditorScreen editor)) {
                ScreenCustomizationLayer mh = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
                if (mh != null) {
                    return mh.getElementByInstanceIdentifier(id);
                }
            } else {
                AbstractEditorElement e = editor.getElementByInstanceIdentifier(id);
                if (e != null) {
                    return e.element;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("id");
        return l;
    }

    @Override
    public String getDisplayName() {
        return I18n.get("fancymenu.helper.placeholder.elementposy");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.placeholder.elementposy.desc")));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("id", "some.element.id");
        return dps;
    }

}