package de.keksuccino.fancymenu.util.file;

import org.jetbrains.annotations.NotNull;

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
 * Resolves paths inside one captured filesystem boundary.
 *
 * <p>The lexical root and its projected real path are captured together. Candidates are checked both lexically and
 * through their final entry or nearest existing ancestor, so nonexistent descendants remain usable without allowing a
 * symbolic link to redirect them outside the boundary. A root may itself be a symbolic link, but retargeting it after
 * capture invalidates every resolved path.</p>
 *
 * <p>Callers must invoke {@link ResolvedPath#revalidate()} immediately before each filesystem operation. Portable Java
 * filesystem APIs cannot make this validation and a later operation atomic on every supported filesystem.</p>
 */
public final class ConfinedPathResolver {

    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");

    private final Path lexicalRoot;
    private final Path realRoot;

    private ConfinedPathResolver(@NotNull Path root) throws IOException {
        this.lexicalRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.realRoot = projectRealPath(this.lexicalRoot);
    }

    @NotNull
    public static ConfinedPathResolver create(@NotNull Path root) throws IOException {
        return new ConfinedPathResolver(root);
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
            if (!projectedPath.startsWith(ConfinedPathResolver.this.realRoot)) {
                throw new SecurityException("Path escapes its allowed root through a symbolic link: " + this.path);
            }
            return this.path;
        }
    }
}
