package me.dawars.coffeetracker.setup.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.dawars.coffeetracker.R;
import me.dawars.coffeetracker.setup.ConnectActivity;

public class ConnectionAdapter extends BaseAdapter {

    List<ConnectActivity.WifiData> list = new ArrayList<>();

    /**
     * constructor for adapter
     */
    public ConnectionAdapter() {
    }

    private static boolean isSecure(int security) {
        return security != 0;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public ConnectActivity.WifiData getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        //Get View
        if (convertView == null) {
            /** get context from parent */
            Context context = parent.getContext();

            /** inflate child view from xml */
            convertView = LayoutInflater.from(context).inflate(R.layout.connect_wifi_item, null);
            holder = new ViewHolder();
            holder.tvSsid = (TextView) convertView.findViewById(R.id.connect_ssid);
            holder.ivSecurityStrength = (ImageView) convertView.findViewById(R.id.connect_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //Fill with data
        ConnectActivity.WifiData wifi = getItem(position);

        int icon = getIconFromStrength(Math.min(3, wifi.getStrength() / 25), isSecure(wifi.getSecutity()));

        holder.tvSsid.setText(wifi.getSsid());
        holder.ivSecurityStrength.setImageResource(icon);

        return convertView;
    }

    public void set(List<ConnectActivity.WifiData> wifiScanList) {
        this.list.clear();
        this.list.addAll(wifiScanList);
        notifyDataSetChanged();
    }

    private int getIconFromStrength(int level, boolean locked) {
        switch (level) {
            case 0:
                return locked ? R.drawable.ic_signal_wifi_1_bar_lock_black_24dp : R.drawable.ic_signal_wifi_1_bar_black_24dp;
            case 1:
                return locked ? R.drawable.ic_signal_wifi_2_bar_lock_black_24dp : R.drawable.ic_signal_wifi_2_bar_black_24dp;
            case 2:
                return locked ? R.drawable.ic_signal_wifi_3_bar_lock_black_24dp : R.drawable.ic_signal_wifi_3_bar_black_24dp;
            case 3:
                return locked ? R.drawable.ic_signal_wifi_4_bar_lock_black_24dp : R.drawable.ic_signal_wifi_4_bar_black_24dp;
            default:
                return -1;
        }
    }

    /**
     * static cache item view
     */
    static class ViewHolder {

        TextView tvSsid;
        ImageView ivSecurityStrength;

        /**
         * holder constructor
         */
        public ViewHolder() {
        }

    }

}