/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.mortarflow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.flow.ScreenContextFactory;
import org.opensilk.common.mortar.ScreenScoper;

import mortar.Mortar;
import mortar.MortarScope;

/**
 * Created by drew on 11/2/14.
 */
public class MortarAppFlowContextFactory implements ScreenContextFactory {
    private final ScreenScoper screenScoper = new ScreenScoper();
    private final AppFlow appFlow;

    public MortarAppFlowContextFactory(AppFlow appFlow) {
        this.appFlow = appFlow;
    }

    @Override public Context setUpContext(Screen screen, Context parentContext) {
        MortarScope screenScope = screenScoper.getScreenScope(parentContext, screen.getName(), screen);
        return new TearDownContext(parentContext, screenScope, appFlow);
    }

    @Override public void tearDownContext(Context context) {
        TearDownContext.destroyScope(context);
    }

    static class TearDownContext extends ContextWrapper {
        private static final String SERVICE = "SNEAKY_MORTAR_PARENT_HOOK";
        private final MortarScope parentScope;
        private final AppFlow appFlow;
        private LayoutInflater inflater;

        static void destroyScope(Context context) {
            MortarScope child = Mortar.getScope(context);
            MortarScope parent = (MortarScope) context.getSystemService(SERVICE);
            parent.destroyChild(child);
        }

        public TearDownContext(Context context, MortarScope scope, AppFlow appFlow) {
            super(scope.createContext(context));
            this.parentScope = Mortar.getScope(context);
            this.appFlow = appFlow;
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

            if (AppFlow.isAppFlowSystemService(name)) {
                return appFlow;
            }

            return super.getSystemService(name);
        }
    }
}
