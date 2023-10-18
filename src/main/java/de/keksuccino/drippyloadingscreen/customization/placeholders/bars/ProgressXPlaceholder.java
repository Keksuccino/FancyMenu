
package de.keksuccino.drippyloadingscreen.customization.placeholders.bars;

import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgressXPlaceholder extends Placeholder {

    public ProgressXPlaceholder() {
        super("drippy_progress_x");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String id = dps.values.get("id");
        if (id != null) {
            CustomizationItemBase element = findCustomizationItemForId(id);
            if (element != null) {
                if (element instanceof AbstractProgressBarCustomizationItem) {
                    return "" + ((AbstractProgressBarCustomizationItem)element).getProgressX();
                }
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
        return I18n.get("drippyloadingscreen.placeholders.bars.progress.x");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(I18n.get("drippyloadingscreen.placeholders.bars.progress.x.desc"), "\n"));
    }

    @Override
    public String getCategory() {
        return "Drippy Loading Screen";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("id", "element_id_of_target");
        return dps;
    }

}
