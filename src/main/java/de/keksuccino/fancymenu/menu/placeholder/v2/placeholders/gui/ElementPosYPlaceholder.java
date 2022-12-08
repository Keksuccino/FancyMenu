//TODO übernehmen
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.gui;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.VanillaButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElementPosYPlaceholder extends Placeholder {

    public ElementPosYPlaceholder() {
        super("elementposy");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            CustomizationItemBase element = findCustomizationItemForId(id);
            if (element != null) {
                if (element instanceof VanillaButtonCustomizationItem) {
                    return "" + ((VanillaButtonCustomizationItem) element).parent.getButton().y;
                }
                return "" + element.getPosY(Minecraft.getInstance().screen);
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
        return Locals.localize("fancymenu.helper.placeholder.elementposy");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.elementposy.desc"), "%n%"));
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
