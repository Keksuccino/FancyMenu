package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnLeaveStructureHighPrecisionListener extends AbstractListener {

    @Nullable
    private String cachedStructureKey;

    public OnLeaveStructureHighPrecisionListener() {
        super("leave_structure_high_precision");
    }

    public void onStructureLeft(@Nullable String structureKey) {
        this.cachedStructureKey = structureKey;
        if ((structureKey != null) && !structureKey.isBlank()) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("structure_key", () -> {
            if (this.cachedStructureKey == null) {
                return "ERROR";
            }
            return this.cachedStructureKey;
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_leave_structure.high_precision");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_leave_structure.high_precision.desc"));
    }
}
