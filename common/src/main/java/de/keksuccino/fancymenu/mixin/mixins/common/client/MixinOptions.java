package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.util.ExtendedMinecraftOptions;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(Options.class)
public class MixinOptions implements ExtendedMinecraftOptions {

    @Unique @NotNull
    private final Map<String, OptionInstance<?>> optionInstancesFancyMenu = new LinkedHashMap<>();

    //IntelliJ shows this as wrong, but should work because of @Coerce
    @Inject(method = "processOptions", at = @At("HEAD"))
    private void beforeProcessOptionsFancyMenu(@Coerce Object $$0, CallbackInfo info) {
        this.optionInstancesFancyMenu.clear();
    }

    //IntelliJ could show this as wrong in the future, but should work because of @Coerce
    @WrapWithCondition(method = "processOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options$FieldAccess;process(Ljava/lang/String;Lnet/minecraft/client/OptionInstance;)V"))
    private boolean wrapProcessInProcessOptionsFancyMenu(@Coerce Object instance, String s, OptionInstance<?> optionInstance) {
        this.optionInstancesFancyMenu.put(s, optionInstance);
        return true;
    }

    @Unique
    @Override
    public @NotNull Map<String, OptionInstance<?>> getOptionInstancesFancyMenu() {
        return this.optionInstancesFancyMenu;
    }

}
