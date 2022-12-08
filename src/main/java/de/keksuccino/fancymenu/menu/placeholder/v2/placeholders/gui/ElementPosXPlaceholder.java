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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElementPosXPlaceholder extends Placeholder {

    public ElementPosXPlaceholder() {
        super("elementposx");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            CustomizationItemBase element = findCustomizationItemForId(id);
            if (element != null) {
                if (element instanceof VanillaButtonCustomizationItem) {
                    return "" + ((VanillaButtonCustomizationItem) element).parent.getButton().x;
                }
                return "" + element.getPosX(Minecraft.getMinecraft().currentScreen);
            }
        }
        return null;
    }

    private CustomizationItemBase findCustomizationItemForId(String id) {
        if (Minecraft.getMinecraft().currentScreen != null) {
            if (!(Minecraft.getMinecraft().currentScreen instanceof LayoutEditorScreen)) {
                MenuHandlerBase mh = MenuHandlerRegistry.getHandlerFor(Minecraft.getMinecraft().currentScreen);
                if (mh != null) {
                    return mh.getItemByActionId(id);
                }
            } else {
                LayoutEditorScreen editor = ((LayoutEditorScreen)Minecraft.getMinecraft().currentScreen);
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
        return Locals.localize("fancymenu.helper.placeholder.elementposx");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.placeholder.elementposx.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.gui");
    }

    @Override
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("id", "some.element.id");
        return dps;
    }

}
