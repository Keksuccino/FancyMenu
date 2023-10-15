package de.keksuccino.fancymenu.util.resources;

import java.io.Closeable;

public interface Resource extends Closeable {

    void reload();

}
