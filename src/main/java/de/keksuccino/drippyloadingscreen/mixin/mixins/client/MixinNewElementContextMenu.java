package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.drippyloadingscreen.customization.items.IDrippyCustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.CustomizationItemRegistry;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorUI;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(LayoutEditorUI.NewElementContextMenu.class)
public class MixinNewElementContextMenu {

    private static final Logger LOGGER = LogManager.getLogger();

    @Redirect(method = "openMenuAt", at = @At(value = "INVOKE", target = "Lde/keksuccino/fancymenu/api/item/CustomizationItemRegistry;getItems()Ljava/util/List;"), remap = false)
    private List<CustomizationItemContainer> redirectGetItemsInOpenMenuAt() {

        List<CustomizationItemContainer> l = CustomizationItemRegistry.getItems();

        if ((Minecraft.getInstance().screen != null) && (Minecraft.getInstance().screen instanceof LayoutEditorScreen)) {
            List<CustomizationItemContainer> l2 = new ArrayList<>();
            for (CustomizationItemContainer c : l) {
                if (!(((LayoutEditorScreen)Minecraft.getInstance().screen).screen instanceof DrippyOverlayScreen)) {
                    //Filter out Drippy-exclusive element types in non-Drippy layouts
                    if (!(c instanceof IDrippyCustomizationItemContainer)) {
                        l2.add(c);
                    }
                } else {
                    //Filter out unsupported element types in Drippy layouts
                    if (c.getIdentifier().equals("fancymenu_customization_player_entity")) {
                        continue;
                    }
                    if (c.getIdentifier().equals("fancymenu_extension:audio_item")) {
                        continue;
                    }
                    l2.add(c);
                }
            }
            l = l2;
        }

        return l;

    }

}
