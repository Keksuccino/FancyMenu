package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class SlotItemCountPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public SlotItemCountPlaceholder() {
        super("slot_item_count");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return "0";

            String slotValue = dps.values.get("slot");
            if (slotValue == null || !MathUtils.isInteger(slotValue)) return "0";

            int slot = Integer.parseInt(slotValue);
            if (slot < 0) return "0";

            Inventory inventory = Minecraft.getInstance().player.getInventory();
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) return "0";
            return "" + stack.getCount();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("slot");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.slot_item_count");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.slot_item_count.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("slot", "0");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}

