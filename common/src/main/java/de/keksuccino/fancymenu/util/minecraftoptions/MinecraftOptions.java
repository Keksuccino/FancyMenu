package de.keksuccino.fancymenu.util.minecraftoptions;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.*;

@SuppressWarnings("all")
public class MinecraftOptions {

    private static final TypeToken<List<String>> RESOURCE_PACK_TYPE = new TypeToken<>() {};
    private static final Gson GSON = new Gson();

    private static final Map<String, MinecraftOption> CACHED_OPTIONS = new LinkedHashMap<>();

    @NotNull
    public static Map<String, MinecraftOption> getOptions() {
        cacheOptions();
        return CACHED_OPTIONS;
    }

    @Nullable
    public static MinecraftOption getOption(@NotNull String name) {
        return getOptions().get(name);
    }

    public static void save() {
        Minecraft.getInstance().options.save();
    }

    private static void cacheOptions() {

        if (CACHED_OPTIONS.isEmpty()) {

            try {

                Options options = Minecraft.getInstance().options;

                //BOOLEAN
                cachePrimitive(Boolean.class, "autoJump", () -> options.autoJump, aBoolean -> options.autoJump = aBoolean);
                cachePrimitive(Boolean.class, "autoSuggestions", () -> options.autoSuggestions, aBoolean -> options.autoSuggestions = aBoolean);
                cachePrimitive(Boolean.class, "chatColors", () -> options.chatColors, aBoolean -> options.chatColors = aBoolean);
                cachePrimitive(Boolean.class, "chatLinks", () -> options.chatLinks, aBoolean -> options.chatLinks = aBoolean);
                cachePrimitive(Boolean.class, "chatLinksPrompt", () -> options.chatLinksPrompt, aBoolean -> options.chatLinksPrompt = aBoolean);
                cachePrimitive(Boolean.class, "enableVsync", () -> options.enableVsync, aBoolean -> options.enableVsync = aBoolean);
                cachePrimitive(Boolean.class, "entityShadows", () -> options.entityShadows, aBoolean -> options.entityShadows = aBoolean);
                cachePrimitive(Boolean.class, "forceUnicodeFont", () -> options.forceUnicodeFont, aBoolean -> options.forceUnicodeFont = aBoolean);
                cachePrimitive(Boolean.class, "discrete_mouse_scroll", () -> options.discreteMouseScroll, aBoolean -> options.discreteMouseScroll = aBoolean);
                cachePrimitive(Boolean.class, "invertYMouse", () -> options.invertYMouse, aBoolean -> options.invertYMouse = aBoolean);
                cachePrimitive(Boolean.class, "realmsNotifications", () -> options.realmsNotifications, aBoolean -> options.realmsNotifications = aBoolean);
                cachePrimitive(Boolean.class, "reducedDebugInfo", () -> options.reducedDebugInfo, aBoolean -> options.reducedDebugInfo = aBoolean);
                cachePrimitive(Boolean.class, "showSubtitles", () -> options.showSubtitles, aBoolean -> options.showSubtitles = aBoolean);
                cachePrimitive(Boolean.class, "touchscreen", () -> options.touchscreen, aBoolean -> options.touchscreen = aBoolean);
                cachePrimitive(Boolean.class, "fullscreen", () -> options.fullscreen, aBoolean -> options.fullscreen = aBoolean);
                cachePrimitive(Boolean.class, "bobView", () -> options.bobView, aBoolean -> options.bobView = aBoolean);
                cachePrimitive(Boolean.class, "toggleCrouch", () -> options.toggleCrouch, aBoolean -> options.toggleCrouch = aBoolean);
                cachePrimitive(Boolean.class, "toggleSprint", () -> options.toggleSprint, aBoolean -> options.toggleSprint = aBoolean);
                cachePrimitive(Boolean.class, "darkMojangStudiosBackground", () -> options.darkMojangStudiosBackground, aBoolean -> options.darkMojangStudiosBackground = aBoolean);
                cachePrimitive(Boolean.class, "hideLightningFlashes", () -> options.hideLightningFlashes, value -> options.hideLightningFlashes = value);
                cachePrimitive(Boolean.class, "backgroundForChatOnly", () -> options.backgroundForChatOnly, aBoolean -> options.backgroundForChatOnly = aBoolean);
                cachePrimitive(Boolean.class, "hideServerAddress", () -> options.hideServerAddress, aBoolean -> options.hideServerAddress = aBoolean);
                cachePrimitive(Boolean.class, "advancedItemTooltips", () -> options.advancedItemTooltips, aBoolean -> options.advancedItemTooltips = aBoolean);
                cachePrimitive(Boolean.class, "pauseOnLostFocus", () -> options.pauseOnLostFocus, aBoolean -> options.pauseOnLostFocus = aBoolean);
                cachePrimitive(Boolean.class, "heldItemTooltips", () -> options.heldItemTooltips, aBoolean -> options.heldItemTooltips = aBoolean);
                cachePrimitive(Boolean.class, "useNativeTransport", () -> options.useNativeTransport, aBoolean -> options.useNativeTransport = aBoolean);
                cachePrimitive(Boolean.class, "rawMouseInput", () -> options.rawMouseInput, aBoolean -> options.rawMouseInput = aBoolean);
                cachePrimitive(Boolean.class, "skipMultiplayerWarning", () -> options.skipMultiplayerWarning, aBoolean -> options.skipMultiplayerWarning = aBoolean);
                cachePrimitive(Boolean.class, "skipRealms32bitWarning", () -> options.skipRealms32bitWarning, aBoolean -> options.skipRealms32bitWarning = aBoolean);
                cachePrimitive(Boolean.class, "hideMatchedNames", () -> options.hideMatchedNames, aBoolean -> options.hideMatchedNames = aBoolean);
                cachePrimitive(Boolean.class, "joinedFirstServer", () -> options.joinedFirstServer, aBoolean -> options.joinedFirstServer = aBoolean);
                cachePrimitive(Boolean.class, "hideBundleTutorial", () -> options.hideBundleTutorial, aBoolean -> options.hideBundleTutorial = aBoolean);
                cachePrimitive(Boolean.class, "syncChunkWrites", () -> options.syncWrites, aBoolean -> options.syncWrites = aBoolean);
                cachePrimitive(Boolean.class, "showAutosaveIndicator", () -> options.showAutosaveIndicator, aBoolean -> options.showAutosaveIndicator = aBoolean);
                cachePrimitive(Boolean.class, "allowServerListing", () -> options.allowServerListing, aBoolean -> options.allowServerListing = aBoolean);

                //DOUBLE
                cachePrimitive(Double.class, "mouseSensitivity", () -> options.sensitivity, value -> options.sensitivity = value);
                cachePrimitive(Double.class, "fov", () -> options.fov, value -> options.fov = value);
                cachePrimitive(Double.class, "gamma", () -> options.gamma, value -> options.gamma = value);
                cachePrimitive(Double.class, "chatOpacity", () -> options.chatOpacity, value -> options.chatOpacity = value);
                cachePrimitive(Double.class, "chatLineSpacing", () -> options.chatLineSpacing, value -> options.chatLineSpacing = value);
                cachePrimitive(Double.class, "textBackgroundOpacity", () -> options.textBackgroundOpacity, value -> options.textBackgroundOpacity = value);
                cachePrimitive(Double.class, "chatHeightFocused", () -> options.chatHeightFocused, value -> options.chatHeightFocused = value);
                cachePrimitive(Double.class, "chatHeightUnfocused", () -> options.chatHeightUnfocused, value -> options.chatHeightUnfocused = value);
                cachePrimitive(Double.class, "chatDelay", () -> options.chatDelay, value -> options.chatDelay = value);
                cachePrimitive(Double.class, "chatScale", () -> options.chatScale, value -> options.chatScale = value);
                cachePrimitive(Double.class, "chatWidth", () -> options.chatWidth, value -> options.chatWidth = value);
                cachePrimitive(Double.class, "mouseWheelSensitivity", () -> options.mouseWheelSensitivity, value -> options.mouseWheelSensitivity = value);

                //FLOAT
                cachePrimitive(Float.class, "screenEffectScale", () -> options.screenEffectScale, value -> options.screenEffectScale = value);
                cachePrimitive(Float.class, "fovEffectScale", () -> options.fovEffectScale, value -> options.fovEffectScale = value);
                cachePrimitive(Float.class, "entityDistanceScaling", () -> options.entityDistanceScaling, value -> options.entityDistanceScaling = value);

                //INTEGER
                cachePrimitive(Integer.class, "renderDistance", () -> options.renderDistance, value -> options.renderDistance = value);
                cachePrimitive(Integer.class, "simulationDistance", () -> options.simulationDistance, value -> options.simulationDistance = value);
                cachePrimitive(Integer.class, "guiScale", () -> options.guiScale, value -> options.guiScale = value);
                cachePrimitive(Integer.class, "maxFps", () -> options.framerateLimit, value -> options.framerateLimit = value);
                cachePrimitive(Integer.class, "biomeBlendRadius", () -> options.biomeBlendRadius, value -> options.biomeBlendRadius = value);
                cachePrimitive(Integer.class, "overrideWidth", () -> options.overrideWidth, value -> options.overrideWidth = value);
                cachePrimitive(Integer.class, "overrideHeight", () -> options.overrideHeight, value -> options.overrideHeight = value);
                cachePrimitive(Integer.class, "mipmapLevels", () -> options.mipmapLevels, value -> options.mipmapLevels = value);
                cachePrimitive(Integer.class, "glDebugVerbosity", () -> options.glDebugVerbosity, value -> options.glDebugVerbosity = value);

                //STRING
                cachePrimitive(String.class, "lastServer", () -> options.lastMpIp, value -> options.lastMpIp = value);
                cachePrimitive(String.class, "lang", () -> options.languageCode, value -> options.languageCode = value);
                cachePrimitive(String.class, "soundDevice", () -> options.soundDevice, value -> options.soundDevice = value);

                //INT-BASED
                cacheIntBased(ParticleStatus.class, "particles", () -> options.particles, value -> options.particles = value, ParticleStatus::byId, ParticleStatus::getId);
                cacheIntBased(Difficulty.class, "difficulty", () -> options.difficulty, value -> options.difficulty = value, Difficulty::byId, Difficulty::getId);
                cacheIntBased(GraphicsStatus.class, "graphicsMode", () -> options.graphicsMode, value -> options.graphicsMode = value, GraphicsStatus::byId, GraphicsStatus::getId);
                cacheIntBased(PrioritizeChunkUpdates.class, "prioritizeChunkUpdates", () -> options.prioritizeChunkUpdates, value -> options.prioritizeChunkUpdates = value, PrioritizeChunkUpdates::byId, PrioritizeChunkUpdates::getId);
                cacheIntBased(ChatVisiblity.class, "chatVisibility", () -> options.chatVisibility, value -> options.chatVisibility = value, ChatVisiblity::byId, ChatVisiblity::getId);
                cacheIntBased(AttackIndicatorStatus.class, "attackIndicator", () -> options.attackIndicator, value -> options.attackIndicator = value, AttackIndicatorStatus::byId, AttackIndicatorStatus::getId);
                cacheIntBased(NarratorStatus.class, "narrator", () -> options.narratorStatus, value -> options.narratorStatus = value, NarratorStatus::byId, NarratorStatus::getId);

                //GENERIC
                cache(AmbientOcclusionStatus.class, "ao", () -> options.ambientOcclusion, value -> options.ambientOcclusion = value, MinecraftOptions::readAmbientOcclusion, value -> Integer.toString(value.getId()));
                cache(CloudStatus.class, "renderClouds", () -> options.renderClouds, value -> options.renderClouds = value, MinecraftOptions::readCloudStatus, MinecraftOptions::writeCloudStatus);
                cache(List.class, "resourcePacks", () -> options.resourcePacks, list -> options.resourcePacks = list, MinecraftOptions::readPackList, GSON::toJson);
                cache(List.class, "incompatibleResourcePacks", () -> options.incompatibleResourcePacks, list -> options.incompatibleResourcePacks = list, MinecraftOptions::readPackList, GSON::toJson);
                cache(HumanoidArm.class, "mainHand", () -> options.mainHand, value -> options.mainHand = value, MinecraftOptions::readMainHand, MinecraftOptions::writeMainHand);
                cache(TutorialSteps.class, "tutorialStep", () -> options.tutorialStep, value -> options.tutorialStep = value, TutorialSteps::getByName, TutorialSteps::getName);

                for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                    MinecraftOption option = MinecraftOption.of(keyMapping);
                    CACHED_OPTIONS.put(option.name, option);
                }

                for(SoundSource source : SoundSource.values()) {
                    cachePrimitive(Float.class, "soundCategory_" + source.getName(), () -> options.getSoundSourceVolume(source), aFloat -> options.setSoundCategoryVolume(source, aFloat));
                }

                for (PlayerModelPart modelPart : PlayerModelPart.values()) {
                    MinecraftOption option = MinecraftOption.of(modelPart);
                    CACHED_OPTIONS.put(option.name, option);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

    }

    private static <T> void cache(@NotNull Class<T> type, @NotNull String name, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, @NotNull Function<String, T> fromString, @NotNull Function<T, String> toString) {
        CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<>(name, getter, setter, new MinecraftOption.OptionCodec<T>(toString::apply, fromString::apply))));
    }

