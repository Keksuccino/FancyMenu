package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.ClientTelemetryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nullable;

public class DummyWorldFactory {

	//TODO übernehmen 2.7.3
	private static ClientTelemetryManager telemetryManager = null;

	public static Level getDummyWorld() {
		return new DummyWorld();
	}

	//TODO übernehmen 2.7.3
	public static ClientLevel getDummyClientWorld() {
		return new DummyClientWorld();
	}

	//TODO übernehmen 2.7.3
	public static class DummyClientWorld extends ClientLevel {

		public DummyClientWorld() {
//			super(new ClientPacketListener(Minecraft.getInstance(), new TitleScreen(), new Connection(PacketFlow.CLIENTBOUND), new GameProfile(UUID.randomUUID(), "steve"), getTelemetryManager()), new ClientLevelData(Difficulty.EASY, false, false), p_205507_, p_205508_, p_205509_, p_205510_, p_205511_, p_205512_, p_205513_, p_205514_);
			super(new ClientPacketListener(Minecraft.getInstance(), new TitleScreen(), new Connection(PacketFlow.CLIENTBOUND), new GameProfile(UUID.randomUUID(), "steve"), getTelemetryManager()), new ClientLevelData(Difficulty.EASY, false, false), null, getDummyDimensionTypeHolder(), 0, 0, null, null, false, 239239L);
		}

	}

	//TODO übernehmen 2.7.3
	private static ClientTelemetryManager getTelemetryManager() {
		if (telemetryManager == null) {
			telemetryManager = Minecraft.getInstance().createTelemetryManager();
		}
		return telemetryManager;
	}

	public static class DummyWorld extends Level {

		protected DummyWorld() {
			super(null, null, getDummyDimensionTypeHolder(), null, false, false, 239239L);
		}

		@Override
		public void sendBlockUpdated(BlockPos p_46612_, BlockState p_46613_, BlockState p_46614_, int p_46615_) {
		}
		@Override
		public void playSound(@Nullable Player p_46543_, double p_46544_, double p_46545_, double p_46546_, SoundEvent p_46547_, SoundSource p_46548_, float p_46549_, float p_46550_) {
		}
		@Override
		public void playSound(@Nullable Player p_46551_, Entity p_46552_, SoundEvent p_46553_, SoundSource p_46554_, float p_46555_, float p_46556_) {
		}
		@Override
		public String gatherChunkSourceStats() {
			return "";
		}
		@Nullable
		@Override
		public Entity getEntity(int p_46492_) {
			return null;
		}
		@Nullable
		@Override
		public MapItemSavedData getMapData(String p_46650_) {
			return null;
		}
		@Override
		public void setMapData(String p_151533_, MapItemSavedData p_151534_) {
		}
		@Override
		public int getFreeMapId() {
			return 0;
		}
		@Override
		public void destroyBlockProgress(int p_46506_, BlockPos p_46507_, int p_46508_) {
		}
		@Override
		public Scoreboard getScoreboard() {
			return null;
		}
		@Override
		public RecipeManager getRecipeManager() {
			return null;
		}
		@Override
		protected LevelEntityGetter<Entity> getEntities() {
			return null;
		}
		@Override
		public LevelTickAccess<Block> getBlockTicks() {
			return null;
		}
		@Override
		public LevelTickAccess<Fluid> getFluidTicks() {
			return null;
		}
		@Override
		public ChunkSource getChunkSource() {
			return null;
		}
		@Override
		public void levelEvent(@Nullable Player p_46771_, int p_46772_, BlockPos p_46773_, int p_46774_) {
		}
		@Override
		public void gameEvent(@Nullable Entity p_151549_, GameEvent p_151550_, BlockPos p_151551_) {
		}
		@Override
		public RegistryAccess registryAccess() {
			return null;
		}
		@Override
		public float getShade(Direction p_45522_, boolean p_45523_) {
			return 0;
		}
		@Override
		public List<? extends Player> players() {
			return null;
		}
		@Override
		public Holder<Biome> getUncachedNoiseBiome(int p_46809_, int p_46810_, int p_46811_) {
			return null;
		}

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
			public boolean is(ResourceLocation p_205713_) {
				return false;
			}
			@Override
			public boolean is(ResourceKey<DimensionType> p_205712_) {
				return false;
			}
			@Override
			public boolean is(Predicate<ResourceKey<DimensionType>> p_205711_) {
				return false;
			}
			@Override
			public boolean is(TagKey<DimensionType> p_205705_) {
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
				return Optional.empty();
			}
			@Override
			public Kind kind() {
				return Kind.REFERENCE;
			}
			@Override
			public boolean isValidInRegistry(Registry<DimensionType> p_205708_) {
				return false;
			}
		};
	}

	public static DimensionType getDummyDimensionType() {
		ResourceKey<Registry<Block>> rk = ResourceKey.createRegistryKey(new ResourceLocation(""));
		TagKey<Block> tk = TagKey.create(rk, new ResourceLocation(""));
		return DimensionType.create(
				OptionalLong.of(1),
				false,
				false,
				false,
				false,
				1.0,
				false,
				false,
				false,
				false,
				false,
				16,
				16,
				16,
				tk,
				new ResourceLocation(""),
				1.0F);
	}

}
