package org.opensilk.common.ui.mortar;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;

import mortar.MortarScope;

/**
 * Semantically the same as the mortar sample but just the flow parts removed
 * Pager uses this to take care of scoping
 */
public final class MortarContextFactory {
  private final ScreenScoper screenScoper;

  public MortarContextFactory() {
    this.screenScoper = new ScreenScoper();
  }

  public MortarContextFactory(ScreenScoper screenScoper) {
    this.screenScoper = screenScoper;
  }

  public Context setUpContext(Screen path, Context parentContext) {
    MortarScope screenScope =
        screenScoper.getScreenScope(parentContext, path);
    return new TearDownContext(parentContext, screenScope);
  }

  public void tearDownContext(Context context) {
    TearDownContext.destroyScope(context);
  }

  static class TearDownContext extends ContextWrapper {
    private static final String SERVICE = "SNEAKY_MORTAR_PARENT_HOOK";
    private final MortarScope parentScope;
    private LayoutInflater inflater;

    static void destroyScope(Context context) {
      MortarScope.getScope(context).destroy();
    }

    public TearDownContext(Context context, MortarScope scope) {
      super(scope.createContext(context));
      this.parentScope = MortarScope.getScope(context);
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
