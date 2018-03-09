package com.babariviere.sms;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONObject;

import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

import static io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Created by babariviere on 08/03/18.
 */

class SmsReceiver implements StreamHandler, RequestPermissionsResultListener {
  private final Registrar registrar;
  private BroadcastReceiver receiver;
  private final Permissions permissions;
  private final String[] permissionsList = new String[] {Manifest.permission.RECEIVE_SMS};
  private EventSink sink;

  SmsReceiver(Registrar registrar) {
    this.registrar = registrar;
    this.permissions = new Permissions(registrar.activity());
    registrar.addRequestPermissionsResultListener(this);
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  public void onListen(Object arguments, EventSink events) {
    receiver = createSmsReceiver(events);
    registrar.context().registerReceiver(receiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    sink = events;
    permissions.checkAndRequestPermission(permissionsList, Permissions.RECV_SMS_ID_REQ);
  }

  @Override
  public void onCancel(Object o) {
    registrar.context().unregisterReceiver(receiver);
    receiver = null;
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private SmsMessage[] readMessages(Intent intent) {
    return Telephony.Sms.Intents.getMessagesFromIntent(intent);
  }


  private BroadcastReceiver createSmsReceiver(final EventSink events) {
    return new BroadcastReceiver() {
      @TargetApi(Build.VERSION_CODES.KITKAT)
      @Override
      public void onReceive(Context context, Intent intent) {
        try {
          SmsMessage[] msgs = readMessages(intent);
          if (msgs == null) {
            return;
          }
          for (SmsMessage msg: msgs) {
            JSONObject obj = new JSONObject();
            obj.put("address", msg.getOriginatingAddress());
            obj.put("body", msg.getMessageBody());
            events.success(obj);
          }
        } catch (Exception e) {
          Log.d("SmsReceiver", e.toString());
        }
      }
    };
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != Permissions.RECV_SMS_ID_REQ) {
      return false;
    }
    boolean isOk = true;
    for (int res: grantResults) {
      if (res != PackageManager.PERMISSION_GRANTED) {
        isOk = false;
        break;
      }
    }
    if (isOk) {
      return true;
    }
    sink.endOfStream();
    return false;
  }
}