
package de.keksuccino.fancymenu.customization.element.elements.splash;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SplashTextElementBuilder extends ElementBuilder<SplashTextElement, SplashTextEditorElement> {

    public final Map<String, SplashTextElement> splashCache = new HashMap<>();
    public boolean isNewMenu = true;
    protected Screen lastScreen = null;

    public SplashTextElementBuilder() {
        super("splash_text");
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @EventListener
    public void onInitScreenPre(InitOrResizeScreenStartingEvent e) {
        this.isNewMenu = (lastScreen == null) || (this.lastScreen.getClass() != e.getScreen().getClass());
        this.lastScreen = e.getScreen();
    }

    @EventListener
    public void onModReloaded(ModReloadEvent e) {
        splashCache.clear();
    }

    @Override
    public @NotNull SplashTextElement buildDefaultInstance() {
        SplashTextElement i = new SplashTextElement(this);
        i.width = 100;
        i.height = 20;
        return i;
    }

    @Override
    public SplashTextElement deserializeElement(@NotNull SerializedElement serialized) {

        SplashTextElement element = this.buildDefaultInstance();

        String sourceMode = serialized.getValue("source_mode");
        if (sourceMode != null) {
            element.sourceMode = SplashTextElement.SourceMode.getByName(sourceMode);
            if (element.sourceMode == null) {
                element.sourceMode = SplashTextElement.SourceMode.DIRECT_TEXT;
            }
        }

        element.source = serialized.getValue("source");

        String rotation = serialized.getValue("rotation");
        if ((rotation != null) && MathUtils.isFloat(rotation)) {
            element.rotation = Float.parseFloat(rotation);
        }

        String refresh = serialized.getValue("refresh");
        if ((refresh != null) && refresh.equalsIgnoreCase("true")) {
            element.refreshOnMenuReload = true;
        }

        String baseColor = serialized.getValue("base_color");
        if (baseColor != null) {
            element.baseColor = DrawableColor.of(baseColor);
        }

        String shadow = serialized.getValue("shadow");
        if ((shadow != null)) {
            if (shadow.equalsIgnoreCase("false")) {
                element.shadow = false;
            }
        }

        String scale = serialized.getValue("scale");
        if ((scale != null) && MathUtils.isFloat(scale)) {
            element.scale = Float.parseFloat(scale);
        }

        String bounce = serialized.getValue("bouncing");
        if ((bounce != null) && bounce.equalsIgnoreCase("false")) {
            element.bounce = false;
        }

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull SplashTextElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("source", "" + element.source);
        serializeTo.putProperty("source_mode", element.sourceMode.name);
        serializeTo.putProperty("scale", "" + element.scale);
        serializeTo.putProperty("shadow", "" + element.shadow);
        serializeTo.putProperty("rotation", "" + element.rotation);
        serializeTo.putProperty("base_color", element.baseColor.getHex());
        serializeTo.putProperty("refresh", "" + element.refreshOnMenuReload);
        serializeTo.putProperty("bouncing", "" + element.bounce);

        return serializeTo;
        
    }

    @Override
    public @NotNull SplashTextEditorElement wrapIntoEditorElement(@NotNull SplashTextElement element, @NotNull LayoutEditorScreen editor) {
        return new SplashTextEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        if ((element instanceof SplashTextElement e) && (e.renderText != null)) {
            return Component.literal(e.renderText);
        }
        return Component.translatable("fancymenu.element.elements.splash_text");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.element.elements.splash_text.desc");
    }

}
