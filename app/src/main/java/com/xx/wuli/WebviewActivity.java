/**
 * <p>DemoActivity Class</p>
 *
 * @author zhuzhenlei 2014-7-17
 * @version V1.0
 * @modificationHistory
 * @modify by user:
 * @modify by reason:
 */
package com.xx.wuli;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;


public class WebviewActivity extends Activity {
    private WebView webView;
    private Button btn_back;
    private TextView title;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview2);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String url = bundle.getString("url");

        btn_back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        webView = findViewById(R.id.webview);
        webView.addJavascriptInterface(new AndroidView(), "AndroidView");
        setSettings();
        webView.loadUrl(url);//加载url

    }

    private void setSettings() {
        WebSettings webSettings = webView.getSettings();
//        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);//不使用缓存，只从网络获取数据.
        webSettings.setAllowFileAccess(true);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        //支持屏幕缩放
        webSettings.setBuiltInZoomControls(true);
        webView.getSettings().setDatabaseEnabled(true);//开启数据库
        webView.setFocusable(true);//获取焦点
        webView.requestFocus();
        String dir = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();//设置数据库路径
        webView.getSettings().setCacheMode(webView.getSettings().LOAD_CACHE_ELSE_NETWORK);//本地缓存
        webView.getSettings().setBlockNetworkImage(false);//显示网络图像
        webView.getSettings().setLoadsImagesAutomatically(true);//显示网络图像
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);//插件支持
        webView.getSettings().setSupportZoom(false);//设置是否支持变焦
        webView.getSettings().setJavaScriptEnabled(true);//支持JavaScriptEnabled
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);//支持JavaScriptEnabled
        webView.getSettings().setGeolocationEnabled(true);//定位
        webView.getSettings().setGeolocationDatabasePath(dir);//数据库
        webView.getSettings().setDomStorageEnabled(true);//缓存 （ 远程web数据的本地化存储）

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String t = view.getTitle();
                if (!TextUtils.isEmpty(t)) {
                    if (t.length() > 14) {
                        t = t.substring(0, 14) + "...";
                    }
                    title.setText(t);
                }
            }
        });//调用
        webView.setWebChromeClient(new WebChromeClient() {

            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }

        });


    }

    class AndroidView {
        @JavascriptInterface
        public String showView(String deviceId, String ip, String port, String name, String pass) {
            System.out.println("---------------------------------------------:" + deviceId + "ip:" + ip + " port:" + port + " pass:" + pass + " name:" + name);
            Intent intent = new Intent(WebviewActivity.this, CameraBaseActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("ip", ip);
            bundle.putInt("port", Integer.parseInt(port));
            bundle.putString("name", name);
            bundle.putString("pass", pass);
            intent.putExtras(bundle);
            startActivity(intent);//不需要返回数据的
            return "1";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        webView.destroy();
        webView = null;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                return super.onKeyDown(keyCode, event);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


}
