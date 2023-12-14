package de.keksuccino.fancymenu.util.enums;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;

/**
 * @param <E> The enum type.
 */
public interface LocalizedEnum<E> extends NamedEnum<E> {

    Supplier<Style> SUCCESS_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt());
    Supplier<Style> WARNING_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt());
    Supplier<Style> ERROR_TEXT_STYLE = () -> Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt());

    @NotNull
    String getLocalizationKeyBase();

    @NotNull
    default String getValueLocalizationKey() {
        return this.getLocalizationKeyBase() + "." + this.getName();
    }

    @NotNull
    default MutableComponent getValueComponent() {
        return new TranslatableComponent(this.getValueLocalizationKey()).withStyle(this.getValueComponentStyle());
    }

    @NotNull
    default Style getValueComponentStyle() {
        return Style.EMPTY;
    }

}
