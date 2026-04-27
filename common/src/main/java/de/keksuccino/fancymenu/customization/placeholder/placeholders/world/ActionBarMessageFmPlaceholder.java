package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGui;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ActionBarMessageFmPlaceholder extends AbstractWorldPlaceholder {

    public ActionBarMessageFmPlaceholder() {
        super("action_bar_message_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Component message = ((IMixinGui) Minecraft.getInstance().gui).get_overlayMessageString_FancyMenu();
        return message != null ? ComponentParser.toJson(message) : "";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.action_bar_message_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
