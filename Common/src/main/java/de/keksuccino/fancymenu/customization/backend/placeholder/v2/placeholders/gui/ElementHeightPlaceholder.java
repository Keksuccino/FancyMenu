package de.keksuccino.fancymenu.customization.backend.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.Placeholder;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.LayoutElement;
import de.keksuccino.fancymenu.customization.backend.item.CustomizationItemBase;
import de.keksuccino.fancymenu.customization.backend.item.v1.VanillaButtonCustomizationItem;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElementHeightPlaceholder extends Placeholder {

    public ElementHeightPlaceholder() {
        super("elementheight");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            CustomizationItemBase element = findCustomizationItemForId(id);
            if (element != null) {
                if (element instanceof VanillaButtonCustomizationItem) {
                    return "" + ((VanillaButtonCustomizationItem) element).parent.getButton().getHeight();
                }
                return "" + element.getHeight();
            }
        }
        return null;
    }

    private CustomizationItemBase findCustomizationItemForId(String id) {
        if (Minecraft.getInstance().screen != null) {
            if (!(Minecraft.getInstance().screen instanceof LayoutEditorScreen)) {
                MenuHandlerBase mh = MenuHandlerRegistry.getHandlerFor(Minecraft.getInstance().screen);
                if (mh != null) {
                    return mh.getItemByActionId(id);
                }
            } else {
                LayoutEditorScreen editor = ((LayoutEditorScreen)Minecraft.getInstance().screen);
                LayoutElement e = editor.getElementByActionId(id);
                if (e != null) {
                    return e.object;
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
        return Locals.localize("fancymenu.helper.placeholder.elementheight");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.elementheight.desc"), "%n%"));
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
