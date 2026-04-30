package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractPackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinFilePackResources;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinVanillaPackResources;
import de.keksuccino.fancymenu.mixin.mixins.forge.client.IMixinForgeDelegatingPackResources;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;
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

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "forge";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Forge";
    }

    @Override
    public String getLoaderVersion() {
        return this.getModVersion("forge");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        try {
            Optional<? extends ModContainer> o = ModList.get().getModContainerById(modId);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> l = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            l.add(info.getModId());
        }
        return l;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isOnClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    @Override
    public @Nullable ResourceLocation getItemKey(@NotNull Item item) {
        try {
            return ForgeRegistries.ITEMS.getKey(item);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEffectKey(@NotNull MobEffect effect) {
        try {
            return ForgeRegistries.MOB_EFFECTS.getKey(effect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEntityKey(@NotNull EntityType<?> type) {
        try {
            return ForgeRegistries.ENTITY_TYPES.getKey(type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @NotNull List<UniversalModContainer> getLoadedMods() {
        List<UniversalModContainer> mods = new ArrayList<>();
        ModList.get().getMods().forEach(mod -> {
            mods.add(new UniversalModContainer(mod.getModId(), mod.getDisplayName(), mod.getDescription(), mod.getOwningFile().getLicense(), null));
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

    private void collectLocationsFromPack(@NotNull PackResources pack, @NotNull Set<ResourceLocation> output) {
        if (pack instanceof DelegatingPackResources delegatingPack) {
            collectLocationsFromDelegatingPack(delegatingPack, output);
            return;
        }
        if (pack instanceof PathPackResources pathPack) {
            collectLocationsFromPathPack(pathPack, output);
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

    private void collectLocationsFromDelegatingPack(@NotNull DelegatingPackResources pack, @NotNull Set<ResourceLocation> output) {
        List<PackResources> delegates = ((IMixinForgeDelegatingPackResources) pack).getDelegates_FancyMenu();
        if ((delegates != null) && !delegates.isEmpty()) {
            for (PackResources delegate : delegates) {
                collectLocationsFromPack(delegate, output);
            }
            return;
        }
        collectLocationsFromPackApi(pack, output);
    }

    private void collectLocationsFromPathPack(@NotNull PathPackResources pack, @NotNull Set<ResourceLocation> output) {
        collectLocationsFromRoot(pack.getSource().resolve(PackType.CLIENT_RESOURCES.getDirectory()), output);
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
