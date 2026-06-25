//
// Decompiled by Jadx - 971ms
//
package com.alibaba.xriver.android.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.alibaba.ariver.app.api.AppContext;
import com.alibaba.ariver.app.api.AppUIContext;
import com.alibaba.ariver.app.api.EntryInfo;
import com.alibaba.ariver.app.api.Page;
import com.alibaba.ariver.app.api.permission.RVNativePermissionRequestProxy;
import com.alibaba.ariver.app.api.point.activity.ActivityOnPausePoint;
import com.alibaba.ariver.app.api.point.activity.ActivityResultPoint;
import com.alibaba.ariver.app.api.ui.StatusBarUtils;
import com.alibaba.ariver.app.api.ui.fragment.IFragmentManager;
import com.alibaba.ariver.app.api.ui.fragment.RVFragment;
import com.alibaba.ariver.app.api.ui.loading.SplashUtils;
import com.alibaba.ariver.app.api.ui.loading.SplashView;
import com.alibaba.ariver.app.api.ui.navigation.NavigationBar;
import com.alibaba.ariver.app.api.ui.navigation.NavigationBarPolicy;
import com.alibaba.ariver.app.api.ui.navigation.Visibility;
import com.alibaba.ariver.app.api.ui.tabbar.TabBar;
import com.alibaba.ariver.kernel.api.extension.ExtensionPoint;
import com.alibaba.ariver.kernel.common.RVProxy;
import com.alibaba.ariver.kernel.common.service.RVConfigService;
import com.alibaba.ariver.kernel.common.service.executor.ExecutorType;
import com.alibaba.ariver.kernel.common.utils.BundleUtils;
import com.alibaba.ariver.kernel.common.utils.ExecutorUtils;
import com.alibaba.ariver.kernel.common.utils.JSONUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.ariver.kernel.common.utils.RVTraceUtils;
import com.alibaba.ariver.kernel.common.utils.ReflectUtils;
import com.alibaba.ariver.kernel.common.utils.UrlUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.xriver.android.UEPController;
import com.alibaba.xriver.android.bridge.CRVNativeBridge;
import com.alibaba.xriver.android.bridge.RenderLoadRefactorConfigManager;
import com.alibaba.xriver.android.cube.CubeManager;
import com.alibaba.xriver.android.cube.CubeOverlayView;
import com.alibaba.xriver.android.cube.CubePreCreateViewPool;
import com.alibaba.xriver.android.node.CRVApp;
import com.alibaba.xriver.android.node.CRVPage;
import com.alibaba.xriver.android.proxy.CRVWindowProxy;
import com.alibaba.xriver.android.ui.overlay.SeaViewOverlayManager;
import com.alibaba.xriver.android.utils.ExitEvent;
import com.alibaba.xriver.android.utils.TaskMonitorRunnable;
import com.alipay.dexaop.DexAOPCenter;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.content.BroadcastReceiver_onReceive_androidcontentContext;
import com.alipay.dexaop.stub.java.lang.Runnable_run__stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.common.utils.ConfigUtils;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.framework.service.ext.openplatform.app.App;
import com.alipay.mobile.nebula.util.H5DimensionUtil;
import com.alipay.mobile.nebulaappproxy.api.H5AppProxyUtil;
import com.alipay.mobile.nebulaintegration.obfuscated.aa;
import com.alipay.mobile.nebulax.NebulaViewGetter;
import com.alipay.mobile.nebulax.engine.renderng.CubeOverlayViewHolder;
import com.alipay.mobile.nebulax.engine.renderng.CubeViewHolder;
import com.alipay.mobile.nebulax.integration.api.Util;
import com.alipay.mobile.nebulax.integration.base.halfscreen.HalfscreenUtils;
import com.alipay.mobile.nebulax.integration.mpaas.R;
import com.alipay.mobile.nebulax.resource.api.appinfo.AppType;
import com.alipay.mobile.nebulax.resource.api.util.NXResourceUtils;
import com.alipay.mobile.nebulax.resource.api.util.PreloadUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public abstract class XRiverLifecycleHelperBase implements CRVWindowProxy.Handler {
    private static final String TAG = "XRIVER:Android:XRiverLifecycleHelperBase";
    public static ChangeQuickRedirect 支;
    private long delayCloseTimeDuration;
    private long firstEnterShowLoadingStartTime;
    private boolean hasUpdateInfo;
    protected FragmentActivity mActivity;
    protected CRVApp mCRVApp;
    private BroadcastReceiver mConfigChangedReceiver;
    protected IFragmentManager mFragmentManager;
    private boolean mHasPushScene;
    private Map<Long, CubeViewHolder> mOverlayViewHolderMap;
    private final List<String> mTabBarHashList;

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 1 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final XRiverLifecycleHelperBase a;

        public 1(XRiverLifecycleHelperBase xRiverLifecycleHelperBase) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = xRiverLifecycleHelperBase;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            RVLogger.d(XRiverLifecycleHelperBase.TAG, "notifyWindowReadyCallback in next run loop");
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.a;
            CRVWindowProxy.Callback.notifyWindowReadyCallback(xRiverLifecycleHelperBase.mCRVApp.getNodeId(), xRiverLifecycleHelperBase);
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 1.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(1.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 10 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final XRiverLifecycleHelperBase a;

        public 10(XRiverLifecycleHelperBase xRiverLifecycleHelperBase) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = xRiverLifecycleHelperBase;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.a;
            AppContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null) {
                appContext.destroy();
            } else {
                xRiverLifecycleHelperBase.mActivity.finish();
            }
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 10.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(10.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 11 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final CRVPage a;
        public final XRiverLifecycleHelperBase b;

        public 11(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, CRVPage cRVPage) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, cRVPage})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = cRVPage;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.b;
            AppContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null && !xRiverLifecycleHelperBase.mActivity.isFinishing() && !xRiverLifecycleHelperBase.mCRVApp.isNativeSessionExited()) {
                appContext.createTabBar(this.a);
            }
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 11.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(11.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 12 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final int a;
        public final XRiverLifecycleHelperBase b;

        public 12(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, int i) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, Integer.valueOf(i)})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = i;
            }
        }

        private void __run_stub_private() {
            TabBar tabBar;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.b;
            AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if ((appContext instanceof AppUIContext) && (tabBar = appContext.getTabBar()) != null) {
                tabBar.setEnableTabClick(true);
                tabBar.switchTab(this.a, 3);
                xRiverLifecycleHelperBase.handleOverlayTabBarIfNeeded((CRVPage) tabBar.getSelectedPage());
            }
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 12.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(12.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 2 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final XRiverLifecycleHelperBase a;

        public 2(XRiverLifecycleHelperBase xRiverLifecycleHelperBase) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = xRiverLifecycleHelperBase;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.a;
            if (xRiverLifecycleHelperBase.hasUpdateInfo) {
                return;
            }
            EntryInfo entryInfo = new EntryInfo();
            App openPlatApp = H5AppProxyUtil.getOpenPlatApp(xRiverLifecycleHelperBase.mCRVApp.getAppId());
            if (openPlatApp != null) {
                entryInfo.iconUrl = openPlatApp.getIconUrl("");
                entryInfo.title = openPlatApp.getName("");
                boolean equalsIgnoreCase = "no".equalsIgnoreCase(NXResourceUtils.getConfig("h5_loadpageslogan"));
                entryInfo.desc = openPlatApp.getDesc("");
                if (!equalsIgnoreCase && openPlatApp.isSmallProgram()) {
                    entryInfo.slogan = openPlatApp.getSlogan("");
                }
            }
            ShowLoadingRunnable showLoadingRunnable = new ShowLoadingRunnable(xRiverLifecycleHelperBase, entryInfo);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(showLoadingRunnable);
            ExecutorUtils.runOnMain(showLoadingRunnable);
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 2.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(2.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 3 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final EntryInfo a;
        public final XRiverLifecycleHelperBase b;

        public 3(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, EntryInfo entryInfo) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, entryInfo})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = entryInfo;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.b;
            AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null && !xRiverLifecycleHelperBase.mActivity.isFinishing()) {
                SplashView splashView = appContext.getSplashView();
                if (splashView == null) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but splashView == null!!");
                    return;
                }
                if (xRiverLifecycleHelperBase.mHasPushScene) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "updateLoadingInfo but hasPushScene");
                    return;
                }
                SplashView.Status status = splashView.getStatus();
                SplashView.Status status2 = SplashView.Status.LOADING;
                EntryInfo entryInfo = this.a;
                if (status != status2) {
                    splashView.showLoading(entryInfo);
                    return;
                } else {
                    splashView.update(entryInfo);
                    return;
                }
            }
            RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but appContext == null!!");
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 3.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(3.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 4 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final int a;
        public final String b;
        public final XRiverLifecycleHelperBase c;

        public 4(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, int i, String str) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, Integer.valueOf(i), str})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.c = xRiverLifecycleHelperBase;
            this.a = i;
            this.b = str;
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.c;
            AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null && !xRiverLifecycleHelperBase.mActivity.isFinishing()) {
                SplashView splashView = appContext.getSplashView();
                if (splashView == null) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "showError but appContext.getSplashView == null!");
                    return;
                } else {
                    splashView.showError(String.valueOf(this.a), String.valueOf(this.b), (Map) null);
                    return;
                }
            }
            RVLogger.w(XRiverLifecycleHelperBase.TAG, "showError but appContext == null!");
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 4.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(4.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 6 extends BroadcastReceiver implements BroadcastReceiver_onReceive_androidcontentContext.androidcontentIntent_stub {
        public static ChangeQuickRedirect 支;
        public final CubeOverlayViewHolder a;
        public final XRiverLifecycleHelperBase b;

        @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
        public class 1 implements Runnable_run__stub, Runnable {
            public static ChangeQuickRedirect 支;
            public final 6 a;

            public 1(6 r4) {
                ConstructorCode proxy;
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{r4})) != null) {
                    proxy.afterSuper(this);
                } else {
                    this.a = r4;
                }
            }

            private void __run_stub_private() {
                int f;
                ViewGroup.LayoutParams layoutParams;
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                    return;
                }
                6 r0 = this.a;
                if (((CubeViewHolder) r0.a).mCubeView != null && (f = aa.f(r0.b.mActivity)) != 0 && (layoutParams = ((CubeViewHolder) r0.a).mCubeView.getLayoutParams()) != null) {
                    layoutParams.width = f;
                    ((CubeViewHolder) r0.a).mCubeView.setLayoutParams(layoutParams);
                    RVLogger.d(XRiverLifecycleHelperBase.TAG, "overlay view width updated to: " + f);
                }
            }

            public void __run_stub() {
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                    __run_stub_private();
                }
            }

            @Override
            public void run() {
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                    if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 1.class) {
                        __run_stub_private();
                    } else {
                        DexAOPEntry.java_lang_Runnable_run_proxy(1.class, this);
                    }
                }
            }
        }

        public 6(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, CubeOverlayViewHolder cubeOverlayViewHolder) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, cubeOverlayViewHolder})) != null) {
                this.a = (CubeOverlayViewHolder) proxy.getFieldValue(0);
                this.b = (XRiverLifecycleHelperBase) proxy.getFieldValue(1);
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = cubeOverlayViewHolder;
            }
        }

        private void __onReceive_stub_private(Context context, Intent intent) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{context, intent, Context.class, Intent.class, Void.TYPE}, this, changeQuickRedirect, "2").isSupported) {
                return;
            }
            RVLogger.d(XRiverLifecycleHelperBase.TAG, "overlay received config changed broadcast");
            XRiverLifecycleHelperBase$6$1 r4 = new XRiverLifecycleHelperBase$6$1(this);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r4);
            ExecutorUtils.runOnMain(r4);
        }

        public void __onReceive_stub(Context context, Intent intent) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{context, intent, Context.class, Intent.class, Void.TYPE}, this, changeQuickRedirect, "1").isSupported) {
                __onReceive_stub_private(context, intent);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{context, intent, Context.class, Intent.class, Void.TYPE}, this, changeQuickRedirect, "3").isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 6.class) {
                    __onReceive_stub_private(context, intent);
                } else {
                    DexAOPEntry.android_content_BroadcastReceiver_onReceive_proxy(6.class, this, context, intent);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 7 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final RelativeLayout a;
        public final CubeOverlayViewHolder b;
        public final RelativeLayout.LayoutParams c;

        public 7(RelativeLayout relativeLayout, CubeOverlayViewHolder cubeOverlayViewHolder, RelativeLayout.LayoutParams layoutParams) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{relativeLayout, cubeOverlayViewHolder, layoutParams})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.a = relativeLayout;
            this.b = cubeOverlayViewHolder;
            this.c = layoutParams;
        }

        private void __run_stub_private() {
            RelativeLayout relativeLayout;
            CubeOverlayViewHolder cubeOverlayViewHolder;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) && (relativeLayout = this.a) != null && (cubeOverlayViewHolder = this.b) != null && cubeOverlayViewHolder.getCubeView() != null && cubeOverlayViewHolder.getCubeView().getParent() == null) {
                relativeLayout.addView((View) cubeOverlayViewHolder.getCubeView(), (ViewGroup.LayoutParams) this.c);
                cubeOverlayViewHolder.setOverlayViewVisibility(0);
            }
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 7.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(7.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 8 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final ViewGroup a;
        public final CubeOverlayViewHolder b;
        public final XRiverLifecycleHelperBase c;

        public 8(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, ViewGroup viewGroup, CubeOverlayViewHolder cubeOverlayViewHolder) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, viewGroup, cubeOverlayViewHolder})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.c = xRiverLifecycleHelperBase;
            this.a = viewGroup;
            this.b = cubeOverlayViewHolder;
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            int f = aa.f(this.c.mActivity);
            ViewGroup viewGroup = this.a;
            CubeOverlayViewHolder cubeOverlayViewHolder = this.b;
            if (f != 0) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(f, H5DimensionUtil.getScreenHeight(viewGroup.getContext()));
                layoutParams.gravity = 81;
                viewGroup.addView(((CubeViewHolder) cubeOverlayViewHolder).mCubeView, 0, layoutParams);
                ((CubeViewHolder) cubeOverlayViewHolder).mCubeView.bringToFront();
            } else {
                cubeOverlayViewHolder.initOverlayView(viewGroup);
            }
            cubeOverlayViewHolder.setOverlayViewVisibility(0);
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 8.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(8.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 9 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final CRVPage a;
        public final boolean b;
        public final XRiverLifecycleHelperBase c;

        public 9(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, CRVPage cRVPage, boolean z) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, cRVPage, Boolean.valueOf(z)})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.c = xRiverLifecycleHelperBase;
            this.a = cRVPage;
            this.b = z;
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            this.c.mFragmentManager.exitPage(this.a, this.b, true);
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 9.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(9.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class HideLoadingRunnable implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final String a;
        public final XRiverLifecycleHelperBase b;

        public HideLoadingRunnable(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, String str) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, str})) != null) {
                this.b = (XRiverLifecycleHelperBase) proxy.getFieldValue(0);
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = str;
            }
        }

        private void __run_stub_private() {
            Page activePage;
            NavigationBar navigationBar;
            View view;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.b;
            AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null && !xRiverLifecycleHelperBase.mActivity.isFinishing()) {
                SplashView splashView = appContext.getSplashView();
                if (splashView == null) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but splashView == null!!");
                    return;
                }
                splashView.exit(this.a, (SplashView.ExitListener) null);
                if (AppType.valueOf(xRiverLifecycleHelperBase.mCRVApp.getAppType()).isTinyGame() && (activePage = xRiverLifecycleHelperBase.mCRVApp.getActivePage()) != null && NavigationBarPolicy.navigationBarOperatorReusedForApp(xRiverLifecycleHelperBase.mCRVApp) && activePage.getPageContext() != null && (navigationBar = activePage.getPageContext().getNavigationBar()) != null && (view = navigationBar.getView()) != null && view.getVisibility() != 0) {
                    RVLogger.d(XRiverLifecycleHelperBase.TAG, "hideLoading(): superSplash, navigationBarView is not visible, show it");
                    navigationBar.setVisibility(Visibility.VISIBLE);
                    return;
                }
                return;
            }
            RVLogger.w(XRiverLifecycleHelperBase.TAG, "try to hide loading but appContext == null!!");
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != HideLoadingRunnable.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(HideLoadingRunnable.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class PushSceneInnerTask extends TaskMonitorRunnable {
        public static ChangeQuickRedirect 支;
        public final CRVPage b;
        public final boolean c;
        public final Bundle d;
        public final long e;
        public final XRiverLifecycleHelperBase f;

        /* JADX WARN: Illegal instructions before constructor call */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public PushSceneInnerTask(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, CRVPage cRVPage, boolean z, Bundle bundle, long j) {
            super((String) r1[0], (String) r1[1]);
            Object[] objArr;
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", (objArr = new Object[]{xRiverLifecycleHelperBase, cRVPage, Boolean.valueOf(z), bundle, Long.valueOf(j)}))) != null) {
                this.f = (XRiverLifecycleHelperBase) proxy.getFieldValue(0);
                proxy.afterSuper(this);
                return;
            }
            this.f = xRiverLifecycleHelperBase;
            super("scenePush", "activityCreate");
            if (cRVPage != null && cRVPage.getApp() != null && Util.isFirstScreen(cRVPage)) {
                bindNodeId(cRVPage.getApp().getNodeId());
            }
            this.b = cRVPage;
            this.c = z;
            this.d = bundle;
            this.e = j;
        }

        public void execute() {
            String str;
            SplashView splashView;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.f;
            if (xRiverLifecycleHelperBase.mActivity.isFinishing()) {
                RVLogger.d(XRiverLifecycleHelperBase.TAG, "pushScene but activity finishing!");
                return;
            }
            if (!xRiverLifecycleHelperBase.mHasPushScene) {
                xRiverLifecycleHelperBase.mHasPushScene = true;
            }
            RVLogger.d(XRiverLifecycleHelperBase.TAG, "loading_delay-->pushSceneInnerTask, firstLoading, TinyBetterLoading: " + BundleUtils.getBoolean(xRiverLifecycleHelperBase.mCRVApp.getStartParams(), "TinyBetterLoading", false));
            RVTraceUtils.traceBeginSection("RV_pushScene");
            boolean equalsIgnoreCase = "YES".equalsIgnoreCase(BundleUtils.getString(this.d, "paladinMode"));
            if (equalsIgnoreCase) {
                str = "Yes";
            } else {
                str = "No";
            }
            RVLogger.d(XRiverLifecycleHelperBase.TAG, "handleFullScreen is gameMode: ".concat(str));
            if (equalsIgnoreCase) {
                xRiverLifecycleHelperBase.mActivity.getWindow().setFlags(1024, 1024);
                xRiverLifecycleHelperBase.mActivity.getWindow().getDecorView().setSystemUiVisibility(4102);
            }
            Util.adjustEdgeToEdge(xRiverLifecycleHelperBase.mActivity.getWindow().getDecorView(), R.id.nebulax_root_view);
            boolean useSuperSplash = SplashUtils.useSuperSplash(xRiverLifecycleHelperBase.mCRVApp.getStartParams());
            if (xRiverLifecycleHelperBase.mCRVApp.getAppContext() == null) {
                splashView = null;
            } else {
                splashView = xRiverLifecycleHelperBase.mCRVApp.getAppContext().getSplashView();
            }
            if (!useSuperSplash && !BundleUtils.getBoolean(xRiverLifecycleHelperBase.mCRVApp.getStartParams(), "TinyBetterLoading", false) && splashView != null && splashView.getStatus() == SplashView.Status.LOADING) {
                splashView.exit("firstSceneReady", (SplashView.ExitListener) null);
            } else if (BundleUtils.getBoolean(xRiverLifecycleHelperBase.mCRVApp.getStartParams(), "TinyBetterLoading", false)) {
                int i = BundleUtils.getInt(xRiverLifecycleHelperBase.mCRVApp.getStartParams(), "enableStaticPluginOnDemand", 0);
                JSONObject configJSONObject = ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigJSONObject("ta_loading_timeout_duration");
                if (i == 2) {
                    HideLoadingRunnable hideLoadingRunnable = new HideLoadingRunnable(xRiverLifecycleHelperBase, "betterLoadingDefaultTimeOut");
                    DexAOPEntry.java_lang_Runnable_newInstance_Created(hideLoadingRunnable);
                    ExecutorUtils.runOnMain(hideLoadingRunnable, JSONUtils.getLong(configJSONObject, "for_demand2", 30000L));
                } else {
                    HideLoadingRunnable hideLoadingRunnable2 = new HideLoadingRunnable(xRiverLifecycleHelperBase, "betterLoadingDefaultTimeOut");
                    DexAOPEntry.java_lang_Runnable_newInstance_Created(hideLoadingRunnable2);
                    ExecutorUtils.runOnMain(hideLoadingRunnable2, JSONUtils.getLong(configJSONObject, "for_default", 5000L));
                }
            }
            RVFragment readyFragment = xRiverLifecycleHelperBase.mFragmentManager.getReadyFragment();
            boolean isAdded = readyFragment.isAdded();
            CRVPage cRVPage = this.b;
            if (!isAdded) {
                RVLogger.d(XRiverLifecycleHelperBase.TAG, "fragment not added");
                Bundle bundle = new Bundle();
                bundle.putLong("ariverAppInstanceId", xRiverLifecycleHelperBase.mCRVApp.getNodeId());
                bundle.putLong("ariverPageInstanceId", cRVPage.getNodeId());
                readyFragment.setArguments(bundle);
            } else {
                RVLogger.d(XRiverLifecycleHelperBase.TAG, "fragment is added," + readyFragment);
                readyFragment.setPage(cRVPage);
            }
            if (this.e > 0) {
                readyFragment.addLifeCycleListener(new RVFragment.FragmentLifecycleListener(this) {
                    public static ChangeQuickRedirect 支;
                    public final PushSceneInnerTask a;

                    {
                        ConstructorCode proxy;
                        ChangeQuickRedirect changeQuickRedirect2 = 支;
                        if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                            this.a = (PushSceneInnerTask) proxy.getFieldValue(0);
                            proxy.afterSuper(this);
                        } else {
                            this.a = this;
                        }
                    }

                    public void onCreateView(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
                        ChangeQuickRedirect changeQuickRedirect2 = 支;
                        if (changeQuickRedirect2 != null && PatchProxy.proxy(new Object[]{fragmentManager, fragment, FragmentManager.class, Fragment.class, Void.TYPE}, this, changeQuickRedirect2, "1").isSupported) {
                            return;
                        }
                        CRVNativeBridge.nativeInvokeCallback(this.a.e, (Object) null);
                    }
                });
            }
            xRiverLifecycleHelperBase.mFragmentManager.pushPage(cRVPage, readyFragment, this.c);
            xRiverLifecycleHelperBase.handleOverlayTabBarIfNeeded(cRVPage);
            RVTraceUtils.traceEndSection("RV_pushScene");
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class ShowLoadingRunnable implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final EntryInfo a;
        public final XRiverLifecycleHelperBase b;

        public ShowLoadingRunnable(XRiverLifecycleHelperBase xRiverLifecycleHelperBase, EntryInfo entryInfo) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{xRiverLifecycleHelperBase, entryInfo})) != null) {
                this.b = (XRiverLifecycleHelperBase) proxy.getFieldValue(0);
                proxy.afterSuper(this);
            } else {
                this.b = xRiverLifecycleHelperBase;
                this.a = entryInfo;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            XRiverLifecycleHelperBase xRiverLifecycleHelperBase = this.b;
            AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
            if (appContext != null && !xRiverLifecycleHelperBase.mActivity.isFinishing()) {
                SplashView splashView = appContext.getSplashView();
                if (splashView == null) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but splashView == null!!");
                    return;
                } else if (xRiverLifecycleHelperBase.mHasPushScene) {
                    RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but hasPushScene");
                    return;
                } else {
                    splashView.showLoading(this.a);
                    return;
                }
            }
            RVLogger.w(XRiverLifecycleHelperBase.TAG, "showLoading but appContext == null!!");
        }

        public void __run_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                __run_stub_private();
            }
        }

        @Override
        public void run() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != ShowLoadingRunnable.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(ShowLoadingRunnable.class, this);
                }
            }
        }
    }

    public XRiverLifecycleHelperBase(com.alibaba.ariver.app.api.App app, IFragmentManager iFragmentManager, FragmentActivity fragmentActivity) {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "1", new Object[]{app, iFragmentManager, fragmentActivity})) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.mTabBarHashList = new ArrayList();
        this.mHasPushScene = false;
        this.hasUpdateInfo = false;
        this.delayCloseTimeDuration = 0L;
        this.firstEnterShowLoadingStartTime = 0L;
        this.mCRVApp = (CRVApp) app;
        this.mFragmentManager = iFragmentManager;
        this.mActivity = fragmentActivity;
        this.mOverlayViewHolderMap = new ConcurrentHashMap();
        if (StatusBarUtils.isSupport() && StatusBarUtils.isConfigSupport()) {
            StatusBarUtils.setTransparentColor(this.mActivity, 0x33000000);
        }
        if (RenderLoadRefactorConfigManager.INSTANCE.delayNotifyWindowReadyToNextRunLoop() && this.mCRVApp.isRenderDecoupleFromActivity()) {
            XRiverLifecycleHelperBase$1 r4 = new XRiverLifecycleHelperBase$1(this);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r4);
            ExecutorUtils.runOnMain(r4, 0L);
        } else {
            RVLogger.d(TAG, "notifyWindowReadyCallback immediately");
            CRVWindowProxy.Callback.notifyWindowReadyCallback(this.mCRVApp.getNodeId(), this);
        }
    }

    public void addOverlayView(long j, String str, String str2, CRVPage cRVPage) {
        CubeOverlayViewHolder.OverlayViewType overlayViewType;
        int i;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Long.valueOf(j), str, str2, cRVPage, Long.TYPE, String.class, String.class, CRVPage.class, Void.TYPE}, this, changeQuickRedirect, "3").isSupported) {
            return;
        }
        RVLogger.d(TAG, "addOverlayView: (" + str + "), viewId: " + j + ", height: " + str2);
        if ("overlay".equalsIgnoreCase(str)) {
            overlayViewType = CubeOverlayViewHolder.OverlayViewType.OVERLAY;
        } else if ("tabbar".equalsIgnoreCase(str)) {
            overlayViewType = CubeOverlayViewHolder.OverlayViewType.TAB_BAR;
        } else {
            overlayViewType = null;
        }
        if (overlayViewType != null) {
            for (Map.Entry<Long, CubeViewHolder> entry : this.mOverlayViewHolderMap.entrySet()) {
                if (entry != null && entry.getValue().type.equals(overlayViewType)) {
                    RVLogger.w(TAG, "already create overlay type " + str + " on holder " + entry);
                    StringBuilder sb = new StringBuilder("addOverlayView Already create ");
                    sb.append(str);
                    RVLogger.w(TAG, sb.toString());
                    return;
                }
            }
        }
        if ("tabbar".equalsIgnoreCase(str) || "overlay".equalsIgnoreCase(str)) {
            CubeOverlayView createView = CubeManager.INSTANCE.createView(this.mCRVApp, str, new NebulaViewGetter(this.mActivity));
            RVLogger.d(TAG, "addOverlayView in new way get preview " + createView);
            if (createView != null && createView.getCubeViewHolder() != null) {
                d(j, createView.getCubeViewHolder(), createView.getCubeViewHolder().type);
                return;
            }
        }
        if ("tabbar".equalsIgnoreCase(str)) {
            CubeOverlayViewHolder cubeOverlayViewHolder = (CubeOverlayViewHolder) this.mCRVApp.getData(CubeViewHolder.class);
            if (cubeOverlayViewHolder == null || cubeOverlayViewHolder.type != null) {
                cubeOverlayViewHolder = (CubeOverlayViewHolder) CubePreCreateViewPool.g().prefetchCubeViewHolder(true);
            }
            CubeOverlayViewHolder.OverlayViewType overlayViewType2 = CubeOverlayViewHolder.OverlayViewType.TAB_BAR;
            cubeOverlayViewHolder.type = overlayViewType2;
            String valueOf = String.valueOf(j);
            cubeOverlayViewHolder.viewId = valueOf;
            d(j, cubeOverlayViewHolder, overlayViewType2);
            RelativeLayout viewOfNebulaXWrapperView = new NebulaViewGetter(this.mActivity).getViewOfNebulaXWrapperView();
            cubeOverlayViewHolder.setNativeDOMToken(valueOf, this.mCRVApp.getAppId());
            cubeOverlayViewHolder.setAntCubeClient(this.mCRVApp, (Page) null);
            if (str2 != null && !str2.contains("%")) {
                i = Integer.parseInt(str2);
            } else {
                i = -1;
            }
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, i);
            layoutParams.addRule(12);
            XRiverLifecycleHelperBase$7 r10 = new XRiverLifecycleHelperBase$7(viewOfNebulaXWrapperView, cubeOverlayViewHolder, layoutParams);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r10);
            ExecutorUtils.runOnMainAtFrontOfQueue(r10);
            return;
        }
        if ("overlay".equalsIgnoreCase(str)) {
            CubeOverlayViewHolder cubeOverlayViewHolder2 = (CubeOverlayViewHolder) CubePreCreateViewPool.g().prefetchCubeViewHolder(true);
            CubeOverlayViewHolder.OverlayViewType overlayViewType3 = CubeOverlayViewHolder.OverlayViewType.OVERLAY;
            cubeOverlayViewHolder2.type = overlayViewType3;
            cubeOverlayViewHolder2.setNativeDOMToken(String.valueOf(j), this.mCRVApp.getAppId());
            cubeOverlayViewHolder2.setAntCubeClient(this.mCRVApp, (Page) null);
            d(j, cubeOverlayViewHolder2, overlayViewType3);
            ViewGroup overlayContainer = CubeManager.INSTANCE.getOverlayContainer(new NebulaViewGetter(this.mActivity));
            if (overlayContainer != null) {
                XRiverLifecycleHelperBase$8 r102 = new XRiverLifecycleHelperBase$8(this, overlayContainer, cubeOverlayViewHolder2);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(r102);
                ExecutorUtils.runOnMainAtFrontOfQueue(r102);
                if (this.mActivity != null && this.mConfigChangedReceiver == null) {
                    this.mConfigChangedReceiver = new XRiverLifecycleHelperBase$6(this, cubeOverlayViewHolder2);
                    DexAOPEntry.android_support_v4_content_LocalBroadcastManager_registerReceiver_proxy(LocalBroadcastManager.getInstance(this.mActivity), this.mConfigChangedReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED_NEBULA"));
                    return;
                }
                return;
            }
            return;
        }
        if ("seaview".equalsIgnoreCase(str)) {
            if (HalfscreenUtils.isHalfScreenApp(this.mCRVApp.getAppId(), this.mCRVApp.getStartParams())) {
                RVLogger.w(TAG, "not support seaview on half screen app");
                return;
            }
            RVLogger.d(TAG, "received seaview type on: " + cRVPage);
            try {
                SeaViewOverlayManager seaViewOverlayManager = new SeaViewOverlayManager(this.mCRVApp, cRVPage, str2, String.valueOf(j));
                d(j, seaViewOverlayManager.initCubeViewHolder(), CubeOverlayViewHolder.OverlayViewType.SEA_VIEW);
                seaViewOverlayManager.initView(this.mActivity);
                this.mCRVApp.putBooleanValue(SeaViewOverlayManager.Companion.getSEAVIEW_ADDED(), true);
            } catch (Exception e) {
                RVLogger.e(TAG, "create SeaView Exception", e);
            }
        }
    }

    public void d(long j, CubeOverlayViewHolder cubeOverlayViewHolder, CubeOverlayViewHolder.OverlayViewType overlayViewType) {
        CubeOverlayViewHolder remove;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Long.valueOf(j), cubeOverlayViewHolder, overlayViewType, Long.TYPE, CubeOverlayViewHolder.class, CubeOverlayViewHolder.OverlayViewType.class, Void.TYPE}, this, changeQuickRedirect, "6").isSupported) {
            return;
        }
        long j2 = -1;
        for (Map.Entry<Long, CubeViewHolder> entry : this.mOverlayViewHolderMap.entrySet()) {
            if (entry != null && entry.getValue().type == overlayViewType) {
                j2 = entry.getKey().longValue();
            }
        }
        if (j2 != -1 && (remove = this.mOverlayViewHolderMap.remove(Long.valueOf(j2))) != null) {
            remove.onOverlayDestroy();
        }
        this.mOverlayViewHolderMap.put(Long.valueOf(j), cubeOverlayViewHolder);
    }

    public void destroyOverlayView(long j) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            if (PatchProxy.proxy(Long.valueOf(j), xRiverLifecycleHelperBase, changeQuickRedirect, "7", Long.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverLifecycleHelperBase = this;
        }
        CubeOverlayViewHolder cubeOverlayViewHolder = xRiverLifecycleHelperBase.mOverlayViewHolderMap.get(Long.valueOf(j));
        if (cubeOverlayViewHolder != null) {
            cubeOverlayViewHolder.onOverlayDestroy();
        }
    }

    public void e(int i) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            if (PatchProxy.proxy(Integer.valueOf(i), xRiverLifecycleHelperBase, changeQuickRedirect, "8", Integer.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverLifecycleHelperBase = this;
        }
        Iterator<Map.Entry<Long, CubeViewHolder>> it = xRiverLifecycleHelperBase.mOverlayViewHolderMap.entrySet().iterator();
        while (it.hasNext()) {
            CubeOverlayViewHolder value = it.next().getValue();
            if (value != null && value.type == CubeOverlayViewHolder.OverlayViewType.TAB_BAR) {
                RVLogger.d(TAG, "handleOverlayTabBarIfNeeded(tabbar) " + i);
                value.setOverlayViewVisibility(i);
            }
        }
    }

    public void exitScene(CRVPage cRVPage, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cRVPage, Boolean.valueOf(z), CRVPage.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "9").isSupported) {
            return;
        }
        RVLogger.d(TAG, "exitScene page: " + cRVPage);
        XRiverLifecycleHelperBase$9 r0 = new XRiverLifecycleHelperBase$9(this, cRVPage, z);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r0);
        ExecutorUtils.runOnMain(r0);
    }

    public void exitWindow() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "10", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "exitWindow");
        ExitEvent.of(this.mActivity).tagging("Helper.exitWindow");
        XRiverLifecycleHelperBase$10 r0 = new XRiverLifecycleHelperBase$10(this);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r0);
        ExecutorUtils.runOnMain(r0);
    }

    public CRVApp getApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "11", CRVApp.class);
            if (proxy.isSupported) {
                return (CRVApp) proxy.result;
            }
        }
        return this.mCRVApp;
    }

    public Map<Long, CubeViewHolder> getCubeViewHolderMap() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "12", Map.class);
            if (proxy.isSupported) {
                return (Map) proxy.result;
            }
        }
        return this.mOverlayViewHolderMap;
    }

    public int getIndexByPageUrl(String str) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        String str2;
        TabBar tabBar;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, xRiverLifecycleHelperBase, changeQuickRedirect, "13", String.class, Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        } else {
            xRiverLifecycleHelperBase = this;
            str2 = str;
        }
        if ((xRiverLifecycleHelperBase.mCRVApp.getAppContext() instanceof AppUIContext) && (tabBar = xRiverLifecycleHelperBase.mCRVApp.getAppContext().getTabBar()) != null) {
            return tabBar.getIndexByPage(UrlUtils.getHash(str2));
        }
        return -1;
    }

    public Map<Long, CubeViewHolder> getOverlayViewHolderMap() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "14", Map.class);
            if (proxy.isSupported) {
                return (Map) proxy.result;
            }
        }
        return this.mOverlayViewHolderMap;
    }

    public void handleOverlayTabBarIfNeeded(CRVPage cRVPage) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        CRVPage cRVPage2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            cRVPage2 = cRVPage;
            if (PatchProxy.proxy(cRVPage2, xRiverLifecycleHelperBase, changeQuickRedirect, "15", CRVPage.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverLifecycleHelperBase = this;
            cRVPage2 = cRVPage;
        }
        if (cRVPage2 != null) {
            int size = xRiverLifecycleHelperBase.mOverlayViewHolderMap.size();
            String pathAndHash = UrlUtils.getPathAndHash(cRVPage2.getUrl());
            RVLogger.d(TAG, "handleOverlayTabBarIfNeeded : " + size + ",url: " + cRVPage2.getUrl());
            if (!cRVPage2.isTabPage() && pathAndHash.length() > 1 && !xRiverLifecycleHelperBase.mTabBarHashList.contains(pathAndHash.substring(1))) {
                e(8);
            } else {
                e(0);
            }
            CRVApp cRVApp = xRiverLifecycleHelperBase.mCRVApp;
            if (cRVApp.tabPageResumeListener == null) {
                cRVApp.addTabPageResumeListener(new CRVApp.TabPageResumeListener(this) {
                    public static ChangeQuickRedirect 支;
                    public final XRiverLifecycleHelperBase a;

                    {
                        ConstructorCode proxy;
                        ChangeQuickRedirect changeQuickRedirect2 = 支;
                        if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                            proxy.afterSuper(this);
                        } else {
                            this.a = this;
                        }
                    }

                    public void hideOverlayTabBar() {
                        ChangeQuickRedirect changeQuickRedirect2 = 支;
                        if (changeQuickRedirect2 != null && PatchProxy.proxy(this, changeQuickRedirect2, "1", Void.TYPE).isSupported) {
                            return;
                        }
                        this.a.e(8);
                    }

                    public void showOverlayTabBar() {
                        ChangeQuickRedirect changeQuickRedirect2 = 支;
                        if (changeQuickRedirect2 != null && PatchProxy.proxy(this, changeQuickRedirect2, "2", Void.TYPE).isSupported) {
                            return;
                        }
                        this.a.e(0);
                    }
                });
            }
        }
    }

    public void hideLoading() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "16", Void.TYPE).isSupported) {
            return;
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null && !cRVApp.getBooleanValue("hasHideLoading")) {
            this.mCRVApp.putBooleanValue("hasHideLoading", true);
            UEPController.decrementCounter(BundleUtils.getString(this.mCRVApp.getStartParams(), "TaskRecordId"));
        }
        RVLogger.d(TAG, "hideLoading");
        HideLoadingRunnable hideLoadingRunnable = new HideLoadingRunnable(this, "betterLoadingDefault");
        DexAOPEntry.java_lang_Runnable_newInstance_Created(hideLoadingRunnable);
        ExecutorUtils.runOnMain(hideLoadingRunnable);
    }

    public void initTabBar(CRVPage cRVPage, byte[] bArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cRVPage, bArr, CRVPage.class, byte[].class, Void.TYPE}, this, changeQuickRedirect, "17").isSupported) {
            return;
        }
        XRiverLifecycleHelperBase$11 r0 = new XRiverLifecycleHelperBase$11(this, cRVPage);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r0);
        ExecutorUtils.runOnMain(r0);
        if (bArr != null) {
            RVLogger.d(TAG, "showTabBar " + cRVPage);
            JSONArray jSONArray = JSONUtils.getJSONArray(JSONUtils.parseObject(bArr), "items", new JSONArray());
            if (jSONArray != null) {
                for (int i = 0; i < jSONArray.size(); i++) {
                    String string = JSONUtils.getString(jSONArray.getJSONObject(i), "url", "");
                    if (string != null && !TextUtils.isEmpty(string)) {
                        this.mTabBarHashList.add(string);
                    }
                }
                return;
            }
            return;
        }
        RVLogger.d(TAG, "showTabBar " + cRVPage + " tabbarJson: null");
    }

    public boolean isTabPage(String str) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        String str2;
        TabBar tabBar;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, xRiverLifecycleHelperBase, changeQuickRedirect, "18", String.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            xRiverLifecycleHelperBase = this;
            str2 = str;
        }
        if ((xRiverLifecycleHelperBase.mCRVApp.getAppContext() instanceof AppUIContext) && (tabBar = xRiverLifecycleHelperBase.mCRVApp.getAppContext().getTabBar()) != null) {
            return tabBar.isTabPage(UrlUtils.getHash(str2));
        }
        return false;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), intent, Integer.TYPE, Integer.TYPE, Intent.class, Void.TYPE}, this, changeQuickRedirect, "19").isSupported) {
            return;
        }
        Page page = this.mCRVApp;
        if (page == null) {
            RVLogger.d(TAG, "onActivityResult but mApp == null!");
            return;
        }
        if (page.getActivePage() != null) {
            page = this.mCRVApp.getActivePage();
        }
        ExtensionPoint.as(ActivityResultPoint.class).node(page).create().onActivityResult(i, i2, intent);
    }

    public void onConfigurationChanged(Configuration configuration) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, xRiverLifecycleHelperBase, changeQuickRedirect, "20", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverLifecycleHelperBase = this;
            configuration2 = configuration;
        }
        RVLogger.d(TAG, "onConfigurationChanged: " + configuration2);
        CRVApp cRVApp = xRiverLifecycleHelperBase.mCRVApp;
        if (cRVApp != null) {
            cRVApp.onConfigurationChanged(configuration2);
        }
    }

    public void onDestroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "21", Void.TYPE).isSupported) {
            return;
        }
        ExitEvent.of(this.mActivity).tagging("Helper.onDestroy");
        FragmentActivity fragmentActivity = this.mActivity;
        if (fragmentActivity != null && this.mConfigChangedReceiver != null) {
            DexAOPEntry.android_support_v4_content_LocalBroadcastManager_unregisterReceiver_proxy(LocalBroadcastManager.getInstance(fragmentActivity), this.mConfigChangedReceiver);
            this.mConfigChangedReceiver = null;
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            cRVApp.exit();
        }
        for (Map.Entry<Long, CubeViewHolder> entry : this.mOverlayViewHolderMap.entrySet()) {
            if (entry != null) {
                entry.getValue().onOverlayDestroy();
            }
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{Integer.valueOf(i), keyEvent, Integer.TYPE, KeyEvent.class, Boolean.TYPE}, this, changeQuickRedirect, "22");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            return cRVApp.onKeyDown(i);
        }
        return false;
    }

    public void onPause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "23", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "onPause");
        Page page = this.mCRVApp;
        if (page.getActivePage() != null) {
            page = this.mCRVApp.getActivePage();
        }
        ExtensionPoint.as(ActivityOnPausePoint.class).node(page).create().onPause();
        for (Map.Entry<Long, CubeViewHolder> entry : this.mOverlayViewHolderMap.entrySet()) {
            if (entry != null) {
                entry.getValue().onOverlayPause();
            }
        }
    }

    public void onRequestPermissionResult(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "24").isSupported) {
            return;
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null && !cRVApp.isDestroyed()) {
            int childCount = this.mCRVApp.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                Page pageByIndex = this.mCRVApp.getPageByIndex(i2);
                if (pageByIndex.getPageContext() != null) {
                    pageByIndex.getPageContext().getEmbedViewManager().onRequestPermissionResult(i, strArr, iArr);
                }
            }
        }
        RVNativePermissionRequestProxy rVNativePermissionRequestProxy = (RVNativePermissionRequestProxy) RVProxy.get(RVNativePermissionRequestProxy.class);
        if (rVNativePermissionRequestProxy != null) {
            rVNativePermissionRequestProxy.onRequestPermissionResult(i, strArr, iArr);
        }
    }

    public void onResume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "25", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "onResume");
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            cRVApp.resume();
        }
        for (Map.Entry<Long, CubeViewHolder> entry : this.mOverlayViewHolderMap.entrySet()) {
            if (entry != null) {
                entry.getValue().onOverlayResume();
            }
        }
    }

    public void onStop() {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "26", Void.TYPE).isSupported) && (cRVApp = this.mCRVApp) != null) {
            cRVApp.pause();
        }
    }

    public void onUserInteraction() {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "27", Void.TYPE).isSupported) && (cRVApp = this.mCRVApp) != null) {
            cRVApp.onUserInteraction();
        }
    }

    public void onUserLeaveHint() {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "28", Void.TYPE).isSupported) && (cRVApp = this.mCRVApp) != null) {
            cRVApp.onUserLeaveHint();
        }
    }

    public void pushScene(CRVPage cRVPage, String str, boolean z, Bundle bundle, Bundle bundle2, long j) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cRVPage, str, Boolean.valueOf(z), bundle, bundle2, Long.valueOf(j), CRVPage.class, String.class, Boolean.TYPE, Bundle.class, Bundle.class, Long.TYPE, Void.TYPE}, this, changeQuickRedirect, "29").isSupported) {
            return;
        }
        try {
        } catch (Exception e) {
            e = e;
        }
        try {
            TaskMonitorRunnable pushSceneInnerTask = new PushSceneInnerTask(this, cRVPage, z, bundle, j);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(pushSceneInnerTask);
            String str2 = "";
            try {
                str2 = ConfigUtils.getCertainValueInSpConfig(this.mActivity, PreloadUtils.SWITCH_XRIVER_OPTIMISE, PreloadUtils.SWITCH_KEY_RUN_FIRST, "");
            } catch (Exception e2) {
                RVLogger.e(TAG, "isRunFirst error", e2);
            }
            if ("true".equals(str2) && !this.mCRVApp.isRenderDecoupleFromActivity()) {
                RVLogger.d(TAG, "push scene run first");
                ExecutorUtils.runOnMainAtFrontOfQueue(pushSceneInnerTask);
            } else {
                ExecutorUtils.runOnMain(pushSceneInnerTask);
            }
        } catch (Exception e3) {
            e = e3;
            RVLogger.e(TAG, "pushScene: error", e);
        }
    }

    public void restartApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "30", Void.TYPE).isSupported) {
            return;
        }
        try {
            Method findMethod = ReflectUtils.findMethod(this.mActivity.getClass(), "restartApp", (String[]) null);
            if (findMethod != null) {
                findMethod.invoke(this.mActivity, null);
            }
        } catch (Exception e) {
            RVLogger.e(TAG, e);
        }
    }

    public void showError(int i, String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), str, Integer.TYPE, String.class, Void.TYPE}, this, changeQuickRedirect, "31").isSupported) {
            return;
        }
        RVLogger.d(TAG, "showError " + i + " msg: " + str);
        XRiverLifecycleHelperBase$4 r0 = new XRiverLifecycleHelperBase$4(this, i, str);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r0);
        ExecutorUtils.runOnMain(r0);
    }

    public void showLoading(String str, String str2, String str3, String str4, String str5) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, str3, str4, str5, String.class, String.class, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "32").isSupported) {
            return;
        }
        RVLogger.d(TAG, "showLoading " + this.mCRVApp.getAppId() + " name:" + str + " icon:" + str2 + " desc:" + str3 + " slogan:" + str4 + " appStartTags:" + str5);
        if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
            EntryInfo entryInfo = new EntryInfo();
            entryInfo.title = str;
            entryInfo.iconUrl = str2;
            entryInfo.desc = str3;
            entryInfo.slogan = str4;
            if (!TextUtils.isEmpty(str5)) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("appStartTags", JSONUtils.parseArray(str5));
                entryInfo.extraInfo = jSONObject;
            }
            ShowLoadingRunnable showLoadingRunnable = new ShowLoadingRunnable(this, entryInfo);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(showLoadingRunnable);
            ExecutorUtils.runOnMain(showLoadingRunnable);
            return;
        }
        ExecutorType executorType = ExecutorType.URGENT_DISPLAY;
        XRiverLifecycleHelperBase$2 r6 = new XRiverLifecycleHelperBase$2(this);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r6);
        ExecutorUtils.runNotOnMain(executorType, r6);
    }

    public void showOverlayView(long j, String str, String str2, CRVPage cRVPage) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Long.valueOf(j), str, str2, cRVPage, Long.TYPE, String.class, String.class, CRVPage.class, Void.TYPE}, this, changeQuickRedirect, "33").isSupported) {
            return;
        }
        addOverlayView(j, str, str2, cRVPage);
    }

    public void switchTab(int i, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Boolean.valueOf(z), Integer.TYPE, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "34").isSupported) {
            return;
        }
        RVLogger.d(TAG, "switchTab: " + i);
        XRiverLifecycleHelperBase$12 r6 = new XRiverLifecycleHelperBase$12(this, i);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r6);
        ExecutorUtils.runOnMain(r6);
    }

    public void toggleTabBar(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "35", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "toggleTabBar visible: " + z);
    }

    public void updateLoadingInfo(String str, String str2, String str3, String str4, String str5) {
        EntryInfo entryInfo;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, str3, str4, str5, String.class, String.class, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "36").isSupported) {
            return;
        }
        RVLogger.d(TAG, "updateLoadingInfo " + this.mCRVApp.getAppId() + " name: " + str + " icon:" + str2 + " desc:" + str3 + " slogan:" + str4 + " appStartTags:" + str5);
        if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
            entryInfo = new EntryInfo();
            entryInfo.title = str;
            entryInfo.iconUrl = str2;
            entryInfo.desc = str3;
            entryInfo.slogan = str4;
            if (!TextUtils.isEmpty(str5)) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("appStartTags", JSONUtils.parseArray(str5));
                entryInfo.extraInfo = jSONObject;
            }
            this.hasUpdateInfo = true;
        } else {
            entryInfo = null;
        }
        XRiverLifecycleHelperBase$3 r5 = new XRiverLifecycleHelperBase$3(this, entryInfo);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r5);
        ExecutorUtils.runOnMain(r5);
    }

    public void updateLoadingProgress(int i) {
        XRiverLifecycleHelperBase xRiverLifecycleHelperBase;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverLifecycleHelperBase = this;
            if (PatchProxy.proxy(Integer.valueOf(i), xRiverLifecycleHelperBase, changeQuickRedirect, "37", Integer.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverLifecycleHelperBase = this;
        }
        RVLogger.d(TAG, "updateLoadingProgress = " + i);
        if (xRiverLifecycleHelperBase.mCRVApp == null) {
            RVLogger.w(TAG, "updateLoadingProgress but mCRVApp == null!!");
            return;
        }
        FragmentActivity fragmentActivity = xRiverLifecycleHelperBase.mActivity;
        if (fragmentActivity == null) {
            RVLogger.w(TAG, "updateLoadingProgress but mActivity == null!!");
            return;
        }
        if (fragmentActivity.isFinishing()) {
            RVLogger.w(TAG, "updateLoadingProgress but mActivity.isFinishing()!!");
            return;
        }
        AppUIContext appContext = xRiverLifecycleHelperBase.mCRVApp.getAppContext();
        if (appContext == null) {
            RVLogger.w(TAG, "updateLoadingProgress but appContext == null!!");
            return;
        }
        if (!(appContext instanceof AppUIContext)) {
            RVLogger.w(TAG, "updateLoadingProgress but appContext is not AppUIContext!!");
            return;
        }
        SplashView splashView = appContext.getSplashView();
        if (splashView == null) {
            RVLogger.w(TAG, "updateLoadingProgress but splashView == null!!");
        } else {
            splashView.updateLoadingProgress(i);
        }
    }

    public XRiverLifecycleHelperBase(FragmentActivity fragmentActivity) {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{fragmentActivity})) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.mTabBarHashList = new ArrayList();
        this.mHasPushScene = false;
        this.hasUpdateInfo = false;
        this.delayCloseTimeDuration = 0L;
        this.firstEnterShowLoadingStartTime = 0L;
        this.mActivity = fragmentActivity;
    }
}
