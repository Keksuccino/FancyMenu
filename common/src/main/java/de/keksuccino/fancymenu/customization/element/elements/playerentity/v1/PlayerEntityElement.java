package de.keksuccino.fancymenu.customization.element.elements.playerentity.v1;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.model.PlayerEntityElementRenderer;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.model.PlayerEntityProperties;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.textures.CapeResourceSupplier;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.textures.SkinResourceSupplier;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class PlayerEntityElement extends AbstractElement {
    
    private static final Logger LOGGER = LogManager.getLogger();

    public final PlayerEntityElementRenderer normalRenderer = buildEntityRenderer(false);
    public final PlayerEntityElementRenderer slimRenderer = buildEntityRenderer(true);

    public volatile boolean copyClientPlayer = false;
    @NotNull
    public volatile String playerName = "Steve";
    public boolean showPlayerName = true;
    public boolean hasParrotOnShoulder = false;
    public boolean parrotOnLeftShoulder = false;
    public boolean crouching = false;
    public boolean isBaby = false;
    public String scale = "30";
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

    public PlayerEntityElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Nullable
    protected static PlayerEntityElementRenderer buildEntityRenderer(boolean slim) {
        try {
            return new PlayerEntityElementRenderer(slim);
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updatePlayerDisplayName();

            this.updateSkinAndCape();

            float scale = this.stringToFloat(this.scale);
            if (scale == 0.0F) scale = 30;

            //Update element size based on entity size
            this.baseWidth = (int)(this.getActiveEntityProperties().getDimensions().width() * scale);
            this.baseHeight = (int)(this.getActiveEntityProperties().getDimensions().height() * scale);

             

            PlayerEntityProperties props = this.getActiveEntityProperties();
            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int mouseOffsetX = this.baseWidth / 2;
            int mouseOffsetY = (this.baseHeight / 4) / 2;
            if (props.isBaby) mouseOffsetY += (this.baseHeight / 2) - mouseOffsetY; //not exactly the same eye pos as for adult size, but good enough
            this.renderPlayerEntity(graphics, x, y, (int)scale, (float)x - mouseX + mouseOffsetX, (float)y - mouseY + mouseOffsetY, props);

        }

    }

    protected void renderPlayerEntity(GuiGraphics graphics, int posX, int posY, int scale, float angleXComponent, float angleYComponent, PlayerEntityProperties props) {
        float f = (float)Math.atan(angleXComponent / 40.0F);
        float f1 = (float)Math.atan(angleYComponent / 40.0F);
        innerRenderPlayerEntity(graphics, posX, posY, scale, f, f1, props, this.getActiveRenderer());
    }

    @SuppressWarnings("all")
    protected void innerRenderPlayerEntity(GuiGraphics graphics, int posX, int posY, int scale, float angleXComponent, float angleYComponent, PlayerEntityProperties props, PlayerEntityElementRenderer renderer) {


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
        this.updatePlayerDisplayName();
    }

    public void setShowPlayerName(boolean showName) {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        this.showPlayerName = showName;
        this.normalRenderer.properties.showDisplayName = showName;
        this.slimRenderer.properties.showDisplayName = showName;
    }

    public void setHasParrotOnShoulder(boolean hasParrot, boolean onLeftShoulder) {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        this.hasParrotOnShoulder = hasParrot;
        this.parrotOnLeftShoulder = onLeftShoulder;
        this.normalRenderer.properties.hasParrotOnShoulder = hasParrot;
        this.slimRenderer.properties.hasParrotOnShoulder = hasParrot;
        this.normalRenderer.properties.parrotOnLeftShoulder = onLeftShoulder;
        this.slimRenderer.properties.parrotOnLeftShoulder = onLeftShoulder;
    }

    public void setCrouching(boolean crouching) {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        this.crouching = crouching;
        this.normalRenderer.properties.crouching = crouching;
        this.slimRenderer.properties.crouching = crouching;
    }

    public void setIsBaby(boolean isBaby) {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        this.isBaby = isBaby;
        this.normalRenderer.properties.isBaby = isBaby;
        this.slimRenderer.properties.isBaby = isBaby;
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

    protected void updateSkinAndCape() {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        if (this.copyClientPlayer || this.autoSkin) {
            this.slim = (this.skinTextureSupplier == null) || this.skinTextureSupplier.isSlimPlayerNameSkin();
        }
        if ((this.capeTextureSupplier != null) && this.capeTextureSupplier.hasNoCape()) {
            this.capeTextureSupplier = null;
        }
        this.normalRenderer.properties.setSkinTextureLocation((this.skinTextureSupplier != null) ? this.skinTextureSupplier.getSkinLocation() : SkinResourceSupplier.DEFAULT_SKIN_LOCATION);
        this.slimRenderer.properties.setSkinTextureLocation((this.skinTextureSupplier != null) ? this.skinTextureSupplier.getSkinLocation() : SkinResourceSupplier.DEFAULT_SKIN_LOCATION);
        ResourceLocation capeLoc = null;
        if ((this.capeTextureSupplier != null) && !this.capeTextureSupplier.hasNoCape()) {
            capeLoc = this.capeTextureSupplier.getCapeLocation();
            if (capeLoc == CapeResourceSupplier.DEFAULT_CAPE_LOCATION) capeLoc = null;
        }
        this.normalRenderer.properties.setCapeTextureLocation(capeLoc);
        this.slimRenderer.properties.setCapeTextureLocation(capeLoc);
    }

    protected void updatePlayerDisplayName() {
        if ((this.normalRenderer == null) || (this.slimRenderer == null)) return;
        this.normalRenderer.properties.displayName = buildComponent(this.playerName);
        this.slimRenderer.properties.displayName = buildComponent(this.playerName);
    }

    public PlayerEntityElementRenderer getActiveRenderer() {
        if (this.slim) {
            return this.slimRenderer;
        }
        return this.normalRenderer;
    }

    public PlayerEntityProperties getActiveEntityProperties() {
        return this.getActiveRenderer().properties;
    }

}
