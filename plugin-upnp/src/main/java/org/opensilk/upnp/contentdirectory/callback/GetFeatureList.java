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

package org.opensilk.upnp.contentdirectory.callback;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.ActionArgument;
import org.fourthline.cling.model.meta.Service;
import org.opensilk.upnp.contentdirectory.Features;
import org.opensilk.upnp.contentdirectory.FeaturesParser;

import java.lang.reflect.Method;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 6/17/14.
 */
public abstract class GetFeatureList extends ActionCallback {

    public GetFeatureList(Service service) {
        super(buildActionInvocation(service));
    }

    @Override
    @DebugLog
    public void success(ActionInvocation actionInvocation) {
        ActionArgumentValue aav = actionInvocation.getOutput("FeatureList");
        if (aav != null) {
            try {
                Features f = new FeaturesParser().parse(aav.toString());
                received(actionInvocation, f);
                return;
            } catch (Exception e) {
                // fall
            }
        }
        failure(actionInvocation, null);
    }

    /*
     * X_GetFeatureList isnt an advertised action so we have to hack cling a bit to get it working
     */
    private static ActionInvocation buildActionInvocation(Service service) {
         // not really a A_ARG_TYPE_Result type but close enough
        ActionArgument argument = new ActionArgument("FeatureList", "A_ARG_TYPE_Result", ActionArgument.Direction.OUT);
        Action action = new Action("X_GetFeatureList", new ActionArgument[]{argument});
        try {
            // package method must be reflected
            Method m = Action.class.getDeclaredMethod("setService", Service.class);
            m.setAccessible(true);
            m.invoke(action, service);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ActionInvocation(action);
    }

    public abstract void received(ActionInvocation actionInvocation, Features features);

}
