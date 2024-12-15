package de.keksuccino.fancymenu.customization;

import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v2.textures.SkinResourceSupplier;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEntity;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinLivingEntity;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public class DummyLocalPlayer extends LocalPlayer {

    public PlayerInfo playerInfo;
    public PlayerSkin skin;
    public Component displayName;
    public boolean glowing = false;

    public DummyLocalPlayer() {
        super(null, null, null, null, null, false, false);
        this.initializeInstance(); // this never gets called here because I use Unsafe to construct the instance
    }

    public void initializeInstance() {

        ((IMixinEntity)this).setDimensions_FancyMenu(EntityType.PLAYER.getDimensions());
        ((IMixinEntity)this).setPosition_FancyMenu(Vec3.ZERO);
        ((IMixinLivingEntity)this).setWalkAnimation_FancyMenu(new WalkAnimationState());
        ((IMixinLivingEntity)this).setElytraAnimationState_FancyMenu(new ElytraAnimationState(this));

        this.playerInfo = new PlayerInfo(new GameProfile(UUID.randomUUID(), "Steve"), false);
        this.skin = new PlayerSkin(SkinResourceSupplier.DEFAULT_SKIN_LOCATION, null, null, null, PlayerSkin.Model.WIDE, false);
        this.displayName = Component.literal("Steve");

    }

    @Override
    public void tick() {
    }

    @Override
    public void aiStep() {
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return this.glowing;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isInvisibleTo(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return false;
    }

    @Override
    public @NotNull Pose getPose() {
        return Pose.STANDING;
    }

    @Override
    public @NotNull Optional<BlockPos> getSleepingPos() {
        return Optional.of(new BlockPos(0,0,0));
    }

    @Override
    public @Nullable Direction getBedOrientation() {
        return Direction.EAST;
    }

    @Override
    public boolean isFallFlying() {
        return false;
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return new ItemStack(Items.AIR);
    }

    @Override
    public @NotNull ItemStack getUseItem() {
        return new ItemStack(Items.AIR);
    }

    @Override
    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isFullyFrozen() {
        return false;
    }

    @Override
    public boolean isAutoSpinAttack() {
        return false;
    }

    @Override
    public @Nullable PlayerTeam getTeam() {
        return null;
    }

    @Override
    public boolean isVehicle() {
        return false;
    }

    @Override
    protected @Nullable PlayerInfo getPlayerInfo() {
        return this.playerInfo;
    }

    @Override
    public @NotNull PlayerSkin getSkin() {
        return this.skin;
    }

    @Override
    public @NotNull Component getName() {
        return this.displayName;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public @Nullable Component getCustomName() {
        return null;
    }

    @Override
    public float getHealth() {
        return 3.0F;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public @NotNull EntityType<?> getType() {
        return EntityType.PLAYER;
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

}