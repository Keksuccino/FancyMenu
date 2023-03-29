package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

public abstract class CustomizationItemBase extends GuiComponent {
	
	/**
	 * This value CANNOT BE NULL!<br>
	 * If null, {@link CustomizationItemBase#shouldRender()} will never return true.
	 */
	public String value;
	public String action;
	/**
	 * NOT similar to {@link CustomizationItemBase#getPosX(Screen)}! This is the raw value without the defined orientation and scale!
	 */
	public int posX = 0;
	/**
	 * NOT similar to {@link CustomizationItemBase#getPosY(Screen)}! This is the raw value without the defined orientation and scale!
	 */
	public int posY = 0;
	public String orientation = "top-left";
	public String orientationElementIdentifier = null;
	public CustomizationItemBase orientationElement = null;

	public int width = -1;
	public int height = -1;
	
	public String advancedWidth;
	public String advancedHeight;
	public String advancedPosX;
	public String advancedPosY;
	

	public volatile boolean delayAppearance = false;
	public volatile boolean delayAppearanceEverytime = false;
	public volatile float delayAppearanceSec = 1.0F;
	public volatile boolean visible = true;
	public volatile boolean fadeIn = false;
	public volatile float fadeInSpeed = 1.0F;
	public volatile float opacity = 1.0F;

	public VisibilityRequirementContainer visibilityRequirementContainer;

	protected String actionId;
	
	public CustomizationItemBase(PropertiesSection item) {
		
		this.action = item.getEntryValue("action");

		this.actionId = item.getEntryValue("actionid");
		if (this.actionId == null) {
			this.actionId = MenuCustomization.generateRandomActionId();
		}

		String fi = item.getEntryValue("fadein");
		if ((fi != null) && fi.equalsIgnoreCase("true")) {
			this.fadeIn = true;
		}
		String fis = item.getEntryValue("fadeinspeed");
		if ((fis != null) && MathUtils.isFloat(fis)) {
			this.fadeInSpeed = Float.parseFloat(fis);
		}
		String da = item.getEntryValue("delayappearance");
		if ((da != null) && da.equalsIgnoreCase("true")) {
			this.delayAppearance = true;
		}
		String legacyDa = item.getEntryValue("hideforseconds");
		if (legacyDa != null) {
			this.delayAppearance = true;
		}
		String dae = item.getEntryValue("delayappearanceeverytime");
		if ((dae != null) && dae.equalsIgnoreCase("true")) {
			this.delayAppearanceEverytime = true;
		}
		String legacyDae = item.getEntryValue("delayonlyfirsttime");
		if ((legacyDae != null) && legacyDae.equalsIgnoreCase("false")) {
			this.delayAppearanceEverytime = true;
		}
		String das = item.getEntryValue("delayappearanceseconds");
		if ((das != null) && MathUtils.isFloat(das)) {
			this.delayAppearanceSec = Float.parseFloat(das);
		}
		if ((legacyDa != null) && MathUtils.isFloat(legacyDa)) {
			this.delayAppearanceSec = Float.parseFloat(legacyDa);
		}

		String x = item.getEntryValue("x");
		String y = item.getEntryValue("y");
		if (x != null) {
			x = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(x);
			if (MathUtils.isInteger(x)) {
				this.posX = Integer.parseInt(x);
			}
		}
		if (y != null) {
			y = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(y);
			if (MathUtils.isInteger(y)) {
				this.posY = Integer.parseInt(y);
			}
		}
	
		String o = item.getEntryValue("orientation");
		if (o != null) {
			this.orientation = o;
		}

		String oe = item.getEntryValue("orientation_element");
		if (oe != null) {
			this.orientationElementIdentifier = oe;
		}

		String w = item.getEntryValue("width");
		if (w != null) {
			w = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(w);
			if (MathUtils.isInteger(w)) {
				this.width = Integer.parseInt(w);
			}
			if (this.width < 0) {
				this.width = 0;
			}
		}

		String h = item.getEntryValue("height");
		if (h != null) {
			h = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(h);
			if (MathUtils.isInteger(h)) {
				this.height = Integer.parseInt(h);
			}
			if (this.height < 0) {
				this.height = 0;
			}
		}

		
		this.advancedWidth = item.getEntryValue("advanced_width");
		this.advancedHeight = item.getEntryValue("advanced_height");
		this.advancedPosX = item.getEntryValue("advanced_posx");
		this.advancedPosY = item.getEntryValue("advanced_posy");
		

		this.visibilityRequirementContainer = new VisibilityRequirementContainer(item, this);

	}

