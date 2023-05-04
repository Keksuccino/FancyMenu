package de.keksuccino.fancymenu.customization.backend.element;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.customization.backend.loadingrequirement.v2.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.backend.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.backend.element.v1.button.VanillaButtonCustomizationItem;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractElement extends GuiComponent implements Renderable {

	public final ElementBuilder<?,?> builder;
	public String orientation = "top-left";
	public String orientationElementIdentifier = null;
	public AbstractElement orientationElement = null;
	/** Not the same as {@link AbstractElement#getX()}! This is the raw value without orientation and scale! **/
	public int rawX = 0;
	/** Not the same as {@link AbstractElement#getY()}! This is the raw value without orientation and scale! **/
	public int rawY = 0;
	public int width = 20;
	public int height = 20;
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

	public AbstractElement(ElementBuilder<?,?> builder, SerializedElement serializedElement) {
		this.builder = builder;
		this.init(serializedElement);
	}

	/**
	 * Deserializes the given {@link SerializedElement} to this {@link AbstractElement}.
	 */
	protected void init(SerializedElement serializedElement) {

		this.instanceIdentifier = serializedElement.getEntryValue("actionid");
		if (this.instanceIdentifier == null) {
			this.instanceIdentifier = MenuCustomization.generateUniqueIdentifier();
		}

		String fi = serializedElement.getEntryValue("fadein");
		if ((fi != null) && fi.equalsIgnoreCase("true")) {
			this.fadeIn = true;
		}
		String fis = serializedElement.getEntryValue("fadeinspeed");
		if ((fis != null) && MathUtils.isFloat(fis)) {
			this.fadeInSpeed = Float.parseFloat(fis);
		}
		String da = serializedElement.getEntryValue("delayappearance");
		if ((da != null) && da.equalsIgnoreCase("true")) {
			this.delayAppearance = true;
		}
		String legacyDa = serializedElement.getEntryValue("hideforseconds");
		if (legacyDa != null) {
			this.delayAppearance = true;
		}
		String dae = serializedElement.getEntryValue("delayappearanceeverytime");
		if ((dae != null) && dae.equalsIgnoreCase("true")) {
			this.delayAppearanceEverytime = true;
		}
		String legacyDae = serializedElement.getEntryValue("delayonlyfirsttime");
		if ((legacyDae != null) && legacyDae.equalsIgnoreCase("false")) {
			this.delayAppearanceEverytime = true;
		}
		String das = serializedElement.getEntryValue("delayappearanceseconds");
		if ((das != null) && MathUtils.isFloat(das)) {
			this.delayAppearanceSec = Float.parseFloat(das);
		}
		if ((legacyDa != null) && MathUtils.isFloat(legacyDa)) {
			this.delayAppearanceSec = Float.parseFloat(legacyDa);
		}

		String x = serializedElement.getEntryValue("x");
		String y = serializedElement.getEntryValue("y");
		if (x != null) {
			x = PlaceholderParser.replacePlaceholders(x);
			if (MathUtils.isInteger(x)) {
				this.rawX = Integer.parseInt(x);
			}
		}
		if (y != null) {
			y = PlaceholderParser.replacePlaceholders(y);
			if (MathUtils.isInteger(y)) {
				this.rawY = Integer.parseInt(y);
			}
		}

		String o = serializedElement.getEntryValue("orientation");
		if (o != null) {
			this.orientation = o;
		}

		String oe = serializedElement.getEntryValue("orientation_element");
		if (oe != null) {
			this.orientationElementIdentifier = oe;
		}

		String w = serializedElement.getEntryValue("width");
		if (w != null) {
			if (w.equals("%guiwidth%")) {
				this.stretchX = true;
			} else {
				if (MathUtils.isInteger(w)) {
					this.width = Integer.parseInt(w);
				}
				if (this.width < 0) {
					this.width = 0;
				}
			}
		}

		String h = serializedElement.getEntryValue("height");
		if (h != null) {
			if (h.equals("%guiheight%")) {
				this.stretchY = true;
			} else {
				if (MathUtils.isInteger(h)) {
					this.height = Integer.parseInt(h);
				}
				if (this.height < 0) {
					this.height = 0;
				}
			}
		}

		this.advancedWidth = serializedElement.getEntryValue("advanced_width");
		this.advancedHeight = serializedElement.getEntryValue("advanced_height");
		this.advancedX = serializedElement.getEntryValue("advanced_posx");
		this.advancedY = serializedElement.getEntryValue("advanced_posy");

		this.loadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(serializedElement);

	}
	
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
		int w = getScreenWidth();
		int x = this.rawX;
		if (orientation.equalsIgnoreCase("top-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("mid-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-centered")) {
			x += (w / 2);
		}
		if (orientation.equalsIgnoreCase("top-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			x += w;
		}
		if (orientation.equalsIgnoreCase("element") && (this.orientationElement != null)) {
			x += this.getOrientationElementPosX();
		}
		return x;
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
		int h = getScreenHeight();
		int y = this.rawY;
		if (orientation.equalsIgnoreCase("mid-left")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-left")) {
			y += h;
		}
		if (orientation.equalsIgnoreCase("mid-centered")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-centered")) {
			y += h;
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			y += h;
		}
		if (orientation.equalsIgnoreCase("element") && (this.orientationElement != null)) {
			y += this.getOrientationElementPosY();
		}
		return y;
	}

	public int getOrientationElementPosX() {
		if (this.orientationElement != null) {
			if (this.orientationElement instanceof VanillaButtonCustomizationItem) {
				AbstractWidget w = ((VanillaButtonCustomizationItem)this.orientationElement).parent.getButton();
				if (w != null) {
					return w.x;
				}
			} else {
				return this.orientationElement.getX();
			}
		}
		return 0;
	}

	public int getOrientationElementPosY() {
		if (this.orientationElement != null) {
			if (this.orientationElement instanceof VanillaButtonCustomizationItem) {
				AbstractWidget w = ((VanillaButtonCustomizationItem)this.orientationElement).parent.getButton();
				if (w != null) {
					return w.y;
				}
			} else {
				return this.orientationElement.getY();
			}
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
