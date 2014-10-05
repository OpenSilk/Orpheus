package org.opensilk.music.loader;

import java.util.List;

/**
 * Created by drew on 10/4/14.
 */
public interface LoaderCallback<T> {
    public void onLoadComplete(List<T> items);
}
