package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NBTBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static CompoundTag buildNbtFromString(@NotNull ItemStack target, @NotNull String nbtJson) {

        try {

            RegistryAccess.Frozen frozenAccess = RegistryAccess.fromRegistryOfRegistries(BuiltinRegistries.REGISTRY).freeze();
            ResourceLocation itemKey = Registry.ITEM.getKey(target.getItem());
            String dummyCommand = itemKey + nbtJson + " 1";

            ItemParser.ItemResult result = ItemParser.parseForItem(HolderLookup.forRegistry(Registry.ITEM), new StringReader(dummyCommand));

            return result.nbt();

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse ItemStack NBT data!", ex);
        }

        return null;

    }

}
