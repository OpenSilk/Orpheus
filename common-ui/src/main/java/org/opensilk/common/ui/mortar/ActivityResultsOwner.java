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

package org.opensilk.common.ui.mortar;

import android.content.Intent;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.util.VersionUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.Presenter;
import mortar.Scoped;
import mortar.bundler.BundleService;

/**
 * Created by drew on 3/10/15.
 */
@ActivityScope
public class ActivityResultsOwner extends Presenter<ActivityResultsActivity> implements ActivityResultsController {

    private final Set<Registration> registrations = new LinkedHashSet<>();

    @Inject
    public ActivityResultsOwner() {
    }

    @Override
    protected BundleService extractBundleService(ActivityResultsActivity view) {
        return BundleService.getBundleService(view.getScope());
    }

    @Override
    public void onExitScope() {
        registrations.clear();
    }

    @Override
    public void register(MortarScope scope, ActivityResultsListener listener) {
        Registration registration = new Registration(listener);
        scope.register(registration);
        boolean added = registrations.add(registration);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        if (hasView()) {
            if (VersionUtils.hasApi16()) {
                getView().startActivityForResult(intent, requestCode, options);
            } else {
                getView().startActivityForResult(intent, requestCode);
            }
        }
    }

    @Override
    public void setResultAndFinish(int resultCode, Intent data) {
        if (hasView()) {
            getView().setResultAndFinish(resultCode, data);
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Registration r : registrations) {
            if (r.registrant.onActivityResult(requestCode, resultCode, data)) {
                return true;
            }
        }
        return false;
    }

    class Registration implements Scoped {
        final ActivityResultsListener registrant;

        Registration(ActivityResultsListener registrant) {
            this.registrant = registrant;
        }

        @Override
        public void onEnterScope(MortarScope scope) {
        }

        @Override
        public void onExitScope() {
            registrations.remove(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Registration that = (Registration) o;

            return registrant.equals(that.registrant);
        }

        @Override
        public int hashCode() {
            return registrant.hashCode();
        }
    }
}
