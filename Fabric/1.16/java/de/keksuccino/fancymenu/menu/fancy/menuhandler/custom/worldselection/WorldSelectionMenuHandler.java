package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

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
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class WorldSelectionMenuHandler extends MenuHandlerBase {

	public WorldSelectionMenuHandler() {
		super(SelectWorldScreen.class.getName());
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {
				Field f = ReflectionHelper.findField(SelectWorldScreen.class, "searchBox", "field_3220");
				TextFieldWidget t = (TextFieldWidget) f.get(e.getGui());
				
				WorldSelectionMenuList l = new WorldSelectionMenuList((SelectWorldScreen) e.getGui(), MinecraftClient.getInstance(), e.getGui().width, e.getGui().height, 48, e.getGui().height - 64, 36, () -> {
					return t.getText();
				}, null, this);

				t.setChangedListener((s) -> {
					l.filter(() -> {
						return s;
					}, false);
				});

				Field f2 = ReflectionHelper.findField(SelectWorldScreen.class, "levelList", "field_3218");
				e.getGui().children().remove(f2.get(e.getGui()));
				
				f2.set(e.getGui(), l);
				addChildren(e.getGui(), l);
			} catch (Exception ex) {
				ex.printStackTrace();
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
