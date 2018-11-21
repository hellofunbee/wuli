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
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class MainActivity extends Activity {
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);

        webView = findViewById(R.id.webview);
        webView.addJavascriptInterface(new AndroidView(), "AndroidView");
        setSettings();
        webView.loadUrl("http://211.82.10.173:8087/wuli/login.html");//加载url

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
        WebViewClient myWebViewClient = new WebViewClient() {
            @Override
            // 在点击请求的是链接是才会调用，重写此方法返回true表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边。这个函数我们可以做很多操作，比如我们读取到某些特殊的URL，于是就可以不打开地址，取消这个操作，进行预先定义的其他操作，这对一个程序是非常必要的。
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 判断url链接中是否含有某个字段，如果有就执行指定的跳转（不执行跳转url链接），如果没有就加载url链接
                if (url.contains("pfsc.agri.cn")) {
                    Intent intent = new Intent(MainActivity.this, WebviewActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            }

        };//建立对象
        webView.setWebViewClient(myWebViewClient);//调用
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
            Intent intent = new Intent(MainActivity.this, CameraBaseActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("ip", ip);
            bundle.putInt("port", Integer.parseInt(port));
            bundle.putString("name", name);
            bundle.putString("pass", pass);
            intent.putExtras(bundle);
            startActivity(intent);//不需要返回数据的
            return "1";
        }

        @JavascriptInterface
        public void open(String url) {
            Intent intent = new Intent(MainActivity.this, WebviewActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        webView.destroy();
        webView = null;
    }

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
            System.exit(0);
        }
    }
}