	public abstract void render(PoseStack matrix, Screen menu) throws IOException;
	
	/**
	 * Should be used to get the REAL and final X-position of this item.<br>
	 * NOT similar to {@code MenuCustomizationItem.posX}! 
	 */
	public int getPosX(Screen menu) {

		
		if (this.advancedPosX != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedPosX).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		

		int w = menu.width;
		int x = this.posX;

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
			x += this.getOrientationElementPosX(menu);
		}
		
		return x;
	}
	
	/**
	 * Should be used to get the REAL and final Y-position of this item.<br>
	 * NOT similar to {@code MenuCustomizationItem.posY}! 
	 */
	public int getPosY(Screen menu) {

		
		if (this.advancedPosY != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedPosY).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
		}
		

		int h = menu.height;
		int y = this.posY;

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
		
		if (orientation.equalsIgnoreCase("top-right")) {
		}
		if (orientation.equalsIgnoreCase("mid-right")) {
			y += (h / 2);
		}
		if (orientation.equalsIgnoreCase("bottom-right")) {
			y += h;
		}

		if (orientation.equalsIgnoreCase("element") && (this.orientationElement != null)) {
			y += this.getOrientationElementPosY(menu);
		}
		
		return y;
	}

	public int getOrientationElementPosX(Screen menu) {
		if (this.orientationElement != null) {
			if (this.orientationElement instanceof VanillaButtonCustomizationItem) {
				AbstractWidget w = ((VanillaButtonCustomizationItem)this.orientationElement).parent.getButton();
				if (w != null) {
					return w.x;
				}
			} else {
				return this.orientationElement.getPosX(menu);
			}
		}
		return 0;
	}

	public int getOrientationElementPosY(Screen menu) {
		if (this.orientationElement != null) {
			if (this.orientationElement instanceof VanillaButtonCustomizationItem) {
				AbstractWidget w = ((VanillaButtonCustomizationItem)this.orientationElement).parent.getButton();
				if (w != null) {
					return w.y;
				}
			} else {
				return this.orientationElement.getPosY(menu);
			}
		}
		return 0;
	}
	
	public boolean shouldRender() {
		if (this.value == null) {
			return false;
		}
		if (!this.visibilityRequirementsMet()) {
			return false;
		}
		return this.visible;
	}

	public String getActionId() {
		return this.actionId;
	}

	public void setActionId(String id) {
		this.actionId = id;
	}

	protected static boolean isEditorActive() {
		return (Minecraft.getInstance().screen instanceof LayoutEditorScreen);
	}

	protected boolean visibilityRequirementsMet() {
		if (isEditorActive()) {
			return true;
		}
		return this.visibilityRequirementContainer.isVisible();
	}

	public int getWidth() {
		
		if (this.advancedWidth != null) {
			String s = PlaceholderParser.replacePlaceholders(this.advancedWidth).replace(" ", "");
			if (MathUtils.isDouble(s)) {
				return (int) Double.parseDouble(s);
			}
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
		
		return this.height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public static enum Alignment {
		
		LEFT("left"),
		RIGHT("right"),
		CENTERED("centered");
		
		public final String key;
		
		private Alignment(String key) {
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

	public static String fixBackslashPath(String path) {
		if (path != null) {
			return path.replace("\\", "/");
		}
		return null;
	}

}
