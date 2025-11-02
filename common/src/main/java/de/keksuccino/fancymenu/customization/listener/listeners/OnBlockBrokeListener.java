package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnBlockBrokeListener extends AbstractListener {

    @Nullable
    private BlockPos lastBlockPos;
    @Nullable
    private String cachedBlockKey;
    @Nullable
    private String cachedToolKey;

    public OnBlockBrokeListener() {
        super("block_broke");
    }

    public void onBlockBroke(@NotNull BlockPos blockPos, @NotNull BlockState blockState, @Nullable String brokeWithItemKey) {
        this.lastBlockPos = blockPos.immutable();
        ResourceLocation blockKeyLocation = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        this.cachedBlockKey = (blockKeyLocation != null) ? blockKeyLocation.toString() : null;
        this.cachedToolKey = (brokeWithItemKey != null && !brokeWithItemKey.isBlank()) ? brokeWithItemKey : null;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> this.cachedBlockKey != null ? this.cachedBlockKey : "ERROR"));
        list.add(new CustomVariable("broke_with_item_key", () -> this.cachedToolKey != null ? this.cachedToolKey : "EMPTY"));
        list.add(new CustomVariable("block_pos_x", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getX()) : "0"));
        list.add(new CustomVariable("block_pos_y", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getY()) : "0"));
        list.add(new CustomVariable("block_pos_z", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getZ()) : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_block_broke");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_block_broke.desc"));
    }
}
