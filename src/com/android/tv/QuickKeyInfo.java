package com.android.tv;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvContract.Channels;
import android.provider.Settings;
import android.app.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.tv.util.TvInputManagerHelper;
import com.android.tv.data.Channel;

import com.droidlogic.app.DroidLogicKeyEvent;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.TvDataBaseManager;

public class QuickKeyInfo  {
    private TvInputManagerHelper mTvInputManagerHelper;
    private Context mContext;
    private ChannelTuner mChannelTuner;
    private MainActivity mActivity;

    public static final int KEYCODE_TV_SHORTCUTKEY_VIEWMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE;
    public static final int KEYCODE_TV_SHORTCUTKEY_VOICEMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE;
    public static final int KEYCODE_TV_SHORTCUTKEY_DISPAYMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE;
    public static final int KEYCODE_TV_SLEEP = DroidLogicKeyEvent.KEYCODE_TV_SLEEP;
    public static final int KEYCODE_FAV = DroidLogicKeyEvent.KEYCODE_FAV;
    public static final int KEYCODE_LIST = DroidLogicKeyEvent.KEYCODE_LIST;
    public static final int KEYCODE_LAST_CHANNEL = DroidLogicKeyEvent.KEYCODE_LAST_CHANNEL;

    public QuickKeyInfo(MainActivity mainactivity, TvInputManagerHelper tvinputmanagerhelper, ChannelTuner channeltuner) {
        this.mChannelTuner = channeltuner;
        this.mTvInputManagerHelper = tvinputmanagerhelper;
        this.mActivity = mainactivity;
        this.mContext = mainactivity.getApplicationContext();
    }

    public void QuickKeyAction (int eventkey) {
        String droidlivetv = "com.droidlogic.droidlivetv/com.droidlogic.droidlivetv.DroidLiveTvActivity";
        Bundle mBundle = new Bundle();
        mBundle.putInt("eventkey", eventkey);
        mBundle.putInt("deviceid", getDeviceId());
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(droidlivetv));
        intent.putExtras(mBundle);
        mContext.startActivity(intent);
    }

    private int getDeviceId() {
        TvInputManager tvinputManager = (TvInputManager) mContext.getSystemService(Context.TV_INPUT_SERVICE);
        List<TvInputInfo> input_list = tvinputManager.getTvInputList();
        int device_id = -1;
        for (TvInputInfo info : input_list) {
            String[] temp = info.getId().split("/");
            if (temp.length == 3) {
                /*  ignore for HDMI CEC device */
                if (temp[2].contains("HDMI"))
                    break;
                device_id = Integer.parseInt(temp[2].substring(2));
                if (device_id >= 0)//hardware device
                    break;
            }
        }
        return device_id;
    }

    public Map<String, Object> getList(int keyvalue) {
        List<Channel> getlist = mChannelTuner.getBrowsableChannelList2();
        int channelnumber = mChannelTuner.getBrowsableChannelCount();
        Channel nowchannel = mChannelTuner.getCurrentChannel();
        long nowchannelid = mChannelTuner.getCurrentChannelId();
        String inputid = nowchannel.getInputId();
        TvInputInfo input = mTvInputManagerHelper.getTvInputInfo(inputid);
        int sourcetype = DroidLogicTvUtils.getSourceType(getDeviceId());

        ArrayList<ChannelInfo> videochannels = null;
        ArrayList<ChannelInfo> radiochannels = null;
        ChannelInfo currentchannelinfo = null;
        TvDataBaseManager xTvDataBaseManager = new TvDataBaseManager(mContext);
        if (input.isPassthroughInput()) {
            currentchannelinfo = ChannelInfo.createPassthroughChannel(inputid);
            if (videochannels == null)
                videochannels = new ArrayList<ChannelInfo>();
            videochannels.add(currentchannelinfo);
        } else {
            videochannels = xTvDataBaseManager.getChannelList(inputid, Channels.SERVICE_TYPE_AUDIO_VIDEO, true);
            radiochannels = xTvDataBaseManager.getChannelList(inputid, Channels.SERVICE_TYPE_AUDIO, true);
        }

        Map<String, Object> tempmap = new HashMap<String, Object>();
        Map<Long, Channel> channelmap = mChannelTuner.getChannelMap();
        tempmap.put("index", nowchannelid);
        tempmap.put("video", videochannels);
        tempmap.put("radio", radiochannels);
        tempmap.put("channel", getlist);
        tempmap.put("channelmap", channelmap);
        return tempmap;
    }

    public void tuneToRecentChannel() {
        long recentChannelIndex = Settings.System.getLong(mContext.getContentResolver(), "recentchannel", -1);
        long currentchannelindex = mChannelTuner.getCurrentChannel().getId();
        if (recentChannelIndex != currentchannelindex) {
            Settings.System.putLong(mContext.getContentResolver(), "recentchannel", currentchannelindex);
            if (recentChannelIndex >= 0) {
                mActivity.tuneToChannel(mChannelTuner.getChannelById(recentChannelIndex));
            }
        }
    }
}