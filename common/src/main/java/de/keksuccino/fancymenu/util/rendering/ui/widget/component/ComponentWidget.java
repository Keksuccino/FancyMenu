package de.keksuccino.fancymenu.util.rendering.ui.widget.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ComponentWidget extends AbstractWidget implements NavigatableWidget {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected ConsumingSupplier<ComponentWidget, MutableComponent> textSupplier;
    protected boolean shadow = true;
    @NotNull
    protected ConsumingSupplier<ComponentWidget, DrawableColor> baseColorSupplier = (var) -> UIBase.getUIColorTheme().generic_text_base_color;
    protected Consumer<ComponentWidget> onHoverOrFocusStart;
    protected Consumer<ComponentWidget> onHoverOrFocusEnd;
    protected Consumer<ComponentWidget> onClick;
    @Nullable
    protected ComponentWidget parent;
    protected List<ComponentWidget> children = new ArrayList<>();
    @NotNull
    protected Font font;
    protected boolean isCurrentlyHoveredOrFocused = false;
    protected int endX;

    public static ComponentWidget of(@NotNull MutableComponent component, int x, int y) {
        return new ComponentWidget(Minecraft.getInstance().font, x, y, component);
    }

    public static ComponentWidget literal(@NotNull String text, int x, int y) {
        ComponentWidget w = new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
        w.setTextSupplier(consumes -> Component.literal(PlaceholderParser.replacePlaceholders(text)));
        return w;
    }

    public static ComponentWidget translatable(@NotNull String key, int x, int y) {
        ComponentWidget w = new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
        w.setTextSupplier(consumes -> Component.translatable(key));
        return w;
    }

    public static ComponentWidget empty(int x, int y) {
        return new ComponentWidget(Minecraft.getInstance().font, x, y, Component.literal(""));
    }

    protected ComponentWidget(@NotNull Font font, int x, int y, @NotNull MutableComponent text) {
        super(x, y, 0, 0, text);
        this.textSupplier = (var) -> text;
        this.font = font;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.width = this.getWidth();
        this.height = this.getHeight();
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    public void renderWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.handleComponentHover();

        RenderSystem.enableBlend();

        this.endX = this.getX();
        if (this.shadow) {
            this.endX = this.font.drawShadow(pose, this.getText(), this.getX(), this.getY(), this.getBaseColor().getColorInt());
        } else {
            this.endX = this.font.draw(pose, this.getText(), this.getX(), this.getY(), this.getBaseColor().getColorInt());
        }

        for (ComponentWidget c : this.children) {
            c.setX(this.endX);
            c.setY(this.getY());
            c.render(pose, mouseX, mouseY, partial);
            this.endX = c.endX;
        }

    }

    public ComponentWidget append(@NotNull ComponentWidget child) {
        child.parent = this;
        this.children.add(child);
        return this;
    }

    public List<ComponentWidget> getChildren() {
        return this.children;
    }

    @Nullable
    public ComponentWidget getParent() {
        return this.parent;
    }

    @NotNull
    public ConsumingSupplier<ComponentWidget, MutableComponent> getTextSupplier() {
        return this.textSupplier;
    }

    public ComponentWidget setTextSupplier(@NotNull ConsumingSupplier<ComponentWidget, MutableComponent> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    @NotNull
    public MutableComponent getText() {
        MutableComponent c = this.textSupplier.get(this);
        if (c == null) c = Component.literal("");
        return c;
    }

    public ComponentWidget setText(@NotNull MutableComponent text) {
        this.textSupplier = (var) -> text;
        return this;
    }

    public boolean hasShadow() {
        return this.shadow;
    }

    public ComponentWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        for (ComponentWidget w : this.children) {
            w.shadow = shadow;
        }
        return this;
    }

    public ComponentWidget setBaseColor(@NotNull DrawableColor baseColor) {
        this.baseColorSupplier = (var) -> baseColor;
        for (ComponentWidget w : this.children) {
            w.baseColorSupplier = this.baseColorSupplier;
        }
        return this;
    }

    @NotNull
    public DrawableColor getBaseColor() {
        DrawableColor c = this.baseColorSupplier.get(this);
        if (c == null) c = DrawableColor.WHITE;
        return c;
    }

    public ComponentWidget setBaseColorSupplier(@NotNull ConsumingSupplier<ComponentWidget, DrawableColor> baseColorSupplier) {
        this.baseColorSupplier = baseColorSupplier;
        for (ComponentWidget w : this.children) {
            w.baseColorSupplier = baseColorSupplier;
        }
        return this;
    }

    @NotNull
    public ConsumingSupplier<ComponentWidget, DrawableColor> getBaseColorSupplier() {
        return this.baseColorSupplier;
    }

    public ComponentWidget setOnHoverOrFocusStart(@Nullable Consumer<ComponentWidget> onHoverOrFocusStart) {
        this.onHoverOrFocusStart = onHoverOrFocusStart;
        return this;
    }

    public ComponentWidget setOnHoverOrFocusEnd(@Nullable Consumer<ComponentWidget> onHoverOrFocusEnd) {
        this.onHoverOrFocusEnd = onHoverOrFocusEnd;
        return this;
    }

    public ComponentWidget setOnClick(@Nullable Consumer<ComponentWidget> onClick) {
        this.onClick = onClick;
        return this;
    }

    @Override
    public int getWidth() {
        int w = this.font.width(this.getText());
        for (ComponentWidget c : this.children) {
            w += c.getWidth();
        }
        return w;
    }

    @Override
    public int getHeight() {
        return this.font.lineHeight;
    }

    protected void handleComponentHover() {
        if (!this.isCurrentlyHoveredOrFocused) {
            if (this.isHoveredOrFocused()) {
                if (this.onHoverOrFocusStart != null) {
                    this.onHoverOrFocusStart.accept(this);
                }
                this.isCurrentlyHoveredOrFocused = true;
            }
        } else if (!this.isHoveredOrFocused()) {
            if (this.onHoverOrFocusEnd != null) {
                this.onHoverOrFocusEnd.accept(this);
            }
            this.isCurrentlyHoveredOrFocused = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isHoveredOrFocused() && (button == 0)) {
            for (ComponentWidget w : this.children) {
                if (w.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (this.onClick != null) {
                this.onClick.accept(this);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
    }

    @Deprecated
    @Override
    public void setMessage(@NotNull Component content) {
        if (content instanceof MutableComponent m) this.textSupplier = (var) -> m;
    }

    @Deprecated
    @Override
    public @NotNull Component getMessage() {
        return this.getText();
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("ComponentWidgets are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("ComponentWidgets are not navigatable!");
    }

}
