/*
 * Copyright (C) 2015 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.common.ui.mortarfragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opensilk.common.ui.mortar.LayoutCreator;
import org.opensilk.common.ui.mortar.ScreenScoper;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 3/10/15.
 */
public abstract class MortarFragment extends Fragment {

    protected MortarScope mScope;
    boolean mHitSavedInstanceState = false;
    Object mScreen;

    protected abstract Object getScreen();

    protected String getScopeName() {
        return getClass().getName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScope = findOrMakeScope();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ensureScreen();
        LayoutCreator layoutCreator = (LayoutCreator) getActivity().getSystemService(LayoutCreator.SERVICE_NAME);
        return LayoutInflater.from(mScope.createContext(getActivity())).inflate(layoutCreator.getLayout(mScreen), container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mHitSavedInstanceState = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO probably not the best
        if (!mHitSavedInstanceState && mScope != null) {
            Timber.d("Destroying fragment scope %s", getScopeName());
            mScope.destroy();
            mScope = null;
        }
    }

    protected MortarScope findOrMakeScope() {
        MortarScope scope = MortarScope.findChild(getActivity(), getScopeName());
        if (scope != null) {
            Timber.d("Reusing fragment scope %s", getScopeName());
        }
        if (scope == null) {
            ScreenScoper scoper = getScreenScoperService();
            scope = scoper.getScreenScope(getActivity(), getScopeName(), mScreen);
            Timber.d("Created new fragment scope %s", getScopeName());
        }
        return scope;
    }

    void ensureScreen() {
        if (mScreen == null) {
            mScreen = getScreen();
        }
    }

    protected ScreenScoper getScreenScoperService() {
        ensureScreen();
        return (ScreenScoper) getActivity().getSystemService(ScreenScoper.SERVICE_NAME);
    }

    public MortarScope getScope() {
        return mScope;
    }
}
