package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGui;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class ActionBarMessageTimeFmPlaceholder extends AbstractWorldPlaceholder {

    public ActionBarMessageTimeFmPlaceholder() {
        super("action_bar_message_time_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(((IMixinGui) Minecraft.getInstance().gui).get_overlayMessageTime_FancyMenu());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.action_bar_message_time_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
