package de.keksuccino.fancymenu.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class FMMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!isKonkreteLoaded()) return false;
        if (isFancyEntityRendererMixin(targetClassName, mixinClassName)) return isFancyEntityRendererLoaded();
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private static boolean isKonkreteLoaded() {
        try {
            Class.forName("de.keksuccino.konkrete.Konkrete", false, FMMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isFancyEntityRendererLoaded() {
        return FMMixinPlugin.class.getClassLoader().getResource("it/crystalnest/fancy_entity_renderer/api/entity/player/FancyPlayerWidget.class") != null;
    }

    private static boolean isFancyEntityRendererMixin(String targetClassName, String mixinClassName) {
        if ((targetClassName != null) && targetClassName.startsWith("it.crystalnest.fancy_entity_renderer.")) return true;
        return (mixinClassName != null) && mixinClassName.endsWith(".MixinFancyPlayerWidget");
    }

}
