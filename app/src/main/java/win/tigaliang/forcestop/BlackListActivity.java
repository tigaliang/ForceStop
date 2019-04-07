package win.tigaliang.forcestop;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import win.tigaliang.forcestop.staffs.AppAnalyzer;
import win.tigaliang.forcestop.staffs.AppBean;
import win.tigaliang.forcestop.staffs.AppListAdapter;
import win.tigaliang.forcestop.staffs.BlackListDao;
import win.tigaliang.forcestop.staffs.HandlerActivity;
import win.tigaliang.forcestop.staffs.SingleTaskThread;

/**
 * Created by tigaliang on 2016/10/3.
 * <p>
 * BlackListActivity
 */

public class BlackListActivity extends HandlerActivity {
    private static final int MSG_BUILD_APP_LIST_FINISH = 0x0001;

    private List<AppBean> mInstalledApps;
    private ProgressDialog mLoadingDialog;
    private AppListAdapter mListViewAdapter;

    private final SingleTaskThread mBuildAppListThread = new SingleTaskThread() {
        @Override
        public void run() {
            try {
                mInstalledApps = AppAnalyzer.getInstalledApps(BlackListActivity.this);
                filterCurrentApp(mInstalledApps);
                setSelections(mInstalledApps);
                Collections.sort(mInstalledApps, new Comparator<AppBean>() {
                    @Override
                    public int compare(AppBean lhs, AppBean rhs) {
                        if (lhs == null || rhs == null) {
                            return -1;
                        }
                        if (lhs.isSelected && !rhs.isSelected) {
                            return -1;
                        } else if (!lhs.isSelected && rhs.isSelected) {
                            return 1;
                        }
                        // sort by first installation time
                        long installTimeLhs = getAppFirstInstallTime(lhs.pkgName);
                        long installTimeRhs = getAppFirstInstallTime(rhs.pkgName);
                        if (installTimeLhs > installTimeRhs) {
                            return -1;
                        } else if (installTimeLhs < installTimeRhs) {
                            return 1;
                        }
                        if (lhs.uid > rhs.uid) {
                            return -1;
                        } else if (lhs.uid < rhs.uid) {
                            return 1;
                        }
                        return 0;
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                sendMessageToMainLooper(MSG_BUILD_APP_LIST_FINISH);
            }
        }
    };

    private void filterCurrentApp(List<AppBean> list) {
        if (list == null) {
            return;
        }
        String currentPkg = getPackageName();
        if (TextUtils.isEmpty(currentPkg)) {
            return;
        }
        for (AppBean bean : list) {
            if (bean == null) {
                continue;
            }
            if (currentPkg.equals(bean.pkgName)) {
                list.remove(bean);
                return;
            }
        }
    }

    private void setSelections(List<AppBean> list) {
        if (list == null) {
            return;
        }
        String[] blackList = BlackListDao.getBlackList(this);
        if (blackList.length < 1) {
            return;
        }
        for (AppBean app : list) {
            if (app == null) {
                continue;
            }
            for (String pkg : blackList) {
                if (TextUtils.isEmpty(pkg)) {
                    continue;
                }
                if (pkg.equals(app.pkgName)) {
                    app.isSelected = true;
                    break;
                }
            }
        }
    }

    private void saveSelections() {
        if (mInstalledApps == null) {
            return;
        }

        List<String> selectedPackages = new ArrayList<>();
        for (AppBean app : mInstalledApps) {
            if (app == null || TextUtils.isEmpty(app.pkgName)) {
                continue;
            }
            if (app.isSelected) {
                selectedPackages.add(app.pkgName);
            }
        }
        Object[] tmp = selectedPackages.toArray();
        String[] result = new String[tmp.length];
        System.arraycopy(tmp, 0, result, 0, tmp.length);
        BlackListDao.setBlackList(this, result);
    }

    private long getAppFirstInstallTime(String pkgName) {
        try {
            return getPackageManager().getPackageInfo(pkgName, PackageManager.GET_META_DATA).firstInstallTime;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.action_black_list);
        }
        initView();
        buildAppList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSelections();
    }

    private void initView() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.analysing));
        mListViewAdapter = new AppListAdapter(this, true, false);
        ListView appListView = (ListView) findViewById(R.id.list_view_app);
        if (appListView != null) {
            appListView.setCacheColorHint(Color.TRANSPARENT);
            appListView.setAdapter(mListViewAdapter);
            appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AppBean app = mListViewAdapter.getItem(position);
                    if (app != null) {
                        app.isSelected = !app.isSelected;
                        mListViewAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void buildAppList() {
        mLoadingDialog.show();
        mBuildAppListThread.start();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_BUILD_APP_LIST_FINISH:
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
                if (mInstalledApps != null) {
                    mListViewAdapter.setData(mInstalledApps);
                    mListViewAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
