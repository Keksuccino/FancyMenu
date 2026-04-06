package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

@Mixin(HolderSet.Named.class)
public interface IMixinHolderSetNamed<T> {

    @Invoker("<init>")
    static <T> HolderSet.Named<T> invoke_new_FancyMenu(HolderOwner<T> owner, TagKey<T> key) {
        throw new AssertionError();
    }

    @Invoker("bind") void invoke_bind_FancyMenu(List<Holder<T>> contents);

}
