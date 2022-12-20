package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.layers.PlayerEntityCapeLayer;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.layers.PlayerEntityRenderLayer;
import de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.render.layers.PlayerEntityShoulderParrotLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerEntityItemRenderer extends PlayerRenderer {

//    private static final EntityModelSet ENTITY_MODEL_SET = Minecraft.getInstance().getEntityModels();
    private static final EntityRendererManager RENDER_CONTEXT = new EntityRendererManager(Minecraft.getInstance().textureManager, Minecraft.getInstance().getItemRenderer(), (IReloadableResourceManager) Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().font, Minecraft.getInstance().options);

    public final PlayerEntityProperties properties;
    public final PlayerEntityModel playerModel;

    public PlayerEntityItemRenderer(boolean slim) {
        super(RENDER_CONTEXT, slim);
        this.properties = new PlayerEntityProperties(slim);
        this.playerModel = new PlayerEntityModel(0.0F, slim, this.properties);
        this.addLayer(new PlayerEntityShoulderParrotLayer(this, this.properties));
        this.addLayer(new PlayerEntityCapeLayer(this, this.properties));
    }

    public void renderPlayerEntityItem(double d11, double d12, double d13, float f11, float f12, MatrixStack matrix, IRenderTypeBuffer bufferSource, int i11) {
        try {
            Vector3d vec3 = this.getRenderOffset(null, f12);
            double d2 = d11 + vec3.x();
            double d3 = d12 + vec3.y();
            double d0 = d13 + vec3.z();
            matrix.pushPose();
            matrix.translate(d2, d3, d0);
            this.render(f11, f12, matrix, bufferSource, i11);
            matrix.translate(-vec3.x(), -vec3.y(), -vec3.z());
            matrix.popPose();
        } catch (Exception ex) {
            CrashReport crashreport = CrashReport.forThrowable(ex, "FancyMenu: Rendering player entity item");
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Rotation", f11);
            crashreportcategory1.setDetail("Delta", f12);
            throw new ReportedException(crashreport);
        }
    }

    protected void render(float f11, float f12, MatrixStack matrix, IRenderTypeBuffer bufferSource, int i11) {
        this.setModelProperties();
        this.innerRender(f11, f12, matrix, bufferSource, i11);
    }

    protected void innerRender(float f11, float f12, MatrixStack matrix, IRenderTypeBuffer bufferSource, int i11) {

        matrix.pushPose();

        boolean shouldSit = this.properties.shouldSit;
        this.playerModel.riding = shouldSit;
        this.playerModel.young = this.properties.isBaby;
        float f = MathHelper.rotLerp(f12, this.properties.yBodyRotO, this.properties.yBodyRot);
        float f1 = MathHelper.rotLerp(f12, this.properties.yHeadRotO, this.properties.yHeadRot);
        float f2 = f1 - f;

        float f6 = MathHelper.lerp(f12, this.properties.xRotO, this.properties.xRot);

        if (this.properties.hasPose(Pose.SLEEPING)) {
            Direction direction = this.properties.getBedOrientation();
            if (direction != null) {
                float f4 = this.properties.getEyeHeight(Pose.STANDING) - 0.1F;
                matrix.translate((float)(-direction.getStepX()) * f4, 0.0F, (float)(-direction.getStepZ()) * f4);
            }
        }

        float f7 = f12;
        this.setupRotations(matrix, f7, f, f12);
        matrix.scale(-1.0F, -1.0F, 1.0F);
        this.scale(matrix, f12);
        matrix.translate(0.0F, -1.501F, 0.0F);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit) {
            f8 = MathHelper.lerp(f12, this.properties.animationSpeedOld, this.properties.animationSpeed);
            f5 = this.properties.animationPosition - this.properties.animationSpeed * (1.0F - f12);
            if (this.properties.isBaby) {
                f5 *= 3.0F;
            }
            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }

        this.playerModel.setupAnimWithoutEntity(f5, f8, f7, f2, f6);
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = !this.properties.invisible;
        boolean flag1 = false;
        boolean glowing = this.properties.glowing;
        RenderType rendertype = this.getRenderType(null, flag, flag1, glowing);
        if (rendertype != null) {
            IVertexBuilder vertexconsumer = bufferSource.getBuffer(rendertype);
            int i = OverlayTexture.pack(OverlayTexture.u(this.getWhiteOverlayProgress(null, f12)), OverlayTexture.v(false));
            this.playerModel.renderToBuffer(matrix, vertexconsumer, i11, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
        }

        if (!this.properties.isSpectator()) {
            for(LayerRenderer renderlayer : this.layers) {
                if (renderlayer instanceof PlayerEntityRenderLayer) {
                    renderlayer.render(matrix, bufferSource, i11, null, f5, f8, f12, f7, f2, f6);
                }
            }
        }

        matrix.popPose();

        if (this.properties.showDisplayName) {
            this.renderNameTag(null, this.properties.displayName, matrix, bufferSource, i11);
        }

    }

    protected void scale(MatrixStack matrix, float f11) {
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
            playermodel.crouching = this.properties.isCrouching();
            BipedModel.ArmPose humanoidmodel$armpose = getArmPose(this.properties, Hand.MAIN_HAND);
            BipedModel.ArmPose humanoidmodel$armpose1 = getArmPose(this.properties, Hand.OFF_HAND);
            if (humanoidmodel$armpose.isTwoHanded()) {
                humanoidmodel$armpose1 = this.properties.getOffhandItem().isEmpty() ? BipedModel.ArmPose.EMPTY : BipedModel.ArmPose.ITEM;
            }

            if (this.properties.getMainArm() == HandSide.RIGHT) {
                playermodel.rightArmPose = humanoidmodel$armpose;
                playermodel.leftArmPose = humanoidmodel$armpose1;
            } else {
                playermodel.rightArmPose = humanoidmodel$armpose1;
                playermodel.leftArmPose = humanoidmodel$armpose;
            }
        }
    }

    private static BipedModel.ArmPose getArmPose(PlayerEntityProperties props, Hand interactionHand) {
        ItemStack itemstack = props.getItemInHand(interactionHand);
        if (itemstack.isEmpty()) {
            return BipedModel.ArmPose.EMPTY;
        } else {
            if (props.getUsedItemHand() == interactionHand && props.getUseItemRemainingTicks() > 0) {
                UseAction useanim = itemstack.getUseAnimation();
                if (useanim == UseAction.BLOCK) {
                    return BipedModel.ArmPose.BLOCK;
                }
                if (useanim == UseAction.BOW) {
                    return BipedModel.ArmPose.BOW_AND_ARROW;
                }
                if (useanim == UseAction.SPEAR) {
                    return BipedModel.ArmPose.THROW_SPEAR;
                }
                if (useanim == UseAction.CROSSBOW && interactionHand == props.getUsedItemHand()) {
                    return BipedModel.ArmPose.CROSSBOW_CHARGE;
                }
            } else if (itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
                return BipedModel.ArmPose.CROSSBOW_HOLD;
            }
            return BipedModel.ArmPose.ITEM;
        }
    }

    @Override
    public Vector3d getRenderOffset(AbstractClientPlayerEntity p_225627_1_, float p_225627_2_) {
        return this.properties.isCrouching() ? new Vector3d(0.0D, -0.125D, 0.0D) : Vector3d.ZERO;
    }

    @Override
    @Nullable
    protected RenderType getRenderType(@Nullable AbstractClientPlayerEntity entity, boolean visible, boolean isVisibleToPlayer, boolean glowing) {
        ResourceLocation resourcelocation = this.getTextureLocation(entity);
        if (isVisibleToPlayer) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (visible) {
            return this.playerModel.renderType(resourcelocation);
        } else {
            return glowing ? RenderType.outline(resourcelocation) : null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(@Nullable AbstractClientPlayerEntity entity) {
        return this.properties.getSkinTextureLocation();
    }
    
    @Override
    protected void renderNameTag(@Nullable AbstractClientPlayerEntity entity, ITextComponent content, MatrixStack matrix, IRenderTypeBuffer bufferSource, int p_114502_) {
        boolean flag = !this.properties.isCrouching();
        float f = this.properties.getDimensions().height + 0.5F;
        int i = 0;
        matrix.pushPose();
        matrix.translate(0.0F, f, 0.0F);
        matrix.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrix.last().pose();
        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int)(f1 * 255.0F) << 24;
        FontRenderer font = this.getFont();
        float f2 = (float)(-font.width(content) / 2);
        font.drawInBatch(content, f2, (float)i, 553648127, false, matrix4f, bufferSource, flag, j, p_114502_);
        if (flag) {
            font.drawInBatch(content, f2, (float)i, -1, false, matrix4f, bufferSource, false, 0, p_114502_);
        }
        matrix.popPose();
    }

    protected void setupRotations(MatrixStack matrix, float f11, float f12, float f13) {
        if (!this.properties.hasPose(Pose.SLEEPING)) {
            matrix.mulPose(Vector3f.YP.rotationDegrees(180.0F - f12));
        }
    }

}
