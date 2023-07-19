package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import org.jetbrains.annotations.NotNull;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    public LayerLayoutEditorWidget(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {
        super(editor, builder);
    }

    @Override
    protected void renderBody(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        RenderingUtils.resetShaderColor();
        fillF(pose, x, y, x + width, y + height, !this.isHovered() ? UIBase.getUIColorScheme().area_background_color.getColorInt() : UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
        RenderingUtils.resetShaderColor();

    }

}
