package de.keksuccino.fancymenu.customization.element.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EditorElementBorderDisplay implements Renderable {

    public final AbstractEditorElement editorElement;
    public final DisplayPosition defaultDisplayPosition;
    public DisplayPosition currentDisplayPosition;
    protected final List<Supplier<Component>> lines = new ArrayList<>();

    public EditorElementBorderDisplay(DisplayPosition defaultDisplayPosition, AbstractEditorElement editorElement) {
        this.defaultDisplayPosition = defaultDisplayPosition;
        this.currentDisplayPosition = defaultDisplayPosition;
        this.editorElement = editorElement;
    }

    public void addLine(Supplier<Component> lineSupplier) {
        this.lines.add(lineSupplier);
    }

    public void clearLines() {
        this.lines.clear();
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

    }

    protected void updateCurrentDisplayPosition() {
        int screenWidth = AbstractElement.getScreenWidth();
        int screenHeight = AbstractElement.getScreenHeight();
        if (this.defaultDisplayPosition == )
    }

    public enum DisplayPosition {
        TOP_LEFT,
        TOP_RIGHT,
        RIGHT_TOP,
        RIGHT_BOTTOM,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT_TOP,
        LEFT_BOTTOM
    }

}
