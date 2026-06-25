//
// Decompiled by Jadx - 728ms
//
package com.alipay.mywebview.sdk;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.InputEvent;
import android.view.KeyEvent;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mywebview.sdk.extension.EmbedViewConfig;
import com.alipay.mywebview.sdk.extension.IEmbedView;
import com.alipay.mywebview.sdk.extension.IEmbedViewContainer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

@MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
public class WebViewClient {
    public static final int ERROR_AUTHENTICATION = -4;
    public static final int ERROR_BAD_URL = -12;
    public static final int ERROR_CONNECT = -6;
    public static final int ERROR_FAILED_SSL_HANDSHAKE = -11;
    public static final int ERROR_FILE = -13;
    public static final int ERROR_FILE_NOT_FOUND = -14;
    public static final int ERROR_HOST_LOOKUP = -2;
    public static final int ERROR_IO = -7;
    public static final int ERROR_PROXY_AUTHENTICATION = -5;
    public static final int ERROR_REDIRECT_LOOP = -9;
    public static final int ERROR_TIMEOUT = -8;
    public static final int ERROR_TOO_MANY_REQUESTS = -15;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_UNSAFE_RESOURCE = -16;
    public static final int ERROR_UNSUPPORTED_AUTH_SCHEME = -3;
    public static final int ERROR_UNSUPPORTED_SCHEME = -10;
    public static final int SAFE_BROWSING_THREAT_BILLING = 4;
    public static final int SAFE_BROWSING_THREAT_MALWARE = 1;
    public static final int SAFE_BROWSING_THREAT_PHISHING = 2;
    public static final int SAFE_BROWSING_THREAT_UNKNOWN = 0;
    public static final int SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE = 3;
    public static ChangeQuickRedirect 支;

    @MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
    @Retention(RetentionPolicy.SOURCE)
    public @interface SafeBrowsingThreat {
    }

