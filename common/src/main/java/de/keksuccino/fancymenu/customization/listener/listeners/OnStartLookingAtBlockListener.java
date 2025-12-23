package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OnStartLookingAtBlockListener extends AbstractListener {

    public static final double MAX_LOOK_DISTANCE = 20.0D;

    @Nullable
    private LookedBlockData currentBlockData;

    public OnStartLookingAtBlockListener() {
        super("start_looking_at_block");
    }

    /**
     * @return {@code true} if a new block target was detected and listeners were notified.
     */
    public boolean onLookAtBlock(@NotNull ClientLevel level, @NotNull BlockHitResult hitResult, double distance) {
        LookedBlockData newData = LookedBlockData.from(level, hitResult, distance);
        LookedBlockData existingData = this.currentBlockData;

        if ((existingData != null) && existingData.isSameTarget(newData)) {
            this.currentBlockData = newData;
            return false;
        }

        this.currentBlockData = newData;
        this.notifyAllInstances();
        return true;
    }

    public void clearCurrentBlock() {
        this.currentBlockData = null;
    }

    @Nullable
    public LookedBlockData getCurrentBlockData() {
        return this.currentBlockData;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> {
            LookedBlockData data = this.currentBlockData;
            if (data == null || data.blockKey() == null) {
                return "ERROR";
            }
            return data.blockKey();
        }));
        list.add(new CustomVariable("block_pos_x", () -> {
            LookedBlockData data = this.currentBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getX());
        }));
        list.add(new CustomVariable("block_pos_y", () -> {
            LookedBlockData data = this.currentBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getY());
        }));
        list.add(new CustomVariable("block_pos_z", () -> {
            LookedBlockData data = this.currentBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getZ());
        }));
        list.add(new CustomVariable("distance_to_player", () -> {
            LookedBlockData data = this.currentBlockData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distance());
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_start_looking_at_block");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_start_looking_at_block.desc"));
    }

    public record LookedBlockData(@NotNull BlockPos blockPos,
                                  @NotNull BlockState blockState,
                                  @NotNull ResourceKey<Level> levelKey,
                                  @Nullable String blockKey,
                                  double distance) {

        private boolean isSameTarget(@NotNull LookedBlockData other) {
            return Objects.equals(this.blockPos, other.blockPos)
                && Objects.equals(this.blockState, other.blockState)
                && Objects.equals(this.levelKey, other.levelKey);
        }

        public static @NotNull LookedBlockData from(@NotNull ClientLevel level,
                                                    @NotNull BlockHitResult hitResult,
                                                    double distance) {
            BlockPos pos = hitResult.getBlockPos().immutable();
            BlockState state = level.getBlockState(pos);
            ResourceKey<Level> levelKey = level.dimension();
            Identifier blockKeyLocation = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            String blockKey = (blockKeyLocation != null) ? blockKeyLocation.toString() : null;
            return new LookedBlockData(pos, state, levelKey, blockKey, distance);
        }
    }
}
