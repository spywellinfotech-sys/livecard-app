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
