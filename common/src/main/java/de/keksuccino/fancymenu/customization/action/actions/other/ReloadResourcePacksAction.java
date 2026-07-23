package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadResourcePacksAction extends Action {

    static final long RELOAD_COOLDOWN_MILLIS = 5000L;
    private static final ReloadCooldown RELOAD_COOLDOWN = new ReloadCooldown(RELOAD_COOLDOWN_MILLIS);

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
        triggerReload();
    }

    public static void triggerReload() {
        if (!RELOAD_COOLDOWN.tryTrigger(System.currentTimeMillis())) return;

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

    /**
     * Isolates the time-only part of the reload guard from the Minecraft call. The caller-provided wall-clock timestamp
     * preserves the existing runtime behavior while allowing the strict cooldown boundary to be tested without a client.
     */
    static final class ReloadCooldown {

        private final long durationMillis;
        private long lastTriggered = -1L;

        ReloadCooldown(long durationMillis) {
            this.durationMillis = durationMillis;
        }

        boolean tryTrigger(long now) {
            if ((this.lastTriggered + this.durationMillis) > now) return false;
            this.lastTriggered = now;
            return true;
        }

    }

}
