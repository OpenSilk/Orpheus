package org.opensilk.common.mortarflow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenContextFactory;
import org.opensilk.common.mortar.ScreenScoper;

import mortar.Mortar;
import mortar.MortarScope;

public final class MortarContextFactory implements ScreenContextFactory {
  private final ScreenScoper screenScoper = new ScreenScoper();

  @Override public Context setUpContext(Screen screen, Context parentContext) {
    MortarScope screenScope = screenScoper.getScreenScope(parentContext, screen.getName(), screen);
    return new TearDownContext(parentContext, screenScope);
  }

  @Override public void tearDownContext(Context context) {
    TearDownContext.destroyScope(context);
  }

  static class TearDownContext extends ContextWrapper {
    private static final String SERVICE = "SNEAKY_MORTAR_PARENT_HOOK";
    private final MortarScope parentScope;
    private LayoutInflater inflater;

    static void destroyScope(Context context) {
      MortarScope child = Mortar.getScope(context);
      MortarScope parent = (MortarScope) context.getSystemService(SERVICE);
      parent.destroyChild(child);
    }

    public TearDownContext(Context context, MortarScope scope) {
      super(scope.createContext(context));
      this.parentScope = Mortar.getScope(context);
    }

    @Override public Object getSystemService(String name) {
      if (LAYOUT_INFLATER_SERVICE.equals(name)) {
        if (inflater == null) {
          inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
        }
        return inflater;
      }

      if (SERVICE.equals(name)) {
        return parentScope;
      }

      return super.getSystemService(name);
    }
  }
}
