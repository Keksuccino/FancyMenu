package de.keksuccino.fancymenu.customization.element;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public abstract class AbstractElement extends GuiComponent implements Renderable, GuiEventListener, NarratableEntry, NavigatableWidget {

	/** The {@link AbstractElement#builder} field is NULL for this element! Keep that in mind when using it as placeholder! **/
	@SuppressWarnings("all")
	public static final AbstractElement EMPTY_ELEMENT = new AbstractElement(null){public void render(@NotNull PoseStack p, int i1, int i2, float f){}};

	public static final int STAY_ON_SCREEN_EDGE_ZONE_SIZE = 2;

	public final ElementBuilder<?,?> builder;
	public ElementAnchorPoint anchorPoint = ElementAnchorPoints.MID_CENTERED;
	public String anchorPointElementIdentifier = null;
	protected AbstractElement cachedAnchorPointElement = null;
	/** Not the same as {@link AbstractElement#getAbsoluteX()}! This is the X-offset from the origin of its anchor! **/
	public int posOffsetX = 0;
	/** Not the same as {@link AbstractElement#getAbsoluteY()}! This is the Y-offset from the origin of its anchor! **/
	public int posOffsetY = 0;
	public int baseWidth = 0;
	public int baseHeight = 0;
	public String advancedX;
	public String advancedY;
	public String advancedWidth;
	public String advancedHeight;
	public boolean stretchX = false;
	public boolean stretchY = false;
	public boolean stayOnScreen = true;
	public volatile boolean visible = true;
	public volatile AppearanceDelay appearanceDelay = AppearanceDelay.NO_DELAY;
	public volatile float appearanceDelayInSeconds = 1.0F;
	public volatile boolean fadeIn = false;
	public volatile float fadeInSpeed = 1.0F;
	public volatile float opacity = 1.0F;
	/**
	 * This is for when the render scale was changed in a non-system-wide way like via {@link PoseStack#translate(float, float, float)}.<br>
	 * Elements that do not support scaling via {@link PoseStack#translate(float, float, float)} need to use this value to manually scale themselves.<br>
	 * This value is -1F by default and is not always set to an actual scale, so check this before using it!
	 **/
	public float customGuiScale = -1F;
	public LoadingRequirementContainer loadingRequirementContainer = new LoadingRequirementContainer();
	@Nullable
	public String customElementLayerName = null;
	private String instanceIdentifier;

	@SuppressWarnings("all")
	public AbstractElement(@NotNull ElementBuilder<?,?> builder) {
		this.builder = builder;
		this.instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
	}

	@Override
	public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

	/** Gets called every {@link Screen} tick, after {@link Screen#tick()} got called. **/
	public void tick() {
	}

	/**
	 * Gets called before a {@link Screen} gets closed.<br>
	 * A screen gets closed when a new active {@link Screen} gets set via {@link Minecraft#setScreen(Screen)}.
	 */
	public void onCloseScreen() {
	}

	/**
	 * Gets called after a {@link Screen} got opened via {@link Minecraft#setScreen(Screen)}.
	 */
	public void onOpenScreen() {
	}

	/** Widgets need to extend {@link GuiEventListener} and {@link NarratableEntry}. **/
	@Nullable
	public List<GuiEventListener> getWidgetsToRegister() {
		return null;
	}

	/**
	 * Should be used to get the ACTUAL X position of the element.<br>
	 * Not the same as {@link AbstractElement#posOffsetX}!
	 */
	public int getAbsoluteX() {
		int x = 0;
		if (this.anchorPoint != null) {
			x = this.anchorPoint.getElementPositionX(this);
		}
		if (this.advancedX != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedX).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				x = (int) Double.parseDouble(s);
			}
		}
		if (this.stretchX) {
			x = 0;
		} else if (this.stayOnScreen) {
			if (x < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
				x = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
			}
			if (x > (getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth())) {
				x = getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth();
			}
		}
		return x;
	}
	
	/**
	 * Should be used to get the ACTUAL Y position of the element.<br>
	 * Not the same as {@link AbstractElement#posOffsetY}!
	 */
	public int getAbsoluteY() {
		int y = 0;
		if (this.anchorPoint != null) {
			y = this.anchorPoint.getElementPositionY(this);
		}
		if (this.advancedY != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedY).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				y = (int) Double.parseDouble(s);
			}
		}
		if (this.stretchY) {
			y = 0;
		} else if (this.stayOnScreen) {
			if (y < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
				y = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
			}
			if (y > (getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight())) {
				y = getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight();
			}
		}
		return y;
	}

	@Nullable
	public AbstractElement getParentElementAnchorPointElement() {
		if (this.anchorPointElementIdentifier == null) return null;
		if (this.cachedAnchorPointElement == null) {
			this.cachedAnchorPointElement = getElementByInstanceIdentifier(this.anchorPointElementIdentifier);
		}
		return this.cachedAnchorPointElement;
	}

	public void setParentElementAnchorPointElement(AbstractElement element) {
		this.cachedAnchorPointElement = element;
	}
	
	public boolean shouldRender() {
		if (!this.loadingRequirementsMet()) {
			return false;
		}
		return this.visible;
	}

	@NotNull
	public String getInstanceIdentifier() {
		return this.instanceIdentifier;
	}

	public void setInstanceIdentifier(@NotNull String id) {
		this.instanceIdentifier = Objects.requireNonNull(id);
	}

	protected boolean loadingRequirementsMet() {
		if (isEditor()) {
			return true;
		}
		return this.loadingRequirementContainer.requirementsMet();
	}

	public int getAbsoluteWidth() {
		if (this.advancedWidth != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedWidth).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchX) {
			return getScreenWidth();
		}
		return this.baseWidth;
	}

	public int getAbsoluteHeight() {
		if (this.advancedHeight != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedHeight).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchY) {
			return getScreenHeight();
		}
		return this.baseHeight;
	}

	/**
	 * This is the X position used by the child element if this element is used as {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	public int getElementAnchorX() {
		return this.getAbsoluteX();
	}

	/**
	 * This is the Y position used by the child element if this element is used as {@link ElementAnchorPoints#ELEMENT} anchor.
	 */
	public int getElementAnchorY() {
		return this.getAbsoluteY();
	}

	public static String fixBackslashPath(String path) {
		if (path != null) {
			return path.replace("\\", "/");
		}
		return null;
	}

	protected static boolean isEditor() {
		return (getScreen() instanceof LayoutEditorScreen);
	}

	@Nullable
	public static Screen getScreen() {
		return Minecraft.getInstance().screen;
	}

	public static int getScreenWidth() {
		Screen s = getScreen();
		return (s != null) ? s.width : 0;
	}

	public static int getScreenHeight() {
		Screen s = getScreen();
		return (s != null) ? s.height : 0;
	}

	@SuppressWarnings("all")
	@Nullable
	public static AbstractElement getElementByInstanceIdentifier(String identifier) {
		if (isEditor()) {
			return ((LayoutEditorScreen)getScreen()).getElementByInstanceIdentifier(identifier).element;
		} else {
			ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
			if (layer != null) return layer.getElementByInstanceIdentifier(identifier);
		}
		return null;
	}

	/**
	 * Replaces placeholders and deserializes serialized {@link MutableComponent}s.
	 */
	@NotNull
	public static Component buildComponent(@NotNull String serializedComponentOrPlainText) {
		serializedComponentOrPlainText = PlaceholderParser.replacePlaceholders(serializedComponentOrPlainText);
		if (!serializedComponentOrPlainText.startsWith("{")) return Component.literal(serializedComponentOrPlainText);
		try {
			Component c = Component.Serializer.fromJson(serializedComponentOrPlainText);
			if (c != null) return c;
		} catch (Exception ignore) {}
		return Component.literal(serializedComponentOrPlainText);
	}

	@Override
	public void setFocused(boolean var1) {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	@Override
	public @NotNull NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
	}

	@Override
	public boolean isFocusable() {
		return false;
	}

	@Override
	public void setFocusable(boolean focusable) {
		throw new RuntimeException("AbstractElements are not focusable!");
	}

	@Override
	public boolean isNavigatable() {
		return false;
	}

	@Override
	public void setNavigatable(boolean navigatable) {
		throw new RuntimeException("AbstractElements are not navigatable!");
	}

	public enum Alignment {
		
		LEFT("left"),
		RIGHT("right"),
		CENTERED("centered");
		
		public final String key;
		
		Alignment(String key) {
			this.key = key;
		}

		public static Alignment getByName(@NotNull String name) {
			for (Alignment a : Alignment.values()) {
				if (a.key.equals(name)) {
					return a;
				}
			}
			return null;
		}
		
	}

	public enum AppearanceDelay {

		NO_DELAY("no_delay"),
		FIRST_TIME("first_time"),
		EVERY_TIME("every_time");

		public final String name;

		AppearanceDelay(String name) {
			this.name = name;
		}

		@Nullable
		public static AppearanceDelay getByName(@NotNull String name) {
			for (AppearanceDelay d : AppearanceDelay.values()) {
				if (d.name.equals(name)) {
					return d;
				}
			}
			return null;
		}

	}

}
