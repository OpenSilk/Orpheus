package org.opensilk.music.playback.service;

import android.media.session.MediaSession.Token;

interface IPlaybackService {
    Token getToken();
}