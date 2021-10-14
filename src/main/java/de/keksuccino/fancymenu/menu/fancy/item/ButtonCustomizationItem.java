package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class ButtonCustomizationItem extends CustomizationItemBase {

	public AdvancedButton button;
	private String hoverLabel;
	private String hoverSound;
	private boolean hover = false;
	private boolean onlyMultiplayer = false;
	private boolean onlySingleplayer = false;
	private boolean onlyOutgame = false;

	public String hoverLabelRaw;
	public String labelRaw;

	public ButtonCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addbutton")) {
			this.labelRaw = item.getEntryValue("label");
			if (this.labelRaw == null) {
				this.labelRaw = "";
			}

			String buttonaction = item.getEntryValue("buttonaction");
			String actionvalue = item.getEntryValue("value");

			if (buttonaction == null) {
				return;
			}
			if (actionvalue == null) {
				actionvalue = "";
			}
			if (!isEditorActive()) {
				actionvalue = DynamicValueHelper.convertFromRaw(actionvalue);
			}

			this.hoverSound = item.getEntryValue("hoversound");
			if (this.hoverSound != null) {
				this.hoverSound = this.hoverSound.replace("\\", "/");
				File f = new File(this.hoverSound);
				if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
					MenuCustomization.registerSound(this.hoverSound, this.hoverSound);
				} else {
					this.hoverSound = null;
				}
			}

			this.hoverLabelRaw = item.getEntryValue("hoverlabel");

			String onlyX = item.getEntryValue("onlydisplayin");
			if (onlyX != null) {
				if (onlyX.equalsIgnoreCase("outgame")) {
					this.onlyOutgame = true;
				}
				if (onlyX.equalsIgnoreCase("multiplayer")) {
					this.onlyMultiplayer = true;
				}
				if (onlyX.equalsIgnoreCase("singleplayer")) {
					this.onlySingleplayer = true;
				}
			}

			String finalAction = actionvalue;
			this.button = new AdvancedButton(0, 0, this.getWidth(), this.getHeight(), this.value, true, (press) -> {
				ButtonScriptEngine.runButtonAction(buttonaction, finalAction);
			});

			String click = item.getEntryValue("clicksound");
			if (click != null) {
				click.replace("\\", "/");
				File f = new File(click);

				if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
					SoundHandler.registerSound(f.getPath(), f.getPath());
					this.button.setClickSound(f.getPath());
				}
			}

			String desc = item.getEntryValue("description");
			if (desc != null) {
				this.button.setDescription(StringUtils.splitLines(DynamicValueHelper.convertFromRaw(desc), "%n%"));
			}

			String backNormal = item.getEntryValue("backgroundnormal");
			String backHover = item.getEntryValue("backgroundhovered");
			String loopBackAnimations = item.getEntryValue("loopbackgroundanimations");
			String restartBackAnimationsOnHover = item.getEntryValue("restartbackgroundanimations");
			String backAnimationNormal = item.getEntryValue("backgroundanimationnormal");
			String backAnimationHover = item.getEntryValue("backgroundanimationhovered");

			if (this.button != null) {
				if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
					this.button.loopBackgroundAnimations = false;
				}
				if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
					this.button.restartBackgroundAnimationsOnHover = false;
				}
				if (backNormal != null) {
					File f = new File(backNormal.replace("\\", "/"));
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundNormal(TextureHandler.getGifResource(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.getResource(f.getPath());
							if (back != null) {
								if (!back.isReady()) {
									back.loadTexture();
								}
								this.button.setBackgroundNormal(back.getResourceLocation());
							}
						}
					}
				} else if (backAnimationNormal != null) {
					if (AnimationHandler.animationExists(backAnimationNormal)) {
						this.button.setBackgroundNormal(AnimationHandler.getAnimation(backAnimationNormal));
					}
				}
				if (backHover != null) {
					File f = new File(backHover.replace("\\", "/"));
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundHover(TextureHandler.getGifResource(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.getResource(f.getPath());
							if (back != null) {
								if (!back.isReady()) {
									back.loadTexture();
								}
								this.button.setBackgroundHover(back.getResourceLocation());
							}
						}
					}
				} else if (backAnimationHover != null) {
					if (AnimationHandler.animationExists(backAnimationHover)) {
						this.button.setBackgroundHover(AnimationHandler.getAnimation(backAnimationHover));
					}
				}
			}

			this.updateValues();

		}
	}

	public void render(MatrixStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		this.updateValues();

		if (this.onlyOutgame && (MinecraftClient.getInstance().world != null)) {
			return;
		}

		if (this.onlyMultiplayer && ((MinecraftClient.getInstance().world == null) || MinecraftClient.getInstance().isIntegratedServerRunning())) {
			return;
		}

		if (this.onlySingleplayer && ((MinecraftClient.getInstance().world == null) || !MinecraftClient.getInstance().isIntegratedServerRunning())) {
			return;
		}

		this.button.setAlpha(this.opacity);

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);

		this.button.setX(x);
		this.button.setY(y);

		if (this.button.isHovered()) {
			if (this.hoverLabel != null) {
				this.button.setMessage(this.hoverLabel);
			}
			if ((this.hoverSound != null) && !this.hover) {
				this.hover = true;
				SoundHandler.resetSound(this.hoverSound);
				SoundHandler.playSound(this.hoverSound);
			}
		} else {
			this.button.setMessage(this.value);
			this.hover = false;
		}

		this.button.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), MinecraftClient.getInstance().getTickDelta());
	}

	protected void updateValues() {

		if (this.labelRaw != null) {
			if (!isEditorActive()) {
				this.value = DynamicValueHelper.convertFromRaw(this.labelRaw);
			} else {
				this.value = StringUtils.convertFormatCodes(this.labelRaw, "&", "§");
			}
		}
		if (this.hoverLabelRaw != null) {
			if (!isEditorActive()) {
				this.hoverLabel = DynamicValueHelper.convertFromRaw(this.hoverLabelRaw);
			} else {
				this.hoverLabel = StringUtils.convertFormatCodes(this.hoverLabelRaw, "&", "§");
			}
		}

	}

	@Override
	public boolean shouldRender() {
		if (this.button == null) {
			return false;
		}
		return super.shouldRender();
	}

	public AdvancedButton getButton() {
		return this.button;
	}

	public Long getId() {
		int ori = 0;
		if (this.orientation.equalsIgnoreCase("original")) {
			ori = 1;
		} else if (this.orientation.equalsIgnoreCase("top-left")) {
			ori = 2;
		} else if (this.orientation.equalsIgnoreCase("mid-left")) {
			ori = 3;
		} else if (this.orientation.equalsIgnoreCase("bottom-left")) {
			ori = 4;
		} else if (this.orientation.equalsIgnoreCase("top-centered")) {
			ori = 5;
		} else if (this.orientation.equalsIgnoreCase("mid-centered")) {
			ori = 6;
		} else if (this.orientation.equalsIgnoreCase("bottom-centered")) {
			ori = 7;
		} else if (this.orientation.equalsIgnoreCase("top-right")) {
			ori = 8;
		} else if (this.orientation.equalsIgnoreCase("mid-right")) {
			ori = 9;
		} else if (this.orientation.equalsIgnoreCase("bottom-right")) {
			ori = 10;
		}

		String idRaw = "00" + ori + "" + Math.abs(this.posX) + "" + Math.abs(this.posY) + "" + Math.abs(this.getWidth());
		long id = 0;
		if (MathUtils.isLong(idRaw)) {
			id = Long.parseLong(idRaw);
		}

		return id;
	}

}
