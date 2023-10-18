package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.DrippyLoadingScreen;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

//TODO übernehmen
@Mixin(MenuCustomizationProperties.class)
public class MixinMenuCustomizationProperties {

    @Inject(method = "getPropertiesWithIdentifier", at = @At("RETURN"), cancellable = true, remap = false)
    private static void filterOutUniversalLayouts(String identifier, CallbackInfoReturnable<List<PropertiesSet>> info) {
        if (identifier.equals("de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen")) {
            if (!DrippyLoadingScreen.config.getOrDefault("allow_universal_layouts", true)) {

                List<PropertiesSet> l = new ArrayList<>();

                for (PropertiesSet set : info.getReturnValue()) {
                    List<PropertiesSection> l2 = set.getPropertiesOfType("customization-meta");
                    if (l2.isEmpty()) {
                        l2 = set.getPropertiesOfType("type-meta");
                    }
                    String s2 = l2.get(0).getEntryValue("identifier");
                    if (s2 != null) {
                        if (!s2.equals("%fancymenu:universal_layout%")) {
                            l.add(set);
                        }
                    }
                }

                info.setReturnValue(l);

            }
        }
    }
}
