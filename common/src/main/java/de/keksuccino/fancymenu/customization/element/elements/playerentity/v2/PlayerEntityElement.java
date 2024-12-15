package de.keksuccino.fancymenu.customization.element.elements.playerentity.v2;

import com.mojang.blaze3d.platform.Lighting;
import de.keksuccino.fancymenu.customization.DummyLocalPlayer;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v2.textures.CapeResourceSupplier;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v2.textures.SkinResourceSupplier;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinEntity;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sun.misc.Unsafe;

import java.awt.*;
import java.lang.reflect.Field;

public class PlayerEntityElement extends AbstractElement {
    
    private static final Logger LOGGER = LogManager.getLogger();

//    private static final Camera CAMERA = new Camera();

    protected DrawableColor nameTagBackgroundColor = DrawableColor.BLACK;

    protected final PlayerRenderState renderState = new PlayerRenderState();
    public final PlayerEntityRenderer normalRenderer = new PlayerEntityRenderer(false);
    public final PlayerEntityRenderer slimRenderer = new PlayerEntityRenderer(true);

    public volatile boolean copyClientPlayer = false;
    @NotNull
    public volatile String playerName = "Steve";
    public boolean showPlayerName = true;
    public boolean hasParrotOnShoulder = false;
    public boolean parrotOnLeftShoulder = false;
    public boolean crouching = false;
    public boolean isBaby = false;
    public String scale = "30"; //unused
    public boolean headFollowsMouse = true;
    public boolean bodyFollowsMouse = true;
    public volatile boolean slim = true;
    public volatile boolean autoSkin = false;
    public volatile boolean autoCape = false;
    @Nullable
    public SkinResourceSupplier skinTextureSupplier;
    @Nullable
    public CapeResourceSupplier capeTextureSupplier;
    public String bodyXRot;
    public String bodyYRot;
    public String headXRot;
    public String headYRot;
    public String headZRot;
    public String leftArmXRot;
    public String leftArmYRot;
    public String leftArmZRot;
    public String rightArmXRot;
    public String rightArmYRot;
    public String rightArmZRot;
    public String leftLegXRot;
    public String leftLegYRot;
    public String leftLegZRot;
    public String rightLegXRot;
    public String rightLegYRot;
    public String rightLegZRot;
    public boolean bodyXRotAdvancedMode;
    public boolean bodyYRotAdvancedMode;
    public boolean headXRotAdvancedMode;
    public boolean headYRotAdvancedMode;
    public boolean headZRotAdvancedMode;
    public boolean leftArmXRotAdvancedMode;
    public boolean leftArmYRotAdvancedMode;
    public boolean leftArmZRotAdvancedMode;
    public boolean rightArmXRotAdvancedMode;
    public boolean rightArmYRotAdvancedMode;
    public boolean rightArmZRotAdvancedMode;
    public boolean leftLegXRotAdvancedMode;
    public boolean leftLegYRotAdvancedMode;
    public boolean leftLegZRotAdvancedMode;
    public boolean rightLegXRotAdvancedMode;
    public boolean rightLegYRotAdvancedMode;
    public boolean rightLegZRotAdvancedMode;

//    public PlayerSkinWidget widget = new PlayerSkinWidget(0, 0, Minecraft.getInstance().getEntityModels(), this::updateSkinAndCape);
//
//    public final DummyLocalPlayer player = createUnsafePlayer();

    public PlayerEntityElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.normalRenderer.playerState = this.renderState;
        this.slimRenderer.playerState = this.renderState;
//        if (this.player != null) {
//            this.player.initializeInstance();
//        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            graphics.fill(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), Color.RED.getRGB());

            graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("PLAYER ENTITY ELEMENT"), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + (this.getAbsoluteHeight() / 2) - 11, -1);
            graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("TEMPORARILY REMOVED FROM FANCYMENU!"), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + (this.getAbsoluteHeight() / 2), -1);
            graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("WILL GET ADDED BACK SOON!"), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + (this.getAbsoluteHeight() / 2) + 11, -1);

//            this.updateSkinAndCape();
//            this.updateParrotOnShoulder();
//            this.updatePose(mouseX, mouseY);
//
//            renderEntityInInventoryFollowsMouse(graphics, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), 30, 0.0625F, mouseX, mouseY, this.player);

//            this.renderEntity(graphics);

//            this.widget.setX(this.getAbsoluteX());
//            this.widget.setY(this.getAbsoluteY());
//            this.widget.setWidth(this.getAbsoluteWidth());
//            this.widget.setHeight(this.getAbsoluteHeight());
//            this.widget.render(graphics, mouseX, mouseY, partial);

//            this.renderNameTag(graphics, mouseX, mouseY, partial);

        }

    }

