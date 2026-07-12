package de.keksuccino.fancymenu.customization.widget;

import java.util.*;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenWidgetDiscoverer {

	private static final Logger LOGGER = LogManager.getLogger();

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen) {
		return getWidgetsOfScreen(screen, false);
	}

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, boolean updateScreenSize) {
		int newWidth = screen.width;
		int newHeight = screen.height;
		if (updateScreenSize) {
			newWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
			newHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		}
		return getWidgetsOfScreen(screen, newWidth, newHeight);
	}

	@NotNull
	public static List<WidgetMeta> getWidgetsOfScreen(@NotNull Screen screen, int newWidth, int newHeight) {
		Map<Long, WidgetMeta> widgetMetas = new LinkedHashMap<>();
		try {
			boolean optionsScreen = screen instanceof OptionsScreen;
			List<WidgetMeta> ids = _getWidgetsOfScreen(screen, 1000, 1000);
			if (optionsScreen) WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(ids.stream().map(WidgetMeta::getWidget).toList());
			List<WidgetMeta> buttons = _getWidgetsOfScreen(screen, newWidth, newHeight);
			// The final pass is authoritative, but both passes need semantic IDs so rebuilt or reordered Options widgets can be paired safely.
			if (optionsScreen) WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(buttons.stream().map(WidgetMeta::getWidget).toList());
			for (WidgetMeta meta : reconcileDiscoveryPasses(ids, buttons, !optionsScreen)) {
				if (!widgetMetas.containsKey(meta.getLongIdentifier())) {
					widgetMetas.put(meta.getLongIdentifier(), meta);
				} else {
					LOGGER.warn("[FANCYMENU] Duplicate widget ID found while discovering screen widgets: " + meta.getLongIdentifier(), new IllegalStateException("There can't be multiple widgets with the same identifier!"));
				}
			}
			List<String> universalIdentifiers = new ArrayList<>();
			for (WidgetMeta meta : widgetMetas.values()) {
				WidgetIdentifierHandler.setUniversalIdentifierOfWidgetMeta(meta);
				if (universalIdentifiers.contains(meta.getUniversalIdentifier())) {
					meta.setUniversalIdentifier(null);
				} else {
					universalIdentifiers.add(meta.getUniversalIdentifier());
				}
			}
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to get widgets of screen!", ex);
		}
		return new ArrayList<>(widgetMetas.values());
	}

	/**
	 * Reconciles the stable-size discovery pass with the final-size widgets. Object identity and unique widget identifiers
	 * are safe across rebuilds. List position is retained for other screens and for Options passes whose unique semantic
	 * anchors remain aligned; otherwise an unmatched final widget keeps its final-pass ID instead of inheriting another widget's legacy ID.
	 */
	@NotNull
	static List<WidgetMeta> reconcileDiscoveryPasses(@NotNull List<WidgetMeta> stableSizeMetas, @NotNull List<WidgetMeta> finalSizeMetas, boolean allowPositionalFallback) {
		if (allowPositionalFallback) return reconcileDiscoveryPassesPositionally(stableSizeMetas, finalSizeMetas);

		Map<AbstractWidget, WidgetMeta> stableMetasByWidget = new IdentityHashMap<>();
		Map<String, WidgetMeta> stableMetasByUniqueIdentifier = getMetasByUniqueIdentifier(stableSizeMetas);
		Map<String, WidgetMeta> finalMetasByUniqueIdentifier = getMetasByUniqueIdentifier(finalSizeMetas);
		Map<WidgetMeta, WidgetMeta> matchedStableMetas = new IdentityHashMap<>();
		Set<WidgetMeta> consumedStableMetas = Collections.newSetFromMap(new IdentityHashMap<>());
		for (WidgetMeta stableMeta : stableSizeMetas) stableMetasByWidget.put(stableMeta.getWidget(), stableMeta);

		for (WidgetMeta finalMeta : finalSizeMetas) {
			WidgetMeta stableMeta = stableMetasByWidget.get(finalMeta.getWidget());
			if (stableMeta != null && consumedStableMetas.add(stableMeta)) matchedStableMetas.put(finalMeta, stableMeta);
		}
		for (Map.Entry<String, WidgetMeta> entry : finalMetasByUniqueIdentifier.entrySet()) {
			WidgetMeta finalMeta = entry.getValue();
			WidgetMeta stableMeta = stableMetasByUniqueIdentifier.get(entry.getKey());
			if (!matchedStableMetas.containsKey(finalMeta) && stableMeta != null && consumedStableMetas.add(stableMeta)) matchedStableMetas.put(finalMeta, stableMeta);
		}
		boolean semanticAnchorsAligned = areUniqueIdentifierAnchorsAligned(stableSizeMetas, finalSizeMetas, stableMetasByUniqueIdentifier, finalMetasByUniqueIdentifier);
		if (semanticAnchorsAligned) addUnambiguousPositionalMatches(stableSizeMetas, finalSizeMetas, matchedStableMetas, consumedStableMetas);

		List<WidgetMeta> reconciledMetas = new ArrayList<>();
		Set<Long> reservedStableIdentifiers = new HashSet<>();
		for (WidgetMeta stableMeta : stableSizeMetas) reservedStableIdentifiers.add(stableMeta.getLongIdentifier());
		Set<Long> usedIdentifiers = new HashSet<>();
		for (WidgetMeta finalMeta : finalSizeMetas) {
			WidgetMeta stableMeta = matchedStableMetas.get(finalMeta);
			long availableIdentifier;
			if (stableMeta != null) {
				availableIdentifier = stableMeta.getLongIdentifier();
			} else {
				Set<Long> unavailableIdentifiers = new HashSet<>(reservedStableIdentifiers);
				unavailableIdentifiers.addAll(usedIdentifiers);
				availableIdentifier = generateAvailableIdFromBaseId(finalMeta.getLongIdentifier(), unavailableIdentifiers);
			}
			usedIdentifiers.add(availableIdentifier);
			reconciledMetas.add(new WidgetMeta(finalMeta.getWidget(), availableIdentifier, finalMeta.getScreen()));
		}
		return reconciledMetas;
	}

	@NotNull
	private static List<WidgetMeta> reconcileDiscoveryPassesPositionally(@NotNull List<WidgetMeta> stableSizeMetas, @NotNull List<WidgetMeta> finalSizeMetas) {
		if (stableSizeMetas.size() != finalSizeMetas.size()) return List.of();
		List<WidgetMeta> reconciledMetas = new ArrayList<>();
		for (int i = 0; i < finalSizeMetas.size(); i++) {
			WidgetMeta stableMeta = stableSizeMetas.get(i);
			WidgetMeta finalMeta = finalSizeMetas.get(i);
			reconciledMetas.add(new WidgetMeta(finalMeta.getWidget(), stableMeta.getLongIdentifier(), finalMeta.getScreen()));
		}
		return reconciledMetas;
	}

	private static void addUnambiguousPositionalMatches(@NotNull List<WidgetMeta> stableSizeMetas, @NotNull List<WidgetMeta> finalSizeMetas, @NotNull Map<WidgetMeta, WidgetMeta> matchedStableMetas, @NotNull Set<WidgetMeta> consumedStableMetas) {
		int index = 0;
		while (index < finalSizeMetas.size()) {
			if (!isUnmatchedPositionalCandidate(stableSizeMetas, finalSizeMetas, matchedStableMetas, consumedStableMetas, index)) {
				index++;
				continue;
			}
			int runStart = index;
			while (index < finalSizeMetas.size() && isUnmatchedPositionalCandidate(stableSizeMetas, finalSizeMetas, matchedStableMetas, consumedStableMetas, index)) index++;
			if (index - runStart == 1) {
				WidgetMeta finalMeta = finalSizeMetas.get(runStart);
				WidgetMeta stableMeta = stableSizeMetas.get(runStart);
				matchedStableMetas.put(finalMeta, stableMeta);
				consumedStableMetas.add(stableMeta);
			}
		}
	}

	private static boolean isUnmatchedPositionalCandidate(@NotNull List<WidgetMeta> stableSizeMetas, @NotNull List<WidgetMeta> finalSizeMetas, @NotNull Map<WidgetMeta, WidgetMeta> matchedStableMetas, @NotNull Set<WidgetMeta> consumedStableMetas, int index) {
		return !matchedStableMetas.containsKey(finalSizeMetas.get(index)) && !consumedStableMetas.contains(stableSizeMetas.get(index));
	}

	private static boolean areUniqueIdentifierAnchorsAligned(@NotNull List<WidgetMeta> stableSizeMetas, @NotNull List<WidgetMeta> finalSizeMetas, @NotNull Map<String, WidgetMeta> stableMetasByUniqueIdentifier, @NotNull Map<String, WidgetMeta> finalMetasByUniqueIdentifier) {
		if (stableSizeMetas.size() != finalSizeMetas.size()) return false;
		for (int i = 0; i < stableSizeMetas.size(); i++) {
			String stableIdentifier = getUniqueIdentifier(stableSizeMetas.get(i), stableMetasByUniqueIdentifier);
			String finalIdentifier = getUniqueIdentifier(finalSizeMetas.get(i), finalMetasByUniqueIdentifier);
			if (!Objects.equals(stableIdentifier, finalIdentifier) && (stableIdentifier != null || finalIdentifier != null)) return false;
		}
		return true;
	}

	@Nullable
	private static String getUniqueIdentifier(@NotNull WidgetMeta meta, @NotNull Map<String, WidgetMeta> metasByUniqueIdentifier) {
		if (!(meta.getWidget() instanceof UniqueWidget uniqueWidget)) return null;
		String identifier = uniqueWidget.getWidgetIdentifierFancyMenu();
		return identifier != null && metasByUniqueIdentifier.get(identifier) == meta ? identifier : null;
	}

	@NotNull
	private static Map<String, WidgetMeta> getMetasByUniqueIdentifier(@NotNull List<WidgetMeta> metas) {
		Map<String, WidgetMeta> metasByIdentifier = new HashMap<>();
		Set<String> duplicateIdentifiers = new HashSet<>();
		for (WidgetMeta meta : metas) {
			if (!(meta.getWidget() instanceof UniqueWidget uniqueWidget)) continue;
			String identifier = uniqueWidget.getWidgetIdentifierFancyMenu();
			if (identifier == null || duplicateIdentifiers.contains(identifier)) continue;
			if (metasByIdentifier.put(identifier, meta) != null) {
				metasByIdentifier.remove(identifier);
				duplicateIdentifiers.add(identifier);
			}
		}
		return metasByIdentifier;
	}

	@NotNull
	private static List<WidgetMeta> _getWidgetsOfScreen(@NotNull Screen screen, int screenWidth, int screenHeight) {
		List<WidgetMeta> widgetMetaList = new ArrayList<>();
		List<Long> ids = new ArrayList<>();

		try {

			((IMixinScreen)screen).getRenderablesFancyMenu().forEach(renderable -> {
				if (renderable instanceof CustomizableWidget w) w.resetWidgetCustomizationsFancyMenu();
			});

			//This is to avoid NullPointers
			if (!((IMixinScreen)screen).get_initialized_FancyMenu()) {
				screen.init(screenWidth, screenHeight);
			} else {
				screen.resize(screenWidth, screenHeight);
			}

			ScrollScreenNormalizer.normalizeScrollableScreen(screen);

			((IMixinScreen)screen).getRenderablesFancyMenu().forEach(renderable -> visitWidget(renderable, ids, widgetMetaList, screen));

		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to get widgets of screen!", ex);
		}
		return widgetMetaList;
	}

	private static void visitWidget(@NotNull Object widget, @NotNull List<Long> ids, @NotNull List<WidgetMeta> widgetMetaList, @NotNull Screen screen) {
		if (widget instanceof AbstractWidget w) {
			//Skip AbstractSelectionLists so they don't appear as customizable widget
			if (widget instanceof AbstractSelectionList<?>) return;
			long id = generateAvailableIdFromBaseId(generateBaseId(w), ids);
			ids.add(id);
			widgetMetaList.add(new WidgetMeta(w, id, screen));
		}
	}

	private static long generateBaseId(@NotNull AbstractWidget widget) {
		String s = Math.abs(widget.getX()) + "" + Math.abs(widget.getY());
		if (!MathUtils.isLong(s)) {
			LOGGER.error("[FANCYMENU] Widget ID is not a Long!", new NumberFormatException("Failed to parse widget identifier to Long!"));
			return 0L;
		}
		return Long.parseLong(s);
	}

	private static long generateAvailableIdFromBaseId(long baseId, @NotNull Collection<Long> ids) {
		if (ids.contains(baseId)) {
			String newId = baseId + "1";
			if (MathUtils.isLong(newId)) {
				return generateAvailableIdFromBaseId(Long.parseLong(newId), ids);
			} else {
				LOGGER.error("[FANCYMENU] Widget ID is not a Long!", new NumberFormatException("Failed to parse widget identifier to Long!"));
			}
		}
		return baseId;
	}

}
