package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldSelectionMenuList extends GuiSlot {

	private MenuHandlerBase handler;
	private GuiSelectWorld screen;
	private GuiSlot parentList;
	
	private Method drawSlotMethod;
	
	public WorldSelectionMenuList(GuiSelectWorld parent, MenuHandlerBase handler) {
		super(Minecraft.getMinecraft(), parent.width, parent.height, 32, parent.height - 64, 36);
		this.handler = handler;
		this.screen = parent;
		try {
			Field f = ReflectionHelper.findField(GuiSelectWorld.class, "field_146638_t");
			if (f != null) {
				this.parentList = (GuiSlot) f.get(this.screen);
			} else {
				//TODO remove debug
				System.out.println("################### ERROR: GUI SLOT NULL");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
        if (this.showSelectionBox) {
            this.mouseX = mouseXIn;
            this.mouseY = mouseYIn;
            int i = this.getScrollBarX();
            int j = i + 6;
            this.bindAmountScrolled();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
            if (!this.handler.canRenderBackground()) {
            	this.drawContainerBackground(tessellator);
            } else {
            	this.screen.drawDefaultBackground();
            }
            int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
            int l = this.top + 4 - (int)this.amountScrolled;

            if (this.hasListHeader)
            {
                this.drawListHeader(k, l, tessellator);
            }

            this.drawSelectionBox(k, l, mouseXIn, mouseYIn);
            GlStateManager.disableDepth();
            this.overlayBackground(0, this.top, 255, 255);
            this.overlayBackground(this.bottom, this.height, 255, 255);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
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
            int j1 = this.func_148135_f();

            if (j1 > 0)
            {
                int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
                k1 = MathHelper.clamp_int(k1, 32, this.bottom - this.top - 8);
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

            this.func_148142_b(mouseXIn, mouseYIn);
            GlStateManager.enableTexture2D();
            GlStateManager.shadeModel(7424);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
        }
    }

	private List<SaveFormatComparator> getSaveFormatComparator() {
		try {
			Field f = ReflectionHelper.findField(GuiSelectWorld.class, "field_146639_s");
			if (f != null) {
				return (List<SaveFormatComparator>) f.get(this.screen);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int getSlotIndexOfParent() {
		try {
			Field f = ReflectionHelper.findField(GuiSelectWorld.class, "field_146640_r");
			if (f != null) {
				return (int) f.get(this.screen);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
//	private void setSlotIndexOfParent(int index) {
//		try {
//			Field f = ReflectionHelper.findField(GuiSelectWorld.class, "field_146640_r");
//			if (f != null) {
//				f.set(this.screen, index);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	@Override
	protected int getSize() {
		return this.getSaveFormatComparator().size();
	}

	@Override
	protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
		try {
			Method m = ReflectionHelper.findMethod(GuiSlot.class, this.parentList, new String[] {"elementClicked", "func_148144_a"}, int.class, boolean.class, int.class, int.class);
			if (m != null) {
				m.invoke(this.parentList, slotIndex, isDoubleClick, mouseX, mouseY);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean isSelected(int slotIndex) {
		return slotIndex == this.getSlotIndexOfParent();
	}

	@Override
	protected void drawBackground() {
		this.screen.drawDefaultBackground();
	}

	@Override
    protected int getContentHeight() {
        return this.getSaveFormatComparator().size() * 36;
    }

	@Override
	protected void drawSlot(int entryID, int i1, int i2, int i3, int mouseXIn, int mouseYIn) {
		try {
			if (this.drawSlotMethod == null) {
				this.drawSlotMethod = ReflectionHelper.findMethod(GuiSlot.class, this.parentList, new String[] {"drawSlot", "func_180791_a"}, int.class, int.class, int.class, int.class, int.class, int.class);
			}
			if (this.drawSlotMethod != null) {
				this.drawSlotMethod.invoke(this.parentList, entryID, i1, i2, i3, mouseXIn, mouseYIn);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
