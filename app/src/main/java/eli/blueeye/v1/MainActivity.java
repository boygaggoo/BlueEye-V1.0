package eli.blueeye.v1;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import eli.blueeye.capture.ScreenRecorder;
import eli.blueeye.capture.ScreenShooter;
import eli.blueeye.server.GravitySensorListener;
import eli.blueeye.util.Util;
import eli.blueeye.view.TakePhotoView;
import eli.blueeye.view.VlcPlayer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TakePhotoView.LongTouchListener{

    /**
     * http://img95.699pic.com/videos/2016/09/05/65b0f4fc-c8da-4287-bdae-603a492c519f.mp4
     * http://img95.699pic.com/videos/2016/09/19/eb3b9233-d919-46ce-b2d5-a30e4dd9fcdb.mp4
     * http://img95.699pic.com/videos/2016/09/18/38d63eab-796a-43be-998f-c00308d186f0.mp4
     */

    private static final String TAG = "MainActivity";
    private static final String url = "/sdcard/1/beach.mp4";
    private Context context ;
    private KeyguardManager keyguardManager;

    private SurfaceView surfaceView;
    private VlcPlayer vlcPlayer;
    private ImageButton playerControl;
    private LinearLayout buttonArea;
    private ImageButton fullScreen;
    private TakePhotoView takePhotoView;
    private ImageButton camera ;

    private boolean isPlayer = true;
    private boolean isLand = false;
    private boolean isShowCamera = false;
    private boolean isScreenShoot = false;
    private boolean isRecording = false;

    private PhotoHandler photoHandler;
    private Handler hiddenHandler;
    private HiddenRunnable hiddenRunnable;
    private GravitySensorListener sensorListener;
    private ScreenShooter screenShooter;
    private ScreenRecorder screenRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = getApplicationContext();
        keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        //初始化视图
        initView();
        //初始化传感器
        sensorListener = new GravitySensorListener(this, fullScreen, keyguardManager);
        //初始化播放器
        initSurface();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vlcPlayer.play();
        //初始化Handler和线程
        initHandlerThread();
        //初始化截录屏工具
        initCapture();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //显示按钮区域
        setAreaVisibility(true);
        removeHiddenThread();
        vlcPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vlcPlayer.releasePlayer();
    }

    /**
     * 初始化视图
     */
    private void initView(){
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceView.setOnClickListener(this);

        playerControl = (ImageButton) findViewById(R.id.control);
        playerControl.setOnClickListener(this);

        fullScreen = (ImageButton) findViewById(R.id.fullScreen);
        fullScreen.setOnClickListener(this);

        buttonArea = (LinearLayout) findViewById(R.id.buttonarea);

        camera = (ImageButton) findViewById(R.id.camera);
        camera.setOnClickListener(this);

        takePhotoView = (TakePhotoView) findViewById(R.id.takephoto);
        takePhotoView.setOnClickListener(this);
        takePhotoView.setOnLongTouchListener(this, 500);

        resetViewSize();
    }

    /**
     * 将播放器重置为16:9
     */
    private void resetViewSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = (width * 9) / 16;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        surfaceView.setLayoutParams(lp);
        //设置工具条布局
        setToolBarPosition(width, height);
    }

    /**
     * 初始化播放器
     */
    private void initSurface() {
        vlcPlayer = new VlcPlayer(surfaceView, this, url);
        vlcPlayer.createPlayer();
    }

    /**
     * 初始化隐藏Handler和线程
     */
    private void initHandlerThread(){
        if (photoHandler == null)
            photoHandler = new PhotoHandler();
        if (hiddenHandler == null)
            hiddenHandler = new Handler();
        addHiddenThread();
    }

    /**
     * 初始化截录屏工具
     */
    private void initCapture() {
        if (screenShooter == null) {
            screenShooter = new ScreenShooter(this, photoHandler);
        }
        if (screenRecorder == null) {
            screenRecorder = new ScreenRecorder(this);
        }
    }

    /**
     * 按键监听事件
     * @param keyCode   按键代码
     * @param event     事件
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isLand)
            fullScreen.performClick();
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 点击事件
     * @param v
     */
    @Override
    public void onClick(View v){
        switch (v.getId()){

            case R.id.control:
                //播放、暂停
                switchControlButton(v);
                break;

            case R.id.surface:
                //显示、隐藏功能按钮
                switchShowArea();
                break;

            case R.id.fullScreen:
                //切换横竖屏
                switchScreenOrientation();
                break;

            case R.id.takephoto:
                //截图
                startCapture();
                break;

            case R.id.camera:
                //切换显示摄像头
                switchShowCamera(v);
                break;
        }
    }

    /**
     * 长按触发事件
     */
    @Override
    public void onLongTouch() {
        Log.i(TAG, "正在录像");
        //停止隐藏线程
        removeHiddenThread();
        if (!isRecording)
            startRecord();
    }

    /**
     * 结束长按触发事件
     */
    @Override
    public void onTouchStop() {
        if (isRecording) {
            isRecording = false;
            stopRecord();
            Log.i(TAG, "录像结束");
            //重新开始隐藏线程
            addHiddenThread();
        }
    }

    /**
     * 切换横竖屏
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //定义传感器方向
        sensorListener.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        //切换UI
        switchFullImage(fullScreen);
        //获取屏幕宽高
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        FrameLayout.LayoutParams lp;
        //横屏
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp = new FrameLayout.LayoutParams(screenWidth, screenHeight);
            surfaceView.setLayoutParams(lp);
            //设置工具条布局
            setToolBarPosition(screenWidth, screenHeight);
        }
        //竖屏
        else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            int height = (screenWidth * 9) / 16;
            lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            surfaceView.setLayoutParams(lp);
            //设置工具条布局
            setToolBarPosition(screenWidth, height);
        }
    }

    /**
     * 处理截录屏请求
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isScreenShoot) {
            Log.i(TAG, "onActivityResult: 截屏");
            if (screenShooter == null) {
                screenShooter = new ScreenShooter(this, photoHandler);
            }
            screenShooter.prepareCapture(resultCode, data);
            screenShooter.start();
        } else if(isRecording) {
            Log.i(TAG, "onActivityResult: 录屏");
            if (screenRecorder == null) {
                screenRecorder = new ScreenRecorder(this);
            }
            screenRecorder.prepareCapture(resultCode, data);
            screenRecorder.start();
        }
    }

    /**
     * 截屏
     */
    private void startCapture() {
        isRecording = false;
        isScreenShoot = true;
        if (screenShooter == null) {
            screenShooter = new ScreenShooter(this, photoHandler);
        }
        startActivityForResult(screenShooter.mediaManager.createScreenCaptureIntent(), 1);
    }

    /**
     * 开始录屏
     */
    private void startRecord() {
        isRecording = true;
        isScreenShoot = false;
        if (screenRecorder == null) {
            screenRecorder = new ScreenRecorder(this);
        }
        startActivityForResult(screenRecorder.mediaProjectionManager.createScreenCaptureIntent(), 1);
    }

    /**
     * 结束录屏
     */
    private void stopRecord() {
        screenRecorder.quit();
        screenRecorder = null;
    }

    /**
     * 实现播放暂停的切换，图标的切换
     * @param v 点击事件的View对象
     */
    private void switchControlButton(View v){
        if (isPlayer){
            //暂停
            Util.setBackImage(this, v, R.drawable.start);
            vlcPlayer.pause();
        }
        else{
            //开始
            Util.setBackImage(this, v, R.drawable.stop);
            vlcPlayer.play();
        }
        isPlayer = !isPlayer;
    }

    /**
     * 全屏/非全屏、横屏/竖屏的切换
     */
    private void switchScreenOrientation() {
        //显示按钮区
        setAreaVisibility(true);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //取消全屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //设置竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            isLand = false;
        }
        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //设置全屏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //设置横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            isLand = true;
        }
    }

    /**
     * 切换全屏按钮的图标
     * @param v 点击事件的View对象
     */
    private void switchFullImage(View v) {
        if (isLand) {
            Util.setBackImage(this, v, R.drawable.small);
        } else {
            Util.setBackImage(this, v, R.drawable.full);
        }
    }

    /**
     * 切换截图按钮的展示
     * @param v 点击事件的View对象
     */
    private void switchShowCamera(View v) {
        if (!Util.isLandscape(this)) {
            //竖屏
            return;
        }
        if (isShowCamera) {
            Util.setBackImage(this, v, R.drawable.camera);
            takePhotoView.setVisibility(View.INVISIBLE);
        }else {
            Util.setBackImage(this, v, R.drawable.camera_);
            takePhotoView.setVisibility(View.VISIBLE);
        }
        isShowCamera = !isShowCamera;
    }

    /**
     * 更改功能区组件布局
     * @param width     总体宽度
     * @param height    总体高度
     */
    private void setToolBarPosition(int width, int height) {
        /*//获取视频宽高
        int mWidth = player.getVideoWidth();
        int mHeight = player.getVideoHeight();
        }*/

        //设置功能区域宽高
        FrameLayout.LayoutParams areaLP = new FrameLayout.LayoutParams(width, height);
        buttonArea.setLayoutParams(areaLP);

        int dip30 = Util.dip2px(context, 30);
        int dip50 = Util.dip2px(context, 50);
        int dip20 = Util.dip2px(context, 20);
        int offset = Util.dip2px(context, 5);

        //设置中间按钮位置
        RelativeLayout centerArea = (RelativeLayout) findViewById(R.id.centerButtonArea);
        LinearLayout.LayoutParams centerAreaLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dip50);
        centerAreaLP.setMargins(0, height/2 - dip50/2, 0, 0);
        centerArea.setLayoutParams(centerAreaLP);

        //设置播放按钮位置
        RelativeLayout.LayoutParams controlLP = new RelativeLayout.LayoutParams(dip30, dip30);
        controlLP.addRule(RelativeLayout.CENTER_HORIZONTAL);
        controlLP.addRule(RelativeLayout.CENTER_VERTICAL);
        playerControl.setLayoutParams(controlLP);

        //设置截图按钮位置
        RelativeLayout.LayoutParams takephotoLP ;
        int takePhotoSideLength = Util.dip2px(context, 30);
        if (isLand)
            takePhotoSideLength = Util.dip2px(context, 50);
        takephotoLP = new RelativeLayout.LayoutParams(takePhotoSideLength, takePhotoSideLength);
        takephotoLP.addRule(RelativeLayout.CENTER_VERTICAL);
        takephotoLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        takephotoLP.setMargins(0, 0, offset * 2, 0);
        takePhotoView.setLayoutParams(takephotoLP);

        //竖屏下关闭截图按钮的显示
        if (!Util.isLandscape(this)) {
            Util.setBackImage(this, camera, R.drawable.camera);
            takePhotoView.setVisibility(View.INVISIBLE);
        }

        //设置工具条位置
        LinearLayout toolBar = (LinearLayout) findViewById(R.id.toolBar);
        LinearLayout.LayoutParams toolBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dip20);
        toolBarLP.setMargins(0, height/2-dip50/2-dip20-offset, 0, 0);
        toolBar.setLayoutParams(toolBarLP);

        //设置文件区是否显示
        LinearLayout fileArea = (LinearLayout) findViewById(R.id.fileArea);
        if (isLand)
            fileArea.setVisibility(View.INVISIBLE);
        else
            fileArea.setVisibility(View.VISIBLE);
    }

    /**
     * 切换按钮区的隐藏和显示
     */
    private void switchShowArea() {
        if (buttonArea.getVisibility() == View.VISIBLE) {
            //隐藏
            setAreaVisibility(false);
        } else if (buttonArea.getVisibility() == View.INVISIBLE) {
            //显示
            setAreaVisibility(true);
        }
    }

    /**
     * 设置按钮区是否可见
     * @param isVisibility  是否可见
     */
    private void setAreaVisibility(boolean isVisibility) {
        if (isVisibility) {
            buttonArea.setVisibility(View.VISIBLE);
            removeHiddenThread();
            addHiddenThread();
        } else {
            buttonArea.setVisibility(View.INVISIBLE);
            removeHiddenThread();
        }
    }

    /**
     * 启动隐藏按钮区的线程
     */
    private void addHiddenThread() {
        if (hiddenRunnable == null)
            hiddenRunnable = new HiddenRunnable();
        hiddenHandler.postDelayed(hiddenRunnable, 5000);
    }

    /**
     * 取消隐藏按钮区的线程
     */
    private void removeHiddenThread() {
        if (hiddenRunnable == null)
            return;
        hiddenHandler.removeCallbacks(hiddenRunnable);
        hiddenRunnable = null;
    }

    /**
     * 隐藏按钮区的线程
     */
    class HiddenRunnable implements Runnable {
        @Override
        public void run() {
            if (buttonArea.getVisibility() == View.VISIBLE) {
                //当处于锁屏且横屏的状态下，不隐藏按钮区
                if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) && (keyguardManager.inKeyguardRestrictedInputMode()))
                    return;
                buttonArea.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * 用于获取异步截图的Handler
     */
    class PhotoHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {
                //获取到截取的图片
                Bitmap bitmap = msg.getData().getParcelable("photo");
                screenShooter = null;
                isScreenShoot = false;
            }
        }
    }
}