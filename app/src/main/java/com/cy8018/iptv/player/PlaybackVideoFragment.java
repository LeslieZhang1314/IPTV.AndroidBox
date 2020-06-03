/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cy8018.iptv.player;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.PlaybackGlue;

import com.cy8018.iptv.model.Station;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Handles video playback with media controls.
 */
public class PlaybackVideoFragment extends VideoSupportFragment {

    private VideoMediaPlayerGlue<ExoPlayerAdapter> mMediaPlayerGlue;
    final VideoSupportFragmentGlueHost mHost = new VideoSupportFragmentGlueHost(this);
    private Station currentStation = null;
    private ArrayList<Station> mStationList;
    private int currentSourceIndex = 0;

    // message to show the control overlay
    public static final int MSG_SHOW_CONTROL = 0;

    // message to hide the control overlay
    public static final int MSG_HIDE_CONTROL = 1;

    public static final int CONTROL_OVERLAY_FADE_TIME = 5;

    public static long lastActiveTimeStamp = 0;

    public final MsgHandler mHandler = new MsgHandler(this);


    private static final String TAG = "PlaybackVideoFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBackgroundType(BG_LIGHT);
        ExoPlayerAdapter playerAdapter = new ExoPlayerAdapter(getActivity());
        mMediaPlayerGlue = new VideoMediaPlayerGlue(getActivity(), playerAdapter);
        mMediaPlayerGlue.setHost(mHost);

        mMediaPlayerGlue.setSeekEnabled(false);
        mMediaPlayerGlue.setControlsOverlayAutoHideEnabled(true);

        mStationList = getActivity().getIntent().getParcelableArrayListExtra("stationList");
        currentStation = getActivity().getIntent().getParcelableExtra("currentStation");

        mMediaPlayerGlue.setTitle(currentStation.name);
        mMediaPlayerGlue.setSubtitle("1/" + currentStation.url.size());
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(currentStation.url.get(0)));

        mMediaPlayerGlue.setCurrentStation(currentStation);
        mMediaPlayerGlue.playWhenPrepared();

        SetLastActiveTime();
        new Thread(controlOverlayCheckRunnable).start();
    }

    static void playWhenReady(VideoMediaPlayerGlue glue) {
        if (glue.isPrepared()) {
            glue.play();
        } else {
            glue.addPlayerCallback(new VideoMediaPlayerGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    if (glue.isPrepared()) {
                        glue.removePlayerCallback(this);
                        glue.play();
                    }
                }
            });
        }
    }

    public void SwitchChanel(boolean isForward) {
        Log.d(TAG, "SwitchChanel: isForward:"+ isForward);

        int index = 0;

        if (isForward)
        {
            index = currentStation.index + 1;
            if (index >= mStationList.size()) {
                index = 0;
            }
        }
        else {
            index = currentStation.index - 1;
            if (index < 0) {
                index = mStationList.size() - 1;
            }
        }

        currentStation = mStationList.get(index);

        mMediaPlayerGlue.setTitle(currentStation.name);
        mMediaPlayerGlue.setSubtitle("1/" + currentStation.url.size());
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(currentStation.url.get(0)));

        currentSourceIndex = 0;
        mMediaPlayerGlue.setCurrentStation(currentStation);
        playWhenReady(mMediaPlayerGlue);
    }

    public void SwitchSource(boolean isForward) {
        Log.d(TAG, "SwitchChanel: isForward:"+ isForward);

        int index = 0;

        if (isForward)
        {
            index = currentSourceIndex + 1;
            if (index >= currentStation.url.size()) {
                index = 0;
            }
        }
        else {
            index = currentSourceIndex - 1;
            if (index < 0) {
                index = currentStation.url.size() - 1;
            }
        }

        currentSourceIndex = index;
        mMediaPlayerGlue.setTitle(currentStation.name);
        mMediaPlayerGlue.setSubtitle(index + 1 + "/" + currentStation.url.size());
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(currentStation.url.get(index)));

        playWhenReady(mMediaPlayerGlue);
    }

    public void ShowControl() {
        showControlsOverlay(true);
        SetLastActiveTime();
    }

    public void SetLastActiveTime() {
        lastActiveTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.pause();
        }
    }

    public static class MsgHandler extends Handler {
        WeakReference<PlaybackVideoFragment> mMainActivityWeakReference;

        MsgHandler(PlaybackVideoFragment playbackVideoFragment) {
            mMainActivityWeakReference = new WeakReference<>(playbackVideoFragment);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            super.handleMessage(msg);

            Log.d(TAG, "Handler: msg.what = " + msg.what);

            PlaybackVideoFragment playbackVideoFragment = mMainActivityWeakReference.get();

            if (msg.what == MSG_SHOW_CONTROL) {
                playbackVideoFragment.showControlsOverlay(true);
            }
            else if (msg.what == MSG_HIDE_CONTROL) {
                playbackVideoFragment.hideControlsOverlay(true);
            }
        }
    }

    Runnable controlOverlayCheckRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {

                try {
                    long nowTimeStamp = System.currentTimeMillis();
                    long timeDiff = (nowTimeStamp - lastActiveTimeStamp);
                    Log.d(TAG, "time diff: " + timeDiff);
                    if (timeDiff > CONTROL_OVERLAY_FADE_TIME * 1000) {
                        mHandler.sendEmptyMessage(MSG_HIDE_CONTROL);
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}