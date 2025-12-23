package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.CapeResourceSupplier;
import de.keksuccino.fancymenu.customization.element.elements.playerentity.textures.SkinResourceSupplier;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.entity.FancyEntityRendererUtils;
import de.keksuccino.fancymenu.util.rendering.entity.WrappedFancyPlayerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

public class PlayerEntityElement extends AbstractElement {
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor MISSING_FER_COLOR = DrawableColor.of(Color.RED);

    public volatile boolean copyClientPlayer = false;
    @NotNull
    public volatile String playerName = "Steve";
    public boolean showPlayerName = true;
    @NotNull
    public PlayerPose pose = PlayerPose.STANDING;
    public boolean bodyMovement = false;
    public boolean hasParrotOnShoulder = false;
    public boolean parrotOnLeftShoulder = false;
    public boolean isBaby = false;
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
    public String bodyZRot;
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
    public boolean bodyZRotAdvancedMode;
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
    @NotNull
    public Wearable leftHandWearable = Wearable.empty();
    @NotNull
    public Wearable rightHandWearable = Wearable.empty();
    @NotNull
    public Wearable headWearable = Wearable.empty();
    @NotNull
    public Wearable chestWearable = Wearable.empty();
    @NotNull
    public Wearable legsWearable = Wearable.empty();
    @NotNull
    public Wearable feetWearable = Wearable.empty();

    @Nullable
    protected WrappedFancyPlayerWidget widget = null;

    public PlayerEntityElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void afterConstruction() {
        super.afterConstruction();
        if (FancyEntityRendererUtils.isFerLoaded()) {
            this.widget = WrappedFancyPlayerWidget.build(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            if (this.widget == null) {

                graphics.fill(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), MISSING_FER_COLOR.getColorInt());
                int xCenter = this.getAbsoluteX() + (this.getAbsoluteWidth() / 2);
                int yCenter = this.getAbsoluteY() + (this.getAbsoluteHeight() / 2);
                graphics.drawCenteredString(Minecraft.getInstance().font, "§lFER (FANCY ENTITY RENDERER) IS NOT INSTALLED!", xCenter, yCenter, -1);
                graphics.drawCenteredString(Minecraft.getInstance().font, "§lPLEASE DOWNLOAD FROM CURSEFORGE OR MODRINTH!", xCenter, yCenter + Minecraft.getInstance().font.lineHeight + 2, -1);

            } else {

                this.widget.setX(this.getAbsoluteX());
                this.widget.setY(this.getAbsoluteY());
                this.widget.setWidth(this.getAbsoluteWidth());
                this.widget.setHeight(this.getAbsoluteHeight());

                this.updateSkinAndCape();

                this.updateEntityPose();

                this.updateParrots();

                this.updateWearables();

                this.updateEntityProperties();

                this.widget.render(graphics, mouseX, mouseY, partial);

            }

        }

    }

    protected void updateEntityProperties() {

        if (this.widget != null) {

            this.widget.setShowName(this.showPlayerName);
            this.widget.setName(this.playerName);
            this.widget.setBaby(this.isBaby);
            this.widget.setHeadFollowsMouse(this.headFollowsMouse);
            this.widget.setBodyFollowsMouse(this.bodyFollowsMouse);
            this.widget.setSlim(this.slim);

        }

    }

    protected void updateWearables() {

        if (this.widget == null) return;

        this.widget.setLeftHandItem(this.leftHandWearable.getWearable());
        this.widget.setRightHandItem(this.rightHandWearable.getWearable());

        this.widget.setHeadWearable(this.headWearable.getWearable());
        this.widget.setChestWearable(this.chestWearable.getWearable());
        this.widget.setLegsWearable(this.legsWearable.getWearable());
        this.widget.setFeetWearable(this.feetWearable.getWearable());

    }

    protected void updateParrots() {
        if (this.widget != null) {
            if (!this.hasParrotOnShoulder) {
                this.widget.setParrots(null, null);
            } else if (this.parrotOnLeftShoulder) {
                this.widget.setParrots(Parrot.Variant.RED_BLUE, null);
            } else {
                this.widget.setParrots(null, Parrot.Variant.RED_BLUE);
            }
        }
    }

    protected void updateEntityPose() {

        if (this.widget != null) {

            this.widget.setPose(this.pose.pose);
            this.widget.setBodyMovement(this.bodyMovement);

            float bodyXRot = stringToFloat(this.bodyXRot);
            float bodyYRot = stringToFloat(this.bodyYRot);
            float bodyZRot = stringToFloat(this.bodyZRot);
            float headXRot = stringToFloat(this.headXRot);
            float headYRot = stringToFloat(this.headYRot);
            float headZRot = stringToFloat(this.headZRot);
            float leftArmXRot = stringToFloat(this.leftArmXRot);
            float leftArmYRot = stringToFloat(this.leftArmYRot);
            float leftArmZRot = stringToFloat(this.leftArmZRot);
            float rightArmXRot = stringToFloat(this.rightArmXRot);
            float rightArmYRot = stringToFloat(this.rightArmYRot);
            float rightArmZRot = stringToFloat(this.rightArmZRot);
            float leftLegXRot = stringToFloat(this.leftLegXRot);
            float leftLegYRot = stringToFloat(this.leftLegYRot);
            float leftLegZRot = stringToFloat(this.leftLegZRot);
            float rightLegXRot = stringToFloat(this.rightLegXRot);
            float rightLegYRot = stringToFloat(this.rightLegYRot);
            float rightLegZRot = stringToFloat(this.rightLegZRot);

            this.widget.setBodyRotation(bodyXRot, bodyYRot, bodyZRot);
            this.widget.setHeadRotation(headXRot, headYRot, headZRot);
            this.widget.setLeftArmRotation(leftArmXRot, leftArmYRot, leftArmZRot);
            this.widget.setRightArmRotation(rightArmXRot, rightArmYRot, rightArmZRot);
            this.widget.setLeftLegRotation(leftLegXRot, leftLegYRot, leftLegZRot);
            this.widget.setRightLegRotation(rightLegXRot, rightLegYRot, rightLegZRot);

        }

    }

