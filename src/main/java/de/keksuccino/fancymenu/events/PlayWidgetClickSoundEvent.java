package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.widget.PressableWidget;

public class PlayWidgetClickSoundEvent extends EventBase {
	
	private PressableWidget widget;
	
	public PlayWidgetClickSoundEvent(PressableWidget widget) {
		this.widget = widget;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public PressableWidget getWidget() {
		return this.widget;
	}
	
	public static class Pre extends PlayWidgetClickSoundEvent {

		public Pre(PressableWidget widget) {
			super(widget);
		}
		
	}
	
	public static class Post extends PlayWidgetClickSoundEvent {

		public Post(PressableWidget widget) {
			super(widget);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
