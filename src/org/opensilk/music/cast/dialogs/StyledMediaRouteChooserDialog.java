/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.cast.dialogs;

import android.content.Context;
import android.support.v7.app.MediaRouteChooserDialog;
import android.support.v7.media.MediaRouter;

import com.andrew.apollo.R;

/**
 * This class implements the route chooser dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to choose a route that matches a given selector.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 */
public class StyledMediaRouteChooserDialog extends MediaRouteChooserDialog {

    public StyledMediaRouteChooserDialog(Context context) {
        this(context, R.style.Orpheus_CastDialog);
    }

    public StyledMediaRouteChooserDialog(Context context, int theme) {
        super(context, theme);
    }

}
