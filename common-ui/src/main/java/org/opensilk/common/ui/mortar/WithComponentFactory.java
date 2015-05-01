package org.opensilk.common.ui.mortar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import mortar.MortarScope;

/**
 * Marks a screen as defining a {@link MortarScope}, with a factory class to
 * create its Dagger module.
 *
 * @see org.opensilk.common.mortar.WithComponent
 * @see org.opensilk.common.mortar.ScreenScoper
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface WithComponentFactory {
  Class<? extends ComponentFactory> value();
}
