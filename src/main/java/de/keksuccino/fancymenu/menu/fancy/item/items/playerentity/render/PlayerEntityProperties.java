package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityProperties {

    public double xo;
    public double yo;
    public double zo;

    public float xRot;
    public float xRotO;
    public float yRot;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;

    public double xCloak;
    public double xCloakO;
    public double yCloak;
    public double yCloakO;
    public double zCloak;
    public double zCloakO;

    public float bob;
    public float oBob;

    public float animationSpeedOld;
    public float animationSpeed;
    public float animationPosition;
    public int tickCount = 1000;

    protected volatile ResourceLocation playerSkinLocation = DefaultPlayerSkin.getDefaultSkin();
    protected volatile ResourceLocation capeLocation = null;
    public boolean shouldSit = false;
    public boolean isBaby = false;
    public boolean crouching = false;
    public boolean spectator = false;
    public boolean invisible = false;
    /** not functional **/
    public boolean glowing = false;
    public boolean hasParrotOnShoulder = false;
    public int shoulderParrotVariant = 0;
    public boolean parrotOnLeftShoulder = false;
    public boolean showDisplayName = true;
    public Component displayName = new TextComponent("Steve");

    private final boolean slim;

    public PlayerEntityProperties(boolean slim) {
        this.slim = slim;
    }

    public boolean isSlim() {
        return this.slim;
    }

    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
    }

    public boolean isSpectator() {
        return this.spectator;
    }

    public boolean isCrouching() {
        return this.crouching;
    }

    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    public ItemStack getOffhandItem() {
        return ItemStack.EMPTY;
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        return ItemStack.EMPTY;
    }

    public InteractionHand getUsedItemHand() {
        return InteractionHand.MAIN_HAND;
    }

    public long getUseItemRemainingTicks() {
        return 0L;
    }

    public boolean hasPose(Pose pose) {
        return false;
    }

    public Direction getBedOrientation() {
        return Direction.SOUTH;
    }

    @NotNull
    public ResourceLocation getSkinTextureLocation() {
        if (this.playerSkinLocation == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        return this.playerSkinLocation;
    }

    public void setSkinTextureLocation(ResourceLocation loc) {
        this.playerSkinLocation = loc;
    }

    public float getEyeHeight(Pose pose) {
        return getEyeHeight(pose, this.getDimensions());
    }

    protected float getEyeHeight(Pose pose, EntityDimensions entityDimensions) {
        return pose == Pose.SLEEPING ? 0.2F : (entityDimensions.height * 0.85F);
    }

    @Nullable
    public ResourceLocation getCapeTextureLocation() {
        return this.capeLocation;
    }

    public void setCapeTextureLocation(ResourceLocation loc) {
        this.capeLocation = loc;
    }

    public EntityType getType() {
        return EntityType.PLAYER;
    }

    public EntityDimensions getDimensions() {
        return this.getType().getDimensions();
    }

}
