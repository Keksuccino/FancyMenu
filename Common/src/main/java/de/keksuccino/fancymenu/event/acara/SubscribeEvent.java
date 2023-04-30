
//Acara - Simple Java Event System

//Copyright (c) 2020-2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.fancymenu.event.acara;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {
	
	int priority() default EventPriority.NORMAL;
	
}
