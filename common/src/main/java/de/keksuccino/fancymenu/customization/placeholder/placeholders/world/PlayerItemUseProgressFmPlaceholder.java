package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerItemUseProgressFmPlaceholder extends AbstractWorldPlaceholder {

    public PlayerItemUseProgressFmPlaceholder() {
        super("player_item_use_progress_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = this.getPlayer();
        if (player == null || !player.isUsingItem()) {
            return "0.0";
        }

        ItemStack stack = player.getUseItem();
        if (stack.isEmpty()) {
            return "0.0";
        }

        int totalDuration = stack.getUseDuration(player);
        int remainingTicks = player.getUseItemRemainingTicks();
        if (totalDuration <= 0) {
            return "0.0";
        }

        float progress = (totalDuration - remainingTicks) / (float) totalDuration;
        if (stack.getItem() instanceof BowItem) {
            progress = (progress * progress + progress * 2.0F) / 3.0F;
            if (progress > 1.0F) {
                progress = 1.0F;
            }
        }

        return String.valueOf(progress);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_item_use_progress_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
