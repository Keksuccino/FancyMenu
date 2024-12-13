package de.keksuccino.fancymenu.customization.element.elements.playerentity.v2;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.renderer.v2.PlayerEntityRenderer;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.CapeResourceSupplier;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.SkinResourceSupplier;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.animal.Parrot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityElement extends AbstractElement {
    
    private static final Logger LOGGER = LogManager.getLogger();

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

    protected final PlayerRenderState renderState = new PlayerRenderState();
    protected final DrawableColor nameTagBackgroundColor = DrawableColor.BLACK;

    public PlayerEntityElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateSkinAndCape();
            this.updateParrotOnShoulder();
            this.updatePose(mouseX, mouseY);

            RenderSystem.enableBlend();

            this.renderEntity(graphics);

            this.renderNameTag(graphics, mouseX, mouseY, partial);

        }

    }

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

    protected void updateSkinAndCape() {

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

        PlayerSkin skin = new PlayerSkin(skinLoc, null, capeLoc, null, this.slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
        this.normalRenderer.skin = skin;
        this.slimRenderer.skin = skin;

    }

    @NotNull
    public PlayerEntityRenderer getActiveRenderer() {
        if (this.slim) return this.slimRenderer;
        return this.normalRenderer;
    }

}
