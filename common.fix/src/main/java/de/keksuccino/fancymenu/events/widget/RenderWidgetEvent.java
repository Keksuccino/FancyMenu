package de.keksuccino.fancymenu.events.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.AbstractWidget;

public class RenderWidgetEvent extends EventBase {
	
	protected AbstractWidget widget;
	protected float alpha;
	protected PoseStack matrix;
	
	public RenderWidgetEvent(PoseStack matrix, AbstractWidget widget, float alpha) {
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
	
	public static class Pre extends RenderWidgetEvent {

		public Pre(PoseStack matrix, AbstractWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetEvent {

		public Post(PoseStack matrix, AbstractWidget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
