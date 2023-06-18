package de.keksuccino.fancymenu.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Classes and methods annotated with {@link Legacy} are mostly deprecated stuff
 * or compatibility layers for old features/formats that will be removed in the future.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Legacy {

	public String value();
	
}
