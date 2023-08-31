package de.keksuccino.fancymenu.customization.element.elements.playerentity.model;

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

    public void setupAnimWithoutEntity(float animationSpeed, float animationSpeedOld, float someFloatThatsAlways1, float headRotY, float headRotX) {
        this.setupAnimRaw(animationSpeed, animationSpeedOld, someFloatThatsAlways1, headRotY, headRotX);
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

    protected void setupAnimRaw(float animationSpeed, float animationSpeedOld, float someFloatThatsAlways1, float headRotY, float headRotX) {

        this.head.yRot = headRotY * ((float)Math.PI / 180F);
        this.head.xRot = headRotX * ((float)Math.PI / 180F);

        this.body.yRot = 0.0F;
        this.rightArm.z = 0.0F;
        this.rightArm.x = -5.0F;
        this.leftArm.z = 0.0F;
        this.leftArm.x = 5.0F;

        float f = 1.0F;

        //Arms X Rot
        this.leftArm.xRot = this.properties.leftArmX * ((float)Math.PI / 180F); // Mth.cos(animationSpeed * 0.6662F) * 2.0F * animationSpeedOld * 0.5F / f;
        this.rightArm.xRot = Mth.cos(animationSpeed * 0.6662F + (float)Math.PI) * 2.0F * animationSpeedOld * 0.5F / f;

        //Arms Y Rot
        this.leftArm.yRot = this.properties.leftArmY * ((float)Math.PI / 180F); // 0.0F;
        this.rightArm.yRot = 0.0F;

        //Arms Z Rot
        this.leftArm.zRot = this.properties.leftArmRotation * ((float)Math.PI / 180F); // 0.0F;
        this.rightArm.zRot = 0.0F;

        //Legs X Rot
        this.rightLeg.xRot = Mth.cos(animationSpeed * 0.6662F) * 1.4F * animationSpeedOld / f;
        this.leftLeg.xRot = Mth.cos(animationSpeed * 0.6662F + (float)Math.PI) * 1.4F * animationSpeedOld / f;

        //Legs Y Rot
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        //Legs Z Rot
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;

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
            AnimationUtils.bobModelPart(this.rightArm, someFloatThatsAlways1, 1.0F);
        }

        if (this.leftArmPose != HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.leftArm, someFloatThatsAlways1, -1.0F);
        }

        this.hat.copyFrom(this.head);

    }

}
