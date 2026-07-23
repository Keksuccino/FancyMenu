package de.keksuccino.fancymenu.util.file;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
            if (hasSiblingPrefix(normalizedAbsolutePath, this.gameDirectoryRoot.lexicalPath) || hasSiblingPrefix(normalizedAbsolutePath, this.minecraftDirectoryRoot.lexicalPath)) {
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
        Path candidate = root.lexicalPath.resolve(relativePath).toAbsolutePath().normalize();
        return this.createResolvedPath(root, candidate);
    }

    private ResolvedPath createResolvedPath(RootBoundary root, Path candidate) throws IOException {
        if (!candidate.startsWith(root.lexicalPath)) {
            throw new SecurityException("Path escapes its allowed root: " + candidate);
        }
        ResolvedPath resolvedPath = new ResolvedPath(root, candidate);
        resolvedPath.revalidate();
        return resolvedPath;
    }

    private RootBoundary findContainingRoot(Path absolutePath) {
        if (absolutePath.startsWith(this.gameDirectoryRoot.lexicalPath)) {
            return this.gameDirectoryRoot;
        }
        if (absolutePath.startsWith(this.minecraftDirectoryRoot.lexicalPath)) {
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

    private static Path projectRealPath(Path path) throws IOException {
        Path current = path;
        List<Path> missingSegments = new ArrayList<>();
        while ((current != null) && !entryExists(current)) {
            Path fileName = current.getFileName();
            if (fileName != null) {
                missingSegments.add(fileName);
            }
            current = current.getParent();
        }
        if (current == null) {
            throw new IOException("Unable to find an existing ancestor for path: " + path);
        }
        Path projected = current.toRealPath();
        for (int i = missingSegments.size() - 1; i >= 0; i--) {
            projected = projected.resolve(missingSegments.get(i));
        }
        return projected.toAbsolutePath().normalize();
    }

    private static boolean entryExists(Path path) throws IOException {
        try {
            Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return true;
        } catch (NoSuchFileException ex) {
            return false;
        }
    }

    public enum AllowedRoot {
        GAME_DIRECTORY,
        DEFAULT_MINECRAFT_DIRECTORY
    }

    public final class ResolvedPath {

        private final RootBoundary root;
        private final Path path;

        private ResolvedPath(RootBoundary root, Path path) {
            this.root = root;
            this.path = path;
        }

        @Nonnull
        public Path path() {
            return this.path;
        }

        @Nonnull
        public Path rootPath() {
            return this.root.lexicalPath;
        }

        @Nonnull
        public AllowedRoot allowedRoot() {
            return this.root.allowedRoot;
        }

        public boolean isRoot() {
            return this.path.equals(this.root.lexicalPath);
        }

        @Nonnull
        public ResolvedPath requireDescendant() {
            if (this.isRoot()) {
                throw new SecurityException("The allowed root itself cannot be selected for this operation: " + this.path);
            }
            return this;
        }

        @Nonnull
        public ResolvedPath resolveRelativeChild(@Nonnull String relativePath) throws IOException {
            Objects.requireNonNull(relativePath, "relativePath");
            String portablePath = relativePath.replace('\\', '/');
            if (portablePath.isEmpty() || portablePath.startsWith("/") || portablePath.startsWith("//") || WINDOWS_DRIVE_PATH.matcher(portablePath).matches()) {
                throw new SecurityException("Relative child path must be a non-empty relative path: " + relativePath);
            }
            Path candidate = this.path.resolve(portablePath).toAbsolutePath().normalize();
            if (candidate.equals(this.path) || !candidate.startsWith(this.path)) {
                throw new SecurityException("Relative child path escapes its parent path: " + relativePath);
            }
            return GameDirectoryActionPathResolver.this.createResolvedPath(this.root, candidate);
        }

        @Nonnull
        public ResolvedPath resolveSingleComponentChild(@Nonnull String fileName) throws IOException {
            validateSingleComponentName(fileName);
            return this.resolveRelativeChild(fileName);
        }

        @Nonnull
        public ResolvedPath resolveSingleComponentSibling(@Nonnull String fileName) throws IOException {
            validateSingleComponentName(fileName);
            Path parent = this.path.getParent();
            if (parent == null) {
                throw new SecurityException("Cannot resolve a sibling without a parent path: " + this.path);
            }
            Path candidate = parent.resolve(fileName).toAbsolutePath().normalize();
            return GameDirectoryActionPathResolver.this.createResolvedPath(this.root, candidate);
        }

        @Nonnull
        public Path revalidate() throws IOException {
            Path currentRootRealPath = projectRealPath(this.root.lexicalPath);
            if (!currentRootRealPath.equals(this.root.realPath)) {
                throw new SecurityException("Allowed root changed after path resolution: " + this.root.lexicalPath);
            }
            Path projectedPath = projectRealPath(this.path);
            if (!projectedPath.startsWith(this.root.realPath)) {
                throw new SecurityException("Path escapes its allowed root through a symbolic link: " + this.path);
            }
            return this.path;
        }
    }

    private static void validateSingleComponentName(String fileName) {
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isEmpty() || fileName.equals(".") || fileName.equals("..") || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0 || WINDOWS_DRIVE_PATH.matcher(fileName).matches()) {
            throw new SecurityException("File name must contain exactly one non-dot path component: " + fileName);
        }
        Path parsedName = Path.of(fileName);
        if (parsedName.isAbsolute() || (parsedName.getNameCount() != 1)) {
            throw new SecurityException("File name must contain exactly one path component: " + fileName);
        }
    }

    private record RootBoundary(AllowedRoot allowedRoot, Path lexicalPath, Path realPath) {

        private static RootBoundary capture(AllowedRoot allowedRoot, Path path) throws IOException {
            Path lexicalPath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
            return new RootBoundary(allowedRoot, lexicalPath, projectRealPath(lexicalPath));
        }
    }
}
