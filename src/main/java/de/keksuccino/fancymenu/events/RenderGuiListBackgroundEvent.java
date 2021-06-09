package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraftforge.eventbus.api.Event;

@SuppressWarnings("rawtypes")
public class RenderGuiListBackgroundEvent extends Event {
	
	protected AbstractList list;
	
	public RenderGuiListBackgroundEvent(AbstractList list) {
		this.list = list;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public AbstractList getList() {
		return this.list;
	}
	
	public static class Pre extends RenderGuiListBackgroundEvent {

		public Pre(AbstractList list) {
			super(list);
		}

	}
	
	public static class Post extends RenderGuiListBackgroundEvent {

		public Post(AbstractList list) {
			super(list);
		}
		
	}
	
}
