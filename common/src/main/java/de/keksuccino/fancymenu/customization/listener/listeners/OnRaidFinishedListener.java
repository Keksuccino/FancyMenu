package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnRaidFinishedListener extends AbstractListener {

    public static final String RESULT_VICTORY = "victory";
    public static final String RESULT_DEFEAT = "defeat";

    @Nullable
    private String cachedRaidResult;

    public OnRaidFinishedListener() {
        super("raid_finished");
    }

    public void onRaidFinished(boolean victory) {
        if (!this.hasInstancesListening()) {
            return;
        }
        this.cachedRaidResult = victory ? RESULT_VICTORY : RESULT_DEFEAT;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("raid_result", () -> (this.cachedRaidResult != null) ? this.cachedRaidResult : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_raid_finished");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_raid_finished.desc"));
    }

}