    public WebViewClient() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) == null) {
            return;
        }
        proxy.afterSuper(this);
    }

    public void doUpdateVisitedHistory(WebView webView, String str, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z2 = PatchProxy.proxy(new Object[]{webView, str, Boolean.valueOf(z), WebView.class, String.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "1").isSupported;
        }
    }

    public IEmbedView getEmbedView(EmbedViewConfig embedViewConfig, IEmbedViewContainer iEmbedViewContainer) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return null;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{embedViewConfig, iEmbedViewContainer, EmbedViewConfig.class, IEmbedViewContainer.class, IEmbedView.class}, this, changeQuickRedirect, "2");
        if (proxy.isSupported) {
            return (IEmbedView) proxy.result;
        }
        return null;
    }

    public void onFirstVisuallyNonEmptyDraw() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported;
        }
    }

    public void onFormResubmission(WebView webView, Message message, Message message2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, message, message2, WebView.class, Message.class, Message.class, Void.TYPE}, this, changeQuickRedirect, "4").isSupported) {
            return;
        }
        message.sendToTarget();
    }

    public void onGpuProcessGone(String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(str, this, changeQuickRedirect, "5", String.class, Void.TYPE).isSupported;
        }
    }

    public void onLoadResource(WebView webView, String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, str, WebView.class, String.class, Void.TYPE}, this, changeQuickRedirect, "6").isSupported;
        }
    }

    public void onPageCommitVisible(WebView webView, String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, str, WebView.class, String.class, Void.TYPE}, this, changeQuickRedirect, "7").isSupported;
        }
    }

    public void onPageFinished(WebView webView, String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, str, WebView.class, String.class, Void.TYPE}, this, changeQuickRedirect, "8").isSupported;
        }
    }

    public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, str, bitmap, WebView.class, String.class, Bitmap.class, Void.TYPE}, this, changeQuickRedirect, "9").isSupported;
        }
    }

    public void onReceivedClientCertRequest(WebView webView, ClientCertRequest clientCertRequest) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, clientCertRequest, WebView.class, ClientCertRequest.class, Void.TYPE}, this, changeQuickRedirect, "10").isSupported) {
            return;
        }
        clientCertRequest.cancel();
    }

    public void onReceivedDispatchResponse(HashMap<String, String> hashMap) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(hashMap, this, changeQuickRedirect, "11", HashMap.class, Void.TYPE).isSupported;
        }
    }

    @Deprecated
    public void onReceivedError(WebView webView, int i, String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, Integer.valueOf(i), str, str2, WebView.class, Integer.TYPE, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "12").isSupported;
        }
    }

    public void onReceivedHttpAuthRequest(WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, httpAuthHandler, str, str2, WebView.class, HttpAuthHandler.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "14").isSupported) {
            return;
        }
        httpAuthHandler.cancel();
    }

    public void onReceivedHttpError(WebView webView, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, webResourceRequest, webResourceResponse, WebView.class, WebResourceRequest.class, WebResourceResponse.class, Void.TYPE}, this, changeQuickRedirect, "15").isSupported;
        }
    }

    public void onReceivedLoginRequest(WebView webView, String str, String str2, String str3) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, str, str2, str3, WebView.class, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "16").isSupported;
        }
    }

    public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, sslErrorHandler, sslError, WebView.class, SslErrorHandler.class, SslError.class, Void.TYPE}, this, changeQuickRedirect, "17").isSupported) {
            return;
        }
        sslErrorHandler.cancel();
    }

    public boolean onRenderProcessGone(WebView webView, RenderProcessGoneDetail renderProcessGoneDetail) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{webView, renderProcessGoneDetail, WebView.class, RenderProcessGoneDetail.class, Boolean.TYPE}, this, changeQuickRedirect, "18");
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public void onSafeBrowsingHit(WebView webView, WebResourceRequest webResourceRequest, int i, SafeBrowsingResponse safeBrowsingResponse) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, webResourceRequest, Integer.valueOf(i), safeBrowsingResponse, WebView.class, WebResourceRequest.class, Integer.TYPE, SafeBrowsingResponse.class, Void.TYPE}, this, changeQuickRedirect, "19").isSupported) {
            return;
        }
        safeBrowsingResponse.showInterstitial(true);
    }

    public void onScaleChanged(WebView webView, float f, float f2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, Float.valueOf(f), Float.valueOf(f2), WebView.class, Float.TYPE, Float.TYPE, Void.TYPE}, this, changeQuickRedirect, "20").isSupported;
        }
    }

    public void onUnhandledInputEvent(WebView webView, InputEvent inputEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, inputEvent, WebView.class, InputEvent.class, Void.TYPE}, this, changeQuickRedirect, "21").isSupported) {
            return;
        }
        if (inputEvent instanceof KeyEvent) {
            onUnhandledKeyEvent(webView, (KeyEvent) inputEvent);
        } else {
            onUnhandledInputEventInternal(webView, inputEvent);
        }
    }

    public void onUnhandledInputEventInternal(WebView webView, InputEvent inputEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{webView, inputEvent, WebView.class, InputEvent.class, Void.TYPE}, this, changeQuickRedirect, "22").isSupported;
        }
    }

    public void onUnhandledKeyEvent(WebView webView, KeyEvent keyEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webView, keyEvent, WebView.class, KeyEvent.class, Void.TYPE}, this, changeQuickRedirect, "23").isSupported) {
            return;
        }
        onUnhandledInputEventInternal(webView, keyEvent);
    }

    public String populateErrorPage(WebView webView, String str, int i, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return null;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{webView, str, Integer.valueOf(i), str2, WebView.class, String.class, Integer.TYPE, String.class, String.class}, this, changeQuickRedirect, "24");
        if (proxy.isSupported) {
            return (String) proxy.result;
        }
        return null;
    }

    public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return null;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{webView, webResourceRequest, WebView.class, WebResourceRequest.class, WebResourceResponse.class}, this, changeQuickRedirect, "25");
        if (proxy.isSupported) {
            return (WebResourceResponse) proxy.result;
        }
        return null;
    }

    public boolean shouldOverrideKeyEvent(WebView webView, KeyEvent keyEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{webView, keyEvent, WebView.class, KeyEvent.class, Boolean.TYPE}, this, changeQuickRedirect, "26");
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{webView, webResourceRequest, WebView.class, WebResourceRequest.class, Boolean.TYPE}, this, changeQuickRedirect, "27");
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{webView, webResourceRequest, webResourceError, WebView.class, WebResourceRequest.class, WebResourceError.class, Void.TYPE}, this, changeQuickRedirect, "13").isSupported) && webResourceRequest.isForMainFrame()) {
            onReceivedError(webView, webResourceError.getErrorCode(), webResourceError.getDescription().toString(), webResourceRequest.getUrl().toString());
        }
    }
}
