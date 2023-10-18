package de.keksuccino.drippyloadingscreen.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class DrippyMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    public DrippyMixinPlugin() {
    }

    public void onLoad(String mixinPackage) {
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return isKonkreteLoaded() && isFancyMenuLoaded();
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isKonkreteLoaded() {
        try {
            Class.forName("de.keksuccino.konkrete.Konkrete", false, DrippyMixinPlugin.class.getClassLoader());
            return true;
        } catch (Exception e) {}
        return false;
    }

    private static boolean isFancyMenuLoaded() {
        try {
            Class.forName("de.keksuccino.fancymenu.FancyMenu", false, DrippyMixinPlugin.class.getClassLoader());
            return true;
        } catch (Exception e) {}
        return false;
    }

}
