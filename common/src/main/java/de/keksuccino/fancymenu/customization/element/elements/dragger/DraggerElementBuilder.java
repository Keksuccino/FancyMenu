package de.keksuccino.fancymenu.customization.element.elements.dragger;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

public class DraggerElementBuilder extends ElementBuilder<DraggerElement, DraggerEditorElement> {

    private static final Logger LOGGER = LogManager.getLogger();

    public DraggerElementBuilder() {
        super("dragger");
    }

    @Override
    public @NotNull DraggerElement buildDefaultInstance() {
        DraggerElement i = new DraggerElement(this);
        i.baseWidth = 100;
        i.baseHeight = 100;
        i.inEditorColor.setDefault(DrawableColor.of(new Color(227, 14, 35)).getHex()).set(DrawableColor.of(new Color(227, 14, 35)).getHex());
        return i;
    }

    @Override
    public DraggerElement deserializeElement(@NotNull SerializedElement serialized) {

        DraggerElement element = this.buildDefaultInstance();

        element.saveDragOffset = deserializeBoolean(element.saveDragOffset, serialized.getValue("save_drag_offset"));

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull DraggerElement element, @NotNull SerializedElement serializeTo) {

        serializeTo.putProperty("save_drag_offset", "" + element.saveDragOffset);

        return serializeTo;

    }

    @Override
    public @NotNull DraggerEditorElement wrapIntoEditorElement(@NotNull DraggerElement element, @NotNull LayoutEditorScreen editor) {
        return new DraggerEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.dragger");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.dragger.desc");
    }

}
