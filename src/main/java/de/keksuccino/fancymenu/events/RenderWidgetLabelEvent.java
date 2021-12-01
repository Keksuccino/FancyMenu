package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;

public class RenderWidgetLabelEvent extends EventBase {
	
	protected PressableWidget widget;
	protected float alpha;
	protected MatrixStack matrix;
	
	public RenderWidgetLabelEvent(MatrixStack matrix, PressableWidget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.matrix = matrix;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public PressableWidget getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public MatrixStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderWidgetLabelEvent {

		public Pre(MatrixStack matrix, PressableWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetLabelEvent {

		public Post(MatrixStack matrix, PressableWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
