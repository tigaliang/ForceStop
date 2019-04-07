package win.tigaliang.forcestop.base;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by tigaliang on 2016/10/3.
 * <p>
 * HandlerActivity:
 */

public abstract class HandlerActivity extends AppCompatActivity {
    private Handler mHandler;

    private static final class MyHandler extends Handler {
        private WeakReference<HandlerActivity> mPageRef;

        MyHandler(HandlerActivity page) {
            super(Looper.getMainLooper());
            mPageRef = new WeakReference<>(page);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mPageRef != null) {
                HandlerActivity activity = mPageRef.get();
                if (activity != null) {
                    activity.handleMessage(msg);
                }
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mHandler = new MyHandler(this);
    }

    public abstract void handleMessage(Message msg);

    public void sendMessageToMainLooper(int what) {
        mHandler.sendEmptyMessage(what);
    }

    public void sendMessageToMainLooper(int what, Object obj) {
        mHandler.sendMessage(mHandler.obtainMessage(what, obj));
    }

    public void sendMessageToMainLooperDelay(int what, long delayMillis) {
        mHandler.sendEmptyMessageDelayed(what, delayMillis);
    }

    public void sendMessageToMainLooperDelay(int what, Object obj, long delayMillis) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(what, obj), delayMillis);
    }
}
