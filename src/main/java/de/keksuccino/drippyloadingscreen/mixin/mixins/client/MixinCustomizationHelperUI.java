package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelperUI;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//TODO übernehmen
@Mixin(CustomizationHelperUI.class)
public class MixinCustomizationHelperUI {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lde/keksuccino/konkrete/config/Config;getOrDefault(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", remap = false), index = 0, remap = false)
    private static String overrideConfigVariableName(String originalName) {
        if (Minecraft.getInstance().screen instanceof DrippyOverlayScreen) {
            return "this_is_absolutely_not_a_valid_name_so_it_will_simply_return_true_lel";
        }
        return originalName;
    }

}
