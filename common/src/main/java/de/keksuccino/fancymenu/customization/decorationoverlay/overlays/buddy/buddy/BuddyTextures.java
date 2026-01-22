package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.animation.AnimationState;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.items.PlayBall;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.items.Poop;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.gui.BuddyGuiButton;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.gui.BuddyStatusScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central place for all Buddy texture lookups and overrides.
 */
public class BuddyTextures {

    // Defaults
    public static final ResourceLocation DEFAULT_ATLAS = AnimationState.TEXTURE_ATLAS;
    public static final ResourceLocation DEFAULT_THOUGHT_BUBBLE = Buddy.TEXTURE_THOUGHT_BUBBLE;
    public static final ResourceLocation DEFAULT_PET_ICON = Buddy.TEXTURE_ICON_WANTS_BEING_PET;
    public static final ResourceLocation DEFAULT_PLAY_ICON = Buddy.TEXTURE_ICON_WANTS_TO_PLAY;
    public static final ResourceLocation DEFAULT_FOOD = FoodItem.TEXTURE_FOOD;
    public static final ResourceLocation DEFAULT_BALL = PlayBall.TEXTURE_BALL;
    public static final ResourceLocation DEFAULT_POOP = Poop.TEXTURE_POOP;
    public static final ResourceLocation DEFAULT_GRAVESTONE = Buddy.TEXTURE_GRAVESTONE;
    public static final ResourceLocation DEFAULT_STATUS_BACKGROUND = BuddyStatusScreen.BACKGROUND_TEXTURE;
    public static final ResourceLocation DEFAULT_STATUS_BACKGROUND_BORDER = BuddyStatusScreen.BACKGROUND_BORDER_TEXTURE;
    public static final ResourceLocation DEFAULT_TAB_BUTTON_NORMAL = BuddyStatusScreen.TAB_BUTTON_TEXTURE_NORMAL;
    public static final ResourceLocation DEFAULT_TAB_BUTTON_SELECTED = BuddyStatusScreen.TAB_BUTTON_TEXTURE_SELECTED;
    public static final ResourceLocation DEFAULT_DEFAULT_BUTTON_NORMAL = BuddyGuiButton.DEFAULT_BUTTON_NORMAL;
    public static final ResourceLocation DEFAULT_DEFAULT_BUTTON_HOVER = BuddyGuiButton.DEFAULT_BUTTON_HOVER;
    public static final ResourceLocation DEFAULT_DEFAULT_BUTTON_INACTIVE = BuddyGuiButton.DEFAULT_BUTTON_INACTIVE;
    public static final ResourceLocation DEFAULT_CLOSE_BUTTON_NORMAL = BuddyGuiButton.BUTTON_CLOSE_NORMAL;
    public static final ResourceLocation DEFAULT_CLOSE_BUTTON_HOVER = BuddyGuiButton.BUTTON_CLOSE_HOVER;
    public static final ResourceLocation DEFAULT_STATUS_BAR = BuddyStatusScreen.STATUS_BAR_TEXTURE;
    public static final ResourceLocation DEFAULT_STATUS_BAR_BACKGROUND = BuddyStatusScreen.STATUS_BAR_BACKGROUND_TEXTURE;
    public static final ResourceLocation DEFAULT_STATUS_ICON_HUNGER = BuddyStatusScreen.STATUS_ICON_HUNGER;
    public static final ResourceLocation DEFAULT_STATUS_ICON_HAPPINESS = BuddyStatusScreen.STATUS_ICON_HAPPINESS;
    public static final ResourceLocation DEFAULT_STATUS_ICON_ENERGY = BuddyStatusScreen.STATUS_ICON_ENERGY;
    public static final ResourceLocation DEFAULT_STATUS_ICON_FUN = BuddyStatusScreen.STATUS_ICON_FUN;
    public static final ResourceLocation DEFAULT_STATUS_ICON_EXPERIENCE = BuddyStatusScreen.STATUS_ICON_EXPERIENCE;

    private @Nullable ResourceSupplier<ITexture> customAtlasTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customThoughtBubbleTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customPetIconTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customPlayIconTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customFoodTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customBallTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customPoopTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customGravestoneTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusBackgroundTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusBorderTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customTabButtonTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customTabButtonSelectedTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customDefaultButtonTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customDefaultButtonHoverTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customDefaultButtonInactiveTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customCloseButtonTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customCloseButtonHoverTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusBarTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusBarBackgroundTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusIconHungerTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusIconHappinessTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusIconEnergyTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusIconFunTextureSupplier = null;
    private @Nullable ResourceSupplier<ITexture> customStatusIconExperienceTextureSupplier = null;

    @NotNull
    public ResourceLocation getAtlasTexture() {
        return resolve(customAtlasTextureSupplier, DEFAULT_ATLAS);
    }

