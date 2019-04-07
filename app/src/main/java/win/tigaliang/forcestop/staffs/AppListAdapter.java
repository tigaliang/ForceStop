package win.tigaliang.forcestop.staffs;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import win.tigaliang.forcestop.R;

/**
 * Created by tigaliang on 2016/10/3.
 * <p>
 * AppListAdapter:
 */

public class AppListAdapter extends BaseAdapter {
    private Context mContext;
    private boolean isCheckBoxEnabled;
    private boolean isNumberEnabled;
    private List<AppBean> mData;

    public AppListAdapter(Context context, boolean isCheckBoxEnabled, boolean isNumberEnabled) {
        mContext = context;
        this.isCheckBoxEnabled = isCheckBoxEnabled;
        this.isNumberEnabled = isNumberEnabled;
    }

    public void setData(List<AppBean> data) {
        mData = data;
    }

    @Override
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public AppBean getItem(int position) {
        return position < 0 || position >= getCount() ? null : mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.lvi_app_check, null);
            vh = new ViewHolder();
            vh.icon = convertView.findViewById(R.id.view_icon);
            vh.appName = (TextView) convertView.findViewById(R.id.text_view_app_name);
            if (isCheckBoxEnabled) {
                vh.checkBox = (CheckBox) convertView.findViewById(R.id.check_box_select);
                vh.checkBox.setVisibility(View.VISIBLE);
            }
            if (isNumberEnabled) {
                vh.number = (TextView) convertView.findViewById(R.id.text_view_num);
                vh.number.setVisibility(View.VISIBLE);
            }
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        AppBean app = getItem(position);
        if (app == null) {
            vh.icon.setBackgroundColor(Color.GRAY);
            vh.appName.setText(mContext.getString(R.string.null_));
            if (isCheckBoxEnabled) {
                vh.checkBox.setChecked(false);
            }
            if (isNumberEnabled) {
                vh.number.setText(String.format(Locale.getDefault(),
                        mContext.getString(R.string.fmt_app_num), 0));
            }
        } else {
            if (app.icLauncher == null) {
                vh.icon.setBackgroundColor(Color.CYAN);
            } else {
                vh.icon.setBackground(app.icLauncher);
            }
            String name = String.format(Locale.getDefault(), "%1$s(%2$s)", app.appName, app.pkgName);
            vh.appName.setText(name);
            if (isCheckBoxEnabled) {
                vh.checkBox.setChecked(app.isSelected);
            }
            if (isNumberEnabled) {
                vh.number.setText(String.format(Locale.getDefault(),
                        mContext.getString(R.string.fmt_app_num), app.number));
            }
        }

        return convertView;
    }

    private class ViewHolder {
        View icon;
        TextView appName;
        CheckBox checkBox;
        TextView number;
    }
}
