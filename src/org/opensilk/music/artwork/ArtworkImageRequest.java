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

package org.opensilk.music.artwork;

import android.graphics.Bitmap;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.ImageRequest;

/**
 * Volley ImageRequest with support for variable priority
 * and public response methods so we can use them in
 * MediaStore requests
 *
 * Created by drew on 3/29/14.
 */
public class ArtworkImageRequest extends ImageRequest {
    public static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

    private final ArtworkType mImageType;

    private Priority mPriority = Priority.NORMAL;
    private boolean mInBackground = false;

    public ArtworkImageRequest(String url, Listener<Bitmap> listener,
                                ArtworkType imageType, ErrorListener errorListener) {
        super(url, listener, ArtworkType.getWidth(imageType), 0, BITMAP_CONFIG, errorListener);
        mImageType = imageType;
        setRetryPolicy(new DefaultRetryPolicy(2500, 2, 1.6f));
    }

    public ArtworkType getImageType() {
        return mImageType;
    }

    public void setPriority(Priority newPriority) {
        mPriority = newPriority;
    }

    @Override
    public Priority getPriority() {
        return mPriority;
    }

    /**
     * There are zero cases a request can be brought back from the background
     * once the ArtworkLoader cancels a request in cannot be uncanceled
     * if this request is still inflight when a new identical request comes in
     * volley will attach it to this and dispatch when this one finishes
     * Hence setBackground takes no arguments;
     */
    public void setBackground() {
        mPriority = Priority.LOW;
        mInBackground = true;
    }

    public boolean isInBackground() {
        return mInBackground;
    }

    @Override
    public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        return super.parseNetworkResponse(response);
    }

    @Override
    public void deliverResponse(Bitmap response) {
        super.deliverResponse(response);
    }
}
