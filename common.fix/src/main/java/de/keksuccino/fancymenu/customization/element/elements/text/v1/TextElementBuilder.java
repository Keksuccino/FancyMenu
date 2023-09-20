package de.keksuccino.fancymenu.customization.element.elements.text.v1;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

@Deprecated
public class TextElementBuilder extends ElementBuilder<TextElement, TextEditorElement> {

    public TextElementBuilder() {
        super("fancymenu_customization_item_text");
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

    @Override
    public @NotNull TextElement buildDefaultInstance() {
        TextElement i = new TextElement(this);
        i.baseWidth = 200;
        i.baseHeight = 40;
        i.source = "< EMPTY TEXT ELEMENT >";
        i.updateContent();
        return i;
    }

    @Override
    public TextElement deserializeElement(@NotNull SerializedElement serialized) {

        //Don't use buildDefaultInstance() here, because updateContent() runs asynchronously and could override the deserialized content with the default one
        TextElement element = new TextElement(this);
        element.baseWidth = 200;
        element.baseHeight = 40;
        element.source = "< EMPTY TEXT ELEMENT >";

        element.source = serialized.getValue("source");

        String sourceModeString = serialized.getValue("source_mode");
        if (sourceModeString != null) {
            TextElement.SourceMode s = TextElement.SourceMode.getByName(sourceModeString);
            if (s != null) {
                element.sourceMode = s;
            }
        }

        String shadowString = serialized.getValue("shadow");
        if ((shadowString != null) && shadowString.equals("false")) {
            element.shadow = false;
        }

        String caseModeString = serialized.getValue("case_mode");
        if (caseModeString != null) {
            TextElement.CaseMode c = TextElement.CaseMode.getByName(caseModeString);
            if (c != null) {
                element.caseMode = c;
            }
        }

        String scaleString = serialized.getValue("scale");
        if (scaleString != null) {
            if (MathUtils.isFloat(scaleString)) {
                element.scale = Float.parseFloat(scaleString);
            }
        }

        String alignmentString = serialized.getValue("alignment");
        if (alignmentString != null) {
            AbstractElement.Alignment a = AbstractElement.Alignment.getByName(alignmentString);
            if (a != null) {
                element.alignment = a;
            }
        }

        String baseColorString = serialized.getValue("base_color");
        if (baseColorString != null) {
            Color c = de.keksuccino.konkrete.rendering.RenderUtils.getColorFromHexString(baseColorString);
            if (c != null) {
                element.baseColorHex = baseColorString;
            }
        }

        String textBorderString = serialized.getValue("text_border");
        if ((textBorderString != null) && MathUtils.isInteger(textBorderString)) {
            element.textBorder = Integer.parseInt(textBorderString);
        }

        String lineSpacingString = serialized.getValue("line_spacing");
        if ((lineSpacingString != null) && MathUtils.isInteger(lineSpacingString)) {
            element.lineSpacing = Integer.parseInt(lineSpacingString);
        }

        element.scrollGrabberColorHexNormal = serialized.getValue("grabber_color_normal");
        element.scrollGrabberColorHexHover = serialized.getValue("grabber_color_hover");

        element.scrollGrabberTextureNormal = serialized.getValue("grabber_texture_normal");
        element.scrollGrabberTextureHover = serialized.getValue("grabber_texture_hover");

        String enableScrollingString = serialized.getValue("enable_scrolling");
        if ((enableScrollingString != null) && enableScrollingString.equals("false")) {
            element.enableScrolling = false;
        }

        element.updateContent();

        return element;

    }

    @Override
    protected SerializedElement serializeElement(@NotNull TextElement element, @NotNull SerializedElement serializeTo) {

        if (element.source != null) {
            serializeTo.putProperty("source", element.source);
        }
        if (element.sourceMode != null) {
            serializeTo.putProperty("source_mode", element.sourceMode.name);
        }
        serializeTo.putProperty("shadow", "" + element.shadow);
        if (element.caseMode != null) {
            serializeTo.putProperty("case_mode", element.caseMode.name);
        }
        serializeTo.putProperty("scale", "" + element.scale);
        if (element.alignment != null) {
            serializeTo.putProperty("alignment", element.alignment.key);
        }
        if (element.baseColorHex != null) {
            serializeTo.putProperty("base_color", element.baseColorHex);
        }
        serializeTo.putProperty("text_border", "" + element.textBorder);
        serializeTo.putProperty("line_spacing", "" + element.lineSpacing);
        if (element.scrollGrabberColorHexNormal != null) {
            serializeTo.putProperty("grabber_color_normal", element.scrollGrabberColorHexNormal);
        }
        if (element.scrollGrabberColorHexHover != null) {
            serializeTo.putProperty("grabber_color_hover", element.scrollGrabberColorHexHover);
        }
        if (element.scrollGrabberTextureNormal != null) {
            serializeTo.putProperty("grabber_texture_normal", element.scrollGrabberTextureNormal);
        }
        if (element.scrollGrabberTextureHover != null) {
            serializeTo.putProperty("grabber_texture_hover", element.scrollGrabberTextureHover);
        }
        serializeTo.putProperty("enable_scrolling", "" + element.enableScrolling);

        return serializeTo;
        
    }

    @Override
    public @NotNull TextEditorElement wrapIntoEditorElement(@NotNull TextElement element, @NotNull LayoutEditorScreen editor) {
        return new TextEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.customization.items.text");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.desc");
    }

}
