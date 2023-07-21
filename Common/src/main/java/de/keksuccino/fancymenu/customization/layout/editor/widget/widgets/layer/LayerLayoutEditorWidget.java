package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    protected ScrollArea scrollArea = new ScrollArea(0, 0, 0, 0);

    public LayerLayoutEditorWidget(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {

        super(editor, builder);

        //TODO FIXEN: area rendert keine entries??
        for (int i = 0; i < 100; i++) {
            this.scrollArea.addEntry(new TextListScrollAreaEntry(this.scrollArea, Component.literal("this is a test texttttttttttttttttttttttttttttttttttttttttttttttt"), getUIColorScheme().listing_dot_color_1, textListScrollAreaEntry -> {
                LogManager.getLogger().info("click");
            }));
        }

    }

    @Override
    protected void renderBody(@NotNull PoseStack pose, double mouseX, double mouseY, float partial, float x, float y, float width, float height) {

        this.scrollArea.setX(x);
        this.scrollArea.setY(y);
        this.scrollArea.setWidth(width);
        this.scrollArea.setHeight(height);
        this.scrollArea.renderScale = this.getComponentScale();
        this.scrollArea.render(pose, (int) mouseX, (int) mouseY, partial);

//        RenderingUtils.resetShaderColor();
//        fillF(pose, x, y, x + width, y + height, !this.isHovered() ? UIBase.getUIColorScheme().area_background_color.getColorInt() : UIBase.getUIColorScheme().element_background_color_hover.getColorInt());
//        RenderingUtils.resetShaderColor();

    }

    @Override
    protected boolean mouseClickedComponent(double mouseX, double mouseY, int button) {
        if (super.mouseClickedComponent(mouseX, mouseY, button)) return true;
        return this.scrollArea.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseReleasedComponent(double mouseX, double mouseY, int button) {
        if (super.mouseReleasedComponent(mouseX, mouseY, button)) return true;
        return this.scrollArea.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedComponent(double mouseX, double mouseY, int button, double d1, double d2) {
        if (super.mouseDraggedComponent(mouseX, mouseY, button, d1, d2)) return true;
        return this.scrollArea.mouseDragged(mouseX, mouseY, button, d1, d2);
    }

    @Override
    protected boolean mouseScrolledComponent(double mouseX, double mouseY, double scrollDelta) {
        if (super.mouseScrolledComponent(mouseX, mouseY, scrollDelta)) return true;
        return this.scrollArea.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

}
