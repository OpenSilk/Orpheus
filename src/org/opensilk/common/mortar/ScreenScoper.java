
package org.opensilk.common.mortar;

import android.content.Context;
import android.content.res.Resources;

import org.opensilk.common.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import mortar.Blueprint;
import mortar.Mortar;
import mortar.MortarScope;

import static java.lang.String.format;

/**
 * Creates {@link MortarScope}s for screens that may be annotated with {@link WithModuleFactory},
 * {@link WithModule}.
 */
public class ScreenScoper {
    private static final ModuleFactory NO_FACTORY = new ModuleFactory() {
        @Override protected Object createDaggerModule(Resources resources, Object screen) {
            throw new UnsupportedOperationException();
        }
    };

    private final Map<Class, ModuleFactory> moduleFactoryCache = new LinkedHashMap<>();

    public MortarScope getScreenScope(Context context, final MortarScreen screen) {
        MortarScope parentScope = Mortar.getScope(context);
        return getScreenScope(context.getResources(), parentScope, screen);
    }

    /**
     * Finds or creates the scope for the given screen, honoring its optoinal {@link
     * WithModuleFactory} or {@link WithModule} annotation. Note the scopes are also created
     * for unannotated screens.
     */
    public MortarScope getScreenScope(Resources resources, MortarScope parentScope,
                                      final MortarScreen screen) {
        ModuleFactory moduleFactory = getModuleFactory(screen);
        MortarScope childScope;
        if (moduleFactory != NO_FACTORY) {
            Blueprint blueprint = moduleFactory.createBlueprint(resources, screen);
            childScope = parentScope.requireChild(blueprint);
        } else {
            // We need every screen to have a scope, so that anything it injects is scoped.  We need
            // this even if the screen doesn't declare a module, because Dagger allows injection of
            // objects that are annotated even if they don't appear in a module.
            Blueprint blueprint = new Blueprint() {
                @Override public String getMortarScopeName() {
                    return screen.getName();
                }

                @Override public Object getDaggerModule() {
                    return null;
                }
            };
            childScope = parentScope.requireChild(blueprint);
        }
        return childScope;
    }

    private ModuleFactory getModuleFactory(MortarScreen screen) {
        Class<?> screenType = ObjectUtils.getClass(screen);
        ModuleFactory moduleFactory = moduleFactoryCache.get(screenType);

        if (moduleFactory != null) return moduleFactory;

        WithModule withModule = screenType.getAnnotation(WithModule.class);
        if (withModule != null) {
            Class<?> moduleClass = withModule.value();

            Constructor<?>[] constructors = moduleClass.getDeclaredConstructors();

            if (constructors.length != 1) {
                throw new IllegalArgumentException(
                        format("Module %s for screen %s should have exactly one public constructor",
                                moduleClass.getName(), screen.getName()));
            }

            Constructor constructor = constructors[0];

            Class[] parameters = constructor.getParameterTypes();

            if (parameters.length > 1) {
                throw new IllegalArgumentException(
                        format("Module %s for screen %s should have 0 or 1 parameter", moduleClass.getName(),
                                screen.getName()));
            }

            Class screenParameter;
            if (parameters.length == 1) {
                screenParameter = parameters[0];
                if (!screenParameter.isInstance(screen)) {
                    throw new IllegalArgumentException(format("Module %s for screen %s should have a "
                                    + "constructor parameter that is a super class of %s", moduleClass.getName(),
                            screen.getName(), screen.getClass().getName()));
                }
            } else {
                screenParameter = null;
            }

            try {
                if (screenParameter == null) {
                    moduleFactory = new NoArgsFactory(constructor);
                } else {
                    moduleFactory = new SingleArgFactory(constructor);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        format("Failed to instantiate module %s for screen %s", moduleClass.getName(),
                                screen.getName()), e);
            }
        }

        if (moduleFactory == null) {
            WithModuleFactory withModuleFactory = screenType.getAnnotation(WithModuleFactory.class);
            if (withModuleFactory != null) {
                Class<? extends ModuleFactory> mfClass = withModuleFactory.value();

                try {
                    moduleFactory = mfClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(format("Failed to instantiate module factory %s for screen %s",
                            withModuleFactory.value().getName(), screen.getName()), e);
                }
            }
        }

        if (moduleFactory == null) moduleFactory = NO_FACTORY;

        moduleFactoryCache.put(screenType, moduleFactory);

        return moduleFactory;
    }

    private static class NoArgsFactory extends ModuleFactory<Object> {
        final Constructor moduleConstructor;

        private NoArgsFactory(Constructor moduleConstructor) {
            this.moduleConstructor = moduleConstructor;
        }

        @Override protected Object createDaggerModule(Resources resources, Object ignored) {
            try {
                return moduleConstructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class SingleArgFactory extends ModuleFactory {
        final Constructor moduleConstructor;

        public SingleArgFactory(Constructor moduleConstructor) {
            this.moduleConstructor = moduleConstructor;
        }

        @Override protected Object createDaggerModule(Resources resources, Object screen) {
            try {
                return moduleConstructor.newInstance(screen);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
