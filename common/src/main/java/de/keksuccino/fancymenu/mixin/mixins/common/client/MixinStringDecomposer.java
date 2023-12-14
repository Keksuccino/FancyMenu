package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {

    @Unique
    private static String cachedIterateFormattedStringFancyMenu;
    @Unique
    private static int cachedIterateFormattedForLoopCharIndexFancyMenu;
    @Unique
    private static Style cachedIterateFormattedEmptyStyleFancyMenu;
    @Unique
    private static char cachedIterateFormattedFormattingCodeCharFancyMenu;

    @Inject(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "HEAD"))
    private static void cacheMethodParametersFancyMenu(String s, int i, Style baseStyle, Style emptyStyle, FormattedCharSink charSink, CallbackInfoReturnable<Boolean> info) {
        cachedIterateFormattedStringFancyMenu = s;
        cachedIterateFormattedEmptyStyleFancyMenu = emptyStyle;
    }

    @Redirect(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"))
    private static ChatFormatting redirectGetByCodeFancyMenu(char c) {
        cachedIterateFormattedFormattingCodeCharFancyMenu = c;
        return ChatFormatting.WHITE;
    }

    @Redirect(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;applyLegacyFormat(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;"))
    private static Style redirectApplyLegacyFormatFancyMenu(Style instance, ChatFormatting chatFormatting) {

        Style s = instance;
        char c = cachedIterateFormattedFormattingCodeCharFancyMenu;

        //Handle custom formatting codes
        TextColorFormatter formatter = TextColorFormatterRegistry.getByCode(c);
        if (formatter != null) {
            s = formatter.getStyle();
        }

        //Handle vanilla formatting codes
        ChatFormatting vanillaFormatting = ChatFormatting.getByCode(c);
        if (vanillaFormatting != null) {
            s = vanillaFormatting == ChatFormatting.RESET ? cachedIterateFormattedEmptyStyleFancyMenu : s.applyLegacyFormat(vanillaFormatting);
        }

        return s;

    }

}
