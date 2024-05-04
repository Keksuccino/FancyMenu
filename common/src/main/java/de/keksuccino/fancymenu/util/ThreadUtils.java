package de.keksuccino.fancymenu.util;

//TODO übernehmen (animation update)
public class ThreadUtils {

    public static void sleep(long millis) {
        try {
             Thread.sleep(millis);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
