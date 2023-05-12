package de.keksuccino.fancymenu.customization.layout.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundType;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.customization.deepcustomization.*;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.v1.*;
import de.keksuccino.fancymenu.customization.element.v1.button.ButtonCustomizationItem;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutAnimation;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutShape;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutSlideshow;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutSplashText;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutString;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutTexture;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutWebString;
import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutWebTexture;
import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutButton;
import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.customization.element.v1.ShapeCustomizationItem.Shape;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class PreloadedLayoutEditorScreen extends LayoutEditorScreen {

	public String single;
	private boolean audioInit = false;

	protected List<PropertiesSet> cachedProperties;
	protected boolean isPreLoadedInitialized = false;

	public PreloadedLayoutEditorScreen(Screen screenToCustomize, List<PropertiesSet> properties) {
		super(screenToCustomize);
		this.cachedProperties = properties;
	}

	@Override
	protected void init() {
		super.init();
		if (!this.isPreLoadedInitialized) {
			this.isPreLoadedInitialized = true;
			this.initPreLoaded();
		}
	}

	protected void initPreLoaded() {

		List<AbstractEditorElement> con = new ArrayList<AbstractEditorElement>();

		if (this.cachedProperties.size() == 1) {
			List<PropertiesSection> l = this.cachedProperties.get(0).getPropertiesOfType("customization-meta");
			if (l.isEmpty()) {
				l = this.cachedProperties.get(0).getPropertiesOfType("type-meta");
			}
			if (!l.isEmpty()) {
				PropertiesSection meta = l.get(0);

				this.requiredmods = meta.getEntryValue("requiredmods");
				this.minimumFM = meta.getEntryValue("minimumfmversion");
				this.maximumFM = meta.getEntryValue("maximumfmversion");
				this.minimumMC = meta.getEntryValue("minimummcversion");
				this.maximumMC = meta.getEntryValue("maximummcversion");

				this.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(meta);

				this.customMenuTitle = meta.getEntryValue("custom_menu_title");

				String ulWhitelist = meta.getEntryValue("universal_layout_whitelist");
				if ((ulWhitelist != null) && ulWhitelist.contains(";")) {
					this.universalLayoutWhitelist.addAll(Arrays.asList(ulWhitelist.split("[;]")));
				}
				String ulBlacklist = meta.getEntryValue("universal_layout_blacklist");
				if ((ulBlacklist != null) && ulBlacklist.contains(";")) {
					this.universalLayoutBlacklist.addAll(Arrays.asList(ulBlacklist.split("[;]")));
				}

				String ranMode = meta.getEntryValue("randommode");
				if ((ranMode != null) && ranMode.equalsIgnoreCase("true")) {
					this.randomMode = true;
				}
				String ranModeGroup = meta.getEntryValue("randomgroup");
				if ((ranModeGroup != null) && MathUtils.isInteger(ranModeGroup)) {
					this.randomGroup = ranModeGroup;
				}
				String ranModeFirstTime = meta.getEntryValue("randomonlyfirsttime");
				if ((ranModeFirstTime != null) && ranModeFirstTime.equalsIgnoreCase("true")) {
					this.randomOnlyFirstTime = true;
				}

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

				String filePath = meta.getEntryValue("path");
				if (filePath != null) {
					this.single = filePath;
				}
			}
		}

		List<PropertiesSection> deepCustomizationSecs = new ArrayList<>();

		for (PropertiesSet s : this.cachedProperties) {
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
							} else if (id.startsWith("button_compatibility_id:")) {
								b = ButtonCache.getButtonForCompatibilityId(id);
							} else {
								b = ButtonCache.getButtonForKey(identifier);
							}
						} else {
							b = ButtonCache.getButtonForKey(identifier);
						}
					}

					if (action.equalsIgnoreCase("backgroundoptions")) {
						String keepAspect = sec.getEntryValue("keepaspectratio");
						if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
							this.keepBackgroundAspectRatio = true;
						}
					}

					if (action.equalsIgnoreCase("setbackgroundpanorama")) {
						String name = sec.getEntryValue("name");
						if (name != null) {
							if (PanoramaHandler.panoramaExists(name)) {
								this.backgroundPanorama = PanoramaHandler.getPanorama(name);
							}
						}
					}

					if (action.equalsIgnoreCase("setbackgroundslideshow")) {
						String name = sec.getEntryValue("name");
						if (name != null) {
							if (SlideshowHandler.slideshowExists(name)) {
								this.backgroundSlideshow = SlideshowHandler.getSlideshow(name);
							}
						}
					}

					if (action.equalsIgnoreCase("texturizebackground")) {
						String value = sec.getEntryValue("path");
						String pano = sec.getEntryValue("wideformat");
						if (pano == null) {
							pano = sec.getEntryValue("panorama");
						}
						if (value != null) {
							value = value.replace("\\", "/");
							String valueFinal = value;
							File f = new File(value);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								valueFinal = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + value;
								f = new File(valueFinal);
							}
							if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
								this.backgroundTexture = ExternalTextureHandler.INSTANCE.getTexture(valueFinal);
								this.backgroundTexturePath = value;
								if ((pano != null) && pano.equalsIgnoreCase("true")) {
									this.panorama = true;
								}
							}
						}
					}

					//Custom background handling (API)
					if (action.equalsIgnoreCase("api:custombackground")) {
						String typeId = sec.getEntryValue("type_identifier");
						String backId = sec.getEntryValue("background_identifier");
						String inputString = sec.getEntryValue("input_string");
						if (typeId != null) {
							MenuBackgroundType type = MenuBackgroundTypeRegistry.getBackgroundTypeByIdentifier(typeId);
							if (type != null) {
								if (type.needsInputString() && (inputString != null)) {
									try {
										this.customMenuBackground = type.createInstanceFromInputString(inputString);
										this.customMenuBackgroundInputString = inputString;
									} catch (Exception ex) {
										ex.printStackTrace();
									}
									if (this.customMenuBackground != null) {
										this.customMenuBackground.onOpenMenu();
									}
								} else if (backId != null) {
									this.customMenuBackground = type.getBackgroundByIdentifier(backId);
									if (this.customMenuBackground != null) {
										this.customMenuBackground.onOpenMenu();
									}
								}
							}
						}
					}

					if (action.equalsIgnoreCase("animatebackground")) {
						String value = sec.getEntryValue("name");
						String random = sec.getEntryValue("random");

						String restartOnLoadString = sec.getEntryValue("restart_on_load");
						if ((restartOnLoadString != null) && restartOnLoadString.equalsIgnoreCase("true")) {
							this.restartAnimationBackgroundOnLoad = true;
						}

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
							LayoutVanillaButton van = this.getVanillaButton(b);
							van.customizationContainer.isButtonHidden = true;
						}
					}

					if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
						String value = sec.getEntryValue("value");
						if ((value != null) && (b != null)) {
							LayoutVanillaButton van = this.getVanillaButton(b);
							van.customizationContainer.customButtonLabel = value;
						}
					}

					if (action.equalsIgnoreCase("resizebutton")) {
						String width = sec.getEntryValue("width");
						String height = sec.getEntryValue("height");
						if ((width != null) && (height != null) && MathUtils.isInteger(width) && MathUtils.isInteger(height) && (b != null)) {
							LayoutVanillaButton van = this.getVanillaButton(b);
							int w = Integer.parseInt(width);
							int h = Integer.parseInt(height);
							van.element.setWidth(w);
							van.element.setHeight(h);
							van.element.advancedWidth = sec.getEntryValue("advanced_width");
							van.element.advancedHeight = sec.getEntryValue("advanced_height");
						}
					}

					if (action.equalsIgnoreCase("movebutton")) {
						String posX = sec.getEntryValue("x");
						String posY = sec.getEntryValue("y");
						String orientation = sec.getEntryValue("orientation");

						if ((orientation != null) && (posX != null) && (posY != null) && MathUtils.isInteger(posX) && MathUtils.isInteger(posY) && (b != null)) {
							LayoutVanillaButton van = this.getVanillaButton(b);
							int x = Integer.parseInt(posX);
							int y = Integer.parseInt(posY);
							van.element.anchorPoint = orientation;
							van.element.baseX = x;
							van.element.baseY = y;
							van.element.advancedX = sec.getEntryValue("advanced_posx");
							van.element.advancedY = sec.getEntryValue("advanced_posy");
							van.element.anchorPointElementIdentifier = sec.getEntryValue("orientation_element");
						}
					}

					if (action.equalsIgnoreCase("setbuttontexture")) {
						if (b != null) {
							LayoutVanillaButton van = this.getVanillaButton(b);
							String backNormal = sec.getEntryValue("backgroundnormal");
							String backHover = sec.getEntryValue("backgroundhovered");
							String backAniNormal = sec.getEntryValue("backgroundanimationnormal");
							String backAniHover = sec.getEntryValue("backgroundanimationhovered");
							String loopBackAnimations = sec.getEntryValue("loopbackgroundanimations");
							String restartBackAnimationsOnHover = sec.getEntryValue("restartbackgroundanimations");
							if ((backNormal != null) || (backHover != null) || (backAniNormal != null) || (backAniHover != null)) {
								if (backNormal != null) {
									File f = new File(backNormal.replace("\\", "/"));
									if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
										f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backNormal.replace("\\", "/"));
									}
									if (f.isFile()) {
										van.customizationContainer.normalBackground = backNormal;
									}
								} else if (backAniNormal != null) {
									if (AnimationHandler.animationExists(backAniNormal)) {
										van.customizationContainer.normalBackground = "animation:" + backAniNormal;
									}
								}
								if (backHover != null) {
									File f = new File(backHover.replace("\\", "/"));
									if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
										f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backHover.replace("\\", "/"));
									}
									if (f.isFile()) {
										van.customizationContainer.hoverBackground = backHover;
									}
								} else if (backAniHover != null) {
									if (AnimationHandler.animationExists(backAniHover)) {
										van.customizationContainer.hoverBackground = "animation:" + backAniHover;
									}
								}
							}
							if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
								van.customizationContainer.loopAnimation = false;
							}
							if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
								van.customizationContainer.restartAnimationOnHover = false;
							}
						}
					}

					if (action.equalsIgnoreCase("addhoversound")) {
						if (b != null) {
							String path = sec.getEntryValue("path");
							if (path != null) {
								File f = new File(path);
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path);
								}
								if (f.isFile() && f.getName().endsWith(".wav")) {
									LayoutVanillaButton van = this.getVanillaButton(b);
									van.customizationContainer.hoverSound = path;
								}
							}
						}
					}

					if (action.equalsIgnoreCase("sethoverlabel")) {
						if (b != null) {
							String label = sec.getEntryValue("label");
							if (label != null) {
								LayoutVanillaButton van = this.getVanillaButton(b);
								van.customizationContainer.hoverLabel = label;
							}
						}
					}

					if (action.equalsIgnoreCase("setbuttonclicksound")) {
						if (b != null) {
							String path = sec.getEntryValue("path");
							if (path != null) {
								File f = new File(path);
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path);
								}
								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
									LayoutVanillaButton van = this.getVanillaButton(b);
									van.customizationContainer.clickSound = path;
								}
							}
						}
					}

					if (action.equalsIgnoreCase("clickbutton")) {
						if (b != null) {
							String clicks = sec.getEntryValue("clicks");
							if ((clicks != null) && (MathUtils.isInteger(clicks))) {
								LayoutVanillaButton van = this.getVanillaButton(b);
								int i = Integer.parseInt(clicks);
								van.customizationContainer.autoButtonClicks = i;
							}
						}
					}

					if (action.equalsIgnoreCase("hidebuttonfor")) {
						if (b != null) {
							String seconds = sec.getEntryValue("seconds");
							String firsttime = sec.getEntryValue("onlyfirsttime");
							String fade = sec.getEntryValue("fadein");
							String fadespeed = sec.getEntryValue("fadeinspeed");
							if ((seconds != null) && (MathUtils.isFloat(seconds))) {
								LayoutVanillaButton van = this.getVanillaButton(b);

								van.element.delayAppearance = true;

								float ds = Float.parseFloat(seconds);
								van.element.appearanceDelayInSeconds = ds;
								this.vanillaDelayAppearance.put(b.getId(), ds);

								if ((firsttime != null) && firsttime.equalsIgnoreCase("true")) {
									van.element.delayAppearanceEverytime = false;
									this.vanillaDelayAppearanceFirstTime.put(b.getId(), true);
								} else {
									van.element.delayAppearanceEverytime = true;
									this.vanillaDelayAppearanceFirstTime.put(b.getId(), false);
								}

								if ((fade != null && fade.equalsIgnoreCase("true"))) {
									float fs = 1.0F;
									if ((fadespeed != null) && MathUtils.isFloat(fadespeed)) {
										fs = Float.parseFloat(fadespeed);
									}
									van.element.fadeIn = true;
									van.element.fadeInSpeed = fs;
									this.vanillaFadeIn.put(b.getId(), fs);
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

						String onlydisplayin = sec.getEntryValue("onlydisplayin");

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

						LayoutButton lb = new LayoutButton(new ScreenCustomizationLayer.ButtonCustomizationContainer(), bc.getWidth(), bc.getHeight(), bc.value, onlydisplayin, this) {
							@Override
							public void init() {

								this.element.delayAppearance = bc.delayAppearance;
								this.element.delayAppearanceEverytime = bc.delayAppearanceEverytime;
								this.element.appearanceDelayInSeconds = bc.appearanceDelayInSeconds;
								this.element.fadeIn = bc.fadeIn;
								this.element.fadeInSpeed = bc.fadeInSpeed;
								this.element.loadingRequirementContainer = bc.loadingRequirementContainer;
								this.element.setInstanceIdentifier(bc.getInstanceIdentifier());

								super.init();
							}
						};

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

						lb.actions = bc.actions;

						String desc = sec.getEntryValue("description");
						if (desc != null) {
							lb.customizationContainer.buttonDescription = desc;
						}

						String backNormal = sec.getEntryValue("backgroundnormal");
						String backHover = sec.getEntryValue("backgroundhovered");
						String backAniNormal = sec.getEntryValue("backgroundanimationnormal");
						String backAniHover = sec.getEntryValue("backgroundanimationhovered");
						String loopBackAnimations = sec.getEntryValue("loopbackgroundanimations");
						String restartBackAnimationsOnHover = sec.getEntryValue("restartbackgroundanimations");
						if ((backNormal != null) || (backHover != null) || (backAniNormal != null) || (backAniHover != null)) {
							if (backNormal != null) {
								File f = new File(backNormal.replace("\\", "/"));
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backNormal.replace("\\", "/"));
								}
								if (f.isFile()) {
									lb.customizationContainer.normalBackground = backNormal;
								}
							} else if (backAniNormal != null) {
								if (AnimationHandler.animationExists(backAniNormal)) {
									lb.customizationContainer.normalBackground = "animation:" + backAniNormal;
								}
							}
							if (backHover != null) {
								File f = new File(backHover.replace("\\", "/"));
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backHover.replace("\\", "/"));
								}
								if (f.isFile()) {
									lb.customizationContainer.hoverBackground = backHover;
								}
							} else if (backAniHover != null) {
								if (AnimationHandler.animationExists(backAniHover)) {
									lb.customizationContainer.hoverBackground = "animation:" + backAniHover;
								}
							}
						}
						if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
							lb.customizationContainer.loopAnimation = false;
						}
						if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
							lb.customizationContainer.restartAnimationOnHover = false;
						}

						String hoverSound = sec.getEntryValue("hoversound");
						if (hoverSound != null) {
							lb.customizationContainer.hoverSound = hoverSound.replace("\\", "/");
						}

						String hoverLabel = sec.getEntryValue("hoverlabel");
						if (hoverLabel != null) {
							lb.customizationContainer.hoverLabel = hoverLabel;
						}

						String clicksound = sec.getEntryValue("clicksound");
						if (clicksound != null) {
							lb.customizationContainer.clickSound = clicksound.replace("\\", "/");
						}

						lb.element.anchorPoint = bc.anchorPoint;
						lb.element.anchorPointElementIdentifier = bc.anchorPointElementIdentifier;
						lb.element.baseX = bc.baseX;
						lb.element.baseY = bc.baseY;

						lb.element.advancedX = bc.advancedX;
						lb.element.advancedY = bc.advancedY;
						lb.element.advancedWidth = bc.advancedWidth;
						lb.element.advancedHeight = bc.advancedHeight;

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
							File home = Minecraft.getInstance().gameDirectory;
							if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
								path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
								if (path.startsWith("\\") || path.startsWith("/")) {
									path = path.substring(1);
								}
							}
							File f = new File(Minecraft.getInstance().gameDirectory, path);
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

					if (action.equalsIgnoreCase("setscale")) {
						String scale = sec.getEntryValue("scale");
						if ((scale != null) && (MathUtils.isInteger(scale) || MathUtils.isDouble(scale))) {
							int sc = (int) Double.parseDouble(scale);
							if (sc >= 0) {
								this.scale = sc;
							}
						}
					}

					if (action.equalsIgnoreCase("autoscale")) {
						String w = sec.getEntryValue("basewidth");
						String h = sec.getEntryValue("baseheight");
						if ((w != null) && (h != null) && MathUtils.isInteger(w) && MathUtils.isInteger(h)) {
							int w2 = Integer.parseInt(w);
							int h2 = Integer.parseInt(h);
							if ((w2 > 0) && (h2 > 0)) {
								this.autoScalingWidth = w2;
								this.autoScalingHeight = h2;
							}
						}
					}

					if (action.equalsIgnoreCase("setopenaudio")) {
						String path = sec.getEntryValue("path");
						if (path != null) {
							File f = new File(path);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path);
							}
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								this.openAudio = path;
							}
						}
					}

					if (action.equalsIgnoreCase("setcloseaudio")) {
						String path = sec.getEntryValue("path");
						if (path != null) {
							File f = new File(path);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path);
							}
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								this.closeAudio = path;
							}
						}
					}

					if (action.equalsIgnoreCase("setbuttondescription")) {
						if (b != null) {
							String desc = sec.getEntryValue("description");
							if (desc != null) {
								LayoutVanillaButton van = this.getVanillaButton(b);
								van.customizationContainer.buttonDescription = desc;
							}
						}
					}

					if (action.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
						if (b != null) {
							LayoutVanillaButton van = this.getVanillaButton(b);
							if (van != null) {
								AbstractElement cusItem = new AbstractElement(sec) {
									@Override public void render(PoseStack matrix, Screen menu) {}
								};
								van.element.loadingRequirementContainer = cusItem.loadingRequirementContainer;
								van.customizationContainer.loadingRequirementContainer = cusItem.loadingRequirementContainer;
							}
						}
					}

					if (action.equalsIgnoreCase("addslideshow")) {
						String name = sec.getEntryValue("name");
						if (name != null) {
							if (SlideshowHandler.slideshowExists(name)) {
								LayoutSlideshow ls = new LayoutSlideshow(new SlideshowCustomizationItem(sec), this);
								int i = isObjectStretched(sec);
								if (i == 3) {
									ls.setStretchedX(true, false);
									ls.setStretchedY(true, false);
								}
								if (i == 2) {
									ls.setStretchedY(true, false);
								}
								if (i == 1) {
									ls.setStretchedX(true, false);
								}
								con.add(ls);
							}
						}
					}

					if (action.equalsIgnoreCase("addshape")) {
						String shape = sec.getEntryValue("shape");
						if (shape != null) {
							Shape sh = Shape.byName(shape);
							if (sh != null) {
								LayoutShape ls = new LayoutShape(new ShapeCustomizationItem(sec), this);
								int i = isObjectStretched(sec);
								if (i == 3) {
									ls.setStretchedX(true, false);
									ls.setStretchedY(true, false);
								}
								if (i == 2) {
									ls.setStretchedY(true, false);
								}
								if (i == 1) {
									ls.setStretchedX(true, false);
								}
								con.add(ls);
							}
						}
					}

					if (action.equalsIgnoreCase("addsplash")) {
						con.add(new LayoutSplashText(new SplashTextCustomizationItem(sec), this));
					}

					if (action.startsWith("deep_customization_element:")) {
						deepCustomizationSecs.add(sec);
					}

					/** CUSTOM ITEMS (API) **/
					if (action.startsWith("custom_layout_element:")) {
						String cusId = action.split("[:]", 2)[1];
						ElementBuilder cusItem = ElementRegistry.getBuilder(cusId);
						if (cusItem != null) {
							CustomizationItem cusItemInstance = cusItem.deserializeElement(sec);
							con.add(cusItem.wrapIntoEditorElement(cusItemInstance, this));
						}
					}

				}
			}
		}

		DeepCustomizationLayer layer = DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.screen.getClass().getName());
		if (layer != null) {
			List<DeepCustomizationElement> addedDeeps = new ArrayList<>();
			for (PropertiesSection sec : deepCustomizationSecs) {
				String action = sec.getEntryValue("action");
				String elementId = action.split(":", 2)[1];
				DeepCustomizationElement e = layer.getElementByIdentifier(elementId);
				if (e != null) {
					DeepCustomizationItem i = e.constructCustomizedItemInstance(sec);
					DeepCustomizationLayoutEditorElement le = e.constructEditorElementInstance(i, this);
					this.content.add(le);
					addedDeeps.add(e);
				}
			}
			for (DeepCustomizationElement e : layer.getElementsList()) {
				if (!addedDeeps.contains(e)) {
					this.content.add(e.constructEditorElementInstance(e.constructDefaultItemInstance(), this));
				}
			}
		}

		this.content.addAll(con);

		for (AbstractEditorElement e : this.content) {
			if (e.element.anchorPointElementIdentifier != null) {
				AbstractEditorElement oe = this.getElementByInstanceIdentifier(e.element.anchorPointElementIdentifier);
				if (oe != null) {
					e.element.anchorPointElement = oe.element;
				}
			}
		}

		this.init();

	}

	protected LayoutVanillaButton getVanillaButton(ButtonData b) {
		ScreenCustomizationLayer.ButtonCustomizationContainer cc = this.vanillaButtonCustomizationContainers.get(b.getId());
		if (cc != null) {
			for (AbstractEditorElement e : this.vanillaButtonContent) {
				if (e instanceof LayoutVanillaButton) {
					if (((LayoutVanillaButton) e).customizationContainer == cc) {
						return (LayoutVanillaButton) e;
					}
				}
			}
		}
		System.out.println("[FANCYMENU] ERROR: PRE-LOADED EDITOR: VANILLA BUTTON NOT FOUND!");
		return null;
	}

	//render
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

		if (!audioInit) {
			audioInit = true;
			for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
				SoundHandler.playSound(m.getKey());
			}
		}

		super.render(matrix, mouseX, mouseY, partialTicks);
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