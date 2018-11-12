package com.android.tv.droidlogic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.InputType;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tv.R;
import com.android.tv.TvSingletons;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.EventParams;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvDataBaseManager;
import com.droidlogic.app.tv.TvScanManager;
import com.droidlogic.tvinput.services.TvMessage;
import com.droidlogic.app.tv.TvControlDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSearchActivity extends Activity implements OnClickListener, TvScanManager.ScannerMessageListener {
    public static final String TAG = "ChannelSearchActivity";

    private PowerManager mPowerManager;

    private TvScanManager mTvScanManager;
    private TvControlManager mTvControlManager;
    private TvControlManager.SourceInput_Type mTvSource;
    private TvControlManager.SourceInput_Type mVirtualTvSource;
    private TvControlManager.SourceInput mTvSourceInput;
    private TvDataBaseManager mTvDataBaseManager;
    private AudioManager mAudioManager;
    private boolean isFinished = false;

    private ProgressBar mProgressBar;
    private TextView mScanningMessage;
    private View mChannelHolder;
    private SimpleAdapter mAdapter;
    private ListView mChannelList;
    private volatile boolean mChannelListVisible;
    private Button mScanButton;
    private Spinner mCountrySetting;
    private Spinner mSearchModeSetting;
    private Spinner mSearchTypeSetting;
    private Spinner mOrderSetting;
    private Spinner mAtvColorSystem;
    private Spinner mAtvSoundSystem;
    private Spinner mDvbcQamModeSetting;
    private EditText mInputChannelFrom;
    private EditText mInputChannelTo;

    private TextView mSearchOptionText;
    private ViewGroup mAllInputLayout;
    private ViewGroup mFromInputLayout;
    private ViewGroup mToInputLayout;
    private TextView mAtvSearchOrderText;
    private TextView mAtvColorSystemText;
    private TextView mAtvSoundSystemText;
    private TextView mDvbcQamModeText;

    private String mInputId;
    private Intent in;
    private boolean isConnected = false;

    public static final int MANUAL_START = 0;
    public static final int AUTO_START = 1;
    public static final int PROCCESS = 2;
    public static final int CHANNEL = 3;
    public static final int STATUS = 4;
    public static final int NUMBER_SEARCH_START = 5;
    public static final int FREQUENCY = 6;
    public static final int CHANNELNUMBER = 7;
	public static final int EXIT_SCAN = 8;
    public static final int ATSCC_OPTION_DEFAULT = 0;

    public static final int SET_DTMB = 0;
    public static final int SET_DVB_C = 1;
    public static final int SET_DVB_T = 2;
    public static final int SET_DVB_T2 = 3;
    public static final int SET_ATSC_T = 4;
    public static final int SET_ATSC_C= 5;
    public static final int SET_ISDB_T = 6;

    public static final String STRING_NAME = "name";
    public static final String STRING_STATUS = "status";

    public static final int START_TIMER= 0;
    public static final int START_INIT = 1;

    //number search  key "numbersearch" true or false, "number" 1~135 or 2~69
    public static final String NUMBERSEARCH = DroidLogicTvUtils.TV_NUMBER_SEARCH_MODE;
    public static final String NUMBER = DroidLogicTvUtils.TV_NUMBER_SEARCH_NUMBER;
    public static final String ATSC_TV_SEARCH_SYS = "atsc_tv_search_sys";
    public static final String ATSC_TV_SEARCH_SOUND_SYS = "atsc_tv_search_sound_sys";
    public static final boolean NUMBERSEARCHDEFAULT = false;
    public static final int NUMBERDEFAULT = -1;
    private ProgressDialog mProDia;
    private boolean isNumberSearchMode = false;
    private String mNumber = null;
    private boolean hasFoundChannel = false;
    private boolean mNumberSearchDtv = true;
    private boolean  mNumberSearchAtv = true;
    private int hasFoundChannelNumber = 0;
    private Map hasFoundInfo =  new HashMap<String, Integer>();

    public static final String hint_channel_number = "number";
    public static final String hint_channel_frequency = "freq MHz";

    /* config in file 'tvconfig.cfg' [atv.auto.scan.mode] */
    /* 0: freq table list sacn mode */
    /* 1: all band sacn mode */
    private int mATvAutoScanMode = 0;
    private int channelCounts = 0;
    private TvControlDataManager mTvControlDataManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mTvDataBaseManager = new TvDataBaseManager(this);
        mTvControlManager = TvControlManager.getInstance();
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mTvControlDataManager = TvSingletons.getSingletons(this).getTvControlDataManager();

        in = getIntent();
        if (in != null) {
            mTvScanManager = new TvScanManager(this, in);
            mTvScanManager.setMessageListener(this);

            if (in.getBooleanExtra(NUMBERSEARCH, NUMBERSEARCHDEFAULT)) {
                mNumber = in.getStringExtra(NUMBER);
                if (null == mNumber) {
                    finish();
                    return;
                }
                //number search channel define
                //atv: 3-0\4-0  dtv: 3-\4- atv&dtv: 3\4
                isNumberSearchMode = true;
                String[] numberarray = mNumber.split("-");
                if (numberarray.length == 1) {
                    mNumberSearchDtv = true;
                    mNumberSearchAtv = true;
                } else if (numberarray.length == 2) {
                    if (Integer.parseInt(numberarray[1]) == 0) {
                        mNumberSearchDtv = false;
                        mNumberSearchAtv = true;
                    } else {
                        mNumberSearchDtv = true;
                        mNumberSearchAtv = false;
                    }
                }
                setContentView(R.layout.tv_number_channel_scan);
            } else {
                setContentView(R.layout.tv_channel_scan);
            }

            mInputId = in.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            DroidLogicTvUtils.saveInputId(this, mInputId);
            int deviceId = in.getIntExtra(DroidLogicTvUtils.EXTRA_CHANNEL_DEVICE_ID, -1);
            if (deviceId == -1)//for TIF compatible
                deviceId = DroidLogicTvUtils.getHardwareDeviceId(mInputId);
            mTvSource = DroidLogicTvUtils.parseTvSourceTypeFromDeviceId(deviceId);
            mTvSourceInput = DroidLogicTvUtils.parseTvSourceInputFromDeviceId(deviceId);
            mVirtualTvSource = mTvSource;
            if (mTvSource == TvControlManager.SourceInput_Type.SOURCE_TYPE_ADTV) {
                long channelId = in.getLongExtra(DroidLogicTvUtils.EXTRA_CHANNEL_NUMBER, -1);
                ChannelInfo currentChannel = mTvDataBaseManager.getChannelInfo(TvContract.buildChannelUri(channelId));
                if (currentChannel != null) {
                    mTvSource = DroidLogicTvUtils.parseTvSourceTypeFromSigType(DroidLogicTvUtils.getSigType(currentChannel));
                    mTvSourceInput = DroidLogicTvUtils.parseTvSourceInputFromSigType(DroidLogicTvUtils.getSigType(currentChannel));
                }
                if (mVirtualTvSource == mTvSource) {//no channels in adtv input, DTV for default.
                    mTvSource = TvControlManager.SourceInput_Type.SOURCE_TYPE_DTV;
                    mTvSourceInput = TvControlManager.SourceInput.DTV;
                }
            }
        }
        isFinished = false;
        mATvAutoScanMode = mTvControlManager.GetAtvAutoScanMode();
        handler.sendEmptyMessage(START_INIT);

        //start number search when service connected
        if (isNumberSearchMode) {
            startShowActivityTimer();
            return;
        }

        mProgressBar = (ProgressBar) findViewById(R.id.tune_progress);
        mScanningMessage = (TextView) findViewById(R.id.tune_description);
        mChannelList = (ListView) findViewById(R.id.channel_list);
        mChannelList.setAdapter(mAdapter);
        mChannelList.setOnItemClickListener(null);
        ViewGroup progressHolder = (ViewGroup) findViewById(R.id.progress_holder);
        mChannelHolder = findViewById(R.id.channel_holder);
        mSearchOptionText = findViewById(R.id.search_option);
        mAllInputLayout = findViewById(R.id.channel_input_type_container);
        mFromInputLayout = findViewById(R.id.channel_input_from_container);
        mToInputLayout = findViewById(R.id.channel_input_to_container);
        mAtvSearchOrderText = findViewById(R.id.order);
        mAtvColorSystemText = findViewById(R.id.atv_color);
        mAtvSoundSystemText = findViewById(R.id.atv_sound);
        mDvbcQamModeText = findViewById(R.id.dvbc_qam_mode);

        mScanButton = (Button) findViewById(R.id.search_channel);
        mScanButton.setOnClickListener(this);
        mScanButton.requestFocus();
        mInputChannelFrom = (EditText) findViewById(R.id.input_channel_from);
        mInputChannelTo= (EditText) findViewById(R.id.input_channel_to);
        initSpinner();
        startShowActivityTimer();
    }

    @Override
    public void onMessage(TvMessage msg) {
        Log.d(TAG, "=====receive message from MessageListener");
        int arg1 = msg.getType();
        int what = msg.getMessage();
        String information = msg.getInformation();
        Message message = new Message();
        message.arg1 = arg1;
        message.what = what;
        if (arg1 == CHANNEL && what == 0) {
            message.obj = getAdapter(getEventParams(information));
        } else {
            message.obj = information;
        }
        mHandler.sendMessage(message);
    }

    private TvControlManager.SourceInput_Type getCurentTvSource () {
        return mTvSource;
    }

    private void setDtvType(String type) {
        if (type != null) {
            mTvControlDataManager.putString(this.getContentResolver(), DroidLogicTvUtils.TV_KEY_DTV_TYPE, type);
        }
    }

    private String getDtvType() {
        return mTvControlDataManager.getString(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_KEY_DTV_TYPE);
    }

    private SimpleAdapter getAdapter (EventParams event) {
        ArrayList<HashMap<String, Object>> dataList = getSearchedInfo(event);
        SimpleAdapter adapter = new SimpleAdapter(this, dataList,
                R.layout.tv_channel_list,
                new String[] {STRING_NAME, STRING_STATUS},
                new int[] {R.id.text_name, R.id.text_status});
        return adapter;
    }

    private ArrayList<HashMap<String, Object>> getSearchedInfo (EventParams event) {
        Log.d(TAG, "===== getSearchedInfo");
        ArrayList<HashMap<String, Object>> list =  new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.frequency_l) + ":");
        item.put(STRING_STATUS, Double.toString(event.getFrequency() / (1000 * 1000)) +
                getResources().getString(R.string.mhz));
        Log.d(TAG, "***** frequency : " + Double.toString(event.getFrequency() / (1000 * 1000)) +
                getResources().getString(R.string.mhz));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.quality) + ":");
        item.put(STRING_STATUS, event.getQuality() + getResources().getString(R.string.db));
        Log.d(TAG, "***** quality : " + event.getQuality() + getResources().getString(R.string.db));
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.strength) + ":");
        item.put(STRING_STATUS, event.getStrength() + "%");
        Log.d(TAG, "***** strength : " + event.getStrength() + "%");
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.tv_channel) + ":");
        item.put(STRING_STATUS, event.getChannelNumber());
        Log.d(TAG, "***** tv channel : " + event.getChannelNumber());
        list.add(item);

        item = new HashMap<String, Object>();
        item.put(STRING_NAME, getResources().getString(R.string.radio_channel) + ":");
        item.put(STRING_STATUS, event.getRadioNumber());
        Log.d(TAG, "***** radio channel : " + event.getRadioNumber());
        list.add(item);

        return list;
    }

    public boolean isSearching() {
        Log.d(TAG, "===== Search Status :" + getSearchStatus());
        return (getSearchStatus() != 0);
    }

    public int getSearchStatus () {
        return mTvControlDataManager.getInt(this.getContentResolver(), DroidLogicTvUtils.TV_SEARCHING_STATE, 0);
    }

    public void resetSearchStatus () {
        mTvControlDataManager.putInt(this.getContentResolver(), DroidLogicTvUtils.TV_SEARCHING_STATE, 0);
    }

    public EventParams getEventParams (String params) {
        EventParams eventParams;
        if (params != null) {
            String[] channelParams = params.split("-");
            channelCounts = Integer.valueOf(channelParams[3]).intValue() + Integer.valueOf(channelParams[4]).intValue();
            eventParams = new EventParams(
                Integer.valueOf(channelParams[0]).intValue(),
                Integer.valueOf(channelParams[1]).intValue(),
                Integer.valueOf(channelParams[2]).intValue(),
                Integer.valueOf(channelParams[3]).intValue(),
                Integer.valueOf(channelParams[4]).intValue());
        } else {
            int firstFreq = 0;
            String country = getCountry();
            switch (country) {
                case "CN":
                    if (DroidLogicTvUtils.isATV(this)) {
                        firstFreq = 49;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 52;
                    }
                    break;
                case "IN":
                    firstFreq = 48;
                    break;
                case "US":
                case "MX":
                    if (TextUtils.equals(getDtvType(), TvContract.Channels.TYPE_ATSC_T)) {
                        firstFreq = 55;
                    } else if (TextUtils.equals(getDtvType(), TvContract.Channels.TYPE_ATSC_C)) {
                        firstFreq = 73;
                    }
                    break;
                case "ID":
                    if (DroidLogicTvUtils.isATV(this)) {
                        firstFreq = 48;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 47;
                    }
                    break;
                case "DE":
                    if (DroidLogicTvUtils.isATV(this)) {
                            firstFreq = 48;
                    } else if (DroidLogicTvUtils.isDTV(this)) {
                        firstFreq = 47;
                    }
                    break;
            }
            eventParams = new EventParams(firstFreq, 0, 0, 0, 0);
        }
        return eventParams;
    }

    public void dtvStopScan() {
        mTvControlManager.DtvStopScan();
    }

    public void stopTv() {
        mTvControlManager.StopTv();
    }

    private void initNumberSearch(Context context) {
        mProDia= new ProgressDialog(this);
        mProDia.setTitle(R.string.number_search_title);
        mProDia.setMessage(getResources().getString(R.string.number_search_dtv));
        mProDia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProDia.show();
        sendMessage(NUMBER_SEARCH_START, 0, null);
    }

    private void exitNumberSearch() {
        if (mProDia != null && mProDia.isShowing()) {
            mProDia.dismiss();
        }
    }

    //definition about country
    public final int COUNTRY_AMERICA = 0;
    public final int COUNTRY_INDIA = 1;
    public final int COUNTRY_INDONESIA = 2;//Indonesia
    public final int COUNTRY_MEXICO = 3;//Mexico
    public final int COUNTRY_GERMANY = 4;
    public final int COUNTRY_CHINA = 5;
    public final int COUNTRY_BRAZIL = 6;

    public final ArrayList<String> COUNTRY_LIST = new ArrayList<String>(){{add("US"); add("IN"); add("ID"); add("MX"); add("DE"); add("CN"); add("BR");}};

    final private int[] COUNTRY = {R.string.tv_america, R.string.tv_india, R.string.tv_indonesia, R.string.tv_mexico, R.string.tv_germany, R.string.tv_china, R.string.tv_brazil};

    final private int[] SEARCH_MODE = {R.string.tv_search_mode_manual, R.string.tv_search_mode_auto};

    final private int[] INDIA_TV_TYPE = {R.string.tv_search_type_atv};
    final private int[] INDONESIA_TV_TYPE = {R.string.tv_search_type_atv, R.string.tv_search_type_dvb_t};
    final private int[] AMERICA_TV_TYPE = {R.string.tv_search_type_atsc_t, R.string.tv_search_type_atsc_c_standard, R.string.tv_search_type_atsc_c_lrc, R.string.tv_search_type_atsc_c_hrc, R.string.tv_search_type_atsc_c_auto};
    final private int[] MEXICO_TV_TYPE = {R.string.tv_search_type_atsc_t, R.string.tv_search_type_atsc_c_standard, R.string.tv_search_type_atsc_c_lrc, R.string.tv_search_type_atsc_c_hrc, R.string.tv_search_type_atsc_c_auto};
    final private int[] GERMANY_TV_TYPE = {R.string.atv, R.string.tv_search_type_dvb_t, R.string.tv_search_type_dvb_c, R.string.tv_search_type_dvb_s};
    final private int[] CHINA_TV_TYPE = {R.string.tv_search_type_atv, R.string.tv_search_type_dtmb};
    final private int[] BRAZIL_TV_TYPE = {R.string.tv_search_type_atv, R.string.tv_search_type_isdb_t};

    final private int[] SEARCH_ORDER = {R.string.tv_search_order_low, R.string.tv_search_order_high};
    final private int[] ATV_COLOR_SYSTEM = {R.string.tv_search_atv_clolor_auto, R.string.tv_search_atv_clolor_pal, R.string.tv_search_atv_clolor_ntsc, R.string.tv_search_atv_clolor_secam};
    final private int[] ATV_SOUND_SYSTEM = {R.string.tv_search_atv_sound_auto, R.string.tv_search_atv_sound_dk, R.string.tv_search_atv_sound_i, R.string.tv_search_atv_sound_bg, R.string.tv_search_atv_sound_m, R.string.tv_search_atv_sound_l};
    final private int[] QAM_FOR_DVB_C = {R.string.ut_tune_dvb_c_qam16, R.string.ut_tune_dvb_c_qam32, R.string.ut_tune_dvb_c_qam64, R.string.ut_tune_dvb_c_qam128, R.string.ut_tune_dvb_c_qam256};

    private void initSpinner() {
        ArrayAdapter<String> country_arr_adapter;
        List<String> country_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_mode_arr_adapter;;
        List<String> search_mode_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_type_arr_adapter;
        List<String> search_type_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_order_arr_adapter;
        List<String> search_order_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_atv_color_arr_adapter;
        List<String> search_atv_color_data_list = new ArrayList<String>();
        ArrayAdapter<String> search_atv_sound_arr_adapter;
        List<String> search_atv_sound_data_list = new ArrayList<String>();
        ArrayAdapter<String> dvbc_qam_mode_arr_adapter;
        List<String> dvbc_qam_mode_data_list = new ArrayList<String>();

        ArrayList<String> countrylist = getSupportCountry();
        for (int i = 0; i < countrylist.size(); i++) {
            country_data_list.add(getString(COUNTRY[COUNTRY_LIST.indexOf(countrylist.get(i))]));
        }
        for (int i = 0; i < SEARCH_MODE.length; i++) {
            if (i == TV_SEARCH_MANUAL &&  (COUNTRY_LIST.get(COUNTRY_AMERICA).equals(getCountry()) || COUNTRY_LIST.get(COUNTRY_MEXICO).equals(getCountry()))) {
                continue;//only auto search mode
            }
            search_mode_data_list.add(getString(SEARCH_MODE[i]));
        }
        for (int i = 0; i < SEARCH_ORDER.length; i++) {
            search_order_data_list.add(getString(SEARCH_ORDER[i]));
        }
        //default india
        String country = getCountry();
        String dtvtype = null;
        int[] list = null;
        switch (country) {
            case "IN"://COUNTRY_LIST.get(COUNTRY_INDIA):
                list = INDIA_TV_TYPE;
                break;
            case "ID"://COUNTRY_LIST.get(COUNTRY_INDONESIA):
                list = INDONESIA_TV_TYPE;
                break;
            case "US"://COUNTRY_LIST.get(COUNTRY_AMERICA):
            case "MX"://COUNTRY_LIST.get(COUNTRY_MEXICO):
                list = AMERICA_TV_TYPE;
                break;
            case "DE"://COUNTRY_LIST.get(COUNTRY_GERMANY):
                list = GERMANY_TV_TYPE;
                break;
            case "CN"://COUNTRY_LIST.get(COUNTRY_CHINA):
                list = CHINA_TV_TYPE;
                break;
            case "BR"://COUNTRY_LIST.get(COUNTRY_BRAZIL):
                list = BRAZIL_TV_TYPE;
                break;
            default:
                list = INDIA_TV_TYPE;
                break;
        }
        for (int i = 0; i < list.length; i++) {
            search_type_data_list.add(getString(list[i]));
        }
        for (int i = 0; i < ATV_COLOR_SYSTEM.length; i++) {
            search_atv_color_data_list.add(getString(ATV_COLOR_SYSTEM[i]));
        }
        for (int i = 0; i < ATV_SOUND_SYSTEM.length; i++) {
            search_atv_sound_data_list.add(getString(ATV_SOUND_SYSTEM[i]));
        }
        for (int i = 0; i < QAM_FOR_DVB_C.length; i++) {
            dvbc_qam_mode_data_list.add(getString(QAM_FOR_DVB_C[i]));
        }

        mCountrySetting = (Spinner) findViewById(R.id.country_spinner);
        mSearchModeSetting = (Spinner) findViewById(R.id.search_mode_spinner);
        mSearchTypeSetting = (Spinner) findViewById(R.id.search_type_spinner);
        mOrderSetting = (Spinner) findViewById(R.id.order_spinner);
        mAtvColorSystem = (Spinner) findViewById(R.id.atv_color_spinner);
        mAtvSoundSystem = (Spinner) findViewById(R.id.atv_sound_spinner);
        mDvbcQamModeSetting = (Spinner) findViewById(R.id.dvbc_qam_mode_spinner);
        country_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, country_data_list);
        country_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mCountrySetting.setAdapter(country_arr_adapter);
        mCountrySetting.setSelection(getSupportCountry().indexOf(getCountry()));
        search_mode_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_mode_data_list);
        search_mode_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mSearchModeSetting.setAdapter(search_mode_arr_adapter);
        mSearchModeSetting.setSelection((COUNTRY_LIST.get(COUNTRY_AMERICA).equals(getCountry()) || COUNTRY_LIST.get(COUNTRY_MEXICO).equals(getCountry())) ? TV_SEARCH_MANUAL : (getSearchMode().equals(SEARCH_MODE_LIST[TV_SEARCH_AUTO]) ? TV_SEARCH_AUTO : TV_SEARCH_MANUAL));
        search_type_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_type_data_list);
        search_type_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mSearchTypeSetting.setAdapter(search_type_arr_adapter);
        mSearchTypeSetting.setSelection(getSearchType());
        search_order_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_order_data_list);
        search_order_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mOrderSetting.setAdapter(search_order_arr_adapter);
        mOrderSetting.setSelection(getSearchOrder());
        search_atv_color_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_atv_color_data_list);
        search_atv_color_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mAtvColorSystem.setAdapter(search_atv_color_arr_adapter);
        mAtvColorSystem.setSelection(getTvSearchTypeSys());
        search_atv_sound_arr_adapter= new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, search_atv_sound_data_list);
        search_atv_sound_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mAtvSoundSystem.setAdapter(search_atv_sound_arr_adapter);
        mAtvSoundSystem.setSelection((getTvSearchSoundSys() + 1) % ATV_SEARCH_SOUND_MAX);
        dvbc_qam_mode_arr_adapter = new ArrayAdapter<String>(ChannelSearchActivity.this, android.R.layout.simple_spinner_item, dvbc_qam_mode_data_list);
        dvbc_qam_mode_arr_adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        mDvbcQamModeSetting.setAdapter(dvbc_qam_mode_arr_adapter);
        mDvbcQamModeSetting.setSelection(getDvbcQamMode() - 1);//mode change from 1~5
        if (!(getAtvDtvModeFlag() == SEARCH_DTV)) {//atv type
            mAtvColorSystem.setEnabled(true);
            mAtvSoundSystem.setEnabled(true);
            hideAtvRelatedOption(false);
        } else {
            mAtvColorSystem.setEnabled(false);
            mAtvSoundSystem.setEnabled(false);
            hideAtvRelatedOption(true);
        }
        if (SEARCH_MODE_LIST[TV_SEARCH_AUTO].equals(getSearchMode())) {
            hideInputChannel(true);
            mScanButton.setText(R.string.ut_auto_scan);
        } else {
            hideInputChannel(false);
            mScanButton.setText(R.string.ut_manual_scan);
        }
        if (!TvContract.Channels.TYPE_DVB_C.equals(getDtvType())) {
            mDvbcQamModeSetting.setVisibility(View.GONE);
            mDvbcQamModeText.setVisibility(View.GONE);
        } else {
            mDvbcQamModeSetting.setVisibility(View.VISIBLE);
            mDvbcQamModeText.setVisibility(View.VISIBLE);
        }
        //no effect at moment, and hide it
        mOrderSetting.setVisibility(View.GONE);
        mAtvSearchOrderText.setVisibility(View.GONE);
        mCountrySetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != getSupportCountry().indexOf((getCountry()))) {
                    String country = getSupportCountry().get(position);
                    setCountry(country);
                    setSearchMode(SEARCH_MODE_LIST[TV_SEARCH_AUTO]);
                    int countryindex = COUNTRY_LIST.indexOf(country);
                    if (countryindex == COUNTRY_AMERICA || countryindex == COUNTRY_MEXICO) {
                        setAtvDtvModeFlag(SEARCH_ATV_DTV);
                    } else {
                        setAtvDtvModeFlag(SEARCH_ATV);
                    }
                    setSearchType(TV_SEARCH_ATV);
                    setDtvType(getTvTypebyCountry(COUNTRY_LIST.indexOf(getSupportCountry().get(position)))[0]);
                    setAtsccListMode(STANDARD);
                    setSearchOrder(TV_SEARCH_ORDER_LOW);
                    setTvSearchTypeSys(TV_SEARCH_SYS_AUTO);
                    setTvSearchSoundSys(ATV_SEARCH_SOUND_AUTO);
                    setDvbcQamMode(DroidLogicTvUtils.TV_SEARCH_DVBC_QAM16);
                    initSpinner();//init again
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mSearchModeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (COUNTRY_LIST.get(COUNTRY_AMERICA).equals(getCountry()) || COUNTRY_LIST.get(COUNTRY_MEXICO).equals(getCountry())) {
                    setSearchMode(SEARCH_MODE_LIST[TV_SEARCH_AUTO]);//auto only
                } else {//both manaul and auto
                    setSearchMode(SEARCH_MODE_LIST[position]);
                }
                if (SEARCH_MODE_LIST[TV_SEARCH_AUTO].equals(getSearchMode())) {
                    hideInputChannel(true);
                    mScanButton.setText(R.string.ut_auto_scan);
                } else {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_manual_scan);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mSearchTypeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSearchType(position);
                if (COUNTRY_LIST.get(COUNTRY_AMERICA).equals(getCountry()) || COUNTRY_LIST.get(COUNTRY_MEXICO).equals(getCountry())) {
                    if (position > 0) {//atsc-c need set value
                        setAtsccListMode(position - 1);
                    }
                     setAtvDtvModeFlag(SEARCH_ATV_DTV);
                } else {
                    if (position == TV_SEARCH_ATV) {
                        setAtvDtvModeFlag(SEARCH_ATV);
                    } else {
                        setAtvDtvModeFlag(SEARCH_DTV);
                    }
                }
                String[] typelist = getTvTypebyCountry(COUNTRY_LIST.indexOf(getCountry()));
                if (getSearchType() < typelist.length) {
                    setDtvType(typelist[getSearchType()]);
                    if (!TvContract.Channels.TYPE_DVB_C.equals(getDtvType())) {
                        mDvbcQamModeSetting.setVisibility(View.GONE);
                        mDvbcQamModeText.setVisibility(View.GONE);
                    } else {
                        mDvbcQamModeSetting.setVisibility(View.VISIBLE);
                        mDvbcQamModeText.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "set search type error position = " + position + " >= " + typelist.length);
                }
                if (!(getAtvDtvModeFlag() == SEARCH_DTV)) {//atv type
                    mAtvColorSystem.setEnabled(true);
                    mAtvSoundSystem.setEnabled(true);
                    hideAtvRelatedOption(false);
                } else {
                    mAtvColorSystem.setEnabled(false);
                    mAtvSoundSystem.setEnabled(false);
                    hideAtvRelatedOption(true);
                }

                if (SEARCH_MODE_LIST[TV_SEARCH_AUTO].equals(getSearchMode())) {
                    hideInputChannel(true);
                    mScanButton.setText(R.string.ut_auto_scan);
                } else {
                    hideInputChannel(false);
                    mScanButton.setText(R.string.ut_manual_scan);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mOrderSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSearchOrder(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtvColorSystem.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= TV_SEARCH_SYS_AUTO && position <= TV_SEARCH_SYS_SECAM) {
                    setTvSearchTypeSys(position);
                } else {
                    setTvSearchTypeSys(TV_SEARCH_SYS_AUTO);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mAtvSoundSystem.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= ATV_SEARCH_SOUND_MIN && position < ATV_SEARCH_SOUND_MAX) {
                    int value = ATV_SEARCH_SOUND_AUTO;
                    if (position == 0) {
                        value = ATV_SEARCH_SOUND_AUTO;
                    } else if (position == 1) {
                        value = ATV_SEARCH_SOUND_DK;
                    } else if (position == 2) {
                        value = ATV_SEARCH_SOUND_I;
                    } else if (position == 3) {
                        value = ATV_SEARCH_SOUND_BG;
                    } else if (position == 4) {
                        value = ATV_SEARCH_SOUND_M;
                    } else if (position == 5) {
                        value = ATV_SEARCH_SOUND_L;
                    } else {
                        value = ATV_SEARCH_SOUND_AUTO;
                    }
                    setTvSearchSoundSys(value);
                } else {
                    setTvSearchSoundSys(ATV_SEARCH_SOUND_AUTO);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        mDvbcQamModeSetting.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= (DroidLogicTvUtils.TV_SEARCH_DVBC_QAM16 - 1) && position < DroidLogicTvUtils.TV_SEARCH_DVBC_QAM256) {
                    setDvbcQamMode(position + 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
    }

    private void hideInputChannel(boolean value) {
        if (value) {
            mAllInputLayout.setVisibility(View.GONE);
        } else {
            mAllInputLayout.setVisibility(View.VISIBLE);
            //show start frequency and end frequency as need
            if (mATvAutoScanMode == TvControlManager.ScanType.SCAN_ATV_AUTO_ALL_BAND && getAtvDtvModeFlag() == SEARCH_ATV) {
                //SpannableString hint = new SpannableString("");
                mInputChannelFrom.setHint(hint_channel_frequency);
                mInputChannelTo.setHint(hint_channel_frequency);
                mInputChannelFrom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            } else {
                mInputChannelFrom.setHint(hint_channel_number);
                mInputChannelTo.setHint(hint_channel_number);
                mInputChannelFrom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                mInputChannelTo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
            }

            mInputChannelFrom.setInputType(InputType.TYPE_CLASS_NUMBER);
            mInputChannelTo.setInputType(InputType.TYPE_CLASS_NUMBER);

            if (false && COUNTRY_LIST.get(COUNTRY_CHINA).equals(getCountry())) {
                mToInputLayout.setVisibility(View.VISIBLE);
            } else {
                mToInputLayout.setVisibility(View.GONE);
            }

        }
        mInputChannelFrom.setText("");
        mInputChannelTo.setText("");
    }

    private void hideSearchOption(boolean value) {
        if (value) {
            mSearchOptionText.setVisibility(View.GONE);
        } else {
            mSearchOptionText.setVisibility(View.VISIBLE);
        }
    }

    private void hideAtvRelatedOption(boolean value) {
        if (value) {
            Log.d(TAG, "hideAtvRelatedOption = " + value);
            mAtvSearchOrderText.setVisibility(View.GONE);
            mOrderSetting.setVisibility(View.GONE);
            mAtvColorSystemText.setVisibility(View.GONE);
            mAtvColorSystem.setVisibility(View.GONE);
            mAtvSoundSystemText.setVisibility(View.GONE);
            mAtvSoundSystem.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "hideAtvRelatedOption = " + value);
            //no effect at monent, and hide it
            //mAtvSearchOrderText.setVisibility(View.VISIBLE);
            //mOrderSetting.setVisibility(View.VISIBLE);
            mAtvColorSystemText.setVisibility(View.VISIBLE);
            mAtvColorSystem.setVisibility(View.VISIBLE);
            mAtvSoundSystemText.setVisibility(View.VISIBLE);
            mAtvSoundSystem.setVisibility(View.VISIBLE);
        }
    }

    private void showStopScanDialog() {
        Log.d(TAG, "showStopScanDialog");
        AlertDialog.Builder stopScanDialog = new AlertDialog.Builder(this);
        String allBandScanMessage;
        if (channelCounts == 1) {
            allBandScanMessage = getString(R.string.tv_all_band_search_dialog_message1)
                + " " + channelCounts + " "
                + getString(R.string.tv_all_band_search_dialog_messages2)
                + " " + getString(R.string.tv_all_band_search_dialog_message2);
        } else {
            allBandScanMessage = getString(R.string.tv_all_band_search_dialog_message1)
                + " " + channelCounts + " "
                + getString(R.string.tv_all_band_search_dialog_messages1)
                + " " + getString(R.string.tv_all_band_search_dialog_message2);
        }
        stopScanDialog.setMessage(allBandScanMessage);
        stopScanDialog.setPositiveButton(getString(R.string.dialog_btn_confirm_text)
            , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (SEARCH_MODE_LIST[TV_SEARCH_MANUAL].equals(getSearchMode())) {
                        if (ShowNoAtvFrequencyOrChannel()) {
                            if (!isManualStarted) {
                                isManualStarted = true;
                                mScanButton.setText(R.string.ut_stop_channel_scan);
                                if (mChannelHolder.getVisibility() == View.GONE) {
                                    mChannelHolder.setVisibility(View.VISIBLE);
                                }
                            } else {
                                isManualStarted = false;
                                mScanButton.setText(R.string.ut_manual_scan);
                                if (mChannelHolder.getVisibility() == View.VISIBLE) {
                                    mChannelHolder.setVisibility(View.GONE);
                                }
                            }
                            sendMessage(MANUAL_START, 0, null);
                        }
                    }
                    dialog.dismiss();
                }
            });
        stopScanDialog.setNegativeButton(getString(R.string.dialog_btn_cancel_text)
            , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    dialog.dismiss();
                }
            });
        stopScanDialog.create().show();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case MANUAL_START:
                    Log.d(TAG, "=====MANUAL_START");
                    if (mTvScanManager.isConnected()) {
                        initparametres(MANUAL_SEARCH);
                        mTvScanManager.startManualScan();
                    } else {
                        Log.d(TAG, "Service connecting, please wait a second!");
                        sendMessageDelayeds(MANUAL_START, 0, null, 200);
                    }
                    break;
                case AUTO_START:
                    Log.d(TAG, "=====AUTO_START");
                    if (mTvScanManager.isConnected()) {
                        initparametres(AUTO_SEARCH);
                        mTvScanManager.startAutoScan();
                    } else {
                        Log.d(TAG, "Service connecting, please wait a second!");
                        sendMessageDelayeds(AUTO_START, 0, null, 200);
                    }
                    break;
                case PROCCESS:
                    Log.d(TAG, "=====PROCCESS");
                    if (!isNumberSearchMode) {
                        mProgressBar.setProgress(msg.what);
                    } else {
                        mProDia.setProgress(msg.what);
                    }
                    break;
                case CHANNEL:
                    Log.d(TAG, "=====CHANNEL");
                    if (!isNumberSearchMode && (isAutoStarted || isManualStarted)) {
                        mAdapter = (SimpleAdapter) msg.obj;
                        if (mAdapter == null) {
                            mAdapter = getAdapter(getEventParams(null));
                        }
                        mChannelList.setAdapter(mAdapter);
                        mChannelList.setOnItemClickListener(null);
                        if (mChannelHolder.getVisibility() == View.GONE) {
                            mChannelHolder.setVisibility(View.VISIBLE);
                        }
                    }
                    if (mATvAutoScanMode == TvControlManager.ScanType.SCAN_ATV_AUTO_ALL_BAND
                        && isManualStarted && isAllBandScanStopped) {
                        isManualStarted = false;
                        isAllBandScanStopped = false;
                        mScanButton.setText(R.string.ut_manual_scan);
                        showStopScanDialog();
                    }
                    break;
                case STATUS:
                    Log.d(TAG, "=====STATUS");
                    if (!isNumberSearchMode) {
                        if (mATvAutoScanMode == TvControlManager.ScanType.SCAN_ATV_AUTO_ALL_BAND) {
                            if ("pause".equals((String) msg.obj)) {//scan pause
                                Log.d(TAG, "set flag to stop scanning");
                                isAllBandScanStopped = true;
                            }
                        } else {
                            mScanningMessage.setText((String) msg.obj);
                        }
                    } else {
                        hasFoundChannelNumber = msg.what;
                        if (hasFoundChannelNumber > 0) {
                            hasFoundChannel = true;
                            Log.d(TAG, "find channel num = " + hasFoundChannelNumber);
                            if (isSearching()) {
                                dtvStopScan();
                            }
                        } else if (hasFoundChannelNumber == -1) {
                            Log.d(TAG, "=====exit");
                            exitNumberSearch();
                            finish();
                        }
                        /*if ("exit".equals((String) msg.obj)) {//scan exit
                            exitNumberSearch();
                            finish();
                        }*/
                    }
                    break;
                case NUMBER_SEARCH_START:
                    Log.d(TAG, "=====NUMBER_SEARCH_START");
                    handler.postDelayed(TimeOutStopScanRunnable, 30000);//timeout 30s
                    initparametres(NUMBER_SEARCH_START);
                    mTvScanManager.startManualScan();
                    break;
                case FREQUENCY:
                    Log.d(TAG, "=====FREQUENCY");
                    if (msg != null && !hasFoundInfo.containsKey((String) msg.obj)) {
                        hasFoundInfo.put((String) msg.obj, msg.what);
                        if (!hasFoundInfo.containsKey(DroidLogicTvUtils.FIRSTAUTOFOUNDFREQUENCY)) {
                            hasFoundInfo.put(DroidLogicTvUtils.FIRSTAUTOFOUNDFREQUENCY, msg.what);
                        }
                        if (!hasFoundInfo.containsKey(DroidLogicTvUtils.AUTO_SEARCH_MODE)) {
                            if (!isNumberSearchMode && SEARCH_MODE_LIST[TV_SEARCH_AUTO].equals(getSearchMode())) {
                                hasFoundInfo.put(DroidLogicTvUtils.AUTO_SEARCH_MODE, 1);
                            }
                        }
                    }
                    break;
                case CHANNELNUMBER:
                    Log.d(TAG, "=====CHANNELNUMBER");
                        hasFoundInfo.put((String) msg.obj, msg.what);
                    break;
                case EXIT_SCAN:
                    Log.d(TAG, "=====EXIT_SCAN");
                    finish();
                    break;
                default :
                    break;
            }
        }
    };

    private static final int MANUAL_SEARCH = 0;
    private static final int AUTO_SEARCH = 1;

    private void initparametres(int type) {
        int flag = getAtvDtvModeFlag();
        switch (flag) {
            case SEARCH_ATV:
                mTvScanManager.setSearchSys(false, true);
                break;
            case SEARCH_DTV:
                mTvScanManager.setSearchSys(true, false);
                break;
            case SEARCH_ATV_DTV:
                mTvScanManager.setSearchSys(true, true);
                break;
            default:
                break;
        }
        if (COUNTRY_LIST.get(COUNTRY_AMERICA).equals(getCountry()) || COUNTRY_LIST.get(COUNTRY_MEXICO).equals(getCountry())) {
            mTvScanManager.setAtsccSearchSys(getAtsccListMode());
            setNumberSearchNeed(0);
        } else {
            mTvScanManager.setAtsccSearchSys(STANDARD);
            setNumberSearchNeed(0);
        }
        if (NUMBER_SEARCH_START== type) {
            //mTvScanManager.setSearchSys(mNumberSearchDtv, mNumberSearchAtv);
            setNumberSearchNeed(1);
        }
        setFrequency();
        setSelectCountry(getCountry());
        /*if (!isNumberSearchMode && mChannelHolder.getVisibility() == View.VISIBLE) {
            mChannelHolder.setVisibility(View.GONE);
        }*/
   }

    private void setNumberSearchNeed (int isNumberSearch) {
        Settings.System.putInt(this.getContentResolver(), DroidLogicTvUtils.IS_NUMBER_SEARCH, isNumberSearch);
    }

    private void setFrequency() {
        String atvdefaultbegain = "42250";
        String atvdefaultend = "868250";
        String dtvdefaultstart = "0";
        String from = null;
        String to = null;

        int[] freqPair = new int[2];

        if (!isNumberSearchMode && mInputChannelFrom.getText() != null && mInputChannelFrom.getText().length() > 0) {
            from = mInputChannelFrom.getText().toString();
        } else if (isNumberSearchMode) {
            from = String.valueOf(mNumber.split("-")[0]);
        } else {
            from = null;
        }
        if (!isNumberSearchMode && mInputChannelTo.getText() != null && mInputChannelTo.getText().length() > 0) {
            to = mInputChannelTo.getText().toString();
        } else if (isNumberSearchMode) {
            to = String.valueOf(mNumber.split("-")[0]);
        } else {
            to = null;
        }

        if (mATvAutoScanMode == TvControlManager.ScanType.SCAN_ATV_AUTO_ALL_BAND && getAtvDtvModeFlag() == SEARCH_ATV) {
            mTvControlManager.ATVGetMinMaxFreq(freqPair);

            atvdefaultbegain = String.valueOf(freqPair[0] / 1000000);
            atvdefaultend = String.valueOf(freqPair[1] / 1000000);
            mTvScanManager.setFrequency(from != null ? from : atvdefaultbegain, to != null ? to : atvdefaultend);
        } else {
            mTvScanManager.setFrequency(from != null ? from : dtvdefaultstart, to != null ? to : dtvdefaultstart);
        }
    }

    private boolean ShowNoAtvFrequencyOrChannel() {
        boolean status = false;
        String searchmode = getSearchMode();
        boolean manualsearch = false;
        int[] freqPair = new int[2];
        int input = 0;

        if (SEARCH_MODE_LIST[TV_SEARCH_MANUAL].equals(searchmode)) {
            manualsearch = true;
        }
        if (manualsearch && mInputChannelFrom.getText() != null && mInputChannelFrom.getText().length() > 0) {
            if (mATvAutoScanMode == TvControlManager.ScanType.SCAN_ATV_AUTO_ALL_BAND && getAtvDtvModeFlag() == SEARCH_ATV) {
                mTvControlManager.ATVGetMinMaxFreq(freqPair);

                input = Integer.valueOf(mInputChannelFrom.getText().toString());

                if (input < (freqPair[0] / 1000000) || (freqPair[1] / 1000000) < input) {
                    status = false;
                    ShowToastTint("Please input from " + freqPair[0] / 1000000 + "MHz to " +  freqPair[1] / 1000000 + "MHz");
                } else {
                    status = true;
                }
            } else {
                status = true;
            }
        } else if (manualsearch && mInputChannelFrom.getText() == null || mInputChannelFrom.getText().length() <= 0) {
            status = false;
            ShowToastTint(getString(R.string.set_frquency_channel));
        } else if (!manualsearch) {
            status = true;
        }
        if (status && COUNTRY_LIST.get(COUNTRY_CHINA).equals(getCountry()) && (mInputChannelFrom.getText() == null || mInputChannelFrom.getText().length() <= 0)) {
            //show it if need a frequency range
            //status = false;
            //ShowToastTint(getString(R.string.set_frquency_channel));
        }
        return status;
    }

    private void ShowToastTint(String text) {
        LayoutInflater inflater = (LayoutInflater)ChannelSearchActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_toast, null);

        TextView propmt = (TextView)layout.findViewById(R.id.toast_content);
        propmt.setText(text);

        Toast toast = new Toast(ChannelSearchActivity.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void sendMessage(int type, int message, String information) {
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessage(msg);
    }

    private void sendMessageDelayeds(int type, int message, String information, long delayMillis) {
        Message msg = new Message();
        msg.arg1 = type;
        msg.what = message;
        msg.obj = information;
        mHandler.sendMessageDelayed(msg, delayMillis);
    }

    boolean isManualStarted = false;
    boolean isAutoStarted = false;
    boolean isAllBandScanStopped = false;

    @Override
    public void onClick(View v) {
        //At the moment, if search_type_changed is 1 in Settings, make it 0,
        //otherwise, when searching channel finished, retreat from LiveTv and lunch LiveTv again, or click Menu-->Channel,
        //it will execute resumeTvIfNeeded and send broadcast to switch channel automatically, so it should be avoid.
        resetSearchTypeChanged();

        switch (v.getId()) {
            case R.id.search_channel:
                if (SEARCH_MODE_LIST[TV_SEARCH_MANUAL].equals(getSearchMode())) {
                    if (ShowNoAtvFrequencyOrChannel()) {
                        if (!isManualStarted) {
                            isManualStarted = true;
                            mScanButton.setText(R.string.ut_stop_channel_scan);
                            if (mChannelHolder.getVisibility() == View.GONE) {
                                mChannelHolder.setVisibility(View.VISIBLE);
                            }
                        } else {
                            isManualStarted = false;
                            mScanButton.setText(R.string.ut_manual_scan);
                            if (mChannelHolder.getVisibility() == View.VISIBLE) {
                                mChannelHolder.setVisibility(View.GONE);
                            }
                        }
                        sendMessage(MANUAL_START, 0, null);
                    }
                } else {
                    if (!isAutoStarted) {
                        isAutoStarted = true;
                        mScanButton.setText(R.string.ut_stop_channel_scan);
                        sendMessage(CHANNEL, 0, null);
                        if (mChannelHolder.getVisibility() == View.GONE) {
                            mChannelHolder.setVisibility(View.VISIBLE);
                        }
                    } else {
                        isAutoStarted = false;
                        mScanButton.setText(R.string.ut_auto_scan);
                        if (mChannelHolder.getVisibility() == View.VISIBLE) {
                            mChannelHolder.setVisibility(View.GONE);
                        }
                    }
                    sendMessage(AUTO_START, 0, null);
                }
                break;
            /*case R.id.tune_auto:
                if (!isAutoStarted) {
                    isAutoStarted = true;
                    mAutoScanButton.setText(R.string.ut_stop_channel_scan);
                } else {
                    isAutoStarted = false;
                    mAutoScanButton.setText(R.string.ut_auto_scan);
                }
                sendMessage(AUTO_START, 0, null);
                break;
            case R.id.manual_tune_atv:
            case R.id.auto_tune_atv:
                if (mManualATV.isChecked() || mAutoATV.isChecked()) {
                    mAtscSearchTypeOption.setEnabled(true);
                } else {
                    mAtscSearchTypeOption.setEnabled(false);
                }
                break;*/
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "==== focus =" + getCurrentFocus() + ", keycode =" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isSearching()) {
                    handler.post(StopScanRunnable);//prevent anr
                } else {
                    finish();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                startShowActivityTimer();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    //prevent anr when stop scan
    Runnable StopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSearching()) {
                dtvStopScan();
                resetSearchStatus();
            }
        }
    };

    public void resetSearchTypeChanged() {
        int searchTypeChanged = mTvControlDataManager.getInt(this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_TYPE_CHANGED, 0);
        if (searchTypeChanged == 1) {
            mTvControlDataManager.putInt(this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_TYPE_CHANGED, 0);
        }
    }

    @Override
    public void finish() {
        //At the moment, if search_type_changed is 1 in Settings, make it 0,
        //otherwise, when change search_type in ChannelSearchActivity, but don't search channel and push the EXIT key to return to LiveTv,
        //it will do resumeTvIfNeeded and the current channel will be switched to next one, so this can't happen.
        resetSearchTypeChanged();

        isFinished = true;

        if (!mPowerManager.isScreenOn()) {
            Log.d(TAG, "TV is going to sleep, stop tv");
            return;
        }
        //send search info to livetv if found any
        if (hasFoundInfo.size() > 0) {
            Intent intent = new Intent();
            for (Object key : hasFoundInfo.keySet()) {
                intent.putExtra((String)key, (int)hasFoundInfo.get(key));
                Log.d(TAG, "searched info key = " + ((String)key) + ", value = " + ((int)hasFoundInfo.get(key)));
            }
            setResult(RESULT_OK, intent);
        }
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (isFinished) {
            ShowToastTint(getString(R.string.tv_search_channel_stopped));
            finish();
            //resume();
            isFinished = false;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(mReceiver);

        if (isSearching()) {
            dtvStopScan();

            if (!mPowerManager.isScreenOn()) {
                stopTv();
            }
        }

        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mTvScanManager.unBindService();
        resetSearchStatus();
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "finalized");
        super.finalize();
    }

    public void startShowActivityTimer () {
        Log.d(TAG, "===== startShowActivityTimer");
        handler.removeMessages(START_TIMER);
        handler.sendEmptyMessageDelayed(START_TIMER, 30 * 1000);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_TIMER:
                    Log.d(TAG, "===== START_TIMER");
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    if ((!isNumberSearchMode && (!isSearching() && !imm. isAcceptingText())) || (isNumberSearchMode && hasFoundChannel)) {
                        finish();
                    } else {
                        sendEmptyMessageDelayed(START_TIMER, 30 * 1000);
                    }
                    break;
                case START_INIT:
                    Log.d(TAG, "===== START_INIT");
                    if (mTvScanManager.isConnected()) {
                        mTvScanManager.init();
                        if (isNumberSearchMode) {
                            initNumberSearch(ChannelSearchActivity.this);
                            return;
                        }
                    } else {
                        sendEmptyMessageDelayed(START_INIT, 200);
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*if (action.equals(DroidLogicTvUtils.ACTION_CHANNEL_CHANGED)) {
                mSettingsManager.setCurrentChannelData(intent);
                    mOptionUiManagerT.init(mSettingsManager);
                currentFragment.refreshList();
            } else */if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra("reason");
                if (TextUtils.equals(reason, "homekey")) {
                    stopTv();
                    Log.d(TAG,"stop tv when exiting by home key");
                    finish();
                }
            }
        }
    };

    private void resume() {
        isManualStarted = false;
        isAutoStarted = false;
        isAllBandScanStopped = false;

        initSpinner();
        mChannelList.setAdapter(null);
        mTvScanManager.init();
        startShowActivityTimer();
    }

    private void release() {
        mTvScanManager.release();
        handler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        handler = null;
        mTvDataBaseManager = null;
        mTvControlDataManager = null;
        mTvScanManager = null;
        exitNumberSearch();
    }

    final int STANDARD = 0;
    final int LRC = 1;
    final int HRC = 2;
    final int AUTO = 3;

    final int TV_SEARCH_SYS_AUTO = 0;
    final int TV_SEARCH_SYS_PAL = 1;
    final int TV_SEARCH_SYS_NTSC = 2;
    final int TV_SEARCH_SYS_SECAM = 3;

    final int ATV_SEARCH_SOUND_MIN = 0;
    final int ATV_SEARCH_SOUND_DK = 0;
    final int ATV_SEARCH_SOUND_I = 1;
    final int ATV_SEARCH_SOUND_BG = 2;
    final int ATV_SEARCH_SOUND_M = 3;
    final int ATV_SEARCH_SOUND_L = 4;
    final int ATV_SEARCH_SOUND_AUTO = 5;
    final int ATV_SEARCH_SOUND_MAX = 6;

    public final String[] DEFAULT_ATSC_TYPE_LIST = {TvContract.Channels.TYPE_ATSC_T};
    public final String[] DEFAULT_DTV_TYPE_LIST = {TvContract.Channels.TYPE_DTMB};
    public final String[] INDIA_TV_TYPE_LIST = {TvContract.Channels.TYPE_NTSC};
    public final String[] INDONESIA_TV_TYPE_LIST = {TvContract.Channels.TYPE_NTSC, TvContract.Channels.TYPE_DVB_T};
    public final String[] AMERICA_TV_TYPE_LIST = {TvContract.Channels.TYPE_ATSC_T, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C};
    public final String[] MEXICO_TV_TYPE_LIST = {TvContract.Channels.TYPE_ATSC_T, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C, TvContract.Channels.TYPE_ATSC_C};
    public final String[] GERMANY_TV_TYPE_LIST = {TvContract.Channels.TYPE_NTSC, TvContract.Channels.TYPE_DVB_T, TvContract.Channels.TYPE_DVB_C, TvContract.Channels.TYPE_DVB_S};
    public final String[] CHINA_TV_TYPE_LIST = {TvContract.Channels.TYPE_PAL, TvContract.Channels.TYPE_DTMB};
    public final String[] BRAZIL_TV_TYPE_LIST = {TvContract.Channels.TYPE_PAL, TvContract.Channels.TYPE_ISDB_T};

    public final int TV_SEARCH_MANUAL = 0;
    public final int TV_SEARCH_AUTO = 1;
    public final String[] SEARCH_MODE_LIST = {"manual", "auto"};

    public final int TV_SEARCH_ATV = 0;
    public final int TV_SEARCH_DVB_T = 1;
    public final int TV_SEARCH_ATSC_T = 2;
    public final int TV_SEARCH_ATSC_C = 3;
    public final int TV_SEARCH_ATSC_AUTO = 0;
    public final int TV_SEARCH_ATSC_LRC = 1;
    public final int TV_SEARCH_ATSC_HRC = 2;

    public final int SEARCH_ATV = 0;
    public final int SEARCH_DTV = 1;
    public final int SEARCH_ATV_DTV = 2;

    public final int TV_SEARCH_ORDER_LOW = 0;
    public final int TV_SEARCH_ORDER_HIGH = 1;
    public final String[] SEARCH_ORDER_LIST = {"low_to_high", "high_to_low"};

    private void setAtsccListMode(int mode) {
        Log.d(TAG, "setAtsccListMode = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_ATSC_CLIST, mode);
    }

    private int getAtsccListMode() {
        Log.d(TAG, "getAtsccListMode = " + mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_ATSC_CLIST, STANDARD));
        return mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_ATSC_CLIST, STANDARD);
    }

    private void setTvSearchTypeSys(int mode) {
        Log.d(TAG, "setTvSearchTypeSys = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, mode);
    }

    private int getTvSearchTypeSys() {
        Log.d(TAG, "getTvSearchTypeSys = " + mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, TV_SEARCH_SYS_AUTO));
        return mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SYS, TV_SEARCH_SYS_AUTO);
    }

    private void setTvSearchSoundSys(int mode) {
        Log.d(TAG, "setTvSearchSoundSys = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SOUND_SYS, mode);
    }

    private int getTvSearchSoundSys() {
        Log.d(TAG, "getTvSearchSoundSys = " + mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SOUND_SYS, ATV_SEARCH_SOUND_AUTO));
        return mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), ATSC_TV_SEARCH_SOUND_SYS, ATV_SEARCH_SOUND_AUTO);
    }

    public void setCountry(String country) {
        Log.d(TAG, "setCountry = " + country);
        mTvControlDataManager.putString(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.KEY_SEARCH_COUNTRY, country);
    }

    public String getCountry() {
        String country = mTvControlDataManager.getString(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.KEY_SEARCH_COUNTRY);
        if (TextUtils.isEmpty(country)) {
            country = COUNTRY_LIST.get(COUNTRY_CHINA);
            setCountry(country);
        }
        Log.d(TAG, "getCountry = " + country);
        return country;
    }

    //set by ui
    public void setCountryByIndex(int index) {
        String country = getSupportCountry().get(index);
        Log.d(TAG, "setCountryByIndex = " + country);
        mTvControlDataManager.putString(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.KEY_SEARCH_COUNTRY, country);
    }

    public String getCountryByIndex(int index) {
        String country = getSupportCountry().get(index);
        Log.d(TAG, "getCountryByIndex = " + country);
        return country;
    }

    public String getTVSupportCountries() {
        return mTvControlManager.GetTVSupportCountries();
    }

    public ArrayList<String> getSupportCountry() {
        String config = getTVSupportCountries();//"US,IN,ID,MX,DE,CN,BR";
        Log.d(TAG, "getCountry = " + config);
        String[] supportcountry = {"US", "IN", "ID", "MX", "DE", "CN", "BR"};//default
        ArrayList<String> getsupportlist = new ArrayList<String>();
        if (!TextUtils.isEmpty(config)) {
            supportcountry = config.split(",");
            for (String temp : supportcountry) {
                getsupportlist.add(temp);
            }
        } else {
            for (String temp : supportcountry) {
                getsupportlist.add(temp);
            }
        }
        return getsupportlist;
    }

    public void setSelectCountry(String country) {
        Log.d(TAG, "setCountrytoTvserver = " + country);
        mTvControlManager.SetTvCountry(country);
    }

    public String[] getTvTypebyCountry(int country) {
        switch (country) {
            case COUNTRY_INDIA:
                return INDIA_TV_TYPE_LIST;
            case COUNTRY_INDONESIA:
                return INDONESIA_TV_TYPE_LIST;
            case COUNTRY_AMERICA:
                return AMERICA_TV_TYPE_LIST;
            case COUNTRY_MEXICO:
                return MEXICO_TV_TYPE_LIST;
            case COUNTRY_GERMANY:
                return GERMANY_TV_TYPE_LIST;
            case COUNTRY_CHINA:
                return CHINA_TV_TYPE_LIST;
            case COUNTRY_BRAZIL:
                return BRAZIL_TV_TYPE_LIST;
            default:
                return INDIA_TV_TYPE_LIST;
        }
    }

    private int getIndex(String value, String[] list) {
        if (value != null && list != null) {
            for (int i = 0; i < list.length; i++) {
                if (value.equals(list[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setSearchMode(String mode) {
        Log.d(TAG, "setSearchMode = " + mode);
        mTvControlDataManager.putString(ChannelSearchActivity.this.getContentResolver(), "tv_search_mode", mode);
    }

    public String getSearchMode() {
        String mode = mTvControlDataManager.getString(ChannelSearchActivity.this.getContentResolver(), "tv_search_mode");
        if (mode == null) {
            mode = SEARCH_MODE_LIST[TV_SEARCH_AUTO];
            setSearchMode(mode);
        }
        Log.d(TAG, "getSearchMode = " + mode);
        return mode;
    }

    public void setSearchType(int mode) {
        Log.d(TAG, "setSearchType = " + mode);
        DroidLogicTvUtils.setSearchType(ChannelSearchActivity.this, mode);
        //mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), "tv_search_type", mode);
    }

    public int getSearchType() {
        int mode = mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), "tv_search_type", TV_SEARCH_ATV);
        Log.d(TAG, "getSearchType = " + mode);
        return mode;
    }

    public void setSearchOrder(int mode) {
        Log.d(TAG, "setSearchOrder = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), "tv_search_order", mode);
    }

    public int getSearchOrder() {
        int mode = mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), "tv_search_order", TV_SEARCH_ORDER_LOW);
        Log.d(TAG, "getSearchOrder = " + mode);
        return mode;
    }

    public void setAtvDtvModeFlag(int mode) {
        Log.d(TAG, "setAtvDtvModeFlag = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), "search_atv_dtv_flag", mode);
    }

    public int getAtvDtvModeFlag() {
        int mode = mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), "search_atv_dtv_flag", SEARCH_ATV);
        Log.d(TAG, "getAtvDtvModeFlag = " + mode);
        return mode;
    }

    public void setDvbcQamMode(int mode) {
        Log.d(TAG, "setDvbcQamMode = " + mode);
        mTvControlDataManager.putInt(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_DVBC_QAM, mode);
    }

    public int getDvbcQamMode() {
        int mode = mTvControlDataManager.getInt(ChannelSearchActivity.this.getContentResolver(), DroidLogicTvUtils.TV_SEARCH_DVBC_QAM, DroidLogicTvUtils.TV_SEARCH_DVBC_QAM16);
        Log.d(TAG, "getDvbcQamMode = " + mode);
        return mode;
    }

     //30s timeout, stop scan
    Runnable TimeOutStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSearching()) {
                dtvStopScan();
            }
            exitNumberSearch();
            finish();
        }
    };
}
