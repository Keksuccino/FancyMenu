package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;

public class AnimationData {
	
	public final String name;
	public final Type type;
	public final IAnimationRenderer animation;
	
	public AnimationData(IAnimationRenderer animation, String name, Type type) {
		this.name = name;
		this.type = type;
		this.animation = animation;
	}
	
	public static enum Type {
		INTERNAL,
		EXTERNAL;
	}

}
