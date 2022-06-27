//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.visibilityrequirements;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.VisibilityRequirementsPopup;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class LegacyVisibilityRequirements {

    public static List<VisibilityRequirementsScreen.Requirement> getLegacyRequirements(VisibilityRequirementsScreen screen, VisibilityRequirementContainer container) {
        
        List<VisibilityRequirementsScreen.Requirement> requirements = new ArrayList<>();

        CharacterFilter integerCharFilter = CharacterFilter.getIntegerCharacterFiler();

        /** Singleplayer **/
        String singleplayerName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.singleplayer");
        String singleplayerDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.singleplayer.desc");
        VisibilityRequirementsScreen.Requirement singleplayer = new VisibilityRequirementsScreen.Requirement(screen, singleplayerName, singleplayerDesc, null, container.vrCheckForSingleplayer, container.vrShowIfSingleplayer,
                (enabledCallback) -> {
                    container.vrCheckForSingleplayer = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfSingleplayer = showIfCallback;
        }, null, null, null);
        requirements.add(singleplayer);

        /** Multiplayer **/
        String multiplayerName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.multiplayer");
        String multiplayerDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.multiplayer.desc");
        VisibilityRequirementsScreen.Requirement multiplayer = new VisibilityRequirementsScreen.Requirement(screen, multiplayerName, multiplayerDesc, null, container.vrCheckForMultiplayer, container.vrShowIfMultiplayer,
                (enabledCallback) -> {
                    container.vrCheckForMultiplayer = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfMultiplayer = showIfCallback;
        }, null, null, null);
        requirements.add(multiplayer);

        /** World Loaded **/
        String worldLoadedName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded");
        String worldLoadedDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded.desc");
        VisibilityRequirementsScreen.Requirement worldLoaded = new VisibilityRequirementsScreen.Requirement(screen, worldLoadedName, worldLoadedDesc, null, container.vrCheckForWorldLoaded, container.vrShowIfWorldLoaded,
                (enabledCallback) -> {
                    container.vrCheckForWorldLoaded = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfWorldLoaded = showIfCallback;
        }, null, null, null);
        requirements.add(worldLoaded);

        /** Is Real Time Hour **/
        String realTimeHourValuePreset = "";
        for (int i : container.vrRealTimeHour) {
            realTimeHourValuePreset += i + ",";
        }
        if (realTimeHourValuePreset.length() > 0) {
            realTimeHourValuePreset = realTimeHourValuePreset.substring(0, realTimeHourValuePreset.length() -1);
        } else {
            realTimeHourValuePreset = "1, 4";
        }
        String realTimeHourName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour");
        String realTimeHourDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour.desc");
        String realTimeHourValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour.valuename");
        VisibilityRequirementsScreen.Requirement realTimeHour = new VisibilityRequirementsScreen.Requirement(screen, realTimeHourName, realTimeHourDesc, realTimeHourValueName, container.vrCheckForRealTimeHour, container.vrShowIfRealTimeHour,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeHour = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeHour = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeHour.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeHour.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeHour.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeHourValuePreset);
        requirements.add(realTimeHour);

        /** Is Real Time Minute **/
        String realTimeMinuteValuePreset = "";
        for (int i : container.vrRealTimeMinute) {
            realTimeMinuteValuePreset += i + ",";
        }
        if (realTimeMinuteValuePreset.length() > 0) {
            realTimeMinuteValuePreset = realTimeMinuteValuePreset.substring(0, realTimeMinuteValuePreset.length() -1);
        } else {
            realTimeMinuteValuePreset = "1, 4";
        }
        String realTimeMinuteName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute");
        String realTimeMinuteDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.desc");
        String realTimeMinuteValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.valuename");
        VisibilityRequirementsScreen.Requirement realTimeMinute = new VisibilityRequirementsScreen.Requirement(screen, realTimeMinuteName, realTimeMinuteDesc, realTimeMinuteValueName, container.vrCheckForRealTimeMinute, container.vrShowIfRealTimeMinute,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeMinute = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeMinute = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeMinute.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeMinute.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeMinute.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeMinuteValuePreset);
        requirements.add(realTimeMinute);

        /** Is Real Time Second **/
        String realTimeSecondValuePreset = "";
        for (int i : container.vrRealTimeSecond) {
            realTimeSecondValuePreset += i + ",";
        }
        if (realTimeSecondValuePreset.length() > 0) {
            realTimeSecondValuePreset = realTimeSecondValuePreset.substring(0, realTimeSecondValuePreset.length() -1);
        } else {
            realTimeSecondValuePreset = "1, 4";
        }
        String realTimeSecondName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond");
        String realTimeSecondDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond.desc");
        String realTimeSecondValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond.valuename");
        VisibilityRequirementsScreen.Requirement realTimeSecond = new VisibilityRequirementsScreen.Requirement(screen, realTimeSecondName, realTimeSecondDesc, realTimeSecondValueName, container.vrCheckForRealTimeSecond, container.vrShowIfRealTimeSecond,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeSecond = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeSecond = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeSecond.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeSecond.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeSecond.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeSecondValuePreset);
        requirements.add(realTimeSecond);

        /** Is Real Time Day **/
        String realTimeDayValuePreset = "";
        for (int i : container.vrRealTimeDay) {
            realTimeDayValuePreset += i + ",";
        }
        if (realTimeDayValuePreset.length() > 0) {
            realTimeDayValuePreset = realTimeDayValuePreset.substring(0, realTimeDayValuePreset.length() -1);
        } else {
            realTimeDayValuePreset = "1, 4";
        }
        String realTimeDayName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeday");
        String realTimeDayDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeday.desc");
        String realTimeDayValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeday.valuename");
        VisibilityRequirementsScreen.Requirement realTimeDay = new VisibilityRequirementsScreen.Requirement(screen, realTimeDayName, realTimeDayDesc, realTimeDayValueName, container.vrCheckForRealTimeDay, container.vrShowIfRealTimeDay,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeDay = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeDay = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeDay.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeDay.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeDay.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeDayValuePreset);
        requirements.add(realTimeDay);

        /** Is Real Time Month **/
        String realTimeMonthValuePreset = "";
        for (int i : container.vrRealTimeMonth) {
            realTimeMonthValuePreset += i + ",";
        }
        if (realTimeMonthValuePreset.length() > 0) {
            realTimeMonthValuePreset = realTimeMonthValuePreset.substring(0, realTimeMonthValuePreset.length() -1);
        } else {
            realTimeMonthValuePreset = "1, 4";
        }
        String realTimeMonthName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth");
        String realTimeMonthDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth.desc");
        String realTimeMonthValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimemonth.valuename");
        VisibilityRequirementsScreen.Requirement realTimeMonth = new VisibilityRequirementsScreen.Requirement(screen, realTimeMonthName, realTimeMonthDesc, realTimeMonthValueName, container.vrCheckForRealTimeMonth, container.vrShowIfRealTimeMonth,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeMonth = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeMonth = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeMonth.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeMonth.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeMonth.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeMonthValuePreset);
        requirements.add(realTimeMonth);

        /** Is Real Time Year **/
        String realTimeYearValuePreset = "";
        for (int i : container.vrRealTimeYear) {
            realTimeYearValuePreset += i + ",";
        }
        if (realTimeYearValuePreset.length() > 0) {
            realTimeYearValuePreset = realTimeYearValuePreset.substring(0, realTimeYearValuePreset.length() -1);
        } else {
            realTimeYearValuePreset = "1, 4";
        }
        String realTimeYearName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeyear");
        String realTimeYearDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeyear.desc");
        String realTimeYearValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeyear.valuename");
        VisibilityRequirementsScreen.Requirement realTimeYear = new VisibilityRequirementsScreen.Requirement(screen, realTimeYearName, realTimeYearDesc, realTimeYearValueName, container.vrCheckForRealTimeYear, container.vrShowIfRealTimeYear,
                (enabledCallback) -> {
                    container.vrCheckForRealTimeYear = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfRealTimeYear = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrRealTimeYear.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrRealTimeYear.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrRealTimeYear.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeYearValuePreset);
        requirements.add(realTimeYear);

        /** Is Window Width **/
        String windowWidthValuePreset = "";
        for (int i : container.vrWindowWidth) {
            windowWidthValuePreset += i + ",";
        }
        if (windowWidthValuePreset.length() > 0) {
            windowWidthValuePreset = windowWidthValuePreset.substring(0, windowWidthValuePreset.length() -1);
        } else {
            windowWidthValuePreset = "503, 918, 1920";
        }
        String windowWidthName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth");
        String windowWidthDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth.desc");
        String windowWidthValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth.valuename");
        VisibilityRequirementsScreen.Requirement windowWidth = new VisibilityRequirementsScreen.Requirement(screen, windowWidthName, windowWidthDesc, windowWidthValueName, container.vrCheckForWindowWidth, container.vrShowIfWindowWidth,
                (enabledCallback) -> {
                    container.vrCheckForWindowWidth = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfWindowWidth = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrWindowWidth.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrWindowWidth.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrWindowWidth.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, windowWidthValuePreset);
        requirements.add(windowWidth);

        /** Is Window Height **/
        String windowHeightValuePreset = "";
        for (int i : container.vrWindowHeight) {
            windowHeightValuePreset += i + ",";
        }
        if (windowHeightValuePreset.length() > 0) {
            windowHeightValuePreset = windowHeightValuePreset.substring(0, windowHeightValuePreset.length() -1);
        } else {
            windowHeightValuePreset = "410, 634, 1080";
        }
        String windowHeightName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight");
        String windowHeightDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.desc");
        String windowHeightValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.valuename");
        VisibilityRequirementsScreen.Requirement windowHeight = new VisibilityRequirementsScreen.Requirement(screen, windowHeightName, windowHeightDesc, windowHeightValueName, container.vrCheckForWindowHeight, container.vrShowIfWindowHeight,
                (enabledCallback) -> {
                    container.vrCheckForWindowHeight = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfWindowHeight = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrWindowHeight.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            container.vrWindowHeight.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        container.vrWindowHeight.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, windowHeightValuePreset);
        requirements.add(windowHeight);

        /** Window Width Bigger Than **/
        String windowWidthBiggerThanName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan");
        String windowWidthBiggerThanDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.desc");
        String windowWidthBiggerThanValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.valuename");
        VisibilityRequirementsScreen.Requirement windowWidthBiggerThan = new VisibilityRequirementsScreen.Requirement(screen, windowWidthBiggerThanName, windowWidthBiggerThanDesc, windowWidthBiggerThanValueName, container.vrCheckForWindowWidthBiggerThan, container.vrShowIfWindowWidthBiggerThan,
                (enabledCallback) -> {
                    container.vrCheckForWindowWidthBiggerThan = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfWindowWidthBiggerThan = showIfCallback;
        }, (valueCallback) -> {
            if ((valueCallback != null) && MathUtils.isInteger(valueCallback)) {
                container.vrWindowWidthBiggerThan = Integer.parseInt(valueCallback);
            } else {
                container.vrWindowWidthBiggerThan = 0;
            }
        }, integerCharFilter, "" + container.vrWindowWidthBiggerThan);
        requirements.add(windowWidthBiggerThan);

        /** Window Height Bigger Than **/
        String windowHeightBiggerThanName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan");
        String windowHeightBiggerThanDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan.desc");
        String windowHeightBiggerThanValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan.valuename");
        VisibilityRequirementsScreen.Requirement windowHeightBiggerThan = new VisibilityRequirementsScreen.Requirement(screen, windowHeightBiggerThanName, windowHeightBiggerThanDesc, windowHeightBiggerThanValueName, container.vrCheckForWindowHeightBiggerThan, container.vrShowIfWindowHeightBiggerThan,
                (enabledCallback) -> {
                    container.vrCheckForWindowHeightBiggerThan = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfWindowHeightBiggerThan = showIfCallback;
        }, (valueCallback) -> {
            if ((valueCallback != null) && MathUtils.isInteger(valueCallback)) {
                container.vrWindowHeightBiggerThan = Integer.parseInt(valueCallback);
            } else {
                container.vrWindowHeightBiggerThan = 0;
            }
        }, integerCharFilter, "" + container.vrWindowHeightBiggerThan);
        requirements.add(windowHeightBiggerThan);

        /** Button Hovered **/
        String buttonHoveredName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered");
        String buttonHoveredDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered.desc");
        String buttonHoveredValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered.valuename");
        String buttonHoveredValuePreset = "";
        if (container.vrButtonHovered != null) {
            buttonHoveredValuePreset = container.vrButtonHovered;
        }
        VisibilityRequirementsScreen.Requirement buttonHovered = new VisibilityRequirementsScreen.Requirement(screen, buttonHoveredName, buttonHoveredDesc, buttonHoveredValueName, container.vrCheckForButtonHovered, container.vrShowIfButtonHovered,
                (enabledCallback) -> {
                    container.vrCheckForButtonHovered = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfButtonHovered = showIfCallback;
        }, (valueCallback) -> {
            container.vrButtonHovered = valueCallback;
        }, null, buttonHoveredValuePreset);
        requirements.add(buttonHovered);

        /** Language **/
        String languageName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language");
        String languageDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.desc");
        String languageValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.valuename");
        String languageValuePreset = Minecraft.getInstance().options.languageCode;
        if (container.vrLanguage != null) {
            languageValuePreset = container.vrLanguage;
        }
        VisibilityRequirementsScreen.Requirement language = new VisibilityRequirementsScreen.Requirement(screen, languageName, languageDesc, languageValueName, container.vrCheckForLanguage, container.vrShowIfLanguage,
                (enabledCallback) -> {
                    container.vrCheckForLanguage = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfLanguage = showIfCallback;
        }, (valueCallback) -> {
            container.vrLanguage = valueCallback;
        }, null, languageValuePreset);
        requirements.add(language);

        /** Fullscreen **/
        String fullscreenName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.fullscreen");
        String fullscreenDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.fullscreen.desc");
        VisibilityRequirementsScreen.Requirement fullscreen = new VisibilityRequirementsScreen.Requirement(screen, fullscreenName, fullscreenDesc, null, container.vrCheckForFullscreen, container.vrShowIfFullscreen,
                (enabledCallback) -> {
                    container.vrCheckForFullscreen = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfFullscreen = showIfCallback;
        }, null, null, null);
        requirements.add(fullscreen);

        /** OS Windows **/
        String osWindowsName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows");
        String osWindowsDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows.desc");
        VisibilityRequirementsScreen.Requirement osWindows = new VisibilityRequirementsScreen.Requirement(screen, osWindowsName, osWindowsDesc, null, container.vrCheckForOsWindows, container.vrShowIfOsWindows,
                (enabledCallback) -> {
                    container.vrCheckForOsWindows = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfOsWindows = showIfCallback;
        }, null, null, null);
        requirements.add(osWindows);

        /** OS Mac **/
        String osMacName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.osmac");
        String osMacDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.osmac.desc");
        VisibilityRequirementsScreen.Requirement osMac = new VisibilityRequirementsScreen.Requirement(screen, osMacName, osMacDesc, null, container.vrCheckForOsMac, container.vrShowIfOsMac,
                (enabledCallback) -> {
                    container.vrCheckForOsMac = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfOsMac = showIfCallback;
        }, null, null, null);
        requirements.add(osMac);

        /** OS Linux **/
        String osLinuxName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oslinux");
        String osLinuxDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oslinux.desc");
        VisibilityRequirementsScreen.Requirement osLinux = new VisibilityRequirementsScreen.Requirement(screen, osLinuxName, osLinuxDesc, null, container.vrCheckForOsLinux, container.vrShowIfOsLinux,
                (enabledCallback) -> {
                    container.vrCheckForOsLinux = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfOsLinux = showIfCallback;
        }, null, null, null);
        requirements.add(osLinux);

        /** Is Mod Loaded **/
        String modLoadedValuePreset = "";
        for (String s : container.vrModLoaded) {
            modLoadedValuePreset += s + ",";
        }
        if (modLoadedValuePreset.length() > 0) {
            modLoadedValuePreset = modLoadedValuePreset.substring(0, modLoadedValuePreset.length() -1);
        } else {
            modLoadedValuePreset = "fancymenu, optifine, somemodid";
        }
        String modLoadedName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded");
        String modLoadedDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.desc");
        String modLoadedValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.valuename");
        VisibilityRequirementsScreen.Requirement modLoaded = new VisibilityRequirementsScreen.Requirement(screen, modLoadedName, modLoadedDesc, modLoadedValueName, container.vrCheckForModLoaded, container.vrShowIfModLoaded,
                (enabledCallback) -> {
                    container.vrCheckForModLoaded = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfModLoaded = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrModLoaded.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (!s.equals("")) {
                            container.vrModLoaded.add(s);
                        }
                    }
                } else {
                    if (!valueCallback.replace(" ", "").equals("")) {
                        container.vrModLoaded.add(valueCallback.replace(" ", ""));
                    }
                }
            }
        }, null, modLoadedValuePreset);
        requirements.add(modLoaded);

        /** Server Online **/
        String serverOnlineName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline");
        String serverOnlineDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline.desc");
        String serverOnlineValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline.valuename");
        String serverOnlineValuePreset = "mycoolserver.com";
        if (container.vrServerOnline != null) {
            serverOnlineValuePreset = container.vrServerOnline;
        }
        VisibilityRequirementsScreen.Requirement serverOnline = new VisibilityRequirementsScreen.Requirement(screen, serverOnlineName, serverOnlineDesc, serverOnlineValueName, container.vrCheckForServerOnline, container.vrShowIfServerOnline,
                (enabledCallback) -> {
                    container.vrCheckForServerOnline = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfServerOnline = showIfCallback;
        }, (valueCallback) -> {
            container.vrServerOnline = valueCallback;
        }, null, serverOnlineValuePreset);
        requirements.add(serverOnline);

        /** Is Gui Scale **/
        String guiScaleValuePreset = "";
        for (String condition : container.vrGuiScale) {
            if (condition.startsWith("double:")) {
                String value = condition.replace("double:", "");
                guiScaleValuePreset += value + ",";
            } else if (condition.startsWith("biggerthan:")) {
                String value = condition.replace("biggerthan:", "");
                guiScaleValuePreset += ">" + value + ",";
            } else if (condition.startsWith("smallerthan:")) {
                String value = condition.replace("smallerthan:", "");
                guiScaleValuePreset += "<" + value + ",";
            }
        }
        if (guiScaleValuePreset.length() > 0) {
            guiScaleValuePreset = guiScaleValuePreset.substring(0, guiScaleValuePreset.length() -1);
        } else {
            guiScaleValuePreset = ">1.20,<3.0";
        }
        String guiScaleName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale");
        String guiScaleDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.desc");
        String guiScaleValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.valuename");
        VisibilityRequirementsScreen.Requirement guiScale = new VisibilityRequirementsScreen.Requirement(screen, guiScaleName, guiScaleDesc, guiScaleValueName, container.vrCheckForGuiScale, container.vrShowIfGuiScale,
                (enabledCallback) -> {
                    container.vrCheckForGuiScale = enabledCallback;
                }, (showIfCallback) -> {
            container.vrShowIfGuiScale = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                container.vrGuiScale.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        container.vrGuiScale.add(s);
                    }
                } else {
                    if (valueCallback.length() > 0) {
                        container.vrGuiScale.add(valueCallback.replace(" ", ""));
                    }
                }
                List<String> l = new ArrayList<>();
                for (String s : container.vrGuiScale) {
                    if (MathUtils.isDouble(s)) {
                        l.add("double:" + s);
                    } else {
                        if (s.startsWith(">")) {
                            String value = s.split("[>]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("biggerthan:" + value);
                            }
                        } else if (s.startsWith("<")) {
                            String value = s.split("[<]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("smallerthan:" + value);
                            }
                        }
                    }
                }
                container.vrGuiScale = l;
            }
        }, null, guiScaleValuePreset);
        requirements.add(guiScale);

        return requirements;

    }

}
