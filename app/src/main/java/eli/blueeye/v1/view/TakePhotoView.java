package eli.blueeye.v1.view;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import eli.blueeye.v1.R;
import eli.blueeye.v1.inter.LongTouchListener;

/**
 * 截屏和录屏的按钮，实现单击和长按不同的接口(单击截图， 长按录屏)
 *
 * @author eli chang
 */
public class TakePhotoView extends View {

    private static final String TAG = "styleable_take_photo";

    //触摸标记
    private boolean isTouch = false;
    //延时间隔
    private int eLongTouchTime;
    //长按的回调接口
    private LongTouchListener eLongTouchListener;

    //定义画笔
    private Paint paint;
    //背景色
    private int eBackColor;
    //透明度
    private int eAlpha = 150;

    //插值器
    private static final TimeInterpolator interpolator = new DecelerateInterpolator();
    //透明度
    private static final float scale = 0.9f;
    //延时
    private static final int duration = 150;
    //点击
    private boolean clickAble = false;

    public TakePhotoView(Context context) {
        this(context, null);
    }

    public TakePhotoView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TakePhotoView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.styleable_take_photo, defStyleAttr, 0);
        eBackColor = ta.getColor(0, Color.WHITE);
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int width = Math.min(getMeasuredHeight(), getMeasuredWidth());
        int paintWidth = width / 25;

        //绘制外环
        paint.setColor(eBackColor);
        paint.setAlpha(eAlpha);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paintWidth);
        canvas.drawCircle(width / 2, width / 2, width / 2 - paintWidth * 2, paint);

        //绘制内圆
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width / 2, width / 2, width / 2 - paintWidth * 3, paint);
    }

    /**
     * 设置为可见状态
     */
    public void setVisible() {
        this.eAlpha = 150;
        this.setClickable(true);
        this.clickAble = true;
        postInvalidate();
    }

    /**
     * 设置为不可见状态
     */
    public void setInVisible() {
        this.eAlpha = 0;
        this.setClickable(false);
        this.clickAble = false;
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //当组件被设置为不可点击，则退出
        if (!clickAble)
            return true;

        //按下按钮
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            /**
             * 背景颜色设为红色
             * 不透明度设为255
             */
            eBackColor = Color.RED;
            eAlpha = 255;
            this.animate().scaleX(scale).scaleY(scale).setDuration(duration).setInterpolator(interpolator);
            postInvalidate();

            isTouch = true;
            new LongTouchTask().execute();
        }
        //抬起按钮
        else if (event.getAction() == MotionEvent.ACTION_UP) {

            /**
             * 背景颜色设为红色
             * 不透明度设为150
             */
            eBackColor = Color.WHITE;
            eAlpha = 150;
            this.animate().scaleX(1).scaleY(1).setDuration(duration).setInterpolator(interpolator);
            postInvalidate();

            isTouch = false;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 取消触摸状态
     */
    public void cancelTouchState() {
        /**
         * 背景颜色设为红色
         * 不透明度设为150
         */
        eBackColor = Color.WHITE;
        eAlpha = 150;
        this.animate().scaleX(1).scaleY(1).setDuration(duration).setInterpolator(interpolator);
        postInvalidate();
        isTouch = false;
        if (eLongTouchListener != null) {
            eLongTouchListener.onTouchStop();
        }
    }

    /**
     * @param listener 监听器
     * @param time     回调的时间间隔
     */
    public void setOnLongTouchListener(LongTouchListener listener, int time) {
        this.eLongTouchListener = listener;
        this.eLongTouchTime = time;
    }

    /**
     * 长按的异步处理器
     */
    class LongTouchTask extends AsyncTask<Void, Integer, Void> {

        /**
         * 异步任务
         * <p>
         * 1.当处于触摸状态下，会每隔1000毫秒的时间回调onProgressUpdate
         * onProgressUpdate中调用onLongTouch方法，触发长按事件
         * <p>
         * 2.首次触发事件时，会延时一个自定义的时间间隔，
         * 用来区分长按和点击
         *
         * @param params
         * @return
         */
        @Override
        protected Void doInBackground(Void... params) {
            boolean isFirst = true;

            while (isTouch) {
                if (isFirst) {
                    sleep(eLongTouchTime);
                    isFirst = false;
                } else
                    sleep(1000);
                if (isTouch)
                    publishProgress(0);
            }
            return null;
        }

        /**
         * 触摸事件结束后，调用onTouchStop()方法，触发长按结束事件
         *
         * @param aVoid
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            if (eLongTouchListener != null) {
                eLongTouchListener.onTouchStop();
            }
        }

        /**
         * 调用onLongTouch(),触发长按事件
         *
         * @param values
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (eLongTouchListener != null)
                eLongTouchListener.onLongTouch();
        }

        /**
         * 延时
         *
         * @param time 需要延时的时间
         */
        private void sleep(int time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {

            }
        }
    }
}