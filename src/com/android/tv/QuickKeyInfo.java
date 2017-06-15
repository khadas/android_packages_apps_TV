package com.android.tv;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.provider.Settings;
import android.app.Activity;
import android.util.Log;

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
import com.droidlogic.app.tv.TvControlManager;

public class QuickKeyInfo  {
    private final static String TAG = "QuickKeyInfo";
    private TvInputManagerHelper mTvInputManagerHelper;
    private Context mContext;
    private ChannelTuner mChannelTuner;
    private MainActivity mActivity;
    private final static boolean mDebug = false;

    public static final int KEYCODE_TV_SHORTCUTKEY_VIEWMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VIEWMODE;
    public static final int KEYCODE_TV_SHORTCUTKEY_VOICEMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_VOICEMODE;
    public static final int KEYCODE_TV_SHORTCUTKEY_DISPAYMODE = DroidLogicKeyEvent.KEYCODE_TV_SHORTCUTKEY_DISPAYMODE;
    public static final int KEYCODE_TV_SLEEP = DroidLogicKeyEvent.KEYCODE_TV_SLEEP;
    public static final int KEYCODE_FAV = DroidLogicKeyEvent.KEYCODE_FAV;
    public static final int KEYCODE_LIST = DroidLogicKeyEvent.KEYCODE_LIST;
    public static final int KEYCODE_LAST_CHANNEL = DroidLogicKeyEvent.KEYCODE_LAST_CHANNEL;

    private TvControlManager mTvControlManager = TvControlManager.getInstance();

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

    public String getAtvAudioOutmodestring(int mode){
        switch (mode) {
            case TvControlManager.AUDIO_OUTMODE_MONO:
                return mActivity.getResources().getString(R.string.audio_outmode_mono);
            case TvControlManager.AUDIO_OUTMODE_STEREO:
                return mActivity.getResources().getString(R.string.audio_outmode_stereo);
            case TvControlManager.AUDIO_OUTMODE_SAP:
                return mActivity.getResources().getString(R.string.audio_outmode_sap);
            default:
                return mActivity.getResources().getString(R.string.audio_outmode_stereo);
        }
    }

    public int getAtvAudioOutmodeint(){
        int mode = mTvControlManager.GetAudioOutmode();
        if (mDebug) {
            Log.d(TAG, "getAtvAudioOutmode"+" mode = " + mode);
        }
        return mode;
    }

    public void setAtvAudioOutmode(int mode) {
        if (mDebug) {
            Log.d(TAG, "setAtvAudioOutmode"+" mode = " + mode);
        }
        mTvControlManager.SetAudioOutmode(mode);
    }

    public Boolean isAtvSign() {
        if (mChannelTuner != null) {
            if (mDebug) {
                Log.d(TAG, "getCurrentChannel Type: " + mChannelTuner.getCurrentChannel().getType());
            }
            return isAnalogChannnel(mChannelTuner.getCurrentChannel().getType());
        }
        return false;
    }

    private boolean isAnalogChannnel(String type) {
        return (type.equals(TvContract.Channels.TYPE_PAL)
            || type.equals(TvContract.Channels.TYPE_NTSC)
            || type.equals(TvContract.Channels.TYPE_SECAM));
    }

    private boolean isAtscChannnel(String type) {
        return (type.equals(TvContract.Channels.TYPE_ATSC_C)
            || type.equals(TvContract.Channels.TYPE_ATSC_T)
            || type.equals(TvContract.Channels.TYPE_ATSC_M_H));
    }
}