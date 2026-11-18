package de.keksuccino.fancymenu.customization.aiexport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hides personal parts of paths in AI exports, because users share them in public chats.<br>
 * The home folder of the user becomes {@code ~}, and the account name in other paths below {@code Users} or {@code home} becomes {@code <user>}.
 */
public final class AiExportRedactor {

    static final String HOME_REPLACEMENT = "~";
    static final String USER_NAME_REPLACEMENT = "<user>";
    private static final Pattern USER_FOLDER_PATTERN = Pattern.compile("(?i)([\\\\/](?:Users|home)[\\\\/])([^\\\\/\\s\"'`<>|:*?]+)");
    private static final String PATH_PART_END_LOOKAHEAD = "(?=$|[\\\\/\\s\"'`<>|:*?)\\]},;])";

    private AiExportRedactor() {
    }

    @NotNull
    public static String redact(@NotNull String text) {
        return redact(text, System.getProperty("user.home"), System.getProperty("user.name"));
    }

    @NotNull
    static String redact(@NotNull String text, @Nullable String userHome, @Nullable String userName) {
        String redacted = text;
        if ((userHome != null) && isSpecificEnough(userHome)) {
            String home = stripTrailingSeparators(userHome);
            // Paths can use either separator, no matter which one the OS uses, so both spellings of the home folder get replaced
            redacted = replaceIgnoreCase(redacted, home.replace('\\', '/'), HOME_REPLACEMENT);
            redacted = replaceIgnoreCase(redacted, home.replace('/', '\\'), HOME_REPLACEMENT);
        }
        if ((userName != null) && !userName.isBlank()) {
            Matcher matcher = USER_FOLDER_PATTERN.matcher(redacted);
            StringBuilder result = new StringBuilder(redacted.length());
            while (matcher.find()) {
                String replacement = matcher.group(2).equalsIgnoreCase(userName) ? (matcher.group(1) + USER_NAME_REPLACEMENT) : matcher.group();
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            redacted = result.toString();
        }
        return redacted;
    }

    /**
     * Home folders like "/" or "C:\" would replace parts of every path, so they don't get replaced.
     */
    private static boolean isSpecificEnough(@NotNull String userHome) {
        return stripTrailingSeparators(userHome).replace('\\', '/').indexOf('/', 1) > 0;
    }

    @NotNull
    private static String stripTrailingSeparators(@NotNull String path) {
        String s = path.strip();
        while ((s.length() > 1) && ((s.endsWith("/")) || (s.endsWith("\\")))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Only replaces the target where the folder name ends, so the home folder "/Users/tim" does not turn "/Users/timothy" into "~othy".
     */
    @NotNull
    private static String replaceIgnoreCase(@NotNull String text, @NotNull String target, @NotNull String replacement) {
        if (target.isEmpty()) return text;
        Pattern pattern = Pattern.compile(Pattern.quote(target) + PATH_PART_END_LOOKAHEAD, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }

}
