package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextElementBuilder extends ElementBuilder<TextElement, TextEditorElement> {

    public TextElementBuilder() {
        super("text_v2");
    }

    @Override
    public @NotNull TextElement buildDefaultInstance() {
        TextElement i = new TextElement(this);
        i.baseWidth = 200;
        i.baseHeight = 40;
        i.setSource(TextElement.SourceMode.DIRECT, I18n.get("fancymenu.customization.items.text.placeholder"));
        return i;
    }

    @Override
    public TextElement deserializeElement(@NotNull SerializedElement serialized) {

        //Don't use buildDefaultInstance() here, because updateContent() runs asynchronously and could override the deserialized content with the default one
        TextElement element = new TextElement(this);
        element.baseWidth = 200;
        element.baseHeight = 40;

        element.source = serialized.getValue("source");
        if (element.source == null) {
            element.source = "";
        }
        element.source = element.source.replace("%n%", "\n");

        String sourceModeString = serialized.getValue("source_mode");
        if (sourceModeString != null) {
            TextElement.SourceMode s = TextElement.SourceMode.getByName(sourceModeString);
            if (s != null) {
                element.sourceMode = s;
            }
        }

        String shadowString = serialized.getValue("shadow");
        if ((shadowString != null) && shadowString.equals("false")) {
            element.markdownRenderer.setTextShadow(false);
        }

//        String caseModeString = serialized.getValue("case_mode");
//        if (caseModeString != null) {
//            TextElement.CaseMode c = TextElement.CaseMode.getByName(caseModeString);
//            if (c != null) {
//                element.caseMode = c;
//            }
//        }

//        String scaleString = serialized.getValue("scale");
//        if (scaleString != null) {
//            if (MathUtils.isFloat(scaleString)) {
//                element.scale = Float.parseFloat(scaleString);
//            }
//        }

        //TODO legacy support für alignment adden
//        String alignmentString = serialized.getValue("alignment");
//        if (alignmentString != null) {
//            AbstractElement.Alignment a = AbstractElement.Alignment.getByName(alignmentString);
//            if (a != null) {
//                element.alignment = a;
//            }
//        }

        String baseColorString = serialized.getValue("base_color");
        if (baseColorString != null) {
            DrawableColor c = DrawableColor.of(baseColorString);
            element.markdownRenderer.setTextBaseColor(c);
        }

        String textBorderString = serialized.getValue("text_border");
        if ((textBorderString != null) && MathUtils.isInteger(textBorderString)) {
            element.markdownRenderer.setBorder(Integer.parseInt(textBorderString));
        }

        String lineSpacingString = serialized.getValue("line_spacing");
        if ((lineSpacingString != null) && MathUtils.isInteger(lineSpacingString)) {
            element.markdownRenderer.setLineSpacing(Integer.parseInt(lineSpacingString));
        }

        element.scrollGrabberColorHexNormal = serialized.getValue("grabber_color_normal");
        element.scrollGrabberColorHexHover = serialized.getValue("grabber_color_hover");

        element.verticalScrollGrabberTextureNormal = serialized.getValue("grabber_texture_normal");
        element.verticalScrollGrabberTextureHover = serialized.getValue("grabber_texture_hover");
        element.horizontalScrollGrabberTextureNormal = serialized.getValue("horizontal_grabber_texture_normal");
        element.horizontalScrollGrabberTextureHover = serialized.getValue("horizontal_grabber_texture_hover");

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
            serializeTo.putProperty("source", element.source.replace("\n", "%n%"));
        }
        serializeTo.putProperty("source_mode", element.sourceMode.name);
        serializeTo.putProperty("shadow", "" + element.markdownRenderer.isTextShadow());
//        if (element.caseMode != null) {
//            serializeTo.putProperty("case_mode", element.caseMode.name);
//        }
//        serializeTo.putProperty("scale", "" + element.scale);
//        if (element.alignment != null) {
//            serializeTo.putProperty("alignment", element.alignment.key);
//        }
        serializeTo.putProperty("base_color", element.markdownRenderer.getTextBaseColor().getHex());
        serializeTo.putProperty("text_border", "" + (int)element.markdownRenderer.getBorder());
        serializeTo.putProperty("line_spacing", "" + (int)element.markdownRenderer.getLineSpacing());
        if (element.scrollGrabberColorHexNormal != null) {
            serializeTo.putProperty("grabber_color_normal", element.scrollGrabberColorHexNormal);
        }
        if (element.scrollGrabberColorHexHover != null) {
            serializeTo.putProperty("grabber_color_hover", element.scrollGrabberColorHexHover);
        }
        if (element.verticalScrollGrabberTextureNormal != null) {
            serializeTo.putProperty("grabber_texture_normal", element.verticalScrollGrabberTextureNormal);
        }
        if (element.verticalScrollGrabberTextureHover != null) {
            serializeTo.putProperty("grabber_texture_hover", element.verticalScrollGrabberTextureHover);
        }
        if (element.horizontalScrollGrabberTextureNormal != null) {
            serializeTo.putProperty("horizontal_grabber_texture_normal", element.horizontalScrollGrabberTextureNormal);
        }
        if (element.horizontalScrollGrabberTextureHover != null) {
            serializeTo.putProperty("horizontal_grabber_texture_hover", element.horizontalScrollGrabberTextureHover);
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
        //TODO remove debug component at end of name
        return Component.translatable("fancymenu.customization.items.text").append(Component.literal(" V2 DEBUG"));
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.customization.items.text.desc");
    }

}
