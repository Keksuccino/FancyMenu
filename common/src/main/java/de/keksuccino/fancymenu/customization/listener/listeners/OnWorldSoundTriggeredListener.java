package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnWorldSoundTriggeredListener extends AbstractListener {

    @Nullable
    private String cachedSoundIdentifier;
    @Nullable
    private String cachedSoundDisplayName;
    @Nullable
    private String cachedSoundOriginPosX;
    @Nullable
    private String cachedSoundOriginPosY;
    @Nullable
    private String cachedSoundOriginPosZ;
    @Nullable
    private String cachedSoundOriginDistanceToPlayer;
    @Nullable
    private String cachedSoundOriginDirectionFromPlayer;

    public OnWorldSoundTriggeredListener() {
        super("world_sound_triggered");
    }

    public void onWorldSoundTriggered(@NotNull SoundInstance sound, @Nullable Component subtitle, float audibleRange) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if ((minecraft.level == null) || (player == null)) {
            return;
        }
        if (sound.isRelative()) {
            return;
        }
        if (!Float.isFinite(audibleRange)) {
            return;
        }

        Vec3 soundPosition = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        Vec3 playerPosition = player.position();
        double distanceToPlayer = soundPosition.distanceTo(playerPosition);

        this.cachedSoundIdentifier = this.resolveSoundResourceLocation(sound);
        this.cachedSoundDisplayName = this.resolveDisplayName(subtitle, this.cachedSoundResourceLocation);
        this.cachedSoundOriginPosX = Double.toString(soundPosition.x);
        this.cachedSoundOriginPosY = Double.toString(soundPosition.y);
        this.cachedSoundOriginPosZ = Double.toString(soundPosition.z);
        this.cachedSoundOriginDistanceToPlayer = Double.toString(distanceToPlayer);
        this.cachedSoundOriginDirectionFromPlayer = Double.toString(this.calculateDirectionDegrees(player, soundPosition));

        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("sound_resource_location", () -> this.cachedSoundIdentifier != null ? this.cachedSoundIdentifier : "ERROR"));
        list.add(new CustomVariable("sound_display_name", () -> this.cachedSoundDisplayName != null ? this.cachedSoundDisplayName : "UNKNOWN"));
        list.add(new CustomVariable("sound_origin_pos_x", () -> this.cachedSoundOriginPosX != null ? this.cachedSoundOriginPosX : "0"));
        list.add(new CustomVariable("sound_origin_pos_y", () -> this.cachedSoundOriginPosY != null ? this.cachedSoundOriginPosY : "0"));
        list.add(new CustomVariable("sound_origin_pos_z", () -> this.cachedSoundOriginPosZ != null ? this.cachedSoundOriginPosZ : "0"));
        list.add(new CustomVariable("sound_origin_distance_to_player", () -> this.cachedSoundOriginDistanceToPlayer != null ? this.cachedSoundOriginDistanceToPlayer : "0"));
        list.add(new CustomVariable("sound_origin_direction_from_player", () -> this.cachedSoundOriginDirectionFromPlayer != null ? this.cachedSoundOriginDirectionFromPlayer : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_world_sound_triggered");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_world_sound_triggered.desc"));
    }

    @Nullable
    private String resolveSoundResourceLocation(@NotNull SoundInstance sound) {
        Sound resolvedSound = sound.getSound();
        if ((resolvedSound != null) && (resolvedSound != SoundManager.EMPTY_SOUND) && (resolvedSound != SoundManager.INTENTIONALLY_EMPTY_SOUND)) {
            Identifier path = resolvedSound.getPath();
            if (path != null) {
                return path.toString();
            }
            Identifier directLocation = resolvedSound.getLocation();
            if (directLocation != null) {
                return directLocation.toString();
            }
        }
        Identifier fallback = sound.getLocation();
        return (fallback != null) ? fallback.toString() : null;
    }

    @NotNull
    private String resolveDisplayName(@Nullable Component subtitle, @Nullable String fallback) {
        if (subtitle != null) {
            String plain = subtitle.getString();
            if (!plain.isBlank()) {
                return plain;
            }
        }
        if ((fallback != null) && !fallback.isBlank()) {
            return fallback;
        }
        return "UNKNOWN";
    }

    private double calculateDirectionDegrees(@NotNull LocalPlayer player, @NotNull Vec3 soundPosition) {
        Vec3 playerPosition = player.position();
        double deltaX = soundPosition.x - playerPosition.x;
        double deltaZ = soundPosition.z - playerPosition.z;
        if ((Math.abs(deltaX) < 1.0E-6D) && (Math.abs(deltaZ) < 1.0E-6D)) {
            return 0.0D;
        }
        double angleToSound = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D;
        double relative = Mth.wrapDegrees(angleToSound - player.getYRot());
        double normalized = relative % 360.0D;
        if (normalized < 0.0D) {
            normalized += 360.0D;
        }
        return normalized;
    }
}


