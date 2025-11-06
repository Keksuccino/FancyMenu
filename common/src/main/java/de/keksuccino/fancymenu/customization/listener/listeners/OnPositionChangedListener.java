package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OnPositionChangedListener extends AbstractListener {

    private boolean cachedPositionsValid;
    private int cachedOldX;
    private int cachedOldY;
    private int cachedOldZ;
    private int cachedNewX;
    private int cachedNewY;
    private int cachedNewZ;

    public OnPositionChangedListener() {
        super("position_changed");
    }

    public void onPositionChanged(@NotNull BlockPos oldPosition, @NotNull BlockPos newPosition) {
        this.cachedPositionsValid = true;
        this.cachedOldX = oldPosition.getX();
        this.cachedOldY = oldPosition.getY();
        this.cachedOldZ = oldPosition.getZ();
        this.cachedNewX = newPosition.getX();
        this.cachedNewY = newPosition.getY();
        this.cachedNewZ = newPosition.getZ();
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("old_pos_x", () -> this.cachedPositionsValid ? Integer.toString(this.cachedOldX) : "ERROR"));
        list.add(new CustomVariable("old_pos_y", () -> this.cachedPositionsValid ? Integer.toString(this.cachedOldY) : "ERROR"));
        list.add(new CustomVariable("old_pos_z", () -> this.cachedPositionsValid ? Integer.toString(this.cachedOldZ) : "ERROR"));
        list.add(new CustomVariable("new_pos_x", () -> this.cachedPositionsValid ? Integer.toString(this.cachedNewX) : "ERROR"));
        list.add(new CustomVariable("new_pos_y", () -> this.cachedPositionsValid ? Integer.toString(this.cachedNewY) : "ERROR"));
        list.add(new CustomVariable("new_pos_z", () -> this.cachedPositionsValid ? Integer.toString(this.cachedNewZ) : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_position_changed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_position_changed.desc"));
    }
}
