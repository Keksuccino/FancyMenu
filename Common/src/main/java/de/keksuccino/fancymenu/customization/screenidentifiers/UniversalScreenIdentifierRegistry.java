package de.keksuccino.fancymenu.customization.screenidentifiers;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.Realms32bitWarningScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.gui.screens.reporting.ChatSelectionScreen;
import net.minecraft.client.gui.screens.reporting.ReportReasonSelectionScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.realms.RealmsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class UniversalScreenIdentifierRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, String> UNIVERSAL_IDENTIFIERS = new LinkedHashMap<>();

    static {

        registerUniversalIdentifier("title_screen", TitleScreen.class.getName());
        registerUniversalIdentifier("realms_main_screen", RealmsMainScreen.class.getName());
        registerUniversalIdentifier("realms_backup_info_screen", RealmsBackupInfoScreen.class.getName());
        registerUniversalIdentifier("realms_backup_screen", RealmsBackupScreen.class.getName());
        registerUniversalIdentifier("realms_broken_world_screen", RealmsBrokenWorldScreen.class.getName());
        registerUniversalIdentifier("realms_client_outdated_screen", RealmsClientOutdatedScreen.class.getName());
        registerUniversalIdentifier("realms_configure_world_screen", RealmsConfigureWorldScreen.class.getName());
        registerUniversalIdentifier("realms_confirm_screen", RealmsConfirmScreen.class.getName());
        registerUniversalIdentifier("realms_create_realm_screen", RealmsCreateRealmScreen.class.getName());
        registerUniversalIdentifier("realms_download_latest_world_screen", RealmsDownloadLatestWorldScreen.class.getName());
        registerUniversalIdentifier("realms_generic_error_screen", RealmsGenericErrorScreen.class.getName());
        registerUniversalIdentifier("realms_invite_screen", RealmsInviteScreen.class.getName());
        registerUniversalIdentifier("realms_long_confirmation_screen", RealmsLongConfirmationScreen.class.getName());
        registerUniversalIdentifier("realms_long_running_mco_task_screen", RealmsLongRunningMcoTaskScreen.class.getName());
        registerUniversalIdentifier("realms_notifications_screen", RealmsNotificationsScreen.class.getName());
        registerUniversalIdentifier("realms_parental_consent_screen", RealmsParentalConsentScreen.class.getName());
        registerUniversalIdentifier("realms_pending_invites_screen", RealmsPendingInvitesScreen.class.getName());
        registerUniversalIdentifier("realms_player_screen", RealmsPlayerScreen.class.getName());
        registerUniversalIdentifier("realms_reset_normal_world_screen", RealmsResetNormalWorldScreen.class.getName());
        registerUniversalIdentifier("realms_reset_world_screen", RealmsResetWorldScreen.class.getName());
        registerUniversalIdentifier("realms_select_file_to_upload_screen", RealmsSelectFileToUploadScreen.class.getName());
        registerUniversalIdentifier("realms_select_world_template_screen", RealmsSelectWorldTemplateScreen.class.getName());
        registerUniversalIdentifier("realms_settings_screen", RealmsSettingsScreen.class.getName());
        registerUniversalIdentifier("realms_slot_options_screen", RealmsSlotOptionsScreen.class.getName());
        registerUniversalIdentifier("realms_subscription_info_screen", RealmsSubscriptionInfoScreen.class.getName());
        registerUniversalIdentifier("realms_terms_screen", RealmsTermsScreen.class.getName());
        registerUniversalIdentifier("realms_upload_screen", RealmsUploadScreen.class.getName());
        registerUniversalIdentifier("accessibility_onboarding_screen", AccessibilityOnboardingScreen.class.getName());
        registerUniversalIdentifier("accessibility_options_screen", AccessibilityOptionsScreen.class.getName());
        registerUniversalIdentifier("alert_screen", AlertScreen.class.getName());
        registerUniversalIdentifier("backup_confirm_screen", BackupConfirmScreen.class.getName());
        registerUniversalIdentifier("chat_options_screen", ChatOptionsScreen.class.getName());
        registerUniversalIdentifier("chat_screen", ChatScreen.class.getName());
        registerUniversalIdentifier("confirm_link_screen", ConfirmLinkScreen.class.getName());
        registerUniversalIdentifier("confirm_screen", ConfirmScreen.class.getName());
        registerUniversalIdentifier("connect_screen", ConnectScreen.class.getName());
        registerUniversalIdentifier("create_buffet_world_screen", CreateBuffetWorldScreen.class.getName());
        registerUniversalIdentifier("create_flat_world_screen", CreateFlatWorldScreen.class.getName());
        registerUniversalIdentifier("credits_and_attribution_screen", CreditsAndAttributionScreen.class.getName());
        registerUniversalIdentifier("datapack_load_failure_screen", DatapackLoadFailureScreen.class.getName());
        registerUniversalIdentifier("death_screen", DeathScreen.class.getName());
        registerUniversalIdentifier("title_confirm_screen", DeathScreen.TitleConfirmScreen.class.getName());
        registerUniversalIdentifier("demo_intro_screen", DemoIntroScreen.class.getName());
        registerUniversalIdentifier("direct_join_server_screen", DirectJoinServerScreen.class.getName());
        registerUniversalIdentifier("disconnected_screen", DisconnectedScreen.class.getName());
        registerUniversalIdentifier("edit_server_screen", EditServerScreen.class.getName());
        registerUniversalIdentifier("error_screen", ErrorScreen.class.getName());
        registerUniversalIdentifier("generic_dirt_message_screen", GenericDirtMessageScreen.class.getName());
        registerUniversalIdentifier("generic_waiting_screen", GenericWaitingScreen.class.getName());
        registerUniversalIdentifier("in_bed_chat_screen", InBedChatScreen.class.getName());
        registerUniversalIdentifier("language_select_screen", LanguageSelectScreen.class.getName());
        registerUniversalIdentifier("level_loading_screen", LevelLoadingScreen.class.getName());
        registerUniversalIdentifier("mouse_settings_screen", MouseSettingsScreen.class.getName());
        registerUniversalIdentifier("online_options_screen", OnlineOptionsScreen.class.getName());
        registerUniversalIdentifier("options_screen", OptionsScreen.class.getName());
        registerUniversalIdentifier("options_sub_screen", OptionsSubScreen.class.getName());
        registerUniversalIdentifier("out_of_memory_screen", OutOfMemoryScreen.class.getName());
        registerUniversalIdentifier("pause_screen", PauseScreen.class.getName());
        registerUniversalIdentifier("popup_screen", PopupScreen.class.getName());
        registerUniversalIdentifier("preset_flat_world_screen", PresetFlatWorldScreen.class.getName());
        registerUniversalIdentifier("progress_screen", ProgressScreen.class.getName());
        registerUniversalIdentifier("receiving_level_screen", ReceivingLevelScreen.class.getName());
        registerUniversalIdentifier("share_to_lan_screen", ShareToLanScreen.class.getName());
        registerUniversalIdentifier("skin_customization_screen", SkinCustomizationScreen.class.getName());
        registerUniversalIdentifier("sound_options_screen", SoundOptionsScreen.class.getName());
        registerUniversalIdentifier("video_settings_screen", VideoSettingsScreen.class.getName());
        registerUniversalIdentifier("win_screen", WinScreen.class.getName());
        registerUniversalIdentifier("stats_screen", StatsScreen.class.getName());
        registerUniversalIdentifier("advancements_screen", AdvancementsScreen.class.getName());
        registerUniversalIdentifier("controls_screen", ControlsScreen.class.getName());
        registerUniversalIdentifier("key_binds_screen", KeyBindsList.class.getName());
        registerUniversalIdentifier("game_mode_switcher_screen", GameModeSwitcherScreen.class.getName());
        registerUniversalIdentifier("anvil_screen", AnvilScreen.class.getName());
        registerUniversalIdentifier("beacon_screen", BeaconScreen.class.getName());
        registerUniversalIdentifier("blast_furnace_screen", BlastFurnaceScreen.class.getName());
        registerUniversalIdentifier("book_edit_screen", BookEditScreen.class.getName());
        registerUniversalIdentifier("book_view_screen", BookViewScreen.class.getName());
        registerUniversalIdentifier("brewing_stand_screen", BrewingStandScreen.class.getName());
        registerUniversalIdentifier("cartography_table_screen", CartographyTableScreen.class.getName());
        registerUniversalIdentifier("command_block_edit_screen", CommandBlockEditScreen.class.getName());
        registerUniversalIdentifier("container_screen", ContainerScreen.class.getName());
        registerUniversalIdentifier("crafting_screen", CraftingScreen.class.getName());
        registerUniversalIdentifier("creative_mode_inventory_screen", CreativeModeInventoryScreen.class.getName());
        registerUniversalIdentifier("dispenser_screen", DispenserScreen.class.getName());
        registerUniversalIdentifier("effect_rendering_inventory_screen", EffectRenderingInventoryScreen.class.getName());
        registerUniversalIdentifier("enchantment_screen", EnchantmentScreen.class.getName());
        registerUniversalIdentifier("furnace_screen", FurnaceScreen.class.getName());
        registerUniversalIdentifier("grindstone_screen", GrindstoneScreen.class.getName());
        registerUniversalIdentifier("hanging_sign_edit_screen", HangingSignEditScreen.class.getName());
        registerUniversalIdentifier("hopper_screen", HopperScreen.class.getName());
        registerUniversalIdentifier("horse_inventory_screen", HorseInventoryScreen.class.getName());
        registerUniversalIdentifier("inventory_screen", InventoryScreen.class.getName());
        registerUniversalIdentifier("item_combiner_screen", ItemCombinerScreen.class.getName());
        registerUniversalIdentifier("jigsaw_block_edit_screen", JigsawBlockEditScreen.class.getName());
        registerUniversalIdentifier("lectern_screen", LecternScreen.class.getName());
        registerUniversalIdentifier("loom_screen", LoomScreen.class.getName());
        registerUniversalIdentifier("merchant_screen", MerchantScreen.class.getName());
        registerUniversalIdentifier("minecart_command_block_edit_screen", MinecartCommandBlockEditScreen.class.getName());
        registerUniversalIdentifier("shulker_box_screen", ShulkerBoxScreen.class.getName());
        registerUniversalIdentifier("sign_edit_screen", SignEditScreen.class.getName());
        registerUniversalIdentifier("smithing_screen", SmithingScreen.class.getName());
        registerUniversalIdentifier("smoker_screen", SmokerScreen.class.getName());
        registerUniversalIdentifier("stonecutter_screen", StonecutterScreen.class.getName());
        registerUniversalIdentifier("structure_block_edit_screen", StructureBlockEditScreen.class.getName());
        registerUniversalIdentifier("join_multiplayer_screen", JoinMultiplayerScreen.class.getName());
        registerUniversalIdentifier("realms_32bit_warning_screen", Realms32bitWarningScreen.class.getName());
        registerUniversalIdentifier("safety_screen", SafetyScreen.class.getName());
        registerUniversalIdentifier("warning_screen", WarningScreen.class.getName());
        registerUniversalIdentifier("pack_selection_screen", PackSelectionScreen.class.getName());
        registerUniversalIdentifier("chat_report_screen", ChatReportScreen.class.getName());
        registerUniversalIdentifier("chat_selection_screen", ChatSelectionScreen.class.getName());
        registerUniversalIdentifier("report_reason_selection_screen", ReportReasonSelectionScreen.class.getName());
        registerUniversalIdentifier("social_interactions_screen", SocialInteractionsScreen.class.getName());
        registerUniversalIdentifier("telemetry_info_screen", TelemetryInfoScreen.class.getName());
        registerUniversalIdentifier("confirm_experimental_features_screen", ConfirmExperimentalFeaturesScreen.class.getName());
        registerUniversalIdentifier("create_world_screen", CreateWorldScreen.class.getName());
        registerUniversalIdentifier("edit_game_rules_screen", EditGameRulesScreen.class.getName());
        registerUniversalIdentifier("edit_world_screen", EditWorldScreen.class.getName());
        registerUniversalIdentifier("experiments_screen", ExperimentsScreen.class.getName());
        registerUniversalIdentifier("optimize_world_screen", OptimizeWorldScreen.class.getName());
        registerUniversalIdentifier("select_world_screen", SelectWorldScreen.class.getName());
        registerUniversalIdentifier("disconnected_realms_screen", DisconnectedRealmsScreen.class.getName());
        registerUniversalIdentifier("realms_screen", RealmsScreen.class.getName());

    }

    public static void registerUniversalIdentifier(@NotNull String universalIdentifier, @NotNull String targetScreenClassPath) {
        if (UNIVERSAL_IDENTIFIERS.containsKey(universalIdentifier)) {
            LOGGER.warn("[FANCYMENU] Universal identifier '" + universalIdentifier + "' already registered! Replacing identifier..");
        }
        UNIVERSAL_IDENTIFIERS.put(universalIdentifier, targetScreenClassPath);
    }

    @Nullable
    public static String getScreenForUniversalIdentifier(@NotNull String universalIdentifier) {
        return UNIVERSAL_IDENTIFIERS.get(universalIdentifier);
    }

    @NotNull
    public static String tryGetUniversalIdentifierFor(@NotNull String screenClassPath) {
        String universal = getUniversalIdentifierFor(screenClassPath);
        return (universal != null) ? universal : screenClassPath;
    }

    @Nullable
    public static String getUniversalIdentifierFor(@NotNull String screenClassPath) {
        if (universalIdentifierExists(screenClassPath)) return screenClassPath;
        for (Map.Entry<String, String> m : UNIVERSAL_IDENTIFIERS.entrySet()) {
            if (m.getValue().equals(screenClassPath)) return m.getKey();
        }
        return null;
    }

    @Nullable
    public static String getUniversalIdentifierFor(@NotNull Screen screen) {
        return getUniversalIdentifierFor(screen.getClass().getName());
    }

    @NotNull
    public static List<String> getUniversalIdentifiers() {
        return new ArrayList<>(UNIVERSAL_IDENTIFIERS.keySet());
    }

    public static boolean universalIdentifierExists(@NotNull String identifier) {
        return UNIVERSAL_IDENTIFIERS.containsKey(identifier);
    }

}
