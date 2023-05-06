package de.keksuccino.fancymenu.customization.element.editor;

import java.awt.Color;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.AdvancedContextMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractEditorElement extends GuiComponent implements Renderable, GuiEventListener {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Color BORDER_COLOR_SELECTED = new Color(3, 219, 252);
	protected static final Color BORDER_COLOR_NORMAL = new Color(3, 148, 252);
	protected static final ConsumingSupplier<AbstractEditorElement, Integer> BORDER_COLOR = (editorElement) -> {
		if (editorElement.isSelected()) {
			return BORDER_COLOR_SELECTED.getRGB();
		}
		return BORDER_COLOR_NORMAL.getRGB();
	};
	protected static final long CURSOR_HORIZONTAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long CURSOR_VERTICAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

	public AbstractElement element;
	public final EditorElementSettings settings;
	public AdvancedContextMenu menu = new AdvancedContextMenu();
	public LayoutEditorScreen editor;
	protected boolean selected = false;
	protected boolean multiSelected = false;
	protected boolean hovered = false;
	protected boolean leftMouseDown = false;
	protected double leftMouseDownX = 0;
	protected double leftMouseDownY = 0;
	protected ResizeGrabber[] resizeGrabbers = new ResizeGrabber[]{new ResizeGrabber(ResizeGrabber.GrabberPosition.TOP), new ResizeGrabber(ResizeGrabber.GrabberPosition.RIGHT), new ResizeGrabber(ResizeGrabber.GrabberPosition.BOTTOM), new ResizeGrabber(ResizeGrabber.GrabberPosition.LEFT)};
	protected ResizeGrabber activeResizeGrabber = null;

	public AbstractEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
		this.settings = (settings != null) ? settings : new EditorElementSettings();
		this.settings.editorElement = this;
		this.editor = editor;
		this.element = element;
		this.init();
	}

	public AbstractEditorElement(@Nonnull AbstractElement element, @Nonnull LayoutEditorScreen editor) {
		this(element, editor, new EditorElementSettings());
	}
	
	public void init() {

	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.hovered = this.isMouseOver(mouseX, mouseY);

		this.renderBorder(pose, mouseX, mouseY, partial);

		//TODO render menu in editor

	}

	protected void renderBorder(PoseStack pose, int mouseX, int mouseY, float partial) {

		//TODO render border

		for (ResizeGrabber g : this.resizeGrabbers) {
			g.render(pose, mouseX, mouseY, partial);
		}

	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {

	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.isSelected()) {
			return false;
		}
		if (button == 0) {
			if (!this.menu.isUserNavigatingInMenu()) {
				this.activeResizeGrabber = this.getHoveredResizeGrabber();
				if (this.isHovered() || this.isGettingResized()) {
					this.leftMouseDown = true;
					this.leftMouseDownX = mouseX;
					this.leftMouseDownY = mouseY;
				}
			}
			if (this.menu.isOpen() && !this.menu.isHovered()) {
				this.menu.closeMenu();
			}
			return true;
		}
		if (button == 1) {
			if (this.isHovered() && !this.isGettingResized()) {
				this.menu.openMenuAtMouse();
				return true;
			}
			if (!this.isHovered()) {
				this.menu.closeMenu();
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.leftMouseDown = false;
			this.activeResizeGrabber = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
		if (button == 0) {
			if (this.leftMouseDown && !this.isGettingResized()) {
				int diffX = (int)-(this.leftMouseDownX - mouseX);
				int diffY = (int)-(this.leftMouseDownY - mouseY);
				this.element.rawX += diffX;
				this.element.rawY += diffY;
				return true;
			}
			if (this.leftMouseDown && this.isGettingResized()) {
				//TODO handle resize
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double $$0, double $$1, double $$2) {
		return false;
	}

	@Override
	public boolean keyPressed(int $$0, int $$1, int $$2) {
		return false;
	}

	@Override
	public boolean keyReleased(int $$0, int $$1, int $$2) {
		return false;
	}

	@Override
	public boolean charTyped(char c, int key) {

		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return (mouseX >= this.element.getX()) && (mouseX <= this.element.getX() + this.element.getWidth()) && (mouseY >= this.element.getY()) && mouseY <= this.element.getY() + this.element.getHeight();
	}

	@Override
	public void setFocused(boolean var1) {}

	@Override
	public boolean isFocused() {
		return false;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isMultiSelected() {
		return this.multiSelected;
	}

	public void setMultiSelected(boolean multiSelected) {
		this.multiSelected = multiSelected;
	}

	public boolean isHovered() {
		return this.hovered;
	}

	public int getX() {
		return this.element.getX();
	}

	public int getY() {
		return this.element.getY();
	}

	public int getWidth() {
		return this.element.getWidth();
	}

	public int getHeight() {
		return this.element.getHeight();
	}

	public boolean isGettingResized() {
		return this.activeResizeGrabber != null;
	}

	@Nullable
	protected ResizeGrabber getHoveredResizeGrabber() {
		if (this.activeResizeGrabber != null) {
			return this.activeResizeGrabber;
		}
		for (ResizeGrabber g : this.resizeGrabbers) {
			if (g.hovered) {
				return g;
			}
		}
		return null;
	}

	protected class ResizeGrabber extends GuiComponent implements Renderable {

		protected int width = 4;
		protected int height = 4;
		protected final GrabberPosition position;
		protected boolean hovered = false;

		protected ResizeGrabber(GrabberPosition position) {
			this.position = position;
		}

		@Override
		public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
			this.hovered = AbstractEditorElement.this.isSelected() && this.isMouseOver(mouseX, mouseY);
			if (AbstractEditorElement.this.isSelected()) {
				fill(pose, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BORDER_COLOR.supply(AbstractEditorElement.this));
			}
		}

		protected int getX() {
			int x = AbstractEditorElement.this.getX();
			if ((this.position == GrabberPosition.TOP) || (this.position == GrabberPosition.BOTTOM)) {
				x += (AbstractEditorElement.this.getWidth() / 2) - (this.width / 2);
			}
			if (this.position == GrabberPosition.RIGHT) {
				x += AbstractEditorElement.this.getWidth() - (this.width / 2);
			}
			if (this.position == GrabberPosition.LEFT) {
				x -= (this.width / 2);
			}
			return x;
		}

		protected int getY() {
			int y = AbstractEditorElement.this.getY();
			if (this.position == GrabberPosition.TOP) {
				y -= (this.height / 2);
			}
			if ((this.position == GrabberPosition.RIGHT) || (this.position == GrabberPosition.LEFT)) {
				y += (AbstractEditorElement.this.getHeight() / 2) - (this.height / 2);
			}
			if (this.position == GrabberPosition.BOTTOM) {
				y += AbstractEditorElement.this.getHeight() - (this.height / 2);
			}
			return y;
		}

		protected boolean isMouseOver(double mouseX, double mouseY) {
			return (mouseX >= this.getX()) && (mouseX <= this.getX() + this.width) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.height;
		}

		protected enum GrabberPosition {
			TOP,
			RIGHT,
			BOTTOM,
			LEFT;
		}

	}

}