    protected void updateSkinAndCape() {
        if (this.widget == null) return;
        if (this.copyClientPlayer || this.autoSkin) {
            this.slim = (this.skinTextureSupplier == null) || this.skinTextureSupplier.isSlimPlayerNameSkin();
        }
        if ((this.capeTextureSupplier != null) && this.capeTextureSupplier.hasNoCape()) {
            this.capeTextureSupplier = null;
        }
        Identifier skinLoc = (this.skinTextureSupplier != null) ? this.skinTextureSupplier.getSkinLocation() : SkinResourceSupplier.DEFAULT_SKIN_LOCATION;
        Identifier capeLoc = null;
        if ((this.capeTextureSupplier != null) && !this.capeTextureSupplier.hasNoCape()) {
            capeLoc = this.capeTextureSupplier.getCapeLocation();
            if (capeLoc == CapeResourceSupplier.DEFAULT_CAPE_LOCATION) capeLoc = null;
        }
        ClientAsset.Texture skinTex = new ClientTexture(skinLoc);
        ClientAsset.Texture capeTex = (capeLoc != null) ? new ClientTexture(capeLoc) : null;
        this.widget.setSkin(new PlayerSkin(skinTex, null, capeTex, this.slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, false));
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
    }

    public void setIsBaby(boolean isBaby) {
        this.isBaby = isBaby;
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

    protected static float stringToFloat(@Nullable String s) {
        if (s == null) return 0.0F;
        s = PlaceholderParser.replacePlaceholders(s);
        s = s.replace(" ", "");
        try {
            return Float.parseFloat(s);
        } catch (Exception ignore) {}
        return 0.0F;
    }

    public enum PlayerPose implements LocalizedCycleEnum<PlayerPose> {

        STANDING("standing", Pose.STANDING),
        CROUCHING("crouching", Pose.CROUCHING),
        SLEEPING("sleeping", Pose.SLEEPING),
        SWIMMING("swimming", Pose.SWIMMING),
        DYING("dying", Pose.DYING),
        SPIN_ATTACK("spin_attack", Pose.SPIN_ATTACK);

        public final String name;
        public final Pose pose;

        PlayerPose(@NotNull String name, @NotNull Pose pose) {
            this.name = name;
            this.pose = pose;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.elements.player_entity.pose";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull PlayerPose[] getValues() {
            return PlayerPose.values();
        }

        @Override
        public @Nullable PlayerPose getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static PlayerPose getByName(@Nullable String name) {
            if (name == null) return null;
            for (PlayerPose p : PlayerPose.values()) {
                if (p.name.equals(name)) return p;
            }
            return null;
        }

    }

    public static class Wearable {

        public static final String WEARABLE_EMPTY_KEY = "fancymenu_wearable_none_dummy";
        private static final String SERIALIZATION_SEPARATOR = ":::";

        @NotNull
        public String itemKey;
        public boolean enchanted;
        @NotNull
        public ItemStack cachedStack = new ItemStack(Items.AIR);
        @Nullable
        protected String lastFinalKey = null;
        protected boolean lastEnchanted = false;

        @NotNull
        public static Wearable empty() {
            return new Wearable(WEARABLE_EMPTY_KEY, false);
        }

        private Wearable(@NotNull String itemKey, boolean enchanted) {
            this.itemKey = itemKey;
            this.enchanted = enchanted;
        }

        public void update() {

            try {

                String keyFinal = PlaceholderParser.replacePlaceholders(this.itemKey);

                if (!keyFinal.equals(this.lastFinalKey) || (this.lastEnchanted != this.enchanted)) {

                    this.lastFinalKey = keyFinal;
                    this.lastEnchanted = enchanted;

                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(keyFinal));
                    this.cachedStack = new ItemStack(item);
                    this.cachedStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, this.enchanted);

                }

            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to update wearable of PlayerEntityElement!", ex);
            }

        }

        public boolean isEmpty() {
            return this.itemKey.equals(WEARABLE_EMPTY_KEY);
        }

        public void setEmpty() {
            this.itemKey = WEARABLE_EMPTY_KEY;
        }

        @Nullable
        public ItemStack getWearable() {
            if (this.isEmpty()) return null;
            this.update();
            return this.cachedStack;
        }

        @NotNull
        public String serialize() {
            StringBuilder serialized = new StringBuilder()
                    .append(this.itemKey).append(SERIALIZATION_SEPARATOR)
                    .append(this.enchanted);
            return serialized.toString();
        }

        @NotNull
        public static Wearable deserialize(@Nullable String serialized) {
            if (serialized == null) return empty();
            try {
                if (serialized.contains(SERIALIZATION_SEPARATOR)) {
                    var array = serialized.split(SERIALIZATION_SEPARATOR);
                    String key = array[0];
                    boolean enchant = SerializationUtils.deserializeBoolean(false, array[1]);
                    return new Wearable(key, enchant);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to deserialize Wearable of PlayerEntityElement!", ex);
            }
            return empty();
        }

    }

    public record ClientTexture(Identifier texturePath) implements ClientAsset.Texture {
        @Override
        public @NotNull Identifier id() {
            return this.texturePath();
        }
    }

}
