package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;

public class RenderWidgetEvent extends EventBase {
	
	protected PressableWidget widget;
	protected float alpha;
	protected MatrixStack matrix;
	
	public RenderWidgetEvent(MatrixStack matrix, PressableWidget widget, float alpha) {
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
	
	public static class Pre extends RenderWidgetEvent {

		public Pre(MatrixStack matrix, PressableWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetEvent {

		public Post(MatrixStack matrix, PressableWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
