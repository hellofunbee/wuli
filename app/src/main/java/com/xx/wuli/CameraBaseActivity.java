/**
 * <p>DemoActivity Class</p>
 *
 * @author zhuzhenlei 2014-7-17
 * @version V1.0
 * @modificationHistory
 * @modify by user:
 * @modify by reason:
 */
package com.xx.wuli;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_COMPRESSIONCFG_V30;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.PTZCommand;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;

import java.util.List;

import static com.hikvision.netsdk.PTZPresetCmd.CLE_PRESET;
import static com.hikvision.netsdk.PTZPresetCmd.SET_PRESET;

/**
 * <pre>
 *  ClassName  DemoActivity Class
 * </pre>
 *
 * @author zhuzhenlei
 * @version V1.0
 * @modificationHistory
 */
public class CameraBaseActivity extends Activity implements Callback, OnTouchListener {
    private SurfaceView m_osurfaceView = null;
    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30 = null;
    private int m_iLogID = -1; // return by NET_DVR_Login_v30
    private int m_iPlayID = -1; // return by NET_DVR_RealPlay_V30
    private int m_iPlaybackID = -1; // return by NET_DVR_PlayBackByTime
    private int m_iPort = -1; // play port
    private int m_iStartChan = 0; // start channel no
    private int m_iChanNum = 0; // channel number
    private final String TAG = "DemoActivity";
    private boolean m_bNeedDecode = true;
    private boolean m_bStopPlayback = false;
    private Thread thread;
    private boolean isShow = true;
    private Button btnUp;
    private Button btnDown;
    private Button btnLeft;
    private Button btnRight;
    private Button btnZoomIn;
    private Button btnZoomOut;
    private CameraManager h1;
    private AppData app;

    public String ADDRESS = "111.53.182.34";
    public int PORT = 9030;
    public String USER = "admin";
    public String PSD = "vr123456";

    float rate = (float) 4.0 / 3;
    int heigth;
    int width;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //如果是竖排,则改为横排;反之然
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Display display = getWindowManager().getDefaultDisplay();
        heigth = display.getWidth();
        width = display.getHeight();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ADDRESS = bundle.getString("ip");
        PORT = bundle.getInt("port");
        USER = bundle.getString("name");
        PSD = bundle.getString("pass");

        CrashUtil crashUtil = CrashUtil.getInstance();
        crashUtil.init(this);
        app = (AppData) getApplication();
        setContentView(R.layout.main);

