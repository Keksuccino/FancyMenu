package de.keksuccino.fancymenu.util;

interface InternetAvailabilityProbe extends AutoCloseable {

    boolean isAvailable() throws Exception;

    @Override
    void close();
}
