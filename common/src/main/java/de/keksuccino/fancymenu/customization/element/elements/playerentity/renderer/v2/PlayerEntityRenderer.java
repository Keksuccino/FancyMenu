package de.keksuccino.fancymenu.customization.element.elements.playerentity.renderer.v2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityRenderer extends PlayerRenderer {

    protected static final EntityRendererProvider.Context RENDER_CONTEXT = new EntityRendererProvider.Context(Minecraft.getInstance().getEntityRenderDispatcher(), Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().getEntityModels(), new EquipmentAssetManager(), Minecraft.getInstance().font);
    protected static final EntityAttachments ENTITY_ATTACHMENTS = EntityAttachments.builder().attach(EntityAttachment.NAME_TAG, 0.0F, 0.0F, 0.0F).build(0.0F, 0.0F);

    @NotNull
    public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
    public boolean isCrouching = false;
    public boolean isBaby = false;
    public boolean isGlowing = false;
    @Nullable
    public Parrot.Variant leftShoulderParrot = null;
    @Nullable
    public Parrot.Variant rightShoulderParrot = null;

    public float leftArmXRot = 0F;
    public float leftArmYRot = 0F;
    public float leftArmZRot = 0F;

    public float rightArmXRot = 0F;
    public float rightArmYRot = 0F;
    public float rightArmZRot = 0F;

    public float leftLegXRot = 0F;
    public float leftLegYRot = 0F;
    public float leftLegZRot = 0F;

    public float rightLegXRot = 0F;
    public float rightLegYRot = 0F;
    public float rightLegZRot = 0F;

    public float headXRot = 0F;
    public float headYRot = 0F;
    public float headZRot = 0F;

    public float bodyXRot = 0F;
    public float bodyYRot = 0F;

    public PlayerEntityRenderer(boolean slim) {

        super(RENDER_CONTEXT, slim);

        this.model = new PlayerModel(RENDER_CONTEXT.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim) {
            @Override
            public void setupAnim(@NotNull PlayerRenderState state) {
                super.setupAnim(state);
                PlayerEntityRenderer.this.updatePlayerProperties(state);
            }
        };

    }

    @SuppressWarnings("all")
    public void updatePlayerProperties(@NotNull PlayerRenderState state) {

        state.skin = this.skin;
        state.isCrouching = this.isCrouching;
        state.parrotOnLeftShoulder = this.leftShoulderParrot;
        state.parrotOnRightShoulder = this.rightShoulderParrot;
        state.nameTag = null;
        state.nameTagAttachment = null;
        state.customName = null;
        state.isBaby = this.isBaby;
        state.appearsGlowing = this.isGlowing;
        state.isSpectator = false;

        // X and Y rotations are switched for some reason

        this.model.leftArm.xRot = this.leftArmYRot;
        this.model.leftArm.yRot = this.leftArmXRot;
        this.model.leftArm.zRot = this.leftArmZRot;

        this.model.rightArm.xRot = this.rightArmYRot;
        this.model.rightArm.yRot = this.rightArmXRot;
        this.model.rightArm.zRot = this.rightArmZRot;

        this.model.leftLeg.xRot = this.leftLegYRot;
        this.model.leftLeg.yRot = this.leftLegXRot;
        this.model.leftLeg.zRot = this.leftLegZRot;

        this.model.rightLeg.xRot = this.rightLegYRot;
        this.model.rightLeg.yRot = this.rightLegXRot;
        this.model.rightLeg.zRot = this.rightLegZRot;

        this.model.root().xRot = this.bodyYRot;
        this.model.root().yRot = this.bodyXRot;

        this.model.head.xRot = this.headYRot;
        this.model.head.yRot = this.headXRot;
        this.model.head.zRot = this.headZRot;

    }

    @Override
    public void render(@NotNull PlayerRenderState state, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int i) {

        this.updatePlayerProperties(state);

        super.render(state, poseStack, bufferSource, i);

    }

}
