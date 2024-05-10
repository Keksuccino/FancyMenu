package de.keksuccino.fancymenu.util;

public class ThreadUtils {

    public static void sleep(long millis) {
        try {
             Thread.sleep(millis);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
