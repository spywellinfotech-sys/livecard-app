package com.spywell.vicard;

import android.content.ContentValues;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import java.io.OutputStream;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
  public class VcBridge {
    @JavascriptInterface public String save(String b64, String name) {
      try {
        byte[] d = Base64.decode(b64, Base64.DEFAULT);
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, name + ".png");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (android.os.Build.VERSION.SDK_INT >= 29) {
          v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DynaMe");
          v.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        android.net.Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (u == null) return "";
        OutputStream o = getContentResolver().openOutputStream(u);
        o.write(d); o.close();
        if (android.os.Build.VERSION.SDK_INT >= 29) {
          ContentValues w = new ContentValues();
          w.put(MediaStore.Images.Media.IS_PENDING, 0);
          getContentResolver().update(u, w, null, null);
        }
        return u.toString();
      } catch (Exception e) { return ""; }
    }
    @JavascriptInterface public String saveFile(String b64, String name, String mime) {
      try {
        if (android.os.Build.VERSION.SDK_INT < 29) return "";
        byte[] d = Base64.decode(b64, Base64.DEFAULT);
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/LiveCard");
        v.put(MediaStore.MediaColumns.IS_PENDING, 1);
        android.net.Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) return "";
        OutputStream o = getContentResolver().openOutputStream(u);
        o.write(d); o.close();
        ContentValues w = new ContentValues();
        w.put(MediaStore.MediaColumns.IS_PENDING, 0);
        getContentResolver().update(u, w, null, null);
        return u.toString();
      } catch (Exception e) { return ""; }
    }
    @JavascriptInterface public String nfcState() {
      android.nfc.NfcAdapter a = android.nfc.NfcAdapter.getDefaultAdapter(MainActivity.this);
      if (a == null) return "none"; return a.isEnabled() ? "on" : "off";
    }
    @JavascriptInterface public void cancelNfc() {
      runOnUiThread(new Runnable() { public void run() {
        try { android.nfc.NfcAdapter a = android.nfc.NfcAdapter.getDefaultAdapter(MainActivity.this); if (a != null) a.disableReaderMode(MainActivity.this); } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public boolean writeNfc(final String url) {
      final android.nfc.NfcAdapter a = android.nfc.NfcAdapter.getDefaultAdapter(MainActivity.this);
      if (a == null || !a.isEnabled()) return false;
      runOnUiThread(new Runnable() { public void run() {
        try {
          a.enableReaderMode(MainActivity.this, new android.nfc.NfcAdapter.ReaderCallback() {
            public void onTagDiscovered(android.nfc.Tag tag) {
              String st;
              try {
                android.nfc.NdefMessage msg = new android.nfc.NdefMessage(android.nfc.NdefRecord.createUri(url));
                android.nfc.tech.Ndef nd = android.nfc.tech.Ndef.get(tag);
                if (nd != null) {
                  nd.connect();
                  if (!nd.isWritable()) st = "readonly";
                  else if (nd.getMaxSize() < msg.toByteArray().length) st = "small";
                  else { nd.writeNdefMessage(msg); st = "ok"; }
                  try { nd.close(); } catch (Exception e2) {}
                } else {
                  android.nfc.tech.NdefFormatable f = android.nfc.tech.NdefFormatable.get(tag);
                  if (f != null) { f.connect(); f.format(msg); try { f.close(); } catch (Exception e3) {} st = "ok"; }
                  else st = "unsupported";
                }
              } catch (Exception e) { st = "error"; }
              final String s2 = st;
              runOnUiThread(new Runnable() { public void run() { try { a.disableReaderMode(MainActivity.this); } catch (Exception e) {} } });
              jsCall("window.__vcNfc&&window.__vcNfc('" + s2 + "')");
            }
          }, android.nfc.NfcAdapter.FLAG_READER_NFC_A | android.nfc.NfcAdapter.FLAG_READER_NFC_B | android.nfc.NfcAdapter.FLAG_READER_NFC_F | android.nfc.NfcAdapter.FLAG_READER_NFC_V | android.nfc.NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | android.nfc.NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
        } catch (Exception e) { jsCall("window.__vcNfc&&window.__vcNfc('error')"); }
      } });
      return true;
    }
    @JavascriptInterface public void emailFile(final String uri, final String to, final String subject, final String body) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_SEND);
          i.setType("application/pdf");
          i.putExtra(Intent.EXTRA_EMAIL, new String[]{ to });
          i.putExtra(Intent.EXTRA_SUBJECT, subject);
          i.putExtra(Intent.EXTRA_TEXT, body);
          i.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri));
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          Intent g = new Intent(i); g.setPackage("com.google.android.gm");
          try { startActivity(g); } catch (Exception e) { startActivity(Intent.createChooser(i, "Email")); }
        } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public void shareFile(final String uri, final String mime) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_SEND);
          i.setType(mime == null || mime.isEmpty() ? "application/pdf" : mime);
          i.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri));
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          startActivity(Intent.createChooser(i, "Share"));
        } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public void share(final String uri) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_SEND);
          i.setType("image/png");
          i.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri));
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          startActivity(Intent.createChooser(i, "Share"));
        } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public void shareImgText(final String uri, final String text) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_SEND);
          i.setType("image/png");
          i.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri));
          i.putExtra(Intent.EXTRA_TEXT, text);
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          startActivity(Intent.createChooser(i, "Share"));
        } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public void shareText(final String text) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_SEND);
          i.setType("text/plain");
          i.putExtra(Intent.EXTRA_TEXT, text);
          startActivity(Intent.createChooser(i, "Share"));
        } catch (Exception e) {}
      } });
    }
    @JavascriptInterface public boolean openWa(final String phone, final String text, final boolean biz) {
      try {
        String u = "https://wa.me/" + (phone == null ? "" : phone) + "?text=" + Uri.encode(text == null ? "" : text);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
        i.setPackage(biz ? "com.whatsapp.w4b" : "com.whatsapp");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        return true;
      } catch (Exception e) { return false; }
    }

    @JavascriptInterface public void openAuth(final String url) {
      runOnUiThread(new Runnable() { public void run() {
        try {
          androidx.browser.customtabs.CustomTabsIntent t = new androidx.browser.customtabs.CustomTabsIntent.Builder().build();
          t.launchUrl(MainActivity.this, Uri.parse(url));
        } catch (Throwable e) {
          try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Throwable e2) {}
        }
      } });
    }
    @JavascriptInterface public void scanQr() {
      runOnUiThread(new Runnable() { public void run() {
        try {
          com.google.mlkit.vision.codescanner.GmsBarcodeScanner sc = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(MainActivity.this);
          sc.startScan()
            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.mlkit.vision.barcode.common.Barcode>() { public void onSuccess(com.google.mlkit.vision.barcode.common.Barcode b) { jsCall("window.__vcQr&&window.__vcQr(" + JSONObject.quote(String.valueOf(b.getRawValue())) + ")"); } })
            .addOnCanceledListener(new com.google.android.gms.tasks.OnCanceledListener() { public void onCanceled() { jsCall("window.__vcQr&&window.__vcQr('')"); } })
            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() { public void onFailure(Exception e) { jsCall("window.__vcQr&&window.__vcQr('ERR')"); } });
        } catch (Throwable e) { jsCall("window.__vcQr&&window.__vcQr('ERR')"); }
      } });
    }
    @JavascriptInterface public void pickContact() {
      runOnUiThread(new Runnable() { public void run() {
        try {
          Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
          contactLauncher.launch(i);
        } catch (Exception e) { jsCall("window.__vcContact&&window.__vcContact('','')"); }
      } });
    }
    @JavascriptInterface public void recentCalls() {
      runOnUiThread(new Runnable() { public void run() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) { sendCalls(); }
        else { requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, 71); }
      } });
    }
    @JavascriptInterface public void addEvents(final String json) {
      runOnUiThread(new Runnable() { public void run() {
        pendingEvents = json;
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) { insertEvents(); }
        else { requestPermissions(new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 72); }
      } });
    }
  }
  private String pendingEvents = null;
  private ActivityResultLauncher<Intent> contactLauncher;
  private void jsCall(final String js) {
    runOnUiThread(new Runnable() { public void run() {
      try { getBridge().getWebView().evaluateJavascript(js, null); } catch (Exception e) {}
    } });
  }
  private void sendCalls() {
    JSONArray out = new JSONArray();
    try {
      Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI,
        new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION},
        null, null, CallLog.Calls.DATE + " DESC");
      java.util.HashSet<String> seen = new java.util.HashSet<String>();
      if (c != null) {
        while (c.moveToNext() && out.length() < 1500) {
          String num = c.getString(0) == null ? "" : c.getString(0).replaceAll("[^0-9]", "");
          if (num.length() > 10) num = num.substring(num.length() - 10);
          if (num.length() < 3) continue;
          JSONObject o = new JSONObject();
          o.put("n", num); o.put("name", c.getString(1) == null ? "" : c.getString(1));
          o.put("t", c.getInt(2)); o.put("d", c.getLong(3)); o.put("s", c.getInt(4));
          out.put(o);
        }
        c.close();
      }
    } catch (Exception e) {}
    jsCall("window.__vcCalls&&window.__vcCalls(" + JSONObject.quote(out.toString()) + ")");
  }
  private long writableCalendar() {
    try {
      Cursor c = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
        new String[]{CalendarContract.Calendars._ID},
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=500 AND " + CalendarContract.Calendars.VISIBLE + "=1",
        null, CalendarContract.Calendars.IS_PRIMARY + " DESC");
      if (c != null) { long id = -1; if (c.moveToFirst()) id = c.getLong(0); c.close(); return id; }
    } catch (Exception e) {}
    return -1;
  }
  private void insertEvents() {
    int n = 0;
    String js = pendingEvents; pendingEvents = null;
    try {
      JSONArray a = new JSONArray(js);
      long cal = writableCalendar();
      if (cal < 0) {
        // no calendar account: open the calendar's own add-event screen for the first item
        JSONObject o = a.getJSONObject(0);
        Intent i = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
          .putExtra(CalendarContract.Events.TITLE, o.optString("t"))
          .putExtra(CalendarContract.Events.EVENT_LOCATION, o.optString("p"))
          .putExtra(CalendarContract.Events.DESCRIPTION, o.optString("note"))
          .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, o.optLong("s"))
          .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, o.optLong("e"))
          .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, o.optBoolean("allDay"));
        startActivity(i);
        jsCall("window.__vcCal&&window.__vcCal(1)");
        return;
      }
      for (int k = 0; k < a.length(); k++) {
        JSONObject o = a.getJSONObject(k);
        ContentValues v = new ContentValues();
        v.put(CalendarContract.Events.CALENDAR_ID, cal);
        v.put(CalendarContract.Events.TITLE, o.optString("t"));
        v.put(CalendarContract.Events.EVENT_LOCATION, o.optString("p"));
        v.put(CalendarContract.Events.DESCRIPTION, o.optString("note"));
        v.put(CalendarContract.Events.DTSTART, o.optLong("s"));
        v.put(CalendarContract.Events.DTEND, o.optLong("e"));
        boolean all = o.optBoolean("allDay");
        v.put(CalendarContract.Events.ALL_DAY, all ? 1 : 0);
        v.put(CalendarContract.Events.EVENT_TIMEZONE, all ? "UTC" : java.util.TimeZone.getDefault().getID());
        v.put(CalendarContract.Events.HAS_ALARM, 1);
        Uri u = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, v);
        if (u == null) continue;
        long eid = Long.parseLong(u.getLastPathSegment());
        JSONArray rm = o.optJSONArray("rem");
        if (rm != null) for (int r = 0; r < rm.length(); r++) {
          ContentValues rv = new ContentValues();
          rv.put(CalendarContract.Reminders.EVENT_ID, eid);
          rv.put(CalendarContract.Reminders.MINUTES, rm.getInt(r));
          rv.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
          getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, rv);
        }
        n++;
      }
    } catch (Exception e) {}
    jsCall("window.__vcCal&&window.__vcCal(" + n + ")");
  }
  @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
    super.onRequestPermissionsResult(code, perms, res);
    boolean ok = res.length > 0;
    for (int r : res) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
    if (code == 71) { if (ok) sendCalls(); else jsCall("window.__vcCalls&&window.__vcCalls('denied')"); }
    if (code == 72) { if (ok) insertEvents(); else { pendingEvents = null; jsCall("window.__vcCal&&window.__vcCal(-1)"); } }
  }
  private void placeholderEnd() {
  }
  private void openLink(Intent i, long delay) {
    if (i == null) return;
    final Uri d = i.getData();
    if (d == null || !"livecard.in".equals(d.getHost())) return;
    final String url = d.toString();
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
      public void run() {
        try { if (getBridge() != null) getBridge().getWebView().loadUrl(url); } catch (Exception e) {}
      }
    }, delay);
  }
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    contactLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
      try {
        Intent d = result.getData();
        if (result.getResultCode() != RESULT_OK || d == null || d.getData() == null) return;
        Cursor c = getContentResolver().query(d.getData(), new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
        if (c != null && c.moveToFirst()) {
          String nm = c.getString(0) == null ? "" : c.getString(0), no = c.getString(1) == null ? "" : c.getString(1);
          c.close();
          jsCall("window.__vcContact&&window.__vcContact(" + JSONObject.quote(nm) + "," + JSONObject.quote(no) + ")");
        }
      } catch (Exception e) {}
    });
    try { getBridge().getWebView().addJavascriptInterface(new VcBridge(), "ViNative"); } catch (Exception e) {}
    try { getBridge().getWebView().clearCache(true); } catch (Exception e) {}
    openLink(getIntent(), 1800);
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override public void handleOnBackPressed() {
        try {
          WebView w = getBridge().getWebView();
          String u = w.getUrl();
          if (w.canGoBack()) { w.goBack(); return; }
          if (u != null && u.startsWith("http") && !u.matches("https://livecard\\.in/studio/?(\\?.*)?(#.*)?")) { w.loadUrl("https://livecard.in/studio/"); return; }
        } catch (Exception e) {}
        setEnabled(false);
        getOnBackPressedDispatcher().onBackPressed();
      }
    });
  }
  @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); openLink(i, 50); }
}
