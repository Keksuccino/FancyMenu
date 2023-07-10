package de.keksuccino.fancymenu.util.enums;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface LocalizedEnum extends NamedEnum {

    Supplier<Style> SUCCESS_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().success_text_color.getColorInt());
    Supplier<Style> WARNING_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().warning_text_color.getColorInt());
    Supplier<Style> ERROR_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorScheme().error_text_color.getColorInt());

    @NotNull
    String getLocalizationKeyBase();

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