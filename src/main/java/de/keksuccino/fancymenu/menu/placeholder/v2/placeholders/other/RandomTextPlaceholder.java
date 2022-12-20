package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.other;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class RandomTextPlaceholder extends Placeholder {

    public static Map<String, RandomTextPackage> randomTextIntervals = new HashMap<>();

    public RandomTextPlaceholder() {
        super("randomtext");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String pathString = dps.values.get("path");
        String intervalString = dps.values.get("interval");
        if ((pathString != null) && (intervalString != null)) {
            File path = new File(pathString);
            if (!path.exists() || !path.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                path = new File(Minecraft.getInstance().gameDirectory, pathString);
            }
            if (MathUtils.isLong(intervalString) && path.isFile() && path.getPath().toLowerCase().endsWith(".txt")) {
                long interval = Long.parseLong(intervalString) * 1000;
                if (interval < 0L) {
                    interval = 0L;
                }
                long currentTime = System.currentTimeMillis();
                RandomTextPackage p;
                if (randomTextIntervals.containsKey(path.getPath())) {
                    p = randomTextIntervals.get(path.getPath());
                } else {
                    p = new RandomTextPackage();
                    randomTextIntervals.put(path.getPath(), p);
                }
                if ((interval > 0) || (p.currentText == null)) {
                    if ((p.lastChange + interval) <= currentTime) {
                        p.lastChange = currentTime;
                        List<String> txtLines = FileUtils.getFileLines(path);
                        if (!txtLines.isEmpty()) {
                            p.currentText = txtLines.get(MathUtils.getRandomNumberInRange(0, txtLines.size()-1));
                        } else {
                            p.currentText = null;
                        }
                    }
                }
                if (p.currentText != null) {
                    return p.currentText;
                } else {
                    return "";
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("path");
        l.add("interval");
        return l;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.randomtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.randomtext.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        dps.values.put("path", "randomtexts.txt");
        dps.values.put("interval", "10");
        return dps;
    }

    public static class RandomTextPackage {
        public String currentText = null;
        public long lastChange = 0L;
    }

}
