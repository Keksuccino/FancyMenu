
package de.keksuccino.fancymenu.customization.loadingrequirement.v2.internal;

import de.keksuccino.fancymenu.customization.loadingrequirement.v1.VisibilityRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.v2.LoadingRequirementRegistry;
import de.keksuccino.konkrete.properties.PropertiesSection;

public class LegacyRequirementConverter {

    public static void deserializeLegacyAndAddTo(PropertiesSection sec, LoadingRequirementContainer addTo) {

        VisibilityRequirementContainer con = null;
        try {
            con = new VisibilityRequirementContainer(sec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (con == null) {
            return;
        }

        if (con.vrCheckForSingleplayer) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_singpleplayer");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfSingleplayer), addTo));
            }
        }

        if (con.vrCheckForMultiplayer) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_multiplayer");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfMultiplayer), addTo));
            }
        }

        if (con.vrCheckForWindowWidth) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_window_width");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrWindowWidth, getMode(con.vrShowIfWindowWidth), addTo));
            }
        }

        if (con.vrCheckForWindowHeight) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_window_height");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrWindowHeight, getMode(con.vrShowIfWindowHeight), addTo));
            }
        }

        if (con.vrCheckForWindowWidthBiggerThan) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_window_width_bigger_than");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrWindowWidthBiggerThan, getMode(con.vrShowIfWindowWidthBiggerThan), addTo));
            }
        }

        if (con.vrCheckForWindowHeightBiggerThan) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_window_height_bigger_than");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrWindowHeightBiggerThan, getMode(con.vrShowIfWindowHeightBiggerThan), addTo));
            }
        }

        if (con.vrCheckForButtonHovered) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_visibility_requirement_is_element_hovered");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrButtonHovered, getMode(con.vrShowIfButtonHovered), addTo));
            }
        }

        if (con.vrCheckForWorldLoaded) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_world_loaded");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfWorldLoaded), addTo));
            }
        }

        if (con.vrCheckForLanguage) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_language");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrLanguage, getMode(con.vrShowIfLanguage), addTo));
            }
        }

        if (con.vrCheckForFullscreen) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_fullscreen");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfFullscreen), addTo));
            }
        }

        if (con.vrCheckForOsWindows) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_os_windows");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfOsWindows), addTo));
            }
        }

        if (con.vrCheckForOsMac) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_os_macos");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfOsMac), addTo));
            }
        }

        if (con.vrCheckForOsLinux) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_os_linux");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, null, getMode(con.vrShowIfOsLinux), addTo));
            }
        }

        if (con.vrCheckForModLoaded) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_mod_loaded");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrModLoaded, getMode(con.vrShowIfModLoaded), addTo));
            }
        }

        if (con.vrCheckForServerOnline) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_server_online");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrServerOnline, getMode(con.vrShowIfServerOnline), addTo));
            }
        }

        if (con.vrCheckForGuiScale) {
            LoadingRequirement r = LoadingRequirementRegistry.getRequirement("fancymenu_loading_requirement_is_gui_scale");
            if (r != null) {
                addTo.addInstance(new LoadingRequirementInstance(r, con.vrGuiScale, getMode(con.vrShowIfGuiScale), addTo));
            }
        }

        for (VisibilityRequirementContainer.RequirementPackage p : con.customRequirements.values()) {
            LoadingRequirement lr = LoadingRequirementRegistry.getRequirement(p.requirement.getIdentifier());
            if (lr != null) {
                addTo.addInstance(new LoadingRequirementInstance(lr, p.value, getMode(p.showIf), addTo));
            }
        }

    }

    private static LoadingRequirementInstance.RequirementMode getMode(boolean b) {
        return b ? LoadingRequirementInstance.RequirementMode.IF : LoadingRequirementInstance.RequirementMode.IF_NOT;
    }

}
