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

package com.android.tv.ui.sidepanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;
import com.android.tv.data.Channel;
import android.view.KeyEvent;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.ChannelInfo;

import com.android.tv.R;

public class MultiOptionFragment extends SideFragment {
    private static final String TAG = "MultiOptionFragment";
    private int mInitialSelectedPosition = INVALID_POSITION;
    private String mSelectedTrackId, mListSelectedTrackId;
    private String mFocusedTrackId, mListFocusedTrackId;
    private int mKeyValue;

    private final static String[] TYPE = {"PMODE", "SMODE", "RATIO", "FAV", "LIST", "SLEEP"};
    private final static String[] LISTKEYWORD = {"PROGLIST", "FAVLIST"};
    private final static int[] KEYVALUE = {DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE, DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE,
        DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE, DroidLogicKeyEvent.KEYCODE_FAV,
        DroidLogicKeyEvent.KEYCODE_LIST, DroidLogicKeyEvent.KEYCODE_TV_SLEEP};

    private final static int[] FAV = {R.string.favourite_list, R.string.tv, R.string.radio};
    private final static int[] LIST = {R.string.channel_list, R.string.tv, R.string.radio};

    private final static int TVLIST = 0;
    private final static int RADIOLIST = 1;
    private final static int HIDELIST = -1;
    private boolean isListShow = false;

    public MultiOptionFragment(int keyvalue) {
        super(keyvalue, KeyEvent.KEYCODE_UNKNOWN);
        this.mKeyValue = keyvalue;
    }

    @Override
    protected String getTitle() {
        return getTitle(mKeyValue);
    }

    @Override
    public String getTrackerLabel() {
        return TAG;
    }

    private String getTitle(int value) {
        String title = null;
        switch (value) {
            case DroidLogicKeyEvent.KEYCODE_FAV: {
                title = getString(FAV[0]);
                break;
            }
            case DroidLogicKeyEvent.KEYCODE_LIST: {
                title = getString(LIST[0]);
                break;
            }
        }
        return title;
    }

    private String getKeyWords(int value) {
        String keywords = null;
        switch (value) {
            case DroidLogicKeyEvent.KEYCODE_FAV: {
                keywords = TYPE[3];
                break;
            }
            case DroidLogicKeyEvent.KEYCODE_LIST: {
                keywords = TYPE[4];
                break;
            }
        }
        return keywords;
    }

    private String[] getAllOption(int Value) {
        int[] id = null;
        switch (Value) {
            case DroidLogicKeyEvent.KEYCODE_FAV: {
                id = FAV;
                break;
            }
            case DroidLogicKeyEvent.KEYCODE_LIST: {
                id = LIST;
                break;
            }
        }
        if (id != null) {
            String[] idtostring = new String[id.length];
            for (int i = 0; i < id.length; i++) {
                idtostring[i] = getString(id[i]);
            }
            return idtostring;
        }
        return null;
    }

    @Override
    protected List<Item> getItemList() {
        String[] getoption = getAllOption(mKeyValue);
        String slectedindextest = read(getKeyWords(mKeyValue));
        List<Item> items = new ArrayList<>();
        if (getoption != null) {
            int pos = 0;
            for (int i = 1; i < getoption.length; i++) {
                RadioButtonItem item = new MultiAudioOptionItem(
                    getoption[i], String.valueOf(i - 1));
                if (slectedindextest.equals(String.valueOf(i - 1))) {
                    item.setChecked(true);
                    mInitialSelectedPosition = pos;
                    mSelectedTrackId = mFocusedTrackId = String.valueOf(i - 1);
                } else if (slectedindextest.equals("-1") && (i - 1) == 0) {
                    item.setChecked(true);
                    mInitialSelectedPosition = pos;
                    mSelectedTrackId = mFocusedTrackId = String.valueOf(i - 1);
                }
                items.add(item);
                ++pos;
            }
        }
        return items;
    }

    private Map<String, Object> allchannelsinfo;
    private ArrayList<ChannelInfo> videochannels;
    private ArrayList<ChannelInfo> videochannelsfav;
    private ArrayList<ChannelInfo> radiochannels;
    private ArrayList<ChannelInfo> radiochannelsfav;
    private long index;
    private ArrayList<ChannelInfo> getlist;
    private List<Channel> allchannel;
    private int videonumber, radionumber;
    private Map<Long, Channel> channelmap;

