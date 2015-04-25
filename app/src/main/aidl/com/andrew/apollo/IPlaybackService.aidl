// IPlaybackService.aidl
package com.andrew.apollo;

// Declare any non-default types here with import statements
import android.media.session.MediaSession.Token;

interface IPlaybackService {
    Token getToken();
}
