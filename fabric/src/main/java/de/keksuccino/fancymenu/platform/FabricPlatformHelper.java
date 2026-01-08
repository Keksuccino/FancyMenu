package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinCompositePackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinFilePackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinFilePackResourcesSharedZipFileAccess;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinPathPackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinVanillaPackResources;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Fabric";
    }

    @Override
    public String getLoaderVersion() {
        return this.getModVersion("fabric");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        try {
            Optional<ModContainer> o = FabricLoader.getInstance().getModContainer(modId);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> l = new ArrayList<>();
        for (ModContainer info : FabricLoader.getInstance().getAllMods()) {
            l.add(info.getMetadata().getId());
        }
        return l;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isOnClient() {
        return (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return KeyBindingHelper.getBoundKeyOf(keyMapping);
    }

    @Override
    public @Nullable ResourceLocation getItemKey(@NotNull Item item) {
        try {
            return BuiltInRegistries.ITEM.getKey(item);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEffectKey(@NotNull MobEffect effect) {
        try {
            return BuiltInRegistries.MOB_EFFECT.getKey(effect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEntityKey(@NotNull EntityType<?> type) {
        try {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @NotNull List<UniversalModContainer> getLoadedMods() {
        List<UniversalModContainer> mods = new ArrayList<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            ModMetadata m = mod.getMetadata();
            List<String> authors = new ArrayList<>();
            m.getAuthors().forEach(person -> authors.add(person.getName()));
            mods.add(new UniversalModContainer(m.getId(), m.getName(), m.getDescription(), String.join("\n", m.getLicense()), authors));
        });
        return mods;
    }

    @Override
    public @NotNull Set<ResourceLocation> getLoadedClientResourceLocations() {
        Set<ResourceLocation> output = new HashSet<>();
        if (!this.isOnClient()) return output;
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            resourceManager.listPacks().forEach(pack -> collectLocationsFromPack(pack, output));
            collectLocationsFromFabricMods(output);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

    private void collectLocationsFromPack(@NotNull PackResources pack, @NotNull Set<ResourceLocation> output) {
        if (pack instanceof CompositePackResources composite) {
            List<PackResources> stack = ((IMixinCompositePackResources) composite).getPackResourcesStack_FancyMenu();
            if (stack != null) {
                for (PackResources nested : stack) {
                    collectLocationsFromPack(nested, output);
                }
            } else {
                PackResources primary = ((IMixinCompositePackResources) composite).getPrimaryPackResources_FancyMenu();
                if (primary != null) {
                    collectLocationsFromPack(primary, output);
                }
            }
            return;
        }
        if (pack instanceof FilePackResources filePack) {
            collectLocationsFromFilePack(filePack, output);
            return;
        }
        if (pack instanceof PathPackResources pathPack) {
            collectLocationsFromPathPack(pathPack, output);
            return;
        }
        if (pack instanceof VanillaPackResources vanillaPack) {
            collectLocationsFromVanillaPack(vanillaPack, output);
        }
    }

    private void collectLocationsFromPathPack(@NotNull PathPackResources pack, @NotNull Set<ResourceLocation> output) {
        Path root = ((IMixinPathPackResources) pack).getRoot_FancyMenu();
        if (root == null) return;
        Path assetsRoot = root.resolve(PackType.CLIENT_RESOURCES.getDirectory());
        collectLocationsFromRoot(assetsRoot, output);
    }

    private void collectLocationsFromVanillaPack(@NotNull VanillaPackResources pack, @NotNull Set<ResourceLocation> output) {
        IMixinVanillaPackResources accessor = (IMixinVanillaPackResources) pack;
        boolean collected = false;
        List<Path> roots = accessor.getPathsForType_FancyMenu().get(PackType.CLIENT_RESOURCES);
        if (roots != null) {
            for (Path root : roots) {
                if (Files.isDirectory(root)) {
                    collectLocationsFromRoot(root, output);
                    collected = true;
                } else {
                    Path parent = root.getParent();
                    if ((parent != null) && Files.isDirectory(parent)) {
                        collectLocationsFromRoot(parent, output);
                        collected = true;
                    }
                }
            }
        }
        if (!collected) {
            List<Path> rootPaths = accessor.getRootPaths_FancyMenu();
            if (rootPaths == null) return;
            for (Path root : rootPaths) {
                Path assetsRoot = root.resolve(PackType.CLIENT_RESOURCES.getDirectory());
                collectLocationsFromRoot(assetsRoot, output);
            }
        }
    }

    private void collectLocationsFromRoot(@NotNull Path assetsRoot, @NotNull Set<ResourceLocation> output) {
        if (!Files.exists(assetsRoot) || !Files.isDirectory(assetsRoot)) return;
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assetsRoot)) {
            for (Path namespaceDir : namespaces) {
                if (!Files.isDirectory(namespaceDir)) continue;
                String namespace = namespaceDir.getFileName().toString();
                if (!ResourceLocation.isValidNamespace(namespace)) continue;
                try (Stream<Path> files = Files.walk(namespaceDir)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        String path = namespaceDir.relativize(file).toString().replace(File.separatorChar, '/');
                        if (path.isEmpty() || path.endsWith(".mcmeta")) return;
                        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
                        if (location != null) output.add(location);
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void collectLocationsFromFilePack(@NotNull FilePackResources pack, @NotNull Set<ResourceLocation> output) {
        IMixinFilePackResources accessor = (IMixinFilePackResources) pack;
        IMixinFilePackResourcesSharedZipFileAccess zipAccessor = (IMixinFilePackResourcesSharedZipFileAccess) accessor.getZipFileAccess_FancyMenu();
        if (zipAccessor == null) return;
        ZipFile zipFile = zipAccessor.getOrCreateZipFile_FancyMenu();
        if (zipFile == null) return;
        String prefix = accessor.getPrefix_FancyMenu();
        if (prefix == null) prefix = "";
        String basePrefix = prefix.isEmpty() ? "" : prefix + "/";
        String assetsPrefix = basePrefix + PackType.CLIENT_RESOURCES.getDirectory() + "/";
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (!name.startsWith(assetsPrefix)) continue;
            String remainder = name.substring(assetsPrefix.length());
            if (remainder.isEmpty()) continue;
            int slashIndex = remainder.indexOf('/');
            if (slashIndex <= 0) continue;
            String namespace = remainder.substring(0, slashIndex);
            if (!ResourceLocation.isValidNamespace(namespace)) continue;
            String path = remainder.substring(slashIndex + 1);
            if (path.isEmpty() || path.endsWith(".mcmeta")) continue;
            ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
            if (location != null) output.add(location);
        }
    }

    private void collectLocationsFromFabricMods(@NotNull Set<ResourceLocation> output) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            mod.findPath("assets").ifPresent(path -> collectLocationsFromRoot(path, output));
        });
    }

}
