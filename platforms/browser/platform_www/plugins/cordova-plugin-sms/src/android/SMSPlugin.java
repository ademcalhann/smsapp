package com.rjfun.cordova.sms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSPlugin
extends CordovaPlugin {
    private static final String LOGTAG = "SMSPlugin";
    
    public static final String ACTION_SET_OPTIONS = "setOptions";

    private static final String ACTION_SEND_SMS = "sendSMS";
    
    public static final String OPT_LICENSE = "license";
    private static final String SEND_SMS_ACTION = "SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String SMS_EXTRA_NAME = "pdus";
    
    public static final String SMS_URI_ALL = "content://sms/";
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    public static final String SMS_URI_SEND = "content://sms/sent";
    public static final String SMS_URI_DRAFT = "content://sms/draft";
    public static final String SMS_URI_OUTBOX = "content://sms/outbox";
    public static final String SMS_URI_FAILED = "content://sms/failed";
    public static final String SMS_URI_QUEUED = "content://sms/queued";
    
    public static final String BOX = "box";
    public static final String ADDRESS = "address";
    public static final String BODY = "body";
    public static final String READ = "read";
    public static final String SEEN = "seen";
    public static final String SUBJECT = "subject";
    public static final String SERVICE_CENTER = "service_center";
    public static final String DATE = "date";
    public static final String DATE_SENT = "date_sent";
    public static final String STATUS = "status";
    public static final String REPLY_PATH_PRESENT = "reply_path_present";
    public static final String TYPE = "type";
    public static final String PROTOCOL = "protocol";
    
    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;
    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_READ = 1;
    public static final int MESSAGE_IS_NOT_SEEN = 0;
    public static final int MESSAGE_IS_SEEN = 1;
    
    private static final String SMS_GENERAL_ERROR = "SMS_GENERAL_ERROR";
    private static final String NO_SMS_SERVICE_AVAILABLE = "NO_SMS_SERVICE_AVAILABLE";
    private static final String SMS_FEATURE_NOT_SUPPORTED = "SMS_FEATURE_NOT_SUPPORTED";
    private static final String SENDING_SMS_ID = "SENDING_SMS";
    
    private ContentObserver mObserver = null;
    private BroadcastReceiver mReceiver = null;
    private boolean mIntercept = false;
    private String lastFrom = "";
    private String lastContent = "";

    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_SET_OPTIONS.equals(action)) {
            JSONObject options = inputs.optJSONObject(0);
            this.setOptions(options);
            result = new PluginResult(PluginResult.Status.OK);
        } else if (ACTION_SEND_SMS.equals(action)) {
            JSONArray addressList = inputs.optJSONArray(0);
            String message = inputs.optString(1);
            result = this.sendSMS(addressList, message, callbackContext);
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(PluginResult.Status.INVALID_ACTION);
        }
        if (result != null) {
            callbackContext.sendPluginResult(result);
        }
        return true;
    }

   

    public void setOptions(JSONObject options) {
        Log.d(LOGTAG, ACTION_SET_OPTIONS);
    }

    protected String __getProductShortName() {
        return "SMS";
    }

    public final String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; ++i) {
                String h = Integer.toHexString(255 & messageDigest[i]);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException digest) {
            return "";
        }
    }

  

 
  

    private PluginResult sendSMS(JSONArray addressList, String text, CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_SEND_SMS);
        if (this.cordova.getActivity().getPackageManager().hasSystemFeature("android.hardware.telephony")) {
            int n;
            if ((n = addressList.length()) > 0) {
                PendingIntent sentIntent = PendingIntent.getBroadcast((Context)this.cordova.getActivity(), (int)0, (Intent)new Intent("SENDING_SMS"), (int)0);
                SmsManager sms = SmsManager.getDefault();
                for (int i = 0; i < n; ++i) {
                    String address;
                    if ((address = addressList.optString(i)).length() <= 0) continue;
                    sms.sendTextMessage(address, null, text, sentIntent, (PendingIntent)null);
                }
            } else {
                PendingIntent sentIntent = PendingIntent.getActivity((Context)this.cordova.getActivity(), (int)0, (Intent)new Intent("android.intent.action.VIEW"), (int)0);
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.putExtra("sms_body", text);
                intent.setType("vnd.android-dir/mms-sms");
                try {
                    sentIntent.send(this.cordova.getActivity().getApplicationContext(), 0, intent);
                }
                catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "OK"));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "SMS is not supported"));
        }
        return null;
    }

   

    private JSONObject getJsonFromCursor(Cursor cur) {
		JSONObject json = new JSONObject();
		
		int nCol = cur.getColumnCount();
		String keys[] = cur.getColumnNames();

		try {
			for(int j=0; j<nCol; j++) {
				switch(cur.getType(j)) {
				case Cursor.FIELD_TYPE_NULL:
					json.put(keys[j], null);
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					json.put(keys[j], cur.getLong(j));
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					json.put(keys[j], cur.getFloat(j));
					break;
				case Cursor.FIELD_TYPE_STRING:
					json.put(keys[j], cur.getString(j));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					json.put(keys[j], cur.getBlob(j));
					break;
				}
			}
		} catch (Exception e) {
			return null;
		}

		return json;
    }

    private void fireEvent(final String event, JSONObject json) {
    	final String str = json.toString();
    	Log.d(LOGTAG, "Event: " + event + ", " + str);
    	
        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
            	String js = String.format("javascript:cordova.fireDocumentEvent(\"%s\", {\"data\":%s});", event, str);
            	webView.loadUrl( js );
            }
        });
    }
    
   

   
    protected void createContentObserver() {
        Activity ctx = this.cordova.getActivity();
        this.mObserver = new ContentObserver(new Handler()){

            public void onChange(boolean selfChange) {
                this.onChange(selfChange, null);
            }

            public void onChange(boolean selfChange, Uri uri) {
                ContentResolver resolver = cordova.getActivity().getContentResolver(); 
                Log.d(LOGTAG, ("onChange, selfChange: " + selfChange + ", uri: " + (Object)uri));
                int id = -1;
                String str;
                if (uri != null && (str = uri.toString()).startsWith(SMS_URI_ALL)) {
                    try {
                        id = Integer.parseInt(str.substring(SMS_URI_ALL.length()));
                        Log.d(LOGTAG, ("sms id: " + id));
                    }
                    catch (NumberFormatException var6_6) {
                        // empty catch block
                    }
                }
                if (id == -1) {
                    uri = Uri.parse(SMS_URI_INBOX);
                }
                Cursor cur = resolver.query(uri, null, null, null, "_id desc");
                if (cur != null) {
                    int n = cur.getCount();
                    Log.d(LOGTAG, ("n = " + n));
                    if (n > 0 && cur.moveToFirst()) {
                        JSONObject json;
                        if ((json = SMSPlugin.this.getJsonFromCursor(cur)) != null) {
                            onSMSArrive(json);
                        } else {
                            Log.d(LOGTAG, "fetch record return null");
                        }
                    }
                    cur.close();
                }
            }
        };
        ctx.getContentResolver().registerContentObserver(Uri.parse(SMS_URI_INBOX), true, this.mObserver);
        Log.d(LOGTAG, "sms inbox observer registered");
    }

   

    private JSONObject getJsonFromSmsMessage(SmsMessage sms) {
    	JSONObject json = new JSONObject();
    	
        try {
        	json.put( ADDRESS, sms.getOriginatingAddress() );
        	json.put( BODY, sms.getMessageBody() ); // May need sms.getMessageBody.toString()
        	json.put( DATE_SENT, sms.getTimestampMillis() );
        	json.put( DATE, System.currentTimeMillis() );
        	json.put( READ, MESSAGE_IS_NOT_READ );
        	json.put( SEEN, MESSAGE_IS_NOT_SEEN );
        	json.put( STATUS, sms.getStatus() );
        	json.put( TYPE, MESSAGE_TYPE_INBOX );
        	json.put( SERVICE_CENTER, sms.getServiceCenterAddress());
        	
        } catch ( Exception e ) { 
            e.printStackTrace(); 
        }

    	return json;
    }
    
    private ContentValues getContentValuesFromJson(JSONObject json) {
    	ContentValues values = new ContentValues();
    	values.put( ADDRESS, json.optString(ADDRESS) );
    	values.put( BODY, json.optString(BODY));
    	values.put( DATE_SENT,  json.optLong(DATE_SENT));
    	values.put( READ, json.optInt(READ));
    	values.put( SEEN, json.optInt(SEEN));
    	values.put( TYPE, json.optInt(TYPE) );
    	values.put( SERVICE_CENTER, json.optString(SERVICE_CENTER));
    	return values;
    }
  