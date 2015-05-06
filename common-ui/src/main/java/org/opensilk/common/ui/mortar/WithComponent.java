package org.opensilk.common.ui.mortar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import mortar.MortarScope;

/**
 * Marks a screen as defining a {@link MortarScope}, with the class of a Dagger module
 * to instantiate via reflection. The module must be a static type with a default
 * constructor. For more flexibility, use {@link WithComponentFactory}.
 *
 * @deprecated Use {@link WithComponentFactory} from now on.
 *
 * @see org.opensilk.common.mortar.ScreenScoper
 */
@Deprecated
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface WithComponent {
  Class<?> value();
}
