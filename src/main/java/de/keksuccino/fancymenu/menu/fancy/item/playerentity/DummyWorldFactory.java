package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.ClientTelemetryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import com.mojang.datafixers.util.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DummyWorldFactory {

	private static final Logger LOGGER = LogManager.getLogger("fancymenu/DummyWorldFactory");

	private static ClientTelemetryManager telemetryManager = null;

	public static ClientLevel getDummyClientWorld() {
		if (!FancyMenu.config.getOrDefault("allow_level_registry_interactions", false)) {
			return null;
		}
		return new DummyClientWorld();
	}

	public static class DummyClientWorld extends ClientLevel {
		public DummyClientWorld() {
			super(new ClientPacketListener(Minecraft.getInstance(), new TitleScreen(), new Connection(PacketFlow.CLIENTBOUND), new GameProfile(UUID.randomUUID(), "steve"), getTelemetryManager()), new ClientLevelData(Difficulty.EASY, false, false), null, getDummyDimensionTypeHolder(), 0, 0, null, null, false, 239239L);
		}
		
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
