package de.keksuccino.fancymenu.util;

/**
 * This supplier consumes (first class parameter) an object and returns (second class parameter) another one.
 */
@FunctionalInterface
public interface ConsumingSupplier<C, R> {

    R get(C consumes);

}
