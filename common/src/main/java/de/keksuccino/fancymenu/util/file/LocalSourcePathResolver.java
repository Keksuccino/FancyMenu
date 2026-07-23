package de.keksuccino.fancymenu.util.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves read-only local-source syntax without allowing it to leave its advertised roots.
 *
 * <p>Ordinary relative paths are rooted at the active game instance. A single leading slash is retained as FancyMenu's
 * long-standing instance-root spelling, while absolute paths already inside an allowed root retain their host-absolute
 * meaning. The default Minecraft directory is available only to consumers that explicitly opt into the documented
 * {@code .minecraft/...} shorthand.</p>
 */
public final class LocalSourcePathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");
    private static final Object CACHE_LOCK = new Object();
    private static volatile CachedResolver gameDirectoryCache;
    private static volatile CachedResolver gameAndMinecraftDirectoriesCache;

    private final RootBoundary gameDirectoryRoot;
    private final RootBoundary minecraftDirectoryRoot;

    private LocalSourcePathResolver(@NotNull Path gameDirectoryRoot, Path minecraftDirectoryRoot) throws IOException {
        this.gameDirectoryRoot = RootBoundary.capture(AllowedRoot.GAME_DIRECTORY, gameDirectoryRoot);
        this.minecraftDirectoryRoot = (minecraftDirectoryRoot != null) ? RootBoundary.capture(AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraftDirectoryRoot) : null;
    }

    @NotNull
    public static LocalSourcePathResolver createForGameDirectory() throws IOException {
        Path gameDirectoryRoot = normalizeRoot(GameDirectoryUtils.getGameDirectory().toPath());
        CachedResolver cached = gameDirectoryCache;
        if ((cached != null) && cached.matches(gameDirectoryRoot, null)) return cached.resolver;
        synchronized (CACHE_LOCK) {
            cached = gameDirectoryCache;
            if ((cached == null) || !cached.matches(gameDirectoryRoot, null)) {
                cached = new CachedResolver(gameDirectoryRoot, null, new LocalSourcePathResolver(gameDirectoryRoot, null));
                gameDirectoryCache = cached;
            }
            return cached.resolver;
        }
    }

    @NotNull
    public static LocalSourcePathResolver createForGameAndMinecraftDirectories() throws IOException {
        Path gameDirectoryRoot = normalizeRoot(GameDirectoryUtils.getGameDirectory().toPath());
        Path minecraftDirectoryRoot = normalizeRoot(DotMinecraftUtils.getMinecraftDirectory());
        CachedResolver cached = gameAndMinecraftDirectoriesCache;
        if ((cached != null) && cached.matches(gameDirectoryRoot, minecraftDirectoryRoot)) return cached.resolver;
        synchronized (CACHE_LOCK) {
            cached = gameAndMinecraftDirectoriesCache;
            if ((cached == null) || !cached.matches(gameDirectoryRoot, minecraftDirectoryRoot)) {
                cached = new CachedResolver(gameDirectoryRoot, minecraftDirectoryRoot, new LocalSourcePathResolver(gameDirectoryRoot, minecraftDirectoryRoot));
                gameAndMinecraftDirectoriesCache = cached;
            }
            return cached.resolver;
        }
    }

    @NotNull
    public static LocalSourcePathResolver createForGameDirectory(@NotNull Path gameDirectoryRoot) throws IOException {
        return new LocalSourcePathResolver(gameDirectoryRoot, null);
    }

    @NotNull
    public static LocalSourcePathResolver createForGameAndMinecraftDirectories(@NotNull Path gameDirectoryRoot, @NotNull Path minecraftDirectoryRoot) throws IOException {
        return new LocalSourcePathResolver(gameDirectoryRoot, minecraftDirectoryRoot);
    }

    @NotNull
    public ResolvedPath resolve(@NotNull String rawPath) throws IOException {
        Objects.requireNonNull(rawPath, "rawPath");
        String portablePath = rawPath.replace('\\', '/');
        if (containsParentTraversal(portablePath)) {
            throw new SecurityException("Local source contains parent traversal: " + rawPath);
        }
        if ((this.minecraftDirectoryRoot != null) && isMinecraftShorthand(portablePath)) {
            String relativePath = portablePath.equals(".minecraft") ? "" : portablePath.substring(".minecraft/".length());
            return this.resolveRelative(this.minecraftDirectoryRoot, relativePath);
        }

        Path suppliedPath = Path.of(portablePath);
        if (suppliedPath.isAbsolute()) {
            Path normalizedAbsolutePath = suppliedPath.toAbsolutePath().normalize();
            RootBoundary containingRoot = this.findContainingRoot(normalizedAbsolutePath);
            if (containingRoot != null) {
                return this.resolveAbsolute(containingRoot, normalizedAbsolutePath);
            }
            if (hasSiblingPrefix(normalizedAbsolutePath, this.gameDirectoryRoot.resolver.rootPath()) || ((this.minecraftDirectoryRoot != null) && hasSiblingPrefix(normalizedAbsolutePath, this.minecraftDirectoryRoot.resolver.rootPath()))) {
                throw new SecurityException("Absolute local source uses an allowed-root string prefix without being inside that root: " + rawPath);
            }
            if (portablePath.startsWith("//")) {
                throw new SecurityException("UNC and network-root local sources are outside their allowed roots: " + rawPath);
            }
            return this.resolveRelative(this.gameDirectoryRoot, stripLeadingSlashes(portablePath));
        }
        if (WINDOWS_DRIVE_PATH.matcher(portablePath).matches() || portablePath.startsWith("//")) {
            throw new SecurityException("Drive-qualified and UNC local sources are outside their allowed roots: " + rawPath);
        }
        if (portablePath.startsWith("/")) {
            return this.resolveRelative(this.gameDirectoryRoot, stripLeadingSlashes(portablePath));
        }
        return this.resolveRelative(this.gameDirectoryRoot, portablePath);
    }

    private ResolvedPath resolveRelative(RootBoundary root, String relativePath) throws IOException {
        return this.resolveAbsolute(root, root.resolver.rootPath().resolve(relativePath));
    }

    private ResolvedPath resolveAbsolute(RootBoundary root, Path path) throws IOException {
        return new ResolvedPath(root, root.resolver.resolve(path));
    }

    private RootBoundary findContainingRoot(Path path) {
        if (path.startsWith(this.gameDirectoryRoot.resolver.rootPath())) {
            return this.gameDirectoryRoot;
        }
        if ((this.minecraftDirectoryRoot != null) && path.startsWith(this.minecraftDirectoryRoot.resolver.rootPath())) {
            return this.minecraftDirectoryRoot;
        }
        return null;
    }

    private static boolean isMinecraftShorthand(String path) {
        return path.equals(".minecraft") || path.startsWith(".minecraft/");
    }

    private static boolean containsParentTraversal(String path) {
        for (String component : path.split("/", -1)) {
            if (component.equals("..")) return true;
        }
        return false;
    }

    private static boolean hasSiblingPrefix(Path path, Path root) {
        return path.toString().startsWith(root.toString()) && !path.startsWith(root);
    }

    private static String stripLeadingSlashes(String path) {
        int firstNonSlash = 0;
        while ((firstNonSlash < path.length()) && (path.charAt(firstNonSlash) == '/')) {
            firstNonSlash++;
        }
        return path.substring(firstNonSlash);
    }

    private static Path normalizeRoot(Path root) {
        return root.toAbsolutePath().normalize();
    }

    public enum AllowedRoot {
        GAME_DIRECTORY,
        DEFAULT_MINECRAFT_DIRECTORY
    }

    public static final class ResolvedPath {

        private final RootBoundary root;
        private final ConfinedPathResolver.ResolvedPath confinedPath;

        private ResolvedPath(RootBoundary root, ConfinedPathResolver.ResolvedPath confinedPath) {
            this.root = root;
            this.confinedPath = confinedPath;
        }

        @NotNull
        public Path path() {
            return this.confinedPath.path();
        }

        @NotNull
        public Path rootPath() {
            return this.root.resolver.rootPath();
        }

        @NotNull
        public AllowedRoot allowedRoot() {
            return this.root.allowedRoot;
        }

        @NotNull
        public Path revalidate() throws IOException {
            return this.confinedPath.revalidate();
        }
    }

    private record RootBoundary(AllowedRoot allowedRoot, ConfinedPathResolver resolver) {

        private static RootBoundary capture(AllowedRoot allowedRoot, Path path) throws IOException {
            return new RootBoundary(allowedRoot, ConfinedPathResolver.create(path));
        }
    }

    private record CachedResolver(Path gameDirectoryRoot, Path minecraftDirectoryRoot, LocalSourcePathResolver resolver) {

        private boolean matches(Path gameDirectoryRoot, Path minecraftDirectoryRoot) {
            return this.gameDirectoryRoot.equals(gameDirectoryRoot) && Objects.equals(this.minecraftDirectoryRoot, minecraftDirectoryRoot);
        }
    }
}
