//package de.keksuccino.fancymenu.menu.animation.v2.resource;
//
//import de.keksuccino.fancymenu.menu.animation.v2.Animation;
//import de.keksuccino.fancymenu.menu.animation.v2.AnimationRegistry;
//import de.keksuccino.fancymenu.menu.animation.v2.resource.packresources.AnimationZipPackResources;
//import net.minecraft.server.packs.PackResources;
//import net.minecraft.server.packs.repository.Pack;
//import net.minecraft.server.packs.repository.PackSource;
//import net.minecraft.server.packs.repository.RepositorySource;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.File;
//import java.io.FileFilter;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//public class AnimationRepositorySource implements RepositorySource {
//
//    private static final Logger LOGGER = LogManager.getLogger("fancymenu/AnimationRepositorySource");
//
//    private static final FileFilter RESOURCEPACK_FILTER = (pack) -> {
//        boolean flag = pack.isFile() && pack.getName().endsWith(".zip");
//        boolean flag1 = pack.isDirectory() && (new File(pack, "animation.properties")).isFile();
//        return flag || flag1;
//    };
//
//    public File folder;
//
//    public AnimationRepositorySource(File folder) {
//        this.folder = folder;
//    }
//
//    @Override
//    public void loadPacks(Consumer<Pack> packConsumer, Pack.PackConstructor constructor) {
//
//        if (!this.folder.isDirectory()) {
//            this.folder.mkdirs();
//        }
//
//        File[] files = this.folder.listFiles(RESOURCEPACK_FILTER);
//        if (files != null) {
//            for(File file1 : files) {
//                String s = "file/" + file1.getName();
//                Supplier<PackResources> supplier = this.createSupplier(file1);
//                //Second param (boolean) is to force-enable the pack
//                Pack pack = Pack.create(s, true, supplier, constructor, Pack.Position.TOP, PackSource.DEFAULT);
//                if (pack != null) {
//                    packConsumer.accept(pack);
//                }
//            }
//        }
//
//    }
//
//    private Supplier<PackResources> createSupplier(File packZipOrFolder) {
//        if (packZipOrFolder.isDirectory()) {
//            return () -> {
//                return null;
//            };
//        } else {
//            return () -> {
//                AnimationZipPackResources r = new AnimationZipPackResources(packZipOrFolder);
//                String name = r.getAnimationPackMeta().getEntryValue("name");
//                if (!AnimationRegistry.hasAnimation(name)) {
//                    try {
//                        Animation a = new Animation(r, true);
//                        AnimationRegistry.registerAnimation(a);
//                        a.prepare();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Animation a = AnimationRegistry.getAnimation(name);
//                    a.setPackResources(r);
//                    a.prepare();
//                    LOGGER.info("[FancyMenu] Updating resources for animation pack: " + name);
//                }
//                return r;
//            };
//        }
//    }
//
//}
