package de.keksuccino.fancymenu.event.events.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventBase;
import net.minecraft.client.gui.components.AbstractWidget;

public class RenderWidgetBackgroundEvent extends EventBase {
	
	protected AbstractWidget widget;
	protected float alpha;
	protected PoseStack matrix;
	
	public RenderWidgetBackgroundEvent(PoseStack matrix, AbstractWidget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.matrix = matrix;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractWidget getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public PoseStack getPoseStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderWidgetBackgroundEvent {

		public Pre(PoseStack matrix, AbstractWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetBackgroundEvent {

		public Post(PoseStack matrix, AbstractWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
