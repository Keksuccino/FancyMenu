package de.keksuccino.fancymenu.util.auth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.CloseableUtils;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthVerifier {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Retrieves the access token for the current Minecraft session using reflection.
     * In Minecraft 1.19.2 with Microsoft account integration the access token is not publicly exposed.
     *
     * @return the access token if available; otherwise, returns null.
     */
    public static String getAccessToken() {
        return Minecraft.getInstance().getUser().getAccessToken();
    }

    /**
     * Retrieves the UUID (player ID) for the current Minecraft session.
     *
     * @return the player's UUID as a String if available; otherwise, returns null.
     */
    public static UUID getUUID() {
        return Minecraft.getInstance().getUser().getGameProfile().getId();
    }

    /**
     * Checks if the current client account appears to be running in cracked mode.<br>
     * This is determined by verifying the access token against the official authentication server.
     *
     * @return true if the client appears to be cracked or not authenticated properly; false if legit.
     */
    public static boolean isCracked() {
        boolean cracked = _isCracked();
        if (cracked) LOGGER.warn("[FANCYMENU] The client was identified as CRACKED client! This is bad!");
        return cracked;
    }

    /**
     * Checks if the current client account appears to be running in cracked mode.<br>
     * This is determined by verifying the access token against the official authentication server.
     *
     * @return true if the client appears to be cracked or not authenticated properly; false if legit.
     */
    private static boolean _isCracked() {

        String accessToken = getAccessToken();
        UUID expectedUUID = getUUID();

        // If no access token or UUID is available, assume offline/cracked mode.
        if ((accessToken == null) || accessToken.isBlank() || (expectedUUID == null) || expectedUUID.toString().isBlank()) {
            return true;
        }

        try {

            // The official endpoint to retrieve the Minecraft profile.
            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // Set the Authorization header using the Bearer token.
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Read the response from the authentication server.
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = reader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            reader.close();

            // Use Gson to parse the JSON response.
            JsonObject json = GSON.fromJson(responseStrBuilder.toString(), JsonObject.class);
            JsonElement uuidElement = json.get("id");

            // If UUID field is null, return "cracked".
            if (uuidElement == null) return true;

            String uuid = uuidElement.getAsString();

            // If the returned UUID does not match the client session UUID, assume the client is not properly authenticated.
            boolean cracked = !uuid.equals(expectedUUID.toString());

            if (!cracked) LOGGER.info("[FANCYMENU] User seems to be a legit (non-cracked) client! This is good!");

            return cracked;

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to check for cracked client!", ex);
        }

        LOGGER.warn("[FANCYMENU] The mod failed to check if the client is cracked! Will return LEGIT as fallback.");

        // In case of any error (network issues, parsing error, etc.), assume offline mode and handle as LEGIT client.
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Forge mods.toml Verification Methods
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the Forge mods.toml file from the /META-INF/ resource folder and returns the value
     * associated with the given field name from the first [[mods]] block.
     *
     * @param fieldName The key to look for (e.g., "authors", "license", "displayName")
     * @return The field value (with quotes removed), or null if not found.
     * @throws AuthException if any error occurs during reading/parsing.
     */
    private static String getForgeModsTomlField(String fieldName) throws AuthException {

        if (!Services.PLATFORM.getPlatformName().equals("forge")) return null;

        String field = null;
        Exception thrown = null;

        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = AuthVerifier.class.getResourceAsStream("/META-INF/mods.toml");
            if (in == null) {
                LOGGER.error("[FANCYMENU] mods.toml not found in resource path.");
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(in));
            boolean inModsBlock = false;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Identify the start of the first mods table.
                if (line.startsWith("[[mods]]")) {
                    inModsBlock = true;
                    continue;
                }
                if (inModsBlock) {
                    // If we hit a new table, exit the mods block.
                    if (line.startsWith("[")) {
                        break;
                    }
                    // Look for the desired field.
                    if (line.startsWith(fieldName)) {
                        // Expect a line like: fieldName = "value"
                        int eqIndex = line.indexOf("=");
                        if (eqIndex != -1) {
                            String valuePart = line.substring(eqIndex + 1).trim();
                            // Remove any surrounding quotes (") if present.
                            if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                                valuePart = valuePart.substring(1, valuePart.length() - 1);
                            }
                            field = valuePart;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            thrown = ex;
        }

        CloseableUtils.closeQuietly(in);
        CloseableUtils.closeQuietly(reader);

        if (thrown != null) {
            throw new AuthException("[FANCYMENU] Failed to get field from mods.toml file!", thrown);
        }

        return field;
    }

    /**
     * Checks if the "authors" field in mods.toml matches the given expected value.
     *
     * @param expected The expected value for authors.
     * @return true if the authors field equals the expected value, false otherwise.
     */
    public static boolean checkForgeAuthorsMeta(String expected) {
        try {
            String authors = getForgeModsTomlField("authors");
            if (authors == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve authors field from mods.toml!");
                return false;
            }
            return authors.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the authors field from the mods.toml file!", ex);
        }
        return true;
    }

    /**
     * Checks if the "license" field in mods.toml matches the given expected value.
     *
     * @param expected The expected value for license.
     * @return true if the license field equals the expected value, false otherwise.
     */
    public static boolean checkForgeLicenseMeta(String expected) {
        try {
            String license = getForgeModsTomlField("license");
            if (license == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve license field from mods.toml!");
                return false;
            }
            return license.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the license field from the mods.toml file!", ex);
        }
        return true;
    }

    /**
     * Checks if the "displayName" field in mods.toml matches the given expected value.
     *
     * @param expected The expected value for displayName.
     * @return true if the displayName field equals the expected value, false otherwise.
     */
    public static boolean checkForgeDisplayNameMeta(String expected) {
        try {
            String displayName = getForgeModsTomlField("displayName");
            if (displayName == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve displayName field from mods.toml!");
                return false;
            }
            return displayName.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the displayName field from the mods.toml file!", ex);
        }
        return true;
    }

    /**
     * Checks if the "modId" field in mods.toml matches the given expected value.
     *
     * @param expected The expected value for modId.
     * @return true if the modId field equals the expected value, false otherwise.
     */
    public static boolean checkForgeModIdMeta(String expected) {
        try {
            String modId = getForgeModsTomlField("modId");
            if (modId == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve modId field from mods.toml!");
                return false;
            }
            return modId.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the modId field from the mods.toml file!", ex);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fabric fabric.mod.json Verification Methods
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the fabric.mod.json file from the resource folder and returns the value
     * associated with the given field name.
     *
     * @param fieldName The key to look for (e.g., "id", "name", "description", "license", "authors")
     * @return The field value as a String, or null if not found.
     * @throws AuthException if any error occurs during reading/parsing.
     */
    private static String getFabricModJsonField(String fieldName) throws AuthException {

        if (!Services.PLATFORM.getPlatformName().equals("fabric")) return null;

        Exception thrown = null;
        String field = null;

        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = AuthVerifier.class.getResourceAsStream("/fabric.mod.json");
            if (in == null) {
                LOGGER.error("[FANCYMENU] fabric.mod.json not found in resource path.");
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(in));
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            JsonElement element = json.get(fieldName);
            if (element == null) {
                return null;
            }
            // For authors, which is typically an array, join into a comma-separated string.
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : array) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(el.getAsString());
                }
                field = sb.toString();
            }
            if (element.isJsonPrimitive()) {
                field = element.getAsString();
            }
            field = element.toString();
        } catch (Exception ex) {
            thrown = ex;
        }

        CloseableUtils.closeQuietly(in);
        CloseableUtils.closeQuietly(reader);

        if (thrown != null) {
            throw new AuthException("[FANCYMENU] Failed to get field from fabric.mod.json file!", thrown);
        }

        return field;

    }

    /**
     * Checks if the "id" field in fabric.mod.json matches the given expected value.
     *
     * @param expected The expected value for id.
     * @return true if the id field equals the expected value, false otherwise.
     */
    public static boolean checkFabricModIdMeta(String expected) {
        try {
            String id = getFabricModJsonField("id");
            if (id == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve id field from fabric.mod.json!");
                return false;
            }
            return id.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the id field from fabric.mod.json!", ex);
        }
        return true;
    }

    /**
     * Checks if the "name" field in fabric.mod.json matches the given expected value.
     *
     * @param expected The expected value for name.
     * @return true if the name field equals the expected value, false otherwise.
     */
    public static boolean checkFabricNameMeta(String expected) {
        try {
            String name = getFabricModJsonField("name");
            if (name == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve name field from fabric.mod.json!");
                return false;
            }
            return name.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the name field from fabric.mod.json!", ex);
        }
        return true;
    }

    /**
     * Checks if the "description" field in fabric.mod.json matches the given expected value.
     *
     * @param expected The expected value for description.
     * @return true if the description field equals the expected value, false otherwise.
     */
    public static boolean checkFabricDescriptionMeta(String expected) {
        try {
            String description = getFabricModJsonField("description");
            if (description == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve description field from fabric.mod.json!");
                return false;
            }
            return description.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the description field from fabric.mod.json!", ex);
        }
        return true;
    }

    /**
     * Checks if the "license" field in fabric.mod.json matches the given expected value.
     *
     * @param expected The expected value for license.
     * @return true if the license field equals the expected value, false otherwise.
     */
    public static boolean checkFabricLicenseMeta(String expected) {
        try {
            String license = getFabricModJsonField("license");
            if (license == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve license field from fabric.mod.json!");
                return false;
            }
            return license.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the license field from fabric.mod.json!", ex);
        }
        return true;
    }

    /**
     * Checks if the "authors" field in fabric.mod.json matches the given expected value.
     *
     * @param expected The expected value for authors.
     * @return true if the authors field equals the expected value, false otherwise.
     */
    public static boolean checkFabricAuthorsMeta(String expected) {
        try {
            String authors = getFabricModJsonField("authors");
            if (authors == null) {
                LOGGER.error("[FANCYMENU] Could not retrieve authors field from fabric.mod.json!");
                return false;
            }
            return authors.equals(expected);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] An error happened while trying to extract the authors field from fabric.mod.json!", ex);
        }
        return true;
    }

    /**
     * Prints all important fields for debugging purposes.
     * This includes authentication details and mod metadata depending on the platform.
     */
    public static void printImportantFields() {

        LOGGER.info("");
        LOGGER.info("======== FANCYMENU DEBUG INFO ========");

        // Authentication details
        LOGGER.info("Access Token: " + getAccessToken());
        LOGGER.info("UUID: " + getUUID());
        LOGGER.info("Is Cracked: " + isCracked());

        String platform = Services.PLATFORM.getPlatformName();
        LOGGER.info("Platform: " + platform);

        if ("forge".equals(platform)) {
            try {
                LOGGER.info("Forge - authors: " + getForgeModsTomlField("authors"));
                LOGGER.info("Forge - license: " + getForgeModsTomlField("license"));
                LOGGER.info("Forge - displayName: " + getForgeModsTomlField("displayName"));
            } catch (AuthException e) {
                LOGGER.error("Failed to retrieve Forge mod metadata", e);
            }
        } else if ("fabric".equals(platform)) {
            try {
                LOGGER.info("Fabric - id: " + getFabricModJsonField("id"));
                LOGGER.info("Fabric - name: " + getFabricModJsonField("name"));
                LOGGER.info("Fabric - description: " + getFabricModJsonField("description"));
                LOGGER.info("Fabric - license: " + getFabricModJsonField("license"));
                LOGGER.info("Fabric - authors: " + getFabricModJsonField("authors"));
            } catch (AuthException e) {
                LOGGER.error("Failed to retrieve Fabric mod metadata", e);
            }
        } else {
            LOGGER.info("No mod metadata available for unknown platform: " + platform);
        }

        LOGGER.info("======================================");
        LOGGER.info("");

    }

}
