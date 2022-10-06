package com.ishara11rathnayake.smsautoread;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class echoes a string called from JavaScript.
 */
public class SMSAutoRead extends CordovaPlugin {

    BroadcastReceiver smsBroadcastReceiver;
    private static final int REQ_USER_CONSENT = 200;
    private CallbackContext callback = null;
    private CordovaPlugin plugin = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callback = callbackContext;
        this.plugin = this;
        switch (action) {
            case "start":
                this.start();
                return true;
            case "startWatching":
                this.startWatching();
                return true;
            case "stop":
                this.stop();
                return true;
        }
        return false;
    }

    private void start() {
        if (smsBroadcastReceiver == null) {
            smsBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SmsRetriever.SMS_RETRIEVED_ACTION)) {
                        Bundle extras = intent.getExtras();

                        Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                        switch (smsRetrieverStatus.getStatusCode()) {
                            case CommonStatusCodes.SUCCESS:
                                if (cordova.getActivity().getPackageName().contains("com.directfn")) {
                                    Intent messageIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);

                                    ComponentName name = messageIntent.resolveActivity(cordova.getActivity().getPackageManager());
                                    
                                    System.out.println("THis is comp nameeeeeeeee:::::: "+ name);
                                    System.out.println("THis is class nameeeeeeeee:::::: "+ name.getClassName());
                                    System.out.println("THis is packagename:::::: "+ name.getPackageName());

                                    if (name.getPackageName().contains("com.directfn") && name.getClassName().equals("SMSAutoRead")) {
                                        cordova.startActivityForResult(plugin, messageIntent, REQ_USER_CONSENT);
                                    }
                                }
                                break;
                            case CommonStatusCodes.TIMEOUT:
                                break;
                        }
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
            cordova.getActivity().getApplicationContext().registerReceiver(smsBroadcastReceiver, intentFilter);
        }
    }

    private void startWatching() {
        SmsRetrieverClient client = SmsRetriever.getClient(cordova.getActivity().getApplicationContext());
        client.startSmsUserConsent(null);
    }

    private void stop() {
        cordova.getActivity().getApplicationContext().unregisterReceiver(smsBroadcastReceiver);
        this.smsBroadcastReceiver = null;
    }

    private String getOtpFromMessage(String message) {
        String mask = "[0-9]+";
        Pattern otpPattern = Pattern.compile(mask);
        Matcher matcher = otpPattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(0);
        }

        return "Not Found";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQ_USER_CONSENT) {
            if ((resultCode == Activity.RESULT_OK) && (intent != null)) {
                String message = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                String otpMsg = getOtpFromMessage(message);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, otpMsg);
                callback.sendPluginResult(pluginResult);
            } else {
                this.startWatching();
            }
        }
    }
}
