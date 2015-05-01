package org.opensilk.common.ui.mortar;

import android.content.res.Resources;

import mortar.MortarScope;

/** @see org.opensilk.common.mortar.WithComponentFactory */
public abstract class ComponentFactory<T> {
  protected abstract Object createDaggerComponent(Resources resources, MortarScope parentScope, T screen);
}
