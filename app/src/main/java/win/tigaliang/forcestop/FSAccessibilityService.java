package win.tigaliang.forcestop;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by tigaliang on 2016/10/4.
 * <p>
 * FSAccessibilityService:
 */

public class FSAccessibilityService extends AccessibilityService {
    private static final String TAG = "FSAccessibilityService";
    private static final String COM_ANDROID_SETTINGS = "com.android.settings";
    private static final String ANDROID_WIDGET_BUTTON = "android.widget.Button";

    private static final Object CALLBACK_ACCESS_LOCK = new Object();

    private static WeakReference<FSClient> sCallback;

    public static void setClient(FSClient callback) {
        if (callback == null) {
            return;
        }
        synchronized (CALLBACK_ACCESS_LOCK) {
            sCallback = new WeakReference<FSClient>(callback);
        }
    }

    public static void removeClient() {
        synchronized (CALLBACK_ACCESS_LOCK) {
            sCallback.clear();
            sCallback = null;
        }
    }

    private FSClient getClient() {
        synchronized (CALLBACK_ACCESS_LOCK) {
            if (sCallback == null) {
                return null;
            }
        }
        return sCallback.get();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent:" + event.getPackageName() + "," + event.getEventType());
        FSClient client = getClient();
        if (client != null && client.enabled()) {
            processAccessibilityEvent(event);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void processAccessibilityEvent(AccessibilityEvent event) {
        if (COM_ANDROID_SETTINGS.equals(event.getPackageName())) {
            performForceStop(event);
        }
    }

    private void performForceStop(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return;
        }

        String textForceStop = getString(R.string.app_details_settings_force_stop);
        performClickButtonByText(source, textForceStop);
        String textOk = getString(R.string.app_details_settings_ok);
        boolean flagStep2 = performClickButtonByText(source, textOk);
        if (flagStep2) {
            FSClient client = getClient();
            if (client != null && client.enabled()) {
                client.onAppStopped();
            }
        }

    }

    private boolean performClickButtonByText(AccessibilityNodeInfo source, String text) {
        Log.d(TAG, "try to perform click on button by text:" + text);
        List<AccessibilityNodeInfo> forceStopNodes = source.findAccessibilityNodeInfosByText(text);
        if (forceStopNodes != null && !forceStopNodes.isEmpty()) {
            for (int i = 0; i < forceStopNodes.size(); i++) {
                AccessibilityNodeInfo node = forceStopNodes.get(i);
                if (node == null) {
                    continue;
                }
                if (ANDROID_WIDGET_BUTTON.equals(node.getClassName())) {
                    if (node.isEnabled()) {
                        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }
        return false;
    }

    public interface FSClient {
        boolean enabled();

        void onAppStopped();
    }
}
