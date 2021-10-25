/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

/**
 * Manages beeps and vibrations.
 */
public final class BeepManager {

    private static final String TAG = BeepManager.class.getSimpleName();

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;

    private final Context context;

    private boolean beepEnabled = true;
    private boolean vibrateEnabled = false;

    public BeepManager(Activity activity) {
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // We do not keep a reference to the Activity itself, to prevent leaks
        this.context = activity.getApplicationContext();
    }

    public boolean isBeepEnabled() {
        return beepEnabled;
    }

    /**
     * Call updatePrefs() after setting this.
     *
     * If the device is in silent mode, it will not beep.
     *
     * @param beepEnabled true to enable beep
     */
    public void setBeepEnabled(boolean beepEnabled) {
        this.beepEnabled = beepEnabled;
    }

    public boolean isVibrateEnabled() {
        return vibrateEnabled;
    }

    /**
     * Call updatePrefs() after setting this.
     *
     * @param vibrateEnabled true to enable vibrate
     */
    public void setVibrateEnabled(boolean vibrateEnabled) {
        this.vibrateEnabled = vibrateEnabled;
    }

    @SuppressLint("MissingPermission")
    public synchronized void playBeepSoundAndVibrate() {
        if (beepEnabled) {
            playBeepSound();
        }
        if (vibrateEnabled) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VIBRATE_DURATION);
            }
        }
    }


    public MediaPlayer playBeepSound() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21) {
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC).build());
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            mp.stop();
            mp.reset();
            mp.release();
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.w(TAG, "Failed to beep " + what + ", " + extra);
            // possibly media player error, so release and recreate
            mp.stop();
            mp.reset();
            mp.release();
            return true;
        });
        try {
            AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.zxing_beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return mediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.reset();
            mediaPlayer.release();
            return null;
        }
    }
}
