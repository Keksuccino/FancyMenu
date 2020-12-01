package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.languagesettings;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.LanguageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.OptionButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class LanguageMenuHandler extends MenuHandlerBase {
	
	private LanguageMenuList list;
	private Button confirmSettingsBtn;
	private Button unicodeButton;

	public LanguageMenuHandler() {
		super(LanguageScreen.class.getName());
	}
	
	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			
			Widget uni = null;
			Widget confirm = null;
			for (Widget w : e.getWidgetList()) {
				if (w instanceof OptionButton) {
					uni = w;
				} else {
					confirm = w;
				}
			}
			if (uni != null) {
				e.removeWidget(uni);
			}
			if (confirm != null) {
				e.removeWidget(confirm);
			}
			
			e.getGui().getEventListeners().clear();
			
			GameSettings s = Minecraft.getInstance().gameSettings;
			
			this.unicodeButton = new OptionButton(e.getGui().width / 2 - 155, e.getGui().height - 38, 150, 20, AbstractOption.FORCE_UNICODE_FONT, AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s), (press) -> {
				AbstractOption.FORCE_UNICODE_FONT.nextValue(s);
				s.saveOptions();
				press.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s));
				Minecraft.getInstance().updateWindowSize();
			});
			e.addWidget(this.unicodeButton);
			
			this.confirmSettingsBtn = new Button(e.getGui().width / 2 - 155 + 160, e.getGui().height - 38, 150, 20, DialogTexts.GUI_DONE, (press) -> {
				LanguageMenuList.LanguageEntry languagescreen$list$languageentry = this.list.getSelected();
				if (languagescreen$list$languageentry != null && !languagescreen$list$languageentry.field_214398_b.getCode().equals(Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode())) {
					Minecraft.getInstance().getLanguageManager().setCurrentLanguage(languagescreen$list$languageentry.field_214398_b);
					s.language = languagescreen$list$languageentry.field_214398_b.getCode();
					net.minecraftforge.client.ForgeHooksClient.refreshResources(Minecraft.getInstance(), net.minecraftforge.resource.VanillaResourceType.LANGUAGES);
					press.setMessage(DialogTexts.GUI_DONE);
					this.unicodeButton.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s));
					s.saveOptions();
				}
				
				Minecraft.getInstance().displayGuiScreen(LanguageMenuHandler.this.getParent());
			});
			e.addWidget(this.confirmSettingsBtn);
			
			this.list = new LanguageMenuList((LanguageScreen) e.getGui(), Minecraft.getInstance(), this);
			addChildren(e.getGui(), this.list);

		}
	}

	@SubscribeEvent
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			e.getGui().renderBackground(e.getMatrixStack());
		}
	}

	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);

		if (this.shouldCustomize(e.getGui()) && (this.list != null) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (this.list != null) {
				this.list.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
			if (this.unicodeButton != null) {
				this.unicodeButton.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
			if (this.confirmSettingsBtn != null) {
				this.confirmSettingsBtn.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}

	private Screen getParent() {
		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			try {
				Field f = ObfuscationReflectionHelper.findField(SettingsScreen.class, "field_228182_a_");
				return (Screen) f.get(Minecraft.getInstance().currentScreen);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230705_e_");
			((java.util.List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
