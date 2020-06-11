package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.properties.PropertiesSet;
import de.keksuccino.core.rendering.RenderUtils;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.ButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.VanillaButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebStringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class MenuHandlerBase {
	
	protected List<CustomizationItemBase> frontRenderItems = new ArrayList<CustomizationItemBase>();
	protected List<CustomizationItemBase> backgroundRenderItems = new ArrayList<CustomizationItemBase>();
	
	protected Map<String, Boolean> audio = new HashMap<String, Boolean>();
	protected List<String> oldAudio = new ArrayList<String>();
	protected IAnimationRenderer backgroundAnimation = null;
	protected IAnimationRenderer lastBackgroundAnimation = null;
	protected List<IAnimationRenderer> backgroundAnimations = new ArrayList<IAnimationRenderer>();
	protected int backgroundAnimationId = 0;
	protected ExternalTextureResourceLocation backgroundTexture = null;
	private String identifier;
	private boolean backgroundDrawable;
	protected boolean panoramaback = false;
	private int panoTick = 0;
	private double panoPos = 0.0;
	private boolean panoMoveBack = false;
	private boolean panoStop = false;
	
	/**
	 * @param identifier Has to be the valid and full class name of the GUI screen.
	 */
	public MenuHandlerBase(@Nonnull String identifier) {
		this.identifier = identifier;
	}
	
	public String getMenuIdentifier() {
		return this.identifier;
	}
	
	@SubscribeEvent
	public void onInitPost(ButtonCachedEvent e) {
		if (!this.shouldCustomize(e.getGui())) {
			return;
		}
		if (!AnimationHandler.isReady()) {
			return;
		}
		if (LayoutCreatorScreen.isActive) {
			return;
		}
		
		List<Widget> buttons = e.getWidgetList();
		List<PropertiesSet> props = MenuCustomizationProperties.getPropertiesWithIdentifier(this.getMenuIdentifier());
		
		audio.clear();
		frontRenderItems.clear();
		backgroundRenderItems.clear();
		this.backgroundAnimation = null;
		this.backgroundAnimations.clear();
		if ((this.backgroundAnimation != null) && (this.backgroundAnimation instanceof AdvancedAnimation)) {
			((AdvancedAnimation)this.backgroundAnimation).stopAudio();
		}
		this.backgroundDrawable = false;
		
		boolean backgroundTextureSet = false;

		for (PropertiesSet s : props) {
			List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
			if (metas.isEmpty()) {
				metas = s.getPropertiesOfType("type-meta");
			}
			if (metas.isEmpty()) {
				continue;
			}
			String renderOrder = metas.get(0).getEntryValue("renderorder");
			for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
				String action = sec.getEntryValue("action");
				if (action != null) {
					String identifier = sec.getEntryValue("identifier");
					Widget b = null;
					if (identifier != null) {
						b = getButton(identifier, buttons);
					}

					if (action.equalsIgnoreCase("texturizebackground")) {
						String value = sec.getEntryValue("path");
						String pano = sec.getEntryValue("panorama");
						if (value != null) {
							File f = new File(value.replace("\\", "/"));
							if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
								if ((this.backgroundTexture == null) || !this.backgroundTexture.getPath().equals(value)) {
									this.backgroundTexture = TextureHandler.getResource(value);
								}
								if ((pano != null) && pano.equalsIgnoreCase("true")) {
									this.panoramaback = true;
								} else {
									this.panoramaback = false;
								}
								backgroundTextureSet = true;
							}
						}
					}
					
					if (action.equalsIgnoreCase("animatebackground")) {
						String value = sec.getEntryValue("name");
						String random = sec.getEntryValue("random");
						boolean ran = false;
						if ((random != null) && random.equalsIgnoreCase("true")) {
							ran = true;
						}
						if (value != null) {
							if (value.contains(",")) {
								for (String s2 : value.split("[,]")) {
									int i = 0;
									for (char c : s2.toCharArray()) {
										if (c != " ".charAt(0)) {
											break;
										}
										i++;
									}
									if (i > s2.length()) {
										continue;
									}
									String temp = new StringBuilder(s2.substring(i)).reverse().toString();
									int i2 = 0;
									for (char c : temp.toCharArray()) {
										if (c != " ".charAt(0)) {
											break;
										}
										i2++;
									}
									String name = new StringBuilder(temp.substring(i2)).reverse().toString();
									if (AnimationHandler.animationExists(name)) {
										this.backgroundAnimations.add(AnimationHandler.getAnimation(name));
									}
								}
							} else {
								if (AnimationHandler.animationExists(value)) {
									this.backgroundAnimations.add(AnimationHandler.getAnimation(value));
								}
							}
							
							if (!this.backgroundAnimations.isEmpty()) {
								if (ran) {
									if ((MenuHandlerRegistry.getLastActiveHandler() == null) || (MenuHandlerRegistry.getLastActiveHandler() != this)) {
										this.backgroundAnimationId = MathUtils.getRandomNumberInRange(0, this.backgroundAnimations.size()-1);
									}
									this.backgroundAnimation = this.backgroundAnimations.get(this.backgroundAnimationId);
								} else {
									if ((this.lastBackgroundAnimation != null) && this.backgroundAnimations.contains(this.lastBackgroundAnimation)) {
										this.backgroundAnimation = this.lastBackgroundAnimation;
									} else {
										this.backgroundAnimationId = 0;
										this.backgroundAnimation = this.backgroundAnimations.get(0);
									}
									this.lastBackgroundAnimation = this.backgroundAnimation;
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("hidebutton")) {
						if (b != null) {
							b.visible = false;
						}
					}

					if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
						String value = sec.getEntryValue("value");
						if ((value != null) && (b != null)) {
							b.setMessage(value);;
						}
					}
					
					if (action.equalsIgnoreCase("resizebutton")) {
						String width = sec.getEntryValue("width");
						String height = sec.getEntryValue("height");
						if (width != null) {
							width = width.replace("%guiwidth%", "" + e.getGui().width).replace("%guiheight%", "" + e.getGui().height);
						}
						if (height != null) {
							height = height.replace("%guiwidth%", "" + e.getGui().width).replace("%guiheight%", "" + e.getGui().height);
						}
						if ((width != null) && (height != null) && (b != null)) {
							int w = (int) MathUtils.calculateFromString(width);
							int h = (int) MathUtils.calculateFromString(height);
							b.setWidth(w);
							b.setHeight(h);;
						}
					}
					
					if (action.equalsIgnoreCase("movebutton")) {
						String posX = sec.getEntryValue("x");
						String posY = sec.getEntryValue("y");
						if (posX != null) {
							posX = posX.replace("%guiwidth%", "" + e.getGui().width).replace("%guiheight%", "" + e.getGui().height);
						}
						if (posY != null) {
							posY = posY.replace("%guiwidth%", "" + e.getGui().width).replace("%guiheight%", "" + e.getGui().height);
						}
						String orientation = sec.getEntryValue("orientation");
						if ((orientation != null) && (posX != null) && (posY != null) && (b != null)) {
							int x = (int) MathUtils.calculateFromString(posX);
							int y = (int) MathUtils.calculateFromString(posY);
							int w = e.getGui().width;
							int h = e.getGui().height;

							//TODO Remove deprecated "original" orientation
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
					
					if (action.equalsIgnoreCase("setbuttontexture")) {
						if (b != null) {
							String backNormal = sec.getEntryValue("backgroundnormal");
							String backHover = sec.getEntryValue("backgroundhovered");
							if ((backNormal != null) && (backHover != null)) {
								File f = new File(backNormal.replace("\\", "/"));
								File f2 = new File(backHover.replace("\\", "/"));
								if (f.isFile() && f.exists() && f2.isFile() && f2.exists()) {
									b.visible = false;
									frontRenderItems.add(new VanillaButtonCustomizationItem(sec, b));
								}
							}
						}
					}

					if (action.equalsIgnoreCase("addhoversound")) {
						if (b != null) {
							if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
								backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, b));
							} else {
								frontRenderItems.add(new VanillaButtonCustomizationItem(sec, b));
							}
						}
					}

					if (action.equalsIgnoreCase("sethoverlabel")) {
						if (b != null) {
							if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
								backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, b));
							} else {
								frontRenderItems.add(new VanillaButtonCustomizationItem(sec, b));
							}
						}
					}
					
					if (action.equalsIgnoreCase("clickbutton")) {
						if (b != null) {
							String clicks = sec.getEntryValue("clicks");
							if ((clicks != null) && (MathUtils.isInteger(clicks))) {
								for (int i = 0; i < Integer.parseInt(clicks); i++) {
									b.onClick(MouseInput.getMouseX(), MouseInput.getMouseY());
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("addtext")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new StringCustomizationItem(sec));
						} else {
							frontRenderItems.add(new StringCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addwebtext")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new WebStringCustomizationItem(sec));
						} else {
							frontRenderItems.add(new WebStringCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addtexture")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new TextureCustomizationItem(sec));
						} else {
							frontRenderItems.add(new TextureCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addwebtexture")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new WebTextureCustomizationItem(sec));
						} else {
							frontRenderItems.add(new WebTextureCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addanimation")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new AnimationCustomizationItem(sec));
						} else {
							frontRenderItems.add(new AnimationCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addbutton")) {
						if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
							backgroundRenderItems.add(new ButtonCustomizationItem(sec));
						} else {
							frontRenderItems.add(new ButtonCustomizationItem(sec));
						}
					}
					
					if (action.equalsIgnoreCase("addaudio")) {
						String path = sec.getEntryValue("path");
						String loopString = sec.getEntryValue("loop");
						boolean loop = false; 
						if ((loopString != null) && loopString.equalsIgnoreCase("true")) {
							loop = true;
						}
						if (path != null) {
							File f = new File(path);
							if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
								try {
									String name = path + Files.size(f.toPath());
									MenuCustomization.registerSound(name, path);
									this.audio.put(name, loop);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}
					
				}
			}
		}
		
		MenuHandlerRegistry.setActiveHandler(this.getMenuIdentifier());
		
		for (String s : this.oldAudio) {
			if (!this.audio.containsKey(s)) {
				SoundHandler.stopSound(s);
			}
		}
		
		this.oldAudio.clear();
		
		for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
			SoundHandler.playSound(m.getKey());
			if (m.getValue()) {
				SoundHandler.setLooped(m.getKey(), true);
			}
			this.oldAudio.add(m.getKey());
		}
		
		if (!backgroundTextureSet) {
			this.backgroundTexture = null;
		}
	}

	@SubscribeEvent
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (PopupHandler.isPopupActive()) {
			return;
		}
		
		if (!this.shouldCustomize(e.getGui())) {
			return;
		}
		
		if (!this.backgroundDrawable) {
			//Rendering all items which SHOULD be rendered in the background IF it's not possible to render them in the background (In this case, they will be forced to render in the foreground)
			for (CustomizationItemBase i : this.backgroundRenderItems) {
				try {
					i.render(e.getGui());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		//Rendering all items which should be rendered in the foreground
		for (CustomizationItemBase i : this.frontRenderItems) {
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
			if (this.canRenderBackground()) {
				if ((this.backgroundAnimation != null) && this.backgroundAnimation.isReady()) {
					boolean b = this.backgroundAnimation.isStretchedToStreensize();
					this.backgroundAnimation.setStretchImageToScreensize(true);
					this.backgroundAnimation.render();
					this.backgroundAnimation.setStretchImageToScreensize(b);
				} else if (this.backgroundTexture != null) {
					RenderSystem.enableBlend();
					Minecraft.getInstance().getTextureManager().bindTexture(this.backgroundTexture.getResourceLocation());
					
					if (!this.panoramaback) {
						IngameGui.blit(0, 0, 1.0F, 1.0F, e.getGui().width, e.getGui().height, e.getGui().width, e.getGui().height);
					} else {
						int w = this.backgroundTexture.getWidth();
						int h = this.backgroundTexture.getHeight();
						double ratio = (double) w / (double) h;
						int wfinal = (int)(e.getGui().height * ratio);

						//Check if the panorama background should move to the left side or to the ride side
						if ((panoPos + (wfinal - e.getGui().width)) <= 0) {
							panoMoveBack = true;
						}
						if (panoPos >= 0) {
							panoMoveBack = false;
						}

						//Fix pos after resizing
						if (panoPos + (wfinal - e.getGui().width) < 0) {
							panoPos = 0 - (wfinal - e.getGui().width);
						}
						if (panoPos > 0) {
							panoPos = 0;
						}
						
						if (!panoStop) {
							if (panoTick >= 1) {
								panoTick = 0;
								if (panoMoveBack) {
									panoPos = panoPos + 0.5;
								} else {
									panoPos = panoPos - 0.5;
								}
								
								if (panoPos + (wfinal - e.getGui().width) == 0) {
									panoStop = true;
								}
								if (panoPos == 0) {
									panoStop = true;
								}
							} else {
								panoTick++;
							}
						} else {
							if (panoTick >= 300) {
								panoStop = false;
								panoTick = 0;
							} else {
								panoTick++;
							}
						}
						if (wfinal <= e.getGui().width) {
							IngameGui.blit(0, 0, 1.0F, 1.0F, e.getGui().width, e.getGui().height, e.getGui().width, e.getGui().height);
						} else {
//							IngameGui.blit(panoPos, 0, 1.0F, 1.0F, wfinal, e.getGui().height, wfinal, e.getGui().height);
							RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wfinal, e.getGui().height);
						}
					}
					
					RenderSystem.disableBlend();
				}
			}

			if (PopupHandler.isPopupActive()) {
				return;
			}
			
			//Rendering all items which should be rendered in the background
			for (CustomizationItemBase i : this.backgroundRenderItems) {
				try {
					i.render(e.getGui());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			this.backgroundDrawable = true;
		}
	}
	
	private static Widget getButton(String identifier, List<Widget> buttons) {
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
	
	protected boolean shouldCustomize(Screen menu) {
		if (getMenuIdentifier() != null) {
			if (!this.getMenuIdentifier().equals(menu.getClass().getName())) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canRenderBackground() {
		return ((this.backgroundAnimation != null) || (this.backgroundTexture != null));
	}
	
	public boolean setBackgroundAnimation(int id) {
		if (id < this.backgroundAnimations.size()) {
			this.backgroundAnimationId = id;
			this.backgroundAnimation = this.backgroundAnimations.get(id);
			this.lastBackgroundAnimation = this.backgroundAnimation;
			return true;
		}
		return false;
	}
	
	public int getCurrentBackgroundAnimationId() {
		return this.backgroundAnimationId;
	}
	
	public List<IAnimationRenderer> backgroundAnimations() {
		return this.backgroundAnimations;
	}

}
