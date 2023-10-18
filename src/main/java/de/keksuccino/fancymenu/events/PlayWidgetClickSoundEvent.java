package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractWidget;

public class PlayWidgetClickSoundEvent extends EventBase {
	
	private final AbstractWidget widget;
	
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
