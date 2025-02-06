package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
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

            ResourceLocation itemKey = Registry.ITEM.getKey(target.getItem());
            String dummyCommand = itemKey + nbtJson + " 1";

            ItemParser parser = new ItemParser(new StringReader(dummyCommand), false);
            parser.parse();

            return parser.getNbt();

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse ItemStack NBT data!", ex);
        }

        return null;

    }

}
