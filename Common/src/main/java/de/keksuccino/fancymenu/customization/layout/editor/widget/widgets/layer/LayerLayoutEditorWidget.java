package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayerLayoutEditorWidget extends AbstractLayoutEditorWidget {

    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)
    //TODO FIXEN: Pos + Size von widgets wird nicht (mehr?) gespeichert (evtl. settings im allgemeinen broken)

    private static final Logger LOGGER = LogManager.getLogger();

    protected ScrollArea scrollArea;

    public LayerLayoutEditorWidget(LayoutEditorScreen editor, AbstractLayoutEditorWidgetBuilder<?> builder) {

        super(editor, builder);

        this.scrollArea = new ScrollArea(0, 0, 0, 0) {
            @Override
            public void updateScrollArea() {
                int grabberOffset = 5;
                this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - grabberOffset;
                this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - this.horizontalScrollBar.grabberHeight - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + grabberOffset;
                this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + grabberOffset;
                this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - this.verticalScrollBar.grabberWidth - grabberOffset - 1;
                this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - grabberOffset;
            }
        };

        this.scrollArea.borderColor = () -> UIBase.getUIColorScheme().area_background_color;

        for (int i = 0; i < 100; i++) {
            TextListScrollAreaEntry entry = new TextListScrollAreaEntry(this.scrollArea, Component.literal("this is a test texttttttttttttttttttttttttttttttttttttttttttttttt"), getUIColorScheme().listing_dot_color_1, textListScrollAreaEntry -> {
                LogManager.getLogger().info("click");
            });
            entry.setHeight(28);
            this.scrollArea.addEntry(entry);
        }

    }

    @Override
    protected void renderBody(@NotNull PoseStack pose, double mouseX, double mouseY, float partial) {

        RenderingUtils.resetShaderColor();
        fillF(pose, this.getRealBodyX(), this.getRealBodyY(), this.getRealBodyX() + this.getBodyWidth(), this.getRealBodyY() + this.getBodyHeight(), UIBase.getUIColorScheme().area_background_color.getColorInt());

        this.scrollArea.setX(this.getRealBodyX());
        this.scrollArea.setY(this.getRealBodyY());
        this.scrollArea.setWidth(this.getBodyWidth());
        this.scrollArea.setHeight(this.getBodyHeight());
        this.scrollArea.setApplyScissor(false);
        this.enableComponentScissor((int) this.getRealBodyX(), (int) this.getRealBodyY(), (int) this.getBodyWidth()+1, (int) this.getBodyHeight()+1, true);
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 400.0F);
        this.scrollArea.render(pose, (int) mouseX, (int) mouseY, partial);
        pose.popPose();
        this.disableComponentScissor();

    }

    @Override
    protected @Nullable ResizingEdge updateHoveredResizingEdge() {
        if (this.scrollArea.isMouseInteractingWithGrabbers()) return null;
        return super.updateHoveredResizingEdge();
    }

    @Override
    protected boolean mouseClickedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {

        if (super.mouseClickedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;

        //Override original mouseClicked of ScrollArea, to use a combination of real and translated mouse coordinates
        if (this.scrollArea.verticalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
        if (this.scrollArea.horizontalScrollBar.mouseClicked(translatedMouseX, translatedMouseY, button)) return true;
        for (ScrollAreaEntry entry : this.scrollArea.getEntries()) {
            if (entry.mouseClicked(realMouseX, realMouseY, button)) return true;
        }

        return this.isVisible() && this.isMouseOver();

    }

    @Override
    protected boolean mouseReleasedComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, int button) {
        if (super.mouseReleasedComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, button)) return true;
        return this.scrollArea.mouseReleased(realMouseX, realMouseY, button);
    }

    @Override
    protected boolean mouseDraggedComponent(double translatedMouseX, double translatedMouseY, int button, double d1, double d2) {
        if (super.mouseDraggedComponent(translatedMouseX, translatedMouseY, button, d1, d2)) return true;
        return this.scrollArea.mouseDragged(translatedMouseX, translatedMouseY, button, d1, d2);
    }

    @Override
    protected boolean mouseScrolledComponent(double realMouseX, double realMouseY, double translatedMouseX, double translatedMouseY, double scrollDelta) {
        if (super.mouseScrolledComponent(realMouseX, realMouseY, translatedMouseX, translatedMouseY, scrollDelta)) return true;
        return this.scrollArea.mouseScrolled(realMouseX, realMouseY, scrollDelta);
    }

}