    @NotNull
    public ResourceLocation getThoughtBubbleTexture() {
        return resolve(customThoughtBubbleTextureSupplier, DEFAULT_THOUGHT_BUBBLE);
    }

    @NotNull
    public ResourceLocation getPetIconTexture() {
        return resolve(customPetIconTextureSupplier, DEFAULT_PET_ICON);
    }

    @NotNull
    public ResourceLocation getPlayIconTexture() {
        return resolve(customPlayIconTextureSupplier, DEFAULT_PLAY_ICON);
    }

    @NotNull
    public ResourceLocation getFoodTexture() {
        return resolve(customFoodTextureSupplier, DEFAULT_FOOD);
    }

    @NotNull
    public ResourceLocation getBallTexture() {
        return resolve(customBallTextureSupplier, DEFAULT_BALL);
    }

    @NotNull
    public ResourceLocation getPoopTexture() {
        return resolve(customPoopTextureSupplier, DEFAULT_POOP);
    }

    @NotNull
    public ResourceLocation getGravestoneTexture() {
        return resolve(customGravestoneTextureSupplier, DEFAULT_GRAVESTONE);
    }

    @NotNull
    public ResourceLocation getStatusBackgroundTexture() {
        return resolve(customStatusBackgroundTextureSupplier, DEFAULT_STATUS_BACKGROUND);
    }

    @NotNull
    public ResourceLocation getStatusBorderTexture() {
        return resolve(customStatusBorderTextureSupplier, DEFAULT_STATUS_BACKGROUND_BORDER);
    }

    @NotNull
    public ResourceLocation getTabButtonTexture() {
        return resolve(customTabButtonTextureSupplier, DEFAULT_TAB_BUTTON_NORMAL);
    }

    @NotNull
    public ResourceLocation getTabButtonSelectedTexture() {
        return resolve(customTabButtonSelectedTextureSupplier, DEFAULT_TAB_BUTTON_SELECTED);
    }

    @NotNull
    public ResourceLocation getDefaultButtonTexture() {
        return resolve(customDefaultButtonTextureSupplier, DEFAULT_DEFAULT_BUTTON_NORMAL);
    }

    @NotNull
    public ResourceLocation getDefaultButtonHoverTexture() {
        return resolve(customDefaultButtonHoverTextureSupplier, DEFAULT_DEFAULT_BUTTON_HOVER);
    }

    @NotNull
    public ResourceLocation getDefaultButtonInactiveTexture() {
        return resolve(customDefaultButtonInactiveTextureSupplier, DEFAULT_DEFAULT_BUTTON_INACTIVE);
    }

    @NotNull
    public ResourceLocation getCloseButtonTexture() {
        return resolve(customCloseButtonTextureSupplier, DEFAULT_CLOSE_BUTTON_NORMAL);
    }

    @NotNull
    public ResourceLocation getCloseButtonHoverTexture() {
        return resolve(customCloseButtonHoverTextureSupplier, DEFAULT_CLOSE_BUTTON_HOVER);
    }

    @NotNull
    public ResourceLocation getStatusBarTexture() {
        return resolve(customStatusBarTextureSupplier, DEFAULT_STATUS_BAR);
    }

    @NotNull
    public ResourceLocation getStatusBarBackgroundTexture() {
        return resolve(customStatusBarBackgroundTextureSupplier, DEFAULT_STATUS_BAR_BACKGROUND);
    }

    @NotNull
    public ResourceLocation getStatusIconHungerTexture() {
        return resolve(customStatusIconHungerTextureSupplier, DEFAULT_STATUS_ICON_HUNGER);
    }

    @NotNull
    public ResourceLocation getStatusIconHappinessTexture() {
        return resolve(customStatusIconHappinessTextureSupplier, DEFAULT_STATUS_ICON_HAPPINESS);
    }

    @NotNull
    public ResourceLocation getStatusIconEnergyTexture() {
        return resolve(customStatusIconEnergyTextureSupplier, DEFAULT_STATUS_ICON_ENERGY);
    }

    @NotNull
    public ResourceLocation getStatusIconFunTexture() {
        return resolve(customStatusIconFunTextureSupplier, DEFAULT_STATUS_ICON_FUN);
    }

    @NotNull
    public ResourceLocation getStatusIconExperienceTexture() {
        return resolve(customStatusIconExperienceTextureSupplier, DEFAULT_STATUS_ICON_EXPERIENCE);
    }

