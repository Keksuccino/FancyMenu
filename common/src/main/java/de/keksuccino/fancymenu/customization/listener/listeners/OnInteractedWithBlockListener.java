package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnInteractedWithBlockListener extends AbstractListener {

    @Nullable
    private String cachedBlockKey;
    @Nullable
    private String cachedBlockPosX;
    @Nullable
    private String cachedBlockPosY;
    @Nullable
    private String cachedBlockPosZ;

    public OnInteractedWithBlockListener() {
        super("interacted_with_block");
    }

    public void onBlockInteracted(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        ResourceLocation blockLocation = Registry.BLOCK.getKey(blockState.getBlock());
        this.cachedBlockKey = blockLocation != null ? blockLocation.toString() : null;
        this.cachedBlockPosX = Integer.toString(blockPos.getX());
        this.cachedBlockPosY = Integer.toString(blockPos.getY());
        this.cachedBlockPosZ = Integer.toString(blockPos.getZ());
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> this.cachedBlockKey != null ? this.cachedBlockKey : "ERROR"));
        list.add(new CustomVariable("block_pos_x", () -> this.cachedBlockPosX != null ? this.cachedBlockPosX : "0"));
        list.add(new CustomVariable("block_pos_y", () -> this.cachedBlockPosY != null ? this.cachedBlockPosY : "0"));
        list.add(new CustomVariable("block_pos_z", () -> this.cachedBlockPosZ != null ? this.cachedBlockPosZ : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_interacted_with_block");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_interacted_with_block.desc"));
    }
}
