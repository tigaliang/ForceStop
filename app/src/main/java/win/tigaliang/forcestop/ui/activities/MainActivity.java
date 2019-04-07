package win.tigaliang.forcestop.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import win.tigaliang.forcestop.services.FSAccessibilityService;
import win.tigaliang.forcestop.R;
import win.tigaliang.forcestop.utils.AppAnalyzer;
import win.tigaliang.forcestop.utils.AppBean;
import win.tigaliang.forcestop.ui.adapters.AppListAdapter;
import win.tigaliang.forcestop.utils.BlackListManager;
import win.tigaliang.forcestop.base.HandlerActivity;
import win.tigaliang.forcestop.base.SingleTaskThread;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tigaliang on 2016/10/3.
 * <p>
 * MainActivity:
 */

public class MainActivity extends HandlerActivity implements View.OnClickListener, FSAccessibilityService.FSClient {
    private static final int MSG_ANALYSE_RUNNING_APP_FINISH = 1;
    private static final int MSG_FORCE_STOP_APPS = 2;
    private static final int MSG_RETURN_TO_MAIN_ACTIVITY = 3;

    private ProgressDialog mLoadingDialog;
    private AppListAdapter mListViewAdapter;
    private List<AppBean> mRunningBlackApp;
    private boolean isSettingsActivityOnForceStopMode = false;

    private final SingleTaskThread mAnalysisRunningAppsThread = new SingleTaskThread() {
        @Override
        public void run() {
            try {
                Context context = MainActivity.this;
                List<AppBean> runningApps = AppAnalyzer.getRunningApps(context);
                String[] blackList = BlackListManager.getBlackList(context);
                if (mRunningBlackApp == null) {
                    mRunningBlackApp = new ArrayList<>();
                } else {
                    mRunningBlackApp.clear();
                }

                if (runningApps != null && !runningApps.isEmpty()) {
                    for (int i = 0; i < runningApps.size(); i++) {
                        AppBean hold = runningApps.get(i);
                        if (hold == null || TextUtils.isEmpty(hold.pkgName)) {
                            continue;
                        }
                        AppBean comp;
                        for (int j = runningApps.size() - 1; j > i; j--) {
                            comp = runningApps.get(j);
                            if (hold.pkgName.equals(comp.pkgName)) {
                                ++hold.number;
                                runningApps.remove(j);
                            }
                        }
                    }

                    for (String pkg : blackList) {
                        if (TextUtils.isEmpty(pkg)) {
                            continue;
                        }
                        for (AppBean app : runningApps) {
                            if (app == null) {
                                continue;
                            }
                            if (pkg.equals(app.pkgName)) {
                                mRunningBlackApp.add(app);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                sendMessageToMainLooper(MSG_ANALYSE_RUNNING_APP_FINISH);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        analyseRunningApp();
        FSAccessibilityService.setClient(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FSAccessibilityService.removeClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListViewAdapter != null) {
            mListViewAdapter.notifyDataSetChanged();
        }
    }

    private void initView() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.analysing));
        mListViewAdapter = new AppListAdapter(this, false, true);
        ListView appListView = (ListView) findViewById(R.id.list_view_app);
        if (appListView != null) {
            appListView.setCacheColorHint(Color.TRANSPARENT);
            appListView.setAdapter(mListViewAdapter);
        }

        View btnLetsGo = findViewById(R.id.btn_lets_go);
        if (btnLetsGo != null) {
            btnLetsGo.setOnClickListener(this);
        }
        View btnRefresh = findViewById(R.id.btn_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(this);
        }
    }

    private void analyseRunningApp() {
        mLoadingDialog.show();
        mAnalysisRunningAppsThread.start();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ANALYSE_RUNNING_APP_FINISH:
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
                if (mRunningBlackApp != null) {
                    mListViewAdapter.setData(mRunningBlackApp);
                    mListViewAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                break;
            case MSG_FORCE_STOP_APPS:
                if (mRunningBlackApp != null && !mRunningBlackApp.isEmpty()) {
                    AppBean app = mRunningBlackApp.remove(0);
                    if (app != null && !TextUtils.isEmpty(app.pkgName)) {
                        forceStopByPackageName(app.pkgName);
                    }
                }
                break;
            case MSG_RETURN_TO_MAIN_ACTIVITY:
                startActivity(new Intent(this, MainActivity.class));
                if (mRunningBlackApp != null && !mRunningBlackApp.isEmpty()) {
                    sendMessageToMainLooper(MSG_FORCE_STOP_APPS);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_black_list:
                startActivity(new Intent(this, BlackListActivity.class));
                break;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_lets_go:
                startForceStoppingRunningApps();
                break;
            case R.id.btn_refresh:
                analyseRunningApp();
                break;
            default:
                break;
        }
    }

    private void startForceStoppingRunningApps() {
        if (mRunningBlackApp == null || mRunningBlackApp.isEmpty()) {
            return;
        }
        sendMessageToMainLooper(MSG_FORCE_STOP_APPS);
    }

    private void forceStopByPackageName(String pkgName) {
        isSettingsActivityOnForceStopMode = true;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.parse("package:" + pkgName);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public boolean enabled() {
        return isSettingsActivityOnForceStopMode;
    }

    @Override
    public void onAppStopped() {
        isSettingsActivityOnForceStopMode = false;
        sendMessageToMainLooper(MSG_RETURN_TO_MAIN_ACTIVITY);
    }
}
