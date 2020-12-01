package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.videosettings;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class VideoSettingsMenuHandler extends MenuHandlerBase {

	private static final GameSettings.Options[] VIDEO_OPTIONS = new GameSettings.Options[] {GameSettings.Options.GRAPHICS, GameSettings.Options.RENDER_DISTANCE, GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT, GameSettings.Options.ANAGLYPH, GameSettings.Options.VIEW_BOBBING, GameSettings.Options.GUI_SCALE, GameSettings.Options.ATTACK_INDICATOR, GameSettings.Options.GAMMA, GameSettings.Options.RENDER_CLOUDS, GameSettings.Options.PARTICLES, GameSettings.Options.USE_FULLSCREEN, GameSettings.Options.ENABLE_VSYNC, GameSettings.Options.MIPMAP_LEVELS, GameSettings.Options.USE_VBO, GameSettings.Options.ENTITY_SHADOWS};
	
	public VideoSettingsMenuHandler() {
		super(GuiVideoSettings.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (isScrollable()) {
				try {
					VideoSettingsList l;
					if (OpenGlHelper.vboSupported) {
						l = new VideoSettingsList(Minecraft.getMinecraft(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 32, 25, VIDEO_OPTIONS, this);
					}
					else {
						GameSettings.Options[] agamesettings$options = new GameSettings.Options[VIDEO_OPTIONS.length - 1];
						int i = 0;
						for (GameSettings.Options gamesettings$options : VIDEO_OPTIONS) {
							if (gamesettings$options == GameSettings.Options.USE_VBO) {
								break;
							}
							agamesettings$options[i] = gamesettings$options;
							++i;
						}
						l = new VideoSettingsList(Minecraft.getMinecraft(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 32, 25, agamesettings$options, this);
					}

					Field f = ReflectionHelper.findField(GuiVideoSettings.class, "optionsRowList", "field_146501_h");
					f.set(e.getGui(), l);
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		super.onButtonsCached(e);
	}
	
	public static boolean isScrollable() {
		Field f = null;
		try {
			f = ReflectionHelper.findField(GuiVideoSettings.class, "optionsRowList", "field_146501_h");
		} catch (Exception e) {}
		return (f != null);
	}

}
