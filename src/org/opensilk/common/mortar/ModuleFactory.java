
package org.opensilk.common.mortar;

import android.content.res.Resources;
import mortar.Blueprint;

/** @see WithModuleFactory */
public abstract class ModuleFactory<T> {
  final Blueprint createBlueprint(final Resources resources, final String name, final T screen) {
    return new Blueprint() {
      @Override public String getMortarScopeName() {
        return name;
      }

      @Override public Object getDaggerModule() {
        return ModuleFactory.this.createDaggerModule(resources, screen);
      }
    };
  }

  protected abstract Object createDaggerModule(Resources resources, T screen);
}
