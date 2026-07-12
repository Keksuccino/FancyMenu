/**
 * Runtime helpers called by code merged into Minecraft client classes. This package must remain outside every package root owned by a Mixin configuration, because Mixin rejects direct loading of ordinary classes below those roots. Types and members used by mixins must remain public because their callers execute from transformed {@code net.minecraft} classes.
 */
package de.keksuccino.fancymenu.mixin.support.client;
