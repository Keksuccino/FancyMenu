package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.eventbus.api.Event;

public class PlayWidgetClickSoundEvent extends Event {
	
	private AbstractWidget widget;
	
	public PlayWidgetClickSoundEvent(AbstractWidget widget) {
		this.widget = widget;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractWidget getWidget() {
		return this.widget;
	}
	
	public static class Pre extends PlayWidgetClickSoundEvent {

		public Pre(AbstractWidget widget) {
			super(widget);
		}
		
	}
	
	public static class Post extends PlayWidgetClickSoundEvent {

		public Post(AbstractWidget widget) {
			super(widget);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
