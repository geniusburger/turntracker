package me.geniusburger.turntracker.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import me.geniusburger.turntracker.utilities.DialogUtil;

public class TagReceiver extends BroadcastReceiver {

    private static final String TAG = TagReceiver.class.getSimpleName();

//        String packageName = MainActivity.class.getPackage().getName();
//        NdefRecord taskRecord = NdefRecord.createUri("http://geniusburger.me/task/123");
//        NdefRecord aarRecord = NdefRecord.createApplicationRecord(packageName);
//        NdefMessage msg = new NdefMessage( new NdefRecord[] { taskRecord, aarRecord });
//        Log.d(TAG, "nfc length " + msg.getByteArrayLength());

    public static Tag getTag(Intent intent) {
        return (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    public static boolean writeNfc(Context context, Tag tag, String text) {
        NdefMessage message = new NdefMessage(createTextRecord(text, Locale.US, true), createExtRecord(context));

        try {
            // If the tag is already formatted, just write the message to it
            Ndef ndef = Ndef.get(tag);
            if(ndef != null) {
                ndef.connect();

                // Make sure the tag is writable
                if(!ndef.isWritable()) {
                    DialogUtil.displayErrorDialog(context, "NFC Write Error", "Tag is not writable");
                    Log.e(TAG, "NFC write error, not writable");
                    return false;
                }

                // Check if there's enough space on the tag for the message
                int size = message.toByteArray().length;
                if(ndef.getMaxSize() < size) {
                    DialogUtil.displayErrorDialog(context, "NFC Write Error", "Not enough space");
                    Log.e(TAG, "NFC write error, not enough space");
                    return false;
                }

                try {
                    ndef.writeNdefMessage(message);
                    DialogUtil.displayInfoDialog(context, "Done writing");
                    return true;
                } catch (TagLostException e) {
                    DialogUtil.displayErrorDialog(context, "NFC Write Error", "Tag lost");
                    Log.e(TAG, "NFC write error", e);
                    return false;
                } catch (IOException | FormatException e) {
                    DialogUtil.displayErrorDialog(context, "NFC Write Error", "Failed to format");
                    Log.e(TAG, "NFC write error", e);
                    return false;
                }
                // If the tag is not formatted, format it with the message
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if(format != null) {
                    try {
                        format.connect();
                        format.format(message);

                        DialogUtil.displayInfoDialog(context, "Done formatting");
                        return true;
                    } catch (TagLostException e) {
                        DialogUtil.displayErrorDialog(context, "NFC Format Error", "Tag lost");
                        Log.e(TAG, "NFC format error", e);
                        return false;
                    } catch (IOException | FormatException e) {
                        DialogUtil.displayErrorDialog(context, "NFC Format Error", "Failed to format");
                        Log.e(TAG, "NFC format error", e);
                        return false;
                    }
                } else {
                    DialogUtil.displayErrorDialog(context, "NFC Format Error", "No NDEF");
                    Log.e(TAG, "No NDEF");
                    return false;
                }
            }
        } catch(Exception e) {
            DialogUtil.displayErrorDialog(context, "NFC Error", "Unknown error");
            Log.e(TAG, "NFC Error", e);
        }

        return false;
    }

    private static NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }

    private static NdefRecord createExtRecord(Context context) {
        //byte[] payload; //assign to your data
        String domain = context.getApplicationContext().getPackageName();
        String type = "externalType";
        return NdefRecord.createExternal(domain, type, domain.getBytes());
    }

    public static void readTag(Intent intent, TagHandler handler) {

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TAG, String.format("%s %s (%s)", key, value.toString(), value.getClass().getName()));
        }
        Log.d(TAG, "msgs: " + rawMsgs.length);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        NdefRecord[] records = msg.getRecords();
        Log.d(TAG, "records: " + records.length);
        for(NdefRecord record : records) {
            if(record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                byte[] payload = record.getPayload();
                String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                int languageCodeLength = payload[0] & 0063;

                try {
                    String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
                    if(!"en".equals(languageCode)) {
                        Log.e(TAG, "unsupported language code " + languageCode);
                        continue;
                    }

                    // Get the Text
                    String queryString = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding).trim();
                    if(!queryString.startsWith("?")) {
                        queryString = "?" + queryString;
                    }
                    Uri uri = Uri.parse(queryString);
                    for(String key : uri.getQueryParameterNames()) {
                        Log.d(TAG, key + " => " + uri.getQueryParameter(key));
                        handler.processText(key, uri.getQueryParameter(key));
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Failed to read NFC tag", e);
                }
            } else if(record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                Log.d(TAG, "ext: " + new String(record.getPayload()));
                handler.processOther(new String(record.getPayload()));
            } else {
                Log.d(TAG, "unhandled record");
                handler.processOther(new String(record.getPayload()));
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "action: " + intent.getAction());
        Bundle bundle = intent.getExtras();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Log.d(TAG, String.format("%s %s (%s)", key, value.toString(), value.getClass().getName()));
        }
    }

    public interface TagHandler {
        void processText(String key, String value);
        void processOther(String payload);
    }
}
