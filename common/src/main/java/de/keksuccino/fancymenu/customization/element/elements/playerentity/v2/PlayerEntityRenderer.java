package de.keksuccino.fancymenu.customization.element.elements.playerentity.v2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityRenderer extends PlayerRenderer {

    @NotNull
    public volatile PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
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

    @Nullable
    public ParrotOnShoulderLayer parrotLayer;
    public PlayerRenderState playerState;

    private static final EntityRendererProvider.Context RENDER_CONTEXT = new EntityRendererProvider.Context(Minecraft.getInstance().getEntityRenderDispatcher(), Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().getEntityModels(), new EquipmentAssetManager(), Minecraft.getInstance().font);

    public PlayerEntityRenderer(boolean slim) {

        super(RENDER_CONTEXT, slim);

        // removing the parrot layer for now because broken
        this.layers.forEach(layer -> {
            if (layer instanceof ParrotOnShoulderLayer p) this.parrotLayer = p;
        });
        if (this.parrotLayer != null) this.layers.remove(this.parrotLayer);

        this.model = new PlayerModel(RENDER_CONTEXT.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim) {
            @Override
            public void setupAnim(@NotNull PlayerRenderState state) {
                super.setupAnim(state);
                updatePlayerProperties(state);
            }
        };

//        this.parrotLayer = new ParrotOnShoulderLayer(this, RENDER_CONTEXT.getModelSet()) {
//            @Override
//            public void render(PoseStack $$0, MultiBufferSource $$1, int $$2, PlayerRenderState $$3, float $$4, float $$5) {
//                updatePlayerProperties(playerState);
//                super.render($$0, $$1, $$2, $$3, $$4, $$5);
//            }
//        };
//        ((IMixinParrotOnShoulderLayer)this.parrotLayer).setModel_FancyMenu(new ParrotModel(RENDER_CONTEXT.getModelSet().bakeLayer(ModelLayers.PARROT)) {
//            @Override
//            public void setupAnim(@NotNull ParrotRenderState state) {
//                super.setupAnim(state);
//                getParrotModel().root().xRot = model.root().xRot;
//                getParrotModel().root().yRot = model.root().yRot;
//                getParrotModel().root().offsetPos(new Vector3f(0.0F, 45.0F, 0.0F));
//            }
//        });
//        this.addLayer(this.parrotLayer);

    }

//    @NotNull
//    public ParrotRenderState getParrotRenderState() {
//        Objects.requireNonNull(this.parrotLayer);
//        return ((IMixinParrotOnShoulderLayer)this.parrotLayer).getParrotState_FancyMenu();
//    }
//
//    @NotNull
//    public ParrotModel getParrotModel() {
//        Objects.requireNonNull(this.parrotLayer);
//        return ((IMixinParrotOnShoulderLayer)this.parrotLayer).getModel_FancyMenu();
//    }

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
        state.ageInTicks = 1000;
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.pose = this.isCrouching ? Pose.CROUCHING : Pose.STANDING;

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
