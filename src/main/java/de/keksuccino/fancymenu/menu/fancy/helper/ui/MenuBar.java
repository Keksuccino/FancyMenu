package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class MenuBar extends UIBase {
	
	public static final ResourceLocation FM_LOGO_TEXTURE = new ResourceLocation("keksuccino", "fm_pixel_logo.png");
	public static final ResourceLocation SHRINK_BTN_TEXTURE = new ResourceLocation("keksuccino", "shrink_btn.png");
	public static final ResourceLocation EXPAND_BTN_TEXTURE = new ResourceLocation("keksuccino", "expand_btn.png");
	
	protected Map<String, AdvancedButton> leftElements = new LinkedHashMap<String, AdvancedButton>();
	protected Map<String, AdvancedButton> rightElements = new LinkedHashMap<String, AdvancedButton>();
	protected List<String> persistentElements = new ArrayList<String>();
	protected Map<String, ContextMenu> childs = new HashMap<String, ContextMenu>();
	
	protected int height = 20;
	protected Color barColor = new Color(247, 237, 255, 255);
	protected boolean expanded = true;
	protected boolean visible = true;
	public boolean opacityChange = true;
	public float barOpacity = 0.8F;
	public int elementSpace = -1;
	
	public MenuBar() {
		
		//Add default FancyMenu button
		AdvancedButton fmBtn = new AdvancedImageButton(0, 0, 0, 0, FM_LOGO_TEXTURE, true, (press) -> {
			
			Minecraft.getMinecraft().displayGuiScreen(new FMConfigScreen(Minecraft.getMinecraft().currentScreen));
			
		}) {
			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				this.width = this.height;
				super.drawButton(mc, mouseX, mouseY, partialTicks);
			}
		};
		fmBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.menubar.settings"), "%n%"));
		this.addElement(fmBtn, "menubar.default.fancymenubtn", ElementAlignment.LEFT, true);
		
		//Add default expand button
		AdvancedButton expandBtn = new AdvancedImageButton(0, 0, 0, 0, SHRINK_BTN_TEXTURE, true, (press) -> {
			this.toggleExpanded();
			if (this.expanded) {
				((AdvancedImageButton)press).setImage(SHRINK_BTN_TEXTURE);
				((AdvancedButton)press).setDescription(Locals.localize("helper.menubar.shrink"));
			} else {
				((AdvancedImageButton)press).setImage(EXPAND_BTN_TEXTURE);
				((AdvancedButton)press).setDescription(Locals.localize("helper.menubar.expand"));
			}
		}) {
			@Override
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				this.width = this.height;
				super.drawButton(mc, mouseX, mouseY, partialTicks);
			}
		};
		expandBtn.setDescription(Locals.localize("helper.menubar.shrink"));
		this.addElement(expandBtn, "menubar.default.extendbtn", ElementAlignment.RIGHT, true);
		
	}
	
	/**
	 * Adds a new element to the menu bar.<br>
	 * Will replace the old element with the new one, if there's already an element with the same key registered.
	 */
	public void addElement(AdvancedButton element, String key, ElementAlignment alignment, boolean persistent) {
		if ((key != null) && (element != null)) {
			this.removeElement(key);
			if (alignment == ElementAlignment.LEFT) {
				this.leftElements.put(key, element);
			} else if (alignment == ElementAlignment.RIGHT) {
				this.rightElements.put(key, element);
			} else {
				this.leftElements.put(key, element);
			}
			this.setElementPersistent(key, persistent);
		}
	}
	
	/**
	 * Adds a new element to the menu bar.<br>
	 * Will replace the old element with the new one, if there's already an element with the same key registered.
	 */
	public void addElement(AdvancedButton element, String key) {
		this.addElement(element, key, null, false);
	}
	
	public AdvancedButton getElement(String key) {
		if (this.leftElements.containsKey(key)) {
			return this.leftElements.get(key);
		}
		if (this.rightElements.containsKey(key)) {
			return this.rightElements.get(key);
		}
		return null;
	}
	
	/**
	 * If the element should be affected by remove actions.
	 */
	public void setElementPersistent(String key, boolean persistent) {
		if (persistent) {
			if (!this.persistentElements.contains(key)) {
				this.persistentElements.add(key);
			}
		} else {
			this.persistentElements.remove(key);
		}
	}
	
	/**
	 * Will remove an element from the menu bar by the given key.<br>
	 * <b>Will NOT remove persistent elements!</b>
	 */
	public void removeElement(String key) {
		if (key != null) {
			if (!this.persistentElements.contains(key)) {
				if (this.leftElements.containsKey(key)) {
					this.leftElements.remove(key);
				}
				if (this.rightElements.containsKey(key)) {
					this.rightElements.remove(key);
				}
			}
		}
	}
	
	/**
	 * Will remove the given element from the menu bar.<br>
	 * <b>Will NOT remove persistent elements!</b>
	 */
	public void removeElement(AdvancedButton element) {
		List<String> keys = new ArrayList<String>();
		for (Map.Entry<String, AdvancedButton> m : this.leftElements.entrySet()) {
			if (m.getValue() == element) {
				keys.add(m.getKey());
			}
		}
		for (Map.Entry<String, AdvancedButton> m : this.rightElements.entrySet()) {
			if (m.getValue() == element) {
				keys.add(m.getKey());
			}
		}
		for (String s : keys) {
			this.removeElement(s);
		}
	}
	
	/**
	 * Will remove all <b>non-persistent</b> elements from the menu bar.
	 */
	public void removeAllElements() {
		for (String s : this.getElementKeys()) {
			if (!this.persistentElements.contains(s)) {
				this.removeElement(s);
			}
		}
	}
	
	public void addChild(ContextMenu child, String key, ElementAlignment alignment) {
		
		child.setAutoAlignment(false);
		
		if (alignment == ElementAlignment.LEFT) {
			child.setAlignment(false, false);
		} else if (alignment == ElementAlignment.RIGHT) {
			child.setAlignment(false, true);
		} else {
			child.setAlignment(false, false);
		}
		
		this.childs.put(key, child);
		
	}
	
	public ContextMenu getChild(String key) {
		return this.childs.get(key);
	}
	
	public List<String> getElementKeys() {
		List<String> l = new ArrayList<String>();
		l.addAll(this.leftElements.keySet());
		l.addAll(this.rightElements.keySet());
		return l;
	}
	
	public List<AdvancedButton> getElements() {
		List<AdvancedButton> l = new ArrayList<AdvancedButton>();
		l.addAll(this.leftElements.values());
		l.addAll(this.rightElements.values());
		return l;
	}
	
	public void removeChild(String key) {
		if (this.childs.containsKey(key)) {
			this.childs.remove(key);
		}
	}
	
	public void removeChild(ContextMenu child) {
		List<String> keys = new ArrayList<String>();
		for (Map.Entry<String, ContextMenu> m : this.childs.entrySet()) {
			if (m.getValue() == child) {
				keys.add(m.getKey());
			}
		}
		for (String s : keys) {
			this.removeChild(s);
		}
	}
	
	public void removeAllChilds() {
		this.childs.clear();
	}
	
	public boolean isChildOpen() {
		for (ContextMenu m : this.childs.values()) {
			if (m.isOpen()) {
				return true;
			}
		}
		return false;
	}
	
	public void render(GuiScreen screen) {
		
		if (AnimationHandler.isReady()) {
			if (screen != null) {
				
				if (this.visible) {
					
					if (this.isChildOpen()) {
						MouseInput.blockVanillaInput("fmcustomizationhelper");
					} else {
						MouseInput.unblockVanillaInput("fmcustomizationhelper");
					}
					
					MouseInput.setRenderScale(this.getScale());
					
					Minecraft mc = Minecraft.getMinecraft();
					int mouseX = MouseInput.getMouseX();
					int mouseY = MouseInput.getMouseY();
					int width = screen.width;
					float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
					
					MouseInput.resetRenderScale();
					
					RenderUtils.setZLevelPre(400);
					
					GlStateManager.scale(this.getScale(), this.getScale(), this.getScale());

					GlStateManager.enableBlend();
					
					if (this.expanded) {
						
						this.renderBackground(screen);
						
						//Render all child context menus
						//(It's important to render childs first, so height, width and scale updates
						//made by button clicks can be applied before the menu is getting rendered.)
						for (ContextMenu m : this.childs.values()) {
							m.setButtonHeight(this.height);
							m.render(mouseX, mouseY);
						}
						
						//Render left bar elements
						int xl = 0;
						for (AdvancedButton b : this.leftElements.values()) {
							if (b.visible) {
								b.height = this.height;
								if (!(b instanceof AdvancedImageButton)) {
									int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(b.displayString);
									b.setWidth(i + 12);
								}
								b.x = xl;
								b.y = 0;
								colorizeButton(b);
								b.drawButton(mc, mouseX, mouseY, partialTicks);
								xl += b.width + this.elementSpace;
							}
						}
						
						//Render right bar elements
						int xr = (int) (width / this.getScale());
						for (AdvancedButton b : this.rightElements.values()) {
							if (b.visible) {
								b.height = this.height;
								if (!(b instanceof AdvancedImageButton)) {
									int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(b.displayString);
									b.setWidth(i + 12);
								}
								xr -= b.width;
								b.x = xr;
								b.y = 0;
								colorizeButton(b);
								b.drawButton(mc, mouseX, mouseY, partialTicks);
								xr -= this.elementSpace;
							}
						}
						
					} else {
						
						AdvancedButton right = this.rightElements.get("menubar.default.extendbtn");
						if (right != null) {

							right.height = this.height;
							right.x = (int) ((width / this.getScale()) - right.width);
							right.y = 0;
							colorizeButton(right);
							right.drawButton(mc, mouseX, mouseY, partialTicks);
							
						}
						
					}
					
					RenderUtils.setZLevelPost();
					
				}
				
			}
		}
		
	}
	
	protected void renderBackground(GuiScreen screen) {
		if (this.expanded) {
			if ((screen != null) && (this.barColor != null)) {
				RenderUtils.fill(0, 0, screen.width / this.getScale(), this.height, this.barColor.getRGB(), this.barOpacity);
			}
		}
	}
	
	public boolean isHovered() {
		if (Minecraft.getMinecraft().currentScreen == null) {
			return false;
		}
		
		MouseInput.setRenderScale(this.getScale());
		
		int width = Minecraft.getMinecraft().currentScreen.width;
		int mX = MouseInput.getMouseX();
		int mY = MouseInput.getMouseY();
		
		MouseInput.resetRenderScale();
		
		for (ContextMenu m : this.childs.values()) {
			if (m.isOpen() && m.isHovered()) {
				return true;
			}
		}
		
		return ((mX <= width) && (mX >= 0) && (mY <= this.height) && (mY >= 0));
	}
	
	public float getScale() {
		return getUIScale();
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setBarColor(Color color) {
		this.barColor = color;
	}
	
	public void setExtended(boolean extended) {
		this.expanded = extended;
	}
	
	public void toggleExpanded() {
		this.expanded = !this.expanded;
	}
	
	public boolean isExtended() {
		return this.expanded;
	}
	
	public static enum ElementAlignment {
		LEFT,
		RIGHT;
	}
	
}
