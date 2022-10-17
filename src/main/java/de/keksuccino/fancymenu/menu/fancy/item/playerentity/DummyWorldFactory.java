package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

public class DummyWorldFactory {
	
	public static World getDummyWorld() {
		if (!FancyMenu.config.getOrDefault("allow_level_registry_interactions", false)) {
			return null;
		}
		return new DummyWorld();
	}
	
	private static WorldProvider getDummyProvider() {
		return new WorldProvider() {
			@Override
			public DimensionType getDimensionType() {
				return DimensionType.OVERWORLD;
			}
		};
	}
	
	public static class DummyWorld extends World {

		protected DummyWorld() {
			super(null, new WorldInfo(new WorldSettings(1, GameType.SURVIVAL, false, false, WorldType.DEFAULT), ""), getDummyProvider(), null, false);
		}
		
		@Override
		protected IChunkProvider createChunkProvider() {
			return null;
		}
		@Override
		protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
			return false;
		}
		@Override
		public BlockPos getSpawnPoint() {
			return new BlockPos(0, 0, 0);
		}
		
	}
	
}
