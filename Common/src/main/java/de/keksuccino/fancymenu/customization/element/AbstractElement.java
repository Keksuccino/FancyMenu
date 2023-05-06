package de.keksuccino.fancymenu.customization.element;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.customization.layouteditor.LayoutEditorScreen;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractElement extends GuiComponent implements Renderable {

	/** The {@link AbstractElement#builder} field is NULL for this element! Keep that in mind when using it as placeholder! **/
	public static final AbstractElement EMPTY_ELEMENT = new AbstractElement(null){public void render(PoseStack p,int i1,int i2,float f){}};

	public final ElementBuilder<?,?> builder;
	public ElementAnchorPoint anchorPoint = ElementAnchorPoint.TOP_LEFT;
	public String anchorPointElementIdentifier = null;
	public AbstractElement anchorPointElement = null;
	/** Not the same as {@link AbstractElement#getX()}! This is the raw value without orientation and scale! **/
	public int rawX = 0;
	/** Not the same as {@link AbstractElement#getY()}! This is the raw value without orientation and scale! **/
	public int rawY = 0;
	public int width = 0;
	public int height = 0;
	public String advancedX;
	public String advancedY;
	public String advancedWidth;
	public String advancedHeight;
	public boolean stretchX = false;
	public boolean stretchY = false;
	public volatile boolean delayAppearance = false;
	public volatile boolean delayAppearanceEverytime = false;
	public volatile float delayAppearanceSec = 1.0F;
	public volatile boolean visible = true;
	public volatile boolean fadeIn = false;
	public volatile float fadeInSpeed = 1.0F;
	public volatile float opacity = 1.0F;
	/**
	 * This is for when the render scale was changed in a non-system-wide way like via {@link PoseStack#translate(float, float, float)}.<br>
	 * Elements that do not support scaling via {@link PoseStack#translate(float, float, float)} need to use this value to manually scale themselves.<br>
	 * This value is -1F by default and is not always set to an actual scale, so check this before using it!
	 **/
	public float customGuiScale = -1F;
	public LoadingRequirementContainer loadingRequirementContainer;
	protected String instanceIdentifier;

	public AbstractElement(@NotNull ElementBuilder<?,?> builder) {
		this.builder = builder;
	}

	@Override
	public abstract void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

	/**
	 * Should be used to get the ACTUAL X position of the element.<br>
	 * Not the same as {@link  AbstractElement#rawX}!
	 */
	public int getX() {
		if (this.advancedX != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedX).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchX) {
			return 0;
		}
		if (this.anchorPoint != null) {
			return this.anchorPoint.getElementPositionX(this);
		}
		return 0;
	}
	
	/**
	 * Should be used to get the ACTUAL Y position of the element.<br>
	 * Not the same as {@link AbstractElement#rawY}!
	 */
	public int getY() {
		if (this.advancedY != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedY).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchY) {
			return 0;
		}
		if (this.anchorPoint != null) {
			return this.anchorPoint.getElementPositionY(this);
		}
		return 0;
	}
	
	public boolean shouldRender() {
		if (!this.loadingRequirementsMet()) {
			return false;
		}
		return this.visible;
	}

	public String getInstanceIdentifier() {
		return this.instanceIdentifier;
	}

	public void setInstanceIdentifier(String id) {
		this.instanceIdentifier = id;
	}

	protected boolean loadingRequirementsMet() {
		if (isEditor()) {
			return true;
		}
		return this.loadingRequirementContainer.requirementsMet();
	}

	public int getWidth() {
		if (this.advancedWidth != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedWidth).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchX) {
			return getScreenWidth();
		}
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		if (this.advancedHeight != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedHeight).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		if (this.stretchY) {
			return getScreenHeight();
		}
		return this.height;
	}

	public void setHeight(int height) {
		this.height = height;
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
	protected static Screen getScreen() {
		return Minecraft.getInstance().screen;
	}

	protected static int getScreenWidth() {
		Screen s = getScreen();
		return (s != null) ? s.width : 0;
	}

	protected static int getScreenHeight() {
		Screen s = getScreen();
		return (s != null) ? s.height : 0;
	}

	public enum Alignment {
		
		LEFT("left"),
		RIGHT("right"),
		CENTERED("centered");
		
		public final String key;
		
		Alignment(String key) {
			this.key = key;
		}

		public static Alignment getByName(String name) {
			for (Alignment a : Alignment.values()) {
				if (a.key.equals(name)) {
					return a;
				}
			}
			return null;
		}
		
	}

}
