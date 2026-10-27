package de.keksuccino.fancymenu.util.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves paths inside one captured filesystem boundary.
 *
 * <p>The lexical root and its projected real path are captured together. Candidates are checked both lexically and
 * through their final entry or nearest existing ancestor, so nonexistent descendants remain usable without allowing a
 * symbolic link to redirect them outside the boundary. A root may itself be a symbolic link, but retargeting it after
 * capture invalidates every resolved path.</p>
 *
 * <p>Relocatable directories are descendants that users may move elsewhere and replace with a symbolic link or
 * junction. The real target each one has at capture time joins the boundary, so candidates may reach it through that
 * link, while links inside the target that lead anywhere else are still rejected. A relocatable directory retargeted
 * after capture is not followed; paths through its new target stay rejected until a new resolver is created.</p>
 *
 * <p>Callers must invoke {@link ResolvedPath#revalidate()} immediately before each filesystem operation. Portable Java
 * filesystem APIs cannot make this validation and a later operation atomic on every supported filesystem.</p>
 */
public final class ConfinedPathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");

    private final Path lexicalRoot;
    private final Path realRoot;
    private final List<Path> relocatedRealRoots;

    private ConfinedPathResolver(@NotNull Path root, @NotNull Collection<String> relocatableDirectories) throws IOException {
        this.lexicalRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.realRoot = projectRealPath(this.lexicalRoot);
        this.relocatedRealRoots = this.projectRelocatedRealRoots(Objects.requireNonNull(relocatableDirectories, "relocatableDirectories"));
    }

    @NotNull
    public static ConfinedPathResolver create(@NotNull Path root) throws IOException {
        return new ConfinedPathResolver(root, List.of());
    }

    /**
     * Creates a resolver whose boundary also covers the current real targets of the given relocatable directories.
     *
     * @param relocatableDirectories descendant directories of the root, as paths relative to it
     */
    @NotNull
    public static ConfinedPathResolver create(@NotNull Path root, @NotNull Collection<String> relocatableDirectories) throws IOException {
        return new ConfinedPathResolver(root, relocatableDirectories);
    }

    @NotNull
    public Path rootPath() {
        return this.lexicalRoot;
    }

    @NotNull
    public ResolvedPath resolve(@NotNull Path candidate) throws IOException {
        Path normalizedCandidate = Objects.requireNonNull(candidate, "candidate").toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(this.lexicalRoot)) {
            throw new SecurityException("Path escapes its allowed root: " + normalizedCandidate);
        }
        ResolvedPath resolvedPath = new ResolvedPath(normalizedCandidate);
        resolvedPath.revalidate();
        return resolvedPath;
    }

    private List<Path> projectRelocatedRealRoots(Collection<String> relocatableDirectories) {
        List<Path> relocatedRealRoots = new ArrayList<>();
        for (String relocatableDirectory : relocatableDirectories) {
            Path directory = this.lexicalRoot.resolve(relocatableDirectory).normalize();
            if (directory.equals(this.lexicalRoot) || !directory.startsWith(this.lexicalRoot)) {
                throw new IllegalArgumentException("Relocatable directory must be a descendant of its root: " + relocatableDirectory);
            }
            try {
                Path realDirectory = projectRealPath(directory);
                // Directories whose real target is still inside the real root are already covered by it.
                if (!realDirectory.startsWith(this.realRoot)) relocatedRealRoots.add(realDirectory);
            } catch (IOException ignored) {
                // An unresolvable directory, such as a dangling link, adds nothing; paths through it fail their own validation.
            }
        }
        return List.copyOf(relocatedRealRoots);
    }

    private boolean isInsideRealBoundary(Path projectedPath) {
        if (projectedPath.startsWith(this.realRoot)) return true;
        for (Path relocatedRealRoot : this.relocatedRealRoots) {
            if (projectedPath.startsWith(relocatedRealRoot)) return true;
        }
        return false;
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

    public final class ResolvedPath {

        private final Path path;

        private ResolvedPath(Path path) {
            this.path = path;
        }

        @NotNull
        public Path path() {
            return this.path;
        }

        public boolean isRoot() {
            return this.path.equals(ConfinedPathResolver.this.lexicalRoot);
        }

        @NotNull
        public ResolvedPath requireDescendant() {
            if (this.isRoot()) {
                throw new SecurityException("The allowed root itself cannot be selected for this operation: " + this.path);
            }
            return this;
        }

        @NotNull
        public ResolvedPath resolveRelativeChild(@NotNull String relativePath) throws IOException {
            Objects.requireNonNull(relativePath, "relativePath");
            String portablePath = relativePath.replace('\\', '/');
            if (portablePath.isEmpty() || portablePath.startsWith("/") || WINDOWS_DRIVE_PATH.matcher(portablePath).matches()) {
                throw new SecurityException("Relative child path must be a non-empty relative path: " + relativePath);
            }
            Path candidate = this.path.resolve(portablePath).toAbsolutePath().normalize();
            if (candidate.equals(this.path) || !candidate.startsWith(this.path)) {
                throw new SecurityException("Relative child path escapes its parent path: " + relativePath);
            }
            return ConfinedPathResolver.this.resolve(candidate);
        }

        @NotNull
        public ResolvedPath resolveSingleComponentChild(@NotNull String fileName) throws IOException {
            validateSingleComponentName(fileName);
            return this.resolveRelativeChild(fileName);
        }

        @NotNull
        public ResolvedPath resolveSingleComponentSibling(@NotNull String fileName) throws IOException {
            validateSingleComponentName(fileName);
            Path parent = this.path.getParent();
            if (parent == null) {
                throw new SecurityException("Cannot resolve a sibling without a parent path: " + this.path);
            }
            return ConfinedPathResolver.this.resolve(parent.resolve(fileName));
        }

        @NotNull
        public Path revalidate() throws IOException {
            Path currentRootRealPath = projectRealPath(ConfinedPathResolver.this.lexicalRoot);
            if (!currentRootRealPath.equals(ConfinedPathResolver.this.realRoot)) {
                throw new SecurityException("Allowed root changed after path resolution: " + ConfinedPathResolver.this.lexicalRoot);
            }
            Path projectedPath = projectRealPath(this.path);
            if (!ConfinedPathResolver.this.isInsideRealBoundary(projectedPath)) {
                throw new SecurityException("Path escapes its allowed root through a symbolic link: " + this.path);
            }
            return this.path;
        }
    }
}
