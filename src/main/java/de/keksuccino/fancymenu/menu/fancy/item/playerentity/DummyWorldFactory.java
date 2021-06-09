package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.WorldInfo;

public class DummyWorldFactory {
	
	public static World getDummyWorld() {
		return new DummyWorld();
	}
	
	public static class DummyWorld extends World {

		protected DummyWorld() {
			super(new WorldInfo(new WorldSettings(1, GameType.SURVIVAL, false, false, WorldType.DEFAULT), ""), DimensionType.OVERWORLD, DummyWorldFactory::getDummyChunkProvider, null, false);
		}
		
		@Override
		public Biome getNoiseBiomeRaw(int x, int y, int z) {
			return null;
		}
		@Override
		public List<? extends PlayerEntity> getPlayers() {
			return null;
		}
		@Override
		public void playEvent(PlayerEntity player, int type, BlockPos pos, int data) {
		}
		@Override
		public ITickList<Fluid> getPendingFluidTicks() {
			return null;
		}
		@Override
		public ITickList<Block> getPendingBlockTicks() {
			return null;
		}
		@Override
		public AbstractChunkProvider getChunkProvider() {
			return null;
		}
		@Override
		public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
		}
		@Override
		public void registerMapData(MapData mapDataIn) {
		}
		@Override
		public void playSound(PlayerEntity player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {
		}
		@Override
		public void playMovingSound(PlayerEntity playerIn, Entity entityIn, SoundEvent eventIn, SoundCategory categoryIn, float volume, float pitch) {
		}
		@Override
		public void notifyBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
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
		public int getNextMapId() {
			return 0;
		}
		@Override
		public MapData getMapData(String mapName) {
			return null;
		}
		@Override
		public Entity getEntityByID(int id) {
			return null;
		}
		@Override
		public NetworkTagManager getTags() {
			return null;
		}
		
	}
	
	private static AbstractChunkProvider getDummyChunkProvider(World w, Dimension d) {
		return new AbstractChunkProvider() {
			@Override
			public IBlockReader getWorld() {
				return w;
			}
			@Override
			public void tick(BooleanSupplier hasTimeLeft) {
			}
			@Override
			public String makeString() {
				return "";
			}
			@Override
			public WorldLightManager getLightManager() {
				return null;
			}
			@Override
			public IChunk getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load) {
				return null;
			}
		};
	}
	
}
