package de.keksuccino.fancymenu.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

public interface LocalizedCycleEnum extends LocalizedEnum {

    @NotNull
    default MutableComponent getCycleComponent() {
        return Component.translatable(this.getLocalizationKeyBase(), this.getEntryComponent()).withStyle(this.getCycleComponentStyle());
    }

    @NotNull
    default Style getCycleComponentStyle() {
        return Style.EMPTY;
    }

}