//    @Nullable
//    public static DummyLocalPlayer createUnsafePlayer() {
//
//        try {
//
//            // Access the Unsafe instance
//            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
//            unsafeField.setAccessible(true);
//            Unsafe unsafe = (Unsafe) unsafeField.get(null);
//
//            // Create an instance of the class without invoking its constructor
//            DummyLocalPlayer instance = (DummyLocalPlayer) unsafe.allocateInstance(DummyLocalPlayer.class);
//
//            return instance;
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return null;
//
//    }
//
//    public static void renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int scale, float yOffset, float mouseX, float mouseY, LocalPlayer localPlayer) {
//        float f = (float)(x1 + x2) / 2.0F;
//        float g = (float)(y1 + y2) / 2.0F;
//        guiGraphics.enableScissor(x1, y1, x2, y2);
//        float h = (float)Math.atan((double)((f - mouseX) / 40.0F));
//        float i = (float)Math.atan((double)((g - mouseY) / 40.0F));
//        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
//        Quaternionf quaternionf2 = new Quaternionf().rotateX(i * 20.0F * (float) (Math.PI / 180.0));
//        quaternionf.mul(quaternionf2);
//        float j = localPlayer.yBodyRot;
//        float k = localPlayer.getYRot();
//        float l = localPlayer.getXRot();
//        float m = localPlayer.yHeadRotO;
//        float n = localPlayer.yHeadRot;
//        localPlayer.yBodyRot = 180.0F + h * 20.0F;
//        localPlayer.setYRot(180.0F + h * 40.0F);
//        localPlayer.setXRot(-i * 20.0F);
//        localPlayer.yHeadRot = localPlayer.getYRot();
//        localPlayer.yHeadRotO = localPlayer.getYRot();
//        float o = localPlayer.getScale();
//        Vector3f vector3f = new Vector3f(0.0F, localPlayer.getBbHeight() / 2.0F + yOffset * o, 0.0F);
//        float p = (float)scale / o;
//        renderEntityInInventory(guiGraphics, f, g, p, vector3f, quaternionf, quaternionf2, localPlayer);
//        localPlayer.yBodyRot = j;
//        localPlayer.setYRot(k);
//        localPlayer.setXRot(l);
//        localPlayer.yHeadRotO = m;
//        localPlayer.yHeadRot = n;
//        guiGraphics.disableScissor();
//    }
//
//    public static void renderEntityInInventory(GuiGraphics guiGraphics, float x, float y, float scale, Vector3f translate, Quaternionf pose, @Nullable Quaternionf cameraOrientation, LocalPlayer localPlayer) {
//
//        LocalPlayer cachedPlayer = Minecraft.getInstance().player;
//        Minecraft.getInstance().player = localPlayer;
//
//        try {
//            guiGraphics.pose().pushPose();
//            guiGraphics.pose().translate((double)x, (double)y, 50.0);
//            guiGraphics.pose().scale(scale, scale, -scale);
//            guiGraphics.pose().translate(translate.x, translate.y, translate.z);
//            guiGraphics.pose().mulPose(pose);
//            guiGraphics.flush();
//            Lighting.setupForEntityInInventory();
//            EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
//            if (entityRenderDispatcher.camera == null) entityRenderDispatcher.camera = CAMERA;
//            if (cameraOrientation != null) {
//                entityRenderDispatcher.overrideCameraOrientation(cameraOrientation.conjugate(new Quaternionf()).rotateY((float) Math.PI));
//            }
//            entityRenderDispatcher.setRenderShadow(false);
//            guiGraphics.drawSpecial(multiBufferSource -> entityRenderDispatcher.render(localPlayer, 0.0, 0.0, 0.0, 1.0F, guiGraphics.pose(), multiBufferSource, 15728880));
//            guiGraphics.flush();
//            entityRenderDispatcher.setRenderShadow(true);
//            guiGraphics.pose().popPose();
//            Lighting.setupFor3DItems();
//        } catch (Exception ex) {
//            LOGGER.error("[FANCYMENU] Failed to render Player Entity element!", ex);
//        }
//
//        Minecraft.getInstance().player = cachedPlayer;
//
//    }

    protected void renderEntity(@NotNull GuiGraphics graphics) {

        float scale = (float)this.getAbsoluteHeight() / 2.125F;

        graphics.pose().pushPose();
        graphics.pose().translate((float)this.getAbsoluteX() + (float)this.getAbsoluteWidth() / 2.0F, (float)(this.getAbsoluteY() + this.getAbsoluteHeight()), 100.0F);
        graphics.pose().scale(scale, scale, scale);
        graphics.pose().translate(0.0F, -0.0625F, 0.0F);
        graphics.flush();
        Lighting.setupForEntityInInventory();

        graphics.pose().pushPose();
        graphics.pose().scale(1.0F, 1.0F, -1.0F);
        graphics.pose().translate(0.0F, -(1.501F + (1.501F / 2.0F)), 0.0F);
        graphics.drawSpecial(multiBufferSource -> this.getActiveRenderer().render(this.renderState, graphics.pose(), multiBufferSource, 15728880));
        graphics.pose().popPose();

        graphics.flush();
        Lighting.setupFor3DItems();
        graphics.pose().popPose();

    }

    protected void renderNameTag(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.showPlayerName) return;

        Component name = this.getDisplayName();
        Font font = Minecraft.getInstance().font;

        float scale = 1.2F;
        int textWidth = (int) (font.width(name) * scale);
        int textHeight = (int) (font.lineHeight * scale);
        int headHeight = (int) ((float)this.getAbsoluteHeight() * 0.30F); // head height is roughly 30% of the element height
        int nameTagOffsetY = 30 + (headHeight / 2);
        int nameTagWidth = textWidth + 10;
        int nameTagHeight = textHeight + 6;
        int nameTagX = this.getAbsoluteX() + (this.getAbsoluteWidth() / 2) - (nameTagWidth / 2);
        int nameTagY = this.getAbsoluteY() + (this.getAbsoluteHeight() / 2) - nameTagHeight - nameTagOffsetY;

        graphics.fill(nameTagX, nameTagY, nameTagX + nameTagWidth, nameTagY + nameTagHeight, this.nameTagBackgroundColor.getColorIntWithAlpha(0.3F));

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);

        graphics.drawString(font, name, (int)(nameTagX / scale) + 5, (int)(nameTagY / scale) + 3, -1, true);

        graphics.pose().popPose();

    }

    @Override
    public @NotNull Component getDisplayName() {
        return buildComponent(this.playerName);
    }

    protected float stringToFloat(@Nullable String s) {
        if (s == null) return 0.0F;
        s = PlaceholderParser.replacePlaceholders(s);
        s = s.replace(" ", "");
        try {
            return Float.parseFloat(s);
        } catch (Exception ignore) {}
        return 0.0F;
    }

    public void setCopyClientPlayer(boolean copyClientPlayer) {
        if (copyClientPlayer) {
            this.copyClientPlayer = true;
            this.autoCape = false;
            this.autoSkin = false;
            this.slim = false;
            this.setPlayerName(Minecraft.getInstance().getUser().getName());
            this.setSkinByPlayerName();
            this.setCapeByPlayerName();
        } else {
            this.copyClientPlayer = false;
            this.skinTextureSupplier = null;
            this.capeTextureSupplier = null;
        }
    }

    public void setPlayerName(@Nullable String playerName) {
        if (playerName == null) {
            playerName = "Steve";
        }
        this.playerName = playerName;
    }

    public void setShowPlayerName(boolean showName) {
        this.showPlayerName = showName;
    }

    public void setHasParrotOnShoulder(boolean hasParrot, boolean onLeftShoulder) {
        this.hasParrotOnShoulder = hasParrot;
        this.parrotOnLeftShoulder = onLeftShoulder;
        this.updateParrotOnShoulder();
    }

    public void updateParrotOnShoulder() {
        Parrot.Variant variantLeft = this.parrotOnLeftShoulder ? Parrot.Variant.RED_BLUE : null;
        Parrot.Variant variantRight = this.parrotOnLeftShoulder ? null : Parrot.Variant.RED_BLUE;
        if (!this.hasParrotOnShoulder) {
            variantLeft = null;
            variantRight = null;
        }
        this.normalRenderer.leftShoulderParrot = variantLeft;
        this.normalRenderer.rightShoulderParrot = variantRight;
        this.slimRenderer.leftShoulderParrot = variantLeft;
        this.slimRenderer.rightShoulderParrot = variantRight;
    }

    public void setCrouching(boolean crouching) {
        this.crouching = crouching;
        this.normalRenderer.isCrouching = crouching;
        this.slimRenderer.isCrouching = crouching;
    }

    public void setIsBaby(boolean isBaby) {
        this.isBaby = isBaby;
        this.normalRenderer.isBaby = isBaby;
        this.slimRenderer.isBaby = isBaby;
    }

    public void setCapeByPlayerName() {
        this.capeTextureSupplier = new CapeResourceSupplier(this.playerName, true);
    }

    public void setSkinByPlayerName() {
        this.skinTextureSupplier = new SkinResourceSupplier(this.playerName, true);
    }

    public void setSkinBySource(@NotNull String resourceSource) {
        this.skinTextureSupplier = new SkinResourceSupplier(resourceSource, false);
    }

    public void setCapeBySource(@NotNull String resourceSource) {
        this.capeTextureSupplier = new CapeResourceSupplier(resourceSource, false);
    }

    protected void updatePose(int mouseX, int mouseY) {

        //Follow-mouse stuff
        float f = (float)(this.getAbsoluteX() + (this.getAbsoluteX() + this.getAbsoluteWidth())) / 2.0F;
        float g = (float)(this.getAbsoluteY() + (this.getAbsoluteY() + this.getAbsoluteHeight())) / 2.0F;
        float h = (float)Math.atan(((f - mouseX) / 40.0F));
        float i = (float)Math.atan(((g - mouseY) / 40.0F));

        if (this.bodyFollowsMouse) {
            this.getActiveRenderer().bodyXRot = -((float) Math.toRadians(h * 20.0F));
            this.getActiveRenderer().bodyYRot = ((float) Math.PI + (float) Math.toRadians(-i * 20.0F)); // Math.PI to fix the model being upside down
        } else {
            this.getActiveRenderer().bodyXRot = (float) Math.toRadians(this.stringToFloat(this.bodyXRot));
            this.getActiveRenderer().bodyYRot = (float) Math.PI + (float) Math.toRadians(this.stringToFloat(this.bodyYRot)); // Math.PI to fix the model being upside down
        }

        if (this.headFollowsMouse) {
            this.getActiveRenderer().headXRot = (float) Math.toRadians(h * 20.0F);
            this.getActiveRenderer().headYRot = (float) Math.toRadians(-i * 20.0F);
            this.getActiveRenderer().headZRot = 0;
        } else {
            this.getActiveRenderer().headXRot = (float) Math.toRadians(this.stringToFloat(this.headXRot));
            this.getActiveRenderer().headYRot = (float) Math.toRadians(this.stringToFloat(this.headYRot));
            this.getActiveRenderer().headZRot = (float) Math.toRadians(this.stringToFloat(this.headZRot));
        }

        this.getActiveRenderer().leftArmXRot = (float) Math.toRadians(this.stringToFloat(this.leftArmXRot));
        this.getActiveRenderer().leftArmYRot = (float) Math.toRadians(this.stringToFloat(this.leftArmYRot));
        this.getActiveRenderer().leftArmZRot = (float) Math.toRadians(this.stringToFloat(this.leftArmZRot));
        this.getActiveRenderer().rightArmXRot = (float) Math.toRadians(this.stringToFloat(this.rightArmXRot));
        this.getActiveRenderer().rightArmYRot = (float) Math.toRadians(this.stringToFloat(this.rightArmYRot));
        this.getActiveRenderer().rightArmZRot = (float) Math.toRadians(this.stringToFloat(this.rightArmZRot));
        this.getActiveRenderer().leftLegXRot = (float) Math.toRadians(this.stringToFloat(this.leftLegXRot));
        this.getActiveRenderer().leftLegYRot = (float) Math.toRadians(this.stringToFloat(this.leftLegYRot));
        this.getActiveRenderer().leftLegZRot = (float) Math.toRadians(this.stringToFloat(this.leftLegZRot));
        this.getActiveRenderer().rightLegXRot = (float) Math.toRadians(this.stringToFloat(this.rightLegXRot));
        this.getActiveRenderer().rightLegYRot = (float) Math.toRadians(this.stringToFloat(this.rightLegYRot));
        this.getActiveRenderer().rightLegZRot = (float) Math.toRadians(this.stringToFloat(this.rightLegZRot));

    }

    protected PlayerSkin updateSkinAndCape() {

        if (this.copyClientPlayer || this.autoSkin) {
            this.slim = (this.skinTextureSupplier == null) || this.skinTextureSupplier.isSlimPlayerNameSkin();
        }
        if ((this.capeTextureSupplier != null) && this.capeTextureSupplier.hasNoCape()) {
            this.capeTextureSupplier = null;
        }

        ResourceLocation skinLoc = (this.skinTextureSupplier != null) ? this.skinTextureSupplier.getSkinLocation() : SkinResourceSupplier.DEFAULT_SKIN_LOCATION;

        ResourceLocation capeLoc = null;
        if ((this.capeTextureSupplier != null) && !this.capeTextureSupplier.hasNoCape()) {
            capeLoc = this.capeTextureSupplier.getCapeLocation();
            if (capeLoc == CapeResourceSupplier.DEFAULT_CAPE_LOCATION) capeLoc = null;
        }

        PlayerSkin skin = new PlayerSkin(skinLoc, null, capeLoc, null, this.slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true);
        this.normalRenderer.skin = skin;
        this.slimRenderer.skin = skin;

        return skin;

    }

    @NotNull
    public PlayerEntityRenderer getActiveRenderer() {
        if (this.slim) return this.slimRenderer;
        return this.normalRenderer;
    }

}
