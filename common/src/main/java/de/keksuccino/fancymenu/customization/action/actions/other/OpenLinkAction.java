package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenLinkAction extends Action {

    public OpenLinkAction() {
        super("openlink");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            WebUtils.openWebLink(StringUtils.convertFormatCodes(value, "§", "&"));
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.openlink");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.openlink.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.openlink.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "https://example.com";
    }

}
