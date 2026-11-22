package de.keksuccino.fancymenu.test;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Defines its own copies of the given classes and their nested classes, so their static state is separate from the copies that the
 * other tests use. Tests use it for permanent lifecycle operations like client shutdown. Every other class comes from the parent loader.
 */
public final class IsolatedClassLoader extends ClassLoader {

    private final Set<String> isolatedClassNames;

    public IsolatedClassLoader(@NotNull ClassLoader parent, @NotNull Class<?>... isolatedClasses) {
        super(parent);
        String[] classNames = new String[isolatedClasses.length];
        for (int i = 0; i < isolatedClasses.length; i++) {
            classNames[i] = isolatedClasses[i].getName();
        }
        this.isolatedClassNames = Set.of(classNames);
    }

    @NotNull
    public Class<?> loadIsolated(@NotNull Class<?> isolatedClass) throws ClassNotFoundException {
        Class<?> loadedClass = Class.forName(isolatedClass.getName(), true, this);
        if (loadedClass == isolatedClass) throw new IllegalStateException("Class is not isolated: " + isolatedClass.getName());
        return loadedClass;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!this.isIsolated(name)) return super.loadClass(name, resolve);
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass == null) loadedClass = this.defineIsolatedClass(name);
            if (resolve) this.resolveClass(loadedClass);
            return loadedClass;
        }
    }

    private boolean isIsolated(@NotNull String name) {
        int nestedSeparator = name.indexOf('$');
        return this.isolatedClassNames.contains((nestedSeparator >= 0) ? name.substring(0, nestedSeparator) : name);
    }

    @NotNull
    private Class<?> defineIsolatedClass(@NotNull String name) throws ClassNotFoundException {
        try (InputStream input = this.getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) throw new ClassNotFoundException(name);
            byte[] bytes = input.readAllBytes();
            return this.defineClass(name, bytes, 0, bytes.length);
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }
    }

}
