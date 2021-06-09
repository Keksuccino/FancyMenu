package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.OptionalLong;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tags.ITagCollectionSupplier;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;
import net.minecraft.world.ITickList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.storage.MapData;

public class DummyWorldFactory {
	
	public static World getDummyWorld() {
		return new DummyWorld();
	}
	
	public static class DummyWorld extends World {

		protected DummyWorld() {
			super(null, null, new DummyDimensionType(), null, false, false, 239239L);
		}
		
		@Override
		public float func_230487_a_(Direction p_230487_1_, boolean p_230487_2_) {
			return 0;
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
		public DynamicRegistries func_241828_r() {
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
		public ITagCollectionSupplier getTags() {
			return null;
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
		
	}
	
	public static class DummyDimensionType extends DimensionType {
		
		protected DummyDimensionType() {
			super(OptionalLong.of(1), false, false, false, false, 1.0, false, false, false, false, 0, new ResourceLocation(""), new ResourceLocation(""), 1.0F);
		}
		
	}
	
}
