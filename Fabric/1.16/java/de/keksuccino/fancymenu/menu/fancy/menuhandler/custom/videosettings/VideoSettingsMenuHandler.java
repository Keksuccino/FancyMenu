package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.videosettings;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent.Pre;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.options.FullScreenOption;
import net.minecraft.client.options.Option;

public class VideoSettingsMenuHandler extends MenuHandlerBase {

	private static final Option[] OPTIONS = new Option[]{Option.GRAPHICS, Option.RENDER_DISTANCE, Option.AO, Option.FRAMERATE_LIMIT, Option.VSYNC, Option.VIEW_BOBBING, Option.GUI_SCALE, Option.ATTACK_INDICATOR, Option.GAMMA, Option.CLOUDS, Option.FULLSCREEN, Option.PARTICLES, Option.MIPMAP_LEVELS, Option.ENTITY_SHADOWS};
	
	public VideoSettingsMenuHandler() {
		super(VideoOptionsScreen.class.getName());
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (isScrollable()) {
				try {
					ButtonListWidget l = new VideoSettingsList(MinecraftClient.getInstance(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 32, 25, this);
					l.addSingleOptionEntry(new FullScreenOption(MinecraftClient.getInstance().getWindow()));
				    l.addSingleOptionEntry(Option.BIOME_BLEND_RADIUS);
				    l.addAll(OPTIONS);
					Field f = ReflectionHelper.findField(VideoOptionsScreen.class, "list", "field_2639");
					e.getGui().children().remove(f.get(e.getGui()));
					f.set(e.getGui(), l);
					addChildren(e.getGui(), l);
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		super.onButtonsCached(e);
	}

	private static void addChildren(Screen s, Element e) {
		try {
			Field f = ReflectionHelper.findField(Screen.class, "children", "field_22786");
			((List<Element>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static boolean isScrollable() {
		Field f = null;
		try {
			f = ReflectionHelper.findField(VideoOptionsScreen.class, "list", "field_2639");
		} catch (Exception e) {}
		return (f != null);
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(Pre e) {
		super.onInitPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(DrawScreenEvent.Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);
	}

}
