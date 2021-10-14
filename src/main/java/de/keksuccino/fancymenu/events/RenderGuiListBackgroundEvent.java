package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.fml.common.eventhandler.Event;

public class RenderGuiListBackgroundEvent extends Event {
	
	protected GuiSlot list;
	
	public RenderGuiListBackgroundEvent(GuiSlot list) {
		this.list = list;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public GuiSlot getList() {
		return this.list;
	}
	
	public static class Pre extends RenderGuiListBackgroundEvent {

		public Pre(GuiSlot list) {
			super(list);
		}

	}
	
	public static class Post extends RenderGuiListBackgroundEvent {

		public Post(GuiSlot list) {
			super(list);
		}
		
	}
	
}
