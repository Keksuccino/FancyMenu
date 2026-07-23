package de.keksuccino.fancymenu.util.file;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves paths used by game-directory file actions against one of their two advertised roots.
 *
 * <p>The lexical root is selected first and never changes for the lifetime of this resolver. The root's projected real
 * path is captured at construction time, so a configured root may itself be a symlink, but replacing that symlink with
 * one pointing somewhere else does not silently move the allowed boundary. Every resolved path is also checked through
 * its final entry or nearest existing ancestor, which rejects descendants that escape through symlinks while still
 * allowing safe descendants that do not exist yet.</p>
 *
 * <p>These checks minimize path-swap windows, but callers must still invoke {@link ResolvedPath#revalidate()} immediately
 * before each filesystem mutation. Java's portable path APIs cannot make an ancestor check and a later mutation one
 * atomic operation on every supported filesystem.</p>
 */
public final class GameDirectoryActionPathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");

    private final RootBoundary gameDirectoryRoot;
    private final RootBoundary minecraftDirectoryRoot;

    private GameDirectoryActionPathResolver(@Nonnull Path gameDirectoryRoot, @Nonnull Path minecraftDirectoryRoot) throws IOException {
        this.gameDirectoryRoot = RootBoundary.capture(AllowedRoot.GAME_DIRECTORY, gameDirectoryRoot);
        this.minecraftDirectoryRoot = RootBoundary.capture(AllowedRoot.DEFAULT_MINECRAFT_DIRECTORY, minecraftDirectoryRoot);
    }

    @Nonnull
    public static GameDirectoryActionPathResolver create() throws IOException {
        return new GameDirectoryActionPathResolver(GameDirectoryUtils.getGameDirectory().toPath(), DotMinecraftUtils.getMinecraftDirectory());
    }

    @Nonnull
    static GameDirectoryActionPathResolver create(@Nonnull Path gameDirectoryRoot, @Nonnull Path minecraftDirectoryRoot) throws IOException {
        return new GameDirectoryActionPathResolver(gameDirectoryRoot, minecraftDirectoryRoot);
    }

    @Nonnull
    public ResolvedPath resolve(@Nonnull String rawPath) throws IOException {
        Objects.requireNonNull(rawPath, "rawPath");
        String portablePath = rawPath.replace('\\', '/');
        if (isMinecraftShorthand(portablePath)) {
            String relativePath = portablePath.equals(".minecraft") ? "" : portablePath.substring(".minecraft/".length());
            return this.resolveAgainstRoot(this.minecraftDirectoryRoot, relativePath);
        }

        Path suppliedPath = Path.of(portablePath);
        if (suppliedPath.isAbsolute()) {
            Path normalizedAbsolutePath = suppliedPath.toAbsolutePath().normalize();
            RootBoundary absoluteRoot = this.findContainingRoot(normalizedAbsolutePath);
            if (absoluteRoot != null) {
                return this.createResolvedPath(absoluteRoot, normalizedAbsolutePath);
            }
            if (hasSiblingPrefix(normalizedAbsolutePath, this.gameDirectoryRoot.resolver.rootPath()) || hasSiblingPrefix(normalizedAbsolutePath, this.minecraftDirectoryRoot.resolver.rootPath())) {
                throw new SecurityException("Absolute path uses an allowed-root string prefix without being inside that root: " + rawPath);
            }
            if (portablePath.startsWith("//")) {
                throw new SecurityException("UNC and network-root paths are not allowed: " + rawPath);
            }
            // A single leading slash is FancyMenu's long-standing syntax for a path relative to the instance root.
            portablePath = stripLeadingSlashes(portablePath);
        } else if (portablePath.startsWith("/") && !portablePath.startsWith("//")) {
            // Some Windows providers do not classify FancyMenu's virtual-root syntax as host-absolute.
            portablePath = stripLeadingSlashes(portablePath);
        } else if (WINDOWS_DRIVE_PATH.matcher(portablePath).matches() || portablePath.startsWith("//")) {
            // Reject foreign-platform absolute/drive-relative forms instead of interpreting them differently per host OS.
            throw new SecurityException("Drive-qualified and UNC paths are not allowed outside an advertised root: " + rawPath);
        }
        return this.resolveAgainstRoot(this.gameDirectoryRoot, portablePath);
    }

    private ResolvedPath resolveAgainstRoot(RootBoundary root, String relativePath) throws IOException {
        Path candidate = root.resolver.rootPath().resolve(relativePath).toAbsolutePath().normalize();
        return this.createResolvedPath(root, candidate);
    }

    private ResolvedPath createResolvedPath(RootBoundary root, Path candidate) throws IOException {
        return new ResolvedPath(root, root.resolver.resolve(candidate));
    }

    private RootBoundary findContainingRoot(Path absolutePath) {
        if (absolutePath.startsWith(this.gameDirectoryRoot.resolver.rootPath())) {
            return this.gameDirectoryRoot;
        }
        if (absolutePath.startsWith(this.minecraftDirectoryRoot.resolver.rootPath())) {
            return this.minecraftDirectoryRoot;
        }
        return null;
    }

    private static boolean isMinecraftShorthand(String path) {
        return path.equals(".minecraft") || path.startsWith(".minecraft/");
    }

    private static boolean hasSiblingPrefix(Path path, Path root) {
        String pathString = path.toString();
        String rootString = root.toString();
        return pathString.startsWith(rootString) && !path.startsWith(root);
    }

    private static String stripLeadingSlashes(String path) {
        int firstNonSlash = 0;
        while ((firstNonSlash < path.length()) && (path.charAt(firstNonSlash) == '/')) {
            firstNonSlash++;
        }
        return path.substring(firstNonSlash);
    }

    public enum AllowedRoot {
        GAME_DIRECTORY,
        DEFAULT_MINECRAFT_DIRECTORY
    }

    public final class ResolvedPath {

        private final RootBoundary root;
        private final ConfinedPathResolver.ResolvedPath confinedPath;

        private ResolvedPath(RootBoundary root, ConfinedPathResolver.ResolvedPath confinedPath) {
            this.root = root;
            this.confinedPath = confinedPath;
        }

        @Nonnull
        public Path path() {
            return this.confinedPath.path();
        }

        @Nonnull
        public Path rootPath() {
            return this.root.resolver.rootPath();
        }

        @Nonnull
        public AllowedRoot allowedRoot() {
            return this.root.allowedRoot;
        }

        public boolean isRoot() {
            return this.confinedPath.isRoot();
        }

        @Nonnull
        public ResolvedPath requireDescendant() {
            this.confinedPath.requireDescendant();
            return this;
        }

        @Nonnull
        public ResolvedPath resolveRelativeChild(@Nonnull String relativePath) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveRelativeChild(relativePath));
        }

        @Nonnull
        public ResolvedPath resolveSingleComponentChild(@Nonnull String fileName) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveSingleComponentChild(fileName));
        }

        @Nonnull
        public ResolvedPath resolveSingleComponentSibling(@Nonnull String fileName) throws IOException {
            return new ResolvedPath(this.root, this.confinedPath.resolveSingleComponentSibling(fileName));
        }

        @Nonnull
        public Path revalidate() throws IOException {
            return this.confinedPath.revalidate();
        }
    }

    private record RootBoundary(AllowedRoot allowedRoot, ConfinedPathResolver resolver) {

        private static RootBoundary capture(AllowedRoot allowedRoot, Path path) throws IOException {
            return new RootBoundary(allowedRoot, ConfinedPathResolver.create(path));
        }
    }
}
