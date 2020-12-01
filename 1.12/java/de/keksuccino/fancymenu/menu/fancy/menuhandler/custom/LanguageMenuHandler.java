package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
public class LanguageMenuHandler extends MenuHandlerBase {

	private GuiScreen current;
	private List list;
	private java.util.List<GuiButton> currentButtons;
	
	private GuiOptionButton forceUnicodeFontBtn;
	private GuiOptionButton confirmSettingsBtn;
	
	public LanguageMenuHandler() {
		super(GuiLanguage.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			this.current = e.getGui();

			this.forceUnicodeFontBtn = new GuiOptionButton(100, e.getGui().width / 2 - 155, e.getGui().height - 38, GameSettings.Options.FORCE_UNICODE_FONT, Minecraft.getMinecraft().gameSettings.getKeyBinding(GameSettings.Options.FORCE_UNICODE_FONT));
	        e.addButton(forceUnicodeFontBtn);
			
			this.confirmSettingsBtn = new GuiOptionButton(6, e.getGui().width / 2 - 155 + 160, e.getGui().height - 38, I18n.format("gui.done"));
			e.addButton(confirmSettingsBtn);
	        
			this.list = new List(Minecraft.getMinecraft());
	        this.list.registerScrollButtons(7, 8);
			
			this.currentButtons = e.getButtonList();
		}
		
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			e.setCanceled(true);
			e.getGui().drawDefaultBackground();
		}
	}
	
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);
		
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (this.list != null) {
				this.list.drawScreen(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
			}
			e.getGui().drawCenteredString(Minecraft.getMinecraft().fontRenderer, I18n.format("options.language"), e.getGui().width / 2, 16, 16777215);
	        e.getGui().drawCenteredString(Minecraft.getMinecraft().fontRenderer, "(" + I18n.format("options.languageWarning") + ")", e.getGui().width / 2, e.getGui().height - 56, 8421504);

			if (this.currentButtons != null) {
				for(int i = 0; i < this.currentButtons.size(); ++i) {
					this.currentButtons.get(i).drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onMouseInput(GuiScreenEvent.MouseInputEvent.Post e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (this.list != null) {
				this.list.handleMouseInput();
			}
		}
	}

    class List extends GuiSlot {
        /** A list containing the many different locale language codes. */
        private final java.util.List<String> langCodeList = Lists.<String>newArrayList();
        /** The map containing the Locale-Language pairs. */
        private final Map<String, Language> languageMap = Maps.<String, Language>newHashMap();

        public List(Minecraft mcIn) {
            super(mcIn, LanguageMenuHandler.this.current.width, LanguageMenuHandler.this.current.height, 32, LanguageMenuHandler.this.current.height - 65 + 4, 18);

            for (Language language : Minecraft.getMinecraft().getLanguageManager().getLanguages())
            {
                this.languageMap.put(language.getLanguageCode(), language);
                this.langCodeList.add(language.getLanguageCode());
            }
        }

        protected int getSize() {
            return this.langCodeList.size();
        }
        
        @Override
        public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
            if (this.visible) {
                this.mouseX = mouseXIn;
                this.mouseY = mouseYIn;
                int i = this.getScrollBarX();
                int j = i + 6;
                this.bindAmountScrolled();
                GlStateManager.disableLighting();
                GlStateManager.disableFog();
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                if (!LanguageMenuHandler.this.canRenderBackground()) {
                	this.drawContainerBackground(tessellator);
                }
                int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
                int l = this.top + 4 - (int)this.amountScrolled;

                if (this.hasListHeader)
                {
                    this.drawListHeader(k, l, tessellator);
                }

                this.drawSelectionBox(k, l, mouseXIn, mouseYIn, partialTicks);
                GlStateManager.disableDepth();
                this.overlayBackground(0, this.top, 255, 255);
                this.overlayBackground(this.bottom, this.height, 255, 255);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                GlStateManager.disableAlpha();
                GlStateManager.shadeModel(7425);
                GlStateManager.disableTexture2D();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)this.left, (double)(this.top + 4), 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.right, (double)(this.top + 4), 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.right, (double)this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.left, (double)this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)this.left, (double)this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.right, (double)this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.right, (double)(this.bottom - 4), 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.left, (double)(this.bottom - 4), 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 0).endVertex();
                tessellator.draw();
                int j1 = this.getMaxScroll();

                if (j1 > 0)
                {
                    int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
                    k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
                    int l1 = (int)this.amountScrolled * (this.bottom - this.top - k1) / j1 + this.top;

                    if (l1 < this.top)
                    {
                        l1 = this.top;
                    }

                    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                    bufferbuilder.pos((double)i, (double)this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                    bufferbuilder.pos((double)j, (double)this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                    bufferbuilder.pos((double)j, (double)this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                    bufferbuilder.pos((double)i, (double)this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
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

                this.renderDecorations(mouseXIn, mouseYIn);
                GlStateManager.enableTexture2D();
                GlStateManager.shadeModel(7424);
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
            }
        }

        /**
         * The element in the slot that was clicked, boolean for whether it was double clicked or not
         */
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            Language language = this.languageMap.get(this.langCodeList.get(slotIndex));
            Minecraft.getMinecraft().getLanguageManager().setCurrentLanguage(language);
            Minecraft.getMinecraft().gameSettings.language = language.getLanguageCode();
            net.minecraftforge.fml.client.FMLClientHandler.instance().refreshResources(net.minecraftforge.client.resource.VanillaResourceType.LANGUAGES);
            Minecraft.getMinecraft().fontRenderer.setUnicodeFlag(Minecraft.getMinecraft().getLanguageManager().isCurrentLocaleUnicode() || Minecraft.getMinecraft().gameSettings.forceUnicodeFont);
            Minecraft.getMinecraft().fontRenderer.setBidiFlag(Minecraft.getMinecraft().getLanguageManager().isCurrentLanguageBidirectional());
            LanguageMenuHandler.this.confirmSettingsBtn.displayString = I18n.format("gui.done");
            LanguageMenuHandler.this.forceUnicodeFontBtn.displayString = Minecraft.getMinecraft().gameSettings.getKeyBinding(GameSettings.Options.FORCE_UNICODE_FONT);
            Minecraft.getMinecraft().gameSettings.saveOptions();
        }

        /**
         * Returns true if the element passed in is currently selected
         */
        protected boolean isSelected(int slotIndex) {
            return ((String)this.langCodeList.get(slotIndex)).equals(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode());
        }

        /**
         * Return the height of the content being scrolled
         */
        protected int getContentHeight() {
            return this.getSize() * 18;
        }

        protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            Minecraft.getMinecraft().fontRenderer.setBidiFlag(true);
            LanguageMenuHandler.this.current.drawCenteredString(Minecraft.getMinecraft().fontRenderer, ((Language)this.languageMap.get(this.langCodeList.get(slotIndex))).toString(), this.width / 2, yPos + 1, 16777215);
            Minecraft.getMinecraft().fontRenderer.setBidiFlag(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().isBidirectional());
        }

		@Override
		protected void drawBackground() {
		}
    }

}
