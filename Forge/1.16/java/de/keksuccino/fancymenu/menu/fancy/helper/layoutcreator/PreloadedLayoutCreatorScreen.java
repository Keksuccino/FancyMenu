package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButtonDummyCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.ButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebStringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;

public class PreloadedLayoutCreatorScreen extends LayoutCreatorScreen {

	public String single;
	private boolean audioInit = false;
	
	public PreloadedLayoutCreatorScreen(Screen screenToCustomize, List<PropertiesSet> properties) {
		super(screenToCustomize);
		
		List<LayoutObject> con = new ArrayList<LayoutObject>();
		Map<Long, LayoutVanillaButton> vanillas = new HashMap<Long, LayoutVanillaButton>();

		if (properties.size() == 1) {
			List<PropertiesSection> l = properties.get(0).getPropertiesOfType("customization-meta");
			if (l.isEmpty()) {
				l = properties.get(0).getPropertiesOfType("type-meta");
			}
			if (!l.isEmpty()) {
				PropertiesSection meta = l.get(0);
				
				this.requiredmods = meta.getEntryValue("requiredmods");
				this.minimumFM = meta.getEntryValue("minimumfmversion");
				this.maximumFM = meta.getEntryValue("maximumfmversion");
				this.minimumMC = meta.getEntryValue("minimummcversion");
				this.maximumMC = meta.getEntryValue("maximummcversion");
				
				String order = meta.getEntryValue("renderorder");
				if ((order != null) && order.equalsIgnoreCase("background")) {
					this.renderorder = "background";
				}

				String biggerthanwidth = meta.getEntryValue("biggerthanwidth");
				if (biggerthanwidth != null) {
					biggerthanwidth = biggerthanwidth.replace(" ", "");
					if (MathUtils.isInteger(biggerthanwidth)) {
						int i = Integer.parseInt(biggerthanwidth);
						this.biggerThanWidth = i;
					}
				}

				String biggerthanheight = meta.getEntryValue("biggerthanheight");
				if (biggerthanheight != null) {
					biggerthanheight = biggerthanheight.replace(" ", "");
					if (MathUtils.isInteger(biggerthanheight)) {
						int i = Integer.parseInt(biggerthanheight);
						this.biggerThanHeight = i;
					}
				}

				String smallerthanwidth = meta.getEntryValue("smallerthanwidth");
				if (smallerthanwidth != null) {
					smallerthanwidth = smallerthanwidth.replace(" ", "");
					if (MathUtils.isInteger(smallerthanwidth)) {
						int i = Integer.parseInt(smallerthanwidth);
						this.smallerThanWidth = i;
					}
				}

				String smallerthanheight = meta.getEntryValue("smallerthanheight");
				if (smallerthanheight != null) {
					smallerthanheight = smallerthanheight.replace(" ", "");
					if (MathUtils.isInteger(smallerthanheight)) {
						int i = Integer.parseInt(smallerthanheight);
						this.smallerThanHeight = i;
					}
				}

				String biggerthan = meta.getEntryValue("biggerthan");
				if ((biggerthan != null) && biggerthan.toLowerCase().contains("x")) {
					String wRaw = biggerthan.replace(" ", "").split("[x]", 2)[0];
					String hRaw = biggerthan.replace(" ", "").split("[x]", 2)[1];
					if (MathUtils.isInteger(wRaw) && MathUtils.isInteger(hRaw)) {
						this.biggerThanWidth = Integer.parseInt(wRaw);
						this.biggerThanHeight = Integer.parseInt(hRaw);
					}
				}

				String smallerthan = meta.getEntryValue("smallerthan");
				if ((smallerthan != null) && smallerthan.toLowerCase().contains("x")) {
					String wRaw = smallerthan.replace(" ", "").split("[x]", 2)[0];
					String hRaw = smallerthan.replace(" ", "").split("[x]", 2)[1];
					if (MathUtils.isInteger(wRaw) && MathUtils.isInteger(hRaw)) {
						this.smallerThanWidth = Integer.parseInt(wRaw);
						this.smallerThanHeight = Integer.parseInt(hRaw);
					}
				}
				
				this.single = meta.getEntryValue("path");
			}
		}
		
		for (PropertiesSet s : properties) {
			for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
				String action = sec.getEntryValue("action");
				if (action != null) {
					String identifier = sec.getEntryValue("identifier");
					ButtonData b = null;
					if (identifier != null) {
						if (identifier.contains("%") && identifier.contains("=")) {
							String id = identifier.split("[=]", 2)[1].replace("%", "").replace(" ", "");
							if (MathUtils.isInteger(id)) {
								b = ButtonCache.getButtonForId(Integer.parseInt(id));
							} else {
								b = ButtonCache.getButtonForKey(identifier);
							}
						} else {
							b = ButtonCache.getButtonForKey(identifier);
						}
					}

					//TODO übernehmen
					if (action.equalsIgnoreCase("setbackgroundpanorama")) {
						String name = sec.getEntryValue("name");
						if (name != null) {
							if (PanoramaHandler.panoramaExists(name)) {
								this.backgroundPanorama = PanoramaHandler.getPanorama(name);
							}
						}
					}
					
					if (action.equalsIgnoreCase("texturizebackground")) {
						String value = sec.getEntryValue("path");
						//TODO übernehmen
						String pano = sec.getEntryValue("wideformat");
						if (pano == null) {
							pano = sec.getEntryValue("panorama");
						}
						//------------
						if (value != null) {
							File f = new File(value.replace("\\", "/"));
							if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
								this.backgroundTexture = TextureHandler.getResource(value);
								this.backgroundTexturePath = value;
								if ((pano != null) && pano.equalsIgnoreCase("true")) {
									this.panorama = true;
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("animatebackground")) {
						String value = sec.getEntryValue("name");
						String random = sec.getEntryValue("random");
						
						if ((random != null) && random.equalsIgnoreCase("true")) {
							this.randomBackgroundAnimation = true;
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
										this.backgroundAnimationNames.add(name);
									}
								}
							} else {
								if (AnimationHandler.animationExists(value)) {
									this.backgroundAnimationNames.add(value);
								}
							}
							if (!this.backgroundAnimationNames.isEmpty()) {
								this.backgroundAnimation = AnimationHandler.getAnimation(this.backgroundAnimationNames.get(0));
							}
						}
					}
					
					if (action.equalsIgnoreCase("hidebutton")) {
						if (b != null) {
							LayoutVanillaButton van;
							if (!vanillas.containsKey(b.getId())) {
								van = new LayoutVanillaButton(b, this);
								vanillas.put(b.getId(), van);
								con.add(van);
							} else {
								van = vanillas.get(b.getId());
							}
							this.hidden.add(van);
							van.hidden = true;
						}
					}

					if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
						String value = sec.getEntryValue("value");
						if ((value != null) && (b != null)) {
							if (!vanillas.containsKey(b.getId())) {
								LayoutVanillaButton van = new LayoutVanillaButton(b, this);
								vanillas.put(b.getId(), van);
								con.add(van);
							}
							vanillas.get(b.getId()).object.value = value;
							this.vanillaButtonNames.put(b.getId(), value);
						}
					}
					
					if (action.equalsIgnoreCase("resizebutton")) {
						String width = sec.getEntryValue("width");
						String height = sec.getEntryValue("height");
						if ((width != null) && (height != null) && MathUtils.isInteger(width) && MathUtils.isInteger(height) && (b != null)) {
							int w = Integer.parseInt(width);
							int h = Integer.parseInt(height);
							if (!vanillas.containsKey(b.getId())) {
								LayoutVanillaButton van = new LayoutVanillaButton(b, this);
								vanillas.put(b.getId(), van);
								con.add(van);
							}
							vanillas.get(b.getId()).object.width = w;
							vanillas.get(b.getId()).object.height = h;
						}
					}
					
					if (action.equalsIgnoreCase("movebutton")) {
						String posX = sec.getEntryValue("x");
						String posY = sec.getEntryValue("y");
						String orientation = sec.getEntryValue("orientation");
						
						if ((orientation != null) && (posX != null) && (posY != null) && MathUtils.isInteger(posX) && MathUtils.isInteger(posY) && (b != null)) {
							int x = Integer.parseInt(posX);
							int y = Integer.parseInt(posY);
							if (!vanillas.containsKey(b.getId())) {
								LayoutVanillaButton van = new LayoutVanillaButton(b, this);
								vanillas.put(b.getId(), van);
								con.add(van);
							}
							vanillas.get(b.getId()).object.orientation = orientation;
							vanillas.get(b.getId()).object.posX = x;
							vanillas.get(b.getId()).object.posY = y;
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
									if (!vanillas.containsKey(b.getId())) {
										LayoutVanillaButton van = new LayoutVanillaButton(b, this);
										vanillas.put(b.getId(), van);
										con.add(van);
									}
									vanillas.get(b.getId()).backNormal = backNormal;
									vanillas.get(b.getId()).backHovered = backHover;

									((LayoutButtonDummyCustomizationItem)vanillas.get(b.getId()).object).setTexture(TextureHandler.getResource(backNormal).getResourceLocation());
									
									List<String> l = new ArrayList<String>();
									l.add(backNormal);
									l.add(backHover);
									
									this.vanillaButtonTextures.put(b.getId(), l);
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("addhoversound")) {
						if (b != null) {
							String path = sec.getEntryValue("path");
							if (path != null) {
								File f = new File(path);
								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
									if (!vanillas.containsKey(b.getId())) {
										LayoutVanillaButton van = new LayoutVanillaButton(b, this);
										vanillas.put(b.getId(), van);
										con.add(van);
									}
									vanillas.get(b.getId()).hoverSound = path;
									this.vanillaHoverSounds.put(b.getId(), path);
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("sethoverlabel")) {
						if (b != null) {
							String label = sec.getEntryValue("label");
							if (label != null) {
								if (!vanillas.containsKey(b.getId())) {
									LayoutVanillaButton van = new LayoutVanillaButton(b, this);
									vanillas.put(b.getId(), van);
									con.add(van);
								}
								vanillas.get(b.getId()).hoverLabel = label;
								this.vanillaHoverLabels.put(b.getId(), label);
							}
						}
					}

					if (action.equalsIgnoreCase("setbuttonclicksound")) {
						if (b != null) {
							String path = sec.getEntryValue("path");
							if (path != null) {
								File f = new File(path);
								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
									if (!vanillas.containsKey(b.getId())) {
										LayoutVanillaButton van = new LayoutVanillaButton(b, this);
										vanillas.put(b.getId(), van);
										con.add(van);
									}
									vanillas.get(b.getId()).clicksound = path;
									this.vanillaClickSounds.put(b.getId(), path);
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("clickbutton")) {
						if (b != null) {
							String clicks = sec.getEntryValue("clicks");
							if ((clicks != null) && (MathUtils.isInteger(clicks))) {
								if (!vanillas.containsKey(b.getId())) {
									LayoutVanillaButton van = new LayoutVanillaButton(b, this);
									vanillas.put(b.getId(), van);
									con.add(van);
								}
								int i = Integer.parseInt(clicks);
								vanillas.get(b.getId()).clicks = i;
								
								if (i > 0) {
									this.vanillaButtonClicks.put(b.getId(), i);
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("hidebuttonfor")) {
						if (b != null) {
							String seconds = sec.getEntryValue("seconds");
							String firsttime = sec.getEntryValue("onlyfirsttime");
							if ((seconds != null) && (MathUtils.isDouble(seconds))) {
								if (!vanillas.containsKey(b.getId())) {
									LayoutVanillaButton van = new LayoutVanillaButton(b, this);
									vanillas.put(b.getId(), van);
									con.add(van);
								}
								double d = Double.parseDouble(seconds);
								vanillas.get(b.getId()).hideforsec = d;
								
								this.vanillaHideFor.put(b.getId(), d);

								if ((firsttime != null) && firsttime.equalsIgnoreCase("true")) {
									vanillas.get(b.getId()).delayonlyfirsttime = true;
									this.vanillaDelayOnlyFirstTime.put(b.getId(), true);
								}
							}
						}
					}
					
					if (action.equalsIgnoreCase("addtext")) {
						con.add(new LayoutString(new StringCustomizationItem(sec), this));
					}
					
					if (action.equalsIgnoreCase("addwebtext")) {
						con.add(new LayoutWebString(new WebStringCustomizationItem(sec), this));
					}

					if (action.equalsIgnoreCase("addtexture")) {
						LayoutTexture o = new LayoutTexture(new TextureCustomizationItem(sec), this);
						int i = isObjectStretched(sec);
						if (i == 3) {
							o.setStretchedX(true, false);
							o.setStretchedY(true, false);
						}
						if (i == 2) {
							o.setStretchedY(true, false);
						}
						if (i == 1) {
							o.setStretchedX(true, false);
						}
						con.add(o);
					}

					if (action.equalsIgnoreCase("addwebtexture")) {
						LayoutWebTexture o = new LayoutWebTexture(new WebTextureCustomizationItem(sec), this);
						int i = isObjectStretched(sec);
						if (i == 3) {
							o.setStretchedX(true, false);
							o.setStretchedY(true, false);
						}
						if (i == 2) {
							o.setStretchedY(true, false);
						}
						if (i == 1) {
							o.setStretchedX(true, false);
						}
						con.add(o);
					}

					if (action.equalsIgnoreCase("addanimation")) {
						LayoutAnimation o = new LayoutAnimation(new AnimationCustomizationItem(sec), this);
						int i = isObjectStretched(sec);
						if (i == 3) {
							o.setStretchedX(true, false);
							o.setStretchedY(true, false);
						}
						if (i == 2) {
							o.setStretchedY(true, false);
						}
						if (i == 1) {
							o.setStretchedX(true, false);
						}
						con.add(o);
					}
					
					if (action.equalsIgnoreCase("addbutton")) {
						ButtonCustomizationItem bc = new ButtonCustomizationItem(sec);
						
						String baction = sec.getEntryValue("buttonaction");
						String actionvalue = sec.getEntryValue("value");
						String backNormal = sec.getEntryValue("backgroundnormal");
						String backHover = sec.getEntryValue("backgroundhovered");
						String hoverLabel = sec.getEntryValue("hoverlabel");
						String hoverSound = sec.getEntryValue("hoversound");
						String hidefor = sec.getEntryValue("hideforseconds");
						String firsttime = sec.getEntryValue("delayonlyfirsttime");
						String onlydisplayin = sec.getEntryValue("onlydisplayin");
						String clicksound = sec.getEntryValue("clicksound");
						//TODO übernehmen
						String desc = sec.getEntryValue("description");

						if (onlydisplayin != null) {
							if (onlydisplayin.equalsIgnoreCase("outgame")) {
								onlydisplayin = "outgame";
							} else if (onlydisplayin.equalsIgnoreCase("singleplayer")) {
								onlydisplayin = "singleplayer";
							} else if (onlydisplayin.equalsIgnoreCase("multiplayer")) {
								onlydisplayin = "multiplayer";
							} else {
								onlydisplayin = null;
							}
						}

						LayoutButton lb = new LayoutButton(bc.width, bc.height, bc.value, onlydisplayin, this);

						int i = isObjectStretched(sec);
						if (i == 3) {
							lb.setStretchedX(true, false);
							lb.setStretchedY(true, false);
						}
						if (i == 2) {
							lb.setStretchedY(true, false);
						}
						if (i == 1) {
							lb.setStretchedX(true, false);
						}
						
						if (baction == null) {
							continue;
						}
						lb.actionType = baction;
						
						if (actionvalue == null) {
							actionvalue = "";
						}
						lb.actionContent = actionvalue;
						
						if ((hidefor != null) && MathUtils.isDouble(hidefor)) {
							lb.hideforsec = Double.parseDouble(hidefor);
							if ((firsttime != null) && firsttime.equalsIgnoreCase("true")) {
								lb.delayonlyfirsttime = true;
							}
						}
						
						//TODO übernehmen
						if (desc != null) {
							lb.description = desc;
						}
						
						if ((backNormal != null) && (backHover != null)) {
							lb.backNormal = backNormal.replace("\\", "/");
							lb.backHovered = backHover.replace("\\", "/");
							ExternalTextureResourceLocation r = TextureHandler.getResource(lb.backNormal);
							ResourceLocation l = null;
							if (r != null) {
								l = r.getResourceLocation();
							}
							if (l != null) {
								((LayoutButtonDummyCustomizationItem)lb.object).setTexture(l);
							}
						}
						
						if (hoverSound != null) {
							lb.hoverSound = hoverSound.replace("\\", "/");
						}
						
						if (hoverLabel != null) {
							lb.hoverLabel = hoverLabel;
						}

						if (clicksound != null) {
							lb.clicksound = clicksound.replace("\\", "/");
						}

						lb.object.orientation = bc.orientation;
						lb.object.posX = bc.posX;
						lb.object.posY = bc.posY;
						
						con.add(lb);
					}
					
					if (action.equalsIgnoreCase("addaudio")) {
						String path = sec.getEntryValue("path");
						String loopString = sec.getEntryValue("loop");
						boolean loop = false; 
						if ((loopString != null) && loopString.equalsIgnoreCase("true")) {
							loop = true;
						}
						if (path != null) {
							File home = new File("");
							if (path.startsWith(home.getAbsolutePath())) {
								path = path.replace(home.getAbsolutePath(), "");
								if (path.startsWith("\\") || path.startsWith("/")) {
									path = path.substring(1);
								}
							}
							File f = new File(path);
							if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
								try {
									this.addAudio(path);
									if (loop) {
										SoundHandler.setLooped(path, true);
										this.audio.put(path, true);
									}
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}

					//TODO übernehmen
					if (action.equalsIgnoreCase("setscale")) {
						String scale = sec.getEntryValue("scale");
						if ((scale != null) && (MathUtils.isInteger(scale) || MathUtils.isDouble(scale))) {
							int sc = (int) Double.parseDouble(scale);
							if (sc >= 0) {
								this.scale = sc;
							}
						}
					}
					
				}
			}
		}
		
		this.content.addAll(con);
	}
	
	//init
	@Override
	protected void init() {
		super.init();
		
		if (this.renderorder.equals("background")) {
			this.renderorderBackgroundButton.setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.background"));
			this.renderorderForegroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
		}
		
		if (this.single != null) {
			this.saveButton = new AdvancedButton(17, (this.height / 2) + 22, 40, 40, Locals.localize("helper.creator.menu.save"), true, (onPress) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new EditSingleLayoutSavePopup(this::saveEditedCustomizationFileCallback));
			});
			LayoutCreatorScreen.colorizeCreatorButton(saveButton);
		}
	}
	
	//render
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		
		if (!audioInit) {
			audioInit = true;
			for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
				SoundHandler.playSound(m.getKey());
			}
		}
		
		super.render(matrix, mouseX, mouseY, partialTicks);
	}
	
	private void saveEditedCustomizationFileCallback(Integer i) {
		if (i == 3) {
			this.disableLayouts();
		}
		if (i == 1) {
			if (this.single != null) {
				try {
					File f = new File(this.single);
					if (f.exists() && f.isFile()) {
						f.delete();
					}
					
					this.saveToCustomizationFile(f.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if ((i == 2) || (i == 3)) {
			try {
				String name = this.getScreenToCustomizeIdentifier();
				if (name.contains(".")) {
					name = new StringBuilder(new StringBuilder(name).reverse().toString().split("[.]", 2)[0]).reverse().toString();
				}
				this.saveToCustomizationFile(this.generateCustomizationFileName(FancyMenu.getCustomizationPath().getPath(), name));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.setMenusUseable(true);
	}

	/**
	 * Returns:<br>
	 * 0 for FALSE<br>
	 * 1 for HORIZONTALLY STRETCHED<br>
	 * 2 for VERTICALLY STRETCHED<br>
	 * 3 for BOTH
	 */
	public static int isObjectStretched(PropertiesSection sec) {
		String w = sec.getEntryValue("width");
		String h = sec.getEntryValue("height");
		String x = sec.getEntryValue("x");
		String y = sec.getEntryValue("y");
		
		boolean stretchX = false;
		if ((w != null) && (x != null)) {
			if (w.equals("%guiwidth%") && x.equals("0")) {
				stretchX = true;
			}
		}
		boolean stretchY = false;
		if ((h != null) && (y != null)) {
			if (h.equals("%guiheight%") && y.equals("0")) {
				stretchY = true;
			}
		}
		
		if (stretchX && stretchY) {
			return 3;
		}
		if (stretchY) {
			return 2;
		}
		if (stretchX) {
			return 1;
		}
		
		return 0;
	}

}
