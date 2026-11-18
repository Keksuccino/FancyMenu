package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(I18n.class)
public interface AccessorMixinI18n {

    @Accessor("language") static Language get_language_FancyMenu() {
        throw new AssertionError();
    }

    @Invoker("setLanguage")
    static void invoke_setLanguage_FancyMenu(Language language) {
        throw new AssertionError();
    }

}
