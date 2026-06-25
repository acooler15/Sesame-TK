//
// Decompiled by Jadx - 902ms
//
package com.alipay.mywebview.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Message;
import android.print.PrintDocumentAdapter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.view.View_onTouchEvent_androidviewMotionEvent_stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mywebview.sdk.embedview.EmbedViewContainer;
import com.alipay.mywebview.sdk.embedview.EmbedViewContainerBridge;
import com.alipay.mywebview.sdk.extension.ConfigService;
import com.alipay.mywebview.sdk.extension.InjectJSProvider;
import com.alipay.mywebview.sdk.extension.OnSoftKeyboardListener;
import com.alipay.mywebview.sdk.extension.SdkVersionManager;
import com.alipay.mywebview.sdk.internal.IWebViewOverride;
import com.alipay.mywebview.sdk.internal.WebViewInternal;
import com.alipay.mywebview.sdk.internal.WebViewInternalForM;
import com.alipay.mywebview.sdk.intf.IEmbedViewContainerBridge;
import com.alipay.mywebview.sdk.intf.IWebView;
import com.alipay.mywebview.sdk.intf.IWebViewGlobal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
public class WebView extends FrameLayout implements View_onTouchEvent_androidviewMotionEvent_stub, IWebViewOverride {
    public static final int RENDERER_PRIORITY_BOUND = 1;
    public static final int RENDERER_PRIORITY_IMPORTANT = 2;
    public static final int RENDERER_PRIORITY_WAIVED = 0;
    private static final String TAG = "WebView";
    private static boolean sEnableDispatchKeyEvent;
    public static ChangeQuickRedirect 支;
    private final IEmbedViewContainerBridge mEmbedViewContainerBridge;
    private final WebViewInternal mInternalView;
    private EmbedViewContainer mTopViewContainer;

    @MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
    public interface FindListener {
        void onFindResultReceived(int i, int i2, boolean z);
    }

    @MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
    @Deprecated
    public interface PictureListener {
        @Deprecated
        void onNewPicture(WebView webView, Picture picture);
    }

    @MpaasClassInfo(BundleName = "android-phone-myweb_api", ExportJarName = "unknown", Level = "product", Product = ":android-phone-myweb_api")
    public static abstract class VisualStateCallback {
        public static ChangeQuickRedirect 支;

