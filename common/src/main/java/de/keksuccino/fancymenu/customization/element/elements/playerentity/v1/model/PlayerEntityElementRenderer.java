package de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@SuppressWarnings("all")
public class PlayerEntityElementRenderer extends PlayerRenderer {

    public static final EntityModelSet ENTITY_MODEL_SET = Minecraft.getInstance().getEntityModels();
    private static final EntityRendererProvider.Context RENDER_CONTEXT = new EntityRendererProvider.Context(Minecraft.getInstance().getEntityRenderDispatcher(), Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().getEntityModels(), new EquipmentAssetManager(), Minecraft.getInstance().font);

    public final PlayerEntityProperties properties;
    public final PlayerEntityModel playerModel;
    public final PlayerRenderState renderState = new PlayerRenderState();

    public PlayerEntityElementRenderer(boolean slim) {
        super(RENDER_CONTEXT, slim);
        this.properties = new PlayerEntityProperties(slim);
        this.playerModel = new PlayerEntityModel(RENDER_CONTEXT.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim, this.properties);
//        this.addLayer(new PlayerEntityShoulderParrotLayer(this, RENDER_CONTEXT.getModelSet(), this.properties));
//        this.addLayer(new PlayerEntityCapeLayer(this, this.properties));
    }

    public void renderPlayerEntity(double d11, double d12, double d13, float f11, float f12, PoseStack pose, MultiBufferSource buffer, int i11) {
        try {
            Vec3 vec3 = this.getRenderOffset(null);
            double d2 = d11 + vec3.x();
            double d3 = d12 + vec3.y();
            double d0 = d13 + vec3.z();
            pose.pushPose();
            pose.translate(d2, d3, d0);
            this.render(f11, f12, pose, buffer, i11);
            pose.translate(-vec3.x(), -vec3.y(), -vec3.z());
            pose.popPose();
        } catch (Exception ex) {
            CrashReport crashreport = CrashReport.forThrowable(ex, "FancyMenu: Rendering 'Player Entity' element");
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Rotation", f11);
            crashreportcategory1.setDetail("Delta", f12);
            throw new ReportedException(crashreport);
        }
    }

    protected void render(float f11, float f12, PoseStack matrix, MultiBufferSource bufferSource, int i11) {
        this.setModelProperties();
        this.innerRender(f11, f12, matrix, bufferSource, i11);
    }

    protected void innerRender(float f11, float f12, PoseStack pose, MultiBufferSource bufferSource, int i11) {

        pose.pushPose();

        boolean shouldSit = this.properties.shouldSit;
        this.renderState.isBaby = this.properties.isBaby;
        float f = Mth.rotLerp(f12, this.properties.yBodyRotO, this.properties.yBodyRot);
        float f1 = Mth.rotLerp(f12, this.properties.yHeadRotO, this.properties.yHeadRot);
        float f2 = f1 - f;

        float f6 = Mth.lerp(f12, this.properties.xRotO, this.properties.xRot);

        if (this.properties.hasPose(Pose.SLEEPING)) {
            Direction direction = this.properties.getBedOrientation();
            if (direction != null) {
                float f4 = this.properties.getEyeHeight(Pose.STANDING) - 0.1F;
                pose.translate((float)(-direction.getStepX()) * f4, 0.0F, (float)(-direction.getStepZ()) * f4);
            }
        }

        float f7 = f12;
        this.setupRotations(pose, f7, f, f12);
        pose.scale(-1.0F, -1.0F, 1.0F);
        this.scale(pose, f12);
        pose.translate(0.0F, -1.501F, 0.0F);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit) {
            f8 = Mth.lerp(f12, this.properties.animationSpeedOld, this.properties.animationSpeed);
            f5 = this.properties.animationPosition - this.properties.animationSpeed * (1.0F - f12);
            if (this.properties.isBaby) {
                f5 *= 3.0F;
            }
            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }

        this.playerModel.setupAnimWithoutEntity(f5, f8, f7, f2, f6);
        boolean visible = !this.properties.invisible;
        boolean flag1 = false;
        boolean glowing = this.properties.glowing;
        RenderType rendertype = this.getRenderType(null, visible, flag1, glowing);
        if (rendertype != null) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(rendertype);
            int i = OverlayTexture.pack(OverlayTexture.u(this.getWhiteOverlayProgress(null)), OverlayTexture.v(false));
            this.playerModel.renderToBuffer(pose, vertexconsumer, i11, i);
        }
        if (!this.properties.isSpectator()) {
            for(RenderLayer<PlayerRenderState, PlayerModel> renderlayer : this.layers) {
                renderlayer.render(pose, bufferSource, i11, this.renderState, f5, f8);
            }
        }

        pose.popPose();

        if (this.properties.showDisplayName) {
            this.renderNameTag(null, this.properties.displayName, pose, bufferSource, i11);
        }

    }

    protected void scale(PoseStack matrix, float f11) {
        float f = 0.9375F;
        matrix.scale(0.9375F, 0.9375F, 0.9375F);
    }

    private void setModelProperties() {
        PlayerEntityModel playermodel = this.playerModel;
        if (this.properties.isSpectator()) {
            playermodel.setAllVisible(false);
            playermodel.head.visible = true;
            playermodel.hat.visible = true;
        } else {
            playermodel.setAllVisible(true);
            playermodel.hat.visible = this.properties.isModelPartShown(PlayerModelPart.HAT);
            playermodel.jacket.visible = this.properties.isModelPartShown(PlayerModelPart.JACKET);
            playermodel.leftPants.visible = this.properties.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            playermodel.rightPants.visible = this.properties.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            playermodel.leftSleeve.visible = this.properties.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            playermodel.rightSleeve.visible = this.properties.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            this.renderState.isCrouching = this.properties.isCrouching();
        }
    }

    @Override
    public Vec3 getRenderOffset(PlayerRenderState $$0) {
        return this.properties.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : Vec3.ZERO;
    }

    @Override
    protected @Nullable RenderType getRenderType(PlayerRenderState state, boolean visible, boolean isVisibleToPlayer, boolean glowing) {
        ResourceLocation resourcelocation = this.getTextureLocation(state);
        if (isVisibleToPlayer) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (visible) {
            return this.playerModel.renderType(resourcelocation);
        } else {
            return glowing ? RenderType.outline(resourcelocation) : null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerRenderState $$0) {
        return this.properties.getSkinTextureLocation();
    }

    @Override
    protected void renderNameTag(PlayerRenderState $$0, Component nameComponent, PoseStack pose, MultiBufferSource bufferSource, int i1) {
        boolean flag = !this.properties.isCrouching();
        float f = this.properties.getDimensions().height() + 0.5F;
        int i = 0;
        pose.pushPose();
        pose.translate(0.0F, f, 0.0F);
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = pose.last().pose();
        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int)(f1 * 255.0F) << 24;
        Font font = this.getFont();
        float f2 = (float)(-font.width(nameComponent) / 2);

        font.drawInBatch(nameComponent, f2, (float)i, 553648127, false, matrix4f, bufferSource, flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, j, i1);
        if (flag) {
            font.drawInBatch(nameComponent, f2, (float)i, -1, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, i1);
        }
        pose.popPose();
    }

    protected void setupRotations(PoseStack matrix, float f11, float f12, float f13) {
        if (!this.properties.hasPose(Pose.SLEEPING)) {
            matrix.mulPose(Axis.YP.rotationDegrees(180.0F - f12));
        }
    }

}