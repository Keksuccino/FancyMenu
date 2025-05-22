package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.Poop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading the Tamagotchi Buddy's state to and from persistent storage.
 */
public class TamagotchiBuddySerializer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILENAME = "buddy_save.json";
    private static final File BUDDY_DIR = new File(FancyMenu.INSTANCE_DATA_DIR, "buddy");

    static {
        // Create buddy directory if it doesn't exist
        if (!BUDDY_DIR.exists()) {
            BUDDY_DIR.mkdirs();
            LOGGER.info("Created buddy save directory at {}", BUDDY_DIR.getAbsolutePath());
        }
    }

    /**
     * Saves the Tamagotchi Buddy's state to a file.
     *
     * @param buddy The buddy instance to save.
     */
    public static void saveBuddy(TamagotchiBuddy buddy) {
        try {
            File saveFile = new File(BUDDY_DIR, SAVE_FILENAME);
            JsonObject json = new JsonObject();

            // Save basic stats
            json.addProperty("hunger", buddy.getHunger());
            json.addProperty("happiness", buddy.getHappiness());
            json.addProperty("energy", buddy.getEnergy());
            json.addProperty("funLevel", buddy.getFunLevel());
            json.addProperty("isPeeking", buddy.isPeeking);
            json.addProperty("hasBeenAwakened", buddy.hasBeenAwakened);
            json.addProperty("isActivelyPeeking", buddy.isActivelyPeeking);
            json.addProperty("peekTimer", buddy.peekTimer);
            json.addProperty("peekDuration", buddy.peekDuration);
            
            // Save screen dimensions
            json.addProperty("screenWidth", buddy.getScreenWidth());
            json.addProperty("screenHeight", buddy.getScreenHeight());
            
            // Save poop locations
            JsonArray poopsArray = new JsonArray();
            for (Poop poop : buddy.getPoops()) {
                JsonObject poopObj = new JsonObject();
                poopObj.addProperty("x", poop.getX());
                poopObj.addProperty("y", poop.getY());
                poopsArray.add(poopObj);
            }
            json.add("poops", poopsArray);

            // Write to file
            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(json, writer);
            }
            
            LOGGER.info("Saved buddy data to {}", saveFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save buddy data", e);
        }
    }

    /**
     * Loads the Tamagotchi Buddy's state from a file.
     *
     * @param buddy The buddy instance to load data into.
     * @return True if data was successfully loaded, false otherwise.
     */
    public static boolean loadBuddy(TamagotchiBuddy buddy) {
        File saveFile = new File(BUDDY_DIR, SAVE_FILENAME);
        if (!saveFile.exists()) {
            LOGGER.info("No buddy save file found at {}", saveFile.getAbsolutePath());
            return false;
        }

        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            // Load basic stats
            if (json.has("hunger")) {
                buddy.setHunger(json.get("hunger").getAsFloat());
            }
            if (json.has("happiness")) {
                buddy.setHappiness(json.get("happiness").getAsFloat());
            }
            if (json.has("energy")) {
                buddy.setEnergy(json.get("energy").getAsFloat());
            }
            if (json.has("funLevel")) {
                buddy.setFunLevel(json.get("funLevel").getAsFloat());
            }
            if (json.has("isPeeking")) {
                buddy.isPeeking = json.get("isPeeking").getAsBoolean();
            }
            if (json.has("hasBeenAwakened")) {
                buddy.hasBeenAwakened = json.get("hasBeenAwakened").getAsBoolean();
            }
            if (json.has("isActivelyPeeking")) {
                buddy.isActivelyPeeking = json.get("isActivelyPeeking").getAsBoolean();
            }
            if (json.has("peekTimer")) {
                buddy.peekTimer = json.get("peekTimer").getAsInt();
            }
            if (json.has("peekDuration")) {
                buddy.peekDuration = json.get("peekDuration").getAsInt();
            }
            
            // Load poop locations
            if (json.has("poops")) {
                JsonArray poopsArray = json.getAsJsonArray("poops");
                List<Poop> poops = new ArrayList<>();
                
                // Get current and saved screen dimensions for scaling
                int currentWidth = buddy.getScreenWidth();
                int currentHeight = buddy.getScreenHeight();
                int savedWidth = json.has("screenWidth") ? json.get("screenWidth").getAsInt() : currentWidth;
                int savedHeight = json.has("screenHeight") ? json.get("screenHeight").getAsInt() : currentHeight;
                
                // Log screen dimensions difference for debugging
                if (savedWidth != currentWidth || savedHeight != currentHeight) {
                    LOGGER.info("Screen size changed since last save: {}x{} -> {}x{}", 
                                savedWidth, savedHeight, currentWidth, currentHeight);
                }
                
                for (int i = 0; i < poopsArray.size(); i++) {
                    JsonObject poopObj = poopsArray.get(i).getAsJsonObject();
                    int savedX = poopObj.get("x").getAsInt();
                    int savedY = poopObj.get("y").getAsInt();
                    
                    // Skip poops with extreme invalid values
                    boolean isInvalid = savedX < 0 || savedY < 0 || 
                                       savedX == Integer.MAX_VALUE || savedY == Integer.MAX_VALUE ||
                                       savedX > 10000 || savedY > 10000;
                    
                    if (isInvalid) {
                        LOGGER.debug("Skipped loading invalid poop at ({}, {})", savedX, savedY);
                        continue;
                    }
                    
                    // Adjust coordinates based on screen size change if needed
                    int x = savedX;
                    int y = savedY;
                    
                    if (savedWidth > 0 && savedHeight > 0 && 
                        (savedWidth != currentWidth || savedHeight != currentHeight)) {
                        // Scale coordinates proportionally to new screen size
                        float xRatio = (float)savedX / savedWidth;
                        float yRatio = (float)savedY / savedHeight;
                        
                        x = (int)(xRatio * currentWidth);
                        y = (int)(yRatio * currentHeight);
                        
                        LOGGER.debug("Scaled poop from ({}, {}) to ({}, {})", savedX, savedY, x, y);
                    }
                    
                    Poop poop = new Poop(x, y, buddy);
                    poops.add(poop);
                    LOGGER.debug("Loaded valid poop at ({}, {})", x, y);
                }
                
                LOGGER.info("Loaded {} valid poops out of {} saved poops", poops.size(), poopsArray.size());
                buddy.setPoops(poops);
            }
            
            LOGGER.info("Loaded buddy data from {}", saveFile.getAbsolutePath());
            return true;
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            LOGGER.error("Failed to load buddy data", e);
            return false;
        }
    }

}