        public VisualStateCallback() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) == null) {
                return;
            }
            proxy.afterSuper(this);
        }

        public abstract void onComplete(long j);
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public WebView(Context context) {
        this((Context) r1[0], (AttributeSet) r1[1]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0", (objArr = new Object[]{context, null}))) == null) {
            this(context, null);
        } else {
            proxy.afterSuper(this);
        }
    }

    private boolean __onTouchEvent_stub_private(MotionEvent motionEvent) {
        WebView webView;
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, webView, changeQuickRedirect, "6", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            webView = this;
            motionEvent2 = motionEvent;
        }
        return webView.mInternalView.onTouchEvent(motionEvent2);
    }

    public static void addChildProcessObserver(ChildProcessStatObserver childProcessStatObserver) {
        ChildProcessStatObserver childProcessStatObserver2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            childProcessStatObserver2 = childProcessStatObserver;
            if (PatchProxy.proxy(childProcessStatObserver2, (Object) null, changeQuickRedirect, "7", ChildProcessStatObserver.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            childProcessStatObserver2 = childProcessStatObserver;
        }
        if (getGlobalImpl() != null) {
            getGlobalImpl().addObserver(childProcessStatObserver2);
        }
    }

    public static void clearClientCertPreferences(Runnable runnable) {
        Runnable runnable2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            runnable2 = runnable;
            if (PatchProxy.proxy(runnable2, (Object) null, changeQuickRedirect, "16", Runnable.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            runnable2 = runnable;
        }
        if (getGlobalImpl() != null) {
            getGlobalImpl().clearClientCertPreferences(runnable2);
        }
    }

    private static IWebViewGlobal getGlobalImpl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy((Object) null, changeQuickRedirect, "43", IWebViewGlobal.class);
            if (proxy.isSupported) {
                return (IWebViewGlobal) proxy.result;
            }
        }
        return WebViewInternal.getGlobalImpl();
    }

    private IWebView getImpl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "45", IWebView.class);
            if (proxy.isSupported) {
                return (IWebView) proxy.result;
            }
        }
        return this.mInternalView.getImpl();
    }

    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy((Object) null, changeQuickRedirect, "53", Uri.class);
            if (proxy.isSupported) {
                return (Uri) proxy.result;
            }
        }
        if (getGlobalImpl() != null) {
            return getGlobalImpl().getSafeBrowsingPrivacyPolicyUrl();
        }
        return null;
    }

    public static void onSavedMemory() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy((Object) null, changeQuickRedirect, "81", Void.TYPE).isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().onSavedMemory();
        }
    }

    public static void removeChildProcessObserver(ChildProcessStatObserver childProcessStatObserver) {
        ChildProcessStatObserver childProcessStatObserver2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            childProcessStatObserver2 = childProcessStatObserver;
            if (PatchProxy.proxy(childProcessStatObserver2, (Object) null, changeQuickRedirect, "92", ChildProcessStatObserver.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            childProcessStatObserver2 = childProcessStatObserver;
        }
        if (getGlobalImpl() != null) {
            getGlobalImpl().removeObserver(childProcessStatObserver2);
        }
    }

    public static void setDataDirectorySuffix(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, (Object) null, changeQuickRedirect, "106", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        if (getGlobalImpl() != null) {
            getGlobalImpl().setDataDirectorySuffix(str2);
        }
    }

    public static void setSafeBrowsingWhitelist(List<String> list, ValueCallback<Boolean> valueCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{list, valueCallback, List.class, ValueCallback.class, Void.TYPE}, (Object) null, changeQuickRedirect, "121").isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().setSafeBrowsingWhitelist(list, valueCallback);
        }
    }

    public static void setWebContentsDebuggingEnabled(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), (Object) null, changeQuickRedirect, "125", Boolean.TYPE, Void.TYPE).isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().setWebContentsDebuggingEnabled(z);
        }
    }

    public static void setWebViewGlobalImpl(IWebViewGlobal iWebViewGlobal) {
        IWebViewGlobal iWebViewGlobal2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            iWebViewGlobal2 = iWebViewGlobal;
            if (PatchProxy.proxy(iWebViewGlobal2, (Object) null, changeQuickRedirect, "127", IWebViewGlobal.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            iWebViewGlobal2 = iWebViewGlobal;
        }
        WebViewInternal.setWebViewGlobalImpl(iWebViewGlobal2);
    }

    public static void startRemoteDebugging(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, (Object) null, changeQuickRedirect, "131", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        if (getGlobalImpl() != null) {
            getGlobalImpl().startRemoteDebugging(str2);
        }
    }

    public static void startSafeBrowsing(Context context, ValueCallback<Boolean> valueCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{context, valueCallback, Context.class, ValueCallback.class, Void.TYPE}, (Object) null, changeQuickRedirect, "132").isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().startSafeBrowsing(context, valueCallback);
        }
    }

    public static void stopRemoteDebugging() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy((Object) null, changeQuickRedirect, "134", Void.TYPE).isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().stopRemoteDebugging();
        }
    }

    public static void warmUpChildProcess() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy((Object) null, changeQuickRedirect, "136", Void.TYPE).isSupported) && getGlobalImpl() != null) {
            getGlobalImpl().warmUpChildProcess();
        }
    }

    public static boolean warmupMWRenderProcessHost(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), (Object) null, changeQuickRedirect, "137", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (getGlobalImpl() != null) {
            return getGlobalImpl().warmupMWRenderProcessHost(z);
        }
        return false;
    }

    public boolean __onTouchEvent_stub(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, this, changeQuickRedirect, "5", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            motionEvent2 = motionEvent;
        }
        return __onTouchEvent_stub_private(motionEvent2);
    }

    public void addJavascriptInterface(Object obj, String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{obj, str, Object.class, String.class, Void.TYPE}, this, changeQuickRedirect, "8").isSupported) {
            return;
        }
        getImpl().addJavascriptInterface(obj, str);
    }

    public boolean canGoBack() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "9", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().canGoBack();
    }

    public boolean canGoBackOrForward(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "10", Integer.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().canGoBackOrForward(i);
    }

    public boolean canGoForward() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "11", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().canGoForward();
    }

    @Deprecated
    public boolean canZoomIn() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "12", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().canZoomIn();
    }

    @Deprecated
    public boolean canZoomOut() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "13", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().canZoomOut();
    }

    public Picture capturePicture() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "14", Picture.class);
            if (proxy.isSupported) {
                return (Picture) proxy.result;
            }
        }
        return getImpl().capturePicture();
    }

    public void clearCache(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "15", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearCache(z);
    }

    public void clearFormData() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "17", Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearFormData();
    }

    public void clearHistory() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "18", Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearHistory();
    }

    public void clearMatches() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "19", Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearMatches();
    }

    public void clearSslPreferences() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "20", Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearSslPreferences();
    }

    @Deprecated
    public void clearView() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "21", Void.TYPE).isSupported) {
            return;
        }
        getImpl().clearView();
    }

    public WebBackForwardList copyBackForwardList() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "22", WebBackForwardList.class);
            if (proxy.isSupported) {
                return (WebBackForwardList) proxy.result;
            }
        }
        return getImpl().copyBackForwardList();
    }

    public void coreOnScrollChanged(int i, int i2, int i3, int i4) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "23").isSupported;
        }
    }

    public void coreOverScrollBy(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z2 = PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.valueOf(i5), Integer.valueOf(i6), Integer.valueOf(i7), Integer.valueOf(i8), Boolean.valueOf(z), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "24").isSupported;
        }
    }

    public PrintDocumentAdapter createPrintDocumentAdapter(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, this, changeQuickRedirect, "25", String.class, PrintDocumentAdapter.class);
            if (proxy.isSupported) {
                return (PrintDocumentAdapter) proxy.result;
            }
        } else {
            str2 = str;
        }
        return getImpl().createPrintDocumentAdapter(str2);
    }

    public WebMessagePort[] createWebMessageChannel() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "26", WebMessagePort[].class);
            if (proxy.isSupported) {
                return (WebMessagePort[]) proxy.result;
            }
        }
        return getImpl().createWebMessageChannel();
    }

    public void destroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "27", Void.TYPE).isSupported) {
            return;
        }
        getImpl().destroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        WebView webView;
        KeyEvent keyEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            keyEvent2 = keyEvent;
            PatchProxyResult proxy = PatchProxy.proxy(keyEvent2, webView, changeQuickRedirect, "28", KeyEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            webView = this;
            keyEvent2 = keyEvent;
        }
        if (sEnableDispatchKeyEvent) {
            if (!webView.mTopViewContainer.dispatchKeyEvent(keyEvent2) && !webView.mInternalView.dispatchKeyEvent(keyEvent2)) {
                return false;
            }
            return true;
        }
        return super.dispatchKeyEvent(keyEvent2);
    }

    public void documentHasImages(Message message) {
        Message message2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            message2 = message;
            if (PatchProxy.proxy(message2, this, changeQuickRedirect, "29", Message.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            message2 = message;
        }
        getImpl().documentHasImages(message2);
    }

    public void evaluateJavascript(String str, ValueCallback<String> valueCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, valueCallback, String.class, ValueCallback.class, Void.TYPE}, this, changeQuickRedirect, "30").isSupported) {
            return;
        }
        getImpl().evaluateJavascript(str, valueCallback);
    }

    public void findAllAsync(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "31", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().findAllAsync(str2);
    }

    public void findNext(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "32", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().findNext(z);
    }

    public void flingScroll(int i, int i2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "33").isSupported) {
            return;
        }
        getImpl().flingScroll(i, i2);
    }

    public SslCertificate getCertificate() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "34", SslCertificate.class);
            if (proxy.isSupported) {
                return (SslCertificate) proxy.result;
            }
        }
        return getImpl().getCertificate();
    }

    public int getContentHeight() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "35", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getContentHeight();
    }

    public int getContentWidth() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "36", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getContentWidth();
    }

    public String getContentsMimeType() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "37", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getImpl().getContentsMimeType();
    }

    public int getCoreScrollX() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "38", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return this.mEmbedViewContainerBridge.getScrollX();
    }

    public int getCoreScrollY() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "39", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return this.mEmbedViewContainerBridge.getScrollY();
    }

    public WebViewCoreStatus getCoreStatus() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "40", WebViewCoreStatus.class);
            if (proxy.isSupported) {
                return (WebViewCoreStatus) proxy.result;
            }
        }
        return getImpl().getCoreStatus();
    }

    public Boolean getCurrentPageSnapshot(Rect rect, Rect rect2, Bitmap bitmap, boolean z, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{rect, rect2, bitmap, Boolean.valueOf(z), Integer.valueOf(i), Rect.class, Rect.class, Bitmap.class, Boolean.TYPE, Integer.TYPE, Boolean.class}, this, changeQuickRedirect, "41");
            if (proxy.isSupported) {
                return (Boolean) proxy.result;
            }
        }
        return getImpl().getCurrentPageSnapshot(rect, rect2, bitmap, z, i);
    }

    public Bitmap getFavicon() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "42", Bitmap.class);
            if (proxy.isSupported) {
                return (Bitmap) proxy.result;
            }
        }
        return getImpl().getFavicon();
    }

    public IWebView.HitTestResult getHitTestResult() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "44", IWebView.HitTestResult.class);
            if (proxy.isSupported) {
                return (IWebView.HitTestResult) proxy.result;
            }
        }
        return getImpl().getHitTestResult();
    }

    public String getLastCommittedUrl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "46", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getImpl().getLastCommittedUrl();
    }

    public String getOriginalUrl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "47", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getImpl().getOriginalUrl();
    }

    public int getProcessAssignmentOutcome() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "48", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getProcessAssignmentOutcome();
    }

    public int getProcessHostId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "49", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getProcessHostId();
    }

    public int getProgress() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "50", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getProgress();
    }

    public boolean getRendererPriorityWaivedWhenNotVisible() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "51", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().getRendererPriorityWaivedWhenNotVisible();
    }

    public int getRendererRequestedPriority() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "52", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return getImpl().getRendererRequestedPriority();
    }

    @ViewDebug.ExportedProperty(category = "webview")
    @Deprecated
    public float getScale() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "54", Float.TYPE);
            if (proxy.isSupported) {
                return ((Float) proxy.result).floatValue();
            }
        }
        return getImpl().getScale();
    }

    public WebSettings getSettings() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "55", WebSettings.class);
            if (proxy.isSupported) {
                return (WebSettings) proxy.result;
            }
        }
        return getImpl().getSettings();
    }

    public String getTitle() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "56", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getImpl().getTitle();
    }

    public String getUrl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "57", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getImpl().getUrl();
    }

    public String getVersion() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "58", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return SdkVersionManager.getInstance().getProductVersion();
    }

    public WebChromeClient getWebChromeClient() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "59", WebChromeClient.class);
            if (proxy.isSupported) {
                return (WebChromeClient) proxy.result;
            }
        }
        return getImpl().getWebChromeClient();
    }

    public WebViewClient getWebViewClient() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "60", WebViewClient.class);
            if (proxy.isSupported) {
                return (WebViewClient) proxy.result;
            }
        }
        return getImpl().getWebViewClient();
    }

    public WebViewRenderProcess getWebViewRenderProcess() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "61", WebViewRenderProcess.class);
            if (proxy.isSupported) {
                return (WebViewRenderProcess) proxy.result;
            }
        }
        return getImpl().getWebViewRenderProcess();
    }

    public WebViewRenderProcessClient getWebViewRenderProcessClient() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "62", WebViewRenderProcessClient.class);
            if (proxy.isSupported) {
                return (WebViewRenderProcessClient) proxy.result;
            }
        }
        return getImpl().getWebViewRenderProcessClient();
    }

    @Deprecated
    public View getZoomControls() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "63", View.class);
            if (proxy.isSupported) {
                return (View) proxy.result;
            }
        }
        return getImpl().getZoomControls();
    }

    public void goBack() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "64", Void.TYPE).isSupported) {
            return;
        }
        getImpl().goBack();
    }

    public void goBackOrForward(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "65", Integer.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().goBackOrForward(i);
    }

    public void goForward() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "66", Void.TYPE).isSupported) {
            return;
        }
        getImpl().goForward();
    }

    public boolean handleJavaJsApi(String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, str2, String.class, String.class, Boolean.TYPE}, this, changeQuickRedirect, "67");
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public void invokeZoomPicker() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "68", Void.TYPE).isSupported) {
            return;
        }
        getImpl().invokeZoomPicker();
    }

    public boolean isMWRender() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "69", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().isMWRender();
    }

    public boolean isPaused() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "70", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().isPaused();
    }

    public boolean isPrivateBrowsingEnabled() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "71", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().isPrivateBrowsingEnabled();
    }

    public void loadData(String str, String str2, String str3) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, str3, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "72").isSupported) {
            return;
        }
        getImpl().loadData(str, str2, str3);
    }

    public void loadDataWithBaseURL(String str, String str2, String str3, String str4, String str5) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, str3, str4, str5, String.class, String.class, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "73").isSupported) {
            return;
        }
        getImpl().loadDataWithBaseURL(str, str2, str3, str4, str5);
    }

    public void loadUrl(String str, Map<String, String> map) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, map, String.class, Map.class, Void.TYPE}, this, changeQuickRedirect, "75").isSupported) {
            getImpl().loadUrl(str, map);
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), intent, Integer.TYPE, Integer.TYPE, Intent.class, Void.TYPE}, this, changeQuickRedirect, "76").isSupported) {
            return;
        }
        getImpl().onActivityResult(i, i2, intent);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "77", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().onCheckIsTextEditor();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        WebView webView;
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, webView, changeQuickRedirect, "78", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            webView = this;
            motionEvent2 = motionEvent;
        }
        return webView.mInternalView.onGenericMotionEvent(motionEvent2);
    }

    public void onPause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "79", Void.TYPE).isSupported) {
            return;
        }
        getImpl().onPause();
    }

    public void onResume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "80", Void.TYPE).isSupported) {
            return;
        }
        getImpl().onResume();
    }

    @Override
    public void onScrollChanged(int i, int i2, int i3, int i4) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "82").isSupported) {
            return;
        }
        super.onScrollChanged(i, i2, i3, i4);
        this.mInternalView.publicOnScrollChanged(i, i2, i3, i4);
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4), Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "83").isSupported) {
            return;
        }
        this.mInternalView.publicOnSizeChanged(i, i2, i3, i4);
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, this, changeQuickRedirect, "84", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            motionEvent2 = motionEvent;
        }
        return getClass() != WebView.class ? __onTouchEvent_stub_private(motionEvent2) : DexAOPEntry.android_view_View_onTouchEvent_proxy(WebView.class, this, motionEvent2);
    }

    public boolean pageDown(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "85", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().pageDown(z);
    }

    public boolean pageUp(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "86", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().pageUp(z);
    }

    public void pauseTimers() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "87", Void.TYPE).isSupported) {
            return;
        }
        getImpl().pauseTimers();
    }

    public void postUrl(String str, byte[] bArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bArr, String.class, byte[].class, Void.TYPE}, this, changeQuickRedirect, "88").isSupported) {
            return;
        }
        getImpl().postUrl(str, bArr);
    }

    public void postVisualStateCallback(long j, VisualStateCallback visualStateCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Long.valueOf(j), visualStateCallback, Long.TYPE, VisualStateCallback.class, Void.TYPE}, this, changeQuickRedirect, "89").isSupported) {
            return;
        }
        getImpl().postVisualStateCallback(j, visualStateCallback);
    }

    public void postWebMessage(WebMessage webMessage, Uri uri) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{webMessage, uri, WebMessage.class, Uri.class, Void.TYPE}, this, changeQuickRedirect, "90").isSupported) {
            return;
        }
        getImpl().postWebMessage(webMessage, uri);
    }

    public void reload() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "91", Void.TYPE).isSupported) {
            return;
        }
        getImpl().reload();
    }

    public void removeJavascriptInterface(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "93", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().removeJavascriptInterface(str2);
    }

    @Override
    public boolean requestFocus(int i, Rect rect) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{Integer.valueOf(i), rect, Integer.TYPE, Rect.class, Boolean.TYPE}, this, changeQuickRedirect, "94");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        getImpl().requestFocus(i, rect);
        return super.requestFocus(i, rect);
    }

    public void requestFocusNodeHref(Message message) {
        Message message2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            message2 = message;
            if (PatchProxy.proxy(message2, this, changeQuickRedirect, "95", Message.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            message2 = message;
        }
        getImpl().requestFocusNodeHref(message2);
    }

    public void requestImageRef(Message message) {
        Message message2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            message2 = message;
            if (PatchProxy.proxy(message2, this, changeQuickRedirect, "96", Message.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            message2 = message;
        }
        getImpl().requestImageRef(message2);
    }

    public WebBackForwardList restoreState(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            PatchProxyResult proxy = PatchProxy.proxy(bundle2, this, changeQuickRedirect, "97", Bundle.class, WebBackForwardList.class);
            if (proxy.isSupported) {
                return (WebBackForwardList) proxy.result;
            }
        } else {
            bundle2 = bundle;
        }
        return getImpl().restoreState(bundle2);
    }

    public void resumeTimers() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "98", Void.TYPE).isSupported) {
            return;
        }
        getImpl().resumeTimers();
    }

    public WebBackForwardList saveState(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            PatchProxyResult proxy = PatchProxy.proxy(bundle2, this, changeQuickRedirect, "99", Bundle.class, WebBackForwardList.class);
            if (proxy.isSupported) {
                return (WebBackForwardList) proxy.result;
            }
        } else {
            bundle2 = bundle;
        }
        return getImpl().saveState(bundle2);
    }

    public void saveWebArchive(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "100", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().saveWebArchive(str2);
    }

    @Override
    public void scrollTo(int i, int i2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "102").isSupported) {
            return;
        }
        this.mEmbedViewContainerBridge.scrollTo(i, i2);
        this.mInternalView.scrollTo(i, i2);
    }

    public void sendJavaEvent(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "103", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().sendJavaEvent(str2);
    }

    public void setAppId(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "104", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().setAppId(str2);
    }

    @Override
    public void setBackgroundColor(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "105", Integer.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().setBackgroundColor(i);
        super.setBackgroundColor(i);
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        DownloadListener downloadListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            downloadListener2 = downloadListener;
            if (PatchProxy.proxy(downloadListener2, this, changeQuickRedirect, "107", DownloadListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            downloadListener2 = downloadListener;
        }
        getImpl().setDownloadListener(downloadListener2);
    }

    public void setFindListener(FindListener findListener) {
        FindListener findListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            findListener2 = findListener;
            if (PatchProxy.proxy(findListener2, this, changeQuickRedirect, "108", FindListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            findListener2 = findListener;
        }
        getImpl().setFindListener(findListener2);
    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean z) {
        WebView webView;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), webView, changeQuickRedirect, "109", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
        }
        super.setHorizontalScrollBarEnabled(z);
        webView.mInternalView.setHorizontalScrollBarEnabled(z);
    }

    public void setInitialScale(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "110", Integer.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().setInitialScale(i);
    }

    public void setInjectJSProvider(InjectJSProvider injectJSProvider) {
        InjectJSProvider injectJSProvider2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            injectJSProvider2 = injectJSProvider;
            if (PatchProxy.proxy(injectJSProvider2, this, changeQuickRedirect, "111", InjectJSProvider.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            injectJSProvider2 = injectJSProvider;
        }
        getImpl().setInjectJSProvider(injectJSProvider2);
    }

    public void setJsDialogFactory(JsDialogFactory jsDialogFactory) {
        JsDialogFactory jsDialogFactory2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            jsDialogFactory2 = jsDialogFactory;
            if (PatchProxy.proxy(jsDialogFactory2, this, changeQuickRedirect, "112", JsDialogFactory.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            jsDialogFactory2 = jsDialogFactory;
        }
        getImpl().setJsDialogFactory(jsDialogFactory2);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        WebView webView;
        ViewGroup.LayoutParams layoutParams2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            layoutParams2 = layoutParams;
            if (PatchProxy.proxy(layoutParams2, webView, changeQuickRedirect, "113", ViewGroup.LayoutParams.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
            layoutParams2 = layoutParams;
        }
        super.setLayoutParams(layoutParams2);
        if (layoutParams2 != null) {
            webView.mInternalView.setLayoutParams(new FrameLayout.LayoutParams(layoutParams2.width, layoutParams2.height));
        }
    }

    public void setMWRenderToken(long j) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Long.valueOf(j), this, changeQuickRedirect, "114", Long.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().setMWRenderToken(j);
    }

    public void setNetworkAvailable(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "115", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().setNetworkAvailable(z);
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        WebView webView;
        View.OnClickListener onClickListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            onClickListener2 = onClickListener;
            if (PatchProxy.proxy(onClickListener2, webView, changeQuickRedirect, "116", View.OnClickListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
            onClickListener2 = onClickListener;
        }
        webView.mInternalView.setOnClickListener(onClickListener2);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        WebView webView;
        View.OnLongClickListener onLongClickListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            onLongClickListener2 = onLongClickListener;
            if (PatchProxy.proxy(onLongClickListener2, webView, changeQuickRedirect, "117", View.OnLongClickListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
            onLongClickListener2 = onLongClickListener;
        }
        webView.mInternalView.setOnLongClickListener(onLongClickListener2);
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        WebView webView;
        View.OnTouchListener onTouchListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            onTouchListener2 = onTouchListener;
            if (PatchProxy.proxy(onTouchListener2, webView, changeQuickRedirect, "118", View.OnTouchListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
            onTouchListener2 = onTouchListener;
        }
        webView.mInternalView.setOnTouchListener(onTouchListener2);
    }

    @Deprecated
    public void setPictureListener(PictureListener pictureListener) {
        PictureListener pictureListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            pictureListener2 = pictureListener;
            if (PatchProxy.proxy(pictureListener2, this, changeQuickRedirect, "119", PictureListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            pictureListener2 = pictureListener;
        }
        getImpl().setPictureListener(pictureListener2);
    }

    public void setRendererPriorityPolicy(int i, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Boolean.valueOf(z), Integer.TYPE, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "120").isSupported) {
            return;
        }
        getImpl().setRendererPriorityPolicy(i, z);
    }

    public void setSoftKeyboardListener(OnSoftKeyboardListener onSoftKeyboardListener, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{onSoftKeyboardListener, Boolean.valueOf(z), OnSoftKeyboardListener.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "122").isSupported) {
            return;
        }
        getImpl().setSoftKeyboardListener(onSoftKeyboardListener, z);
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean z) {
        WebView webView;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webView = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), webView, changeQuickRedirect, "123", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webView = this;
        }
        super.setVerticalScrollBarEnabled(z);
        webView.mInternalView.setVerticalScrollBarEnabled(z);
    }

    public void setWebChromeClient(WebChromeClient webChromeClient) {
        WebChromeClient webChromeClient2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webChromeClient2 = webChromeClient;
            if (PatchProxy.proxy(webChromeClient2, this, changeQuickRedirect, "124", WebChromeClient.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webChromeClient2 = webChromeClient;
        }
        getImpl().setWebChromeClient(webChromeClient2);
    }

    public void setWebViewClient(WebViewClient webViewClient) {
        WebViewClient webViewClient2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webViewClient2 = webViewClient;
            if (PatchProxy.proxy(webViewClient2, this, changeQuickRedirect, "126", WebViewClient.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webViewClient2 = webViewClient;
        }
        getImpl().setWebViewClient(webViewClient2);
    }

    public void setWebViewRenderProcessClient(Executor executor, WebViewRenderProcessClient webViewRenderProcessClient) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{executor, webViewRenderProcessClient, Executor.class, WebViewRenderProcessClient.class, Void.TYPE}, this, changeQuickRedirect, "129").isSupported) {
            getImpl().setWebViewRenderProcessClient(executor, webViewRenderProcessClient);
        }
    }

    public void setXRiverPageId(long j) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Long.valueOf(j), this, changeQuickRedirect, "130", Long.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().setXRiverPageId(j);
    }

    public void stopLoading() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "133", Void.TYPE).isSupported) {
            return;
        }
        getImpl().stopLoading();
    }

    public boolean switchToInProcessMode() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "135", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().switchToInProcessMode();
    }

    public void zoomBy(float f) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Float.valueOf(f), this, changeQuickRedirect, "138", Float.TYPE, Void.TYPE).isSupported) {
            return;
        }
        getImpl().zoomBy(f);
    }

    public boolean zoomIn() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "139", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().zoomIn();
    }

    public boolean zoomOut() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "140", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getImpl().zoomOut();
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public WebView(Context context, AttributeSet attributeSet) {
        this((Context) r1[0], (AttributeSet) r1[1], ((Integer) r1[2]).intValue());
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "1", (objArr = new Object[]{context, attributeSet, null}))) == null) {
            this(context, attributeSet, 0);
        } else {
            proxy.afterSuper(this);
        }
    }

    public void loadUrl(String str) {
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            str2 = str;
            if (PatchProxy.proxy(str2, this, changeQuickRedirect, "74", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            str2 = str;
        }
        getImpl().loadUrl(str2);
    }

    public void saveWebArchive(String str, boolean z, ValueCallback<String> valueCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, Boolean.valueOf(z), valueCallback, String.class, Boolean.TYPE, ValueCallback.class, Void.TYPE}, this, changeQuickRedirect, "101").isSupported) {
            getImpl().saveWebArchive(str, z, valueCallback);
        }
    }

    public void setWebViewRenderProcessClient(WebViewRenderProcessClient webViewRenderProcessClient) {
        WebViewRenderProcessClient webViewRenderProcessClient2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            webViewRenderProcessClient2 = webViewRenderProcessClient;
            if (PatchProxy.proxy(webViewRenderProcessClient2, this, changeQuickRedirect, "128", WebViewRenderProcessClient.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            webViewRenderProcessClient2 = webViewRenderProcessClient;
        }
        getImpl().setWebViewRenderProcessClient(webViewRenderProcessClient2);
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public WebView(Context context, AttributeSet attributeSet, int i) {
        this((Context) r1[0], (AttributeSet) r1[1], ((Integer) r1[2]).intValue(), (Map) r1[3], ((Boolean) r1[4]).booleanValue());
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "2", (objArr = new Object[]{context, attributeSet, Integer.valueOf(i), null, null}))) == null) {
            this(context, attributeSet, i, null, false);
        } else {
            proxy.afterSuper(this);
        }
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public WebView(Context context, AttributeSet attributeSet, int i, Map<String, Object> map, boolean z) {
        this((Context) r1[0], (AttributeSet) r1[1], ((Integer) r1[2]).intValue(), (Map) r1[3], ((Boolean) r1[4]).booleanValue(), (WebViewConfig) r1[5]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "3", (objArr = new Object[]{context, attributeSet, Integer.valueOf(i), map, Boolean.valueOf(z), null}))) == null) {
            this(context, attributeSet, i, map, z, null);
        } else {
            proxy.afterSuper(this);
        }
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public WebView(Context context, AttributeSet attributeSet, int i, Map<String, Object> map, boolean z, WebViewConfig webViewConfig) {
        super((Context) r1[0], (AttributeSet) r1[1], ((Integer) r1[2]).intValue());
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "4", (objArr = new Object[]{context, attributeSet, Integer.valueOf(i), map, Boolean.valueOf(z), webViewConfig}))) != null) {
            proxy.afterSuper(this);
            return;
        }
        super(context, attributeSet, i);
        if (!sEnableDispatchKeyEvent) {
            sEnableDispatchKeyEvent = ConfigService.getInstance().getConfigBoolean("myweb_enable_dispatch_key_event", true);
        }
        EmbedViewContainer embedViewContainer = new EmbedViewContainer(context);
        addViewInLayout(embedViewContainer, 0, new FrameLayout.LayoutParams(-1, -1));
        this.mInternalView = new WebViewInternalForM(context, attributeSet, i);
        if (map != null && getImpl() != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                getImpl().addJavascriptInterface(entry.getValue(), entry.getKey());
            }
        }
        EmbedViewContainer embedViewContainer2 = new EmbedViewContainer(context);
        this.mTopViewContainer = embedViewContainer2;
        embedViewContainer2.setIsTopContainer();
        EmbedViewContainerBridge embedViewContainerBridge = new EmbedViewContainerBridge(embedViewContainer, this.mTopViewContainer, this.mInternalView);
        this.mEmbedViewContainerBridge = embedViewContainerBridge;
        this.mInternalView.init(this, embedViewContainerBridge, webViewConfig);
        addViewInLayout(this.mInternalView, 1, new FrameLayout.LayoutParams(-1, -1));
        addViewInLayout(this.mTopViewContainer, 2, new FrameLayout.LayoutParams(-1, -1));
    }
}
