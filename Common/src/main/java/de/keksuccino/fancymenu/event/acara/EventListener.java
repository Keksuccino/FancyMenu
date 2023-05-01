
//Acara - Simple Java Event System

//Copyright (c) 2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.fancymenu.event.acara;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

	/**
	 * The priority of the event listener.<br>
	 * The listener with the highest priority gets notified first.<br>
	 * Priorities get handled per event type.
	 **/
	int priority() default EventPriority.NORMAL;
	
}
