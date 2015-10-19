package org.opensilk.common.ui.mortar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import dagger.Component;
import mortar.MortarScope;

import static java.lang.String.format;
import static org.opensilk.common.core.mortar.DaggerService.DAGGER_SERVICE;

/**
 * Creates {@link MortarScope}s for screens that may be annotated with {@link WithComponentFactory},
 * {@link WithComponent}.
 *
 * Taken from mortar sample, modified for dagger2 the {@link WithComponent} annotation
 * requires only one {@link dagger.Component#modules() module} defined with either a default
 * constuctor or single arg constructor that takes the containing {@link flow.Path screen}
 * the component dependencies are assumed to be a single component that can be fetched with
 * {@link mortar.dagger2support.DaggerService#getDaggerComponent(android.content.Context)}
 * from the parent context.
 *
 * TODO remove @WithComponent, @WithComponentFactory is more flexible and uses way less reflection.
 */
public class ScreenScoper {
    public static final String SERVICE_NAME = ScreenScoper.class.getName();

    private static final ComponentFactory NO_FACTORY = new ComponentFactory() {
        @Override protected Object createDaggerComponent(Resources resources, MortarScope parentScope, Object screen) {
            throw new UnsupportedOperationException();
        }
    };

    private final Map<Class, ComponentFactory> moduleFactoryCache = new LinkedHashMap<>();

    @SuppressLint("WrongConstant")
    public static ScreenScoper getService(Context context) {
        //noinspection ResourceType
        return (ScreenScoper) context.getSystemService(SERVICE_NAME);
    }

    public static ScreenScoper getService(MortarScope scope) {
        if (scope.hasService(SERVICE_NAME)) {
            return scope.getService(SERVICE_NAME);
        }
        throw new IllegalArgumentException(String.format("No ScreenScoper service in scope %s", scope.getName()));
    }

    public MortarScope getScreenScope(Context context, Screen screen, Object... services) {
        MortarScope parentScope = MortarScope.getScope(context);
        return getScreenScope(context.getResources(), parentScope, screen, services);
    }

    /**
     * Finds or creates the scope for the given screen, honoring its optional {@link
     * WithComponentFactory} or {@link WithComponent} annotation. Note that scopes are also created
     * for unannotated screens.
     */
    public MortarScope getScreenScope(Resources resources, MortarScope parentScope, Screen screen, Object... services) {
        MortarScope childScope = parentScope.findChild(screen.getName());
        if (childScope == null) {
            ComponentFactory componentFactory = getModuleFactory(screen);

            Object childComponent;
            if (componentFactory != NO_FACTORY) {
                childComponent = componentFactory.createDaggerComponent(resources, parentScope, screen);
            } else {
                throw new IllegalArgumentException("Screen must have a component");
            }

            MortarScope.Builder bob = parentScope.buildChild()
                    .withService(DAGGER_SERVICE, childComponent);

            if (services != null && services.length > 0) {
                if (services.length % 2 != 0) {
                    throw new IllegalArgumentException("Services must be name, service pairs");
                }
                for (int ii=0; ii<services.length; ii+=2) {
                    bob.withService((String) services[ii], services[ii+1]);
                }
            }
            childScope = bob.build(screen.getName());
        }
        return childScope;
    }

    private ComponentFactory getModuleFactory(Object screen) {
        Class<?> screenType = ObjectUtils.getClass(screen);
        ComponentFactory componentFactory = moduleFactoryCache.get(screenType);

        if (componentFactory != null) return componentFactory;

        WithComponent withComponent = screenType.getAnnotation(WithComponent.class);
        if (withComponent != null) {

            //component class
            Class<?> componentClass = withComponent.value();
            Component component = componentClass.getAnnotation(Component.class);

            // get modules
            Class<?>[] modules = component.modules();

            //only want to deal with on module at the moment
            if (modules.length != 1) {
                throw new IllegalArgumentException(
                        format("Component %s for screen %s can only have one module." +
                                        "Use the factory or fix this if you need more than one",
                                componentClass.getName(), screen));
            }

            Class<?> moduleClass = modules[0];

            Constructor<?>[] constructors = moduleClass.getDeclaredConstructors();

            if (constructors.length != 1) {
                throw new IllegalArgumentException(
                        format("Module %s for screen %s should have exactly one public constructor",
                                moduleClass.getName(), screen));
            }

            Constructor moduleConstructor = constructors[0];

            Class[] parameters = moduleConstructor.getParameterTypes();

            if (parameters.length > 1) {
                throw new IllegalArgumentException(
                        format("Module %s for screen %s should have 0 or 1 parameter", moduleClass.getName(),
                                screen));
            }

            Class screenParameter;
            if (parameters.length == 1) {
                screenParameter = parameters[0];
                if (!screenParameter.isInstance(screen)) {
                    throw new IllegalArgumentException(format("Module %s for screen %s should have a "
                                    + "constructor parameter that is a super class of %s", moduleClass.getName(),
                            screen, screen.getClass().getName()));
                }
            } else {
                screenParameter = null;
            }

            try {
                if (screenParameter == null) {
                    componentFactory = new NoArgsFactory(componentClass, moduleConstructor);
                } else {
                    componentFactory = new SingleArgFactory(componentClass, moduleConstructor);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        format("Failed to instantiate module %s for screen %s", moduleClass.getName(), screen),
                        e);
            }
        }

        if (componentFactory == null) {
            WithComponentFactory withComponentFactory = screenType.getAnnotation(WithComponentFactory.class);
            if (withComponentFactory != null) {
                Class<? extends ComponentFactory> mfClass = withComponentFactory.value();

                try {
                    componentFactory = mfClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(format("Failed to instantiate module factory %s for screen %s",
                            withComponentFactory.value().getName(), screen), e);
                }
            }
        }

        if (componentFactory == null) componentFactory = NO_FACTORY;

        moduleFactoryCache.put(screenType, componentFactory);

        return componentFactory;
    }

    private static class NoArgsFactory extends ComponentFactory<Object> {
        final Class component;
        final Constructor moduleConstructor;

        private NoArgsFactory(Class component, Constructor moduleConstructor) {
            this.component = component;
            this.moduleConstructor = moduleConstructor;
        }

        @Override protected Object createDaggerComponent(Resources resources, MortarScope parentScope, Object ignored) {
            Object parentComponent = parentScope.getService(DAGGER_SERVICE);
            if (parentComponent != null) {
                return DaggerService.createComponent(component, parentComponent);
            } else {
                return DaggerService.createComponent(component);
            }
        }
    }

    private static class SingleArgFactory extends ComponentFactory {
        final Class component;
        final Constructor moduleConstructor;

        public SingleArgFactory(Class component, Constructor moduleConstructor) {
            this.component = component;
            this.moduleConstructor = moduleConstructor;
        }

        @Override protected Object createDaggerComponent(Resources resources, MortarScope parentScope, Object screen) {
            try {
                Object module = moduleConstructor.newInstance(screen);
                Object parentComponent = parentScope.getService(DAGGER_SERVICE);
                if (parentComponent != null) {
                    return DaggerService.createComponent(component, module, parentComponent);
                } else {
                    return DaggerService.createComponent(component, module);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
