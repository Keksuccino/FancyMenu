package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.scores.Scoreboard;

public class DummyWorldFactory {

	public static Level getDummyWorld() {
		return new DummyWorld();
	}

	public static class DummyWorld extends Level {

		protected DummyWorld() {
			super(null, null, new DummyDimensionType(), null, false, false, 239239L);
		}

		@Override
		public TickList<Block> getBlockTicks() {
			return null;
		}
		@Override
		public TickList<Fluid> getLiquidTicks() {
			return null;
		}
		@Override
		public ChunkSource getChunkSource() {
			return null;
		}
		@Override
		public void levelEvent(Player player, int eventId, BlockPos pos, int data) {
		}
		@Override
		public RegistryAccess registryAccess() {
			return null;
		}
		@Override
		public List<? extends Player> players() {
			return null;
		}
		@Override
		public Biome getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
			return null;
		}
		@Override
		public float getShade(Direction direction, boolean shaded) {
			return 0;
		}
		@Override
		public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		}
		@Override
		public void playSound(Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
		}
		@Override
		public void playSound(Player player, Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch) {
		}
		@Override
		public Entity getEntity(int id) {
			return null;
		}
		@Override
		public MapItemSavedData getMapData(String id) {
			return null;
		}
		@Override
		public void setMapData(MapItemSavedData mapState) {
		}
		@Override
		public int getFreeMapId() {
			return 0;
		}
		@Override
		public void destroyBlockProgress(int entityId, BlockPos pos, int progress) {
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
		public TagContainer getTagManager() {
			return null;
		}

	}

	public static class DummyDimensionType extends DimensionType {

		protected DummyDimensionType() {
			super(OptionalLong.of(1), false, false, false, false, 1.0, false, false, false, false, 0, new ResourceLocation(""), new ResourceLocation(""), 1.0F);
		}

	}

}
