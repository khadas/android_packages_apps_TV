/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tv.droidlogic.subtitleui;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.media.tv.TvTrackInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.tv.ui.sidepanel.SwitchItem;
import com.android.tv.ui.sidepanel.SideFragment;
import com.android.tv.ui.sidepanel.Item;
import com.android.tv.ui.sidepanel.DividerItem;
import com.android.tv.ui.sidepanel.RadioButtonItem;
import com.android.tv.ui.sidepanel.SubMenuItem;
import com.android.tv.util.CaptionSettings;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import com.android.tv.R;

public class TeleTextFragment extends SideFragment {
    private static final String TRACKER_LABEL ="teletext" ;
    private List<Item> mItems;
    private boolean mPaused;

    public TeleTextFragment() {
        super(KeyEvent.KEYCODE_CAPTIONS, KeyEvent.KEYCODE_S);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.side_panel_title_teletext);
    }

    @Override
    public String getTrackerLabel() {
        return TRACKER_LABEL;
    }

    private String getLabel(TvTrackInfo track) {
        if (track.getLanguage() != null) {
            return new Locale(track.getLanguage()).getDisplayName();
        }
        return "";
    }

    @Override
    protected List<Item> getItemList() {

        mItems = new ArrayList<>();

        List<TvTrackInfo> tracks = getMainActivity().getTracks(TvTrackInfo.TYPE_SUBTITLE);
        if (tracks != null && !tracks.isEmpty()) {
            String trackId = SubtitleFragment.getCaptionsEnabled(getMainActivity()) ?
                    getMainActivity().getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE) : null;
            boolean isEnabled = trackId != null;

            for (final TvTrackInfo track : tracks) {
                TeleTextOptionItem item = new TeleTextOptionItem(getLabel(track),
                        CaptionSettings.OPTION_ON, track.getId(), track.getLanguage());
                if (isEnabled && track.getId().equals(trackId)) {
                    item.setChecked(true);
                }
                mItems.add(item);
            }
        }

        //mItems.add(new DividerItem(getString(R.string.subtitle_teletext_settings)));
        mItems.add(new SubMenuItem(getString(R.string.subtitle_teletext_open),
                null, getMainActivity().getOverlayManager().getSideFragmentManager()) {
            @Override
            protected SideFragment getFragment() {
                SideFragment fragment = new TeleTextSettingFragment();
                fragment.setListener(mSideFragmentListener);
                return fragment;
            }
        });

        return mItems;
    }

    private final SideFragmentListener mSideFragmentListener = new SideFragmentListener() {
        @Override
        public void onSideFragmentViewDestroyed() {
            notifyDataSetChanged();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPaused) {
            resetItemList(getItemList());
        }
        mPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private class TeleTextOptionItem extends RadioButtonItem {
        private final int mOption;
        private final String mTrackId;
        private final String mLanguage;

        private TeleTextOptionItem(String title, int option, String trackId, String language) {
            super(title);
            mOption = option;
            mTrackId = trackId;
            mLanguage = language;
        }

        @Override
        protected void onSelected() {
            super.onSelected();
            getMainActivity().selectSubtitleTrack(mOption, mTrackId);
            //closeFragment();
        }

        @Override
        protected void onFocused() {
            super.onFocused();
        }
    }
}