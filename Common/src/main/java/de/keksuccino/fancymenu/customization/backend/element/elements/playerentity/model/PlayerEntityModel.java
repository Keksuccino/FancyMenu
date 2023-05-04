package de.keksuccino.fancymenu.customization.backend.element.elements.playerentity.model;

import de.keksuccino.fancymenu.mixin.mixins.client.IMixinPlayerModel;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class PlayerEntityModel extends PlayerModel {

    public final PlayerEntityProperties properties;

    public PlayerEntityModel(ModelPart modelPart, boolean slim, PlayerEntityProperties properties) {
        super(modelPart, slim);
        this.properties = properties;
    }

    public void setupAnimWithoutEntity(float p_103396_, float p_103397_, float p_103398_, float p_103399_, float p_103400_) {
        this.setupAnimRaw(p_103396_, p_103397_, p_103398_, p_103399_, p_103400_);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
        if (this.properties.isCrouching()) {
            ((IMixinPlayerModel)this).getCloakFancyMenu().z = 1.4F;
            ((IMixinPlayerModel)this).getCloakFancyMenu().y = 1.85F;
        } else {
            ((IMixinPlayerModel)this).getCloakFancyMenu().z = 0.0F;
            ((IMixinPlayerModel)this).getCloakFancyMenu().y = 0.0F;
        }
    }

    protected void setupAnimRaw(float p_102867_, float p_102868_, float p_102869_, float p_102870_, float p_102871_) {

        this.head.yRot = p_102870_ * ((float)Math.PI / 180F);
        this.head.xRot = p_102871_ * ((float)Math.PI / 180F);

        this.body.yRot = 0.0F;
        this.rightArm.z = 0.0F;
        this.rightArm.x = -5.0F;
        this.leftArm.z = 0.0F;
        this.leftArm.x = 5.0F;
        float f = 1.0F;

        if (f < 1.0F) {
            f = 1.0F;
        }

        this.rightArm.xRot = Mth.cos(p_102867_ * 0.6662F + (float)Math.PI) * 2.0F * p_102868_ * 0.5F / f;
        this.leftArm.xRot = Mth.cos(p_102867_ * 0.6662F) * 2.0F * p_102868_ * 0.5F / f;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.xRot = Mth.cos(p_102867_ * 0.6662F) * 1.4F * p_102868_ / f;
        this.leftLeg.xRot = Mth.cos(p_102867_ * 0.6662F + (float)Math.PI) * 1.4F * p_102868_ / f;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;
        if (this.riding) {
            this.rightArm.xRot += (-(float)Math.PI / 5F);
            this.leftArm.xRot += (-(float)Math.PI / 5F);
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = ((float)Math.PI / 10F);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = (-(float)Math.PI / 10F);
            this.leftLeg.zRot = -0.07853982F;
        }

        this.leftArm.yRot = 0.0F;
        this.rightArm.yRot = 0.0F;

        if (this.crouching) {
            this.body.xRot = 0.5F;
            this.rightArm.xRot += 0.4F;
            this.leftArm.xRot += 0.4F;
            this.rightLeg.z = 4.0F;
            this.leftLeg.z = 4.0F;
            this.rightLeg.y = 12.2F;
            this.leftLeg.y = 12.2F;
            this.head.y = 4.2F;
            this.body.y = 3.2F;
            this.leftArm.y = 5.2F;
            this.rightArm.y = 5.2F;
        } else {
            this.body.xRot = 0.0F;
            this.rightLeg.z = 0.1F;
            this.leftLeg.z = 0.1F;
            this.rightLeg.y = 12.0F;
            this.leftLeg.y = 12.0F;
            this.head.y = 0.0F;
            this.body.y = 0.0F;
            this.leftArm.y = 2.0F;
            this.rightArm.y = 2.0F;
        }

        if (this.rightArmPose != HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.rightArm, p_102869_, 1.0F);
        }

        if (this.leftArmPose != HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.leftArm, p_102869_, -1.0F);
        }

        this.hat.copyFrom(this.head);

    }

}