    public void setCustomAtlasTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customAtlasTextureSupplier = supplier;
    }

    public void setCustomThoughtBubbleTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customThoughtBubbleTextureSupplier = supplier;
    }

    public void setCustomPetIconTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customPetIconTextureSupplier = supplier;
    }

    public void setCustomPlayIconTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customPlayIconTextureSupplier = supplier;
    }

    public void setCustomFoodTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customFoodTextureSupplier = supplier;
    }

    public void setCustomBallTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customBallTextureSupplier = supplier;
    }

    public void setCustomPoopTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customPoopTextureSupplier = supplier;
    }

    public void setCustomGravestoneTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customGravestoneTextureSupplier = supplier;
    }

    public void setCustomStatusBackgroundTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusBackgroundTextureSupplier = supplier;
    }

    public void setCustomStatusBorderTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusBorderTextureSupplier = supplier;
    }

    public void setCustomTabButtonTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customTabButtonTextureSupplier = supplier;
    }

    public void setCustomTabButtonSelectedTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customTabButtonSelectedTextureSupplier = supplier;
    }

    public void setCustomDefaultButtonTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customDefaultButtonTextureSupplier = supplier;
    }

    public void setCustomDefaultButtonHoverTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customDefaultButtonHoverTextureSupplier = supplier;
    }

    public void setCustomDefaultButtonInactiveTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customDefaultButtonInactiveTextureSupplier = supplier;
    }

    public void setCustomCloseButtonTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customCloseButtonTextureSupplier = supplier;
    }

    public void setCustomCloseButtonHoverTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customCloseButtonHoverTextureSupplier = supplier;
    }

    public void setCustomStatusBarTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusBarTextureSupplier = supplier;
    }

    public void setCustomStatusBarBackgroundTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusBarBackgroundTextureSupplier = supplier;
    }

    public void setCustomStatusIconHungerTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusIconHungerTextureSupplier = supplier;
    }

    public void setCustomStatusIconHappinessTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusIconHappinessTextureSupplier = supplier;
    }

    public void setCustomStatusIconEnergyTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusIconEnergyTextureSupplier = supplier;
    }

    public void setCustomStatusIconFunTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusIconFunTextureSupplier = supplier;
    }

    public void setCustomStatusIconExperienceTextureSupplier(@Nullable ResourceSupplier<ITexture> supplier) {
        this.customStatusIconExperienceTextureSupplier = supplier;
    }

    /**
     * Copies all custom texture suppliers from the provided instance.
     * Default fallbacks are not affected.
     */
    public void copyFrom(@NotNull BuddyTextures source) {
        this.customAtlasTextureSupplier = source.customAtlasTextureSupplier;
        this.customThoughtBubbleTextureSupplier = source.customThoughtBubbleTextureSupplier;
        this.customPetIconTextureSupplier = source.customPetIconTextureSupplier;
        this.customPlayIconTextureSupplier = source.customPlayIconTextureSupplier;
        this.customFoodTextureSupplier = source.customFoodTextureSupplier;
        this.customBallTextureSupplier = source.customBallTextureSupplier;
        this.customPoopTextureSupplier = source.customPoopTextureSupplier;
        this.customGravestoneTextureSupplier = source.customGravestoneTextureSupplier;
        this.customStatusBackgroundTextureSupplier = source.customStatusBackgroundTextureSupplier;
        this.customStatusBorderTextureSupplier = source.customStatusBorderTextureSupplier;
        this.customTabButtonTextureSupplier = source.customTabButtonTextureSupplier;
        this.customTabButtonSelectedTextureSupplier = source.customTabButtonSelectedTextureSupplier;
        this.customDefaultButtonTextureSupplier = source.customDefaultButtonTextureSupplier;
        this.customDefaultButtonHoverTextureSupplier = source.customDefaultButtonHoverTextureSupplier;
        this.customDefaultButtonInactiveTextureSupplier = source.customDefaultButtonInactiveTextureSupplier;
        this.customCloseButtonTextureSupplier = source.customCloseButtonTextureSupplier;
        this.customCloseButtonHoverTextureSupplier = source.customCloseButtonHoverTextureSupplier;
        this.customStatusBarTextureSupplier = source.customStatusBarTextureSupplier;
        this.customStatusBarBackgroundTextureSupplier = source.customStatusBarBackgroundTextureSupplier;
        this.customStatusIconHungerTextureSupplier = source.customStatusIconHungerTextureSupplier;
        this.customStatusIconHappinessTextureSupplier = source.customStatusIconHappinessTextureSupplier;
        this.customStatusIconEnergyTextureSupplier = source.customStatusIconEnergyTextureSupplier;
        this.customStatusIconFunTextureSupplier = source.customStatusIconFunTextureSupplier;
        this.customStatusIconExperienceTextureSupplier = source.customStatusIconExperienceTextureSupplier;
    }

    @NotNull
    private ResourceLocation resolve(@Nullable ResourceSupplier<ITexture> supplier, @NotNull ResourceLocation fallback) {
        if (supplier != null) {
            ITexture texture = supplier.get();
            if (texture != null && texture.getResourceLocation() != null) {
                return texture.getResourceLocation();
            }
        }
        return fallback;
    }
}
