package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.OptionalLong;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.util.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import net.minecraft.client.world.ClientWorld.ClientWorldInfo;

public class DummyWorldFactory {

	public static ClientWorld getDummyClientWorld() {
		return new DummyClientWorld();
	}

	public static class DummyClientWorld extends ClientWorld {

		public DummyClientWorld() {
			super(new ClientPlayNetHandler(Minecraft.getInstance(), new MainMenuScreen(), new NetworkManager(PacketDirection.CLIENTBOUND), new GameProfile(UUID.randomUUID(), "steve")), new ClientWorldInfo(Difficulty.EASY, false, false), null, new DummyDimensionType(), 0, null, null, false, 239239L);
		}
		@Override
		public RegistryKey<World> dimension() {
			return RegistryKey.create(RegistryKey.createRegistryKey(new ResourceLocation("")), new ResourceLocation(""));
		}

	}
	
	public static class DummyDimensionType extends DimensionType {
		
		protected DummyDimensionType() {
			super(OptionalLong.of(1), false, false, false, false, 1.0, false, false, false, false, 0, new ResourceLocation(""), new ResourceLocation(""), 1.0F);
		}
		
	}
	
}
