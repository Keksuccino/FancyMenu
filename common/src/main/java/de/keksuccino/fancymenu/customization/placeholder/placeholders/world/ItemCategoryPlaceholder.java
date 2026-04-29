package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemCategoryPlaceholder extends AbstractWorldPlaceholder {

    private static final String UNKNOWN = "UNKNOWN";
    private static final Map<Item, String> CATEGORY_DISPLAY_CACHE = new HashMap<>();
    private static final Map<Item, String> CATEGORY_KEY_CACHE = new HashMap<>();

    public ItemCategoryPlaceholder() {
        super("item_category");
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("item", "as_key");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return "";

        String key = dps.values.get("item");
        if (key == null || key.isEmpty()) return UNKNOWN;

        String asKeyString = dps.values.get("as_key");
        boolean asKey = (asKeyString != null) && asKeyString.equalsIgnoreCase("true");

        ResourceLocation itemId = ResourceLocation.tryParse(key);
        if (itemId == null) return UNKNOWN;

        Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemId);
        if (itemOptional.isEmpty()) return UNKNOWN;

        if (!ensureTabsBuilt()) return "";

        Item item = itemOptional.get();
        String cached = asKey ? CATEGORY_KEY_CACHE.get(item) : CATEGORY_DISPLAY_CACHE.get(item);
        if (cached != null) return cached;

        String category = findItemCategory(item, asKey);
        if (asKey) {
            CATEGORY_KEY_CACHE.put(item, category);
        } else {
            CATEGORY_DISPLAY_CACHE.put(item, category);
        }
        return category;
    }

    private static boolean ensureTabsBuilt() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return false;

        FeatureFlagSet enabledFeatures = player.connection != null ? player.connection.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS;
        boolean hasPermissions = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();

        boolean rebuilt = CreativeModeTabs.tryRebuildTabContents(enabledFeatures, hasPermissions, level.registryAccess());
        if (rebuilt) {
            CATEGORY_DISPLAY_CACHE.clear();
            CATEGORY_KEY_CACHE.clear();
        }

        return true;
    }

    @NotNull
    private static String findItemCategory(@NotNull Item item, boolean asKey) {
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;

            for (ItemStack stack : tab.getSearchTabDisplayItems()) {
                if (stack.getItem() == item) {
                    if (asKey) {
                        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).map(k -> k.location().toString()).orElse(UNKNOWN);
                    }
                    return tab.getDisplayName().getString();
                }
            }
        }
        return UNKNOWN;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.item_category";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("item", "minecraft:stone");
        values.put("as_key", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
