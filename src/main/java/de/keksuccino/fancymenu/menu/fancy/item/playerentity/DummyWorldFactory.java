package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.OptionalLong;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.TagManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.DimensionType;

public class DummyWorldFactory {

	public static World getDummyWorld() {
		return new DummyWorld();
	}

	public static class DummyWorld extends World {

		protected DummyWorld() {
			super(null, null, new DummyDimensionType(), null, false, false, 239239L);
		}

		@Override
		public TickScheduler<Block> getBlockTickScheduler() {
			return null;
		}
		@Override
		public TickScheduler<Fluid> getFluidTickScheduler() {
			return null;
		}
		@Override
		public ChunkManager getChunkManager() {
			return null;
		}
		@Override
		public void syncWorldEvent(PlayerEntity player, int eventId, BlockPos pos, int data) {
		}
		@Override
		public DynamicRegistryManager getRegistryManager() {
			return null;
		}
		@Override
		public List<? extends PlayerEntity> getPlayers() {
			return null;
		}
		@Override
		public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
			return null;
		}
		@Override
		public float getBrightness(Direction direction, boolean shaded) {
			return 0;
		}
		@Override
		public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		}
		@Override
		public void playSound(PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		}
		@Override
		public void playSoundFromEntity(PlayerEntity player, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		}
		@Override
		public Entity getEntityById(int id) {
			return null;
		}
		@Override
		public MapState getMapState(String id) {
			return null;
		}
		@Override
		public void putMapState(MapState mapState) {
		}
		@Override
		public int getNextMapId() {
			return 0;
		}
		@Override
		public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
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
		public TagManager getTagManager() {
			return null;
		}

	}

	public static class DummyDimensionType extends DimensionType {

		protected DummyDimensionType() {
			super(OptionalLong.of(1), false, false, false, false, 1.0, false, false, false, false, 0, new Identifier(""), new Identifier(""), 1.0F);
		}

	}

}
