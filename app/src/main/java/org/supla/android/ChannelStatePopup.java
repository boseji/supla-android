package org.supla.android;

/*
 Copyright (C) AC SOFTWARE SP. Z O.O.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.supla.android.db.Channel;
import org.supla.android.db.DbHelper;
import org.supla.android.lib.SuplaChannelState;
import org.supla.android.lib.SuplaClient;
import org.supla.android.lib.SuplaConst;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class ChannelStatePopup implements DialogInterface.OnCancelListener {

    private final int REFRESH_INTERVAL_MS = 6000;

    private boolean refreshPossible;
    private long lastRefreshTime;
    private Timer refreshTimer;
    private AlertDialog alertDialog;
    private int remoteId;
    private Context context;

    private LinearLayout llIP;
    private LinearLayout llMAC;
    private LinearLayout llBatteryLevel;
    private LinearLayout llBatteryPowered;
    private LinearLayout llWiFiRSSI;
    private LinearLayout llWiFiSignalStrength;
    private LinearLayout llBridgeNodeOnline;
    private LinearLayout llBridgeNodeSignalStrength;
    private LinearLayout llUptime;
    private LinearLayout llConnectionUptime;
    private LinearLayout llBatteryHealth;
    private LinearLayout llLastConnectionResetCause;
    private LinearLayout llLightSourceHealth;
    private LinearLayout llProgress;

    private TextView tvIP;
    private TextView tvMAC;
    private TextView tvBatteryLevel;
    private TextView tvBatteryPowered;
    private TextView tvWiFiRSSI;
    private TextView tvWiFiSignalStrength;
    private TextView tvBridgeNodeOnline;
    private TextView tvBridgeNodeSignalStrength;
    private TextView tvUptime;
    private TextView tvConnectionUptime;
    private TextView tvBatteryHealth;
    private TextView tvLastConnectionResetCause;
    private TextView tvLightSourceHealth;

    public ChannelStatePopup(Context context) {
        this.context = context;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.channelstate, null);
        builder.setView(view);

        llIP = view.findViewById(R.id.llIP);
        llMAC = view.findViewById(R.id.llMAC);
        llBatteryLevel = view.findViewById(R.id.llBatteryLevel);
        llBatteryPowered = view.findViewById(R.id.llBatteryPowered);
        llWiFiRSSI = view.findViewById(R.id.llWiFiRSSI);
        llWiFiSignalStrength = view.findViewById(R.id.llWiFiSignalStrength);
        llBridgeNodeOnline = view.findViewById(R.id.llBridgeNodeOnline);
        llBridgeNodeSignalStrength = view.findViewById(R.id.llBridgeNodeSignalStrength);
        llUptime = view.findViewById(R.id.llUptime);
        llConnectionUptime = view.findViewById(R.id.llConnectionUptime);
        llBatteryHealth = view.findViewById(R.id.llBatteryHealth);
        llLastConnectionResetCause = view.findViewById(R.id.llLastConnectionResetCause);
        llLightSourceHealth = view.findViewById(R.id.llLightSourceHealth);
        llProgress = view.findViewById(R.id.llProgress);

        tvIP = view.findViewById(R.id.tvIP);
        tvMAC = view.findViewById(R.id.tvMAC);
        tvBatteryLevel = view.findViewById(R.id.tvBatteryLevel);
        tvBatteryPowered = view.findViewById(R.id.tvBatteryPowered);
        tvWiFiRSSI = view.findViewById(R.id.tvWiFiRSSI);
        tvWiFiSignalStrength = view.findViewById(R.id.tvWiFiSignalStrength);
        tvBridgeNodeOnline = view.findViewById(R.id.tvBridgeNodeOnline);
        tvBridgeNodeSignalStrength = view.findViewById(R.id.tvBridgeNodeSignalStrength);
        tvUptime = view.findViewById(R.id.tvUptime);
        tvConnectionUptime = view.findViewById(R.id.tvConnectionUptime);
        tvBatteryHealth = view.findViewById(R.id.tvBatteryHealth);
        tvLastConnectionResetCause = view.findViewById(R.id.tvLastConnectionResetCause);
        tvLightSourceHealth = view.findViewById(R.id.tvLightSourceHealth);


        alertDialog = builder.create();
        alertDialog.setOnCancelListener(this);
    }

    public void show(int remoteId) {
        update(remoteId);
        alertDialog.show();
        startRefreshTimer();
    }

    public boolean isVisible() {
        return alertDialog.isShowing();
    }

    public int getRemoteId() {
        return remoteId;
    }

    public void update(SuplaChannelState state) {

        llIP.setVisibility(View.GONE);
        llMAC.setVisibility(View.GONE);
        llBatteryLevel.setVisibility(View.GONE);
        llBatteryPowered .setVisibility(View.GONE);
        llWiFiRSSI.setVisibility(View.GONE);
        llWiFiSignalStrength.setVisibility(View.GONE);
        llBridgeNodeOnline.setVisibility(View.GONE);
        llBridgeNodeSignalStrength.setVisibility(View.GONE);
        llUptime.setVisibility(View.GONE);
        llConnectionUptime.setVisibility(View.GONE);
        llBatteryHealth.setVisibility(View.GONE);
        llLastConnectionResetCause.setVisibility(View.GONE);
        llLightSourceHealth.setVisibility(View.GONE);
        llProgress.setVisibility(View.VISIBLE);

        if (state == null) {
            return;
        }

        lastRefreshTime = System.currentTimeMillis();
        startRefreshTimer();

        if (state.getIpv4() != null) {
            llIP.setVisibility(View.VISIBLE);
            llProgress.setVisibility(View.GONE);
            tvIP.setText(state.getIpv4());
        }
        Trace.d("AAA", state.getLightSourceHealthLeft().toString());
    }


    public void update(int remoteId) {
        this.remoteId = remoteId;
        DbHelper dbHelper = new DbHelper(context);
        Channel channel = dbHelper.getChannel(remoteId);
        refreshPossible = false;

        if (channel != null) {
            if ((channel.getFlags() & SuplaConst.SUPLA_CHANNEL_FLAG_CHANNELSTATE) > 0) {
                refreshPossible = true;
            }

            if (channel.getExtendedValue() != null
                    && (channel.getExtendedValue().getType() == SuplaConst.EV_TYPE_CHANNEL_STATE_V1
                    || channel.getExtendedValue().getType()
                    == SuplaConst.EV_TYPE_CHANNEL_AND_TIMER_STATE_V1)) {

                update(channel.getExtendedValue().getExtendedValue().ChannelStateValue);
            }
        }
    }

    private void startRefreshTimer() {
        cancelRefreshTimer();

        if (!refreshPossible) {
            return;
        }

        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (context instanceof Activity) {
                    ((Activity)context).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (System.currentTimeMillis()- lastRefreshTime >= REFRESH_INTERVAL_MS) {
                                cancelRefreshTimer();

                                SuplaClient client = SuplaApp.getApp().getSuplaClient();

                                if (client != null) {
                                    client.getChannelState(remoteId);
                                }
                            }
                        }
                    });
                }
            }
        }, 0, 500);
    }

    private void cancelRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancelRefreshTimer();
    }
}
;