    private static <T> void cacheIntBased(@NotNull Class<T> type, @NotNull String name, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter, @NotNull IntFunction<T> fromInt, @NotNull ToIntFunction<T> toInt) {
        cache(type, name, getter, setter, s -> {
            if (MathUtils.isInteger(s)) return fromInt.apply(Integer.parseInt(s));
            return null;
        }, t -> "" + toInt.applyAsInt(t));
    }

    private static <T> void cachePrimitive(@NotNull Class<T> type, @NotNull String name, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter) {
        if (type == Integer.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<Integer>(name, (Supplier<Integer>) getter, (Consumer<Integer>) setter, MinecraftOption.OptionCodec.INTEGER_CODEC)));
        } else if (type == Long.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<Long>(name, (Supplier<Long>) getter, (Consumer<Long>) setter, MinecraftOption.OptionCodec.LONG_CODEC)));
        } else if (type == Double.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<Double>(name, (Supplier<Double>) getter, (Consumer<Double>) setter, MinecraftOption.OptionCodec.DOUBLE_CODEC)));
        } else if (type == Float.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<Float>(name, (Supplier<Float>) getter, (Consumer<Float>) setter, MinecraftOption.OptionCodec.FLOAT_CODEC)));
        } else if (type == Boolean.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<Boolean>(name, (Supplier<Boolean>) getter, (Consumer<Boolean>) setter, MinecraftOption.OptionCodec.BOOLEAN_CODEC)));
        } else if (type == String.class) {
            CACHED_OPTIONS.put(name, MinecraftOption.of(name, new MinecraftOption.OptionInstance<String>(name, (Supplier<String>) getter, (Consumer<String>) setter, MinecraftOption.OptionCodec.STRING_CODEC)));
        }
    }

    private static List<String> readPackList(String serialized) {
        List<String> packList = GsonHelper.fromJson(GSON, serialized, RESOURCE_PACK_TYPE);
        return packList != null ? packList : Lists.newArrayList();
    }

    private static CloudStatus readCloudStatus(String serialized) {
        switch(serialized) {
            case "true":
                return CloudStatus.FANCY;
            case "fast":
                return CloudStatus.FAST;
            case "false":
            default:
                return CloudStatus.OFF;
        }
    }

    private static String writeCloudStatus(CloudStatus cloudStatus) {
        switch(cloudStatus) {
            case FANCY:
                return "true";
            case FAST:
                return "fast";
            case OFF:
            default:
                return "false";
        }
    }

    private static AmbientOcclusionStatus readAmbientOcclusion(String serialized) {
        if (isTrue(serialized)) {
            return AmbientOcclusionStatus.MAX;
        } else {
            return isFalse(serialized) ? AmbientOcclusionStatus.OFF : AmbientOcclusionStatus.byId(Integer.parseInt(serialized));
        }
    }

    private static HumanoidArm readMainHand(String serialized) {
        return "left".equals(serialized) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    private static String writeMainHand(HumanoidArm mainHand) {
        return mainHand == HumanoidArm.LEFT ? "left" : "right";
    }

    static boolean isTrue(String serialized) {
        return "true".equals(serialized);
    }

    static boolean isFalse(String serialized) {
        return "false".equals(serialized);
    }

}