        if (!initeActivity()) {
            this.finish();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                init();
            }
        }).start();

    }

    private void init() {
        if (!initeSdk()) {
            this.finish();
            return;
        }
        //设置连接时间与重连时间
        HCNetSDK.getInstance().NET_DVR_SetConnectTime(8000);
        HCNetSDK.getInstance().NET_DVR_SetRecvTimeOut(8000);
        HCNetSDK.getInstance().NET_DVR_SetReconnect(10000, true);
        // login on the device
        m_iLogID = loginDevice();
        if (m_iLogID < 0) {
            showMsg("登录失败！");
            Log.e(TAG, "----------------This device logins failed!-------------------");

            return;
        } else {
            System.out.println("-----------------------m_iLogID=---------------------" + m_iLogID);
            try {
                //获取分辨率
                NET_DVR_COMPRESSIONCFG_V30 info = CameraManager.getCompressInfo(m_iLogID);
                VideoParamsBean vp = new VideoParamsBean();
                vp.parse(info);
                VideoShemaBean vb = CameraManager.getIpcAbility(m_iLogID);

                List<VideoResolution> ress = vb.getMainChannel().getSolutions();
                String c_res = vp.getMainStream().getResolution();
                for (VideoResolution v : ress) {
                    if (c_res.equals(v.getIndex())) {
                        String xy = v.getResolution();
                        if (xy != null && xy.indexOf("*") != -1) {
                            xy = xy.replace("*", "&");
                            String[] xys = xy.split("&");
                            float w = Float.parseFloat(xys[0]);
                            float h = Float.parseFloat(xys[1]);
                            rate = w / h;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    RelativeLayout.LayoutParams linearParams = (RelativeLayout.LayoutParams) m_osurfaceView.getLayoutParams(); //取控件textView当前的布局参数 linearParams.height = 20;// 控件的高强制设成20
                                    linearParams.width = (int) (width * rate);// 控件的宽强制设成30
                                    linearParams.height = width;// 控件的宽强制设成30
                                    m_osurfaceView.setLayoutParams(linearParams); //使设置好的布局参数应用到控件

                                }
                            });
                        }
                  /* 1280*720
                   1280*960
                   1920*1080
                   2048*1536*/
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // get instance of exception callback and set
        ExceptionCallBack oexceptionCbf = getExceptiongCbf();
        if (oexceptionCbf == null) {
            Log.e(TAG, "ExceptionCallBack object is failed!");
            return;
        }
        if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(
                oexceptionCbf)) {
            Log.e(TAG, "NET_DVR_SetExceptionCallBack is failed!");
            return;
        }

        //预览
        final NET_DVR_PREVIEWINFO ClientInfo = new NET_DVR_PREVIEWINFO();
        ClientInfo.lChannel = 0;
        ClientInfo.dwStreamType = 0; // substream
        ClientInfo.bBlocked = 1;
        //设置默认点
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    SystemClock.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isShow)
                                startSinglePreview();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    // @Override
    public void surfaceCreated(SurfaceHolder holder) {
        m_osurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i(TAG, "surface is created" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface.isValid()) {
            if (!Player.getInstance()
                    .setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    // @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    // @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Player setVideoWindow release!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (holder.getSurface().isValid()) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("m_iPort", m_iPort);
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_iPort = savedInstanceState.getInt("m_iPort");
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * @return true - success;false - fail
     * @fn initeSdk
     * @author zhuzhenlei
     * @brief SDK init
     */
    private boolean initeSdk() {
        // init net sdk
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e(TAG, "HCNetSDK init is failed!");
            return false;
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/", true);
        return true;
    }

    // GUI init
    private boolean initeActivity() {
        findViews();
        m_osurfaceView.getHolder().addCallback(this);
        return true;
    }

    // get controller instance
    private void findViews() {

        final RelativeLayout btns = findViewById(R.id.CAMlinear);
        this.btnZoomOut = (Button) findViewById(R.id.btn_ZoomOut);
        this.btnZoomIn = (Button) findViewById(R.id.btn_ZoomIn);
        this.btnRight = (Button) findViewById(R.id.btn_Right);
        this.btnLeft = (Button) findViewById(R.id.btn_Left);
        this.btnDown = (Button) findViewById(R.id.btn_Down);
        this.btnUp = (Button) findViewById(R.id.btn_Up);
        btnUp.setOnTouchListener(this);
        btnDown.setOnTouchListener(this);
        btnLeft.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);
        btnZoomIn.setOnTouchListener(this);
        btnZoomOut.setOnTouchListener(this);
        this.m_osurfaceView = (SurfaceView) findViewById(R.id.sf_VideoMonitor);

        RelativeLayout.LayoutParams linearParams = (RelativeLayout.LayoutParams) m_osurfaceView.getLayoutParams(); //取控件textView当前的布局参数 linearParams.height = 20;// 控件的高强制设成20
        linearParams.width = (int) (width * rate);// 控件的宽强制设成30
        linearParams.height = width;// 控件的宽强制设成30
        m_osurfaceView.setLayoutParams(linearParams); //使设置好的布局参数应用到控件

        System.out.println("----------------rate----------------------:" + rate);

        m_osurfaceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btns.getVisibility() == View.GONE){
                    showAndHiddenAnimation(btns,1,300);
                }else {
                    showAndHiddenAnimation(btns,0,300);
                }
            }
        });

    }

    /**
     * 渐隐渐现动画
     * @param view 需要实现动画的对象
     * @param state 需要实现的状态
     * @param duration 动画实现的时长（ms）
     */
    public static void showAndHiddenAnimation(final View view,int state,long duration){
        float start = 0f;
        float end = 0f;
        if(state == 1){
            end = 1f;
            view.setVisibility(View.VISIBLE);
        } else
        if(state == 0){
            start = 1f;
            view.setVisibility(View.GONE);
        }
        AlphaAnimation animation = new AlphaAnimation(start, end);
        animation.setDuration(duration);
        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.clearAnimation();
            }
        });
        view.setAnimation(animation);
        animation.start();
    }


    @Override
    public boolean onTouch(final View v, final MotionEvent event) {

        if (!NotNull.isNotNull(h1)) return false;
        new Thread() {
            @Override
            public void run() {
                switch (v.getId()) {
                    case R.id.btn_Up:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startMove(8, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopMove(8, m_iLogID);
                        }
                        break;
                    case R.id.btn_Left:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startMove(4, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopMove(4, m_iLogID);
                        }
                        break;
                    case R.id.btn_Right:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startMove(6, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopMove(6, m_iLogID);
                        }
                        break;
                    case R.id.btn_Down:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startMove(2, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopMove(2, m_iLogID);
                        }
                        break;
                    case R.id.btn_ZoomIn:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startZoom(1, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopZoom(1, m_iLogID);
                        }
                        break;
                    case R.id.btn_ZoomOut:
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            h1.startZoom(-1, m_iLogID);
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            h1.stopZoom(-1, m_iLogID);
                        }
                        break;
                    default:
                        break;
                }
            }
        }.start();
        return false;
    }


    private AlertDialog getDialongView(View view) {
        final AlertDialog.Builder builder6 = new AlertDialog.Builder(CameraBaseActivity.this);
        builder6.setView(view);
        builder6.create();
        AlertDialog dialog = builder6.show();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        window.setAttributes(lp);
        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cleanup();
        m_iLogID = -1;
        // whether we have logout
        if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)) {
            Log.e(TAG, " NET_DVR_Logout is failed!");
            return;
        }
        stopSinglePreview();
    }

    private void startSinglePreview() {
        if (m_iPlaybackID >= 0) {
            Log.i(TAG, "Please stop palyback first");
            return;
        }
        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e(TAG, "fRealDataCallBack object is failed!");
            return;
        }
        Log.i(TAG, "m_iStartChan:" + m_iStartChan);

        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = m_iStartChan;
        previewInfo.dwStreamType = 1; // substream
        previewInfo.bBlocked = 1;
