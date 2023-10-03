package de.keksuccino.fancymenu.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mixin classes annotated with {@link UnoptimizedMixin} are unstable, inefficient, badly coded or similar.<br>
 * They should get revisited/rewritten as soon as possible.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface UnoptimizedMixin {

	public String value();
	
}
