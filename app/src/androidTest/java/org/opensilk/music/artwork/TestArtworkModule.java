package org.opensilk.music.artwork;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.opensilk.common.dagger.qualifier.ForApplication;

import java.io.IOException;
import java.util.Map;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 10/22/14.
 */
@Module (
        includes = ArtworkModule.class,
        overrides = true,
        injects = {
                ArtworkRequestManagerTest.class,
        },
        library = true
)
public class TestArtworkModule {

    @Provides @Singleton
    // override the volley request queue with mock objects
    public RequestQueue provideRequestQueue() {
        RequestQueue q = new RequestQueue(new MockCache(), new BasicNetwork(new MockHttpStack()), 1, new ImmediateResponseDelivery());
        q.start();
        return q;
    }

}
