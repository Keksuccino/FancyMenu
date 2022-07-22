package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.ClientTelemetryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionDefaults;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import com.mojang.datafixers.util.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class DummyWorldFactory {

	private static final Logger LOGGER = LogManager.getLogger("fancymenu/DummyWorldFactory");

	private static ClientTelemetryManager telemetryManager = null;

	public static ClientLevel getDummyClientWorld() {
		return new DummyClientWorld();
	}

	public static class DummyClientWorld extends ClientLevel {
		public DummyClientWorld() {
			super(new ClientPacketListener(Minecraft.getInstance(), new TitleScreen(), new Connection(PacketFlow.CLIENTBOUND), new GameProfile(UUID.randomUUID(), "steve"), getTelemetryManager()), new ClientLevelData(Difficulty.EASY, false, false), null, getDummyDimensionTypeHolder(), 0, 0, null, null, false, 239239L);
		}
		//TODO übernehmen
		@Override
		public ResourceKey<Level> dimension() {
			return ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("")), new ResourceLocation(""));
		}
		@Override
		public ResourceKey<DimensionType> dimensionTypeId() {
			return ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("")), new ResourceLocation(""));
		}
		//----------------------
	}

	private static ClientTelemetryManager getTelemetryManager() {
		if (telemetryManager == null) {
			telemetryManager = Minecraft.getInstance().createTelemetryManager();
		}
		return telemetryManager;
	}

	public static Holder<DimensionType> getDummyDimensionTypeHolder() {
		return new Holder<DimensionType>() {
			@Override
			public DimensionType value() {
				return getDummyDimensionType();
			}
			@Override
			public boolean isBound() {
				return false;
			}
			@Override
			public boolean is(ResourceLocation id) {
				return false;
			}
			@Override
			public boolean is(ResourceKey<DimensionType> key) {
				return false;
			}
			@Override
			public boolean is(Predicate<ResourceKey<DimensionType>> predicate) {
				return false;
			}
			@Override
			public boolean is(TagKey<DimensionType> tag) {
				return false;
			}
			@Override
			public Stream<TagKey<DimensionType>> tags() {
				return null;
			}
			@Override
			public Either<ResourceKey<DimensionType>, DimensionType> unwrap() {
				return null;
			}
			@Override
			public Optional<ResourceKey<DimensionType>> unwrapKey() {
				return Optional.of(BuiltinDimensionTypes.OVERWORLD);
			}
			@Override
			public Kind kind() {
				return Kind.REFERENCE;
			}
			@Override
			public boolean isValidInRegistry(Registry<DimensionType> registry) {
				return false;
			}
		};
	}

	public static DimensionType getDummyDimensionType() {
		ResourceKey<Registry<Block>> rk = ResourceKey.createRegistryKey(new ResourceLocation(""));
		TagKey<Block> tk = TagKey.create(rk, new ResourceLocation(""));
		return new DimensionType(
				OptionalLong.of(1),
				false,
				false,
				false,
				false,
				1.0D,
				false,
				false,
				16,
				16,
				16,
				tk,
				new ResourceLocation(""),
				1.0F,
				new DimensionType.MonsterSettings(false, false, null, 0));
	}
	
}
