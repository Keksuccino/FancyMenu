package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGui;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinSpectatorGui;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class HighlightedItemTimeFmPlaceholder extends AbstractWorldPlaceholder {

    public HighlightedItemTimeFmPlaceholder() {
        super("highlighted_item_time_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        int time = ((IMixinGui) Minecraft.getInstance().gui).get_toolHighlightTimer_FancyMenu();
        if ((Minecraft.getInstance().player != null) && Minecraft.getInstance().player.isSpectator()) {
            IMixinSpectatorGui spectatorGui = (IMixinSpectatorGui) Minecraft.getInstance().gui.getSpectatorGui();
            if (spectatorGui.invoke_getHotbarAlpha_FancyMenu() > 0.0F) {
                time = (int) (40.0 * Minecraft.getInstance().options.notificationDisplayTime().get());
            } else {
                time = 0;
            }
        }
        return String.valueOf(time);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.highlighted_item_time_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
