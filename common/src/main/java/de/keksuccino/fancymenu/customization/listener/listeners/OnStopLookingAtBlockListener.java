package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtBlockListener.LookedBlockData;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnStopLookingAtBlockListener extends AbstractListener {

    @Nullable
    private LookedBlockData lastBlockData;

    public OnStopLookingAtBlockListener() {
        super("stop_looking_at_block");
    }

    public void onStopLooking(@Nullable LookedBlockData data) {
        this.lastBlockData = data;
        if (data != null) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("block_key", () -> {
            LookedBlockData data = this.lastBlockData;
            if (data == null || data.blockKey() == null) {
                return "ERROR";
            }
            return data.blockKey();
        }));
        list.add(new CustomVariable("block_pos_x", () -> {
            LookedBlockData data = this.lastBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getX());
        }));
        list.add(new CustomVariable("block_pos_y", () -> {
            LookedBlockData data = this.lastBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getY());
        }));
        list.add(new CustomVariable("block_pos_z", () -> {
            LookedBlockData data = this.lastBlockData;
            if (data == null) {
                return "0";
            }
            return Integer.toString(data.blockPos().getZ());
        }));
        list.add(new CustomVariable("distance", () -> {
            LookedBlockData data = this.lastBlockData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distance());
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_stop_looking_at_block");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_stop_looking_at_block.desc"));
    }
}
