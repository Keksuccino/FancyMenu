package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.input.MouseInput;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.LanguageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.OptionButton;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class LanguageMenuHandler extends MenuHandlerBase {

	private Screen current;
	private List list;
	private java.util.List<Widget> currentButtons;
	
	public LanguageMenuHandler() {
		super(LanguageScreen.class.getName());
	}
	
	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			this.current = e.getGui();
			
			e.getGui().children().clear();
			
			this.list = new List(Minecraft.getInstance());
			addChildren(e.getGui(), this.list);
			
			GameSettings s = Minecraft.getInstance().gameSettings;
			
			OptionButton optionbutton = new OptionButton(e.getGui().width / 2 - 155, e.getGui().height - 38, 150, 20, AbstractOption.FORCE_UNICODE_FONT, AbstractOption.FORCE_UNICODE_FONT.func_216743_c(s), (p_213037_1_) -> {
				AbstractOption.FORCE_UNICODE_FONT.func_216740_a(s);
				s.saveOptions();
				p_213037_1_.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_216743_c(s));
				Minecraft.getInstance().updateWindowSize();
			});
			e.addWidget(optionbutton);
			
			Button confirmSettingsBtn = new Button(e.getGui().width / 2 - 155 + 160, e.getGui().height - 38, 150, 20, I18n.format("gui.done"), (press) -> {
				LanguageMenuHandler.List.LanguageEntry languagescreen$list$languageentry = this.list.getSelected();
				if (languagescreen$list$languageentry != null && !languagescreen$list$languageentry.field_214398_b.getCode().equals(Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode())) {
					Minecraft.getInstance().getLanguageManager().setCurrentLanguage(languagescreen$list$languageentry.field_214398_b);
					s.language = languagescreen$list$languageentry.field_214398_b.getCode();
					net.minecraftforge.client.ForgeHooksClient.refreshResources(Minecraft.getInstance(), net.minecraftforge.resource.VanillaResourceType.LANGUAGES);
					Minecraft.getInstance().fontRenderer.setBidiFlag(Minecraft.getInstance().getLanguageManager().isCurrentLanguageBidirectional());
					press.setMessage(I18n.format("gui.done"));
					optionbutton.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_216743_c(s));
					s.saveOptions();
				}

				Minecraft.getInstance().displayGuiScreen(LanguageMenuHandler.this.getParent());
			});
			e.addWidget(confirmSettingsBtn);
			
			this.currentButtons = e.getWidgetList();
		}
		
		super.onInitPost(e);
	}
	
	@SubscribeEvent
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			e.setCanceled(true);
			e.getGui().renderBackground();
		}
	}
	
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);
		
		if (this.shouldCustomize(e.getGui())) {
			this.list.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			e.getGui().drawCenteredString(Minecraft.getInstance().fontRenderer, e.getGui().getTitle().getFormattedText(), e.getGui().width / 2, 16, 16777215);
			e.getGui().drawCenteredString(Minecraft.getInstance().fontRenderer, "(" + I18n.format("options.languageWarning") + ")", e.getGui().width / 2, e.getGui().height - 56, 8421504);

			for(int i = 0; i < this.currentButtons.size(); ++i) {
				this.currentButtons.get(i).render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}
	
	private Screen getParent() {
		try {
			Field f = ObfuscationReflectionHelper.findField(LanguageScreen.class, "field_146453_a");
			return (Screen) f.get(this.current);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	class List extends ExtendedList<LanguageMenuHandler.List.LanguageEntry> {
		public List(Minecraft mcIn) {
			super(mcIn, LanguageMenuHandler.this.current.width, LanguageMenuHandler.this.current.height, 32, LanguageMenuHandler.this.current.height - 65 + 4, 18);

			for(Language language : Minecraft.getInstance().getLanguageManager().getLanguages()) {
				LanguageMenuHandler.List.LanguageEntry languagescreen$list$languageentry = new LanguageMenuHandler.List.LanguageEntry(language);
				this.addEntry(languagescreen$list$languageentry);
				if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode().equals(language.getCode())) {
					this.setSelected(languagescreen$list$languageentry);
				}
			}

			if (this.getSelected() != null) {
				this.centerScrollOn(this.getSelected());
			}

		}
		
		@Override
		public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
			int i = this.getScrollbarPosition();
			int j = i + 6;
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();
			
			if (!LanguageMenuHandler.this.canRenderBackground()) {
				this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
			      GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			      bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((double)((float)this.x0 / 32.0F), (double)((float)(this.y1 + (int)this.getScrollAmount()) / 32.0F)).color(32, 32, 32, 255).endVertex();
			      bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((double)((float)this.x1 / 32.0F), (double)((float)(this.y1 + (int)this.getScrollAmount()) / 32.0F)).color(32, 32, 32, 255).endVertex();
			      bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((double)((float)this.x1 / 32.0F), (double)((float)(this.y0 + (int)this.getScrollAmount()) / 32.0F)).color(32, 32, 32, 255).endVertex();
			      bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((double)((float)this.x0 / 32.0F), (double)((float)(this.y0 + (int)this.getScrollAmount()) / 32.0F)).color(32, 32, 32, 255).endVertex();
			      tessellator.draw();
			}
			
			int k = this.getRowLeft();
			int l = this.y0 + 4 - (int)this.getScrollAmount();

			this.renderList(k, l, p_render_1_, p_render_2_, p_render_3_);
		      GlStateManager.disableDepthTest();
		      this.renderHoleBackground(0, this.y0, 255, 255);
		      this.renderHoleBackground(this.y1, this.height, 255, 255);
		      GlStateManager.enableBlend();
		      GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		      GlStateManager.disableAlphaTest();
		      GlStateManager.shadeModel(7425);
		      GlStateManager.disableTexture();
		      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		      bufferbuilder.pos((double)this.x0, (double)(this.y0 + 4), 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 0).endVertex();
		      bufferbuilder.pos((double)this.x1, (double)(this.y0 + 4), 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 0).endVertex();
		      bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
		      bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
		      tessellator.draw();
		      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		      bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
		      bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
		      bufferbuilder.pos((double)this.x1, (double)(this.y1 - 4), 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 0).endVertex();
		      bufferbuilder.pos((double)this.x0, (double)(this.y1 - 4), 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 0).endVertex();
		      tessellator.draw();
		      int j1 = this.getMaxScroll();
		      if (j1 > 0) {
		         int k1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
		         k1 = MathHelper.clamp(k1, 32, this.y1 - this.y0 - 8);
		         int l1 = (int)this.getScrollAmount() * (this.y1 - this.y0 - k1) / j1 + this.y0;
		         if (l1 < this.y0) {
		            l1 = this.y0;
		         }

		         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		         bufferbuilder.pos((double)i, (double)this.y1, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
		         bufferbuilder.pos((double)j, (double)this.y1, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
		         bufferbuilder.pos((double)j, (double)this.y0, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
		         bufferbuilder.pos((double)i, (double)this.y0, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
		         tessellator.draw();
		         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		         bufferbuilder.pos((double)i, (double)(l1 + k1), 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
		         bufferbuilder.pos((double)j, (double)(l1 + k1), 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
		         bufferbuilder.pos((double)j, (double)l1, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
		         bufferbuilder.pos((double)i, (double)l1, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
		         tessellator.draw();
		         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		         bufferbuilder.pos((double)i, (double)(l1 + k1 - 1), 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
		         bufferbuilder.pos((double)(j - 1), (double)(l1 + k1 - 1), 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
		         bufferbuilder.pos((double)(j - 1), (double)l1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
		         bufferbuilder.pos((double)i, (double)l1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
		         tessellator.draw();
		      }

		      this.renderDecorations(p_render_1_, p_render_2_);
		      GlStateManager.enableTexture();
		      GlStateManager.shadeModel(7424);
		      GlStateManager.enableAlphaTest();
		      GlStateManager.disableBlend();
		}

		private int getMaxScroll() {
			return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
		}
		
		protected int getScrollbarPosition() {
			return super.getScrollbarPosition() + 20;
		}

		public int getRowWidth() {
			return super.getRowWidth() + 50;
		}

		public void setSelected(@Nullable LanguageMenuHandler.List.LanguageEntry p_setSelected_1_) {
			super.setSelected(p_setSelected_1_);
			if (p_setSelected_1_ != null) {
				NarratorChatListener.INSTANCE.func_216864_a((new TranslationTextComponent("narrator.select", p_setSelected_1_.field_214398_b)).getString());
			}

		}

		protected boolean isFocused() {
			return LanguageMenuHandler.this.current.getFocused() == this;
		}

		public class LanguageEntry extends ExtendedList.AbstractListEntry<LanguageMenuHandler.List.LanguageEntry> {
			private final Language field_214398_b;

			public LanguageEntry(Language p_i50494_2_) {
				this.field_214398_b = p_i50494_2_;
			}

			public void render(int p_render_1_, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int p_render_6_, int p_render_7_, boolean p_render_8_, float p_render_9_) {
				Minecraft.getInstance().fontRenderer.setBidiFlag(true);
				List.this.drawCenteredString(Minecraft.getInstance().fontRenderer, this.field_214398_b.toString(), List.this.width / 2, p_render_2_ + 1, 16777215);
				Minecraft.getInstance().fontRenderer.setBidiFlag(Minecraft.getInstance().getLanguageManager().getCurrentLanguage().isBidirectional());
			}

			public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
				if (p_mouseClicked_5_ == 0) {
					this.func_214395_a();
					return true;
				} else {
					return false;
				}
			}

			private void func_214395_a() {
				List.this.setSelected(this);
			}
		}
	}
	
	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "children");
			((java.util.List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
