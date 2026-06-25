//
// Decompiled by Jadx - 1608ms
//
package com.alibaba.xriver.android.node;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import com.alibaba.ariver.app.AppNode;
import com.alibaba.ariver.app.AppUtils;
import com.alibaba.ariver.app.NodeInstance;
import com.alibaba.ariver.app.api.App;
import com.alibaba.ariver.app.api.AppContext;
import com.alibaba.ariver.app.api.AppLoadResult;
import com.alibaba.ariver.app.api.AppManager;
import com.alibaba.ariver.app.api.AppRenderContext;
import com.alibaba.ariver.app.api.AppUIContext;
import com.alibaba.ariver.app.api.EmbedType;
import com.alibaba.ariver.app.api.EntryInfo;
import com.alibaba.ariver.app.api.Page;
import com.alibaba.ariver.app.api.ParamUtils;
import com.alibaba.ariver.app.api.model.AppConfigModel;
import com.alibaba.ariver.app.api.point.app.AppDestroyPoint;
import com.alibaba.ariver.app.api.point.app.AppExitPoint;
import com.alibaba.ariver.app.api.point.app.AppInteractionPoint;
import com.alibaba.ariver.app.api.point.app.AppLeaveHintPoint;
import com.alibaba.ariver.app.api.point.app.AppLoadInterceptorPoint;
import com.alibaba.ariver.app.api.point.app.AppOnConfigurationChangedPoint;
import com.alibaba.ariver.app.api.point.app.AppOnLoadResultPoint;
import com.alibaba.ariver.app.api.point.app.AppPausePoint;
import com.alibaba.ariver.app.api.point.app.AppResumePoint;
import com.alibaba.ariver.app.api.point.app.BackKeyDownPoint;
import com.alibaba.ariver.app.api.point.view.TabBarInfoQueryPoint;
import com.alibaba.ariver.app.api.ui.darkmode.ColorSchemeDecider;
import com.alibaba.ariver.app.api.ui.darkmode.ThemeUtils;
import com.alibaba.ariver.app.api.ui.tabbar.TabBar;
import com.alibaba.ariver.app.api.ui.tabbar.model.TabBarModel;
import com.alibaba.ariver.app.ipc.ClientMsgReceiver;
import com.alibaba.ariver.app.ipc.IpcClientUtils;
import com.alibaba.ariver.app.ipc.IpcServerUtils;
import com.alibaba.ariver.engine.api.EngineFactory;
import com.alibaba.ariver.engine.api.EngineUtils;
import com.alibaba.ariver.engine.api.RVEngine;
import com.alibaba.ariver.engine.api.bridge.SendToWorkerCallback;
import com.alibaba.ariver.engine.api.bridge.model.EngineInitCallback;
import com.alibaba.ariver.engine.api.bridge.model.EngineSetupCallback;
import com.alibaba.ariver.engine.api.bridge.model.InitParams;
import com.alibaba.ariver.engine.api.bridge.model.SendToRenderCallback;
import com.alibaba.ariver.engine.api.resources.Resource;
import com.alibaba.ariver.engine.api.resources.ResourceLoadPoint;
import com.alibaba.ariver.kernel.api.extension.Action;
import com.alibaba.ariver.kernel.api.extension.ExtensionManager;
import com.alibaba.ariver.kernel.api.extension.ExtensionPoint;
import com.alibaba.ariver.kernel.api.node.Node;
import com.alibaba.ariver.kernel.api.track.EventTracker;
import com.alibaba.ariver.kernel.common.RVProxy;
import com.alibaba.ariver.kernel.common.bigdata.BigDataChannelManager;
import com.alibaba.ariver.kernel.common.log.AppLog;
import com.alibaba.ariver.kernel.common.log.AppLogContext;
import com.alibaba.ariver.kernel.common.log.AppLogger;
import com.alibaba.ariver.kernel.common.log.PageSource;
import com.alibaba.ariver.kernel.common.log.TracePairLog;
import com.alibaba.ariver.kernel.common.network.NetworkUtil;
import com.alibaba.ariver.kernel.common.service.RVConfigService;
import com.alibaba.ariver.kernel.common.service.executor.ExecutorType;
import com.alibaba.ariver.kernel.common.utils.BundleUtils;
import com.alibaba.ariver.kernel.common.utils.ExecutorUtils;
import com.alibaba.ariver.kernel.common.utils.FileUtils;
import com.alibaba.ariver.kernel.common.utils.JSONUtils;
import com.alibaba.ariver.kernel.common.utils.ProcessUtils;
import com.alibaba.ariver.kernel.common.utils.RVKernelUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.ariver.kernel.common.utils.RVTraceUtils;
import com.alibaba.ariver.kernel.common.utils.UrlUtils;
import com.alibaba.ariver.kernel.ipc.IpcMessageHandler;
import com.alibaba.ariver.remotedebug.jsapi.RemoteDebugBridgeExtension;
import com.alibaba.ariver.remotedebug.utils.RemoteDebugUtils;
import com.alibaba.ariver.resource.api.ResourceContext;
import com.alibaba.ariver.resource.api.content.OfflineResource;
import com.alibaba.ariver.resource.api.content.ResourcePackage;
import com.alibaba.ariver.resource.api.content.ResourceProvider;
import com.alibaba.ariver.resource.api.content.ResourceQuery;
import com.alibaba.ariver.resource.api.extension.AppConfigModelInitPoint;
import com.alibaba.ariver.resource.api.extension.AppModelInitPoint;
import com.alibaba.ariver.resource.api.extension.PackageParsedPoint;
import com.alibaba.ariver.resource.api.models.AppInfoModel;
import com.alibaba.ariver.resource.api.models.AppInfoScene;
import com.alibaba.ariver.resource.api.models.AppModel;
import com.alibaba.ariver.resource.api.models.TemplateConfigModel;
import com.alibaba.ariver.resource.api.storage.PluginStore;
import com.alibaba.ariver.resource.api.storage.TabBarDataStorage;
import com.alibaba.ariver.resource.content.ResourceUtils;
import com.alibaba.ariver.resource.runtime.ResourceContextManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.xriver.android.annotation.CallByNative;
import com.alibaba.xriver.android.bigdata.XRiverBufferStore;
import com.alibaba.xriver.android.bridge.CRVJSIBridge;
import com.alibaba.xriver.android.bridge.CRVNativeBridge;
import com.alibaba.xriver.android.bridge.RenderLoadRefactorConfigManager;
import com.alibaba.xriver.android.cube.CubeManager;
import com.alibaba.xriver.android.legacy.XRiverSessionPlugin;
import com.alibaba.xriver.android.mywebview.JSIOOMDialogService;
import com.alibaba.xriver.android.mywebview.MYWebViewUtils;
import com.alibaba.xriver.android.proxy.CRVWindowProxy;
import com.alibaba.xriver.android.resource.CRVResourceLoadExtension;
import com.alibaba.xriver.android.taskwatch.TaskManager;
import com.alibaba.xriver.android.utils.CRVEventTracker;
import com.alibaba.xriver.android.utils.CRVParamMap;
import com.alibaba.xriver.android.utils.DarkModeHelper;
import com.alibaba.xriver.android.utils.ExitEvent;
import com.alibaba.xriver.android.utils.PaladinServiceUtils;
import com.alibaba.xriver.android.worker.BigDataChannelClient;
import com.alibaba.xriver.android.worker.XRiverWorker;
import com.alipay.android.phone.paladin_api.PaladinService;
import com.alipay.dexaop.DexAOPCenter;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.java.lang.Runnable_run__stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.common.logging.api.LoggerFactory;
import com.alipay.mobile.framework.LauncherApplicationAgent;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.framework.component.ComponentService;
import com.alipay.mobile.framework.service.ext.openplatform.app.App;
import com.alipay.mobile.framework.service.ext.openplatform.modle.AppSourceTag;
import com.alipay.mobile.framework.stack.StackManagerServiceUnify;
import com.alipay.mobile.h5container.api.H5BridgeContext;
import com.alipay.mobile.h5container.api.H5CoreNode;
import com.alipay.mobile.h5container.api.H5Data;
import com.alipay.mobile.h5container.api.H5Event;
import com.alipay.mobile.h5container.api.H5EventFilter;
import com.alipay.mobile.h5container.api.H5Listener;
import com.alipay.mobile.h5container.api.H5Page;
import com.alipay.mobile.h5container.api.H5PageLoader;
import com.alipay.mobile.h5container.api.H5Plugin;
import com.alipay.mobile.h5container.api.H5PluginManager;
import com.alipay.mobile.h5container.api.H5Scenario;
import com.alipay.mobile.liteprocess.api.LiteProcessExport;
import com.alipay.mobile.nebula.appcenter.api.H5ContentProvider;
import com.alipay.mobile.nebula.appcenter.model.H5Refer;
import com.alipay.mobile.nebula.log.H5LogData;
import com.alipay.mobile.nebula.log.H5LogUtil;
import com.alipay.mobile.nebula.log.linkmonitor.H5LinkMonitor;
import com.alipay.mobile.nebula.performance.ThreadController;
import com.alipay.mobile.nebula.provider.TaConfigProvider;
import com.alipay.mobile.nebula.util.H5SecurityUtil;
import com.alipay.mobile.nebula.util.PerfUtils;
import com.alipay.mobile.nebula.util.XriverH5Utils;
import com.alipay.mobile.nebula.wallet.H5ThreadPoolFactory;
import com.alipay.mobile.nebulaappproxy.api.H5AppProxyUtil;
import com.alipay.mobile.nebulacore.Nebula;
import com.alipay.mobile.nebulacore.config.H5PluginConfigManager;
import com.alipay.mobile.nebulacore.data.H5MemData;
import com.alipay.mobile.nebulacore.env.H5Environment;
import com.alipay.mobile.nebulacore.manager.H5PluginManagerImpl;
import com.alipay.mobile.nebulacore.plugin.H5ScreenPlugin;
import com.alipay.mobile.nebulacore.plugin.H5SnapshotPlugin;
import com.alipay.mobile.nebulacore.plugin.tinyapp.TinyAppHistoryInfoPlugin;
import com.alipay.mobile.nebulacore.util.PerformanceConfigCenter;
import com.alipay.mobile.nebulaintegration.obfuscated.f2;
import com.alipay.mobile.nebulax.engine.api.EngineType;
import com.alipay.mobile.nebulax.engine.renderng.CubeOverlayViewHolder;
import com.alipay.mobile.nebulax.engine.renderng.CubeViewHolder;
import com.alipay.mobile.nebulax.engine.renderng.RenderNgSetup;
import com.alipay.mobile.nebulax.integration.api.BufferStore;
import com.alipay.mobile.nebulax.integration.base.api.ICRVApp;
import com.alipay.mobile.nebulax.integration.base.legacy.H5ContentProviderLegacy;
import com.alipay.mobile.nebulax.integration.mpaas.extensions.TrackExtension;
import com.alipay.mobile.nebulax.integration.mpaas.ipc.NebulaAppMsgReceiver;
import com.alipay.mobile.nebulax.resource.api.appinfo.AppType;
import com.alipay.mobile.nebulax.resource.api.paladin.PaladinUtils;
import com.alipay.mobile.nebulax.resource.api.prepare.CaprimulgusLoader;
import com.alipay.mobile.nebulax.resource.api.prepare.PrepareUtils;
import com.alipay.mobile.nebulax.resource.api.util.NXResourceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public class CRVApp extends ICRVApp {
    public static ChangeQuickRedirect 支;
    public MYWebViewUtils.JSIOOMListener A;
    public final AtomicBoolean B;
    public volatile boolean C;
    public volatile boolean D;
    public final Object E;
    public final LinkedList F;
    public String G;
    public H5LinkMonitor H;
    public int I;
    public String J;
    public final List K;
    public final List L;
    public final ArrayList M;
    public boolean N;
    public boolean O;
    public final AtomicBoolean P;
    public CRVPage Q;
    public boolean R;
    public String S;
    public boolean T;
    public boolean U;
    public H5Scenario V;
    public final H5MemData W;
    public final String a;
    public String b;
    public AppUIContext c;
    public final CountDownLatch d;
    public RVEngine e;
    public Bundle f;
    public Bundle g;
    public long h;
    public final String i;
    public final CountDownLatch j;
    public CRVWindowProxy.Handler k;
    public H5PluginManagerImpl l;
    public H5ContentProviderLegacy m;
    public final NebulaAppMsgReceiver n;
    public String o;
    public boolean p;
    public final boolean q;
    public final boolean r;
    public boolean s;
    public boolean t;
    public TabPageResumeListener tabPageResumeListener;
    public final XRiverBufferStore u;
    public AppRenderContext v;
    public NetworkUtil.NetworkListener w;
    public DarkModeHelper x;
    public final boolean y;
    public boolean z;
    public static final String X = AppType.WEB_TINY.name();
    public static AtomicBoolean Y = null;
    public static final Parcelable.Creator<CRVApp> CREATOR = new Object();

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 1 implements Parcelable.Creator<CRVApp> {
        public static ChangeQuickRedirect 支;

        @Override
        public CRVApp createFromParcel(Parcel parcel) {
            return createFromParcel(parcel);
        }

        @Override
        public CRVApp[] newArray(int i) {
            return newArray(i);
        }

        @Override
        public CRVApp createFromParcel(Parcel parcel) {
            Parcel parcel2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                parcel2 = parcel;
                PatchProxyResult proxy = PatchProxy.proxy(parcel2, this, changeQuickRedirect, "0", Parcel.class, CRVApp.class);
                if (proxy.isSupported) {
                    return (CRVApp) proxy.result;
                }
            } else {
                parcel2 = parcel;
            }
            return new CRVApp(parcel2);
        }

        @Override
        public CRVApp[] newArray(int i) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "2", Integer.TYPE, CRVApp[].class);
                if (proxy.isSupported) {
                    return (CRVApp[]) proxy.result;
                }
            }
            return new CRVApp[i];
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 13 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final TaConfigProvider a;
        public final CRVApp b;

        public 13(CRVApp cRVApp, TaConfigProvider taConfigProvider) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp, taConfigProvider})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = cRVApp;
                this.a = taConfigProvider;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            this.a.releaseTinyAppConfig(this.b.a);
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 13.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(13.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 14 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final CRVApp a;

        public 14(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVApp;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            this.a.exit();
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 14.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(14.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 16 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final long a;
        public final CRVApp b;

        public 16(CRVApp cRVApp, long j) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp, Long.valueOf(j)})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = cRVApp;
                this.a = j;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            CRVApp cRVApp = this.b;
            RVEngine rVEngine = cRVApp.e;
            long j = this.a;
            if (rVEngine == null) {
                RVLogger.e("XRIVER:Android:CRVApp", "mEngineProxy == null1");
                CRVNativeBridge.nativeInvokeCallback(j, false);
                return;
            }
            CRVNativeBridge.nativeInvokeCallback(j, true);
            if (cRVApp.e != null && cRVApp.G != null && cRVApp.isTinyApp()) {
                BigDataChannelManager.getInstance().registerReceiveDataCallback(cRVApp.G, new BigDataChannelClient(cRVApp.e.getEngineRouter().getWorkerById(cRVApp.G)));
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 16.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(16.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 18 implements JSIOOMDialogService.DialogCallback {
        public static ChangeQuickRedirect 支;
        public final AtomicBoolean a;
        public final CountDownLatch b;

        public 18(AtomicBoolean atomicBoolean, CountDownLatch countDownLatch) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{atomicBoolean, countDownLatch})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = atomicBoolean;
                this.b = countDownLatch;
            }
        }

        public void onCancel() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "1", Void.TYPE).isSupported) {
                return;
            }
            this.a.set(false);
            this.b.countDown();
        }

        public void onConfirm() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            this.a.set(true);
            this.b.countDown();
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 19 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final CRVApp a;

        public 19(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVApp;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            Parcelable.Creator<CRVApp> creator = CRVApp.CREATOR;
            CRVApp cRVApp = this.a;
            cRVApp.getClass();
            RVLogger.d("XRIVER:Android:CRVApp", "interceptOOMFromJSI showJSIOOMDialog");
            new JSIOOMDialogService().showErrorDialog(cRVApp, false);
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 19.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(19.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 2 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final AppModel a;
        public final CRVApp b;

        public 2(CRVApp cRVApp, boolean z, AppModel appModel) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp, Boolean.valueOf(z), appModel})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = cRVApp;
                this.a = appModel;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            Parcelable.Creator<CRVApp> creator = CRVApp.CREATOR;
            CRVApp cRVApp = this.b;
            cRVApp.getClass();
            RVTraceUtils.traceBeginSection("invokeAppInfoExtension");
            AppModelInitPoint create = ExtensionPoint.as(AppModelInitPoint.class).node(cRVApp).create();
            AppModel appModel = this.a;
            create.onGetAppInfo(appModel);
            if (appModel != null) {
                Bundle startParams = cRVApp.getStartParams();
                if (startParams != null) {
                    if (CRVApp.Y == null) {
                        CRVApp.Y = new AtomicBoolean(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("ta_fix_capr_unify_url", true));
                        RVLogger.d("XRIVER:Android:CRVApp", "fix capr params unify, " + CRVApp.Y.get());
                    }
                    if (CRVApp.Y.get()) {
                        if (startParams.containsKey("u")) {
                            RVLogger.d("XRIVER:Android:CRVApp", "some idiot put url as u to start params, need verify");
                            ParamUtils.unify(cRVApp.getStartParams(), "url", false);
                        }
                    } else {
                        ParamUtils.unify(cRVApp.getStartParams(), "url", false);
                    }
                }
                String string = BundleUtils.getString(cRVApp.getStartParams(), "url", "");
                String str = null;
                if (TextUtils.isEmpty(string) && appModel.getAppInfoModel() != null) {
                    string = appModel.getAppInfoModel().getMainUrl();
                    if (!TextUtils.isEmpty(string) && string.startsWith("/")) {
                        string = null;
                    }
                }
                if (string != null) {
                    if (!TextUtils.isEmpty(string) && string.startsWith(appModel.getAppInfoModel().getVhost())) {
                        RVLogger.d("XRIVER:Android:CRVApp", "vhost not use capr ".concat(string));
                    } else {
                        if (appModel.getExtendInfos() != null) {
                            str = JSONUtils.getString(appModel.getExtendInfos().getJSONObject("launchParams"), "caprMode", "");
                        }
                        if (TextUtils.isEmpty(str)) {
                            str = BundleUtils.getString(cRVApp.getStartParams(), "caprMode", "");
                        }
                        if (TextUtils.isEmpty(str)) {
                            str = PrepareUtils.getCaprModeFromURI(string, cRVApp.getStartParams());
                        }
                        if (!TextUtils.isEmpty(str) && !cRVApp.t) {
                            cRVApp.t = true;
                            CaprimulgusLoader.getInstance().preloadManifest(appModel.getAppId(), string, str, cRVApp.getStartParams());
                            RVLogger.d("XRIVER:Android:CRVApp", "LaunchParams preloadManifest url = ".concat(string));
                        }
                    }
                }
            }
            RVTraceUtils.traceEndSection("invokeAppInfoExtension");
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
        public final CRVApp a;

        public 3(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVApp;
            }
        }

        private void __run_stub_private() {
            AppSourceTag appSourceTag;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            CRVApp cRVApp = this.a;
            try {
                RVTraceUtils.traceBeginSection("getOpenPlatApp");
                App openPlatApp = H5AppProxyUtil.getOpenPlatApp(cRVApp.a);
                if (openPlatApp != null && openPlatApp.isSmallProgram()) {
                    cRVApp.getSceneParams().putString("usePresetPopmenu", "YES");
                } else if (AppInfoScene.isDevSource(cRVApp.getStartParams())) {
                    cRVApp.getSceneParams().putString("usePresetPopmenu", "YES");
                }
                if (openPlatApp != null && (appSourceTag = openPlatApp.appSourceTag()) != null) {
                    cRVApp.getSceneParams().putInt("KEY_APPSOURCETAG", appSourceTag.ordinal());
                    RVLogger.d("XRIVER:Android:CRVApp", "got appSourceTag: " + appSourceTag.ordinal());
                }
            } finally {
                try {
                } finally {
                }
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 3.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(3.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 6 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final CRVApp a;

        public 6(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVApp;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            CRVApp cRVApp = this.a;
            for (AppContenxtReadyListener appContenxtReadyListener : cRVApp.L) {
                if (appContenxtReadyListener != null) {
                    appContenxtReadyListener.onContextReady();
                }
            }
            cRVApp.L.clear();
            cRVApp.B.set(false);
            RVLogger.d("XRIVER:Android:CRVApp", "mPendingJSAPIDispatch set to false");
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 6.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(6.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class 7 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final TabBar a;
        public final long b;

        public 7(TabBar tabBar, long j) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{tabBar, Long.valueOf(j)})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = tabBar;
                this.b = j;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            this.a.reset();
            CRVNativeBridge.nativeInvokeCallback(this.b, (Object) null);
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
        public final Page a;
        public final CRVApp b;

        public 8(CRVApp cRVApp, Page page) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp, page})) != null) {
                proxy.afterSuper(this);
            } else {
                this.b = cRVApp;
                this.a = page;
            }
        }

        private void __run_stub_private() {
            CRVApp cRVApp;
            AppUIContext appUIContext;
            Page page;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) && (appUIContext = (cRVApp = this.b).c) != null && appUIContext.getTabBar() != null && (page = this.a) != null && cRVApp.c.getTabBar().isTabPage(page)) {
                cRVApp.c.getTabBar().switchTab(cRVApp.c.getTabBar().getIndexByPage(page), 3);
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

        public 9(CRVPage cRVPage) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVPage})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVPage;
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            StringBuilder sb = new StringBuilder("H5");
            sb.append(H5SecurityUtil.getMD5((System.currentTimeMillis() + XriverH5Utils.getUid(H5Environment.getContext())) + ""));
            H5PageLoader.h5Token = sb.toString();
            StringBuilder sb2 = new StringBuilder("H5Session");
            sb2.append(H5SecurityUtil.getMD5((System.currentTimeMillis() + XriverH5Utils.getUid(H5Environment.getContext())) + ""));
            H5PageLoader.h5SessionToken = sb2.toString();
            CRVPage cRVPage = this.a;
            if (cRVPage.getPageData() != null) {
                cRVPage.getPageData().setH5Token(H5PageLoader.h5Token);
                cRVPage.getPageData().setH5SessionToken(H5PageLoader.h5SessionToken);
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != 9.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(9.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public interface AppContenxtReadyListener {
        void onContextReady();
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class CRVResourcePackage implements ResourcePackage {
        public static ChangeQuickRedirect 支;
        public final CRVApp a;

        public CRVResourcePackage(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = cRVApp;
            }
        }

        public boolean access(@NonNull ResourceQuery resourceQuery) {
            CRVResourcePackage cRVResourcePackage;
            ResourceQuery resourceQuery2;
            AppInfoModel appInfoModel;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                cRVResourcePackage = this;
                resourceQuery2 = resourceQuery;
                PatchProxyResult proxy = PatchProxy.proxy(resourceQuery2, cRVResourcePackage, changeQuickRedirect, "1", ResourceQuery.class, Boolean.TYPE);
                if (proxy.isSupported) {
                    return ((Boolean) proxy.result).booleanValue();
                }
            } else {
                cRVResourcePackage = this;
                resourceQuery2 = resourceQuery;
            }
            String str = resourceQuery2.pureUrl;
            CRVApp cRVApp = cRVResourcePackage.a;
            AppModel appModel = (AppModel) cRVApp.getData(AppModel.class, false);
            if (str != null && !str.startsWith("https") && resourceQuery2.isNeedAutoCompleteHost() && appModel != null && (appInfoModel = appModel.getAppInfoModel()) != null) {
                str = FileUtils.combinePath(appInfoModel.getVhost(), str);
            }
            return CRVNativeBridge.nativeAccessResource(cRVApp.getNodeId(), str, resourceQuery2.isCanUseFallback());
        }

        public void add(Resource resource) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(resource, this, changeQuickRedirect, "2", Resource.class, Void.TYPE).isSupported;
            }
        }

        public String appId() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "3", String.class);
                if (proxy.isSupported) {
                    return (String) proxy.result;
                }
            }
            return this.a.a;
        }

        public int count() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return 0;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "4", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
            return 0;
        }

        public Resource get(@NonNull ResourceQuery resourceQuery) {
            CRVResourcePackage cRVResourcePackage;
            ResourceQuery resourceQuery2;
            AppInfoModel appInfoModel;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                cRVResourcePackage = this;
                resourceQuery2 = resourceQuery;
                PatchProxyResult proxy = PatchProxy.proxy(resourceQuery2, cRVResourcePackage, changeQuickRedirect, "5", ResourceQuery.class, Resource.class);
                if (proxy.isSupported) {
                    return (Resource) proxy.result;
                }
            } else {
                cRVResourcePackage = this;
                resourceQuery2 = resourceQuery;
            }
            String str = resourceQuery2.pureUrl;
            CRVApp cRVApp = cRVResourcePackage.a;
            AppModel appModel = (AppModel) cRVApp.getData(AppModel.class, false);
            if (str != null && !str.startsWith("https") && resourceQuery2.isNeedAutoCompleteHost() && appModel != null && (appInfoModel = appModel.getAppInfoModel()) != null) {
                str = FileUtils.combinePath(appInfoModel.getVhost(), str);
            }
            byte[] nativeGetResource = CRVNativeBridge.nativeGetResource(cRVApp.getNodeId(), str, resourceQuery2.isCanUseFallback());
            if (nativeGetResource != null) {
                return new OfflineResource(resourceQuery2.originUrl, nativeGetResource);
            }
            return null;
        }

        public boolean isPrepareDone() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return true;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "6", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
            return true;
        }

        public Set<String> keySet() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return null;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "7", Set.class);
            if (proxy.isSupported) {
                return (Set) proxy.result;
            }
            return null;
        }

        public boolean needWaitForSetup() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return false;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "8", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
            return false;
        }

        public void reload() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(this, changeQuickRedirect, "9", Void.TYPE).isSupported;
            }
        }

        public void remove(String str) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(str, this, changeQuickRedirect, "10", String.class, Void.TYPE).isSupported;
            }
        }

        public void setup(boolean z) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z2 = PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "11", Boolean.TYPE, Void.TYPE).isSupported;
            }
        }

        public void teardown() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(this, changeQuickRedirect, "12", Void.TYPE).isSupported;
            }
        }

        public String version() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "13", String.class);
                if (proxy.isSupported) {
                    return (String) proxy.result;
                }
            }
            return this.a.getAppVersion();
        }

        public void waitForParse() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(this, changeQuickRedirect, "14", Void.TYPE).isSupported;
            }
        }

        public void waitForSetup() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                boolean z = PatchProxy.proxy(this, changeQuickRedirect, "15", Void.TYPE).isSupported;
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class PushPageRunnable implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        public final String a;
        public final Bundle b;
        public final Bundle c;
        public final CRVApp d;

        public PushPageRunnable(CRVApp cRVApp, String str, Bundle bundle, Bundle bundle2) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp, str, bundle, bundle2})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.d = cRVApp;
            this.a = str;
            this.b = bundle;
            this.c = bundle2;
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                return;
            }
            Bundle bundle = this.c;
            EmbedType embedType = EmbedType.NO;
            CRVApp cRVApp = this.d;
            CRVPage createPage = CRVPage.createPage(cRVApp, this.a, this.b, bundle, embedType);
            cRVApp.pushChild(createPage);
            AppUIContext appUIContext = cRVApp.c;
            if (appUIContext != null) {
                appUIContext.pushPage(createPage);
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
                if ((DexAOPCenter.sFlag & 2) == 0 || getClass() != PushPageRunnable.class) {
                    __run_stub_private();
                } else {
                    DexAOPEntry.java_lang_Runnable_run_proxy(PushPageRunnable.class, this);
                }
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public interface TabPageResumeListener {
        void hideOverlayTabBar();

        void showOverlayTabBar();
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public CRVApp(Parcel parcel) {
        super((Parcel) r1[0]);
        Parcelable readParcelable;
        Parcelable readParcelable2;
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "2", (objArr = new Object[]{parcel}))) != null) {
            proxy.afterSuper(this);
            return;
        }
        super(parcel);
        this.b = X;
        this.d = new CountDownLatch(1);
        this.j = new CountDownLatch(1);
        this.p = true;
        this.q = false;
        this.r = false;
        this.s = false;
        this.t = false;
        this.y = false;
        this.z = false;
        this.B = new AtomicBoolean(true);
        this.C = false;
        this.D = false;
        this.E = new Object();
        this.F = new LinkedList();
        this.K = Collections.synchronizedList(new ArrayList());
        this.L = Collections.synchronizedList(new ArrayList());
        this.M = new ArrayList();
        this.tabPageResumeListener = null;
        this.O = false;
        this.P = new AtomicBoolean(false);
        this.Q = null;
        this.R = true;
        this.T = false;
        this.U = false;
        this.W = new H5MemData();
        this.a = parcel.readString();
        this.h = parcel.readLong();
        this.b = parcel.readString();
        this.f = parcel.readBundle(AppNode.class.getClassLoader());
        this.g = parcel.readBundle(AppNode.class.getClassLoader());
        this.q = true;
        if (parcel.readInt() == 1 && (readParcelable2 = parcel.readParcelable(AppNode.class.getClassLoader())) != null) {
            setData(EntryInfo.class, readParcelable2);
        }
        if (parcel.readInt() != 1 || (readParcelable = parcel.readParcelable(AppNode.class.getClassLoader())) == null) {
            return;
        }
        setData(AppModel.class, readParcelable);
    }

    private native void nativePause(long j);

    private native void nativeResume(long j);

    public void addAppContextReady(AppContenxtReadyListener appContenxtReadyListener) {
        CRVApp cRVApp;
        AppContenxtReadyListener appContenxtReadyListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            appContenxtReadyListener2 = appContenxtReadyListener;
            if (PatchProxy.proxy(appContenxtReadyListener2, cRVApp, changeQuickRedirect, "4", AppContenxtReadyListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            appContenxtReadyListener2 = appContenxtReadyListener;
        }
        if (appContenxtReadyListener2 != null) {
            if (cRVApp.d.getCount() > 0) {
                cRVApp.K.add(appContenxtReadyListener2);
            } else {
                appContenxtReadyListener2.onContextReady();
            }
        }
    }

    public void addAppContextReadyForJSAPIOnBridgeThread(AppContenxtReadyListener appContenxtReadyListener) {
        CRVApp cRVApp;
        AppContenxtReadyListener appContenxtReadyListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            appContenxtReadyListener2 = appContenxtReadyListener;
            if (PatchProxy.proxy(appContenxtReadyListener2, cRVApp, changeQuickRedirect, "5", AppContenxtReadyListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            appContenxtReadyListener2 = appContenxtReadyListener;
        }
        if (appContenxtReadyListener2 != null) {
            if (cRVApp.B.get()) {
                cRVApp.L.add(appContenxtReadyListener2);
            } else {
                appContenxtReadyListener2.onContextReady();
            }
        }
    }

    public void addAppJSIOOMListener(MYWebViewUtils.JSIOOMListener jSIOOMListener) {
        CRVApp cRVApp;
        MYWebViewUtils.JSIOOMListener jSIOOMListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            jSIOOMListener2 = jSIOOMListener;
            if (PatchProxy.proxy(jSIOOMListener2, cRVApp, changeQuickRedirect, "6", MYWebViewUtils.JSIOOMListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            jSIOOMListener2 = jSIOOMListener;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "JSIOOMListener register for current app: " + this);
        cRVApp.A = jSIOOMListener2;
        MYWebViewUtils.addJSIOOMListener(jSIOOMListener2);
    }

    public boolean addChild(H5CoreNode h5CoreNode) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(h5CoreNode, this, changeQuickRedirect, "7", H5CoreNode.class, Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public synchronized void addListener(H5Listener h5Listener) {
        CRVApp cRVApp;
        H5Listener h5Listener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            h5Listener2 = h5Listener;
            if (PatchProxy.proxy(h5Listener2, cRVApp, changeQuickRedirect, "8", H5Listener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            h5Listener2 = h5Listener;
        }
        synchronized (this) {
            if (h5Listener2 != null) {
                LinkedList linkedList = cRVApp.F;
                if (linkedList != null) {
                    Iterator it = linkedList.iterator();
                    while (it.hasNext()) {
                        if (h5Listener2.equals((H5Listener) it.next())) {
                            return;
                        }
                    }
                    cRVApp.F.add(h5Listener2);
                }
            }
        }
    }

    public boolean addPage(H5Page h5Page) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(h5Page, this, changeQuickRedirect, "9", H5Page.class, Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public void addPageReadyListener(App.PageReadyListener pageReadyListener) {
        CRVApp cRVApp;
        App.PageReadyListener pageReadyListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            pageReadyListener2 = pageReadyListener;
            if (PatchProxy.proxy(pageReadyListener2, cRVApp, changeQuickRedirect, "10", App.PageReadyListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            pageReadyListener2 = pageReadyListener;
        }
        Page activePage = getActivePage();
        if (activePage != null) {
            pageReadyListener2.onPageReady(activePage);
            return;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "addPageReadyListener activePage: " + activePage);
        synchronized (cRVApp.M) {
            cRVApp.M.add(pageReadyListener2);
        }
    }

    public void addTabPageResumeListener(TabPageResumeListener tabPageResumeListener) {
        CRVApp cRVApp;
        TabPageResumeListener tabPageResumeListener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            tabPageResumeListener2 = tabPageResumeListener;
            if (PatchProxy.proxy(tabPageResumeListener2, cRVApp, changeQuickRedirect, "11", TabPageResumeListener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            tabPageResumeListener2 = tabPageResumeListener;
        }
        cRVApp.tabPageResumeListener = tabPageResumeListener2;
    }

    @CallByNative
    public void afterRelaunch(Page page) {
        Page page2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            page2 = page;
            if (PatchProxy.proxy(page2, this, changeQuickRedirect, "12", Page.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            page2 = page;
        }
        CRVApp$8 r7 = new CRVApp$8(this, page2);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r7);
        ExecutorUtils.runOnMain(r7);
    }

    public void b(CRVPage cRVPage) {
        CRVApp cRVApp;
        CRVPage cRVPage2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            cRVPage2 = cRVPage;
            if (PatchProxy.proxy(cRVPage2, cRVApp, changeQuickRedirect, "13", CRVPage.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            cRVPage2 = cRVPage;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (cRVApp.M) {
            try {
                Iterator it = cRVApp.M.iterator();
                while (it.hasNext()) {
                    ((App.PageReadyListener) it.next()).onPageReady(cRVPage2);
                }
                if (cRVApp.M.size() > 0) {
                    RVLogger.d("XRIVER:Android:CRVApp", "onPageStarted flush pageReadyListener size: " + cRVApp.M.size() + " cost: " + (SystemClock.elapsedRealtime() - elapsedRealtime));
                }
                cRVApp.M.clear();
            } catch (Throwable th) {
                throw th;
            }
        }
        AppUIContext appUIContext = cRVApp.c;
        if (appUIContext != null && cRVApp.w == null) {
            cRVApp.w = new NetworkUtil.NetworkListener(this) {
                public static ChangeQuickRedirect 支;
                public final CRVApp a;

                {
                    ConstructorCode proxy;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                        proxy.afterSuper(this);
                    } else {
                        this.a = this;
                    }
                }

                public void onNetworkChanged(NetworkUtil.Network network, NetworkUtil.Network network2) {
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && PatchProxy.proxy(new Object[]{network, network2, NetworkUtil.Network.class, NetworkUtil.Network.class, Void.TYPE}, this, changeQuickRedirect2, "1").isSupported) {
                        return;
                    }
                    Parcelable.Creator<CRVApp> creator = CRVApp.CREATOR;
                    Page activePage = this.a.getActivePage();
                    if (activePage != null && activePage.getRender() != null) {
                        String transferNetworkType = NetworkUtil.transferNetworkType(network2);
                        JSONObject jSONObject = new JSONObject();
                        jSONObject.put("isConnected", Boolean.valueOf(true ^ "none".equals(transferNetworkType)));
                        jSONObject.put("networkType", transferNetworkType);
                        JSONObject jSONObject2 = new JSONObject();
                        jSONObject2.put("data", jSONObject);
                        EngineUtils.sendToRender(activePage.getRender(), "h5NetworkChange", jSONObject2, (SendToRenderCallback) null);
                    }
                }
            };
            NetworkUtil.addListener(appUIContext.getContext(), cRVApp.w);
        }
    }

    public boolean backPressed() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "14", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        Boolean intercept = ExtensionPoint.as(BackKeyDownPoint.class).node(this).defaultValue(Boolean.FALSE).create().intercept(this);
        if (intercept != null && intercept.booleanValue()) {
            return true;
        }
        Page activePage = getActivePage();
        if (activePage != null) {
            RVLogger.d("XRIVER:Android:CRVApp", "backPressed handled by page: " + activePage);
            return activePage.backPressed();
        }
        RVLogger.d("XRIVER:Android:CRVApp", "backPressed handled by app itself: " + this);
        return nativeOnBack(getNodeId());
    }

    @CallByNative
    public void beforeDoStart() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "15", Void.TYPE).isSupported) {
            return;
        }
        ResourceContextManager resourceContextManager = ResourceContextManager.getInstance();
        long startToken = getStartToken();
        String str = this.a;
        ResourceContext resourceContext = resourceContextManager.get(str, startToken);
        resourceContext.setContentProvider(new CRVResourceProvider(this));
        resourceContext.setMainPackage(new CRVResourcePackage(this));
        getExtensionManager().registerExtensionByPoint(this, ResourceLoadPoint.class, new CRVResourceLoadExtension(this));
        this.m = new H5ContentProviderLegacy(this);
        H5PluginManager pluginManager = getPluginManager();
        pluginManager.register(new XRiverSessionPlugin(this));
        pluginManager.register(new H5ScreenPlugin());
        pluginManager.register(new H5SnapshotPlugin(this));
        pluginManager.register(new TinyAppHistoryInfoPlugin());
        H5Plugin createPlugin = H5PluginConfigManager.getInstance().createPlugin(true, "session", pluginManager);
        if (createPlugin != null) {
            pluginManager.register(createPlugin);
        }
        f2 f2Var = new f2(0, this);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(f2Var);
        if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("extension_point_async_onAppStart", false)) {
            String string = RemoteDebugUtils.getSharedPreference().getString(RemoteDebugBridgeExtension.buildJsapiDebugModeStorageKey(str), null);
            AppInfoScene extractScene = AppInfoScene.extractScene(this.f);
            if (!AppInfoScene.DEBUG.equals(extractScene) && !AppInfoScene.INSPECT.equals(extractScene) && !AppInfoScene.TRIAL.equals(extractScene) && TextUtils.isEmpty(string)) {
                ExecutorUtils.execute(ExecutorType.NORMAL, f2Var);
                return;
            }
        }
        f2Var.run();
    }

    @CallByNative
    public void beforeRelaunch(long j) {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            if (PatchProxy.proxy(Long.valueOf(j), cRVApp, changeQuickRedirect, "16", Long.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
        }
        AppUIContext appUIContext = cRVApp.c;
        if (appUIContext != null && appUIContext.getTabBar() != null) {
            CRVApp$7 r2 = new CRVApp$7(cRVApp.c.getTabBar(), j);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r2);
            ExecutorUtils.runOnMain(r2);
            return;
        }
        CRVNativeBridge.nativeInvokeCallback(j, (Object) null);
    }

    public void bindContext(AppContext appContext) {
        CRVApp cRVApp;
        AppContext appContext2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            appContext2 = appContext;
            if (PatchProxy.proxy(appContext2, cRVApp, changeQuickRedirect, "17", AppContext.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            appContext2 = appContext;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "CRVApp:bindContext on: " + this + " context : " + appContext2);
        synchronized (cRVApp.P) {
            try {
                cRVApp.c = (AppUIContext) appContext2;
                if (cRVApp.P.get()) {
                    cRVApp.P.set(false);
                    c(cRVApp.Q, appContext2.getContext());
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        ClientMsgReceiver.getInstance().registerAppHandler(this);
        cRVApp.d.countDown();
        for (AppContenxtReadyListener appContenxtReadyListener : cRVApp.K) {
            if (appContenxtReadyListener != null) {
                appContenxtReadyListener.onContextReady();
            }
        }
        cRVApp.K.clear();
        long nodeId = getNodeId();
        CRVApp$6 r7 = new CRVApp$6(this);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r7);
        CRVNativeBridge.postRunnableToBridgeThread(nodeId, r7);
    }

    public void bindPreRenderContext(AppRenderContext appRenderContext) {
        CRVApp cRVApp;
        AppRenderContext appRenderContext2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            appRenderContext2 = appRenderContext;
            if (PatchProxy.proxy(appRenderContext2, cRVApp, changeQuickRedirect, "18", AppRenderContext.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            appRenderContext2 = appRenderContext;
        }
        cRVApp.v = appRenderContext2;
    }

    public void c(CRVPage cRVPage, Context context) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cRVPage, context, CRVPage.class, Context.class, Void.TYPE}, this, changeQuickRedirect, "19").isSupported) {
            return;
        }
        if (cRVPage == null) {
            RVLogger.e("XRIVER:Android:CRVApp", "sendLegacyStartEvent page is null");
            return;
        }
        cRVPage.bindH5Context(context);
        if (!this.N) {
            Executor singleThreadExecutor = H5ThreadPoolFactory.getSingleThreadExecutor();
            CRVApp$9 r1 = new CRVApp$9(cRVPage);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r1);
            DexAOPEntry.executorExecuteProxy(singleThreadExecutor, r1);
            cRVPage.sendEvent("h5SessionStart", (JSONObject) null);
            sendSessionFromNativeEvent(cRVPage, false);
            this.N = true;
            this.I = cRVPage.getWebViewId();
        }
        cRVPage.sendEvent("h5PageStart", (JSONObject) null);
    }

    public int canAliveAppBeingReused(Bundle bundle, Bundle bundle2, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{bundle, bundle2, Boolean.valueOf(z), Bundle.class, Bundle.class, Boolean.TYPE, Integer.TYPE}, this, changeQuickRedirect, "20");
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        if (isExited()) {
            return 0;
        }
        if (!this.T && !z) {
            RVLogger.w("XRIVER:Android:CRVApp", "worklet check failed:  canReuseAliveApp not received");
            return 0;
        }
        return nativeCanAliveAppBeingReused(getNodeId(), CRVParamMap.bundleToParamMap(bundle), CRVParamMap.bundleToParamMap(bundle2));
    }

    @CallByNative
    public void clearOffscreenParams() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "21", Void.TYPE).isSupported) {
            return;
        }
        synchronized (this.E) {
            this.g.remove("kSceneParamsStartAppInOffScreen");
            this.g.remove("kSceneParamKeyMiniUseLinkToken");
            this.g.remove("kSceneParamKeyMiniUseControlMode");
            this.g.remove("free_window_ctx_id");
            this.f.remove("kSceneParamsStartAppInOffScreen");
            this.f.remove("kSceneParamKeyMiniUseLinkToken");
        }
    }

    public void exit() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "22", Void.TYPE).isSupported) {
            return;
        }
        ExitEvent.of(this).tagging("CRVApp.exit");
        if (this.U) {
            return;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "exit() workerId=" + this.G);
        this.U = true;
        if (this.q) {
            RVLogger.d("XRIVER:Android:CRVApp", "exit with shadowNode!");
            IpcServerUtils.sendMsgToClient(getAppId(), getStartToken(), 4, (Bundle) null);
        } else {
            ExtensionPoint.as(AppExitPoint.class).node(this).actionOn(ExecutorType.UI).when(new Action.Complete<Void>(this) {
                public static ChangeQuickRedirect 支;
                public final CRVApp a;

                {
                    ConstructorCode proxy;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                        proxy.afterSuper(this);
                    } else {
                        this.a = this;
                    }
                }

                public void onComplete(Object obj) {
                    onComplete((Void) obj);
                }

                public void onComplete(Void r7) {
                    12 r1;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null) {
                        r1 = this;
                        if (PatchProxy.proxy(r7, r1, changeQuickRedirect2, "2", Void.class, Void.TYPE).isSupported) {
                            return;
                        }
                    } else {
                        r1 = this;
                    }
                    CRVApp cRVApp = r1.a;
                    cRVApp.nativeExit(cRVApp.getNodeId());
                    cRVApp.onDestroy();
                }
            }).create().onAppExit(this);
            BigDataChannelManager.getInstance().releaseChannelByWorkerId(this.G);
            ThreadController.forceStopThreadControl("main");
        }
    }

    public boolean exitSession() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "23", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        ExitEvent.of(this).tagging("CRVApp.exitSession");
        AppUIContext appUIContext = this.c;
        boolean z = true;
        if (appUIContext != null && appUIContext.moveToBackground()) {
            RVLogger.d("XRIVER:Android:CRVApp", "exitSession(): keep alive, appContext.moveToBackground() called.");
            return true;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "exitSession(): no keep alive");
        if (getBooleanValue("title_bar_minimize_button_enabled")) {
            try {
                RVLogger.d("XRIVER:Android:CRVApp", "startAlipayHomepageAndExit(): call framework to start alipay homepage");
                ((StackManagerServiceUnify) LauncherApplicationAgent.getInstance().getMicroApplicationContext().findServiceByInterface(StackManagerServiceUnify.class.getName())).launchHomeFromSavedTask(((Activity) this.c.getContext()).getTaskId(), this.c.getContext().getClass().getName());
            } catch (Throwable th) {
                RVLogger.e("XRIVER:Android:CRVApp", "startAlipayHomepageAndExit(): call framework to start alipay homepage with Throwable.", th);
            }
            RVConfigService rVConfigService = (RVConfigService) RVProxy.get(RVConfigService.class, true);
            long j = 500;
            if (rVConfigService != null) {
                JSONObject configJSONObject = rVConfigService.getConfigJSONObject("h5_titleBarMinimizeButtonCloseDelay");
                z = JSONUtils.getBoolean(configJSONObject, "delayed", true);
                j = JSONUtils.getLong(configJSONObject, "interval", 500L);
            }
            RVLogger.d("XRIVER:Android:CRVApp", "startAlipayHomepageAndExit(): delayed = " + z + ", interval = " + j);
            if (z) {
                CRVApp$14 r0 = new CRVApp$14(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(r0);
                ExecutorUtils.runOnMain(r0, j);
                return false;
            }
            exit();
            return false;
        }
        exit();
        return false;
    }

    @Nullable
    public Page getActivePage() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "24", Page.class);
            if (proxy.isSupported) {
                return (Page) proxy.result;
            }
        }
        if (isExited()) {
            RVLogger.w("XRIVER:Android:CRVApp", "CRVApp has exited, getActivePage return null");
            return null;
        }
        return nativeGetActivePage(getNodeId());
    }

    public int getAlivePageCount() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "25", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return nativeGetPageCount(getNodeId());
    }

    @Nullable
    public AppContext getAppContext() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "26", AppContext.class);
            if (proxy.isSupported) {
                return (AppContext) proxy.result;
            }
        }
        return this.c;
    }

    public CountDownLatch getAppContextReadyLatch() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "27", CountDownLatch.class);
            if (proxy.isSupported) {
                return (CountDownLatch) proxy.result;
            }
        }
        return this.d;
    }

    public String getAppId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "28", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.a;
    }

    public String getAppType() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "29", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.b;
    }

    public String getAppVersion() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "30", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        AppModel appModel = (AppModel) getData(AppModel.class, false);
        if (appModel != null) {
            return appModel.getAppVersion();
        }
        return null;
    }

    public String getAppxVersionInRender() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "31", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.J;
    }

    public BufferStore getBufferStore() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "32", BufferStore.class);
            if (proxy.isSupported) {
                return (BufferStore) proxy.result;
            }
        }
        return this.u;
    }

    public Node getChildAt(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "33", Integer.TYPE, Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        }
        return nativeGetPageByIndex(getNodeId(), i);
    }

    public int getChildCount() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "34", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return nativeGetPageCount(getNodeId());
    }

    public DarkModeHelper getDarkModeHelper() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "35", DarkModeHelper.class);
            if (proxy.isSupported) {
                return (DarkModeHelper) proxy.result;
            }
        }
        return this.x;
    }

    public H5Data getData() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "36", H5Data.class);
            if (proxy.isSupported) {
                return (H5Data) proxy.result;
            }
        }
        return this.W;
    }

    public CountDownLatch getEngineInitLock() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "37", CountDownLatch.class);
            if (proxy.isSupported) {
                return (CountDownLatch) proxy.result;
            }
        }
        return this.j;
    }

    public RVEngine getEngineProxy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "38", RVEngine.class);
            if (proxy.isSupported) {
                return (RVEngine) proxy.result;
            }
        }
        return this.e;
    }

    public ExtensionManager getExtensionManager() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "39", ExtensionManager.class);
            if (proxy.isSupported) {
                return (ExtensionManager) proxy.result;
            }
        }
        return NodeInstance.sExtensionManager;
    }

    public int getFirstPageViewId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "40", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return this.I;
    }

    public H5LinkMonitor getH5LinkMonitor() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "41", H5LinkMonitor.class);
            if (proxy.isSupported) {
                return (H5LinkMonitor) proxy.result;
            }
        }
        return this.H;
    }

    public boolean getHasRendererNative() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "42", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.s;
    }

    public String getId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "43", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.i;
    }

    public int getIndexOfChild(Node node) {
        Node node2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            node2 = node;
            PatchProxyResult proxy = PatchProxy.proxy(node2, this, changeQuickRedirect, "44", Node.class, Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        } else {
            node2 = node;
        }
        return super/*com.alibaba.ariver.app.NodeInstance*/.getIndexOfChild(node2);
    }

    public IpcMessageHandler getMsgHandler() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "45", IpcMessageHandler.class);
            if (proxy.isSupported) {
                return (IpcMessageHandler) proxy.result;
            }
        }
        return this.n;
    }

    public Page getPageByIndex(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "46", Integer.TYPE, Page.class);
            if (proxy.isSupported) {
                return (Page) proxy.result;
            }
        }
        return nativeGetPageByIndex(getNodeId(), i);
    }

    public Page getPageByNodeId(long j) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Long.valueOf(j), this, changeQuickRedirect, "47", Long.TYPE, Page.class);
            if (proxy.isSupported) {
                return (Page) proxy.result;
            }
        }
        return nativeGetPageByNodeId(getNodeId(), j);
    }

    public Stack<H5Page> getPages() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "48", Stack.class);
            if (proxy.isSupported) {
                return (Stack) proxy.result;
            }
        }
        Stack childNodes = getChildNodes();
        Stack<H5Page> stack = new Stack<>();
        Iterator it = childNodes.iterator();
        while (it.hasNext()) {
            CRVPage cRVPage = (Node) it.next();
            if (cRVPage instanceof CRVPage) {
                stack.add((H5Page) cRVPage);
                CRVPage cRVPage2 = cRVPage;
                if (cRVPage2.getEmbedPage() != null) {
                    stack.add((H5Page) cRVPage2.getEmbedPage());
                }
            }
        }
        return stack;
    }

    public Bundle getParams() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "49", Bundle.class);
            if (proxy.isSupported) {
                return (Bundle) proxy.result;
            }
        }
        return this.f;
    }

    public H5CoreNode getParent() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "50", H5CoreNode.class);
            if (proxy.isSupported) {
                return (H5CoreNode) proxy.result;
            }
        }
        return Nebula.getService();
    }

    public H5PluginManager getPluginManager() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "51", H5PluginManager.class);
            if (proxy.isSupported) {
                return (H5PluginManager) proxy.result;
            }
        }
        return this.l;
    }

    @Nullable
    public AppRenderContext getRenderContext() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "52", AppRenderContext.class);
            if (proxy.isSupported) {
                return (AppRenderContext) proxy.result;
            }
        }
        return this.v;
    }

    public H5Scenario getScenario() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "53", H5Scenario.class);
            if (proxy.isSupported) {
                return (H5Scenario) proxy.result;
            }
        }
        return this.V;
    }

    public Bundle getSceneParams() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "54", Bundle.class);
            if (proxy.isSupported) {
                return (Bundle) proxy.result;
            }
        }
        return this.g;
    }

    public Class<?> getScopeType() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "55", Class.class);
            if (proxy.isSupported) {
                return (Class) proxy.result;
            }
            return com.alibaba.ariver.app.api.App.class;
        }
        return com.alibaba.ariver.app.api.App.class;
    }

    public String getServiceWorkerID() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "56", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.G;
    }

    public Bundle getStartParams() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "57", Bundle.class);
            if (proxy.isSupported) {
                return (Bundle) proxy.result;
            }
        }
        return this.f;
    }

    public long getStartToken() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "58", Long.TYPE);
            if (proxy.isSupported) {
                return ((Long) proxy.result).longValue();
            }
        }
        return this.h;
    }

    public String getStartUrl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "59", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return this.o;
    }

    public H5Page getTopPage() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "60", H5Page.class);
            if (proxy.isSupported) {
                return (H5Page) proxy.result;
            }
        }
        return getActivePage();
    }

    public H5ContentProvider getWebProvider() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "61", H5ContentProvider.class);
            if (proxy.isSupported) {
                return (H5ContentProvider) proxy.result;
            }
        }
        return this.m;
    }

    public CRVWindowProxy.Handler getWindowHandler() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "62", CRVWindowProxy.Handler.class);
            if (proxy.isSupported) {
                return (CRVWindowProxy.Handler) proxy.result;
            }
        }
        return this.k;
    }

    public boolean handleEvent(H5Event h5Event, H5BridgeContext h5BridgeContext) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{h5Event, h5BridgeContext, H5Event.class, H5BridgeContext.class, Boolean.TYPE}, this, changeQuickRedirect, "63");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (h5Event != null && ("exitSession".equals(h5Event.getAction()) || "exitTinyApp".equals(h5Event.getAction()) || "exitApp".equals(h5Event.getAction()))) {
            ExitEvent.of(this).tagging("H5Event." + h5Event.getAction());
        }
        H5PluginManagerImpl h5PluginManagerImpl = this.l;
        if (h5PluginManagerImpl != null && h5PluginManagerImpl.handleEvent(h5Event, h5BridgeContext)) {
            return true;
        }
        return false;
    }

    public void init(String str, Bundle bundle, Bundle bundle2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bundle, bundle2, String.class, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "64").isSupported) {
            return;
        }
        syncNativeParamsToJava(bundle, bundle2);
        Nebula.getService().setSessionListener(this.f.getString("sessionIdForSinglePage"), this);
        this.h = BundleUtils.getLong(bundle2, "startToken", -1L);
        this.x = new DarkModeHelper(this);
        RVLogger.d("XRIVER:Android:CRVApp", "app " + str + " token: " + this.h + " init with startParam " + bundle);
    }

    @CallByNative
    public void initEngine(String str, long j) {
        boolean z;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, Long.valueOf(j), String.class, Long.TYPE, Void.TYPE}, this, changeQuickRedirect, "65").isSupported) {
            return;
        }
        if (this.e != null && TextUtils.equals(str, this.b)) {
            RVLogger.d("XRIVER:Android:CRVApp", "initEngine already setup with " + str);
            return;
        }
        if (BundleUtils.getBoolean(getSceneParams(), "kSceneParamsStartAppInOffScreen", false)) {
            this.C = true;
            this.D = true;
        } else {
            RenderLoadRefactorConfigManager renderLoadRefactorConfigManager = RenderLoadRefactorConfigManager.INSTANCE;
            this.C = renderLoadRefactorConfigManager.useNewContextReadyWithAppId(this.a, str);
            if (this.C && renderLoadRefactorConfigManager.enableRenderDecoupleFromActivity(this.a, str)) {
                if (!isTinyGame() && !renderLoadRefactorConfigManager.enableSystemWebViewRenderDecoupleFromActivity(this.a) && !com.alipay.mobile.nebulax.engine.webview.mywebview.MYWebViewUtils.getUseMYWebStatus(this)) {
                    z = false;
                } else {
                    z = true;
                }
                this.D = z;
            } else {
                this.D = false;
            }
        }
        if (this.C) {
            this.f.putBoolean("UseNewContextReady", true);
        }
        if (this.D) {
            this.f.putBoolean("RenderDecoupleFromActivity", true);
        }
        RVLogger.d("XRIVER:Android:CRVApp", "initEngine with appType: " + str + ", useNewContextReady: " + this.C + ", decoupleFromActivity: " + this.D);
        this.b = str;
        if (PerformanceConfigCenter.INSTANCE.getPreOnLoadResultPoint()) {
            AppModel appModel = (AppModel) getData(AppModel.class);
            AppLoadResult appLoadResult = new AppLoadResult();
            appLoadResult.appType = getAppType();
            if (appModel != null) {
                appLoadResult.appVersion = appModel.getAppVersion();
            }
            appLoadResult.mainHtmlUrl = BundleUtils.getString(this.f, "url");
            RVLogger.d("XRIVER:Android:CRVApp", "invokeAppLoadResultPoint");
            ExtensionPoint.as(AppOnLoadResultPoint.class).node(this).create().onLoadResult(this, appLoadResult);
        }
        if (RVKernelUtils.isDebug()) {
            addAppContextReady(new AppContenxtReadyListener(this) {
                public static ChangeQuickRedirect 支;
                public final CRVApp a;

                @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
                public class 1 implements Runnable_run__stub, Runnable {
                    public static ChangeQuickRedirect 支;
                    public final String a;
                    public final 15 b;

                    public 1(15 r4, String str) {
                        ConstructorCode proxy;
                        ChangeQuickRedirect changeQuickRedirect = 支;
                        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{r4, str})) != null) {
                            proxy.afterSuper(this);
                        } else {
                            this.b = r4;
                            this.a = str;
                        }
                    }

                    private void __run_stub_private() {
                        AppContext appContext;
                        ChangeQuickRedirect changeQuickRedirect = 支;
                        if ((changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) || (appContext = this.b.a.getAppContext()) == null) {
                            return;
                        }
                        H5Environment.showToast(appContext.getContext(), this.a, 0);
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

                {
                    ConstructorCode proxy;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                        proxy.afterSuper(this);
                    } else {
                        this.a = this;
                    }
                }

                @Override
                public void onContextReady() {
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && PatchProxy.proxy(this, changeQuickRedirect2, "1", Void.TYPE).isSupported) {
                        return;
                    }
                    try {
                        String str2 = "XRiver容器";
                        if (ProcessUtils.isTinyProcess()) {
                            str2 = "XRiver容器Lite" + LiteProcessExport.getLpid();
                        }
                        boolean contains = getClass().getName().contains("XRiverRender");
                        CRVApp cRVApp = this.a;
                        if (contains) {
                            str2 = str2 + cRVApp.getAppContext().getContext().getClass().getSimpleName();
                        }
                        CRVApp$15$1 r1 = new CRVApp$15$1(this, "使用" + str2 + "启动" + cRVApp.getAppId() + cRVApp.getAppType());
                        DexAOPEntry.java_lang_Runnable_newInstance_Created(r1);
                        ExecutorUtils.runOnMain(r1);
                    } catch (Throwable th) {
                        RVLogger.w("XRIVER:Android:CRVApp", "ignore", th);
                    }
                }
            });
        }
        String engineType = ((EngineFactory) RVProxy.get(EngineFactory.class)).getEngineType(str);
        putStringValue("appEngineType", engineType);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        RVEngine createEngine = ((EngineFactory) RVProxy.get(EngineFactory.class)).createEngine(engineType, this, this.a);
        this.e = createEngine;
        CRVApp$16 r10 = new CRVApp$16(this, j);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r10);
        if (createEngine == null) {
            RVLogger.e("XRIVER:Android:CRVApp", "mEngineProxy == null2");
            CRVNativeBridge.nativeInvokeCallback(j, false);
            return;
        }
        createEngine.setup(this.f, this.g, new EngineSetupCallback(this, elapsedRealtime, j, engineType) {
            public static ChangeQuickRedirect 支;
            public final long a;
            public final long b;
            public final String c;
            public final CRVApp d;

            {
                ConstructorCode proxy;
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this, Long.valueOf(elapsedRealtime), Long.valueOf(j), engineType})) != null) {
                    proxy.afterSuper(this);
                    return;
                }
                this.d = this;
                this.a = elapsedRealtime;
                this.b = j;
                this.c = engineType;
            }

            public void setupResult(boolean z2, String str2) {
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null && PatchProxy.proxy(new Object[]{Boolean.valueOf(z2), str2, Boolean.TYPE, String.class, Void.TYPE}, this, changeQuickRedirect2, "1").isSupported) {
                    return;
                }
                long j2 = this.b;
                if (z2) {
                    RVLogger.d("XRIVER:Android:CRVApp", "initEngine onSetupFinish, cost=" + (SystemClock.elapsedRealtime() - this.a));
                    EventTracker eventTracker = (EventTracker) RVProxy.get(EventTracker.class);
                    CRVApp cRVApp = this.d;
                    eventTracker.stub(cRVApp, "EngineSetup");
                    InitParams initParams = new InitParams();
                    initParams.startParams = cRVApp.f;
                    initParams.mainResourceUrl = null;
                    RVEngine rVEngine = cRVApp.e;
                    if (rVEngine == null) {
                        RVLogger.e("XRIVER:Android:CRVApp", "mEngineProxy == null2");
                        CRVNativeBridge.nativeInvokeCallback(j2, false);
                        return;
                    } else {
                        rVEngine.init(initParams, new EngineInitCallback(this) {
                            public static ChangeQuickRedirect 支;
                            public final 17 a;

                            {
                                ConstructorCode proxy;
                                ChangeQuickRedirect changeQuickRedirect3 = 支;
                                if (changeQuickRedirect3 != null && (proxy = PatchProxy.proxy(changeQuickRedirect3, "0", new Object[]{this})) != null) {
                                    proxy.afterSuper(this);
                                } else {
                                    this.a = this;
                                }
                            }

                            public void initResult(boolean z3, @Nullable String str3) {
                                ChangeQuickRedirect changeQuickRedirect3 = 支;
                                if (changeQuickRedirect3 != null && PatchProxy.proxy(new Object[]{Boolean.valueOf(z3), str3, Boolean.TYPE, String.class, Void.TYPE}, this, changeQuickRedirect3, "1").isSupported) {
                                    return;
                                }
                                RVLogger.d("XRIVER:Android:CRVApp", "initEngine initResult : " + z3);
                                EventTracker eventTracker2 = (EventTracker) RVProxy.get(EventTracker.class);
                                17 r6 = this.a;
                                eventTracker2.stub(r6.d, "EngineInit");
                                r6.d.j.countDown();
                            }
                        });
                        return;
                    }
                }
                if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("game_handle_setup_fail", true) && EngineType.PALADIN.name().equalsIgnoreCase(this.c)) {
                    RVLogger.e("XRIVER:Android:CRVApp", "mEngineProxy setup failed, callback false");
                    CRVNativeBridge.nativeInvokeCallback(j2, false);
                }
            }
        });
        AppModel appModel2 = (AppModel) getData(AppModel.class);
        if (appModel2 != null && appModel2.getAppInfoModel() != null && isTinyApp()) {
            String combinePath = FileUtils.combinePath(appModel2.getAppInfoModel().getVhost(), "index.worker.js");
            RVEngine rVEngine = this.e;
            if (rVEngine == null) {
                RVLogger.e("XRIVER:Android:CRVApp", "mEngineProxy == null3");
                CRVNativeBridge.nativeInvokeCallback(j, false);
                return;
            }
            rVEngine.getEngineRouter().registerWorker(combinePath, new XRiverWorker(combinePath, this));
        }
        TrackExtension.onAppInitEngineFinishedStatic(this);
        r10.run();
    }

    public boolean interceptEvent(H5Event h5Event, H5BridgeContext h5BridgeContext) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{h5Event, h5BridgeContext, H5Event.class, H5BridgeContext.class, Boolean.TYPE}, this, changeQuickRedirect, "66");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        H5PluginManagerImpl h5PluginManagerImpl = this.l;
        if (h5PluginManagerImpl != null && h5PluginManagerImpl.interceptEvent(h5Event, h5BridgeContext)) {
            return true;
        }
        return false;
    }

    public boolean isActive() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "67", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.z;
    }

    public boolean isClientDarkMode() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "68", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeIsClientDarkMode(getNodeId());
    }

    public boolean isDestroyed() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "69", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.U;
    }

    public boolean isExited() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "70", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.U;
    }

    public boolean isFirstPage() {
        AppUIContext appUIContext;
        Page activePage;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "71", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (getAlivePageCount() == 1 || ((appUIContext = this.c) != null && appUIContext.getTabBar() != null && (activePage = getActivePage()) != null && activePage.getPageURI() != null && this.c.getTabBar().isTabPage(activePage))) {
            return true;
        }
        return false;
    }

    public boolean isH5AppWithHybridWorker() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "72", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeIsH5AppWithHybridWorker(getNodeId());
    }

    public boolean isMiniUseSession() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "73", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeIsMiniUseSession(getNodeId());
    }

    public boolean isNativeSessionExited() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "74", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeIsSessionExited(getNodeId());
    }

    public boolean isNebulaX() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return true;
        }
        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "75", Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return true;
    }

    public boolean isPluginSupportFeature(String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, str2, String.class, String.class, Boolean.TYPE}, this, changeQuickRedirect, "76");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        try {
            return nativePluginSupportFeature(getNodeId(), str, str2);
        } catch (Throwable th) {
            RVLogger.e("XRIVER:Android:CRVApp", th);
            return false;
        }
    }

    public boolean isRenderDecoupleFromActivity() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "77", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.D;
    }

    public boolean isShadowNode() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "78", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.q;
    }

    public boolean isTinyApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "79", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return AppType.valueOf(getAppType()).isTiny();
    }

    public boolean isTinyAppDarkMode() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "80", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeIsTinyAppDarkMode(getNodeId());
    }

    public boolean isTinyGame() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "81", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return AppType.valueOf(getAppType()).isTinyGame();
    }

    public boolean isUseNewContextReady() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "82", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.C;
    }

    public boolean isWorkerletReadyForAliveFunction() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "83", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.T;
    }

    public boolean keepAliveExit() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "84", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return nativeKeepAliveExit(getNodeId());
    }

    public native void nativeAppDidEnterBackgroundMode(long j, boolean z);

    public native boolean nativeAppIsOnBackgroundMode(long j);

    public final native int nativeCanAliveAppBeingReused(long j, long j2, long j3);

    public native boolean nativeEnableBackgroundModeForApp(long j);

    public final native void nativeExit(long j);

    public final native Page nativeGetActivePage(long j);

    public final native Page nativeGetPageByIndex(long j, int i);

    public final native Page nativeGetPageByNodeId(long j, long j2);

    public final native int nativeGetPageCount(long j);

    public native boolean nativeIsClientDarkMode(long j);

    public native boolean nativeIsH5AppWithHybridWorker(long j);

    public native boolean nativeIsMiniUseSession(long j);

    public native boolean nativeIsSessionExited(long j);

    public native boolean nativeIsTinyAppDarkMode(long j);

    public final native boolean nativeKeepAliveExit(long j);

    public native boolean nativeOnBack(long j);

    public final native boolean nativePluginSupportFeature(long j, String str, String str2);

    public final native void nativePopTo(long j, int i);

    public final native void nativeRelaunchToUrl(long j, String str, long j2, long j3);

    public final native void nativeRestart(long j, long j2, long j3);

    public final native void nativeSetPageToTop(long j, long j2);

    public final native void nativeSyncSceneParams(long j, long j2);

    public final native void nativeSyncStartParams(long j, long j2);

    @CallByNative
    public void onAppInfoPrepare(String str, byte[] bArr, boolean z, boolean z2) {
        AppModel fromJSON;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bArr, Boolean.valueOf(z), Boolean.valueOf(z2), String.class, byte[].class, Boolean.TYPE, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "110").isSupported) {
            return;
        }
        RVTraceUtils.traceBeginSection("onAppInfoPrepare");
        if (z2) {
            fromJSON = (AppModel) JSONUtils.parseObject(bArr, AppModel.class);
        } else {
            fromJSON = NXResourceUtils.fromJSON(JSONUtils.parseObject(bArr), AppInfoScene.parse(str));
        }
        if (fromJSON == null) {
            RVLogger.w("XRIVER:Android:CRVApp", "onAppInfoPrepare appModel is null".concat(new String(bArr)));
            return;
        }
        if (!z) {
            if (PaladinUtils.isTinyGame(fromJSON)) {
                PaladinUtils.injectPaladinStartParams(this.f);
            }
            ResourceContextManager.getInstance().get(this.a, getStartToken()).setMainPackageInfo(fromJSON);
            setData(AppModel.class, fromJSON);
            setData(EntryInfo.class, ResourceUtils.getEntryInfo(fromJSON));
            PluginStore pluginStore = new PluginStore();
            pluginStore.batchPutStaticPluginModel(fromJSON);
            setData(PluginStore.class, pluginStore);
            if (isTinyApp() && fromJSON.getAppInfoModel() != null) {
                setServiceWorkerID(FileUtils.combinePath(fromJSON.getAppInfoModel().getVhost(), "index.worker.js"));
            }
        }
        CRVApp$2 r6 = new CRVApp$2(this, z, fromJSON);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r6);
        ExecutorUtils.execute(ExecutorType.URGENT_DISPLAY, r6);
        RVTraceUtils.traceEndSection("onAppInfoPrepare");
    }

    @CallByNative
    public void onAppModelInit(String str, byte[] bArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bArr, String.class, byte[].class, Void.TYPE}, this, changeQuickRedirect, "111").isSupported) || PerformanceConfigCenter.INSTANCE.getOptIsInner()) {
            return;
        }
        RVTraceUtils.traceBeginSection("onAppModelInit");
        CRVApp$3 r5 = new CRVApp$3(this);
        DexAOPEntry.java_lang_Runnable_newInstance_Created(r5);
        ExecutorUtils.execute(ExecutorType.URGENT_DISPLAY, r5);
        RVTraceUtils.traceEndSection("onAppModelInit");
    }

    public void onConfigurationChanged(Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "112", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            configuration2 = configuration;
        }
        ExtensionPoint.as(AppOnConfigurationChangedPoint.class).node(this).create().onConfigurationChanged(this, configuration2, ThemeUtils.getColorScheme(configuration2));
    }

    public void onDestroy() {
        int size;
        AppUIContext appUIContext;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "113", Void.TYPE).isSupported) {
            return;
        }
        TaConfigProvider taConfigProvider = (TaConfigProvider) XriverH5Utils.getProvider(TaConfigProvider.class.getName());
        if (taConfigProvider != null) {
            ExecutorType executorType = ExecutorType.IO;
            CRVApp$13 r2 = new CRVApp$13(this, taConfigProvider);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r2);
            ExecutorUtils.execute(executorType, r2);
        }
        sendEvent("h5SessionExit", null);
        ExtensionPoint.as(AppDestroyPoint.class).node(this).actionOn(ExecutorType.UI).create().onAppDestroy(this);
        if (this.w != null && (appUIContext = this.c) != null) {
            NetworkUtil.removeListener(appUIContext.getContext(), this.w);
            this.w = null;
        }
        MYWebViewUtils.JSIOOMListener jSIOOMListener = this.A;
        if (jSIOOMListener != null) {
            MYWebViewUtils.removeJSIOOMListener(jSIOOMListener);
            this.A = null;
        }
        RVEngine rVEngine = this.e;
        if (rVEngine != null) {
            rVEngine.destroy();
            this.e = null;
        }
        AppUIContext appUIContext2 = this.c;
        if (appUIContext2 != null) {
            appUIContext2.destroy();
            this.c = null;
        }
        if (this.r) {
            Nebula.getService().removeSession(getId());
        }
        LinkedList linkedList = this.F;
        if (linkedList == null) {
            size = 0;
        } else {
            size = linkedList.size();
        }
        while (true) {
            size--;
            if (size < 0) {
                break;
            } else {
                ((H5Listener) linkedList.get(size)).onSessionDestroyed(this);
            }
        }
        removeAllListener();
        this.tabPageResumeListener = null;
        this.Q = null;
        this.k = null;
        ((AppManager) RVProxy.get(AppManager.class)).exitApp(getNodeId());
        onFinalized();
        CRVNativeBridge.removeAllBridgeCallback(getNodeId());
        DarkModeHelper darkModeHelper = this.x;
        if (darkModeHelper != null) {
            darkModeHelper.destroy();
            this.x = null;
        }
    }

    public void onInitialize(H5CoreNode h5CoreNode) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(h5CoreNode, this, changeQuickRedirect, "114", H5CoreNode.class, Void.TYPE).isSupported;
        }
    }

    public boolean onKeyDown(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "115", Integer.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (i == 4) {
            ExitEvent.of(this).tagging("CRVApp.KEYCODE_BACK");
            return backPressed();
        }
        return false;
    }

    @CallByNative
    public void onLoadResult(Bundle bundle, Bundle bundle2) {
        String str;
        ResourceContext resourceContext;
        byte[] nativeGetResourceNotWait;
        DarkModeHelper darkModeHelper;
        JSONObject tabBarObject;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{bundle, bundle2, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "116").isSupported) {
            return;
        }
        getExtensionManager().registerExtensionByPoint(this, TabBarInfoQueryPoint.class, new TabBarInfoQueryPoint(this) {
            public static ChangeQuickRedirect 支;
            final CRVApp this$0;

            {
                ConstructorCode proxy;
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                    this.this$0 = (CRVApp) proxy.getFieldValue(0);
                    proxy.afterSuper(this);
                } else {
                    this.this$0 = this;
                }
            }

            public void onFinalized() {
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null) {
                    boolean z = PatchProxy.proxy(this, changeQuickRedirect2, "1", Void.TYPE).isSupported;
                }
            }

            public void onInitialized() {
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null) {
                    boolean z = PatchProxy.proxy(this, changeQuickRedirect2, "2", Void.TYPE).isSupported;
                }
            }

            public void queryTabBarInfo(TabBarInfoQueryPoint.OnTabBarInfoQueryListener onTabBarInfoQueryListener) {
                4 r1;
                TabBarInfoQueryPoint.OnTabBarInfoQueryListener onTabBarInfoQueryListener2;
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null) {
                    r1 = this;
                    onTabBarInfoQueryListener2 = onTabBarInfoQueryListener;
                    if (PatchProxy.proxy(onTabBarInfoQueryListener2, r1, changeQuickRedirect2, "3", TabBarInfoQueryPoint.OnTabBarInfoQueryListener.class, Void.TYPE).isSupported) {
                        return;
                    }
                } else {
                    r1 = this;
                    onTabBarInfoQueryListener2 = onTabBarInfoQueryListener;
                }
                if (onTabBarInfoQueryListener2 != null) {
                    ResourceContextManager.getInstance().get(r1.this$0.getAppId(), r1.this$0.getStartToken()).tabBarDataStorage.retrieveData(new TabBarDataStorage.Listener(onTabBarInfoQueryListener2) {
                        public static ChangeQuickRedirect 支;
                        public final TabBarInfoQueryPoint.OnTabBarInfoQueryListener a;

                        {
                            ConstructorCode proxy;
                            ChangeQuickRedirect changeQuickRedirect3 = 支;
                            if (changeQuickRedirect3 != null && (proxy = PatchProxy.proxy(changeQuickRedirect3, "0", new Object[]{onTabBarInfoQueryListener2})) != null) {
                                proxy.afterSuper(this);
                            } else {
                                this.a = onTabBarInfoQueryListener2;
                            }
                        }

                        public void onGetData(TabBarModel tabBarModel) {
                            1 r12;
                            TabBarModel tabBarModel2;
                            ChangeQuickRedirect changeQuickRedirect3 = 支;
                            if (changeQuickRedirect3 != null) {
                                r12 = this;
                                tabBarModel2 = tabBarModel;
                                if (PatchProxy.proxy(tabBarModel2, r12, changeQuickRedirect3, "1", TabBarModel.class, Void.TYPE).isSupported) {
                                    return;
                                }
                            } else {
                                r12 = this;
                                tabBarModel2 = tabBarModel;
                            }
                            r12.a.onTabInfoGot(tabBarModel2);
                        }
                    });
                }
            }
        });
        AppModel appModel = (AppModel) getData(AppModel.class, false);
        if (appModel == null) {
            RVLogger.w("XRIVER:Android:CRVApp", "onLoadResult but appModel == null!!!");
            return;
        }
        boolean isEnableProgressiveLoad = CRVNativeBridge.isEnableProgressiveLoad(getNodeId());
        StringBuilder sb = new StringBuilder("app ");
        String str2 = this.a;
        sb.append(str2);
        sb.append(" isProgressive ");
        sb.append(isEnableProgressiveLoad);
        sb.append(" onLoadResult with AppModel: ");
        sb.append(appModel);
        RVLogger.d("XRIVER:Android:CRVApp", sb.toString());
        putBooleanValue("appProgressive", isEnableProgressiveLoad);
        this.f.putAll(bundle);
        this.g.putAll(bundle2);
        AppLoadResult appLoadResult = new AppLoadResult();
        appLoadResult.appType = getAppType();
        appLoadResult.appVersion = appModel.getAppVersion();
        String string = BundleUtils.getString(bundle, "url");
        appLoadResult.mainHtmlUrl = string;
        this.o = string;
        PerformanceConfigCenter performanceConfigCenter = PerformanceConfigCenter.INSTANCE;
        if (performanceConfigCenter.getPreOnLoadResultPoint()) {
            RVLogger.d("XRIVER:Android:CRVApp", "onLoadResult finish");
        } else {
            RVLogger.d("XRIVER:Android:CRVApp", "invokeAppLoadResultPoint");
            ExtensionPoint.as(AppOnLoadResultPoint.class).node(this).create().onLoadResult(this, appLoadResult);
        }
        nativeSyncStartParams(getNodeId(), CRVParamMap.bundleToParamMap(this.f));
        nativeSyncSceneParams(getNodeId(), CRVParamMap.bundleToParamMap(this.g));
        byte[] nativeGetResourceNotWait2 = CRVNativeBridge.nativeGetResourceNotWait(getNodeId(), FileUtils.combinePath(appModel.getAppInfoModel().getVhost(), "tabBar.json"), false, false);
        TemplateConfigModel templateConfig = appModel.getAppInfoModel().getTemplateConfig();
        if (templateConfig != null && templateConfig.isTemplateValid() && templateConfig.getExtModel() != null && templateConfig.getExtModel().isExtEnable() && (tabBarObject = templateConfig.getExtModel().getTabBarObject()) != null) {
            nativeGetResourceNotWait2 = tabBarObject.toJSONString().getBytes();
        }
        ColorSchemeDecider colorSchemeDecider = (ColorSchemeDecider) getData(ColorSchemeDecider.class);
        OfflineResource offlineResource = new OfflineResource("", nativeGetResourceNotWait2);
        ResourceContext resourceContext2 = ResourceContextManager.getInstance().get(str2, getStartToken());
        TabBarModel refreshTabBarModel = TabBarModel.refreshTabBarModel(offlineResource, DarkModeHelper.getWalletDarkMode(), isClientDarkMode(), isTinyAppDarkMode(), BundleUtils.getBoolean(this.f, "hideDefaultTabBar", false), colorSchemeDecider);
        if (refreshTabBarModel == null || (darkModeHelper = this.x) == null) {
            str = "XRIVER:Android:CRVApp";
        } else {
            str = "XRIVER:Android:CRVApp";
            darkModeHelper.setTabBarColor(refreshTabBarModel.getBackgroundColor());
        }
        JSONObject parseObject = JSONUtils.parseObject(offlineResource.getBytes());
        if (parseObject != null && !parseObject.isEmpty()) {
            RVLogger.d(str, "put tabBar.json" + parseObject);
            putJsonValue("Json_tabBar", parseObject);
            resourceContext2.tabBarDataStorage.onGetData(refreshTabBarModel);
        }
        if (appModel.getAppInfoModel() != null && (nativeGetResourceNotWait = CRVNativeBridge.nativeGetResourceNotWait(getNodeId(), FileUtils.combinePath(appModel.getAppInfoModel().getVhost(), "appConfig.json"), false, false)) != null) {
            ResourceContextManager.getInstance().get(str2, getStartToken());
            AppConfigModel parseFromJSON = AppConfigModel.parseFromJSON(nativeGetResourceNotWait);
            setData(AppConfigModel.class, parseFromJSON);
            if (parseFromJSON != null) {
                setData(ColorSchemeDecider.class, new ColorSchemeDecider.DefaultDecider(JSONUtils.getJSONArray(parseFromJSON.getAppLaunchParams(), "supportColorScheme", (JSONArray) null)));
            }
            ExtensionPoint.as(AppConfigModelInitPoint.class).node(this).create().onAppConfigModelInit(this, parseFromJSON);
        }
        if (!performanceConfigCenter.getRemoveUnusedExPoint()) {
            AppLoadInterceptorPoint create = ExtensionPoint.as(AppLoadInterceptorPoint.class).node(this).create();
            if (create != null) {
                create.intercept(getAppId(), bundle, bundle2, appLoadResult);
            }
            PackageParsedPoint create2 = ExtensionPoint.as(PackageParsedPoint.class).node(this).create();
            if (create2 != null && (resourceContext = ResourceContextManager.getInstance().get(str2, getStartToken())) != null) {
                create2.onResourceParsed(appModel, resourceContext.getMainPackage());
            }
        }
        if (BundleUtils.getBoolean(this.f, "hideDefaultTabBar", false) && RenderNgSetup.hasSetuped()) {
            RVLogger.d(str, "hideDefaultTabBar, RenderNg has set up");
            DisplayMetrics displayMetrics = LauncherApplicationAgent.getInstance().getApplicationContext().getResources().getDisplayMetrics();
            setData(CubeViewHolder.class, new CubeOverlayViewHolder(LauncherApplicationAgent.getInstance().getApplicationContext(), displayMetrics.widthPixels, displayMetrics.heightPixels));
        }
    }

    @CallByNative
    public void onNativeOverlayCreate(String str, String str2, String str3) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, str3, String.class, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "117").isSupported) {
            return;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "onNativeOverlayCreate viewId: " + str + " type: " + str2 + " height: " + str3);
        if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2) && ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("ta_nativeOverlay_first", "yes").equalsIgnoreCase("yes")) {
            RVLogger.d("XRIVER:Android:CRVApp", "onNativeOverlayCreate first");
            CubeManager.INSTANCE.initOverlayViewHolder(this, str, str2, str3);
        }
    }

    public void onPrepare(H5EventFilter h5EventFilter) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(h5EventFilter, this, changeQuickRedirect, "118", H5EventFilter.class, Void.TYPE).isSupported;
        }
    }

    public void onRelease() {
        H5PluginManagerImpl h5PluginManagerImpl;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "119", Void.TYPE).isSupported) && (h5PluginManagerImpl = this.l) != null) {
            h5PluginManagerImpl.onRelease();
            this.l = null;
        }
    }

    @CallByNative
    public void onSyncParams(Bundle bundle, Bundle bundle2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{bundle, bundle2, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "120").isSupported) {
            return;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "app " + this.a + " onSyncParams ");
        Bundle bundle3 = this.f;
        if (bundle3 != null) {
            bundle3.putAll(bundle);
        }
        Bundle bundle4 = this.g;
        if (bundle4 != null) {
            bundle4.putAll(bundle2);
        }
    }

    public void onUserInteraction() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "121", Void.TYPE).isSupported) {
            return;
        }
        ExtensionPoint.as(AppInteractionPoint.class).node(this).create().onAppInteraction(this);
    }

    public void onUserLeaveHint() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "122", Void.TYPE).isSupported) {
            return;
        }
        ExtensionPoint.as(AppLeaveHintPoint.class).node(this).create().onAppLeaveHint(this);
    }

    @CallByNative
    public void onWorkletFunctionReady(String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "123").isSupported) {
            return;
        }
        if (RVKernelUtils.isDebug()) {
            RVLogger.d("XRIVER:Android:CRVApp", "onWorkletFunctionReady: " + str + ", " + str2 + ", thread:" + Thread.currentThread().getName());
        }
        if ("canReuseAliveApp".equals(str)) {
            this.T = true;
        }
    }

    public void pause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "124", Void.TYPE).isSupported) {
            return;
        }
        this.z = false;
        long elapsedRealtime = SystemClock.elapsedRealtime();
        nativePause(getNodeId());
        long elapsedRealtime2 = SystemClock.elapsedRealtime();
        ExtensionPoint.as(AppPausePoint.class).node(this).create().onAppPause(this);
        long elapsedRealtime3 = SystemClock.elapsedRealtime();
        if (PerfUtils.isReport) {
            PerfUtils.recordPhaseCostTime("CRVAppPause", (elapsedRealtime3 - elapsedRealtime) + ":" + (elapsedRealtime2 - elapsedRealtime) + ":" + (elapsedRealtime3 - elapsedRealtime2));
        }
    }

    public void performBack() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(this, changeQuickRedirect, "125", Void.TYPE).isSupported;
        }
    }

    public void popPage(@Nullable JSONObject jSONObject) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(jSONObject, this, changeQuickRedirect, "126", JSONObject.class, Void.TYPE).isSupported;
        }
    }

    public void popTo(int i, boolean z, @Nullable JSONObject jSONObject) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Boolean.valueOf(z), jSONObject, Integer.TYPE, Boolean.TYPE, JSONObject.class, Void.TYPE}, this, changeQuickRedirect, "127").isSupported) {
            return;
        }
        nativePopTo(getNodeId(), i);
    }

    public Page preCreatePage() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return null;
        }
        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "128", Page.class);
        if (proxy.isSupported) {
            return (Page) proxy.result;
        }
        return null;
    }

    public void pushChild(Node node) {
        CRVApp cRVApp;
        Node node2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            node2 = node;
            if (PatchProxy.proxy(node2, cRVApp, changeQuickRedirect, "129", Node.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            node2 = node;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "pushChild " + node2);
        if (node2 instanceof CRVPage) {
            if (cRVApp.R) {
                cRVApp.R = false;
                PageSource pageSource = ((AppLogContext) node2.getData(AppLogContext.class, true)).getPageSource();
                pageSource.sourceType = PageSource.SourceType.START_APP;
                pageSource.sourcePageAppLogToken = BundleUtils.getString(cRVApp.f, "srcPageAppLogToken");
                pageSource.sourceDesc = " appId: " + BundleUtils.getString(cRVApp.f, "startAppSourceId") + " chinfo: " + BundleUtils.getString(cRVApp.f, "chInfo");
                Bundle bundle = new Bundle();
                bundle.putLong("nodeId", getNodeId());
                IpcClientUtils.sendMsgToServerByApp(this, 3, bundle);
                Iterator it = cRVApp.F.iterator();
                while (it.hasNext()) {
                    ((H5Listener) it.next()).onSessionCreated(this);
                }
            } else {
                ((AppLogContext) node2.getData(AppLogContext.class, true)).getPageSource().sourceType = PageSource.SourceType.PUSH_WINDOW;
            }
            CRVPage cRVPage = (CRVPage) node2;
            synchronized (cRVApp.P) {
                try {
                    AppUIContext appUIContext = cRVApp.c;
                    if (appUIContext == null) {
                        cRVApp.P.set(true);
                        cRVApp.Q = cRVPage;
                    } else {
                        c(cRVPage, appUIContext.getContext());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            Iterator it2 = cRVApp.F.iterator();
            while (it2.hasNext()) {
                ((H5Listener) it2.next()).onPageCreated(cRVPage);
            }
        }
        super/*com.alibaba.ariver.app.NodeInstance*/.pushChild(node2);
    }

    public boolean pushPage(String str, Bundle bundle, Bundle bundle2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, bundle, bundle2, String.class, Bundle.class, Bundle.class, Boolean.TYPE}, this, changeQuickRedirect, "130");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        Bundle bundle3 = this.f;
        if (bundle3 != null && TextUtils.equals(BundleUtils.getString(bundle3, "startScene"), "createPage")) {
            if (this.q) {
                RVLogger.w("XRIVER:Android:CRVApp", "pushPage with shadowNode not work!");
                return false;
            }
            RVLogger.d("XRIVER:Android:CRVApp", "pushPage " + str + " with stack: " + Log.getStackTraceString(new Throwable("Just Print")));
            PushPageRunnable pushPageRunnable = new PushPageRunnable(this, str, bundle, bundle2);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(pushPageRunnable);
            ExecutorUtils.runOnMain(pushPageRunnable);
        }
        return false;
    }

    public void relaunchToUrl(String str, Bundle bundle, Bundle bundle2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bundle, bundle2, String.class, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "131").isSupported) {
            return;
        }
        bundle.putString("url", str);
        nativeRelaunchToUrl(getNodeId(), str, CRVParamMap.bundleToParamMap(bundle), CRVParamMap.bundleToParamMap(bundle2));
    }

    public synchronized void removeAllListener() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "132", Void.TYPE).isSupported) {
            return;
        }
        synchronized (this) {
            LinkedList linkedList = this.F;
            if (linkedList != null && !linkedList.isEmpty()) {
                this.F.clear();
            }
        }
    }

    public boolean removeChild(H5CoreNode h5CoreNode) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(h5CoreNode, this, changeQuickRedirect, "134", H5CoreNode.class, Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    public synchronized void removeListener(H5Listener h5Listener) {
        CRVApp cRVApp;
        H5Listener h5Listener2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            h5Listener2 = h5Listener;
            if (PatchProxy.proxy(h5Listener2, cRVApp, changeQuickRedirect, "135", H5Listener.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            h5Listener2 = h5Listener;
        }
        synchronized (this) {
            if (h5Listener2 != null) {
                LinkedList linkedList = cRVApp.F;
                if (linkedList != null) {
                    linkedList.remove(h5Listener2);
                }
            }
        }
    }

    public void removePage(Page page, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z2 = PatchProxy.proxy(new Object[]{page, Boolean.valueOf(z), Page.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "136").isSupported;
        }
    }

    public void restart(Bundle bundle, Bundle bundle2) {
        String bundle3;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{bundle, bundle2, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "138").isSupported) {
            return;
        }
        ParamUtils.unify(bundle, "url", false);
        this.S = BundleUtils.getString(bundle, "startAppSessionId");
        AppLog.Builder appId = new AppLog.Builder().setState("container awake").setAppId(this.a);
        if (bundle == null) {
            bundle3 = "";
        } else {
            bundle3 = bundle.toString();
        }
        AppLogger.log(appId.setDesc(bundle3).setParentId(this.S).build());
        if (isTinyGame()) {
            PaladinServiceUtils.restartWithKeepAlive(this, bundle, bundle2, true);
        }
        nativeRestart(getNodeId(), CRVParamMap.bundleToParamMap(bundle), CRVParamMap.bundleToParamMap(bundle2));
    }

    public void restartFromServer(@Nullable Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "139", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        restart(bundle2, null);
    }

    public void resume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "140", Void.TYPE).isSupported) {
            return;
        }
        String str = this.S;
        if (str == null) {
            str = BundleUtils.getString(this.f, "startAppSessionId");
        }
        boolean z = this.p;
        String str2 = this.a;
        if (z) {
            AppLogger.log(new AppLog.Builder().setState("appearance start").setAppId(str2).setParentId(str).build());
            AppLogger.log(new TracePairLog.Builder().setParentId("-").setPairType("startAppSessionId_nodeId").setMessage("startAppSessionId: " + str + ", nodeId: " + getNodeId()).build());
        }
        nativeResume(getNodeId());
        ExtensionPoint.as(AppResumePoint.class).node(this).create().onAppResume(this);
        if (this.p) {
            AppLogger.log(new AppLog.Builder().setState("appearance finish").setAppId(str2).setParentId(str).build());
        }
        this.p = false;
        this.z = true;
    }

    public void sendEvent(String str, JSONObject jSONObject) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, jSONObject, String.class, JSONObject.class, Void.TYPE}, this, changeQuickRedirect, "141").isSupported) {
            return;
        }
        if (Nebula.DEBUG) {
            RVLogger.d("XRIVER:Android:CRVApp", "dispatch action " + str);
        }
        Nebula.getDispatcher().sendEvent(str, jSONObject, this);
    }

    public synchronized void sendSessionFromNativeEvent(CRVPage cRVPage, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cRVPage, Boolean.valueOf(z), CRVPage.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "142").isSupported) {
            return;
        }
        synchronized (this) {
            if (!this.O && cRVPage.isInitPlugin()) {
                cRVPage.sendEvent("H5_AL_SESSION_FROM_NATIVE", (JSONObject) null);
                this.O = true;
            }
        }
    }

    public void setAppType(String str) {
        CRVApp cRVApp;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            str2 = str;
            if (PatchProxy.proxy(str2, cRVApp, changeQuickRedirect, "143", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            str2 = str;
        }
        cRVApp.b = str2;
    }

    public void setAppxVersionInRender(String str) {
        CRVApp cRVApp;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            str2 = str;
            if (PatchProxy.proxy(str2, cRVApp, changeQuickRedirect, "144", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            str2 = str;
        }
        cRVApp.J = str2;
    }

    public void setData(H5Data h5Data) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(h5Data, this, changeQuickRedirect, "145", H5Data.class, Void.TYPE).isSupported) {
            return;
        }
        RVLogger.w("XRIVER:Android:CRVApp", "setData null impl!");
    }

    public void setH5LinkMonitor(H5LinkMonitor h5LinkMonitor) {
        CRVApp cRVApp;
        H5LinkMonitor h5LinkMonitor2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            h5LinkMonitor2 = h5LinkMonitor;
            if (PatchProxy.proxy(h5LinkMonitor2, cRVApp, changeQuickRedirect, "146", H5LinkMonitor.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            h5LinkMonitor2 = h5LinkMonitor;
        }
        cRVApp.H = h5LinkMonitor2;
    }

    @CallByNative
    public void setHasRendererNative(boolean z) {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), cRVApp, changeQuickRedirect, "147", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "hasRendererNative: " + z);
        cRVApp.s = z;
    }

    public void setId(String str) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(str, this, changeQuickRedirect, "148", String.class, Void.TYPE).isSupported;
        }
    }

    public void setOnPageResume(boolean z) {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), cRVApp, changeQuickRedirect, "149", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
        }
        TabPageResumeListener tabPageResumeListener = cRVApp.tabPageResumeListener;
        if (tabPageResumeListener != null) {
            if (z) {
                tabPageResumeListener.showOverlayTabBar();
            } else {
                tabPageResumeListener.hideOverlayTabBar();
            }
        }
    }

    public void setPageToTop(Page page) {
        Page page2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            page2 = page;
            if (PatchProxy.proxy(page2, this, changeQuickRedirect, "150", Page.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            page2 = page;
        }
        if (page2 != null) {
            nativeSetPageToTop(getNodeId(), page2.getNodeId());
        } else {
            RVLogger.e("XRIVER:Android:CRVApp", "setPageToTop failed , newPage is null");
        }
    }

    public void setParent(H5CoreNode h5CoreNode) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(h5CoreNode, this, changeQuickRedirect, "151", H5CoreNode.class, Void.TYPE).isSupported;
        }
    }

    public void setScenario(H5Scenario h5Scenario) {
        CRVApp cRVApp;
        H5Scenario h5Scenario2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            h5Scenario2 = h5Scenario;
            if (PatchProxy.proxy(h5Scenario2, cRVApp, changeQuickRedirect, "152", H5Scenario.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            h5Scenario2 = h5Scenario;
        }
        cRVApp.V = h5Scenario2;
    }

    public void setServiceWorkerID(String str) {
        CRVApp cRVApp;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            str2 = str;
            if (PatchProxy.proxy(str2, cRVApp, changeQuickRedirect, "153", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            str2 = str;
        }
        cRVApp.G = str2;
    }

    @CallByNative
    public void setWindowHandler(CRVWindowProxy.Handler handler) {
        CRVApp cRVApp;
        CRVWindowProxy.Handler handler2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            handler2 = handler;
            if (PatchProxy.proxy(handler2, cRVApp, changeQuickRedirect, "154", CRVWindowProxy.Handler.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            handler2 = handler;
        }
        cRVApp.k = handler2;
    }

    public boolean shouldInterceptJSIOOM(String str, Map<String, Integer> map) {
        PaladinService paladinService;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, map, String.class, Map.class, Boolean.TYPE}, this, changeQuickRedirect, "155");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (!isExited() && isActive()) {
            H5LogUtil.logH5Exception(H5LogData.seedId("TINY_MONITOR_JSI_OOM").param3().add("appId", this.a).add("appType", this.b).add("embedderName", str).addMapParam(map));
            if (TextUtils.isEmpty(str)) {
                RVLogger.d("XRIVER:Android:CRVApp", "interceptOOMFromJSI embedderName is empty, return false");
                return false;
            }
            if (isTinyGame() && this.e != null && (paladinService = (PaladinService) ComponentService.get(PaladinService.class)) != null && ComponentService.ready(PaladinService.class)) {
                boolean onJSIOutOfMemoryError = paladinService.onJSIOutOfMemoryError(this.a, this, this.e, str, map);
                LoggerFactory.getTraceLogger().info("XRIVER:Android:CRVApp", "interceptOOMFromJSI intercept by Paladin: " + onJSIOutOfMemoryError);
                if (onJSIOutOfMemoryError) {
                    return true;
                }
            }
            if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("ta_enableAsyncInterceptJSIOOMDialog", false)) {
                AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                CountDownLatch countDownLatch = new CountDownLatch(1);
                a aVar = new a(this, atomicBoolean, countDownLatch);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(aVar);
                ExecutorUtils.runOnMain(aVar);
                try {
                    if (!countDownLatch.await(5L, TimeUnit.SECONDS)) {
                        LoggerFactory.getTraceLogger().warn("XRIVER:Android:CRVApp", "Dialog response timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LoggerFactory.getTraceLogger().error("XRIVER:Android:CRVApp", "Dialog wait interrupted", e);
                }
                LoggerFactory.getTraceLogger().info("XRIVER:Android:CRVApp", "interceptOOMFromJSI final result: " + atomicBoolean.get() + ", with enableAsyncInterceptJSIOOMDialog ON.");
                return atomicBoolean.get();
            }
            CRVApp$19 r12 = new CRVApp$19(this);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r12);
            ExecutorUtils.runOnMain(r12);
            LoggerFactory.getTraceLogger().info("XRIVER:Android:CRVApp", "interceptOOMFromJSI intercept by default with sync enableAsyncInterceptJSIOOMDialog OFF.");
            return true;
        }
        RVLogger.d("XRIVER:Android:CRVApp", "interceptOOMFromJSI CRVApp is exited or not active, return false");
        return false;
    }

    @CallByNative
    public boolean shouldKeepAliveExitByPlatform() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "156", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return PaladinServiceUtils.shouldKeepAliveExit(this);
    }

    @CallByNative
    public boolean shouldUseKeepAliveOnRestartByPlatform(Bundle bundle, String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{bundle, str, str2, Bundle.class, String.class, String.class, Boolean.TYPE}, this, changeQuickRedirect, "157");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return PaladinServiceUtils.shouldUseKeepAliveOnRestart(this, bundle, str, str2);
    }

    public void start() {
        String bundle;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "158", Void.TYPE).isSupported) {
            return;
        }
        AppLog.Builder builder = new AppLog.Builder();
        String str = this.a;
        AppLog.Builder appId = builder.setAppId(str);
        Bundle bundle2 = this.f;
        if (bundle2 == null) {
            bundle = "";
        } else {
            bundle = bundle2.toString();
        }
        AppLogger.log(appId.setDesc(bundle).setParentId(BundleUtils.getString(this.f, "startAppSessionId")).setState("container start").build());
        AppLogger.log(new AppLog.Builder().setAppId(str).setParentId(BundleUtils.getString(this.f, "startAppSessionId")).setState("container finish").build());
        addPageReadyListener(new App.PageReadyListener(this) {
            public static ChangeQuickRedirect 支;
            public final CRVApp a;

            {
                ConstructorCode proxy;
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                    proxy.afterSuper(this);
                } else {
                    this.a = this;
                }
            }

            public void onPageReady(Page page) {
                10 r1;
                ChangeQuickRedirect changeQuickRedirect2 = 支;
                if (changeQuickRedirect2 != null) {
                    r1 = this;
                    if (PatchProxy.proxy(page, r1, changeQuickRedirect2, "1", Page.class, Void.TYPE).isSupported) {
                        return;
                    }
                } else {
                    r1 = this;
                }
                long currentTimeMillis = System.currentTimeMillis();
                CRVApp cRVApp = r1.a;
                CRVEventTracker.stub(cRVApp, "AppFirstPageReady", currentTimeMillis);
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("timestamp", Long.valueOf(System.currentTimeMillis()));
                CRVJSIBridge.sendPushMessage(cRVApp.getNodeId(), "firstPageReady", jSONObject, (SendToWorkerCallback) null);
            }
        });
        if (this.y) {
            addAppJSIOOMListener(new MYWebViewUtils.JSIOOMListener(this) {
                public static ChangeQuickRedirect 支;
                public final CRVApp a;

                {
                    ConstructorCode proxy;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null && (proxy = PatchProxy.proxy(changeQuickRedirect2, "0", new Object[]{this})) != null) {
                        proxy.afterSuper(this);
                    } else {
                        this.a = this;
                    }
                }

                public boolean onInterceptJSIOOM(String str2, Map<String, Integer> map) {
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null) {
                        PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str2, map, String.class, Map.class, Boolean.TYPE}, this, changeQuickRedirect2, "1");
                        if (proxy.isSupported) {
                            return ((Boolean) proxy.result).booleanValue();
                        }
                    }
                    return this.a.shouldInterceptJSIOOM(str2, map);
                }
            });
        }
    }

    @CallByNative
    public void syncNativeParamsToJava(Bundle bundle, Bundle bundle2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{bundle, bundle2, Bundle.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "159").isSupported) {
            return;
        }
        this.f = bundle;
        this.g = bundle2;
    }

    @CallByNative
    public void syncStartParamFromNative(Bundle bundle) {
        CRVApp cRVApp;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, cRVApp, changeQuickRedirect, "160", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            cRVApp = this;
            bundle2 = bundle;
        }
        try {
            cRVApp.f.putAll(bundle2);
            TaskManager.INSTANCE.getCommonSessionAttrModel(this).appendStartParams(cRVApp.f, cRVApp.g);
        } catch (Throwable unused) {
        }
    }

    @NonNull
    public String toString() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "161", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return "CRVApp@" + getNodeId() + "@" + this.a;
    }

    public void writeToParcel(Parcel parcel, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{parcel, Integer.valueOf(i), Parcel.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "162").isSupported) {
            return;
        }
        super/*com.alibaba.ariver.app.NodeInstance*/.writeToParcel(parcel, i);
        parcel.writeString(this.a);
        parcel.writeLong(this.h);
        parcel.writeString(this.b);
        JSONArray parseArray = JSONUtils.parseArray(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfig("h5_filterStartParamsOnParcel", (String) null));
        if (parseArray != null && !parseArray.isEmpty()) {
            Bundle bundle = (Bundle) this.f.clone();
            AppUtils.filterBundleKey(bundle, parseArray);
            parcel.writeBundle(bundle);
        } else {
            parcel.writeBundle(this.f);
        }
        parcel.writeBundle(this.g);
        Parcelable parcelable = (Parcelable) getData(EntryInfo.class);
        if (parcelable != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(parcelable, 0);
        } else {
            parcel.writeInt(0);
        }
        Parcelable parcelable2 = (Parcelable) getData(AppModel.class);
        if (parcelable2 != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(parcelable2, 0);
        } else {
            parcel.writeInt(0);
        }
    }

    public boolean removeChild(Node node) {
        CRVApp cRVApp;
        Node node2;
        LinkedList linkedList;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cRVApp = this;
            node2 = node;
            PatchProxyResult proxy = PatchProxy.proxy(node2, cRVApp, changeQuickRedirect, "133", Node.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            cRVApp = this;
            node2 = node;
        }
        boolean removeChild = super/*com.alibaba.ariver.app.NodeInstance*/.removeChild(node2);
        CRVPage cRVPage = (CRVPage) node2;
        if (cRVPage != null && (linkedList = cRVApp.F) != null) {
            int size = linkedList.size();
            while (true) {
                size--;
                if (size < 0) {
                    break;
                }
                ((H5Listener) linkedList.get(size)).onPageDestroyed(cRVPage);
            }
        }
        return removeChild;
    }

    public boolean removePage(H5Page h5Page) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(h5Page, this, changeQuickRedirect, "137", H5Page.class, Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public class CRVResourceProvider implements ResourceProvider {
        public static ChangeQuickRedirect 支;
        public final String a;
        public final ConcurrentHashMap b;
        public final CRVApp c;

        public CRVResourceProvider(CRVApp cRVApp) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{cRVApp})) != null) {
                proxy.afterSuper(this);
                return;
            }
            this.c = cRVApp;
            this.b = new ConcurrentHashMap();
            this.a = BundleUtils.getString(cRVApp.getStartParams(), "onlineHost");
        }

        @Nullable
        public Resource getContent(ResourceQuery resourceQuery) {
            CRVResourceProvider cRVResourceProvider;
            ResourceQuery resourceQuery2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                cRVResourceProvider = this;
                resourceQuery2 = resourceQuery;
                PatchProxyResult proxy = PatchProxy.proxy(resourceQuery2, cRVResourceProvider, changeQuickRedirect, "1", ResourceQuery.class, Resource.class);
                if (proxy.isSupported) {
                    return (Resource) proxy.result;
                }
            } else {
                cRVResourceProvider = this;
                resourceQuery2 = resourceQuery;
            }
            if (resourceQuery2.isNeedAutoCompleteHost()) {
                resourceQuery2.pureUrl = FileUtils.combinePath(cRVResourceProvider.a, resourceQuery2.pureUrl);
            }
            Resource rawResource = getRawResource(resourceQuery2);
            return rawResource != null ? rawResource : (Resource) cRVResourceProvider.b.get(resourceQuery2.pureUrl);
        }

        public String getFallbackUrl(String str) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return null;
            }
            PatchProxyResult proxy = PatchProxy.proxy(str, this, changeQuickRedirect, "3", String.class, String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
            return null;
        }

        public Resource getLocalResource(String str) {
            String str2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                str2 = str;
                PatchProxyResult proxy = PatchProxy.proxy(str2, this, changeQuickRedirect, "4", String.class, Resource.class);
                if (proxy.isSupported) {
                    return (Resource) proxy.result;
                }
            } else {
                str2 = str;
            }
            return getRawResource(ResourceQuery.asUrl(str2));
        }

        @Nullable
        public Resource getRawResource(ResourceQuery resourceQuery) {
            CRVResourceProvider cRVResourceProvider;
            ResourceQuery resourceQuery2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                cRVResourceProvider = this;
                resourceQuery2 = resourceQuery;
                PatchProxyResult proxy = PatchProxy.proxy(resourceQuery2, cRVResourceProvider, changeQuickRedirect, "5", ResourceQuery.class, Resource.class);
                if (proxy.isSupported) {
                    return (Resource) proxy.result;
                }
            } else {
                cRVResourceProvider = this;
                resourceQuery2 = resourceQuery;
            }
            byte[] nativeGetResource = CRVNativeBridge.nativeGetResource(cRVResourceProvider.c.getNodeId(), resourceQuery2.pureUrl, resourceQuery2.isCanUseFallback());
            if (nativeGetResource != null) {
                return new OfflineResource(resourceQuery2.originUrl, nativeGetResource);
            }
            return null;
        }

        public boolean hasInputException() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return false;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "6", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
            return false;
        }

        public boolean isLocal() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null) {
                return false;
            }
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "7", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
            return false;
        }

        public void mapContent(String str, Resource resource) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, resource, String.class, Resource.class, Void.TYPE}, this, changeQuickRedirect, "8").isSupported) {
                return;
            }
            this.b.put(UrlUtils.purifyUrl(str), resource);
        }

        public void releaseContent() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "9", Void.TYPE).isSupported) {
                return;
            }
            this.b.clear();
        }

        public void setFallbackCache(String str, byte[] bArr) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, bArr, String.class, byte[].class, Void.TYPE}, this, changeQuickRedirect, "10").isSupported) {
                return;
            }
            this.b.put(str, new OfflineResource(str, bArr));
        }

        @Nullable
        public Resource getContent(String str) {
            String str2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                str2 = str;
                PatchProxyResult proxy = PatchProxy.proxy(str2, this, changeQuickRedirect, "2", String.class, Resource.class);
                if (proxy.isSupported) {
                    return (Resource) proxy.result;
                }
            } else {
                str2 = str;
            }
            return getRawResource(ResourceQuery.asUrl(str2));
        }
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public CRVApp(long j, String str, String str2) {
        super((Node) r1[0]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "1", (objArr = new Object[]{Long.valueOf(j), str, str2}))) != null) {
            proxy.afterSuper(this);
            return;
        }
        super((Node) RVProxy.get(AppManager.class));
        this.b = X;
        this.d = new CountDownLatch(1);
        this.j = new CountDownLatch(1);
        this.p = true;
        this.q = false;
        this.r = false;
        this.s = false;
        this.t = false;
        this.y = false;
        this.z = false;
        this.B = new AtomicBoolean(true);
        this.C = false;
        this.D = false;
        this.E = new Object();
        this.F = new LinkedList();
        this.K = Collections.synchronizedList(new ArrayList());
        this.L = Collections.synchronizedList(new ArrayList());
        this.M = new ArrayList();
        this.tabPageResumeListener = null;
        this.O = false;
        this.P = new AtomicBoolean(false);
        this.Q = null;
        this.R = true;
        this.T = false;
        this.U = false;
        this.W = new H5MemData();
        AppManager appManager = (AppManager) RVProxy.get(AppManager.class);
        appManager.pushChild(this);
        appManager.manualPushStack(this);
        setNodeId(j);
        this.a = str;
        this.i = str2;
        this.l = new H5PluginManagerImpl(this);
        this.n = new NebulaAppMsgReceiver(this);
        this.u = new XRiverBufferStore();
        H5Refer.referUrl = LoggerFactory.getLogContext().getContextParam("refViewID");
        Nebula.getService().initServicePlugin();
        Nebula.getService().addSession(this);
        this.r = "yes".equalsIgnoreCase(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("h5_removeNebulaServiceSession", "yes"));
        this.y = "yes".equalsIgnoreCase(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("ta_enableInterceptJSIOOMListener", "no"));
        RVLogger.d("XRIVER:Android:CRVApp", "create with nodeId " + j + " appId: " + str);
    }
}
