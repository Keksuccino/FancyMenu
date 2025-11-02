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

public class OnSteppingOnBlockListener extends AbstractListener {

    @Nullable
    private BlockPos lastBlockPos;
    @Nullable
    private String cachedBlockKey;

    public OnSteppingOnBlockListener() {
        super("stepping_on_block");
    }

    public void onSteppedOnBlock(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        this.lastBlockPos = blockPos.immutable();
        ResourceLocation blockKeyLocation = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        this.cachedBlockKey = (blockKeyLocation != null) ? blockKeyLocation.toString() : null;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> this.cachedBlockKey != null ? this.cachedBlockKey : "ERROR"));
        list.add(new CustomVariable("block_pos_x", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getX()) : "0"));
        list.add(new CustomVariable("block_pos_y", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getY()) : "0"));
        list.add(new CustomVariable("block_pos_z", () -> this.lastBlockPos != null ? Integer.toString(this.lastBlockPos.getZ()) : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_stepping_on_block");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_stepping_on_block.desc"));
    }
}