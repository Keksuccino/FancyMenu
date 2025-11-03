package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class OnMouseButtonClickedListener extends AbstractListener {

    @Nullable
    private Integer lastButton;
    @Nullable
    private Double lastMouseX;
    @Nullable
    private Double lastMouseY;

    public OnMouseButtonClickedListener() {
        super("mouse_button_clicked");
    }

    public void onMouseButtonClicked(int button, double mouseX, double mouseY) {
        this.lastButton = button;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("button", () -> this.formatButton(this.lastButton)));
        list.add(new CustomVariable("mouse_pos_x", () -> this.formatCoordinate(this.lastMouseX)));
        list.add(new CustomVariable("mouse_pos_y", () -> this.formatCoordinate(this.lastMouseY)));
    }

    private String formatButton(@Nullable Integer button) {
        if (button == null) {
            return "ERROR";
        }
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "left";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "right";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "middle";
            default -> Integer.toString(button);
        };
    }

    private String formatCoordinate(@Nullable Double value) {
        if (value == null) {
            return "ERROR";
        }
        return Double.toString(value);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_mouse_button_clicked");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_mouse_button_clicked.desc"));
    }
}