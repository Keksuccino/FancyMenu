package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves game rule values by name for the client and server side of the {@link GameruleValuePlaceholder}.<br>
 * Accepts current game rule IDs (like {@code advance_time} or {@code minecraft:advance_time}) and the camelCase names game rules had
 * before Minecraft 1.21.11 (like {@code doDaylightCycle}), ignoring case and surrounding whitespace.
 */
public final class GameruleValueResolver {

    /**
     * Maps the lowercased camelCase game rule names from before Minecraft 1.21.11 to the game rules that replaced them.<br>
     * Mirrors vanilla's {@code GameRuleRegistryFix}, which converts these rules when loading old worlds, so layouts made for older versions keep working.<br>
     * Rules that fix drops without a replacement ({@code spawnChunkRadius}, {@code entitiesWithPassengersCanUsePortals}, {@code gameLoopFunction})
     * are left out on purpose, and so is {@code pvp}, because its old name is also its current ID.
     */
    private static final Map<String, LegacyGameRule<?>> LEGACY_GAME_RULES = createLegacyGameRules();

    private GameruleValueResolver() {
    }

    /**
     * Returns the value of the game rule with the given name as string, or {@code null} if no game rule with that name is available in the given {@link GameRules}.<br>
     * Values of old game rule names that got inverted or merged into another rule get converted back to what the old rule would return.
     */
    @Nullable
    public static String getValue(@NotNull GameRules gameRules, @Nullable String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }
        String key = normalizedName.toLowerCase(Locale.ROOT);
        Identifier identifier = Identifier.tryParse(key);
        GameRule<?> gameRule = (identifier != null) ? BuiltInRegistries.GAME_RULE.getValue(identifier) : null;
        if (gameRule != null) {
            return isAvailable(gameRules, gameRule) ? gameRules.getAsString(gameRule) : null;
        }
        LegacyGameRule<?> legacyGameRule = LEGACY_GAME_RULES.get(key);
        if ((legacyGameRule != null) && isAvailable(gameRules, legacyGameRule.gameRule())) {
            return legacyGameRule.getValue(gameRules);
        }
        return null;
    }

    /**
     * Returns the trimmed game rule name, or {@code null} if the name is {@code null} or blank.
     */
    @Nullable
    public static String normalizeName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * {@link GameRules} only contains the rules whose required features are enabled in the world, and throws when reading any other rule.
     */
    private static boolean isAvailable(@NotNull GameRules gameRules, @NotNull GameRule<?> gameRule) {
        return gameRules.availableRules().anyMatch(availableRule -> availableRule == gameRule);
    }

    @NotNull
    private static Map<String, LegacyGameRule<?>> createLegacyGameRules() {
        Map<String, LegacyGameRule<?>> map = new HashMap<>();
        putRenamed(map, "allowEnteringNetherUsingPortals", GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS);
        putRenamed(map, "announceAdvancements", GameRules.SHOW_ADVANCEMENT_MESSAGES);
        putRenamed(map, "blockExplosionDropDecay", GameRules.BLOCK_EXPLOSION_DROP_DECAY);
        putRenamed(map, "commandBlockOutput", GameRules.COMMAND_BLOCK_OUTPUT);
        putRenamed(map, "enableCommandBlocks", GameRules.COMMAND_BLOCKS_WORK);
        putRenamed(map, "commandBlocksEnabled", GameRules.COMMAND_BLOCKS_WORK);
        putRenamed(map, "commandModificationBlockLimit", GameRules.MAX_BLOCK_MODIFICATIONS);
        putInverted(map, "disableElytraMovementCheck", GameRules.ELYTRA_MOVEMENT_CHECK);
        putInverted(map, "disablePlayerMovementCheck", GameRules.PLAYER_MOVEMENT_CHECK);
        putInverted(map, "disableRaids", GameRules.RAIDS);
        putRenamed(map, "doDaylightCycle", GameRules.ADVANCE_TIME);
        putRenamed(map, "doEntityDrops", GameRules.ENTITY_DROPS);
        // The old fire rules got merged into one radius rule: 0 means fire never spreads, -1 means it spreads everywhere, other values limit it to that distance around players
        put(map, "doFireTick", new LegacyGameRule<>(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius -> String.valueOf(radius != 0)));
        put(map, "allowFireTicksAwayFromPlayer", new LegacyGameRule<>(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius -> String.valueOf(radius == -1)));
        putRenamed(map, "doImmediateRespawn", GameRules.IMMEDIATE_RESPAWN);
        putRenamed(map, "doInsomnia", GameRules.SPAWN_PHANTOMS);
        putRenamed(map, "doLimitedCrafting", GameRules.LIMITED_CRAFTING);
        putRenamed(map, "doMobLoot", GameRules.MOB_DROPS);
        putRenamed(map, "doMobSpawning", GameRules.SPAWN_MOBS);
        putRenamed(map, "doPatrolSpawning", GameRules.SPAWN_PATROLS);
        putRenamed(map, "doTileDrops", GameRules.BLOCK_DROPS);
        putRenamed(map, "doTraderSpawning", GameRules.SPAWN_WANDERING_TRADERS);
        putRenamed(map, "doVinesSpread", GameRules.SPREAD_VINES);
        putRenamed(map, "doWardenSpawning", GameRules.SPAWN_WARDENS);
        putRenamed(map, "doWeatherCycle", GameRules.ADVANCE_WEATHER);
        putRenamed(map, "drowningDamage", GameRules.DROWNING_DAMAGE);
        putRenamed(map, "enderPearlsVanishOnDeath", GameRules.ENDER_PEARLS_VANISH_ON_DEATH);
        putRenamed(map, "fallDamage", GameRules.FALL_DAMAGE);
        putRenamed(map, "fireDamage", GameRules.FIRE_DAMAGE);
        putRenamed(map, "forgiveDeadPlayers", GameRules.FORGIVE_DEAD_PLAYERS);
        putRenamed(map, "freezeDamage", GameRules.FREEZE_DAMAGE);
        putRenamed(map, "globalSoundEvents", GameRules.GLOBAL_SOUND_EVENTS);
        putRenamed(map, "keepInventory", GameRules.KEEP_INVENTORY);
        putRenamed(map, "lavaSourceConversion", GameRules.LAVA_SOURCE_CONVERSION);
        putRenamed(map, "locatorBar", GameRules.LOCATOR_BAR);
        putRenamed(map, "logAdminCommands", GameRules.LOG_ADMIN_COMMANDS);
        putRenamed(map, "maxCommandChainLength", GameRules.MAX_COMMAND_SEQUENCE_LENGTH);
        putRenamed(map, "maxCommandForkCount", GameRules.MAX_COMMAND_FORKS);
        putRenamed(map, "maxEntityCramming", GameRules.MAX_ENTITY_CRAMMING);
        putRenamed(map, "minecartMaxSpeed", GameRules.MAX_MINECART_SPEED);
        putRenamed(map, "mobExplosionDropDecay", GameRules.MOB_EXPLOSION_DROP_DECAY);
        putRenamed(map, "mobGriefing", GameRules.MOB_GRIEFING);
        putRenamed(map, "naturalRegeneration", GameRules.NATURAL_HEALTH_REGENERATION);
        putRenamed(map, "playersNetherPortalCreativeDelay", GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY);
        putRenamed(map, "playersNetherPortalDefaultDelay", GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY);
        putRenamed(map, "playersSleepingPercentage", GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        putRenamed(map, "projectilesCanBreakBlocks", GameRules.PROJECTILES_CAN_BREAK_BLOCKS);
        putRenamed(map, "randomTickSpeed", GameRules.RANDOM_TICK_SPEED);
        putRenamed(map, "reducedDebugInfo", GameRules.REDUCED_DEBUG_INFO);
        putRenamed(map, "sendCommandFeedback", GameRules.SEND_COMMAND_FEEDBACK);
        putRenamed(map, "showDeathMessages", GameRules.SHOW_DEATH_MESSAGES);
        putRenamed(map, "snowAccumulationHeight", GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        putRenamed(map, "spawnMonsters", GameRules.SPAWN_MONSTERS);
        putRenamed(map, "spawnRadius", GameRules.RESPAWN_RADIUS);
        putRenamed(map, "spawnerBlocksEnabled", GameRules.SPAWNER_BLOCKS_WORK);
        putRenamed(map, "spectatorsGenerateChunks", GameRules.SPECTATORS_GENERATE_CHUNKS);
        putRenamed(map, "tntExplodes", GameRules.TNT_EXPLODES);
        putRenamed(map, "tntExplosionDropDecay", GameRules.TNT_EXPLOSION_DROP_DECAY);
        putRenamed(map, "universalAnger", GameRules.UNIVERSAL_ANGER);
        putRenamed(map, "waterSourceConversion", GameRules.WATER_SOURCE_CONVERSION);
        return Map.copyOf(map);
    }

    private static <T> void putRenamed(@NotNull Map<String, LegacyGameRule<?>> map, @NotNull String legacyName, @NotNull GameRule<T> gameRule) {
        put(map, legacyName, new LegacyGameRule<>(gameRule, gameRule::serialize));
    }

    private static void putInverted(@NotNull Map<String, LegacyGameRule<?>> map, @NotNull String legacyName, @NotNull GameRule<Boolean> gameRule) {
        put(map, legacyName, new LegacyGameRule<>(gameRule, value -> String.valueOf(!value)));
    }

    private static void put(@NotNull Map<String, LegacyGameRule<?>> map, @NotNull String legacyName, @NotNull LegacyGameRule<?> legacyGameRule) {
        if (map.put(legacyName.toLowerCase(Locale.ROOT), legacyGameRule) != null) {
            throw new IllegalStateException("Duplicate legacy game rule name: " + legacyName);
        }
    }

    private record LegacyGameRule<T>(@NotNull GameRule<T> gameRule, @NotNull Function<T, String> valueConverter) {

        @NotNull
        private String getValue(@NotNull GameRules gameRules) {
            return this.valueConverter.apply(gameRules.get(this.gameRule));
        }

    }

}
