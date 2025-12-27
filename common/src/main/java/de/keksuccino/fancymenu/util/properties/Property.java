package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings({"unchecked", "unused"})
public class Property<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected final String key;
    @Nullable
    protected T defaultValue;
    @Nullable
    protected T currentValue;
    @Nullable
    protected ConsumingSupplier<String, T> deserializationCodec;
    @Nullable
    protected ConsumingSupplier<T, String> serializationCodec = Object::toString;
    @Nullable
    protected ContextMenuEntrySupplier<? extends PropertyHolder, T> contextMenuEntrySupplier;
    @NotNull
    protected String contextMenuEntryLocalizationKeyBase;

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<String> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> consumes;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<String> resolved = (Property<String>) consumes.getProperty(key);
                if (resolved != null) return resolved.get();
                return defaultValue;
            }, (b, s) -> {
                Property<String> resolved = (Property<String>) b.getProperty(key);
                if (resolved != null) resolved.set(s);
            }, null, multiLine, placeholders, Component.translatable(contextMenuEntryLocalizationKeyBase), true, property.getDefault(), null, null);
        };
        return p;
    }

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return stringProperty(key, defaultValue, defaultValue, multiLine, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, int currentValue) {
        Property<Integer> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Integer::valueOf;
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue) {
        Property<Integer> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Integer::valueOf;
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, int currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Integer> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Integer::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Integer defaultVal = property.getDefault();
            int resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            if (placeholders) {
                return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                    Property<Integer> resolved = (Property<Integer>) consumes.getProperty(key);
                    Integer value = (resolved != null) ? resolved.get() : defaultVal;
                    return (value != null) ? String.valueOf(value) : null;
                }, (b, s) -> {
                    if (s == null) return;
                    try {
                        Integer parsed = Integer.valueOf(s);
                        Property<Integer> resolved = (Property<Integer>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    } catch (NumberFormatException ignored) {
                    }
                }, null, false, true, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                        (defaultVal != null) ? String.valueOf(defaultVal) : String.valueOf(defaultValue), null, null);
            }
            return builder.buildIntegerInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Integer> resolved = (Property<Integer>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Integer> resolved = (Property<Integer>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, null, null);
        };
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return integerProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, double currentValue) {
        Property<Double> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Double::valueOf;
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue) {
        Property<Double> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Double::valueOf;
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, double currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Double> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Double::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Double defaultVal = property.getDefault();
            double resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            if (placeholders) {
                return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                    Property<Double> resolved = (Property<Double>) consumes.getProperty(key);
                    Double value = (resolved != null) ? resolved.get() : defaultVal;
                    return (value != null) ? String.valueOf(value) : null;
                }, (b, s) -> {
                    if (s == null) return;
                    try {
                        Double parsed = Double.valueOf(s);
                        Property<Double> resolved = (Property<Double>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    } catch (NumberFormatException ignored) {
                    }
                }, null, false, true, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                        (defaultVal != null) ? String.valueOf(defaultVal) : String.valueOf(defaultValue), null, null);
            }
            return builder.buildDoubleInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Double> resolved = (Property<Double>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Double> resolved = (Property<Double>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, null, null);
        };
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return doubleProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, long currentValue) {
        Property<Long> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Long::valueOf;
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue) {
        Property<Long> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Long::valueOf;
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, long currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Long> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Long::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Long defaultVal = property.getDefault();
            long resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            if (placeholders) {
                return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                    Property<Long> resolved = (Property<Long>) consumes.getProperty(key);
                    Long value = (resolved != null) ? resolved.get() : defaultVal;
                    return (value != null) ? String.valueOf(value) : null;
                }, (b, s) -> {
                    if (s == null) return;
                    try {
                        Long parsed = Long.valueOf(s);
                        Property<Long> resolved = (Property<Long>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    } catch (NumberFormatException ignored) {
                    }
                }, null, false, true, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                        (defaultVal != null) ? String.valueOf(defaultVal) : String.valueOf(defaultValue), null, null);
            }
            return builder.buildLongInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Long> resolved = (Property<Long>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Long> resolved = (Property<Long>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, null, null);
        };
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return longProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, float currentValue) {
        Property<Float> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Float::valueOf;
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue) {
        Property<Float> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Float::valueOf;
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, float currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Float> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Float::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Float defaultVal = property.getDefault();
            float resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            if (placeholders) {
                return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                    Property<Float> resolved = (Property<Float>) consumes.getProperty(key);
                    Float value = (resolved != null) ? resolved.get() : defaultVal;
                    return (value != null) ? String.valueOf(value) : null;
                }, (b, s) -> {
                    if (s == null) return;
                    try {
                        Float parsed = Float.valueOf(s);
                        Property<Float> resolved = (Property<Float>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    } catch (NumberFormatException ignored) {
                    }
                }, null, false, true, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                        (defaultVal != null) ? String.valueOf(defaultVal) : String.valueOf(defaultValue), null, null);
            }
            return builder.buildFloatInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Float> resolved = (Property<Float>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Float> resolved = (Property<Float>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, null, null);
        };
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return floatProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue) {
        Property<Boolean> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = Boolean::valueOf;
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue) {
        Property<Boolean> p = new Property<>(key, defaultValue);
        p.deserializationCodec = Boolean::valueOf;
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Boolean> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Boolean::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> builder.buildToggleContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
            Property<Boolean> resolved = (Property<Boolean>) consumes.getProperty(key);
            return (resolved != null) ? resolved.get() : property.getDefault();
        }, (b, value) -> {
            Property<Boolean> resolved = (Property<Boolean>) b.getProperty(key);
            if (resolved != null) resolved.set(value);
        }, contextMenuEntryLocalizationKeyBase);
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return booleanProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @Nullable ResourceSource currentValue) {
        Property<ResourceSource> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = ResourceSource::getSerializationSource;
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue) {
        Property<ResourceSource> p = new Property<>(key, defaultValue);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = ResourceSource::getSerializationSource;
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @Nullable ResourceSource currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<ResourceSource> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = ResourceSource::getSerializationSource;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            ResourceSupplier<Resource> defaultSupplier = null;
            ResourceSource defaultSource = property.getDefault();
            if (defaultSource != null) {
                defaultSupplier = new ResourceSupplier<>(Resource.class, FileMediaType.TEXT, defaultSource.getSourceWithPrefix());
            }
            return builder.buildGenericResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
                    () -> ResourceChooserScreen.generic(null, null, file -> {}),
                    source -> new ResourceSupplier<>(Resource.class, FileMediaType.TEXT, source),
                    defaultSupplier,
                    consumes -> {
                        Property<ResourceSource> resolved = (Property<ResourceSource>) consumes.getProperty(key);
                        ResourceSource value = (resolved != null) ? resolved.get() : defaultSource;
                        if (value == null) return null;
                        return new ResourceSupplier<>(Resource.class, FileMediaType.TEXT, value.getSourceWithPrefix());
                    },
                    (b, supplier) -> {
                        Property<ResourceSource> resolved = (Property<ResourceSource>) b.getProperty(key);
                        if (resolved == null) return;
                        if (supplier == null) {
                            resolved.set(null);
                        } else {
                            resolved.set(ResourceSource.of(supplier.getSourceWithPrefix()));
                        }
                    },
                    Component.translatable(contextMenuEntryLocalizationKeyBase), true, null, null, true, true, true);
        };
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return resourceSourceProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @Nullable ResourceSupplier<R> currentValue) {
        Property<ResourceSupplier<R>> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = consumes -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.image(consumes);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.audio(consumes);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.video(consumes);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.text(consumes);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to deserialize ResourceSupplier property!");
        };
        p.serializationCodec = ResourceSupplier::getSourceWithPrefix;
        return p;
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue) {
        return resourceSupplierProperty(resourceType, key, defaultValue, defaultValue);
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @Nullable ResourceSupplier<R> currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<ResourceSupplier<R>> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.image(consumes);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.audio(consumes);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.video(consumes);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.text(consumes);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to deserialize ResourceSupplier property!");
        };
        p.serializationCodec = ResourceSupplier::getSourceWithPrefix;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return builder.buildImageResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
                        (ResourceSupplier<ITexture>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<ITexture>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, null, true, true, true);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return builder.buildAudioResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
                        (ResourceSupplier<IAudio>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IAudio>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, null, true, true, true);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return builder.buildVideoResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
                        (ResourceSupplier<IVideo>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IVideo>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, null, true, true, true);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return builder.buildTextResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
                        (ResourceSupplier<IText>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IText>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, null, true, true, true);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to build ResourceSupplier context menu entry!");
        };
        return p;
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return resourceSupplierProperty(resourceType, key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, @Nullable DrawableColor currentValue) {
        Property<DrawableColor> p = new Property<>(key, defaultValue, currentValue);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = DrawableColor::getHex;
        return p;
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue) {
        Property<DrawableColor> p = new Property<>(key, defaultValue);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = DrawableColor::getHex;
        return p;
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, @Nullable DrawableColor currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<DrawableColor> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = DrawableColor::getHex;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type,
                consumes -> {
                    Property<DrawableColor> resolved = (Property<DrawableColor>) consumes.getProperty(key);
                    DrawableColor value = (resolved != null) ? resolved.get() : property.getDefault();
                    return (value != null) ? value.getHex() : null;
                },
                (b, s) -> {
                    if (s == null) return;
                    DrawableColor parsed = DrawableColor.of(s);
                    if (parsed != DrawableColor.EMPTY) {
                        Property<DrawableColor> resolved = (Property<DrawableColor>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    }
                },
                null, false, placeholders, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                (property.getDefault() != null) ? property.getDefault().getHex() : null,
                (!placeholders) ? TextValidators.HEX_COLOR_TEXT_VALIDATOR : null, null);
        return p;
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return drawableColorProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this.key = Objects.requireNonNull(key);
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
        this.contextMenuEntryLocalizationKeyBase = Objects.requireNonNull(contextMenuEntryLocalizationKeyBase);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue) {
        this(key, defaultValue, currentValue, key);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this.key = Objects.requireNonNull(key);
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
        this.contextMenuEntryLocalizationKeyBase = Objects.requireNonNull(contextMenuEntryLocalizationKeyBase);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue) {
        this(key, defaultValue, defaultValue, key);
    }

    public @NotNull String getKey() {
        return key;
    }

    public @Nullable T getDefault() {
        return defaultValue;
    }

    public Property<T> setDefault(@Nullable T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public @Nullable T get() {
        return currentValue;
    }

    /**
     * First tries to return the current value if it is not null, else will try to return the default value, and will throw an error if both are null.
     */
    public T tryGetNonNull() {
        if (this.currentValue != null) return this.currentValue;
        return Objects.requireNonNull(this.defaultValue);
    }

    /**
     * First tries to return the current value if it is not null, then tries to return the default value. If both are null, it will return {@code elseValue}.
     */
    public T tryGetNonNullElse(@NotNull T elseValue) {
        if (this.currentValue != null) return this.currentValue;
        return Objects.requireNonNullElse(this.defaultValue, elseValue);
    }

    public Property<T> set(@Nullable T currentValue) {
        this.currentValue = currentValue;
        return this;
    }

    public @Nullable ConsumingSupplier<String, T> getDeserializationCodec() {
        return deserializationCodec;
    }

    public Property<T> setDeserializationCodec(@NotNull ConsumingSupplier<String, T> deserializationCodec) {
        this.deserializationCodec = deserializationCodec;
        return this;
    }

    public @Nullable ConsumingSupplier<T, String> getSerializationCodec() {
        return serializationCodec;
    }

    public Property<T> setSerializationCodec(@NotNull ConsumingSupplier<T, String> serializationCodec) {
        this.serializationCodec = serializationCodec;
        return this;
    }

    public Property<T> deserialize(@NotNull PropertyContainer properties) {
        if (this.deserializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), new NullPointerException("No deserialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            String serialized = properties.getValue(this.getKey());
            this.currentValue = (serialized != null) ? this.deserializationCodec.get(serialized) : this.defaultValue;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
        }
        return this;
    }

    public Property<T> serialize(@NotNull PropertyContainer properties) {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            properties.putProperty(this.getKey(), this.serializationCodec.get(this.currentValue));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return this;
    }

    public @NotNull String getContextMenuEntryLocalizationKeyBase() {
        return contextMenuEntryLocalizationKeyBase;
    }

    public Property<T> setContextMenuEntryLocalizationKeyBase(@NotNull String contextMenuEntryLocalizationKeyBase) {
        this.contextMenuEntryLocalizationKeyBase = contextMenuEntryLocalizationKeyBase;
        return this;
    }

    public Property<T> setContextMenuEntrySupplier(@NotNull ContextMenuEntrySupplier<? extends PropertyHolder, T> contextMenuEntrySupplier) {
        this.contextMenuEntrySupplier = contextMenuEntrySupplier;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <H extends PropertyHolder> ContextMenu.ContextMenuEntry<?> buildContextMenuEntry(@NotNull Class<H> propertyHolderType, @NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentMenu) {
        Objects.requireNonNull(this.contextMenuEntrySupplier, "ContextMenuEntrySupplier is null! Can't build entry!");
        ContextMenuEntrySupplier<H, T> supplier = (ContextMenuEntrySupplier<H, T>) this.contextMenuEntrySupplier;
        return supplier.get(Objects.requireNonNull(propertyHolderType), this, Objects.requireNonNull(contextMenuBuilder), Objects.requireNonNull(parentMenu));
    }

    @Override
    @Nullable
    public String toString() {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return null;
        }
        try {
            return this.serializationCodec.get(this.currentValue);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return null;
    }

    @FunctionalInterface
    public interface ContextMenuEntrySupplier<H extends PropertyHolder, T> {
        @NotNull
        ContextMenu.ContextMenuEntry<?> get(@NotNull Class<H> propertyHolderType, @NotNull Property<T> property, @NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentMenu);
    }

}
