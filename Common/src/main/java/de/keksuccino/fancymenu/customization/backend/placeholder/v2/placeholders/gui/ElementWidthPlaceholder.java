package de.keksuccino.fancymenu.customization.backend.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.backend.element.AbstractElement;
import de.keksuccino.fancymenu.customization.backend.element.v1.button.VanillaButtonCustomizationItem;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElementWidthPlaceholder extends Placeholder {

    public ElementWidthPlaceholder() {
        super("elementwidth");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            AbstractElement element = findCustomizationItemForId(id);
            if (element != null) {
                if (element instanceof VanillaButtonCustomizationItem) {
                    return "" + ((VanillaButtonCustomizationItem) element).parent.getButton().getWidth();
                }
                return "" + element.getWidth();
            }
        }
        return null;
    }

    private AbstractElement findCustomizationItemForId(String id) {
        if (Minecraft.getInstance().screen != null) {
            if (!(Minecraft.getInstance().screen instanceof LayoutEditorScreen)) {
                ScreenCustomizationLayer mh = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
                if (mh != null) {
                    return mh.getItemByActionId(id);
                }
            } else {
                LayoutEditorScreen editor = ((LayoutEditorScreen)Minecraft.getInstance().screen);
                AbstractEditorElement e = editor.getElementByActionId(id);
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
        return Locals.localize("fancymenu.helper.placeholder.elementwidth");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.elementwidth.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("id", "some.element.id");
        return dps;
    }

}
