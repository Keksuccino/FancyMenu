package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.Buddy;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.BuddyStatConfig;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.BuddyWidget;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class BuddyDecorationOverlay extends AbstractDecorationOverlay<BuddyDecorationOverlay> {

    public final Property<ResourceSupplier<ITexture>> customAtlasTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_atlas_texture", null, "fancymenu.decoration_overlays.buddy.custom_atlas_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customThoughtBubbleTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_thought_texture", null, "fancymenu.decoration_overlays.buddy.custom_thought_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPetIconTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_pet_icon_texture", null, "fancymenu.decoration_overlays.buddy.custom_pet_icon_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPlayIconTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_play_icon_texture", null, "fancymenu.decoration_overlays.buddy.custom_play_icon_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customFoodTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_food_texture", null, "fancymenu.decoration_overlays.buddy.custom_food_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customBallTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_ball_texture", null, "fancymenu.decoration_overlays.buddy.custom_ball_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPoopTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_poop_texture", null, "fancymenu.decoration_overlays.buddy.custom_poop_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customGravestoneTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_gravestone_texture", null, "fancymenu.decoration_overlays.buddy.custom_gravestone_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBackgroundTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_background_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_background_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBorderTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_border_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_border_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBarTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_bar_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_bar_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBarBackgroundTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_bar_background_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_bar_background_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusIconHungerTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_icon_hunger_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_icon_hunger_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusIconHappinessTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_icon_happiness_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_icon_happiness_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusIconEnergyTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_icon_energy_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_icon_energy_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusIconFunTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_icon_fun_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_icon_fun_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusIconExperienceTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_icon_experience_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_icon_experience_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customTabButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_tab_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_tab_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customTabButtonSelectedTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_tab_button_selected_texture", null, "fancymenu.decoration_overlays.buddy.custom_tab_button_selected_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonHoverTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_hover_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_hover_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonInactiveTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_inactive_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_inactive_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customCloseButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_close_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_close_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customCloseButtonHoverTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_close_button_hover_texture", null, "fancymenu.decoration_overlays.buddy.custom_close_button_hover_texture", true, true, true, null));
    public final Property<Float> hungerDecayPerTick = putProperty(Property.floatProperty("buddy_hunger_decay_per_tick", Buddy.DEFAULT_HUNGER_DECAY_PER_TICK, "fancymenu.decoration_overlays.buddy.hunger_decay_per_tick"));
    public final Property<Float> happinessDecayPerTick = putProperty(Property.floatProperty("buddy_happiness_decay_per_tick", Buddy.DEFAULT_HAPPINESS_DECAY_PER_TICK, "fancymenu.decoration_overlays.buddy.happiness_decay_per_tick"));
    public final Property<Float> funDecayPerTick = putProperty(Property.floatProperty("buddy_fun_decay_per_tick", Buddy.DEFAULT_FUN_DECAY_PER_TICK, "fancymenu.decoration_overlays.buddy.fun_decay_per_tick"));
    public final Property<Float> energyDecayPerTick = putProperty(Property.floatProperty("buddy_energy_decay_per_tick", Buddy.DEFAULT_ENERGY_DECAY_PER_TICK, "fancymenu.decoration_overlays.buddy.energy_decay_per_tick"));
    public final Property<Float> energySleepRegenPerTick = putProperty(Property.floatProperty("buddy_energy_sleep_regen_per_tick", Buddy.DEFAULT_ENERGY_SLEEP_REGEN_PER_TICK, "fancymenu.decoration_overlays.buddy.energy_sleep_regen_per_tick"));
    public final Property<Float> playEnergyDrainPerTick = putProperty(Property.floatProperty("buddy_play_energy_drain_per_tick", Buddy.DEFAULT_PLAY_ENERGY_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.play_energy_drain_per_tick"));
    public final Property<Float> playFunGainPerTick = putProperty(Property.floatProperty("buddy_play_fun_gain_per_tick", Buddy.DEFAULT_PLAY_FUN_GAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.play_fun_gain_per_tick"));
    public final Property<Float> playHappinessGainPerTick = putProperty(Property.floatProperty("buddy_play_happiness_gain_per_tick", Buddy.DEFAULT_PLAY_HAPPINESS_GAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.play_happiness_gain_per_tick"));
    public final Property<Float> playHungerDrainPerTick = putProperty(Property.floatProperty("buddy_play_hunger_drain_per_tick", Buddy.DEFAULT_PLAY_HUNGER_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.play_hunger_drain_per_tick"));
    public final Property<Float> chaseEnergyDrainPerTick = putProperty(Property.floatProperty("buddy_chase_energy_drain_per_tick", Buddy.DEFAULT_CHASE_ENERGY_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.chase_energy_drain_per_tick"));
    public final Property<Float> chaseHungerDrainPerTick = putProperty(Property.floatProperty("buddy_chase_hunger_drain_per_tick", Buddy.DEFAULT_CHASE_HUNGER_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.chase_hunger_drain_per_tick"));
    public final Property<Float> hopEnergyDrainPerTick = putProperty(Property.floatProperty("buddy_hop_energy_drain_per_tick", Buddy.DEFAULT_HOP_ENERGY_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.hop_energy_drain_per_tick"));
    public final Property<Float> hopHungerDrainPerTick = putProperty(Property.floatProperty("buddy_hop_hunger_drain_per_tick", Buddy.DEFAULT_HOP_HUNGER_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.hop_hunger_drain_per_tick"));
    public final Property<Float> excitedEnergyDrainPerTick = putProperty(Property.floatProperty("buddy_excited_energy_drain_per_tick", Buddy.DEFAULT_EXCITED_ENERGY_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.excited_energy_drain_per_tick"));
    public final Property<Float> excitedHappinessGainPerTick = putProperty(Property.floatProperty("buddy_excited_happiness_gain_per_tick", Buddy.DEFAULT_EXCITED_HAPPINESS_GAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.excited_happiness_gain_per_tick"));
    public final Property<Float> excitedHungerDrainPerTick = putProperty(Property.floatProperty("buddy_excited_hunger_drain_per_tick", Buddy.DEFAULT_EXCITED_HUNGER_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.excited_hunger_drain_per_tick"));
    public final Property<Float> runningEnergyDrainPerTick = putProperty(Property.floatProperty("buddy_running_energy_drain_per_tick", Buddy.DEFAULT_RUNNING_ENERGY_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.running_energy_drain_per_tick"));
    public final Property<Float> runningHungerDrainPerTick = putProperty(Property.floatProperty("buddy_running_hunger_drain_per_tick", Buddy.DEFAULT_RUNNING_HUNGER_DRAIN_PER_TICK, "fancymenu.decoration_overlays.buddy.running_hunger_drain_per_tick"));
    public final Property<Float> foodHungerGain = putProperty(Property.floatProperty("buddy_food_hunger_gain", Buddy.DEFAULT_FOOD_HUNGER_GAIN, "fancymenu.decoration_overlays.buddy.food_hunger_gain"));
    public final Property<Float> foodHappinessGain = putProperty(Property.floatProperty("buddy_food_happiness_gain", Buddy.DEFAULT_FOOD_HAPPINESS_GAIN, "fancymenu.decoration_overlays.buddy.food_happiness_gain"));
    public final Property<Float> petHappinessGain = putProperty(Property.floatProperty("buddy_pet_happiness_gain", Buddy.DEFAULT_PET_HAPPINESS_GAIN, "fancymenu.decoration_overlays.buddy.pet_happiness_gain"));
    public final Property<Float> wakeupHappinessPenalty = putProperty(Property.floatProperty("buddy_wakeup_happiness_penalty", Buddy.DEFAULT_WAKEUP_HAPPINESS_PENALTY, "fancymenu.decoration_overlays.buddy.wakeup_happiness_penalty"));
    public final Property<Integer> maxPoopsCap = putProperty(Property.integerProperty("buddy_max_poops_cap", Buddy.DEFAULT_MAX_POOPS_CAP, "fancymenu.decoration_overlays.buddy.max_poops_cap"));
    public final Property<Boolean> canDie = putProperty(Property.booleanProperty("buddy_can_die", Buddy.DEFAULT_CAN_DIE, "fancymenu.decoration_overlays.buddy.can_die"));

    private final BuddyWidget buddyWidget = new BuddyWidget(0, 0);
    private int lastGuiTick = -1;

    public BuddyDecorationOverlay() {
        this.buddyWidget.setInstanceIdentifier(getInstanceIdentifier());
        bindTextureProperty(this.customAtlasTexture, this.buddyWidget::setCustomAtlasTextureSupplier);
        bindTextureProperty(this.customThoughtBubbleTexture, supplier -> this.buddyWidget.getTextures().setCustomThoughtBubbleTextureSupplier(supplier));
        bindTextureProperty(this.customPetIconTexture, supplier -> this.buddyWidget.getTextures().setCustomPetIconTextureSupplier(supplier));
        bindTextureProperty(this.customPlayIconTexture, supplier -> this.buddyWidget.getTextures().setCustomPlayIconTextureSupplier(supplier));
        bindTextureProperty(this.customFoodTexture, supplier -> this.buddyWidget.getTextures().setCustomFoodTextureSupplier(supplier));
        bindTextureProperty(this.customBallTexture, supplier -> this.buddyWidget.getTextures().setCustomBallTextureSupplier(supplier));
        bindTextureProperty(this.customPoopTexture, supplier -> this.buddyWidget.getTextures().setCustomPoopTextureSupplier(supplier));
        bindTextureProperty(this.customGravestoneTexture, supplier -> this.buddyWidget.getTextures().setCustomGravestoneTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBackgroundTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBackgroundTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBorderTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBorderTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBarTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBarTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBarBackgroundTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBarBackgroundTextureSupplier(supplier));
        bindTextureProperty(this.customStatusIconHungerTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusIconHungerTextureSupplier(supplier));
        bindTextureProperty(this.customStatusIconHappinessTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusIconHappinessTextureSupplier(supplier));
        bindTextureProperty(this.customStatusIconEnergyTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusIconEnergyTextureSupplier(supplier));
        bindTextureProperty(this.customStatusIconFunTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusIconFunTextureSupplier(supplier));
        bindTextureProperty(this.customStatusIconExperienceTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusIconExperienceTextureSupplier(supplier));
        bindTextureProperty(this.customTabButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomTabButtonTextureSupplier(supplier));
        bindTextureProperty(this.customTabButtonSelectedTexture, supplier -> this.buddyWidget.getTextures().setCustomTabButtonSelectedTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonHoverTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonHoverTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonInactiveTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonInactiveTextureSupplier(supplier));
        bindTextureProperty(this.customCloseButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomCloseButtonTextureSupplier(supplier));
        bindTextureProperty(this.customCloseButtonHoverTexture, supplier -> this.buddyWidget.getTextures().setCustomCloseButtonHoverTextureSupplier(supplier));
        bindStatProperties();
        bindPoopCapProperty();
        bindCanDieProperty();
        this.showOverlay.addValueSetListener((oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                resetTickCounter();
            } else if (Boolean.TRUE.equals(oldValue)) {
                this.buddyWidget.cleanup();
            }
        });
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        ContextMenu texturesMenu = new ContextMenu();
        menu.addSubMenuEntry("buddy_custom_textures", Component.translatable("fancymenu.decoration_overlays.buddy.custom_textures"), texturesMenu)
                .setIcon(MaterialIcons.TEXTURE)
                .setStackable(true);

        addTextureEntry(texturesMenu, this.customAtlasTexture, "fancymenu.decoration_overlays.buddy.custom_atlas_texture.desc");
        addTextureEntry(texturesMenu, this.customThoughtBubbleTexture, "fancymenu.decoration_overlays.buddy.custom_thought_texture.desc");
        addTextureEntry(texturesMenu, this.customPetIconTexture, "fancymenu.decoration_overlays.buddy.custom_pet_icon_texture.desc");
        addTextureEntry(texturesMenu, this.customPlayIconTexture, "fancymenu.decoration_overlays.buddy.custom_play_icon_texture.desc");
        addTextureEntry(texturesMenu, this.customFoodTexture, "fancymenu.decoration_overlays.buddy.custom_food_texture.desc");
        addTextureEntry(texturesMenu, this.customBallTexture, "fancymenu.decoration_overlays.buddy.custom_ball_texture.desc");
        addTextureEntry(texturesMenu, this.customPoopTexture, "fancymenu.decoration_overlays.buddy.custom_poop_texture.desc");
        addTextureEntry(texturesMenu, this.customGravestoneTexture, "fancymenu.decoration_overlays.buddy.custom_gravestone_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBackgroundTexture, "fancymenu.decoration_overlays.buddy.custom_status_background_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBorderTexture, "fancymenu.decoration_overlays.buddy.custom_status_border_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBarTexture, "fancymenu.decoration_overlays.buddy.custom_status_bar_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBarBackgroundTexture, "fancymenu.decoration_overlays.buddy.custom_status_bar_background_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusIconHungerTexture, "fancymenu.decoration_overlays.buddy.custom_status_icon_hunger_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusIconHappinessTexture, "fancymenu.decoration_overlays.buddy.custom_status_icon_happiness_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusIconEnergyTexture, "fancymenu.decoration_overlays.buddy.custom_status_icon_energy_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusIconFunTexture, "fancymenu.decoration_overlays.buddy.custom_status_icon_fun_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusIconExperienceTexture, "fancymenu.decoration_overlays.buddy.custom_status_icon_experience_texture.desc");
        addTextureEntry(texturesMenu, this.customTabButtonTexture, "fancymenu.decoration_overlays.buddy.custom_tab_button_texture.desc");
        addTextureEntry(texturesMenu, this.customTabButtonSelectedTexture, "fancymenu.decoration_overlays.buddy.custom_tab_button_selected_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonHoverTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_hover_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonInactiveTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_inactive_texture.desc");
        addTextureEntry(texturesMenu, this.customCloseButtonTexture, "fancymenu.decoration_overlays.buddy.custom_close_button_texture.desc");
        addTextureEntry(texturesMenu, this.customCloseButtonHoverTexture, "fancymenu.decoration_overlays.buddy.custom_close_button_hover_texture.desc");

        ContextMenu statsMenu = new ContextMenu();
        menu.addSubMenuEntry("buddy_stats_tuning", Component.translatable("fancymenu.decoration_overlays.buddy.stats"), statsMenu)
                .setIcon(MaterialIcons.BAR_CHART)
                .setStackable(true);

        addFloatEntry(statsMenu, hungerDecayPerTick, "fancymenu.decoration_overlays.buddy.hunger_decay_per_tick.desc");
        addFloatEntry(statsMenu, happinessDecayPerTick, "fancymenu.decoration_overlays.buddy.happiness_decay_per_tick.desc");
        addFloatEntry(statsMenu, funDecayPerTick, "fancymenu.decoration_overlays.buddy.fun_decay_per_tick.desc");
        addFloatEntry(statsMenu, energyDecayPerTick, "fancymenu.decoration_overlays.buddy.energy_decay_per_tick.desc");
        addFloatEntry(statsMenu, energySleepRegenPerTick, "fancymenu.decoration_overlays.buddy.energy_sleep_regen_per_tick.desc");
        addFloatEntry(statsMenu, playEnergyDrainPerTick, "fancymenu.decoration_overlays.buddy.play_energy_drain_per_tick.desc");
        addFloatEntry(statsMenu, playFunGainPerTick, "fancymenu.decoration_overlays.buddy.play_fun_gain_per_tick.desc");
        addFloatEntry(statsMenu, playHappinessGainPerTick, "fancymenu.decoration_overlays.buddy.play_happiness_gain_per_tick.desc");
        addFloatEntry(statsMenu, playHungerDrainPerTick, "fancymenu.decoration_overlays.buddy.play_hunger_drain_per_tick.desc");
        addFloatEntry(statsMenu, chaseEnergyDrainPerTick, "fancymenu.decoration_overlays.buddy.chase_energy_drain_per_tick.desc");
        addFloatEntry(statsMenu, chaseHungerDrainPerTick, "fancymenu.decoration_overlays.buddy.chase_hunger_drain_per_tick.desc");
        addFloatEntry(statsMenu, hopEnergyDrainPerTick, "fancymenu.decoration_overlays.buddy.hop_energy_drain_per_tick.desc");
        addFloatEntry(statsMenu, hopHungerDrainPerTick, "fancymenu.decoration_overlays.buddy.hop_hunger_drain_per_tick.desc");
        addFloatEntry(statsMenu, excitedEnergyDrainPerTick, "fancymenu.decoration_overlays.buddy.excited_energy_drain_per_tick.desc");
        addFloatEntry(statsMenu, excitedHappinessGainPerTick, "fancymenu.decoration_overlays.buddy.excited_happiness_gain_per_tick.desc");
        addFloatEntry(statsMenu, excitedHungerDrainPerTick, "fancymenu.decoration_overlays.buddy.excited_hunger_drain_per_tick.desc");
        addFloatEntry(statsMenu, runningEnergyDrainPerTick, "fancymenu.decoration_overlays.buddy.running_energy_drain_per_tick.desc");
        addFloatEntry(statsMenu, runningHungerDrainPerTick, "fancymenu.decoration_overlays.buddy.running_hunger_drain_per_tick.desc");
        addFloatEntry(statsMenu, foodHungerGain, "fancymenu.decoration_overlays.buddy.food_hunger_gain.desc");
        addFloatEntry(statsMenu, foodHappinessGain, "fancymenu.decoration_overlays.buddy.food_happiness_gain.desc");
        addFloatEntry(statsMenu, petHappinessGain, "fancymenu.decoration_overlays.buddy.pet_happiness_gain.desc");
        addFloatEntry(statsMenu, wakeupHappinessPenalty, "fancymenu.decoration_overlays.buddy.wakeup_happiness_penalty.desc");
        addIntEntry(statsMenu, maxPoopsCap, "fancymenu.decoration_overlays.buddy.max_poops_cap.desc");
        addBooleanEntry(statsMenu, canDie, "fancymenu.decoration_overlays.buddy.can_die.desc");

        menu.addSeparatorEntry("separator_before_reset_buddy_save").setStackable(true);
        menu.addClickableEntry("reset_buddy_save", Component.translatable("fancymenu.decoration_overlays.buddy.reset_save"), (contextMenu, entry) -> requestResetBuddySave())
                .setIcon(MaterialIcons.RESET_SETTINGS)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.buddy.reset_save.desc")))
                .setStackable(true);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        tickBuddyIfNeeded();
        this.buddyWidget.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        this.buddyWidget.setScreenSize(screen.width, screen.height);
        resetTickCounter();
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        this.buddyWidget.cleanup();
    }

    @Override
    public void setInstanceIdentifier(@NotNull String instanceIdentifier) {
        super.setInstanceIdentifier(instanceIdentifier);
        this.buddyWidget.setInstanceIdentifier(instanceIdentifier);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private void resetTickCounter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.gui != null) {
            this.lastGuiTick = minecraft.gui.getGuiTicks();
        } else {
            this.lastGuiTick = -1;
        }
    }

    private void tickBuddyIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gui == null) {
            return;
        }
        int guiTick = minecraft.gui.getGuiTicks();
        if (this.lastGuiTick < 0 || guiTick < this.lastGuiTick) {
            this.lastGuiTick = guiTick;
            return;
        }
        while (this.lastGuiTick < guiTick) {
            this.buddyWidget.tick();
            this.lastGuiTick++;
        }
    }

    private void bindTextureProperty(@NotNull Property<ResourceSupplier<ITexture>> property, @NotNull Consumer<ResourceSupplier<ITexture>> setter) {
        property.addValueSetListener((oldValue, newValue) -> setter.accept(newValue));
        setter.accept(property.get());
    }

    private void bindStatProperties() {
        Consumer<Float> update = value -> applyStatConfig();
        hungerDecayPerTick.addValueSetListener((o, n) -> update.accept(n));
        happinessDecayPerTick.addValueSetListener((o, n) -> update.accept(n));
        funDecayPerTick.addValueSetListener((o, n) -> update.accept(n));
        energyDecayPerTick.addValueSetListener((o, n) -> update.accept(n));
        energySleepRegenPerTick.addValueSetListener((o, n) -> update.accept(n));
        playEnergyDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        playFunGainPerTick.addValueSetListener((o, n) -> update.accept(n));
        playHappinessGainPerTick.addValueSetListener((o, n) -> update.accept(n));
        playHungerDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        chaseEnergyDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        chaseHungerDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        hopEnergyDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        hopHungerDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        excitedEnergyDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        excitedHappinessGainPerTick.addValueSetListener((o, n) -> update.accept(n));
        excitedHungerDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        runningEnergyDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        runningHungerDrainPerTick.addValueSetListener((o, n) -> update.accept(n));
        foodHungerGain.addValueSetListener((o, n) -> update.accept(n));
        foodHappinessGain.addValueSetListener((o, n) -> update.accept(n));
        petHappinessGain.addValueSetListener((o, n) -> update.accept(n));
        wakeupHappinessPenalty.addValueSetListener((o, n) -> update.accept(n));
        applyStatConfig();
    }

    private void bindPoopCapProperty() {
        Consumer<Integer> update = value -> this.buddyWidget.setMaxPoopsCap(Math.max(0, value));
        maxPoopsCap.addValueSetListener((o, n) -> update.accept(n));
        update.accept(maxPoopsCap.get());
    }

    private void bindCanDieProperty() {
        Consumer<Boolean> update = value -> this.buddyWidget.setCanDie(Boolean.TRUE.equals(value));
        canDie.addValueSetListener((o, n) -> update.accept(n));
        update.accept(canDie.get());
    }

    @SuppressWarnings("all")
    private void applyStatConfig() {
        BuddyStatConfig config = new BuddyStatConfig(
                hungerDecayPerTick.get(),
                happinessDecayPerTick.get(),
                funDecayPerTick.get(),
                energyDecayPerTick.get(),
                energySleepRegenPerTick.get(),
                playEnergyDrainPerTick.get(),
                playFunGainPerTick.get(),
                playHappinessGainPerTick.get(),
                playHungerDrainPerTick.get(),
                chaseEnergyDrainPerTick.get(),
                chaseHungerDrainPerTick.get(),
                hopEnergyDrainPerTick.get(),
                hopHungerDrainPerTick.get(),
                excitedEnergyDrainPerTick.get(),
                excitedHappinessGainPerTick.get(),
                excitedHungerDrainPerTick.get(),
                runningEnergyDrainPerTick.get(),
                runningHungerDrainPerTick.get(),
                foodHungerGain.get(),
                foodHappinessGain.get(),
                petHappinessGain.get(),
                wakeupHappinessPenalty.get()
        );
        this.buddyWidget.setStatConfig(config);
    }

    private void requestResetBuddySave() {
        Dialogs.openMessageWithCallback(
                Component.translatable("fancymenu.decoration_overlays.buddy.reset_save.confirm"),
                MessageDialogStyle.WARNING,
                confirmed -> {
                    if (confirmed) {
                        this.buddyWidget.resetBuddySave();
                    }
                }
        );
    }

    private void addTextureEntry(@NotNull ContextMenu menu, @NotNull Property<ResourceSupplier<ITexture>> property, @NotNull String descriptionKey) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.TEXTURE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(descriptionKey)));
    }

    private void addFloatEntry(@NotNull ContextMenu menu, @NotNull Property<Float> property, @NotNull String descriptionKey) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(descriptionKey)));
    }

    private void addIntEntry(@NotNull ContextMenu menu, @NotNull Property<Integer> property, @NotNull String descriptionKey) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FORMAT_LIST_NUMBERED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(descriptionKey)));
    }

    private void addBooleanEntry(@NotNull ContextMenu menu, @NotNull Property<Boolean> property, @NotNull String descriptionKey) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.TOGGLE_ON)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(descriptionKey)));
    }
}
