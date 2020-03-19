package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationItem.Type;
import de.keksuccino.math.MathUtils;
import de.keksuccino.properties.PropertiesSection;
import de.keksuccino.properties.PropertiesSet;
import de.keksuccino.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public abstract class MenuHandlerBase {
	
	protected List<MenuCustomizationItem> items = new ArrayList<MenuCustomizationItem>();
	protected IAnimationRenderer backgroundAnimation = null;
	protected boolean replayIntro = false;
	private boolean animationSet = false;
	
	//TODO Identifier and type are not used like planned, so I will change this later 
	public abstract String getMenuIdentifier();
	
	@Nullable
	public abstract Class<?> getMenuType();
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (!this.shouldCustomize(e.getGui())) {
			return;
		}
		
		List<GuiButton> buttons = e.getButtonList();
		List<PropertiesSet> props = MenuCustomizationProperties.getPropertiesWithIdentifier(this.getMenuIdentifier());
		
		items.clear();
		
		for (PropertiesSet s : props) {
			for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
				String action = sec.getEntryValue("action");
				if (action != null) {
					String identifier = sec.getEntryValue("identifier");
					GuiButton b = null;
					if (identifier != null) {
						b = getButton(identifier, buttons);
					}

					if (action.equalsIgnoreCase("animatebackground")) {
						String value = sec.getEntryValue("name");
						String intro = sec.getEntryValue("replayanimation");
						if (value != null) {
							if (AnimationHandler.animationExists(value)) {
								this.backgroundAnimation = AnimationHandler.getAnimation(value);
								this.animationSet = true;
							} else {
								this.backgroundAnimation = null;
							}
						}
						if (intro != null) {
							if (intro.equalsIgnoreCase("true")) {
								this.replayIntro = true;
							}
							if (intro.equalsIgnoreCase("false")) {
								this.replayIntro = false;
							}
						}
					} else {
						if (!this.animationSet) {
							this.backgroundAnimation = null;
						}
					}
					
					if (action.equalsIgnoreCase("hidebutton")) {
						if (b != null) {
							b.visible = false;
						}
					}
					
					if (action.equalsIgnoreCase("renamebutton")) {
						String value = sec.getEntryValue("value");
						if ((value != null) && (b != null)) {
							b.displayString = value;
						}
					}
					
					if (action.equalsIgnoreCase("resizebutton")) {
						String width = sec.getEntryValue("width");
						String height = sec.getEntryValue("height");
						if ((width != null) && (height != null) && MathUtils.isInteger(width) && MathUtils.isInteger(height) && (b != null)) {
							int w = Integer.parseInt(width);
							int h = Integer.parseInt(height);
							b.setWidth(w);
							b.height = h;
						}
					}
					
					if (action.equalsIgnoreCase("movebutton")) {
						String posX = sec.getEntryValue("x");
						String posY = sec.getEntryValue("y");
						String orientation = sec.getEntryValue("orientation");
						if ((orientation != null) && (posX != null) && (posY != null) && MathUtils.isInteger(posX) && MathUtils.isInteger(posY) && (b != null)) {
							int x = Integer.parseInt(posX);
							int y = Integer.parseInt(posY);
							int w = e.getGui().width;
							int h = e.getGui().height;
							
							if (orientation.equalsIgnoreCase("original")) {
								b.x = b.x + x;
								b.y = b.y + y;
							}
							//-----------------------------
							if (orientation.equalsIgnoreCase("top-left")) {
								b.x = x;
								b.y = y;
							}
							
							if (orientation.equalsIgnoreCase("mid-left")) {
								b.x = x;
								b.y = (h / 2) + y;
							}
							
							if (orientation.equalsIgnoreCase("bottom-left")) {
								b.x = x;
								b.y = h + y;
							}
							//----------------------------
							if (orientation.equalsIgnoreCase("top-centered")) {
								b.x = (w / 2) + x;
								b.y = y;
							}
							
							if (orientation.equalsIgnoreCase("mid-centered")) {
								b.x = (w / 2) + x;
								b.y = (h / 2) + y;
							}
							
							if (orientation.equalsIgnoreCase("bottom-centered")) {
								b.x = (w / 2) + x;
								b.y = h + y;
							}
							//-----------------------------
							if (orientation.equalsIgnoreCase("top-right")) {
								b.x = w + x;
								b.y = y;
							}
							
							if (orientation.equalsIgnoreCase("mid-right")) {
								b.x = w + x;
								b.y = (h / 2) + y;
							}
							
							if (orientation.equalsIgnoreCase("bottom-right")) {
								b.x = w + x;
								b.y = h + y;
							}
						}
					}
					
					if (action.equalsIgnoreCase("addtext")) {
						items.add(new MenuCustomizationItem(sec, Type.STRING));
					}
					
					if (action.equalsIgnoreCase("addtexture")) {
						items.add(new MenuCustomizationItem(sec, Type.TEXTURE));
					}
					
					if (action.equalsIgnoreCase("addanimation")) {
						items.add(new MenuCustomizationItem(sec, Type.ANIMATION));
					}
					
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (this.canRenderBackground() && this.shouldCustomize(e.getGui())) {
			if (this.replayIntro) {
				this.backgroundAnimation.resetAnimation();
			}
			this.animationSet = false;
		}
	}
	
	@SubscribeEvent
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (!this.shouldCustomize(e.getGui())) {
			return;
		}
		
		for (MenuCustomizationItem i : this.items) {
			try {
				i.render(e.getGui());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@SubscribeEvent
	public void drawToBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			//Rendering the background animation to the menu
			if (this.canRenderBackground() && this.backgroundAnimation.isReady()) {
				boolean b = this.backgroundAnimation.isStretchedToStreensize();
				this.backgroundAnimation.setStretchImageToScreensize(true);
				this.backgroundAnimation.render();
				this.backgroundAnimation.setStretchImageToScreensize(b);
			}
		}
	}
	
	private static GuiButton getButton(String identifier, List<GuiButton> buttons) {
		if (identifier.startsWith("%id=")) { //%id=1%
			String p = identifier.split("[=]")[1].replace("%", "");
			if (!MathUtils.isInteger(p)) {
				return null;
			}
			int id = Integer.parseInt(p);
			
			ButtonData b = ButtonCache.getButtonForId(id);
			if (b != null) {
				return b.getButton();
			}
		} else {
			ButtonData b = null;
			if (I18n.hasKey(identifier)) {
				b = ButtonCache.getButtonForKey(identifier);
			} else {
				b = ButtonCache.getButtonForName(identifier);
			}
			if (b != null) {
				return b.getButton();
			}
		}
		return null;
	}
	
	protected boolean shouldCustomize(GuiScreen menu) {
		if (getMenuType() != null) {
			if (!this.getMenuType().isAssignableFrom(menu.getClass())) {
				return false;
			}
		}
		return true;
	}
	
	protected boolean canRenderBackground() {
		return (this.backgroundAnimation != null);
	}

}
