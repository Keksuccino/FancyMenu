package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.menu.placeholder.v1.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.PlaceholderInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.item.playerentity.PlayerEntityCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.playerentity.PlayerEntityCustomizationItem.MenuPlayerEntity;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.web.WebUtils;

public class LayoutPlayerEntity extends LayoutElement {
	
	public String skinUrl;
	public String skinPath;
	public String capeUrl;
	public String capePath;
	
	public boolean isCLientPlayerName = false;
	
	public LayoutPlayerEntity(PlayerEntityCustomizationItem parent, LayoutEditorScreen handler) {
		super(parent, true, handler);
		this.setScale(parent.scale);
	}
	
	@Override
	public void init() {

		this.fadeable = false;
		this.supportsAdvancedSizing = false;
		
		super.init();
		
		this.rightclickMenu.setAutoclose(true);
		
		AdvancedButton scaleB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.setscale"), true, (press) -> {

			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.playerentity.setscale") + ":", CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						if (MathUtils.isInteger(call)) {
							int i = Integer.parseInt(call);
							
							if (i != this.getObject().scale) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.getObject().scale = i;
						}
					} else {
						if (30 != this.getObject().scale) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.getObject().scale = 30;
					}
				}

			});
			
			t.setText("" + this.getObject().scale);
			
			PopupHandler.displayPopup(t);
			
		});
		scaleB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.setscale.btndesc"), "%n%"));
		this.rightclickMenu.addContent(scaleB);

		FMContextMenu playernamePopup = new FMContextMenu();
		this.rightclickMenu.addChild(playernamePopup);
		
		AdvancedButton playernameB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.playername"), true, (press) -> {
			playernamePopup.setParentButton((AdvancedButton) press);
			playernamePopup.openMenuAt(0, press.y);
		});
		playernameB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.playername.btndesc"), "%n%"));
		this.rightclickMenu.addContent(playernameB);
		
		AdvancedButton autoPlayernameB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.playername.auto"), true, (press) -> {
			if (!this.isCLientPlayerName) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.isCLientPlayerName = true;
			this.getObject().playerName = "%playername%";
			this.reloadEntity();
			this.rightclickMenu.closeMenu();
		});
		autoPlayernameB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.playername.auto.btndesc"), "%n%"));
		playernamePopup.addContent(autoPlayernameB);
		
		AdvancedButton setPlayernameB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.playername.setname"), true, (press) -> {
			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.playerentity.playername.setname") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						if ((call != this.getObject().playerName) || (this.isCLientPlayerName != false)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.isCLientPlayerName = false;
						this.getObject().playerName = call;
						this.reloadEntity();
					} else {
						if (this.getObject().playerName != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.isCLientPlayerName = false;
						this.getObject().playerName = null;
						this.reloadEntity();
					}
				}
				
			});
			
			if (this.getObject().playerName != null) {
				t.setText(StringUtils.convertFormatCodes(this.getObject().playerName, "§", "&"));
			}
			
			PopupHandler.displayPopup(t);
			
		});
		setPlayernameB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.playername.setname.btndesc"), "%n%"));
		playernamePopup.addContent(setPlayernameB);

		FMContextMenu skinPopup = new FMContextMenu();
		this.rightclickMenu.addChild(skinPopup);
		
		AdvancedButton skinB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.skin"), true, (press) -> {
			skinPopup.setParentButton((AdvancedButton) press);
			skinPopup.openMenuAt(0, press.y);
		});
		skinB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.skin.btndesc"), "%n%"));
		this.rightclickMenu.addContent(skinB);
		
		AdvancedButton localSkinB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.texture.local"), true, (press) -> {
			
			ChooseFilePopup t = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.equals("")) {
						File home = Minecraft.getInstance().gameDirectory;
						call = call.replace("\\", "/");
						File f = new File(call);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							f = new File(Minecraft.getInstance().gameDirectory, call);
						}
						String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
						if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
							if (filename.equals(f.getName())) {
								if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
									call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
									if (call.startsWith("\\") || call.startsWith("/")) {
										call = call.substring(1);
									}
								}
								if ((this.skinPath == null) || !this.skinPath.equals(call)) {
									this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
								}

								this.skinUrl = null;
								this.skinPath = call;
								this.reloadEntity();

							} else {
								LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
							}
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.title"), "", Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.skinPath != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.skinPath = null;
						this.reloadEntity();

					}
				}
			}, "png");
			
			if (this.skinPath != null) {
				t.setText(this.skinPath);
			}
			
			PopupHandler.displayPopup(t);
			
		});
		skinPopup.addContent(localSkinB);
		
		AdvancedButton urlSkinB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.texture.url"), true, (press) -> {

			FMTextInputPopup t = new PlaceholderInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.playerentity.texture.url") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						String finalURL = null;
						call = WebUtils.filterURL(call);
						finalURL = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(call);

						if (WebUtils.isValidUrl(finalURL)) {
							if (this.skinUrl != call) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}

							this.skinPath = null;
							this.skinUrl = call;
							this.reloadEntity();

						} else {
							LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.web.invalidurl"), "", "", "", "", "", "");
						}

					} else {

						if (this.skinUrl != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}

						this.skinUrl = null;
						this.reloadEntity();

					}
				}
			});
			
			if (this.skinUrl != null) {
				t.setText(this.skinUrl);
			}
			
			PopupHandler.displayPopup(t);
			
		});
		skinPopup.addContent(urlSkinB);

		FMContextMenu capePopup = new FMContextMenu();
		this.rightclickMenu.addChild(capePopup);
		
		AdvancedButton capeB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.cape"), true, (press) -> {
			capePopup.setParentButton((AdvancedButton) press);
			capePopup.openMenuAt(0, press.y);
		});
		capeB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.cape.btndesc"), "%n%"));
		this.rightclickMenu.addContent(capeB);
		
		AdvancedButton localCapeB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.texture.local"), true, (press) -> {

			ChooseFilePopup t = new ChooseFilePopup((call) -> {
				if (call != null) {
					if (!call.equals("")) {
						File home = Minecraft.getInstance().gameDirectory;
						call = call.replace("\\", "/");
						File f = new File(call);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							f = new File(Minecraft.getInstance().gameDirectory, call);
						}
						String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
						if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
							if (filename.equals(f.getName())) {
								if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
									call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
									if (call.startsWith("\\") || call.startsWith("/")) {
										call = call.substring(1);
									}
								}
								if ((this.capePath == null) || !this.capePath.equals(call)) {
									this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
								}

								this.capeUrl = null;
								this.capePath = call;
								this.reloadEntity();

							} else {
								LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
							}
						} else {
							LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.title"), "", Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
						}
					} else {
						if (this.capePath != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.capePath = null;
						this.reloadEntity();

					}
				}
			}, "png");
			
			if (this.capePath != null) {
				t.setText(this.capePath);
			}
			
			PopupHandler.displayPopup(t);
			
		});
		capePopup.addContent(localCapeB);
		
		AdvancedButton urlCapeB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.texture.url"), true, (press) -> {

			FMTextInputPopup t = new PlaceholderInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.playerentity.texture.url") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						String finalURL = null;
						call = WebUtils.filterURL(call);
						finalURL = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(call);

						if (WebUtils.isValidUrl(finalURL)) {
							if (this.capeUrl != call) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}

							this.capePath = null;
							this.capeUrl = call;
							this.reloadEntity();

						} else {
							LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.web.invalidurl"), "", "", "", "", "", "");
						}

					} else {

						if (this.capeUrl != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}

						this.capeUrl = null;
						this.reloadEntity();

					}
				}
			});
			
			if (this.capeUrl != null) {
				t.setText(this.capeUrl);
			}
			
			PopupHandler.displayPopup(t);
			
		});
		capePopup.addContent(urlCapeB);

		FMContextMenu rotationPopup = new FMContextMenu();
		this.rightclickMenu.addChild(rotationPopup);
		
		AdvancedButton rotationB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.rotation"), true, (press) -> {
			rotationPopup.setParentButton((AdvancedButton) press);
			rotationPopup.openMenuAt(0, press.y);
		});
		rotationB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.rotation.btndesc"), "%n%"));
		this.rightclickMenu.addContent(rotationB);
		
		AdvancedButton autoRotationB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.rotation.auto"), true, (press) -> {
			if (!this.getObject().autoRotation) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			this.getObject().autoRotation = true;
			this.rightclickMenu.closeMenu();
		});
		autoRotationB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.rotation.auto.btndesc"), "%n%"));
		rotationPopup.addContent(autoRotationB);
		
		AdvancedButton customRotationB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.playerentity.rotation.custom"), true, (press) -> {
			PopupHandler.displayPopup(new PlayerEntityRotationPopup(this.handler, this));
		});
		customRotationB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.rotation.custom.btndesc"), "%n%"));
		rotationPopup.addContent(customRotationB);
		
		String slimLabel = Locals.localize("helper.creator.items.playerentity.slim.off");
		if (this.getEntity().isSlimSkin()) {
			slimLabel = Locals.localize("helper.creator.items.playerentity.slim.on");
		}
		AdvancedButton slimB = new AdvancedButton(0, 0, 0, 16, slimLabel, true, (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			if (this.getEntity().isSlimSkin()) {
				this.getEntity().setSlimSkin(false);
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.slim.off")));
			} else {
				this.getEntity().setSlimSkin(true);
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.slim.on")));
			}
		});
		slimB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.slim.btndesc"), "%n%"));
		this.rightclickMenu.addContent(slimB);
		
		String crouchingLabel = Locals.localize("helper.creator.items.playerentity.crouching.off");
		if (this.getEntity().crouching) {
			crouchingLabel = Locals.localize("helper.creator.items.playerentity.crouching.on");
		}
		AdvancedButton crouchingB = new AdvancedButton(0, 0, 0, 16, crouchingLabel, true, (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			if (this.getEntity().crouching) {
				this.getEntity().crouching = false;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.crouching.off")));
			} else {
				this.getEntity().crouching = true;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.crouching.on")));
			}
		});
		crouchingB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.crouching.btndesc"), "%n%"));
		this.rightclickMenu.addContent(crouchingB);
		
		String showNameLabel = Locals.localize("helper.creator.items.playerentity.showname.off");
		if (this.getEntity().showName) {
			showNameLabel = Locals.localize("helper.creator.items.playerentity.showname.on");
		}
		AdvancedButton showNameB = new AdvancedButton(0, 0, 0, 16, showNameLabel, true, (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			if (this.getEntity().showName) {
				this.getEntity().showName = false;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.showname.off")));
			} else {
				this.getEntity().showName = true;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.showname.on")));
			}
		});
		showNameB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.showname.btndesc"), "%n%"));
		this.rightclickMenu.addContent(showNameB);
		
		String parrotLabel = Locals.localize("helper.creator.items.playerentity.parrot.off");
		if (this.getEntity().hasParrot) {
			parrotLabel = Locals.localize("helper.creator.items.playerentity.parrot.on");
		}
		AdvancedButton parrotB = new AdvancedButton(0, 0, 0, 16, parrotLabel, true, (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			if (this.getEntity().hasParrot) {
				this.getEntity().hasParrot = false;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.parrot.off")));
			} else {
				this.getEntity().hasParrot = true;
				press.setMessage(Component.literal(Locals.localize("helper.creator.items.playerentity.parrot.on")));
			}
		});
		parrotB.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.playerentity.parrot.btndesc"), "%n%"));
		this.rightclickMenu.addContent(parrotB);
		
	}

	@Override
	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
		//horizontal line top
		fill(matrix, this.getEntityPosX(), this.getEntityPosY(), this.getEntityPosX() + this.object.getWidth(), this.getEntityPosY() + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		fill(matrix, this.getEntityPosX(), this.getEntityPosY() + this.object.getHeight(), this.getEntityPosX() + this.object.getWidth() + 1, this.getEntityPosY() + this.object.getHeight() + 1, Color.BLUE.getRGB());
		//vertical line left
		fill(matrix, this.getEntityPosX(), this.getEntityPosY(), this.getEntityPosX() + 1, this.getEntityPosY() + this.object.getHeight(), Color.BLUE.getRGB());
		//vertical line right
		fill(matrix, this.getEntityPosX() + this.object.getWidth(), this.getEntityPosY(), this.getEntityPosX() + this.object.getWidth() + 1, this.getEntityPosY() + this.object.getHeight(), Color.BLUE.getRGB());

		//Render pos and size values
		Font font = Minecraft.getInstance().font;
		RenderUtils.setScale(matrix, 0.5F);
		font.draw(matrix, Locals.localize("helper.creator.items.border.orientation")+ ": " + this.object.orientation, this.getEntityPosX()*2, (this.getEntityPosY()*2) - 35, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.string.border.scale") + ": " + this.getObject().scale, this.getEntityPosX()*2, (this.getEntityPosY()*2) - 26, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.posx") + ": " + this.getEntityPosX(), this.getEntityPosX()*2, (this.getEntityPosY()*2) - 17, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.width") + ": " + this.object.getWidth(), this.getEntityPosX()*2, (this.getEntityPosY()*2) - 8, Color.WHITE.getRGB());

		font.draw(matrix, Locals.localize("helper.creator.items.border.posy") + ": " + this.getEntityPosY(), ((this.getEntityPosX() + this.object.getWidth())*2)+3, ((this.getEntityPosY() + this.object.getHeight())*2) - 14, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.height") + ": " + this.object.getHeight(), ((this.getEntityPosX() + this.object.getWidth())*2)+3, ((this.getEntityPosY() + this.object.getHeight())*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale(matrix);
	}

	@Override
	protected void renderHighlightBorder(PoseStack matrix) {
		Color c = new Color(0, 200, 255, 255);

		//horizontal line top
		fill(matrix, this.getEntityPosX(), this.getEntityPosY(), this.getEntityPosX() + this.object.getWidth(), this.getEntityPosY() + 1, c.getRGB());
		//horizontal line bottom
		fill(matrix, this.getEntityPosX(), this.getEntityPosY() + this.object.getHeight(), this.getEntityPosX() + this.object.getWidth() + 1, this.getEntityPosY() + this.object.getHeight() + 1, c.getRGB());
		//vertical line left
		fill(matrix, this.getEntityPosX(), this.getEntityPosY(), this.getEntityPosX() + 1, this.getEntityPosY() + this.object.getHeight(), c.getRGB());
		//vertical line right
		fill(matrix, this.getEntityPosX() + this.object.getWidth(), this.getEntityPosY(), this.getEntityPosX() + this.object.getWidth() + 1, this.getEntityPosY() + this.object.getHeight(), c.getRGB());
	}
	
	public void reloadEntity() {
		PropertiesSection sec = this.getProperties().get(0);
		this.object = new PlayerEntityCustomizationItem(sec);
	}
	
	public MenuPlayerEntity getEntity() {
		return this.getObject().entity;
	}
	
	private int getEntityPosX() {
		return (int) (this.getObject().getPosX(this.handler) - ((this.getObject().entity.getBbWidth()*this.getObject().scale) / 2));
	}
	
	private int getEntityPosY() {
		return (int) (this.getObject().getPosY(this.handler) - (this.getObject().entity.getBbHeight()*this.getObject().scale));
	}
	
	public PlayerEntityCustomizationItem getObject() {
		return ((PlayerEntityCustomizationItem)this.object);
	}
	
	@Override
	public boolean isGrabberPressed() {
		return false;
	}
	
	@Override
	public int getActiveResizeGrabber() {
		return -1;
	}

	@Override
	protected void setOrientation(String pos) {
		super.setOrientation(pos);
		if (this.object.orientation.startsWith("top-")) {
			this.object.posY += this.object.getHeight();
		}
		if (this.object.orientation.startsWith("bottom-")) {
			this.object.posY += this.object.getHeight();
		}
		if (this.object.orientation.startsWith("mid-")) {
			this.object.posY += this.object.getHeight();
		}
		if (this.object.orientation.endsWith("-left")) {
			this.object.posX += this.object.getWidth();
		}
		if (this.object.orientation.endsWith("-centered")) {
			this.object.posX += this.object.getWidth() / 2;
		}
	}
	
	public void setScale(int scale) {
		if (this.getObject().scale != scale) {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
		}
		
		this.getObject().scale = scale;
		this.setWidth((int)(this.getObject().entity.getBbWidth()*scale));
		this.setHeight((int)(this.getObject().entity.getBbHeight()*scale));
	}

	@Override
	protected void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.getEntityPosX()) && (mouseX <= this.getEntityPosX() + this.object.getWidth()) && (mouseY >= this.getEntityPosY()) && mouseY <= this.getEntityPosY() + this.object.getHeight()) {
			this.hovered = true;
		} else {
			this.hovered = false;
		}
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("actionid", this.object.getActionId());
		if (this.object.advancedPosX != null) {
			p1.addEntry("advanced_posx", this.object.advancedPosX);
		}
		if (this.object.advancedPosY != null) {
			p1.addEntry("advanced_posy", this.object.advancedPosY);
		}
		if (this.object.advancedWidth != null) {
			p1.addEntry("advanced_width", this.object.advancedWidth);
		}
		if (this.object.advancedHeight != null) {
			p1.addEntry("advanced_height", this.object.advancedHeight);
		}
		if (this.object.delayAppearance) {
			p1.addEntry("delayappearance", "true");
			p1.addEntry("delayappearanceeverytime", "" + this.object.delayAppearanceEverytime);
			p1.addEntry("delayappearanceseconds", "" + this.object.delayAppearanceSec);
			if (this.object.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
			}
		}
		p1.addEntry("action", "addentity");
		p1.addEntry("x", "" + this.object.posX);
		p1.addEntry("y", "" + this.object.posY);
		p1.addEntry("orientation", this.object.orientation);
		if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.object.orientationElementIdentifier);
		}
		p1.addEntry("scale", "" + this.getObject().scale);
		if (this.getObject().playerName != null) {
			if (this.isCLientPlayerName) {
				p1.addEntry("playername", "%playername%");
			} else {
				p1.addEntry("playername", this.getObject().playerName);
			}
		}
		if (this.skinPath != null) {
			p1.addEntry("skinpath", "" + this.skinPath);
		}
		if (this.skinUrl != null) {
			p1.addEntry("skinurl", "" + this.skinUrl);
		}
		if (this.capePath != null) {
			p1.addEntry("capepath", "" + this.capePath);
		}
		if (this.capeUrl != null) {
			p1.addEntry("capeurl", "" + this.capeUrl);
		}
		if (!this.getObject().autoRotation) {
			p1.addEntry("autorotation", "false");
			p1.addEntry("bodyrotationx", "" + this.getObject().bodyRotationX);
			p1.addEntry("bodyrotationy", "" + this.getObject().bodyRotationY);
			p1.addEntry("headrotationx", "" + this.getObject().headRotationX);
			p1.addEntry("headrotationy", "" + this.getObject().headRotationY);
		}
		p1.addEntry("slim", "" + this.getEntity().isSlimSkin());
		p1.addEntry("parrot", "" + this.getEntity().hasParrot);
		p1.addEntry("crouching", "" + this.getEntity().crouching);
		p1.addEntry("showname", "" + this.getEntity().showName);

		this.addLoadingRequirementPropertiesTo(p1);

		l.add(p1);
		
		return l;
	}

}
