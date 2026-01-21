package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadResourcePacksAction extends Action {

    private static long lastTriggered = -1L;

    public ReloadResourcePacksAction() {
        super("reload_resource_packs");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(@Nullable String value) {

        // Block the action if called too fast (5 seconds cooldown after last call)
        long now = System.currentTimeMillis();
        if ((lastTriggered + 5000) > now) return;
        lastTriggered = now;

        Minecraft.getInstance().reloadResourcePacks();

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.reload_resource_packs");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.reload_resource_packs.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

}
