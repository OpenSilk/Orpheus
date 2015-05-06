package org.opensilk.common.ui.mortar;

import android.content.Context;

import org.opensilk.common.core.util.ObjectUtils;
import org.opensilk.common.core.util.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;

import mortar.MortarScope;

/**
 * Created by drew on 3/11/15.
 */
public class LayoutCreator {
    public static final String SERVICE_NAME = LayoutCreator.class.getName();

    private final Map<Class, Integer> layoutCache = new LinkedHashMap<>();

    public static LayoutCreator getService(Context context) {
        return (LayoutCreator) context.getSystemService(SERVICE_NAME);
    }

    public static LayoutCreator getService(MortarScope scope) {
        if (scope.hasService(SERVICE_NAME)) {
            return (LayoutCreator) scope.getService(SERVICE_NAME);
        }
        throw new IllegalArgumentException(String.format("No LayoutCreator service in scope %s", scope.getName()));
    }

    public int getLayout(Object screen) {
        Class<Object> pathType = ObjectUtils.getClass(screen);
        Integer layoutResId = layoutCache.get(pathType);
        if (layoutResId == null) {
            Layout layout = pathType.getAnnotation(Layout.class);
            Preconditions.checkNotNull(layout, "@%s annotation not found on class %s",
                    Layout.class.getSimpleName(), pathType.getName());
            layoutResId = layout.value();
            layoutCache.put(pathType, layoutResId);
        }
        return layoutResId;
    }
}