    private void refreshItems(int select) {
        allchannelsinfo = getMainActivity().mQuickKeyInfo.getList(mKeyValue);
        videochannels = (ArrayList<ChannelInfo>) (allchannelsinfo.get("video"));
        radiochannels = (ArrayList<ChannelInfo>) (allchannelsinfo.get("radio"));
        allchannel =  (List<Channel>) (allchannelsinfo.get("channel"));
        index =(long) (allchannelsinfo.get("index"));
        channelmap = (Map<Long, Channel>) (allchannelsinfo.get("channelmap"));

        if (videochannels != null) {
            videochannelsfav = getFavList(videochannels);
            videonumber = videochannels.size();
        }
        if (radiochannels != null) {
            radiochannelsfav = getFavList(radiochannels);
            radionumber = radiochannels.size();
        }

        String[] getoption = getAllOption(mKeyValue);
        String keywords, slectedone;
        if (DroidLogicKeyEvent.KEYCODE_LIST == mKeyValue) {
            if (select == TVLIST) {
                getlist = videochannels;
            } else {
                getlist = radiochannels;
            }
        } else {
            if (select == TVLIST) {
                getlist = videochannelsfav;
            } else {
                getlist = radiochannelsfav;
            }
        }
        List<Item> items = new ArrayList<>();
        if (true) {
            MultiAudioOptionItem item1 = new MultiAudioOptionItem(
                getoption[1], String.valueOf(0));
            if (read(getKeyWords(mKeyValue)).equals(String.valueOf(0))) {
                item1.setChecked(true);
            } else {
                item1.setChecked(false);
            }
            items.add(item1);

            if (getlist != null && select == TVLIST) {
                int pos = 0;
                for (int i = 0; i < getlist.size(); i++) {
                MultiListItem item = new MultiListItem(
                    getlist.get(i).getDisplayNumber() + "  " + getlist.get(i).getDisplayName(), String.valueOf(i), TVLIST);
                    if (index == getlist.get(i).getId()) {
                        item.setChecked(true);
                    }
                    items.add(item);
                    ++pos;
                }
            }
            MultiAudioOptionItem item2 = new MultiAudioOptionItem(
                getoption[2], String.valueOf(1));
            if (read(getKeyWords(mKeyValue)).equals(String.valueOf(1))) {
                item2.setChecked(true);
            } else {
                item2.setChecked(false);
            }
            items.add(item2);
            if (getlist != null && select == RADIOLIST) {
                int pos = 0;
                for (int i = 0; i < getlist.size(); i++) {
                MultiListItem item = new MultiListItem(
                    getlist.get(i).getDisplayNumber() + "  " + getlist.get(i).getDisplayName(), String.valueOf(i), RADIOLIST);
                if (index == getlist.get(i).getId()) {
                    item.setChecked(true);
                }
                    items.add(item);
                    ++pos;
                }
            }
        }
        setItems(items);
    }

    private ArrayList<ChannelInfo> getFavList(ArrayList<ChannelInfo> list) {
        ArrayList<ChannelInfo> favList = new ArrayList<ChannelInfo>();
        for (int i = 0; i < list.size(); i++) {
            ChannelInfo info = list.get(i);
            if (info.isFavourite())
                favList.add(info);
        }
        return favList;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mInitialSelectedPosition != INVALID_POSITION) {
            setSelectedPosition(mInitialSelectedPosition);
        }
    }

    private class MultiAudioOptionItem extends RadioButtonItem {
        private final String mTrackId;

        private MultiAudioOptionItem(String title, String trackId) {
            super(title);
            mTrackId = trackId;
        }

        @Override
        protected void onSelected() {
            super.onSelected();
            mSelectedTrackId = mFocusedTrackId = mTrackId;
            String previous = read(getKeyWords(mKeyValue));
            store(getKeyWords(mKeyValue), mTrackId);
            if (!isListShow) {
                isListShow = true;
                refreshItems(Integer.parseInt(mTrackId));
            } else {
                isListShow = false;
                if (!previous.equals(mTrackId)) {
                    refreshItems(Integer.parseInt(mTrackId));
                } else {
                    refreshItems(HIDELIST);
                }
            }
            //closeFragment();
        }

        @Override
        protected void onFocused() {
            super.onFocused();
            mFocusedTrackId = mTrackId;
        }
    }

    private class MultiListItem extends ListItem {
        private final String mTrackId;
        private final int mType;

        private MultiListItem(String title, String trackId, int type) {
            super(title);
            mTrackId = trackId;
            mType = type;
        }

        @Override
        protected void onSelected() {
            super.onSelected();
            mListSelectedTrackId = mListFocusedTrackId = mTrackId;

            Channel  channel = channelmap.get(getlist.get(Integer.parseInt(mTrackId)).getId());
            getMainActivity().tuneToChannel(channel);
            refreshItems(mType);
            //closeFragment();
        }

        @Override
        protected void onFocused() {
            super.onFocused();
            mListFocusedTrackId = mTrackId;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void store(String keyword, String content) {
        SharedPreferences DealData = getMainActivity().getApplicationContext().getSharedPreferences("database", 0);
        Editor editor = DealData.edit();
        editor.putString(keyword, content);
        editor.commit();
        Log.d(TAG, "store keyword: " + keyword + ",content: " + content);
    }

    private String read(String keyword) {
        SharedPreferences DealData = getMainActivity().getApplicationContext().getSharedPreferences("database", 0);
        Log.d(TAG, "read keyword: " + keyword + ",value: " + DealData.getString(keyword, "0"));
        return DealData.getString(keyword, "-1");
    }
}

