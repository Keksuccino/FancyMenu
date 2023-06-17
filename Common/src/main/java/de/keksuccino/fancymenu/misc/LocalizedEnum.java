package de.keksuccino.fancymenu.misc;

import de.keksuccino.fancymenu.rendering.ui.UIBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface LocalizedEnum {

    Supplier<Style> SUCCESS_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().successTextColor.getColorInt());
    Supplier<Style> WARNING_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().warningTextColor.getColorInt());
    Supplier<Style> ERROR_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().errorTextColor.getColorInt());

    @NotNull
    String getLocalizationKeyBase();

    @NotNull
    String getName();

    @NotNull
    default String getEntryLocalizationKey() {
        return this.getLocalizationKeyBase() + "." + this.getName();
    }

    @NotNull
    default MutableComponent getEntryComponent() {
        return Component.translatable(this.getEntryLocalizationKey()).withStyle(this.getEntryComponentStyle());
    }

    @NotNull
    default Style getEntryComponentStyle() {
        return Style.EMPTY;
    }

}
