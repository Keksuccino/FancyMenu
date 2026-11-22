package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameruleValueResolverTest {

    /**
     * Every old game rule name that vanilla's {@code GameRuleRegistryFix} renames to a current game rule.
     */
    private static final List<String> LEGACY_NAMES = List.of(
            "allowEnteringNetherUsingPortals", "announceAdvancements", "blockExplosionDropDecay", "commandBlockOutput", "enableCommandBlocks",
            "commandBlocksEnabled", "commandModificationBlockLimit", "disableElytraMovementCheck", "disablePlayerMovementCheck", "disableRaids",
            "doDaylightCycle", "doEntityDrops", "doFireTick", "allowFireTicksAwayFromPlayer", "doImmediateRespawn", "doInsomnia", "doLimitedCrafting",
            "doMobLoot", "doMobSpawning", "doPatrolSpawning", "doTileDrops", "doTraderSpawning", "doVinesSpread", "doWardenSpawning", "doWeatherCycle",
            "drowningDamage", "enderPearlsVanishOnDeath", "fallDamage", "fireDamage", "forgiveDeadPlayers", "freezeDamage", "globalSoundEvents",
            "keepInventory", "lavaSourceConversion", "locatorBar", "logAdminCommands", "maxCommandChainLength", "maxCommandForkCount", "maxEntityCramming",
            "minecartMaxSpeed", "mobExplosionDropDecay", "mobGriefing", "naturalRegeneration", "playersNetherPortalCreativeDelay",
            "playersNetherPortalDefaultDelay", "playersSleepingPercentage", "projectilesCanBreakBlocks", "pvp", "randomTickSpeed", "reducedDebugInfo",
            "sendCommandFeedback", "showDeathMessages", "snowAccumulationHeight", "spawnMonsters", "spawnRadius", "spawnerBlocksEnabled",
            "spectatorsGenerateChunks", "tntExplodes", "tntExplosionDropDecay", "universalAnger", "waterSourceConversion");

    @BeforeAll
    static void setUp() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void defaultPlaceholderNameIsACurrentGameRuleId() {
        GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        gameRules.set(GameRules.ADVANCE_TIME, false, null);
        String defaultName = new GameruleValuePlaceholder().getDefaultPlaceholderString().values.get("name");
        Identifier defaultId = Identifier.tryParse(defaultName);

        assertNotNull(defaultId, defaultName);
        assertTrue(BuiltInRegistries.GAME_RULE.containsKey(defaultId), defaultName);
        assertEquals("false", GameruleValueResolver.getValue(gameRules, defaultName));
    }

    @Test
    void resolvesCurrentIdsWithAndWithoutNamespaceIgnoringCaseAndWhitespace() {
        GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        gameRules.set(GameRules.KEEP_INVENTORY, true, null);
        gameRules.set(GameRules.RANDOM_TICK_SPEED, 7, null);

        assertEquals("true", GameruleValueResolver.getValue(gameRules, "keep_inventory"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "minecraft:keep_inventory"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "  KEEP_INVENTORY "));
        assertEquals("7", GameruleValueResolver.getValue(gameRules, "random_tick_speed"));
    }

    @Test
    void resolvesRenamedLegacyNamesToTheValueOfTheirNewGameRule() {
        GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        gameRules.set(GameRules.KEEP_INVENTORY, true, null);
        gameRules.set(GameRules.ADVANCE_TIME, false, null);
        gameRules.set(GameRules.RANDOM_TICK_SPEED, 7, null);
        gameRules.set(GameRules.RESPAWN_RADIUS, 3, null);

        assertEquals("true", GameruleValueResolver.getValue(gameRules, "keepInventory"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, " KEEPINVENTORY "));
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "doDaylightCycle"));
        assertEquals("7", GameruleValueResolver.getValue(gameRules, "randomTickSpeed"));
        assertEquals("3", GameruleValueResolver.getValue(gameRules, "spawnRadius"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "pvp"));
    }

    @Test
    void invertsTheValuesOfLegacyDisableRules() {
        GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);

        assertEquals("false", GameruleValueResolver.getValue(gameRules, "disableRaids"));
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "disableElytraMovementCheck"));
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "disablePlayerMovementCheck"));

        gameRules.set(GameRules.RAIDS, false, null);
        gameRules.set(GameRules.ELYTRA_MOVEMENT_CHECK, false, null);
        gameRules.set(GameRules.PLAYER_MOVEMENT_CHECK, false, null);

        assertEquals("true", GameruleValueResolver.getValue(gameRules, "disableRaids"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "disableElytraMovementCheck"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "disablePlayerMovementCheck"));
    }

    @Test
    void derivesTheLegacyFireRulesFromTheFireSpreadRadius() {
        GameRules gameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);

        gameRules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, null);
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "doFireTick"));
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "allowFireTicksAwayFromPlayer"));

        gameRules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 128, null);
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "doFireTick"));
        assertEquals("false", GameruleValueResolver.getValue(gameRules, "allowFireTicksAwayFromPlayer"));

        gameRules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, null);
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "doFireTick"));
        assertEquals("true", GameruleValueResolver.getValue(gameRules, "allowFireTicksAwayFromPlayer"));
    }

    @Test
    void resolvesEveryLegacyNameWhenAllFeaturesAreEnabled() {
        GameRules gameRules = new GameRules(FeatureFlags.REGISTRY.allFlags());
        for (String legacyName : LEGACY_NAMES) {
            assertNotNull(GameruleValueResolver.getValue(gameRules, legacyName), legacyName);
        }
    }

    @Test
    void returnsNullForGameRulesOfDisabledFeatures() {
        GameRules defaultGameRules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        GameRules allFeaturesGameRules = new GameRules(FeatureFlags.REGISTRY.allFlags());

        assertNull(GameruleValueResolver.getValue(defaultGameRules, "max_minecart_speed"));
        assertNull(GameruleValueResolver.getValue(defaultGameRules, "minecartMaxSpeed"));
        assertEquals("8", GameruleValueResolver.getValue(allFeaturesGameRules, "max_minecart_speed"));
        assertEquals("8", GameruleValueResolver.getValue(allFeaturesGameRules, "minecartMaxSpeed"));
    }

    @Test
    void returnsNullForUnknownRemovedAndBlankNames() {
        GameRules gameRules = new GameRules(FeatureFlags.REGISTRY.allFlags());

        assertNull(GameruleValueResolver.getValue(gameRules, null));
        assertNull(GameruleValueResolver.getValue(gameRules, ""));
        assertNull(GameruleValueResolver.getValue(gameRules, "   "));
        assertNull(GameruleValueResolver.getValue(gameRules, "not_a_game_rule"));
        assertNull(GameruleValueResolver.getValue(gameRules, "fancymenu:keep_inventory"));
        assertNull(GameruleValueResolver.getValue(gameRules, "keep inventory!"));
        assertNull(GameruleValueResolver.getValue(gameRules, "minecraft:keep_inventory:extra"));
        assertNull(GameruleValueResolver.getValue(gameRules, "spawnChunkRadius"));
        assertNull(GameruleValueResolver.getValue(gameRules, "entitiesWithPassengersCanUsePortals"));
        assertNull(GameruleValueResolver.getValue(gameRules, "gameLoopFunction"));
    }

    @Test
    void normalizesNamesByTrimmingAndRejectingBlankOnes() {
        assertEquals("keepInventory", GameruleValueResolver.normalizeName("  keepInventory  "));
        assertNull(GameruleValueResolver.normalizeName(" \t "));
        assertNull(GameruleValueResolver.normalizeName(null));
    }

}
