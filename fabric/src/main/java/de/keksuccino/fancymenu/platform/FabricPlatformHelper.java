package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractPackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinFilePackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinVanillaPackResources;
import de.keksuccino.fancymenu.mixin.mixins.fabric.client.IMixinFabricModNioPackResources;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
            return Registry.ITEM.getKey(item);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEffectKey(@NotNull MobEffect effect) {
        try {
            return Registry.MOB_EFFECT.getKey(effect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEntityKey(@NotNull EntityType<?> type) {
        try {
            return Registry.ENTITY_TYPE.getKey(type);
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
        if (pack instanceof ModResourcePack && collectLocationsFromFabricModPack(pack, output)) {
            return;
        }
        if (pack instanceof FilePackResources filePack) {
            collectLocationsFromFilePack(filePack, output);
            return;
        }
        if (pack instanceof FolderPackResources folderPack) {
            collectLocationsFromFolderPack(folderPack, output);
            return;
        }
        if (pack instanceof VanillaPackResources vanillaPack) {
            collectLocationsFromVanillaPack(vanillaPack, output);
            return;
        }
        collectLocationsFromPackApi(pack, output);
    }

    private boolean collectLocationsFromFabricModPack(@NotNull PackResources pack, @NotNull Set<ResourceLocation> output) {
        if (pack instanceof IMixinFabricModNioPackResources nioPack) {
            List<Path> basePaths = nioPack.getBasePaths_FancyMenu();
            if (basePaths != null) {
                for (Path basePath : basePaths) {
                    collectLocationsFromRoot(basePath.resolve(PackType.CLIENT_RESOURCES.getDirectory()), output);
                }
            }
            return true;
        }
        return false;
    }

    private void collectLocationsFromFolderPack(@NotNull FolderPackResources pack, @NotNull Set<ResourceLocation> output) {
        File root = ((IMixinAbstractPackResources) pack).getFile_FancyMenu();
        if (root == null) return;
        collectLocationsFromRoot(root.toPath().resolve(PackType.CLIENT_RESOURCES.getDirectory()), output);
    }

    private void collectLocationsFromVanillaPack(@NotNull VanillaPackResources pack, @NotNull Set<ResourceLocation> output) {
        int before = output.size();
        Path generatedDir = VanillaPackResources.generatedDir;
        if (generatedDir != null) {
            collectLocationsFromRoot(generatedDir.resolve(PackType.CLIENT_RESOURCES.getDirectory()), output);
            collectVanillaClientObjectResources(output);
        }

        Map<PackType, Path> roots = IMixinVanillaPackResources.getRootDirByType_FancyMenu();
        Path assetsRoot = roots.get(PackType.CLIENT_RESOURCES);
        if (assetsRoot != null) {
            collectLocationsFromRoot(assetsRoot, output);
        }

        if (output.size() == before) {
            collectLocationsFromPackApi(pack, output);
        }
    }

    private void collectVanillaClientObjectResources(@NotNull Set<ResourceLocation> output) {
        Class<?> clientObject = VanillaPackResources.clientObject;
        if (clientObject == null) return;
        try {
            Enumeration<URL> resources = clientObject.getClassLoader().getResources(PackType.CLIENT_RESOURCES.getDirectory() + "/");
            while (resources.hasMoreElements()) {
                URI uri = resources.nextElement().toURI();
                if ("file".equals(uri.getScheme())) {
                    collectLocationsFromRoot(Paths.get(uri), output);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void collectLocationsFromRoot(@NotNull Path assetsRoot, @NotNull Set<ResourceLocation> output) {
        if (!Files.isDirectory(assetsRoot)) return;
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assetsRoot)) {
            for (Path namespaceDir : namespaces) {
                if (!Files.isDirectory(namespaceDir)) continue;
                String namespace = namespaceDir.getFileName().toString();
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
        try {
            ZipFile zipFile = ((IMixinFilePackResources) pack).getOrCreateZipFile_FancyMenu();
            if (zipFile == null) return;
            String assetsPrefix = PackType.CLIENT_RESOURCES.getDirectory() + "/";
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
                String path = remainder.substring(slashIndex + 1);
                if (path.isEmpty() || path.endsWith(".mcmeta")) continue;
                ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
                if (location != null) output.add(location);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void collectLocationsFromFabricMods(@NotNull Set<ResourceLocation> output) {
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            mod.findPath(PackType.CLIENT_RESOURCES.getDirectory()).ifPresent(path -> collectLocationsFromRoot(path, output));
        });
    }

    private void collectLocationsFromPackApi(@NotNull PackResources pack, @NotNull Set<ResourceLocation> output) {
        try {
            for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                Collection<ResourceLocation> locations = pack.getResources(PackType.CLIENT_RESOURCES, namespace, "", location -> true);
                output.addAll(locations);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
