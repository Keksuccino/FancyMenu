package de.keksuccino.fancymenu.menu.animation.v2.resource.packresources;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.ResourcePackFileNotFoundException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class AnimationZipPackResources extends AnimationPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Splitter SPLITTER = Splitter.on('/').omitEmptyStrings().limit(3);
    @Nullable
    private ZipFile zipFile;

    public AnimationZipPackResources(File animationZip) {
        super(animationZip);
    }

    private ZipFile getOrCreateZipFile() throws IOException {
        if (this.zipFile == null) {
            this.zipFile = new ZipFile(this.file);
        }

        return this.zipFile;
    }

    protected InputStream getResource(String path) throws IOException {
        ZipFile zipfile = this.getOrCreateZipFile();
        ZipEntry zipentry = zipfile.getEntry(path);
        if (zipentry == null) {
            throw new ResourcePackFileNotFoundException(this.file, path);
        } else {
            return zipfile.getInputStream(zipentry);
        }
    }

    public boolean hasResource(String path) {
        try {
            return this.getOrCreateZipFile().getEntry(path) != null;
        } catch (IOException ioexception) {
            return false;
        }
    }

    public Set<String> getNamespaces(PackType packType) {
        ZipFile zipfile;
        try {
            zipfile = this.getOrCreateZipFile();
        } catch (IOException ioexception) {
            return Collections.emptySet();
        }

        Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
        Set<String> set = Sets.newHashSet();

        while(enumeration.hasMoreElements()) {
            ZipEntry zipentry = enumeration.nextElement();
            String s = zipentry.getName();
            if (s.startsWith(packType.getDirectory() + "/")) {
                List<String> list = Lists.newArrayList(SPLITTER.split(s));
                if (list.size() > 1) {
                    String s1 = list.get(1);
                    if (s1.equals(s1.toLowerCase(Locale.ROOT))) {
                        set.add(s1);
                    } else {
                        this.logWarning(s1);
                    }
                }
            }
        }

        return set;
    }

    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public void close() {
        if (this.zipFile != null) {
            IOUtils.closeQuietly((Closeable)this.zipFile);
            this.zipFile = null;
        }

    }

    public Collection<ResourceLocation> getResources(PackType packType, String string, String string2, Predicate<ResourceLocation> predicate) {
        ZipFile zipfile;
        try {
            zipfile = this.getOrCreateZipFile();
        } catch (IOException ioexception) {
            return Collections.emptySet();
        }
        Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
        List<ResourceLocation> list = Lists.newArrayList();
        String s = packType.getDirectory() + "/" + string + "/";
        String s1 = s + string2 + "/";
        while(enumeration.hasMoreElements()) {
            ZipEntry zipentry = enumeration.nextElement();
            if (!zipentry.isDirectory()) {
                String s2 = zipentry.getName();
                if (!s2.endsWith(".mcmeta") && s2.startsWith(s1)) {
                    String s3 = s2.substring(s.length());
                    ResourceLocation resourcelocation = ResourceLocation.tryBuild(string, s3);
                    if (resourcelocation == null) {
                        LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", string, s3);
                    } else if (predicate.test(resourcelocation)) {
                        list.add(resourcelocation);
                    }
                }
            }
        }
        return list;
    }
}