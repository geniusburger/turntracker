package me.geniusburger.turntracker.utilities;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class NfcUtil {

    private static final String TAG = NfcUtil.class.getSimpleName();

    private long autoTurnTaskId;

    public long getAutoTurnTaskId() {
        return autoTurnTaskId;
    }

//        String packageName = MainActivity.class.getPackage().getName();
//        NdefRecord taskRecord = NdefRecord.createUri("http://geniusburger.me/task/123");
//        NdefRecord aarRecord = NdefRecord.createApplicationRecord(packageName);
//        NdefMessage msg = new NdefMessage( new NdefRecord[] { taskRecord, aarRecord });
//        Log.d(TAG, "nfc length " + msg.getByteArrayLength());

    public boolean readTag(Intent intent) {

        //Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        Log.d(TAG, "msgs: " + rawMsgs.length);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        NdefRecord[] records = msg.getRecords();
        Log.d(TAG, "records: " + records.length);
        for(NdefRecord record : records) {
            if(record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                byte[] payload = record.getPayload();
                String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

                // Get the Language Code
                int languageCodeLength = payload[0] & 0063;

                try {
                    String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
                    if(!"en".equals(languageCode)) {
                        Log.d(TAG, "unsupported language code " + languageCode);
                        continue;
                    }
                    // e.g. "en"

                    // Get the Text
                    String queryString = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding).trim();
                    if(!queryString.startsWith("?")) {
                        queryString = "?" + queryString;
                    }
                    Uri uri = Uri.parse(queryString);
                    for(String key : uri.getQueryParameterNames()) {
                        Log.d(TAG, key + " => " + uri.getQueryParameter(key));
                        if("task".equals(key)) {
                            autoTurnTaskId = Long.parseLong(uri.getQueryParameter(key));
                            return true;
                        }
                    }
                    Log.d(TAG, languageCode + ": " + queryString);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } else if(record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                Log.d(TAG, "ext: " + new String(record.getPayload()));
            } else {
                Log.d(TAG, "unhandled record");
            }
        }

        return false;
    }
}
