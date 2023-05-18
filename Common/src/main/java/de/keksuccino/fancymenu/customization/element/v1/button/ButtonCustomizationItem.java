package de.keksuccino.fancymenu.customization.element.v1.button;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.IActionExecutorElement;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.fancymenu.resources.texture.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ButtonCustomizationItem extends AbstractElement implements IActionExecutorElement {

	private static final Logger LOGGER = LogManager.getLogger();

	public AdvancedButton button;
	private String hoverLabel;
	private String hoverSound;
	private boolean hover = false;
	private boolean onlyMultiplayer = false;
	private boolean onlySingleplayer = false;
	private boolean onlyOutgame = false;

	public String hoverLabelRaw;
	public String labelRaw;
	public String tooltip;
	
	public List<ActionExecutor.ActionContainer> actions = new ArrayList<>();

	public ButtonCustomizationItem(PropertyContainer item) {
		super(item);

		if ((this.elementType != null) && this.elementType.equalsIgnoreCase("addbutton")) {
			this.labelRaw = item.getValue("label");
			if (this.labelRaw == null) {
				this.labelRaw = "";
			}

			String buttonaction = item.getValue("buttonaction");
			String actionvalue = item.getValue("value");

			if (actionvalue == null) {
				actionvalue = "";
			}

			this.hoverSound = item.getValue("hoversound");
			if (this.hoverSound != null) {
				this.hoverSound = this.hoverSound.replace("\\", "/");
				File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.hoverSound));
				if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
					ScreenCustomization.registerSound(this.hoverSound, this.hoverSound);
				} else {
					this.hoverSound = null;
				}
			}

			this.hoverLabelRaw = item.getValue("hoverlabel");

			String onlyX = item.getValue("onlydisplayin");
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

			
			if (buttonaction != null) {
				if (buttonaction.contains("%btnaction_splitter_fm%")) {
					for (String s : StringUtils.splitLines(buttonaction, "%btnaction_splitter_fm%")) {
						if (s.length() > 0) {
							String action = s;
							String value = null;
							if (s.contains(";")) {
								action = s.split(";", 2)[0];
								value = s.split(";", 2)[1];
							}
							this.actions.add(new ActionExecutor.ActionContainer(action, value));
						}
					}
				} else {
					this.actions.add(new ActionExecutor.ActionContainer(buttonaction, actionvalue));
				}
			}
			

			
			this.button = new AdvancedButton(0, 0, this.getWidth(), this.getHeight(), "", true, (press) -> {
				for (ActionExecutor.ActionContainer c : this.actions) {
					c.execute();
				}
			});
			

			String click = item.getValue("clicksound");
			if (click != null) {
				click.replace("\\", "/");
				File f = new File(click);
				if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
					f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + click);
				}
				if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
					SoundHandler.registerSound(f.getPath(), f.getPath());
					this.button.setClickSound(f.getPath());
				}
			}

			this.tooltip = item.getValue("description");
			if (this.tooltip != null) {
				TooltipHandler.INSTANCE.addWidgetTooltip(this.button, Tooltip.create(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(this.tooltip), "%n%")), false, true);
			}

			String backNormal = fixBackslashPath(item.getValue("backgroundnormal"));
			String backHover = fixBackslashPath(item.getValue("backgroundhovered"));
			String loopBackAnimations = item.getValue("loopbackgroundanimations");
			String restartBackAnimationsOnHover = item.getValue("restartbackgroundanimations");
			String backAnimationNormal = item.getValue("backgroundanimationnormal");
			String backAnimationHover = item.getValue("backgroundanimationhovered");

			if (this.button != null) {
				if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
					this.button.loopBackgroundAnimations = false;
				}
				if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
					this.button.restartBackgroundAnimationsOnHover = false;
				}
				if (backNormal != null) {
					File f = new File(backNormal.replace("\\", "/"));
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
						f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backNormal);
					}
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundNormal(TextureHandler.INSTANCE.getGifTexture(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.INSTANCE.getTexture(f.getPath());
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
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
						f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + backHover);
					}
					if (f.isFile()) {
						if (f.getPath().toLowerCase().endsWith(".gif")) {
							this.button.setBackgroundHover(TextureHandler.INSTANCE.getGifTexture(f.getPath()));
						} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
							ExternalTextureResourceLocation back = TextureHandler.INSTANCE.getTexture(f.getPath());
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

	public void render(PoseStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		this.updateValues();

		if (this.onlyOutgame && (Minecraft.getInstance().level != null)) {
			return;
		}

		if (this.onlyMultiplayer && ((Minecraft.getInstance().level == null) || Minecraft.getInstance().hasSingleplayerServer())) {
			return;
		}

		if (this.onlySingleplayer && ((Minecraft.getInstance().level == null) || !Minecraft.getInstance().hasSingleplayerServer())) {
			return;
		}

		this.button.setAlpha(this.opacity);

		int x = this.getX(menu);
		int y = this.getY(menu);

		this.button.setX(x);
		this.button.setY(y);

		
		if (this.button.isHoveredOrFocused() && this.button.active) {
			if (this.hoverLabel != null) {
				this.button.setMessage(this.hoverLabel);
			} else {
				this.button.setMessage(this.value);
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

		this.button.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getFrameTime());

	}

	protected void updateValues() {

		if (this.tooltip != null) {
			if (this.button.isHoveredOrFocused()) {
				TooltipHandler.INSTANCE.addWidgetTooltip(this.button, Tooltip.create(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(this.tooltip), "%n%")), false, true);
			}
		}
		if (this.labelRaw != null) {
			if (!isEditor()) {
				this.value = PlaceholderParser.replacePlaceholders(this.labelRaw);
			} else {
				this.value = StringUtils.convertFormatCodes(this.labelRaw, "&", "§");
			}
		}
		if (this.hoverLabelRaw != null) {
			if (!isEditor()) {
				this.hoverLabel = PlaceholderParser.replacePlaceholders(this.hoverLabelRaw);
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

	
	@Override
	public List<ActionExecutor.ActionContainer> getActionList() {
		return this.actions;
	}

}
