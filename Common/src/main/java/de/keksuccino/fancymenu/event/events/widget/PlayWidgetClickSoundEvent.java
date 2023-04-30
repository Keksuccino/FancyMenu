package de.keksuccino.fancymenu.event.events.widget;

import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.components.AbstractWidget;

public class PlayWidgetClickSoundEvent extends EventBase {
	
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
