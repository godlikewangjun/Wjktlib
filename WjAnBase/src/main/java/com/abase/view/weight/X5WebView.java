package com.abase.view.weight;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.abase.util.AbFileUtil;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceError;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.tencent.smtt.utils.TbsLog;
import com.wj.eventbus.WjEventBus;

public class X5WebView extends WebView {
	public String url;//加载的地址
	TextView title;
	private WebViewClient client = new WebViewClient() {
		/**
		 * 防止加载网页时调起系统浏览器
		 */
//		public boolean shouldOverrideUrlLoading(WebView view, String url) {
//			view.loadUrl(url);
//			return true;
//		}

		@Override
		public void onPageFinished(WebView webView, String s) {
			super.onPageFinished(webView, s);
			WjEventBus.getInit().post(LoadWeb.LOADFINSH, "");
		}

		@Override
		public void onReceivedError(WebView webView, int i, String s, String s1) {
			super.onReceivedError(webView, i, s, s1);
			WjEventBus.getInit().post(LoadWeb.LOADERROE, "");
		}

		@Override
		public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
			super.onReceivedError(webView, webResourceRequest, webResourceError);
			WjEventBus.getInit().post(LoadWeb.LOADERROE, "");
		}

		@Override
		public void onReceivedHttpError(WebView webView, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
			super.onReceivedHttpError(webView, webResourceRequest, webResourceResponse);
		}

		@Override
		public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
			sslErrorHandler.proceed();
		}
	};

	@SuppressLint("SetJavaScriptEnabled")
	public X5WebView(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
		this.setWebViewClient(client);
		// this.setWebChromeClient(chromeClient);
		// WebStorage webStorage = WebStorage.getInstance();
		initWebViewSettings();
		this.getView().setClickable(true);
	}

	private void initWebViewSettings() {
		WebSettings webSetting = getSettings();
		webSetting.setAllowFileAccess(true);
		webSetting.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
		webSetting.setSupportZoom(true);
		webSetting.setBuiltInZoomControls(true);
		webSetting.setUseWideViewPort(true);
		webSetting.setSupportMultipleWindows(false);
		// webSetting.setLoadWithOverviewMode(true);
		webSetting.setAppCacheEnabled(true);
		// webSetting.setDatabaseEnabled(true);
		webSetting.setDomStorageEnabled(true);
		webSetting.setJavaScriptEnabled(true);
		webSetting.setGeolocationEnabled(true);
		webSetting.setAppCacheMaxSize(Long.MAX_VALUE);
		webSetting.setAppCachePath(AbFileUtil.getCacheDownloadDir(getContext()));
		webSetting.setDatabasePath(getContext().getDir("databases", 0).getPath());
		webSetting.setGeolocationDatabasePath(getContext().getDir("geolocation", 0)
				.getPath());
		// webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
		webSetting.setPluginState(WebSettings.PluginState.ON_DEMAND);

		CookieSyncManager.createInstance(getContext());
		CookieSyncManager.getInstance().sync();
	}
	/**
	 * 重新加载url
	 *
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
		if (url.indexOf("http") < 0) {
			url = "file:///android_asset/" + url;
		} else if(url.indexOf("http")==0 || url.indexOf("https")==0) {
			// webview is ready now, just tell session client to bind
				loadUrl(url);
		}
	}

	/**
	 * 加载html
	 * @param html
	 */
	public void loadHtml(String html){
		loadData(html, "text/html; charset=UTF-8", null);//这种写法可以正确解码
	}
	/**
	 * 设置js调用java的接口
	 */
	@SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
	public void setJavaToJs(Object obj, String interfaceName) {
		addJavascriptInterface(obj, interfaceName);
	}

	public X5WebView(Context arg0) {
		super(arg0);
		setBackgroundColor(85621);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		destroy();
	}
}
