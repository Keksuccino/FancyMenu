package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class InventoryItemCountPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public InventoryItemCountPlaceholder() {
        super("inventory_item_count");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return "0";

            Inventory inventory = Minecraft.getInstance().player.getInventory();
            String itemKey = dps.values.get("item");

            if (itemKey == null || itemKey.trim().isEmpty()) {
                int total = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) total += stack.getCount();
                }
                return "" + total;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(itemKey.trim());
            if (itemId == null) return "0";

            Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemId);
            if (itemOptional.isEmpty()) return "0";
            Item targetItem = itemOptional.get();

            int total = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && stack.getItem() == targetItem) {
                    total += stack.getCount();
                }
            }
            return "" + total;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get replacement for '" + this.getIdentifier() + "' placeholder.", ex);
        }

        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("item");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.inventory_item_count");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.inventory_item_count.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("item", "minecraft:stone");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}

