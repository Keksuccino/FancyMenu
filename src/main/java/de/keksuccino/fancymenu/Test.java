package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.playerentity.PlayerEntityCache;

import java.io.File;

public class Test {

    public static void main(String[] args) {

        System.out.println("SHA-1: " + PlayerEntityCache.calculateSHA1(new File("image.jpg")));

    }

}
