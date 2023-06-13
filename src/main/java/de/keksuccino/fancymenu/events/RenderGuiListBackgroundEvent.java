package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractSelectionList;

@SuppressWarnings("rawtypes")
public class RenderGuiListBackgroundEvent extends EventBase {
	
	protected AbstractSelectionList list;
	protected GuiGraphics graphics;
	
	public RenderGuiListBackgroundEvent(GuiGraphics graphics, AbstractSelectionList list) {
		this.list = list;
		this.graphics = graphics;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public AbstractSelectionList getList() {
		return this.list;
	}

	public GuiGraphics getGuiGraphics() {
		return this.graphics;
	}

	public PoseStack getPoseStack() {
		return this.graphics.pose();
	}

	@Deprecated
	public PoseStack getMatrixStack() {
		return this.graphics.pose();
	}
	
	public static class Pre extends RenderGuiListBackgroundEvent {

		public Pre(GuiGraphics graphics, AbstractSelectionList list) {
			super(graphics, list);
		}

	}
	
	public static class Post extends RenderGuiListBackgroundEvent {

		public Post(GuiGraphics graphics, AbstractSelectionList list) {
			super(graphics, list);
		}
		
	}
	
}