//
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID,
                previewInfo, fRealDataCallBack);
        if (m_iPlayID < 0) {
            Log.e(TAG, "NET_DVR_RealPlay is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }
        isShow = false;
        if (NotNull.isNotNull(thread)) {
            thread.interrupt();
        }
        h1 = new CameraManager();
        h1.setLoginId(m_iLogID);
        Intent intent = getIntent();
        if (NotNull.isNotNull(intent) && intent.getIntExtra("INDEX", -1) != -1) {
            int point = app.preferences.getInt("POINT", 0);
            boolean b = HCNetSDK.getInstance().NET_DVR_PTZPreset(m_iPlayID, PTZCommand.GOTO_PRESET,
                    point);
        }
    }

    /**
     * @return NULL
     * @fn stopSinglePreview
     * @author zhuzhenlei
     * @brief stop preview
     */
    private void stopSinglePreview() {
        if (m_iPlayID < 0) {
            Log.e(TAG, "m_iPlayID < 0");
            return;
        }
        // net sdk stop preview
        if (!HCNetSDK.getInstance().NET_DVR_StopRealPlay(m_iPlayID)) {
            Log.e(TAG, "StopRealPlay is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }

        m_iPlayID = -1;
        stopSinglePlayer();
    }

    private void stopSinglePlayer() {
        Player.getInstance().stopSound();
        // player stop play
        if (!Player.getInstance().stop(m_iPort)) {
            Log.e(TAG, "stop is failed!");
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "closeStream is failed!");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "freePort is failed!" + m_iPort);
            return;
        }
        m_iPort = -1;
    }

    /**
     * @return login ID
     * @fn loginNormalDevice
     * @author zhuzhenlei
     * @brief login on device
     */
    private int loginNormalDevice() {
        // get instance
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e(TAG, "HKNetDvrDeviceInfoV30 new is failed!");
            return -1;
        }
        // call NET_DVR_Login_v30 to login on, port 8000 as default
        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(ADDRESS, PORT,
                USER, PSD, m_oNetDvrDeviceInfoV30);

        if (iLogID < 0) {
            Log.e(TAG, "NET_DVR_Login is failed!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        if (m_oNetDvrDeviceInfoV30.byChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        } else if (m_oNetDvrDeviceInfoV30.byIPChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum
                    + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256;
        }
        Log.i(TAG, "---------------NET_DVR_Login is Successful!---------------------");
        return iLogID;
    }

    /**
     * @return login ID
     * @fn loginDevice
     * @author zhangqing
     * @brief login on device
     */
    private int loginDevice() {
        int iLogID = -1;
        iLogID = loginNormalDevice();
        return iLogID;
    }

    /**
     * @return exception instance
     * @fn getExceptiongCbf
     */
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * @return callback instance
     * @fn getRealPlayerCbf
     * @brief get realplay callback instance
     */
    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType,
                                          byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                CameraBaseActivity.this.processRealData(iDataType, pDataBuffer,
                        iDataSize, Player.STREAM_REALTIME);
            }
        };
        return cbf;
    }

    /**
     * @param iDataType   - data type [in]
     * @param pDataBuffer - data buffer [in]
     * @param iDataSize   - data size [in]
     * @param iStreamMode - stream mode [in]
     * @return NULL
     * @fn processRealData
     * @author zhuzhenlei
     * @brief process real data
     */
    public void processRealData(int iDataType,
                                byte[] pDataBuffer, int iDataSize, int iStreamMode) {
        if (!m_bNeedDecode) {
            // Log.i(TAG, "iPlayViewNo:" + iPlayViewNo + ",iDataType:" +
            // iDataType + ",iDataSize:" + iDataSize);
        } else {
            if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
                if (m_iPort >= 0) {
                    return;
                }
                m_iPort = Player.getInstance().getPort();
                if (m_iPort == -1) {
                    Log.e(TAG, "getPort is failed with: "
                            + Player.getInstance().getLastError(m_iPort));
                    return;
                }
                Log.i(TAG, "getPort succ with: " + m_iPort);
                if (iDataSize > 0) {
                    if (!Player.getInstance().setStreamOpenMode(m_iPort,
                            iStreamMode)) // set stream mode
                    {
                        Log.e(TAG, "setStreamOpenMode failed");
                        return;
                    }
                    if (!Player.getInstance().openStream(m_iPort, pDataBuffer,
                            iDataSize, 2 * 1024 * 1024)) // open stream
                    {
                        Log.e(TAG, "openStream failed");
                        return;
                    }
                    if (!Player.getInstance().play(m_iPort,
                            m_osurfaceView.getHolder())) {
                        Log.e(TAG, "play failed");
                        return;
                    }
                    if (!Player.getInstance().playSound(m_iPort)) {
                        Log.e(TAG, "playSound failed with error code:"
                                + Player.getInstance().getLastError(m_iPort));
                        return;
                    }
                }
            } else {
                if (!Player.getInstance().inputData(m_iPort, pDataBuffer,
                        iDataSize)) {
                    // Log.e(TAG, "inputData failed with: " +
                    // Player.getInstance().getLastError(m_iPort));
                    for (int i = 0; i < 4000 && m_iPlaybackID >= 0
                            && !m_bStopPlayback; i++) {
                        if (Player.getInstance().inputData(m_iPort,
                                pDataBuffer, iDataSize)) {
                            break;

                        }

                        if (i % 100 == 0) {
                            Log.e(TAG, "inputData failed with: "
                                    + Player.getInstance()
                                    .getLastError(m_iPort) + ", i:" + i);
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * @return NULL
     * @fn Cleanup
     * @author zhuzhenlei
     * @brief cleanup
     */
    public void Cleanup() {
        // release player resource

        Player.getInstance().freePort(m_iPort);
        m_iPort = -1;
        // release net SDK resource
        HCNetSDK.getInstance().NET_DVR_Cleanup();
    }


    public void SetOnclick(View view) {
        View linearLayout = getLayoutInflater().inflate(R.layout.setting_page, null);
        Button button2 = (Button) linearLayout.findViewById(R.id.button2);
        Button button1 = (Button) linearLayout.findViewById(R.id.button1);
        Button button = (Button) linearLayout.findViewById(R.id.button);
        //设置预置点
        final EditText editText = (EditText) linearLayout.findViewById(R.id.editText);
        final AlertDialog dialog = getDialongView(linearLayout);
        //设置预置点
        button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer integer = Integer.valueOf(editText.getText().toString());
                if (integer > 255 || integer < 0) {
                    Toast.makeText(CameraBaseActivity.this, "请设置0-255之间", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean b = HCNetSDK.getInstance().NET_DVR_PTZPreset(m_iPlayID, SET_PRESET,
                        integer);
                if (b) {
                    Toast.makeText(CameraBaseActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                    app.editor.putInt("POINT", integer).commit();
                } else {
                    Toast.makeText(CameraBaseActivity.this, "设置失败", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onClick: " + b);
            }
        });
        //清楚预置点
        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Integer integer = Integer.valueOf(editText.getText().toString());
                if (integer > 255 || integer < 0) {
                    Toast.makeText(CameraBaseActivity.this, "请设置0-255之间", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean b = HCNetSDK.getInstance().NET_DVR_PTZPreset(m_iPlayID, CLE_PRESET,
                        integer);
                if (b) {
                    app.editor.remove("POINT").commit();
                    Toast.makeText(CameraBaseActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CameraBaseActivity.this, "清除失败", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onClick: " + b);
                dialog.dismiss();
            }
        });
        //转到预置点
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Integer integer = Integer.valueOf(editText.getText().toString());
                if (integer > 255 || integer < 0) {

                    Toast.makeText(CameraBaseActivity.this, "请设置0-255之间", Toast.LENGTH_SHORT).show();
                    return;
                }
                int point = app.preferences.getInt("POINT", 0);
                if (point == 0) {
                    Toast.makeText(app, "请先设置预设点", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean b = HCNetSDK.getInstance().NET_DVR_PTZPreset(m_iPlayID, PTZCommand.GOTO_PRESET,
                        point);
                Log.d(TAG, "onClick: " + b);
            }
        });


    }

    private void showMsg(final String msg) {
        final CharSequence cm = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), cm.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出视频",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
        }
    }
}
