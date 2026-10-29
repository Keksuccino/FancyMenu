#!/bin/sh
# Launches a dev client that never shows a window or takes focus. See TESTING_GUIDELINES.md.
# Usage: sh offscreen_client.sh <loader module> [additional Gradle arguments]
# Set JAVA_HOME to the project's Java version before running it.

if [ "$#" -lt 1 ]; then
    echo "Usage: sh offscreen_client.sh <loader module> [additional Gradle arguments]" >&2
    exit 2
fi
LOADER="$1"
shift
cd "$(dirname "$0")" || exit 1

# Config overrides for the run, restored when the script exits:
# - Rinku uses CEF's default Chromium profile instead of the user's persistent Rinku profile (see reset_chromium_crash_mark).
# - (Neo)Forge opens its early loading window before any mod code runs, so only its FML config can keep it closed.
RINKU_CONFIG="run_client/config/rinku/rinku.properties"
FML_CONFIG="run_client/config/fml.toml"
CLIENT_LOCK="${TMPDIR:-/tmp}/fancymenu-offscreen-client.lock"
HOLDS_CLIENT_LOCK=false

set_config_line() {
    # Replaces the line of the given key (or appends it), keeping the rest of the file untouched
    mkdir -p "$(dirname "$1")"
    if [ -f "$1" ] && grep -q "^$2[ =]" "$1"; then
        awk -v key="$2" -v line="$3" 'index($0, key) == 1 && substr($0, length(key) + 1, 1) ~ /[ =]/ { print line; next } { print }' "$1" > "$1.offscreen-tmp" && mv "$1.offscreen-tmp" "$1"
    else
        echo "$3" >> "$1"
    fi
}

reset_chromium_crash_mark() {
    # Chromium marks its profile as crashed while it runs and only clears the mark on a clean exit. A killed test run leaves
    # the mark behind, which makes Chromium open a visible "Restore pages?" window in the next run, so it gets cleared first.
    for prefs in "$HOME/Library/Application Support/CEF/User Data/Default/Preferences" "$HOME/.config/cef_user_data/Default/Preferences"; do
        if [ -f "$prefs" ] && grep -q '"exit_type":"Crashed"' "$prefs"; then
            sed 's/"exit_type":"Crashed"/"exit_type":"Normal"/' "$prefs" > "$prefs.offscreen-tmp" && chmod 600 "$prefs.offscreen-tmp" && mv "$prefs.offscreen-tmp" "$prefs"
        fi
    done
}

acquire_client_lock() {
    # Off-screen clients share one Chromium profile (see reset_chromium_crash_mark). A Chromium that starts while another one
    # uses the profile makes the running one open a visible "New Tab" window, so clients of all workspaces run one at a time.
    # The lock is a directory holding a subdirectory named after the owning script's PID, so a killed run's lock can be taken over.
    while ! mkdir "$CLIENT_LOCK" 2>/dev/null; do
        holder="$(ls "$CLIENT_LOCK" 2>/dev/null)"
        if [ -n "$holder" ] && ! kill -0 "$holder" 2>/dev/null; then
            rmdir "$CLIENT_LOCK/$holder" "$CLIENT_LOCK" 2>/dev/null
            continue
        fi
        echo "Waiting for the off-screen client of process ${holder:-?} to finish.."
        sleep 5
    done
    mkdir "$CLIENT_LOCK/$$"
    HOLDS_CLIENT_LOCK=true
}

release_client_lock() {
    if [ "$HOLDS_CLIENT_LOCK" = true ]; then
        rmdir "$CLIENT_LOCK/$$" "$CLIENT_LOCK" 2>/dev/null
    fi
}

override_config() {
    # A backup left behind by a killed run still holds the original file
    if [ -f "$1.offscreen-backup" ]; then
        mv "$1.offscreen-backup" "$1"
    fi
    if [ -f "$1" ]; then
        cp "$1" "$1.offscreen-backup"
    fi
    set_config_line "$1" "$2" "$3"
}

restore_config() {
    if [ -f "$1.offscreen-backup" ]; then
        mv "$1.offscreen-backup" "$1"
    else
        # The file didn't exist before, so it gets the value the mod would have created it with
        set_config_line "$1" "$2" "$3"
    fi
}

restore_all() {
    release_client_lock
    restore_config "$RINKU_CONFIG" use-cache "use-cache=true"
    if [ "$LOADER" != "fabric" ]; then
        restore_config "$FML_CONFIG" earlyWindowControl "earlyWindowControl = true"
    fi
}

# ForgeGradle names the client run task "Client"
CLIENT_TASK="runClient"
if [ "$LOADER" = "forge" ]; then
    CLIENT_TASK="Client"
fi

# Compiles before taking the client lock, so other workspaces don't wait for this build
sh gradlew ":$LOADER:classes" "$@" || exit 1

trap restore_all EXIT
trap 'exit 130' INT TERM
acquire_client_lock
override_config "$RINKU_CONFIG" use-cache "use-cache=false"
reset_chromium_crash_mark
if [ "$LOADER" != "fabric" ]; then
    override_config "$FML_CONFIG" earlyWindowControl "earlyWindowControl = false"
fi

FANCYMENU_TEST_OFFSCREEN=true sh gradlew ":$LOADER:$CLIENT_TASK" "$@"
