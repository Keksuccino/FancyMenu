package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OnLookingAtBlockListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private BlockPos lastBlockPos;
    @Nullable
    private BlockState lastBlockState;
    @Nullable
    private ResourceKey<Level> lastLevelKey;
    @Nullable
    private String cachedBlockKey;
    @Nullable
    private String cachedDistance;

    public OnLookingAtBlockListener() {
        super("looking_at_block");
    }

    public void onLookAtBlock(@NotNull ClientLevel level, @NotNull BlockHitResult hitResult, double distance) {
        BlockPos blockPos = hitResult.getBlockPos().immutable();
        BlockState blockState = level.getBlockState(blockPos);
        ResourceKey<Level> levelKey = level.dimension();

        if (Objects.equals(this.lastBlockPos, blockPos) && Objects.equals(this.lastBlockState, blockState) && Objects.equals(this.lastLevelKey, levelKey)) {
            return;
        }

        this.lastBlockPos = blockPos;
        this.lastBlockState = blockState;
        this.lastLevelKey = levelKey;

        ResourceLocation blockKeyLocation = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        this.cachedBlockKey = (blockKeyLocation != null) ? blockKeyLocation.toString() : null;
        this.cachedDistance = Double.toString(distance);

        this.notifyAllInstances();
    }

    public void onStopLooking() {
        this.lastBlockPos = null;
        this.lastBlockState = null;
        this.lastLevelKey = null;
        this.cachedBlockKey = null;
        this.cachedDistance = null;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> this.cachedBlockKey != null ? this.cachedBlockKey : "ERROR"));
        list.add(new CustomVariable("distance", () -> this.cachedDistance != null ? this.cachedDistance : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_looking_at_block");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_looking_at_block.desc"));
    }
}
