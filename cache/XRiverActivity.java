//
// Decompiled by Jadx - 1622ms
//
package com.alipay.mobile.nebulax.xriver.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.alibaba.ariver.app.api.App;
import com.alibaba.ariver.app.api.Page;
import com.alibaba.ariver.app.api.activity.ActivityAnimBean;
import com.alibaba.ariver.app.api.activity.StartClientBundle;
import com.alibaba.ariver.app.api.performance.LifeCycleBlockOptimizeEventTracker;
import com.alibaba.ariver.app.api.point.activity.ActivityOnDestroyPoint;
import com.alibaba.ariver.app.api.ui.navigation.NavigationBarOperator;
import com.alibaba.ariver.app.api.ui.navigation.SharedNavigationBarOperator;
import com.alibaba.ariver.app.ipc.IpcClientUtils;
import com.alibaba.ariver.engine.api.EngineUtils;
import com.alibaba.ariver.engine.api.bridge.SendToWorkerCallback;
import com.alibaba.ariver.engine.api.bridge.model.SendToRenderCallback;
import com.alibaba.ariver.integration.RVInitializer;
import com.alibaba.ariver.jsapi.logging.AppPerformanceBridgeExtension;
import com.alibaba.ariver.kernel.api.extension.ExtensionPoint;
import com.alibaba.ariver.kernel.api.track.EventTracker;
import com.alibaba.ariver.kernel.common.RVProxy;
import com.alibaba.ariver.kernel.common.service.RVConfigService;
import com.alibaba.ariver.kernel.common.service.executor.ExecutorType;
import com.alibaba.ariver.kernel.common.utils.BundleUtils;
import com.alibaba.ariver.kernel.common.utils.CollectionUtils;
import com.alibaba.ariver.kernel.common.utils.ExecutorUtils;
import com.alibaba.ariver.kernel.common.utils.JSONUtils;
import com.alibaba.ariver.kernel.common.utils.ProcessUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.ariver.kernel.common.utils.RVTraceKey;
import com.alibaba.ariver.kernel.common.utils.RVTraceUtils;
import com.alibaba.ariver.resource.api.models.AppInfoModel;
import com.alibaba.ariver.resource.api.models.AppInfoScene;
import com.alibaba.ariver.resource.api.models.AppModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.xriver.android.CRVUtils;
import com.alibaba.xriver.android.bridge.CRVNativeBridge;
import com.alibaba.xriver.android.node.CRVApp;
import com.alibaba.xriver.android.resource.CRVAppModelUtils;
import com.alibaba.xriver.android.taskwatch.TaskManager;
import com.alibaba.xriver.android.taskwatch.TaskModel;
import com.alibaba.xriver.android.ui.XRiverActivityHelper;
import com.alibaba.xriver.android.utils.ExitEvent;
import com.alibaba.xriver.android.utils.TaskMonitor;
import com.alibaba.xriver.android.utils.TaskMonitorUtils;
import com.alipay.android.phone.fulllinktracker.api.FullLinkSdk;
import com.alipay.android.phone.fulllinktracker.internal.sync.SyncData;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.app.Activity_onActivityResult_int;
import com.alipay.dexaop.stub.android.app.Activity_onCreate_androidosBundle_stub;
import com.alipay.dexaop.stub.android.app.Activity_onDestroy__stub;
import com.alipay.dexaop.stub.android.app.Activity_onNewIntent_androidcontentIntent_stub;
import com.alipay.dexaop.stub.android.app.Activity_onPause__stub;
import com.alipay.dexaop.stub.android.app.Activity_onRequestPermissionsResult_int;
import com.alipay.dexaop.stub.android.app.Activity_onResume__stub;
import com.alipay.dexaop.stub.android.app.Activity_onSaveInstanceState_androidosBundle_stub;
import com.alipay.dexaop.stub.android.app.Activity_onStop__stub;
import com.alipay.dexaop.stub.android.app.Activity_onUserInteraction__stub;
import com.alipay.dexaop.stub.android.app.Activity_onUserLeaveHint__stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.content.ContextWrapper_attachBaseContext_androidcontentContext_stub;
import com.alipay.dexaop.stub.android.view.KeyEvent;
import com.alipay.dexaop.stub.android.view.Window;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.base.config.ConfigService;
import com.alipay.mobile.common.logging.CrashBridge;
import com.alipay.mobile.common.logging.api.LoggerFactory;
import com.alipay.mobile.common.logging.api.antevent.AntEvent;
import com.alipay.mobile.core.impl.MicroApplicationContextImpl;
import com.alipay.mobile.framework.LauncherApplicationAgent;
import com.alipay.mobile.framework.MicroApplicationContext;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.framework.app.ActivityApplication;
import com.alipay.mobile.framework.app.ApplicationDescription;
import com.alipay.mobile.framework.app.IApplicationEngine;
import com.alipay.mobile.framework.app.ui.BaseFragmentActivity;
import com.alipay.mobile.framework.pipeline.TaskControlManager;
import com.alipay.mobile.framework.service.common.RpcService;
import com.alipay.mobile.framework.stack.ActivityUtils;
import com.alipay.mobile.h5container.api.H5Flag;
import com.alipay.mobile.h5container.service.H5Service;
import com.alipay.mobile.liteprocess.ConfigShared;
import com.alipay.mobile.liteprocess.LiteNebulaXCompat;
import com.alipay.mobile.liteprocess.LiteProcessApi;
import com.alipay.mobile.liteprocess.LiteProcessClientManager;
import com.alipay.mobile.liteprocess.LiteXRiverCompat;
import com.alipay.mobile.liteprocess.Util;
import com.alipay.mobile.liteprocess.advice.ContextExtensionKt;
import com.alipay.mobile.liteprocess.advice.OrphanTaskState;
import com.alipay.mobile.liteprocess.ipc.IpcMsgClient;
import com.alipay.mobile.liteprocess.render.MicroPhoenix;
import com.alipay.mobile.nebula.activity.INebulaActivity;
import com.alipay.mobile.nebula.log.H5LogData;
import com.alipay.mobile.nebula.log.H5LogUtil;
import com.alipay.mobile.nebula.performance.PerfTestUtil;
import com.alipay.mobile.nebula.provider.H5BizProvider;
import com.alipay.mobile.nebula.util.H5StatusBarUtils;
import com.alipay.mobile.nebula.util.InsideUtils;
import com.alipay.mobile.nebula.util.PerfUtils;
import com.alipay.mobile.nebula.util.XriverH5Utils;
import com.alipay.mobile.nebulacore.Nebula;
import com.alipay.mobile.nebulacore.wallet.TinyLifecycle;
import com.alipay.mobile.nebulaintegration.obfuscated.aa;
import com.alipay.mobile.nebulaintegration.obfuscated.bk;
import com.alipay.mobile.nebulaintegration.obfuscated.d1;
import com.alipay.mobile.nebulaintegration.obfuscated.d4;
import com.alipay.mobile.nebulaintegration.obfuscated.e3;
import com.alipay.mobile.nebulaintegration.obfuscated.f2;
import com.alipay.mobile.nebulaintegration.obfuscated.g2;
import com.alipay.mobile.nebulaintegration.obfuscated.gb;
import com.alipay.mobile.nebulaintegration.obfuscated.j;
import com.alipay.mobile.nebulaintegration.obfuscated.k4;
import com.alipay.mobile.nebulaintegration.obfuscated.n4;
import com.alipay.mobile.nebulaintegration.obfuscated.of;
import com.alipay.mobile.nebulaintegration.obfuscated.on;
import com.alipay.mobile.nebulaintegration.obfuscated.p;
import com.alipay.mobile.nebulaintegration.obfuscated.pn;
import com.alipay.mobile.nebulaintegration.obfuscated.q9;
import com.alipay.mobile.nebulaintegration.obfuscated.rn;
import com.alipay.mobile.nebulaintegration.obfuscated.sn;
import com.alipay.mobile.nebulaintegration.obfuscated.u;
import com.alipay.mobile.nebulaintegration.obfuscated.u3;
import com.alipay.mobile.nebulaintegration.obfuscated.v;
import com.alipay.mobile.nebulaintegration.obfuscated.x8;
import com.alipay.mobile.nebulaintegration.obfuscated.y3;
import com.alipay.mobile.nebulaintegration.obfuscated.yn;
import com.alipay.mobile.nebulaintegration.obfuscated.z8;
import com.alipay.mobile.nebulax.NebulaXCompat;
import com.alipay.mobile.nebulax.integration.api.MultiAppUtils;
import com.alipay.mobile.nebulax.integration.api.TinyWindowFocusChangedHelper;
import com.alipay.mobile.nebulax.integration.base.api.INebulaPage;
import com.alipay.mobile.nebulax.integration.base.api.NXUtils;
import com.alipay.mobile.nebulax.integration.base.config.ConfigUtils;
import com.alipay.mobile.nebulax.integration.base.config.FastConfigList;
import com.alipay.mobile.nebulax.integration.base.halfscreen.HalfscreenUtils;
import com.alipay.mobile.nebulax.integration.base.halfscreen.view.slidinguppanel.SlidingUpPanelLayout;
import com.alipay.mobile.nebulax.integration.base.view.navigation.ActivityNavigationBarHelper;
import com.alipay.mobile.nebulax.integration.base.view.navigation.util.NavigationBarUtils;
import com.alipay.mobile.nebulax.integration.mpaas.R;
import com.alipay.mobile.nebulax.integration.mpaas.main.AliveHandler;
import com.alipay.mobile.nebulax.integration.mpaas.main.H5ApplicationDelegate;
import com.alipay.mobile.nebulax.integration.mpaas.proxy.impl.rpc.MpaasRemoteRpcServiceImpl;
import com.alipay.mobile.nebulax.resource.api.appinfo.AppInfoUtil;
import com.alipay.mobile.nebulax.resource.api.legacy.NXResourceLegacyUtils;
import com.alipay.mobile.nebulax.resource.api.util.PreloadUtils;
import com.alipay.mobile.nebulax.xriver.MicroPhoenixBuilder;
import com.alipay.mobile.nebulax.xriver.XRiverAppContext;
import com.alipay.mobile.nebulax.xriver.XRiverApplicationDelegate;
import com.alipay.mobile.performance.PerformanceDog;
import com.alipay.mobile.quinox.data.DataProvider;
import com.alipay.mobile.quinox.utils.ContextHolder;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt__IterablesKt;
import kotlin.collections.CollectionsKt___CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt__StringsJVMKt;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public class XRiverActivity extends BaseFragmentActivity implements Activity_onActivityResult_int.int.androidcontentIntent_stub, Activity_onCreate_androidosBundle_stub, Activity_onDestroy__stub, Activity_onNewIntent_androidcontentIntent_stub, Activity_onPause__stub, Activity_onRequestPermissionsResult_int.ArjavalangString.Arint_stub, Activity_onResume__stub, Activity_onSaveInstanceState_androidosBundle_stub, Activity_onStop__stub, Activity_onUserInteraction__stub, Activity_onUserLeaveHint__stub, ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub, ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub, ContextWrapper_attachBaseContext_androidcontentContext_stub, KeyEvent.Callback_onKeyDown_int.androidviewKeyEvent_stub, Window.Callback_onWindowFocusChanged_boolean_stub, OrphanTaskState, INebulaActivity {
    public static final SparseArray<Class<? extends Activity>> ACTIVITY_CLASSES;
    private static final String COMPLETE_PRELOAD_ACTIVITY_SIMPLE_NAME = "NebulaPreloadActivity";
    private static final int MYPDFVIEWER_RESULT_PRE_CODE = -68600;
    static final String RESTORE_APPID = "RESTORE_APPID";
    private static final String RESTORE_PARAMS = "RESTORE_PARAMS";
    private static final String RESTORE_TRADE_PAY = "RESTORE_TRADE_PAY";
    private static final String TAG = "XRIVER:Android:XRiverActivity";
    public static boolean sAlreadyCreated;
    public static ChangeQuickRedirect 支;
    private int i;
    protected boolean isXRiverPreloadActivity;
    private ActivityAnimBean mActivityAnimBean;
    private ActivityNavigationBarHelper mActivityNavigationBarHelper;
    private long mAttachBaseContextSystemTime;
    private long mAttachBaseContextTime;
    private sn mBroadcastReceiver;
    private CRVApp mCRVApp;
    private boolean mCloseAllAnim;
    private final n4 mExitTagged;
    protected boolean mFromRestore;
    private boolean mIsHalfScreenApp;
    private boolean mIsResizeable;
    private boolean mIsTinyApp;
    private long mNodeId;
    private MicroPhoenix mPhoenix;
    private Bundle mSceneParams;
    private Bundle mStartParams;
    private int mWrapperViewWidth;
    protected XRiverActivityHelper mXRiverActivityHelper;
    private String monitorId;
    private Intent newIntent;

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class App01 extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public App01() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class App02 extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public App02() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class App03 extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public App03() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class App04 extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public App04() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class App05 extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public App05() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class MagicFullScreen extends XRiverActivity {
        public static ChangeQuickRedirect 支;

        public MagicFullScreen() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLite1 extends XRiverLiteBase {
        public static ChangeQuickRedirect 支;

        public XRiverLite1() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLite2 extends XRiverLiteBase {
        public static ChangeQuickRedirect 支;

        public XRiverLite2() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLite3 extends XRiverLiteBase {
        public static ChangeQuickRedirect 支;

        public XRiverLite3() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLite4 extends XRiverLiteBase {
        public static ChangeQuickRedirect 支;

        public XRiverLite4() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLite5 extends XRiverLiteBase {
        public static ChangeQuickRedirect 支;

        public XRiverLite5() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class XRiverLiteBase extends XRiverActivity implements Activity_onCreate_androidosBundle_stub, Activity_onDestroy__stub, Activity_onStop__stub, Activity_onUserLeaveHint__stub, ContextWrapper_attachBaseContext_androidcontentContext_stub {
        public static Intent sIpcIntent;
        public static ChangeQuickRedirect 支;

        public XRiverLiteBase() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __attachBaseContext_stub_private(Context context) {
            Context context2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                context2 = context;
                if (PatchProxy.proxy(context2, this, changeQuickRedirect, "2", Context.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                context2 = context;
            }
            NXUtils.doGlobalSetup();
            RVTraceUtils.asyncTraceEnd(RVTraceKey.RV_appPhase_processInit);
            RVTraceUtils.asyncTraceBegin(RVTraceKey.RV_appPhase_uICreate);
            super.attachBaseContext(context2);
            RVInitializer.setupProxy(context2);
            Handler asyncHandler = LiteProcessClientManager.getAsyncHandler();
            b bVar = new b(this);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(bVar);
            DexAOPEntry.lite_hanlerPostProxy(asyncHandler, bVar);
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r11v11, types: [com.alipay.mobile.framework.app.IApplicationEngine, java.lang.Object] */
        private void __onCreate_stub_private(Bundle bundle) {
            XRiverActivity xRiverActivity;
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                xRiverActivity = this;
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, xRiverActivity, changeQuickRedirect, "4", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                xRiverActivity = this;
                bundle2 = bundle;
            }
            LoggerFactory.getTraceLogger().debug(XRiverActivity.TAG, "XRiverActivity.onCreate in lite " + this);
            Bundle bundle3 = null;
            if (sIpcIntent != null) {
                LoggerFactory.getTraceLogger().debug(XRiverActivity.TAG, "XRiverActivity.onCreate use ipc intent: " + sIpcIntent);
                long longExtra = getIntent().getLongExtra("EXTRA_INTENT_TIME_STAMP", 0L);
                long longExtra2 = sIpcIntent.getLongExtra("EXTRA_INTENT_TIME_STAMP", 0L);
                if (longExtra2 != longExtra) {
                    LoggerFactory.getTraceLogger().debug(XRiverActivity.TAG, "XRiverActivity.onCreate intent mismatch! ipcTs: " + longExtra2 + " originTs: " + longExtra);
                    H5LogUtil.logNebulaTech(H5LogData.seedId("h5_nebulaActivityIntentMismatch"));
                }
                setIntent(sIpcIntent);
                sIpcIntent = null;
            }
            getIntent().setExtrasClassLoader(XRiverActivity.class.getClassLoader());
            Intent intent = getIntent();
            if (intent == null) {
                RVLogger.d(XRiverActivity.TAG, "isCompletePreloadInOtherActivity intent is null");
            } else {
                if ("2021002128684774".equals(intent.getStringExtra("app_id")) && !getClass().getName().contains(XRiverActivity.COMPLETE_PRELOAD_ACTIVITY_SIMPLE_NAME)) {
                    RVLogger.d(XRiverActivity.TAG, "error!!! start 2021002128684774 in !NebulaPreloadActivity class = ".concat(getClass().getName()));
                    H5LogUtil.logNebulaTech(H5LogData.seedId("isCompletePreloadInOtherActivity").param1().add("className", getClass().getName()));
                } else {
                    try {
                        StartClientBundle parcelable = BundleUtils.getParcelable(getIntent().getExtras(), "ariverStartBundle");
                        if (parcelable != null) {
                            String string = parcelable.startParams.getString("CompletePreload");
                            if ("yes".equals(string) && !getClass().getName().contains(XRiverActivity.COMPLETE_PRELOAD_ACTIVITY_SIMPLE_NAME)) {
                                RVLogger.d(XRiverActivity.TAG, "error!!! start completePreloadStr = " + string + " in !NebulaPreloadActivity class = " + getClass().getName());
                            }
                        }
                    } catch (Throwable unused) {
                        RVLogger.e(XRiverActivity.TAG, "isCompletePreloadInOtherActivity intent is null");
                    }
                }
                RVLogger.d(XRiverActivity.TAG, "error !!! isCompletePreloadInOtherActivity.onCreate do finish!!!");
                super.onCreate(bundle2);
                finish();
                return;
            }
            if (intent != null && !intent.getBooleanExtra("IS_LITE_MOVE_TASK", false)) {
                NebulaXCompat.isCurrentNebulaX = false;
                PerfTestUtil.traceBeginSection("XRiverActivityHelper_onCreate_setUpInLite");
                CrashBridge.addCrashHeadInfo("nebulax", "yes");
                CrashBridge.addCrashHeadInfo("xriver", "yes");
                Intent intent2 = getIntent();
                if (intent2 != null) {
                    Util.setContext(this);
                    XriverH5Utils.findServiceByInterface(H5Service.class.getName());
                    String stringExtra = intent2.getStringExtra("app_id");
                    LoggerFactory.getTraceLogger().debug(XRiverActivity.TAG, "start " + this + " in appId: " + stringExtra);
                    if (!TextUtils.isEmpty(stringExtra)) {
                        ApplicationDescription applicationDescription = new ApplicationDescription();
                        applicationDescription.setAppId(stringExtra);
                        applicationDescription.setEngineType("NXShadow");
                        LauncherApplicationAgent.getInstance().getMicroApplicationContext().registerApplicationEngine("NXShadow", (IApplicationEngine) new Object());
                        LauncherApplicationAgent.getInstance().getMicroApplicationContext().addDescription(new ApplicationDescription[]{applicationDescription});
                    }
                    Bundle bundleExtra = intent2.getBundleExtra("nxConfigValues");
                    if (bundleExtra != null) {
                        for (String str : FastConfigList.KEY_SET) {
                            String string2 = bundleExtra.getString(str, "");
                            RVLogger.debug(XRiverActivity.TAG, "put fastCfg key " + str + " " + string2);
                            ((RVConfigService) RVProxy.get(RVConfigService.class)).putConfigCache(str, string2);
                        }
                    }
                    LauncherApplicationAgent.getInstance().getMicroApplicationContext().registerService(RpcService.class.getName(), new MpaasRemoteRpcServiceImpl());
                }
                PerfTestUtil.traceEndSection("XRiverActivityHelper_onCreate_setUpInLite");
                PerfTestUtil.traceBeginSection("XRiverActivityHelper_onCreate_RVInitializer");
                RVInitializer.init(this);
                Application applicationContext = LauncherApplicationAgent.getInstance().getApplicationContext();
                if (!CRVUtils.loadSo(applicationContext)) {
                    RVLogger.d(XRiverActivity.TAG, "CRVUtils loadSo in XRiverLiteBase failed");
                    super.onCreate(bundle2);
                    finish();
                    return;
                }
                CRVUtils.init(applicationContext);
                LiteXRiverCompat.initInLite(this, (App) null, getIntent());
                PerfTestUtil.traceEndSection("XRiverActivityHelper_onCreate_RVInitializer");
                try {
                    LauncherApplicationAgent.getInstance().getBundleContext().loadBundle("android-phone-fulllinktracker");
                    RVLogger.d(XRiverActivity.TAG, "load fullLinkClassLoader:" + SyncData.class.getClassLoader() + ", on " + this);
                } catch (Throwable th) {
                    RVLogger.e(XRiverActivity.TAG, "BundleLoader android-phone-fulllinktracker error", th);
                }
                try {
                    Parcelable parcelableExtra = getIntent().getParcelableExtra("flRestoreData");
                    if (parcelableExtra != null) {
                        RVLogger.d(XRiverActivity.TAG, "FullLinkSdk restore flData!");
                        FullLinkSdk.getDriverApi().restoreFLData(parcelableExtra);
                    } else if (intent.getBundleExtra("mExtras") != null) {
                        Parcelable parcelable2 = intent.getBundleExtra("mExtras").getParcelable("flRestoreData");
                        if (parcelable2 != null) {
                            RVLogger.d(XRiverActivity.TAG, "FullLinkSdk restore flData! 2");
                            FullLinkSdk.getDriverApi().restoreFLData(parcelable2);
                        } else {
                            H5LogUtil.logNebulaTech(H5LogData.seedId("h5_LiteProcessActivityMissFlRestoreData"));
                        }
                    } else {
                        H5LogUtil.logNebulaTech(H5LogData.seedId("h5_LiteProcessActivityMissFlRestoreData"));
                    }
                } catch (BadParcelableException e) {
                    RVLogger.e(XRiverActivity.TAG, "BadParcelableException ", e);
                    H5LogUtil.logNebulaTech(H5LogData.seedId("h5_LiteProcessActivityFlRestoreDataError"));
                }
                super.onCreate(bundle2);
                if (isFinishing()) {
                    RVLogger.d(XRiverActivity.TAG, "XRiverActivity.onCreate already finished by super.onCreate ");
                    return;
                }
                NXResourceLegacyUtils.replaceH5AppProvider();
                Handler asyncHandler = LiteProcessClientManager.getAsyncHandler();
                c cVar = new c(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(cVar);
                DexAOPEntry.lite_hanlerPostProxy(asyncHandler, cVar);
                XRiverActivityHelper xRiverActivityHelper = xRiverActivity.mXRiverActivityHelper;
                if (xRiverActivityHelper != null && xRiverActivityHelper.getApp() != null) {
                    IpcClientUtils.sendMsgToServerByApp(xRiverActivity.mXRiverActivityHelper.getApp(), 103, (Bundle) null);
                    bundle3 = xRiverActivity.mXRiverActivityHelper.getApp().getStartParams();
                }
                if (bundle3 != null && "YES".equals(bundle3.getString("updateApp")) && bundle3.getInt("last_lpid", -1) != -1) {
                    int i = bundle3.getInt("last_lpid", -1);
                    if (LiteProcessApi.getLpid() != i) {
                        Bundle bundle4 = new Bundle();
                        bundle4.putInt("last_lpid", i);
                        IpcClientUtils.sendMsgToServerByApp(xRiverActivity.mXRiverActivityHelper.getApp(), 102, bundle4);
                    }
                    bundle3.remove("last_lpid");
                    bundle3.remove("updateApp");
                    RVLogger.w(XRiverActivity.TAG, "sendToServer destroy on update, lite = " + i);
                }
                gb.f = xRiverActivity.mXRiverActivityHelper.getApp().getStartToken();
                RVLogger.d(XRiverActivity.TAG, "XRiverActivity.onCreate done " + xRiverActivity.mXRiverActivityHelper.getApp().getAppId() + " this startToken = " + xRiverActivity.mXRiverActivityHelper.getApp().getStartToken());
                return;
            }
            LoggerFactory.getTraceLogger().error(XRiverActivity.TAG, "UNEXPECTED onCreate!!! Just Finish!!!");
            super.onCreate(bundle2);
            finish();
        }

        /* JADX WARN: Multi-variable type inference failed */
        private void __onDestroy_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "6", Void.TYPE).isSupported) {
                return;
            }
            boolean isTaskRoot = isTaskRoot();
            super.onDestroy();
            if (isTaskRoot) {
                TaskControlManager.getInstance().start();
                if (Nebula.getH5LogHandler() != null) {
                    Nebula.getH5LogHandler().upload();
                }
                if (!this.isXRiverPreloadActivity) {
                    f fVar = new f(this);
                    DexAOPEntry.java_lang_Runnable_newInstance_Created(fVar);
                    ExecutorUtils.runOnMain(fVar, 500L);
                } else {
                    RVLogger.d(XRiverActivity.TAG, "XRiverActivity.onDestroy isNebulaPreloadActivity");
                }
                TaskControlManager.getInstance().end();
            }
        }

        private void __onStop_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "8", Void.TYPE).isSupported) {
                return;
            }
            super.onStop();
            if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("ta_flushLogOnLiteStop", true)) {
                ExecutorType executorType = ExecutorType.IO;
                d dVar = new d(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(dVar);
                ExecutorUtils.execute(executorType, dVar);
            }
        }

        private void __onUserLeaveHint_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "10", Void.TYPE).isSupported) {
                return;
            }
            super.onUserLeaveHint();
            LiteNebulaXCompat.onUserLeaveHint();
        }

        public static ActivityApplication e(XRiverLiteBase xRiverLiteBase) {
            XRiverLiteBase xRiverLiteBase2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                xRiverLiteBase2 = xRiverLiteBase;
                PatchProxyResult proxy = PatchProxy.proxy(xRiverLiteBase2, (Object) null, changeQuickRedirect, "12", XRiverLiteBase.class, ActivityApplication.class);
                if (proxy.isSupported) {
                    return (ActivityApplication) proxy.result;
                }
            } else {
                xRiverLiteBase2 = xRiverLiteBase;
            }
            return ((BaseFragmentActivity) xRiverLiteBase2).mApp;
        }

        @Override
        public void __attachBaseContext_stub(Context context) {
            Context context2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                context2 = context;
                if (PatchProxy.proxy(context2, this, changeQuickRedirect, "1", Context.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                context2 = context;
            }
            __attachBaseContext_stub_private(context2);
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "3", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void __onDestroy_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "5", Void.TYPE).isSupported) {
                __onDestroy_stub_private();
            }
        }

        @Override
        public void __onStop_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "7", Void.TYPE).isSupported) {
                __onStop_stub_private();
            }
        }

        @Override
        public void __onUserLeaveHint_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "9", Void.TYPE).isSupported) {
                __onUserLeaveHint_stub_private();
            }
        }

        @Override
        public void attachBaseContext(Context context) {
            Context context2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                context2 = context;
                if (PatchProxy.proxy(context2, this, changeQuickRedirect, "11", Context.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                context2 = context;
            }
            if (getClass() != XRiverLiteBase.class) {
                __attachBaseContext_stub_private(context2);
            } else {
                DexAOPEntry.android_content_ContextWrapper_attachBaseContext_proxy(XRiverLiteBase.class, this, context2);
            }
        }

        @Override
        public void onCreate(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "13", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            if (getClass() != XRiverLiteBase.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(XRiverLiteBase.class, this, bundle2);
            }
        }

        @Override
        public void onDestroy() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "14", Void.TYPE).isSupported) {
                if (getClass() != XRiverLiteBase.class) {
                    __onDestroy_stub_private();
                } else {
                    DexAOPEntry.android_app_Activity_onDestroy_proxy(XRiverLiteBase.class, this);
                }
            }
        }

        @Override
        public void onStop() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "15", Void.TYPE).isSupported) {
                if (getClass() != XRiverLiteBase.class) {
                    __onStop_stub_private();
                } else {
                    DexAOPEntry.android_app_Activity_onStop_proxy(XRiverLiteBase.class, this);
                }
            }
        }

        @Override
        public void onUserLeaveHint() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "16", Void.TYPE).isSupported) {
                if (getClass() != XRiverLiteBase.class) {
                    __onUserLeaveHint_stub_private();
                } else {
                    DexAOPEntry.android_app_Activity_onUserLeaveHint_proxy(XRiverLiteBase.class, this);
                }
            }
        }
    }

    static {
        SparseArray<Class<? extends Activity>> sparseArray = new SparseArray<>();
        sparseArray.put(1, XRiverLite1.class);
        sparseArray.put(2, XRiverLite2.class);
        sparseArray.put(3, XRiverLite3.class);
        sparseArray.put(4, XRiverLite4.class);
        sparseArray.put(5, XRiverLite5.class);
        ACTIVITY_CLASSES = sparseArray;
    }

    public XRiverActivity() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "1")) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.isXRiverPreloadActivity = false;
        this.mIsHalfScreenApp = false;
        this.mIsResizeable = false;
        this.mWrapperViewWidth = 0;
        this.mIsTinyApp = true;
        this.mBroadcastReceiver = new sn(this);
        this.mExitTagged = new n4();
        this.i = 0;
        this.newIntent = null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __attachBaseContext_stub_private(Context context) {
        XRiverActivity xRiverActivity;
        Context context2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            context2 = context;
            if (PatchProxy.proxy(context2, xRiverActivity, changeQuickRedirect, "3", Context.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
            context2 = context;
        }
        TinyLifecycle.activityAttachBaseContext();
        AppPerformanceBridgeExtension.sTinyStartingT1 = true;
        AppPerformanceBridgeExtension.sTinyStartingT2 = true;
        NXUtils.doGlobalSetup();
        xRiverActivity.mAttachBaseContextTime = SystemClock.elapsedRealtime();
        xRiverActivity.mAttachBaseContextSystemTime = System.currentTimeMillis();
        sAlreadyCreated = true;
        RVTraceUtils.asyncTraceEnd(RVTraceKey.RV_appPhase_processInit);
        RVTraceUtils.asyncTraceBegin(RVTraceKey.RV_appPhase_uICreate);
        super/*android.content.ContextWrapper*/.attachBaseContext(context2);
        RVInitializer.setupProxy(context2);
    }

    private void __onActivityResult_stub_private(int i, int i2, Intent intent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), intent, Integer.TYPE, Integer.TYPE, Intent.class, Void.TYPE}, this, changeQuickRedirect, "5").isSupported) {
            return;
        }
        super/*android.support.v4.app.FragmentActivity*/.onActivityResult(i, i2, intent);
        LoggerFactory.getTraceLogger().debug(TAG, "XRiverActivity.onActivityResult resultCode = " + i2);
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onActivityResult(i, i2, intent);
            if (i2 / 100 == MYPDFVIEWER_RESULT_PRE_CODE) {
                this.mXRiverActivityHelper.onDestroy();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onConfigurationChanged_stub_private(Configuration configuration) {
        XRiverActivity xRiverActivity;
        Configuration configuration2;
        Page activePage;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, xRiverActivity, changeQuickRedirect, "7", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
            configuration2 = configuration;
        }
        super/*android.support.v4.app.FragmentActivity*/.onConfigurationChanged(configuration2);
        RVLogger.d(TAG, "XRiverActivity.onConfigurationChanged " + configuration2);
        XRiverActivityHelper xRiverActivityHelper = xRiverActivity.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onConfigurationChanged(configuration2);
        }
        getResources().updateConfiguration(configuration2, getResources().getDisplayMetrics());
        if (aa.h()) {
            String str = aa.h;
            aa.h = null;
            aa.c = 0.0f;
            aa.d = null;
            if (!TextUtils.equals(str, aa.d(this)) && (activePage = xRiverActivity.mCRVApp.getActivePage()) != null && activePage.getRender() != null) {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("screenType", aa.d(this));
                if (aa.i == 0) {
                    try {
                        aa.i = LauncherApplicationAgent.getInstance().getApplicationContext().getResources().getConfiguration().screenWidthDp;
                    } catch (Exception e) {
                        RVLogger.e("NebulaX.AriverInt:LargeScreenUtils", "getScreenWidthDp fallback failed", e);
                    }
                }
                jSONObject.put("screenWidth", Integer.valueOf(aa.i));
                if (aa.j == 0) {
                    try {
                        aa.j = LauncherApplicationAgent.getInstance().getApplicationContext().getResources().getConfiguration().screenHeightDp;
                    } catch (Exception e2) {
                        RVLogger.e("NebulaX.AriverInt:LargeScreenUtils", "getScreenHeightDp fallback failed", e2);
                    }
                }
                jSONObject.put("screenHeight", Integer.valueOf(aa.j));
                EngineUtils.sendToRender(activePage.getRender(), "windowResize", jSONObject, (SendToRenderCallback) null);
                RVLogger.d(TAG, "sendToRender " + activePage.getRender() + " done");
            }
            setWrapperViewWidth(0);
        }
        aa.a(this, xRiverActivity.mStartParams, xRiverActivity.mCRVApp, CRVNativeBridge.isInMagicMode());
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent("android.intent.action.CONFIGURATION_CHANGED_NEBULA");
        intent.putExtra("config", configuration2);
        localBroadcastManager.sendBroadcast(intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:102:0x0562  */
    /* JADX WARN: Removed duplicated region for block: B:128:0x0729  */
    /* JADX WARN: Removed duplicated region for block: B:144:0x07a4  */
    /* JADX WARN: Removed duplicated region for block: B:149:0x07ce A[Catch: all -> 0x07d3, TryCatch #1 {all -> 0x07d3, blocks: (B:147:0x07c6, B:149:0x07ce, B:151:0x07d8, B:153:0x07df, B:154:0x07e7), top: B:146:0x07c6 }] */
    /* JADX WARN: Removed duplicated region for block: B:151:0x07d8 A[Catch: all -> 0x07d3, TryCatch #1 {all -> 0x07d3, blocks: (B:147:0x07c6, B:149:0x07ce, B:151:0x07d8, B:153:0x07df, B:154:0x07e7), top: B:146:0x07c6 }] */
    /* JADX WARN: Removed duplicated region for block: B:153:0x07df A[Catch: all -> 0x07d3, TryCatch #1 {all -> 0x07d3, blocks: (B:147:0x07c6, B:149:0x07ce, B:151:0x07d8, B:153:0x07df, B:154:0x07e7), top: B:146:0x07c6 }] */
    /* JADX WARN: Removed duplicated region for block: B:157:0x07fc  */
    /* JADX WARN: Removed duplicated region for block: B:161:0x07d5  */
    /* JADX WARN: Removed duplicated region for block: B:208:0x04de  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x01f5  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x01fb  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0209 A[Catch: all -> 0x0216, TRY_LEAVE, TryCatch #2 {all -> 0x0216, blocks: (B:42:0x0200, B:44:0x0209), top: B:41:0x0200 }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x021a  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x0357 A[ORIG_RETURN, RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x0358  */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0492  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x04d9  */
    /* JADX WARN: Removed duplicated region for block: B:96:0x0528  */
    /* JADX WARN: Removed duplicated region for block: B:99:0x054e  */
    /* JADX WARN: Type inference failed for: r0v124, types: [java.lang.Object, com.alipay.mobile.nebulaintegration.obfuscated.z] */
    /* JADX WARN: Type inference failed for: r0v28, types: [java.lang.Object, com.alipay.mobile.nebulaintegration.obfuscated.yn] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void __onCreate_stub_private(Bundle bundle) {
        FragmentActivity fragmentActivity;
        Bundle bundle2;
        TaskMonitor taskMonitor;
        long longExtra;
        Enum extractScene;
        Bundle bundleExtra;
        Enum r24;
        Bundle bundle3;
        TaskMonitor taskMonitor2;
        String str;
        boolean z;
        boolean z2;
        CRVApp cRVApp;
        Bundle bundle4;
        String str2;
        Bundle bundle5;
        boolean z3;
        String string;
        Bundle bundle6;
        boolean z4;
        boolean z5;
        Object obj;
        Throwable th;
        FragmentManager supportFragmentManager;
        FragmentTransaction fragmentTransaction;
        H5BizProvider h5BizProvider;
        AppModel transferNativeByteArrayToJava;
        Intent intent;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            fragmentActivity = this;
            if (PatchProxy.proxy(bundle, this, changeQuickRedirect, "9", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            fragmentActivity = this;
            bundle2 = bundle;
        }
        TinyLifecycle.activityOnCreate();
        long longExtra2 = fragmentActivity.getIntent().getLongExtra("START_XRIVER_ACTIVITY_TIME", -1L);
        String stringExtra = fragmentActivity.getIntent().getStringExtra("START_PROCESS_WAYS");
        try {
            taskMonitor = TaskMonitorUtils.generateTaskMonitor("XRIVER_ACTIVITY_ONCREATE", "startActivity");
            try {
                taskMonitor.beginTask();
            } catch (Throwable th2) {
                th = th2;
                RVLogger.e(TAG, "ignore, just print", th);
                super.onCreate(bundle);
                PerformanceDog.doSample(true, false);
                String stringExtra2 = fragmentActivity.getIntent().getStringExtra("appId");
                TaskMonitor taskMonitor3 = taskMonitor;
                longExtra = fragmentActivity.getIntent().getLongExtra("nodeId", -1L);
                int intExtra = fragmentActivity.getIntent().getIntExtra("startAction", -1);
                byte[] byteArrayExtra = fragmentActivity.getIntent().getByteArrayExtra("appInfo");
                fragmentActivity.mStartParams = fragmentActivity.getIntent().getBundleExtra("startParams");
                LoggerFactory.getTraceLogger().info(TAG, "onCreate-monitorId:" + fragmentActivity.monitorId);
                extractScene = AppInfoScene.extractScene(fragmentActivity.mStartParams);
                fragmentActivity.mSceneParams = fragmentActivity.getIntent().getBundleExtra("sceneParams");
                bundleExtra = fragmentActivity.getIntent().getBundleExtra("xriverParams");
                LoggerFactory.getTraceLogger().info(TAG, "ProcessUtils.isMainProcess() = " + ProcessUtils.isMainProcess() + ", ProcessUtils.isTinyProcess() = " + ProcessUtils.isTinyProcess() + ", savedInstanceState = " + bundle2);
                String str3 = "";
                if (XRiverApplicationDelegate.sHasStarted) {
                }
                r24 = extractScene;
                if (!ProcessUtils.isMainProcess()) {
                }
                if (!ProcessUtils.isTinyProcess()) {
                }
                if (longExtra != -1) {
                }
                bundle3 = fragmentActivity.mSceneParams;
                if (bundle3 != null) {
                }
                try {
                    RVInitializer.init(fragmentActivity);
                    if (CRVUtils.loadSo(fragmentActivity)) {
                    }
                } catch (Throwable th3) {
                    RVLogger.e(TAG, "onCreate RVInitializer init failed Exception", th3);
                    d(bundleExtra);
                    fragmentActivity.finish();
                    return;
                }
            }
        } catch (Throwable th4) {
            th = th4;
            taskMonitor = null;
        }
        super.onCreate(bundle);
        PerformanceDog.doSample(true, false);
        String stringExtra22 = fragmentActivity.getIntent().getStringExtra("appId");
        TaskMonitor taskMonitor32 = taskMonitor;
        longExtra = fragmentActivity.getIntent().getLongExtra("nodeId", -1L);
        int intExtra2 = fragmentActivity.getIntent().getIntExtra("startAction", -1);
        byte[] byteArrayExtra2 = fragmentActivity.getIntent().getByteArrayExtra("appInfo");
        fragmentActivity.mStartParams = fragmentActivity.getIntent().getBundleExtra("startParams");
        LoggerFactory.getTraceLogger().info(TAG, "onCreate-monitorId:" + fragmentActivity.monitorId);
        extractScene = AppInfoScene.extractScene(fragmentActivity.mStartParams);
        fragmentActivity.mSceneParams = fragmentActivity.getIntent().getBundleExtra("sceneParams");
        bundleExtra = fragmentActivity.getIntent().getBundleExtra("xriverParams");
        LoggerFactory.getTraceLogger().info(TAG, "ProcessUtils.isMainProcess() = " + ProcessUtils.isMainProcess() + ", ProcessUtils.isTinyProcess() = " + ProcessUtils.isTinyProcess() + ", savedInstanceState = " + bundle2);
        String str32 = "";
        if (XRiverApplicationDelegate.sHasStarted && ProcessUtils.isMainProcess()) {
            r24 = extractScene;
            if (!"startPage".equals(BundleUtils.getString(fragmentActivity.mStartParams, "startScene"))) {
                LoggerFactory.getTraceLogger().warn(TAG, "launch with sHasStarted == false!!");
                H5ApplicationDelegate.openAlipayHomePageCompact("YES".equalsIgnoreCase(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfig("ta_fix_activity_restore", "")));
                String string2 = BundleUtils.getString(bundle2, RESTORE_APPID);
                if (fragmentActivity.getIntent() != null) {
                    str32 = BundleUtils.getString(fragmentActivity.getIntent().getBundleExtra("startParams"), "ap_framework_scheme");
                }
                AntEvent.Builder builder = new AntEvent.Builder();
                builder.setEventID("101340");
                builder.setBizType("base-framework");
                builder.setLoggerLevel(2);
                builder.addExtParam("appId", string2);
                builder.addExtParam("hit_type", "abnormal");
                builder.addExtParam("referer_url", str32);
                builder.addExtParam("top", fragmentActivity.getClass().getName());
                builder.addExtParam("duration", "0");
                builder.build().send();
                if (fragmentActivity.isTaskRoot()) {
                    fragmentActivity.finishAndRemoveTask();
                }
                LoggerFactory.getTraceLogger().warn(TAG, "onCreate but from restore, just finish!");
                fragmentActivity.mFromRestore = true;
                fragmentActivity.finish();
            }
        } else {
            r24 = extractScene;
        }
        if (!ProcessUtils.isMainProcess() && handleRestoreInMainProc(bundle)) {
            fragmentActivity.postHandleRestoreInMainProc();
            LoggerFactory.getTraceLogger().error(TAG, "handleRestoreInMainProc!!! Just Finish!!!");
        } else if (!ProcessUtils.isTinyProcess() && bundle2 != null) {
            RVLogger.d(TAG, "XRiverActivity.onCreate " + fragmentActivity.getIntent());
            LoggerFactory.getTraceLogger().error(TAG, "handleRestoreInLiteProc!!! Just Finish!!!");
        } else {
            if (longExtra != -1) {
                fragmentActivity.mNodeId = longExtra;
            }
            bundle3 = fragmentActivity.mSceneParams;
            if (bundle3 != null) {
                bundle3.get("a");
            }
            RVInitializer.init(fragmentActivity);
            if (CRVUtils.loadSo(fragmentActivity)) {
                RVLogger.d("CRVUtils loadSo in XRiverActivity failed");
                d(bundleExtra);
                fragmentActivity.finish();
                return;
            }
            CRVUtils.init(fragmentActivity);
            if (taskMonitor32 != null) {
                taskMonitor2 = taskMonitor32;
                taskMonitor2.setUniqueId(BundleUtils.getString(fragmentActivity.mStartParams, TaskManager.INSTANCE.getTASK_RECORD_ID_KEY(), ""));
                taskMonitor2.bindNodeId(longExtra);
            } else {
                taskMonitor2 = taskMonitor32;
            }
            MicroPhoenix create = new MicroPhoenixBuilder().create(fragmentActivity.getIntent());
            fragmentActivity.mPhoenix = create;
            if (taskMonitor2 != null) {
                str = taskMonitor2.getUniqueId();
            } else {
                str = "-1";
            }
            create.start(fragmentActivity, stringExtra22, str);
            TaskMonitor taskMonitor4 = taskMonitor2;
            fragmentActivity.processTaskRecode(longExtra, bundleExtra, longExtra2, stringExtra);
            CRVApp createAppAndStart = XRiverActivityHelper.createAppAndStart(stringExtra22, longExtra, intExtra2, byteArrayExtra2, fragmentActivity.mStartParams, fragmentActivity.mSceneParams, bundleExtra);
            fragmentActivity.mCRVApp = createAppAndStart;
            fragmentActivity.mPhoenix.bindSession(createAppAndStart);
            LifeCycleBlockOptimizeEventTracker.addLifeCycleBlockOptimizeEvent(fragmentActivity.mCRVApp);
            fragmentActivity.mIsHalfScreenApp = HalfscreenUtils.isHalfScreenApp(fragmentActivity.mCRVApp);
            LoggerFactory.getTraceLogger().debug(TAG, "onCreate instant create CRVApp: " + fragmentActivity.mCRVApp);
            Intrinsics.checkNotNullParameter(fragmentActivity, "activity");
            ?? obj2 = new Object();
            ((yn) obj2).a = stringExtra22;
            ((yn) obj2).b = new WeakReference(fragmentActivity);
            CRVApp cRVApp2 = fragmentActivity.mCRVApp;
            d1 d1Var = new d1(7, fragmentActivity);
            Intrinsics.checkNotNullParameter(d1Var, "callback");
            if (StringsKt__StringsJVMKt.equals("YES", ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("ta_fix_invalid_session", "YES"), true)) {
                e3 e3Var = new e3((Object) obj2, d1Var, 1);
                g2 g2Var = new g2(longExtra, e3Var);
                if (cRVApp2 == null) {
                    RVLogger.d("AppRestore", "CRVApp is null, restore....");
                    z = g2Var.a(true);
                } else {
                    Activity activity = (Activity) ((WeakReference) ((yn) obj2).b).get();
                    if (activity != null && (intent = activity.getIntent()) != null && (intent.getFlags() & 1048576) == 1048576) {
                        boolean isTinyProcess = ProcessUtils.isTinyProcess();
                        RVLogger.w("AppRestore", "start from history, check session status, async=" + isTinyProcess);
                        if (isTinyProcess) {
                            ExecutorType executorType = ExecutorType.URGENT;
                            f2 f2Var = new f2(6, g2Var);
                            DexAOPEntry.java_lang_Runnable_newInstance_Created(f2Var);
                            ExecutorUtils.runNotOnMain(executorType, f2Var, true);
                            Handler asyncHandler = XriverH5Utils.getAsyncHandler();
                            z8 z8Var = new z8(g2Var, longExtra, e3Var, 1);
                            DexAOPEntry.java_lang_Runnable_newInstance_Created(z8Var);
                            DexAOPEntry.lite_hanlerPostDelayedProxy(asyncHandler, z8Var, 1500L);
                        } else {
                            z = g2Var.a(false);
                        }
                    }
                }
                if (!z) {
                    return;
                }
                if (fragmentActivity.mCRVApp == null) {
                    LoggerFactory.getTraceLogger().error(TAG, "onCreate cannot find mCRVApp for nodeId: " + fragmentActivity.mNodeId);
                    d(bundleExtra);
                    fragmentActivity.finish();
                    return;
                }
                d(bundleExtra);
                if (byteArrayExtra2 != null && ((fragmentActivity.isHalfScreenApp() || ProcessUtils.isTinyProcess()) && (transferNativeByteArrayToJava = CRVAppModelUtils.transferNativeByteArrayToJava(byteArrayExtra2, r24.name())) != null)) {
                    fragmentActivity.mCRVApp.setData(AppInfoModel.class, transferNativeByteArrayToJava.getAppInfoModel());
                    fragmentActivity.mIsTinyApp = AppInfoUtil.getAppType(transferNativeByteArrayToJava).isTiny();
                }
                ((EventTracker) RVProxy.get(EventTracker.class)).stub(fragmentActivity.mCRVApp, "nbx_attachContext", fragmentActivity.mAttachBaseContextTime);
                ((EventTracker) RVProxy.get(EventTracker.class)).stub(fragmentActivity.mCRVApp, "xriver_nbx_attachContext", fragmentActivity.mAttachBaseContextSystemTime);
                fragmentActivity.mCloseAllAnim = BundleUtils.getBoolean(fragmentActivity.mCRVApp.getStartParams(), "closeAllActivityAnimation", false);
                try {
                    if (Build.VERSION.SDK_INT >= 33) {
                        fragmentActivity.mActivityAnimBean = (ActivityAnimBean) q9.c(fragmentActivity.getIntent());
                    } else {
                        fragmentActivity.mActivityAnimBean = fragmentActivity.getIntent().getParcelableExtra("ariverActivityAnimBean");
                    }
                } catch (Throwable th5) {
                    RVLogger.e(TAG, "get mActivityAnimBean ", th5);
                    H5LogUtil.logNebulaTech(H5LogData.seedId("ANIM_BEAN_CLASS_NOT_FOUND_EX"));
                }
                RVLogger.d(TAG, "onCreate with animBean: " + fragmentActivity.mActivityAnimBean);
                ((BaseFragmentActivity) fragmentActivity).mApp.setAppId(fragmentActivity.mCRVApp.getAppId());
                ((BaseFragmentActivity) fragmentActivity).mApp.setSceneParams(fragmentActivity.mCRVApp.getSceneParams());
                if (!fragmentActivity.mCloseAllAnim) {
                    LinkedHashMap linkedHashMap = XRiverLoadingActivity.h;
                    if (!BundleUtils.getBoolean(bundleExtra, "__has_loading_ahead__", false)) {
                        ActivityAnimBean activityAnimBean = fragmentActivity.mActivityAnimBean;
                        if (activityAnimBean != null) {
                            fragmentActivity.overridePendingTransitionOpt(activityAnimBean.enter, activityAnimBean.exit);
                        }
                        z2 = false;
                        fragmentActivity.setContentView(fragmentActivity.getContentViewId(fragmentActivity.mCRVApp.getAppId(), fragmentActivity.mCRVApp.getStartParams()));
                        TaskManager taskManager = TaskManager.INSTANCE;
                        Integer num = null;
                        TaskModel obtainCurrentTaskModel = taskManager.obtainCurrentTaskModel("XRIVER_ACTIVITY_SET_VIEW", fragmentActivity.mCRVApp, (INebulaPage) null, z2);
                        obtainCurrentTaskModel.getAttrs().put("name", fragmentActivity.toString());
                        taskManager.obtainTaskTracker().postTask(obtainCurrentTaskModel, true);
                        cRVApp = fragmentActivity.mCRVApp;
                        if (cRVApp != null) {
                            cRVApp.addListener(new rn(fragmentActivity));
                        }
                        RVTraceUtils.asyncTraceEnd(RVTraceKey.RV_appPhase_uICreate);
                        RVTraceUtils.asyncTraceBegin(RVTraceKey.RV_appPhase_waitLoadApp);
                        ((EventTracker) RVProxy.get(EventTracker.class)).addAttr(fragmentActivity.mCRVApp, "ucPreloadStatus", String.valueOf(H5Flag.ucPreloadStatusLast));
                        ((EventTracker) RVProxy.get(EventTracker.class)).addAttr(fragmentActivity.mCRVApp, "xriverPreloadScene", PreloadUtils.getPreloadStatus());
                        EventTracker eventTracker = (EventTracker) RVProxy.get(EventTracker.class);
                        CRVApp cRVApp3 = fragmentActivity.mCRVApp;
                        bundle4 = fragmentActivity.mStartParams;
                        if (bundle4 != null) {
                            str2 = "";
                        } else {
                            str2 = bundle4.getString("isUpgradeMainThread");
                        }
                        eventTracker.addAttr(cRVApp3, "isUpgradeMainThread", str2);
                        fragmentActivity.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new on(fragmentActivity));
                        NavigationBarUtils.setupSystemNavigationBar(fragmentActivity.getWindow(), fragmentActivity);
                        XRiverAppContext xRiverAppContext = new XRiverAppContext(fragmentActivity.mCRVApp, fragmentActivity, R.id.fragment_container, R.id.tab_container);
                        fragmentActivity.mCRVApp.bindContext(xRiverAppContext);
                        fragmentActivity.mXRiverActivityHelper = new XRiverActivityHelper(fragmentActivity.mCRVApp, xRiverAppContext.getFragmentManager(), fragmentActivity);
                        if (BundleUtils.getBoolean(fragmentActivity.mCRVApp.getStartParams(), "fullscreen", false)) {
                            fragmentActivity.mCRVApp.getStartParams().putBoolean("transparent", true);
                            RVLogger.d(TAG, "fullScreen true,put transparent ");
                        }
                        ActivityNavigationBarHelper activityNavigationBarHelper = new ActivityNavigationBarHelper();
                        fragmentActivity.mActivityNavigationBarHelper = activityNavigationBarHelper;
                        activityNavigationBarHelper.createSharedNavigationBarOperator(fragmentActivity, fragmentActivity.mCRVApp);
                        bundle5 = fragmentActivity.mStartParams;
                        if (BundleUtils.getBoolean(bundle5, "fullscreen", false)) {
                            fragmentActivity.getWindow().setFlags(1024, 1024);
                        }
                        if ("YES".equalsIgnoreCase(BundleUtils.getString(bundle5, "paladinMode"))) {
                            fragmentActivity.getWindow().addFlags(1024);
                            fragmentActivity.getWindow().getDecorView().setSystemUiVisibility(4102);
                        }
                        com.alipay.mobile.nebulax.integration.api.Util.adjustEdgeToEdge(fragmentActivity.getWindow().getDecorView(), R.id.nebulax_root_view);
                        if (fragmentActivity.mNodeId <= 0 && fragmentActivity.mStartParams != null) {
                            ((EventTracker) RVProxy.get(EventTracker.class)).stub(fragmentActivity.mCRVApp, "nbx_activityCreated");
                            LoggerFactory.getTraceLogger().info(TAG, "XRiverActivity.onCreate " + fragmentActivity + " with app: " + fragmentActivity.mCRVApp + " nodeId: " + fragmentActivity.mNodeId + " startParams: " + fragmentActivity.mStartParams + " sceneParams: " + fragmentActivity.mSceneParams);
                            fragmentActivity.mIsResizeable = BundleUtils.getBoolean(fragmentActivity.mStartParams, "resizeable", false);
                            aa.a(fragmentActivity, fragmentActivity.mStartParams, fragmentActivity.mCRVApp, CRVNativeBridge.isInMagicMode());
                            fragmentActivity.mXRiverActivityHelper.onCreate();
                            String string3 = BundleUtils.getString(fragmentActivity.mStartParams, "landscape");
                            if (string3.equals("landscape")) {
                                RVLogger.d(TAG, "handleStartParams(): mStartClientBundle.startParams[\"landscape\"] = landscape");
                                android.view.Window window = fragmentActivity.getWindow();
                                RVLogger.d(TAG, "setCutoutModeShortEdges(): window = " + window);
                                if (window != null) {
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        WindowManager.LayoutParams attributes = window.getAttributes();
                                        if (attributes != null) {
                                            j.a(attributes);
                                            window.setAttributes(attributes);
                                            RVLogger.d(TAG, "setCutoutModeShortEdges(): window layout params cutout mode set to shortEdges");
                                        } else {
                                            RVLogger.e(TAG, "setCutoutModeShortEdges(): window layout params is null, cannot set");
                                        }
                                    } else {
                                        RVLogger.d(TAG, "setCutoutModeShortEdges(): API level lower than 28, cannot set");
                                    }
                                }
                                if (fragmentActivity.getRequestedOrientation() != 0) {
                                    fragmentActivity.setRequestedOrientation(0);
                                }
                            } else if (string3.equals("auto") && fragmentActivity.getRequestedOrientation() != -1) {
                                fragmentActivity.setRequestedOrientation(-1);
                            }
                            CRVApp cRVApp4 = fragmentActivity.mCRVApp;
                            if (cRVApp4 == null) {
                                LoggerFactory.getTraceLogger().error(TAG, " app is null, not set activity background");
                            } else {
                                Bundle startParams = cRVApp4.getStartParams();
                                if (!BundleUtils.contains(startParams, "backgroundColor")) {
                                    LoggerFactory.getTraceLogger().error(TAG, " startParams not exist backgroundColor, not set activity background");
                                } else if (BundleUtils.getBoolean(startParams, "transparent", false)) {
                                    LoggerFactory.getTraceLogger().error(TAG, " startParams has transparent true , not set activity background");
                                } else if (BundleUtils.contains(startParams, "backgroundImageUrl")) {
                                    LoggerFactory.getTraceLogger().error(TAG, " startParams contains backgroundImageUrl, not set activity background");
                                } else if (!ConfigUtils.valueInConfigJsonArray("h5_set_activity_background", cRVApp4.getAppId(), false)) {
                                    LoggerFactory.getTraceLogger().error(TAG, " disable set activity background color by config, not set activity background");
                                } else {
                                    int i = BundleUtils.getInt(startParams, "backgroundColor", -16777216);
                                    if (i != -16777216) {
                                        int i2 = (-16777216) | i;
                                        fragmentActivity.findViewById(R.id.nebulax_root_view).setBackgroundColor(i2);
                                        LoggerFactory.getTraceLogger().error(TAG, " set activity background,targetColor=" + i2);
                                    }
                                    if (H5StatusBarUtils.isSupport()) {
                                        try {
                                            H5StatusBarUtils.setTransparentColor(fragmentActivity, 0x4f000000);
                                        } catch (Exception e) {
                                            RVLogger.e(TAG, e);
                                        }
                                    }
                                }
                            }
                            if (!MultiAppUtils.INSTANCE.isAlipayApp()) {
                                try {
                                    z3 = false;
                                    try {
                                        bk.a(fragmentActivity, fragmentActivity.getCurrentUri(), fragmentActivity.mStartParams, BundleUtils.getBoolean(fragmentActivity.mCRVApp.getStartParams(), "transparent", false), false);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        LoggerFactory.getTraceLogger().error(TAG, th);
                                        if (taskMonitor4 != null) {
                                        }
                                        string = BundleUtils.getString(fragmentActivity.mStartParams, "nebulaAuthCodeKey", "");
                                        if (!TextUtils.isEmpty(string)) {
                                            RVLogger.d(TAG, "cancelBizTimeoutCheck key = " + string);
                                            h5BizProvider.cancelBizTimeoutCheck(string);
                                        }
                                        DexAOPEntry.android_support_v4_content_LocalBroadcastManager_registerReceiver_proxy(LocalBroadcastManager.getInstance(fragmentActivity), fragmentActivity.mBroadcastReceiver, new IntentFilter("halfscreen_Close"));
                                        bundle6 = fragmentActivity.mStartParams;
                                        if (!"20002117".equals(BundleUtils.getString(bundle6, "lastReferAppId"))) {
                                        }
                                        z4 = z3;
                                        boolean equalsIgnoreCase = "com.antgroup.zhixiaobao.android".equalsIgnoreCase(XriverH5Utils.getContext().getPackageName());
                                        if (!z4) {
                                        }
                                        z5 = z3;
                                        if (z5) {
                                        }
                                        n4 n4Var = fragmentActivity.mExitTagged;
                                        n4Var.getClass();
                                        Intrinsics.checkNotNullParameter(fragmentActivity, "activity");
                                        n4Var.b = new WeakReference(fragmentActivity);
                                        Result.Companion companion = Result.Companion;
                                        supportFragmentManager = fragmentActivity.getSupportFragmentManager();
                                        if (supportFragmentManager == null) {
                                        }
                                        if (fragmentTransaction != null) {
                                        }
                                        if (fragmentTransaction != null) {
                                        }
                                        obj = Result.constructor-impl(num);
                                        th = Result.exceptionOrNull-impl(obj);
                                        if (th != null) {
                                        }
                                        ExecutorType executorType2 = ExecutorType.NORMAL;
                                        k4 k4Var = new k4(n4Var, fragmentActivity, 2);
                                        DexAOPEntry.java_lang_Runnable_newInstance_Created(k4Var);
                                        ExecutorUtils.runNotOnMain(executorType2, k4Var);
                                        AliveHandler.Companion companion2 = AliveHandler.Companion;
                                        Context applicationContext = fragmentActivity.getApplicationContext();
                                        Intrinsics.checkNotNullExpressionValue(applicationContext, "getApplicationContext(...)");
                                        companion2.register(applicationContext);
                                        n4Var.g = System.currentTimeMillis();
                                        ?? obj3 = new Object();
                                        Context applicationContext2 = fragmentActivity.getApplicationContext();
                                        Intrinsics.checkNotNullExpressionValue(applicationContext2, "getApplicationContext(...)");
                                        obj3.b(applicationContext2, true);
                                        return;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    z3 = false;
                                }
                            } else {
                                z3 = false;
                            }
                            if (taskMonitor4 != null) {
                                taskMonitor4.endTask();
                            }
                            string = BundleUtils.getString(fragmentActivity.mStartParams, "nebulaAuthCodeKey", "");
                            if (!TextUtils.isEmpty(string) && (h5BizProvider = (H5BizProvider) XriverH5Utils.getProvider(H5BizProvider.class.getName())) != null) {
                                RVLogger.d(TAG, "cancelBizTimeoutCheck key = " + string);
                                h5BizProvider.cancelBizTimeoutCheck(string);
                            }
                            DexAOPEntry.android_support_v4_content_LocalBroadcastManager_registerReceiver_proxy(LocalBroadcastManager.getInstance(fragmentActivity), fragmentActivity.mBroadcastReceiver, new IntentFilter("halfscreen_Close"));
                            bundle6 = fragmentActivity.mStartParams;
                            if (!"20002117".equals(BundleUtils.getString(bundle6, "lastReferAppId")) && !"20000067".equals(BundleUtils.getString(bundle6, "appId"))) {
                                z4 = true;
                            } else {
                                z4 = z3;
                            }
                            boolean equalsIgnoreCase2 = "com.antgroup.zhixiaobao.android".equalsIgnoreCase(XriverH5Utils.getContext().getPackageName());
                            if (!z4 && equalsIgnoreCase2) {
                                z5 = true;
                            } else {
                                z5 = z3;
                            }
                            if (z5) {
                                RVLogger.d(TAG, "isFromZXBHome");
                                View findViewById = fragmentActivity.findViewById(R.id.nebulax_root_view);
                                findViewById.addOnLayoutChangeListener(new pn(fragmentActivity, findViewById));
                            }
                            n4 n4Var2 = fragmentActivity.mExitTagged;
                            n4Var2.getClass();
                            Intrinsics.checkNotNullParameter(fragmentActivity, "activity");
                            n4Var2.b = new WeakReference(fragmentActivity);
                            try {
                                Result.Companion companion3 = Result.Companion;
                                supportFragmentManager = fragmentActivity.getSupportFragmentManager();
                                if (supportFragmentManager == null) {
                                    fragmentTransaction = supportFragmentManager.beginTransaction();
                                } else {
                                    fragmentTransaction = null;
                                }
                                if (fragmentTransaction != null) {
                                    fragmentTransaction.add(n4Var2, "exit-fragment");
                                }
                                if (fragmentTransaction != null) {
                                    num = Integer.valueOf(fragmentTransaction.commitAllowingStateLoss());
                                }
                                obj = Result.constructor-impl(num);
                            } catch (Throwable th8) {
                                Result.Companion companion4 = Result.Companion;
                                obj = Result.constructor-impl(ResultKt.createFailure(th8));
                            }
                            th = Result.exceptionOrNull-impl(obj);
                            if (th != null) {
                                RVLogger.w(n4Var2.a, "failed to attach activity: " + th);
                            }
                            ExecutorType executorType22 = ExecutorType.NORMAL;
                            k4 k4Var2 = new k4(n4Var2, fragmentActivity, 2);
                            DexAOPEntry.java_lang_Runnable_newInstance_Created(k4Var2);
                            ExecutorUtils.runNotOnMain(executorType22, k4Var2);
                            AliveHandler.Companion companion22 = AliveHandler.Companion;
                            Context applicationContext3 = fragmentActivity.getApplicationContext();
                            Intrinsics.checkNotNullExpressionValue(applicationContext3, "getApplicationContext(...)");
                            companion22.register(applicationContext3);
                            n4Var2.g = System.currentTimeMillis();
                            ?? obj32 = new Object();
                            Context applicationContext22 = fragmentActivity.getApplicationContext();
                            Intrinsics.checkNotNullExpressionValue(applicationContext22, "getApplicationContext(...)");
                            obj32.b(applicationContext22, true);
                            return;
                        }
                        LoggerFactory.getTraceLogger().info(TAG, "XRiverActivity.onCreate illegal start params!");
                        fragmentActivity.finish();
                        return;
                    }
                }
                z2 = false;
                fragmentActivity.overridePendingTransitionOpt(0, 0);
                fragmentActivity.setContentView(fragmentActivity.getContentViewId(fragmentActivity.mCRVApp.getAppId(), fragmentActivity.mCRVApp.getStartParams()));
                TaskManager taskManager2 = TaskManager.INSTANCE;
                Integer num2 = null;
                TaskModel obtainCurrentTaskModel2 = taskManager2.obtainCurrentTaskModel("XRIVER_ACTIVITY_SET_VIEW", fragmentActivity.mCRVApp, (INebulaPage) null, z2);
                obtainCurrentTaskModel2.getAttrs().put("name", fragmentActivity.toString());
                taskManager2.obtainTaskTracker().postTask(obtainCurrentTaskModel2, true);
                cRVApp = fragmentActivity.mCRVApp;
                if (cRVApp != null) {
                }
                RVTraceUtils.asyncTraceEnd(RVTraceKey.RV_appPhase_uICreate);
                RVTraceUtils.asyncTraceBegin(RVTraceKey.RV_appPhase_waitLoadApp);
                ((EventTracker) RVProxy.get(EventTracker.class)).addAttr(fragmentActivity.mCRVApp, "ucPreloadStatus", String.valueOf(H5Flag.ucPreloadStatusLast));
                ((EventTracker) RVProxy.get(EventTracker.class)).addAttr(fragmentActivity.mCRVApp, "xriverPreloadScene", PreloadUtils.getPreloadStatus());
                EventTracker eventTracker2 = (EventTracker) RVProxy.get(EventTracker.class);
                CRVApp cRVApp32 = fragmentActivity.mCRVApp;
                bundle4 = fragmentActivity.mStartParams;
                if (bundle4 != null) {
                }
                eventTracker2.addAttr(cRVApp32, "isUpgradeMainThread", str2);
                fragmentActivity.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new on(fragmentActivity));
                NavigationBarUtils.setupSystemNavigationBar(fragmentActivity.getWindow(), fragmentActivity);
                XRiverAppContext xRiverAppContext2 = new XRiverAppContext(fragmentActivity.mCRVApp, fragmentActivity, R.id.fragment_container, R.id.tab_container);
                fragmentActivity.mCRVApp.bindContext(xRiverAppContext2);
                fragmentActivity.mXRiverActivityHelper = new XRiverActivityHelper(fragmentActivity.mCRVApp, xRiverAppContext2.getFragmentManager(), fragmentActivity);
                if (BundleUtils.getBoolean(fragmentActivity.mCRVApp.getStartParams(), "fullscreen", false)) {
                }
                ActivityNavigationBarHelper activityNavigationBarHelper2 = new ActivityNavigationBarHelper();
                fragmentActivity.mActivityNavigationBarHelper = activityNavigationBarHelper2;
                activityNavigationBarHelper2.createSharedNavigationBarOperator(fragmentActivity, fragmentActivity.mCRVApp);
                bundle5 = fragmentActivity.mStartParams;
                if (BundleUtils.getBoolean(bundle5, "fullscreen", false)) {
                }
                if ("YES".equalsIgnoreCase(BundleUtils.getString(bundle5, "paladinMode"))) {
                }
                com.alipay.mobile.nebulax.integration.api.Util.adjustEdgeToEdge(fragmentActivity.getWindow().getDecorView(), R.id.nebulax_root_view);
                if (fragmentActivity.mNodeId <= 0) {
                }
                LoggerFactory.getTraceLogger().info(TAG, "XRiverActivity.onCreate illegal start params!");
                fragmentActivity.finish();
                return;
            }
            z = false;
            if (!z) {
            }
        }
        LoggerFactory.getTraceLogger().warn(TAG, "onCreate but from restore, just finish!");
        fragmentActivity.mFromRestore = true;
        fragmentActivity.finish();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onDestroy_stub_private() {
        SharedNavigationBarOperator sharedNavigationBarOperator;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "11", Void.TYPE).isSupported) {
            return;
        }
        TinyLifecycle.activityOnDestroy();
        RVLogger.d(TAG, "XRiverActivity.onDestroy " + this);
        ExitEvent.of(this).tagging("Activity.onDestroy");
        ActivityNavigationBarHelper activityNavigationBarHelper = this.mActivityNavigationBarHelper;
        if (activityNavigationBarHelper != null && (sharedNavigationBarOperator = activityNavigationBarHelper.sharedNavigationBarOperator) != null) {
            sharedNavigationBarOperator.onDestroy();
            if ("yes".equalsIgnoreCase(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("leakfix_XRiver_Integration", "no"))) {
                RVLogger.d(TAG, "XRiverActivity.onDestroy set mActivityNavigationBarHelper = null");
                this.mActivityNavigationBarHelper = null;
            }
        }
        super.onDestroy();
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onDestroy();
            if (((BaseFragmentActivity) this).mApp != null && !"startPage".equals(BundleUtils.getString(this.mStartParams, "startScene"))) {
                Bundle bundle = new Bundle();
                bundle.putString("DESTROY_DESCRIPTION", "xriver_onDestroy");
                ((BaseFragmentActivity) this).mApp.destroy(bundle);
            }
        }
        if (this.mBroadcastReceiver != null) {
            DexAOPEntry.android_support_v4_content_LocalBroadcastManager_unregisterReceiver_proxy(LocalBroadcastManager.getInstance(this), this.mBroadcastReceiver);
            this.mBroadcastReceiver = null;
        }
        if (!MultiAppUtils.INSTANCE.isAlipayApp()) {
            Iterator it = bk.a.iterator();
            while (it.hasNext()) {
                WeakReference weakReference = (WeakReference) it.next();
                if (weakReference != null && weakReference.get() != null && ((Activity) weakReference.get()).equals(this)) {
                    RVLogger.d("XRIVER:Android:XRiverActivity:SoftInputCompat", "disposeActivity");
                    it.remove();
                }
            }
        }
        PerfUtils.initParams(false);
        aa.h = null;
        aa.c = 0.0f;
        aa.d = null;
        aa.i = 0;
        aa.j = 0;
        RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "reSetScreenState");
        PerfUtils.endTime = SystemClock.elapsedRealtime();
    }

    private boolean __onKeyDown_stub_private(int i, android.view.KeyEvent keyEvent) {
        FragmentManager supportFragmentManager;
        App app;
        SlidingUpPanelLayout b;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{Integer.valueOf(i), keyEvent, Integer.TYPE, android.view.KeyEvent.class, Boolean.TYPE}, this, changeQuickRedirect, "13");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (keyEvent.getKeyCode() == 4 && keyEvent.getRepeatCount() == 0 && (supportFragmentManager = getSupportFragmentManager()) != null) {
            d4 findFragmentByTag = supportFragmentManager.findFragmentByTag("embed_fragment_container");
            if (findFragmentByTag instanceof d4) {
                d4 d4Var = findFragmentByTag;
                if (keyEvent.getKeyCode() == 4 && keyEvent.getRepeatCount() == 0 && (app = ((u3) d4Var).a) != null && app.isFirstPage() && (b = x8.b(d4Var.getActivity(), (ViewGroup) d4Var.getView(), R.id.nebulax_root_view)) != null) {
                    b.setPanelState(SlidingUpPanelLayout.PanelState.b);
                    return true;
                }
                y3 y3Var = d4Var.c;
                if (y3Var != null) {
                    return y3Var.onKeyDown(i, keyEvent);
                }
                return false;
            }
        }
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null && xRiverActivityHelper.onKeyDown(i, keyEvent)) {
            return true;
        }
        return super/*android.support.v4.app.FragmentActivity*/.onKeyDown(i, keyEvent);
    }

    private void __onNewIntent_stub_private(Intent intent) {
        XRiverActivity xRiverActivity;
        Intent intent2;
        ActivityAnimBean activityAnimBean;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            intent2 = intent;
            if (PatchProxy.proxy(intent2, xRiverActivity, changeQuickRedirect, "15", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
            intent2 = intent;
        }
        super.onNewIntent(intent2);
        xRiverActivity.newIntent = intent2;
        if (intent2.getBooleanExtra("needStartAnim", true) && (activityAnimBean = xRiverActivity.mActivityAnimBean) != null && activityAnimBean.needRestartAnim) {
            overridePendingTransitionOpt(activityAnimBean.enterFast, activityAnimBean.exitFast);
        }
        XRiverActivityHelper xRiverActivityHelper = xRiverActivity.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onNewIntent(intent2);
        }
        LiteNebulaXCompat.onAppRestart(intent2);
    }

    private void __onPause_stub_private() {
        String str;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "17", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "XRiverActivity.onPause begin" + this);
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            str = cRVApp.getAppId();
        } else {
            str = "null";
        }
        PerfUtils.reset(str, true);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        super.onPause();
        long elapsedRealtime2 = SystemClock.elapsedRealtime();
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onPause();
        }
        long elapsedRealtime3 = SystemClock.elapsedRealtime();
        if (PerfUtils.isReport) {
            PerfUtils.recordPhaseCostTime("onPause", (elapsedRealtime3 - elapsedRealtime) + ":" + (elapsedRealtime2 - elapsedRealtime) + ":" + (elapsedRealtime3 - elapsedRealtime2));
        }
        RVLogger.d(TAG, "XRiverActivity.onPause end" + this);
    }

    private void __onRequestPermissionsResult_stub_private(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "19").isSupported) {
            return;
        }
        super.onRequestPermissionsResult(i, strArr, iArr);
        RVLogger.d(TAG, "onRequestPermissionsResult requestCode:" + i + " permissions:" + Arrays.toString(strArr) + " grantResult: " + Arrays.toString(iArr));
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onRequestPermissionResult(i, strArr, iArr);
        } else {
            RVLogger.w(TAG, "XriverActivity.onRequestPermissionResult but XriverActivity null!!!");
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onResume_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "21", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "XRiverActivity.onResume " + this);
        super.onResume();
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onResume();
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            v.e(this, cRVApp.getAppId(), false);
        }
        CRVApp cRVApp2 = this.mCRVApp;
        if (cRVApp2 != null && cRVApp2.isClientDarkMode() && this.mCRVApp.isTinyAppDarkMode() && this.mCRVApp.getDarkModeHelper() != null) {
            NavigationBarUtils.setupSystemNavigationBar(getWindow(), this, (int) this.mCRVApp.getDarkModeHelper().getTabBarColor(), this.mCRVApp);
        } else {
            NavigationBarUtils.setupSystemNavigationBar(getWindow(), this);
        }
        CRVApp cRVApp3 = this.mCRVApp;
        if (cRVApp3 != null && cRVApp3.isClientDarkMode() && getSupportFragmentManager() != null && getSupportFragmentManager().findFragmentByTag("SplashViewImpl") != null) {
            NavigationBarUtils.setupSystemNavigationBar(getWindow(), this, 0, this.mCRVApp);
        }
    }

    private void __onSaveInstanceState_stub_private(Bundle bundle) {
        XRiverActivity xRiverActivity;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, xRiverActivity, changeQuickRedirect, "23", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
            bundle2 = bundle;
        }
        RVLogger.d(TAG, "XRiverActivity.onSaveInstanceState " + this);
        XRiverActivityHelper xRiverActivityHelper = xRiverActivity.mXRiverActivityHelper;
        if (xRiverActivityHelper != null && xRiverActivityHelper.getApp() != null) {
            CRVApp app = xRiverActivity.mXRiverActivityHelper.getApp();
            bundle2.putString(RESTORE_APPID, app.getAppId());
            Bundle bundle3 = (Bundle) BundleUtils.getParcelable(app.getSceneParams(), "nxOriginStartupParams");
            if (bundle3 != null) {
                bundle2.putParcelable(RESTORE_PARAMS, bundle3);
            }
            if (InsideUtils.isInside()) {
                bundle2.putBoolean("isInside", true);
            }
            if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("tiny_forbidTradePayRestore", true) && !TextUtils.isEmpty(app.getStringValue("reTradePayTime"))) {
                bundle2.putBoolean(RESTORE_TRADE_PAY, true);
                RVLogger.d(TAG, "XRiverActivity.onSaveInstanceState record trade pay " + this);
            }
        }
        RVLogger.d(TAG, "XRiverActivity.onSaveInstanceState done put outState: " + bundle2);
    }

    private void __onStop_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "25", Void.TYPE).isSupported) {
            return;
        }
        TinyLifecycle.activityOnStop();
        PerfUtils.onStopStartTime = SystemClock.elapsedRealtime();
        long currentTimeMillis = System.currentTimeMillis();
        super.onStop();
        long currentTimeMillis2 = System.currentTimeMillis();
        RVLogger.d(TAG, "XRiverActivity.onStop " + this);
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onStop();
        }
        aa.h = null;
        aa.c = 0.0f;
        aa.d = null;
        aa.i = 0;
        aa.j = 0;
        RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "reSetScreenState");
        long currentTimeMillis3 = System.currentTimeMillis();
        PerfUtils.onStopEndTime = SystemClock.elapsedRealtime();
        if (PerfUtils.isReport) {
            PerfUtils.recordPhaseCostTime("onStop", (currentTimeMillis3 - currentTimeMillis) + ":" + (currentTimeMillis2 - currentTimeMillis) + ":" + (currentTimeMillis3 - currentTimeMillis2));
            PerfUtils.reportANRBehaviorEvent();
        }
        if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("H5_ACTIVITY_ON_STOP_COST", false)) {
            H5LogData seedId = H5LogData.seedId("H5_ACTIVITY_ON_STOP_COST");
            seedId.param3().add("SuperOnStopCost", Long.valueOf(currentTimeMillis2 - currentTimeMillis)).add("ActivityHelperOnStopCost", Long.valueOf(currentTimeMillis3 - currentTimeMillis2)).add("NebulaActivityOnStopCost", Long.valueOf(currentTimeMillis3 - currentTimeMillis)).add("appId", ((BaseFragmentActivity) this).mApp.getAppId()).add("isXRiver", "yes");
            H5LogUtil.logNebulaTech(seedId);
        }
    }

    private void __onUserInteraction_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "27", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "XRiverActivity.onUserInteraction " + this);
        super.onUserInteraction();
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onUserInteraction();
        }
    }

    private void __onUserLeaveHint_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "29", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "XRiverActivity.onUserLeaveHint " + this);
        super.onUserLeaveHint();
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            xRiverActivityHelper.onUserLeaveHint();
        }
    }

    private void __onWindowFocusChanged_stub_private(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "31", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        super.onWindowFocusChanged(z);
        TinyWindowFocusChangedHelper.focusChanged(z);
    }

    public static void d(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, (Object) null, changeQuickRedirect, "37", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        LinkedHashMap linkedHashMap = XRiverLoadingActivity.h;
        if (!BundleUtils.getBoolean(bundle2, "__has_loading_ahead__", false)) {
            return;
        }
        RVLogger.d("XRiverLoadingActivity", "notify XRiverLoadingActivity close");
        Message obtain = Message.obtain();
        obtain.what = 0;
        if (ProcessUtils.isMainProcess()) {
            yn A = of.A();
            Intrinsics.checkNotNull(obtain);
            A.a(obtain);
            return;
        }
        IpcMsgClient.send("__close_loading_", obtain);
    }

    public void __attachBaseContext_stub(Context context) {
        Context context2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            context2 = context;
            if (PatchProxy.proxy(context2, this, changeQuickRedirect, "2", Context.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            context2 = context;
        }
        __attachBaseContext_stub_private(context2);
    }

    public void __onActivityResult_stub(int i, int i2, Intent intent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), intent, Integer.TYPE, Integer.TYPE, Intent.class, Void.TYPE}, this, changeQuickRedirect, "4").isSupported) {
            __onActivityResult_stub_private(i, i2, intent);
        }
    }

    public void __onConfigurationChanged_stub(Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "6", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            configuration2 = configuration;
        }
        __onConfigurationChanged_stub_private(configuration2);
    }

    public void __onCreate_stub(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "8", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        __onCreate_stub_private(bundle2);
    }

    public void __onDestroy_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "10", Void.TYPE).isSupported) {
            __onDestroy_stub_private();
        }
    }

    public boolean __onKeyDown_stub(int i, android.view.KeyEvent keyEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{Integer.valueOf(i), keyEvent, Integer.TYPE, android.view.KeyEvent.class, Boolean.TYPE}, this, changeQuickRedirect, "12");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return __onKeyDown_stub_private(i, keyEvent);
    }

    public void __onNewIntent_stub(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "14", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        __onNewIntent_stub_private(intent2);
    }

    public void __onPause_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "16", Void.TYPE).isSupported) {
            __onPause_stub_private();
        }
    }

    public void __onRequestPermissionsResult_stub(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "18").isSupported) {
            __onRequestPermissionsResult_stub_private(i, strArr, iArr);
        }
    }

    public void __onResume_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "20", Void.TYPE).isSupported) {
            __onResume_stub_private();
        }
    }

    public void __onSaveInstanceState_stub(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "22", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        __onSaveInstanceState_stub_private(bundle2);
    }

    public void __onStop_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "24", Void.TYPE).isSupported) {
            __onStop_stub_private();
        }
    }

    public void __onUserInteraction_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "26", Void.TYPE).isSupported) {
            __onUserInteraction_stub_private();
        }
    }

    public void __onUserLeaveHint_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "28", Void.TYPE).isSupported) {
            __onUserLeaveHint_stub_private();
        }
    }

    public void __onWindowFocusChanged_stub(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "30", Boolean.TYPE, Void.TYPE).isSupported) {
            __onWindowFocusChanged_stub_private(z);
        }
    }

    public void applyTransparentTitle(boolean z) {
        XRiverActivity xRiverActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), xRiverActivity, changeQuickRedirect, "33", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
        }
        ActivityNavigationBarHelper activityNavigationBarHelper = xRiverActivity.mActivityNavigationBarHelper;
        if (activityNavigationBarHelper != null) {
            activityNavigationBarHelper.applyTransparentTitle(z);
        }
    }

    public void attachBaseContext(Context context) {
        Context context2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            context2 = context;
            if (PatchProxy.proxy(context2, this, changeQuickRedirect, "34", Context.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            context2 = context;
        }
        if (getClass() != XRiverActivity.class) {
            __attachBaseContext_stub_private(context2);
        } else {
            DexAOPEntry.android_content_ContextWrapper_attachBaseContext_proxy(XRiverActivity.class, this, context2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void finish() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "38", Void.TYPE).isSupported) {
            return;
        }
        RVLogger.d(TAG, "XRiverActivity.finish");
        ExitEvent.of(this).tagging("Activity.finish");
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            ExtensionPoint.as(ActivityOnDestroyPoint.class).node(cRVApp.getActivePage()).create().onDestroy();
        }
        super.finish();
        if (this.mCloseAllAnim) {
            overridePendingTransitionOpt(0, 0);
        } else {
            ActivityAnimBean activityAnimBean = this.mActivityAnimBean;
            if (activityAnimBean != null && activityAnimBean.needPopAnim) {
                overridePendingTransitionOpt(activityAnimBean.popEnter, activityAnimBean.popExit);
            }
        }
        RVLogger.w(TAG, "finish with stack print!", new RuntimeException("Just Print"));
        if (BundleUtils.getInt(this.mStartParams, "__AND_RESUME__") == 1) {
            H5ApplicationDelegate.openAlipayHomePageCompact(true);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void finishAndRemoveTask() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "39", Void.TYPE).isSupported) {
            return;
        }
        if (this.mCloseAllAnim) {
            overridePendingTransitionOpt(0, 0);
        } else {
            ActivityAnimBean activityAnimBean = this.mActivityAnimBean;
            if (activityAnimBean != null && activityAnimBean.needPopAnim) {
                overridePendingTransitionOpt(activityAnimBean.popEnter, activityAnimBean.popExit);
            }
        }
        ExitEvent.of(this).tagging("Activity.finishAndRemoveTask");
        super/*android.app.Activity*/.finishAndRemoveTask();
        if (BundleUtils.getInt(this.mStartParams, "__AND_RESUME__") == 1) {
            H5ApplicationDelegate.openAlipayHomePageCompact(true);
        }
    }

    public int getContentViewId(String str, Bundle bundle) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, bundle, String.class, Bundle.class, Integer.TYPE}, this, changeQuickRedirect, "40");
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return R.layout.layout_nebulax_main;
    }

    public App getCurrentApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "41", App.class);
            if (proxy.isSupported) {
                return (App) proxy.result;
            }
        }
        XRiverActivityHelper xRiverActivityHelper = this.mXRiverActivityHelper;
        if (xRiverActivityHelper != null) {
            return xRiverActivityHelper.getApp();
        }
        RVLogger.w(TAG, "XRiverActivity.getCurrentApp but mXRiverActivityHelper null!!!");
        return null;
    }

    public String getCurrentUri() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "42", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp != null) {
            Page activePage = cRVApp.getActivePage();
            if (activePage != null) {
                return activePage.getPageURI();
            }
            return null;
        }
        RVLogger.w(TAG, "XRiverActivity.getCurrentUri but mCRVApp null!!!");
        return null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    @NonNull
    public Intent[] getIntents() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "43", Intent[].class);
            if (proxy.isSupported) {
                return (Intent[]) proxy.result;
            }
        }
        return new Intent[]{getIntent(), this.newIntent};
    }

    public NavigationBarOperator getNavigationBarOperator() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "44", NavigationBarOperator.class);
            if (proxy.isSupported) {
                return (NavigationBarOperator) proxy.result;
            }
        }
        ActivityNavigationBarHelper activityNavigationBarHelper = this.mActivityNavigationBarHelper;
        if (activityNavigationBarHelper != null) {
            return activityNavigationBarHelper.sharedNavigationBarOperator;
        }
        return null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public int getWrapperViewWidth() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "45", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        int i = this.mWrapperViewWidth;
        if (i != 0) {
            return i;
        }
        View findViewById = findViewById(R.id.nebulax_wrapper_view);
        if (findViewById != null) {
            this.mWrapperViewWidth = findViewById.getWidth();
        }
        return this.mWrapperViewWidth;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean handleRestoreInMainProc(@Nullable Bundle bundle) {
        Bundle bundle2;
        Bundle bundle3;
        String string;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            PatchProxyResult proxy = PatchProxy.proxy(bundle2, this, changeQuickRedirect, "46", Bundle.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            bundle2 = bundle;
        }
        LoggerFactory.getTraceLogger().debug(TAG, "XRiverActivity.onCreate handleRestoreInMainProc");
        if (bundle2 != null && !bundle2.isEmpty()) {
            if (!XriverH5Utils.isInWallet()) {
                return true;
            }
            String string2 = BundleUtils.getString(bundle2, RESTORE_APPID);
            String config = ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfig("h5_canRestoreInMainProc_allowList", "");
            RVLogger.d(TAG, "h5_canRestoreInMainProc_allowList  cfg: " + config);
            JSONArray parseArray = JSONUtils.parseArray(config);
            if ((string2 != null && CollectionUtils.isEmpty(parseArray)) || (config != null && !parseArray.contains(string2))) {
                RVLogger.d(TAG, "not in can restore allow list ! just finish!");
                return true;
            }
            try {
                bundle3 = (Bundle) BundleUtils.getParcelable(bundle2, RESTORE_PARAMS);
                try {
                    BundleUtils.tryUnparcel(bundle3);
                } catch (Throwable th) {
                    th = th;
                    RVLogger.e(TAG, "tryUnParcel exception!", th);
                    LoggerFactory.getTraceLogger().debug(TAG, "XRiverActivity.onCreate handleRestoreInMainProc get restoreAppId: " + string2 + ", restoreParam: " + bundle3);
                    string = BundleUtils.getString(bundle3, "ap_framework_sceneId");
                    String string3 = BundleUtils.getString(bundle3, "bizScenario");
                    if (XriverH5Utils.isScanAppId(string)) {
                    }
                    RVLogger.d(TAG, "restore from scan, just finish!");
                    return true;
                }
            } catch (Throwable th2) {
                th = th2;
                bundle3 = null;
            }
            LoggerFactory.getTraceLogger().debug(TAG, "XRiverActivity.onCreate handleRestoreInMainProc get restoreAppId: " + string2 + ", restoreParam: " + bundle3);
            string = BundleUtils.getString(bundle3, "ap_framework_sceneId");
            String string32 = BundleUtils.getString(bundle3, "bizScenario");
            if (XriverH5Utils.isScanAppId(string) && !"scanApp".equals(string32)) {
                if (!TextUtils.isEmpty(string2) && bundle3 != null) {
                    RVInitializer.setupProxy(this);
                    RVLogger.d(TAG, "restore appId: " + string2);
                    MicroApplicationContext microApplicationContext = LauncherApplicationAgent.getInstance().getMicroApplicationContext();
                    microApplicationContext.setStartActivityContext(this);
                    bundle3.remove("nebulaStartflag");
                    bundle3.putBoolean("closeAllActivityAnimation", true);
                    microApplicationContext.startApp((String) null, string2, bundle3);
                } else {
                    H5ApplicationDelegate.openAlipayHomePage();
                }
                finish();
                return true;
            }
            RVLogger.d(TAG, "restore from scan, just finish!");
            return true;
        }
        LoggerFactory.getTraceLogger().debug(TAG, "XRiverActivity.onCreate handleRestoreInMainProc is not restore,return. ");
        return false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean isActivityHostUsedByOrphanTask() {
        Object obj;
        Set set;
        boolean z;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "47", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        AtomicBoolean atomicBoolean = v.a;
        Intrinsics.checkNotNullParameter(this, "activity");
        LinkedHashMap linkedHashMap = v.b().e;
        Intrinsics.checkNotNullParameter(this, "activity");
        Object obj2 = null;
        try {
            Result.Companion companion = Result.Companion;
            List runningTaskInfo$default = ContextExtensionKt.runningTaskInfo$default(this, 0, 1, (Object) null);
            if (runningTaskInfo$default != null) {
                ArrayList arrayList = new ArrayList(CollectionsKt__IterablesKt.collectionSizeOrDefault(runningTaskInfo$default, 10));
                Iterator it = runningTaskInfo$default.iterator();
                while (it.hasNext()) {
                    arrayList.add(Integer.valueOf(ContextExtensionKt.compactTaskId((ActivityManager.RunningTaskInfo) it.next())));
                }
                set = CollectionsKt___CollectionsKt.toSet(arrayList);
            } else {
                set = null;
            }
            LinkedHashMap linkedHashMap2 = new LinkedHashMap();
            for (Map.Entry entry : linkedHashMap.entrySet()) {
                if (set != null && set.contains(entry.getKey())) {
                    z = true;
                } else {
                    z = false;
                }
                if (!z) {
                    linkedHashMap2.put(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry entry2 : linkedHashMap2.entrySet()) {
                RVLogger.e("ActivityTask", "remove orphan task: " + entry2.getKey() + ", " + entry2.getValue() + "}");
                linkedHashMap.remove(entry2.getKey());
            }
            obj = Result.constructor-impl(Unit.INSTANCE);
        } catch (Throwable th) {
            Result.Companion companion2 = Result.Companion;
            obj = Result.constructor-impl(ResultKt.createFailure(th));
        }
        Throwable th2 = Result.exceptionOrNull-impl(obj);
        if (th2 != null) {
            RVLogger.e("ActivityTask", "orphan task, size= " + linkedHashMap.size() + ", trim failed", th2);
        }
        Class<?> cls = (Class) linkedHashMap.get(Integer.valueOf(getTaskId()));
        if (cls == null) {
            Iterator it2 = linkedHashMap.values().iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                Object next = it2.next();
                if (Intrinsics.areEqual((Class) next, getClass())) {
                    obj2 = next;
                    break;
                }
            }
            cls = (Class) obj2;
        }
        if (cls == getClass()) {
            return true;
        }
        return false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean isCloseSelfWindowScheduled() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "48", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra("__closeSelfWindow__", false);
    }

    public boolean isHalfScreenApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "49", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.mIsHalfScreenApp;
    }

    public boolean isResizeable() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "50", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.mIsResizeable;
    }

    public boolean isTinyApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "51", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.mIsTinyApp;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean isUnderMiniuseControl() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "52", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        CRVApp cRVApp = this.mCRVApp;
        if (cRVApp == null) {
            boolean z = !TextUtils.isEmpty(BundleUtils.getString(getIntent().getBundleExtra("sceneParams"), "kSceneParamKeyMiniUseLinkToken"));
            RVLogger.d(TAG, "isUnderMiniuseControl mCRVApp is null, judge from sceneparams, result:" + z);
            return z;
        }
        if (TextUtils.isEmpty(BundleUtils.getString(cRVApp.getSceneParams(), "kSceneParamKeyMiniUseLinkToken"))) {
            RVLogger.d(TAG, "isUnderMiniuseControl MINI_USE_LINK_TOKEN is empty, return false");
            return false;
        }
        return CRVNativeBridge.isSessionUnderMiniUseControl(this.mCRVApp.getNodeId());
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void markCloseSelfWindow(boolean z) {
        Intent intent;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "53", Boolean.TYPE, Void.TYPE).isSupported) && (intent = getIntent()) != null) {
            intent.putExtra("__closeSelfWindow__", z);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean moveTaskToBack(boolean z) {
        XRiverActivity xRiverActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), xRiverActivity, changeQuickRedirect, "54", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            xRiverActivity = this;
        }
        boolean moveTaskToBack = super/*android.app.Activity*/.moveTaskToBack(z);
        if (xRiverActivity.mCloseAllAnim) {
            overridePendingTransitionOpt(0, 0);
        } else {
            ActivityAnimBean activityAnimBean = xRiverActivity.mActivityAnimBean;
            if (activityAnimBean != null && activityAnimBean.needPopAnim) {
                overridePendingTransitionOpt(activityAnimBean.popEnter, activityAnimBean.popExit);
            }
        }
        CRVApp cRVApp = xRiverActivity.mCRVApp;
        if (cRVApp != null) {
            String appId = cRVApp.getAppId();
            AtomicBoolean atomicBoolean = v.a;
            Intrinsics.checkNotNullParameter(this, "activity");
            Intrinsics.checkNotNullParameter(appId, "appId");
            if (v.d(this)) {
                u b = v.b();
                b.getClass();
                Intrinsics.checkNotNullParameter(this, "activity");
                int taskId = getTaskId();
                boolean equals = StringsKt__StringsJVMKt.equals("YES", ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfig("ta_skip_duplicated_schedule", ""), true);
                LinkedHashMap linkedHashMap = b.f;
                if (!equals || !linkedHashMap.containsKey(Integer.valueOf(taskId))) {
                    long canStopDuration = ConfigShared.INSTANCE.getCanStopDuration();
                    p pVar = new p(canStopDuration, taskId, b, equals);
                    DexAOPEntry.java_lang_Runnable_newInstance_Created(pVar);
                    linkedHashMap.put(Integer.valueOf(taskId), new WeakReference(pVar));
                    DexAOPEntry.lite_hanlerPostDelayedProxy((Handler) b.g.getValue(), pVar, canStopDuration);
                    RVLogger.d("ActivityTask", "addStopRunnable " + taskId + ":" + canStopDuration);
                }
                v.e(this, appId, true);
            }
        }
        n4 n4Var = xRiverActivity.mExitTagged;
        n4Var.f.set(true);
        ExitEvent.Companion.of(n4Var.h).tagging("Activity.moveTaskToBack");
        return moveTaskToBack;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), intent, Integer.TYPE, Integer.TYPE, Intent.class, Void.TYPE}, this, changeQuickRedirect, "55").isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onActivityResult_stub_private(i, i2, intent);
            } else {
                DexAOPEntry.android_app_Activity_onActivityResult_proxy(XRiverActivity.class, this, i, i2, intent);
            }
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "56", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            configuration2 = configuration;
        }
        if (getClass() != XRiverActivity.class) {
            __onConfigurationChanged_stub_private(configuration2);
        } else {
            DexAOPEntry.android_content_ComponentCallbacks2_onConfigurationChanged_proxy(XRiverActivity.class, this, configuration2);
        }
    }

    public void onCreate(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "57", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        if (getClass() != XRiverActivity.class) {
            __onCreate_stub_private(bundle2);
        } else {
            DexAOPEntry.android_app_Activity_onCreate_proxy(XRiverActivity.class, this, bundle2);
        }
    }

    public void onDestroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "58", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onDestroy_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onDestroy_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onHandHalfScreenBroadcast(Intent intent) {
        XRiverActivity xRiverActivity;
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            intent2 = intent;
            if (PatchProxy.proxy(intent2, xRiverActivity, changeQuickRedirect, "59", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
            intent2 = intent;
        }
        String action = intent2.getAction();
        String stringExtra = intent2.getStringExtra("source_node_id");
        RVLogger.e(TAG, "onHandHalfScreenBroadcast, action= " + action + ", source_node_id= " + stringExtra);
        if (action != null && stringExtra != null && TextUtils.equals("halfscreen_Close", action) && xRiverActivity.mCRVApp != null && stringExtra.equals(String.valueOf(xRiverActivity.mNodeId)) && xRiverActivity.mCRVApp.getActivePage() != null) {
            EngineUtils.sendPushWorkMessage(xRiverActivity.mCRVApp.getActivePage().getRender(), "halfScreenAppClose", (JSONObject) null, (SendToWorkerCallback) null);
        }
    }

    public boolean onKeyDown(int i, android.view.KeyEvent keyEvent) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{Integer.valueOf(i), keyEvent, Integer.TYPE, android.view.KeyEvent.class, Boolean.TYPE}, this, changeQuickRedirect, "60");
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return getClass() != XRiverActivity.class ? __onKeyDown_stub_private(i, keyEvent) : DexAOPEntry.android_view_KeyEvent_Callback_onKeyDown_proxy(XRiverActivity.class, this, i, keyEvent);
    }

    public void onNewIntent(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "61", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        if (getClass() != XRiverActivity.class) {
            __onNewIntent_stub_private(intent2);
        } else {
            DexAOPEntry.android_app_Activity_onNewIntent_proxy(XRiverActivity.class, this, intent2);
        }
    }

    public void onPause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "62", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onPause_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onPause_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "63").isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onRequestPermissionsResult_stub_private(i, strArr, iArr);
            } else {
                DexAOPEntry.android_app_Activity_onRequestPermissionsResult_proxy(XRiverActivity.class, this, i, strArr, iArr);
            }
        }
    }

    public void onResume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "64", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onResume_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onResume_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "65", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        if (getClass() != XRiverActivity.class) {
            __onSaveInstanceState_stub_private(bundle2);
        } else {
            DexAOPEntry.android_app_Activity_onSaveInstanceState_proxy(XRiverActivity.class, this, bundle2);
        }
    }

    public void onStop() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "66", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onStop_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onStop_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onUserInteraction() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "67", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onUserInteraction_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onUserInteraction_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onUserLeaveHint() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "68", Void.TYPE).isSupported) {
            if (getClass() != XRiverActivity.class) {
                __onUserLeaveHint_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onUserLeaveHint_proxy(XRiverActivity.class, this);
            }
        }
    }

    public void onWindowFocusChanged(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "69", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        if (getClass() != XRiverActivity.class) {
            __onWindowFocusChanged_stub_private(z);
        } else {
            DexAOPEntry.android_view_Window_Callback_onWindowFocusChanged_proxy(XRiverActivity.class, this, z);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void overridePendingTransitionOpt(int i, int i2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "70").isSupported) {
            return;
        }
        overridePendingTransition(i, i2);
    }

    public void postHandleRestoreInMainProc() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "71", Void.TYPE).isSupported) {
            return;
        }
        LoggerFactory.getTraceLogger().error(TAG, "handleRestoreInMainProc!!! postHandleRestoreInMainProc");
        MicroApplicationContextImpl microApplicationContext = LauncherApplicationAgent.getInstance().getMicroApplicationContext();
        ConfigService configService = (ConfigService) microApplicationContext.findServiceByInterface(ConfigService.class.getName());
        if (configService != null && "YES".equalsIgnoreCase(configService.getConfig("h5_useNewPostHandleRestoreInMainProc")) && microApplicationContext.findAppById("20000001") == null && (microApplicationContext instanceof MicroApplicationContextImpl) && microApplicationContext.getApplicationManager().getActiveActivityCount() == 1) {
            LoggerFactory.getTraceLogger().error(TAG, "handleRestoreInMainProc!!! Finish and open home");
            finish();
            H5ApplicationDelegate.openAlipayHomePage();
        } else {
            LoggerFactory.getTraceLogger().error(TAG, "handleRestoreInMainProc!!! Just Finish!!!");
            finish();
        }
    }

    public void processTaskRecode(long j, Bundle bundle, long j2, String str) {
        long j3;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{Long.valueOf(j), bundle, Long.valueOf(j2), str, Long.TYPE, Bundle.class, Long.TYPE, String.class, Void.TYPE}, this, changeQuickRedirect, "72").isSupported) {
            return;
        }
        try {
            boolean isMainProcess = ProcessUtils.isMainProcess();
            TaskMonitor.Builder builder = new TaskMonitor.Builder("PROCESS_LAUNCH_TASK");
            long j4 = BundleUtils.getLong(bundle, "XRIVER_PERFORMANCE_startWindow");
            Long l = (Long) DataProvider.getData("process_launch_time");
            if (l != null) {
                j3 = l.longValue() + (System.currentTimeMillis() - SystemClock.elapsedRealtime());
            } else {
                j3 = 0;
            }
            builder.triggerFrom("StartWindow").triggerTime(j4).nodeId(j);
            if (!isMainProcess && TextUtils.equals(str, "activity")) {
                builder.beginTime(j3).endTime(j2);
            } else {
                builder.beginTime(j4).endTime(j4);
            }
            HashMap hashMap = new HashMap();
            hashMap.put("START_PROCESS_WAYS", str);
            hashMap.put("IsMainProcess", String.valueOf(isMainProcess));
            builder.attrs(hashMap);
            builder.build();
        } catch (Throwable th) {
            RVLogger.e(TAG, "ignore ,just recode", th);
        }
    }

    public void resetOrStopSelfInClientWhenDestroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "73", Void.TYPE).isSupported) {
            return;
        }
        if (this.mFromRestore) {
            RVLogger.d(TAG, "XRiverActivity.onDestroy not call resetOrStopSelfInClientWhenDestroy (from restore)");
        } else if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("ta_nebula_reuse_liteprocess", true)) {
            RVLogger.d(TAG, "XRiverActivity.onDestroy schedule resetSelf! ".concat(getClass().getName()));
            LiteProcessApi.resetSelfInClient();
        } else {
            RVLogger.d(TAG, "XRiverActivity.onDestroy schedule stopSelf! ".concat(getClass().getName()));
            LiteProcessApi.stopSelfInClient();
        }
    }

    public void restartApp() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "74", Void.TYPE).isSupported) {
            return;
        }
        try {
            this.mCRVApp.putBooleanValue("KEY_RESTARTING_APP", true);
            Bundle bundle = new Bundle();
            bundle.putBundle("startParams", this.mStartParams);
            Bundle sceneParams4Restart = XriverH5Utils.getSceneParams4Restart(this.mSceneParams);
            if (sceneParams4Restart != null) {
                RVLogger.d(TAG, "restartApp, sceneParams4Restart: " + sceneParams4Restart);
                bundle.putBundle("sceneParams", sceneParams4Restart);
            }
            IpcClientUtils.sendMsgToServerByApp(this.mCRVApp, 104, bundle);
        } catch (Throwable th) {
            RVLogger.e(TAG, "exitAndRestartApp......e=" + th);
        }
    }

    public void setWrapperViewWidth(int i) {
        XRiverActivity xRiverActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity = this;
            if (PatchProxy.proxy(Integer.valueOf(i), xRiverActivity, changeQuickRedirect, "75", Integer.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity = this;
        }
        xRiverActivity.mWrapperViewWidth = i;
    }

    /* JADX WARN: Code restructure failed: missing block: B:32:0x00c1, code lost:
    
        if (isCloseSelfWindowScheduled() != false) goto L39;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0080  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0084  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void startActivity(Intent intent, @Nullable Bundle bundle) {
        Activity activity;
        ComponentName component;
        ComponentName component2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{intent, bundle, Intent.class, Bundle.class, Void.TYPE}, this, changeQuickRedirect, "76").isSupported) {
            return;
        }
        AtomicBoolean atomicBoolean = v.a;
        Intrinsics.checkNotNullParameter(this, "activity");
        Intrinsics.checkNotNullParameter(intent, "intent");
        if (Build.VERSION.SDK_INT > 33 && v.d(this) && (component2 = intent.getComponent()) != null) {
            String className = component2.getClassName();
            Intrinsics.checkNotNullExpressionValue(className, "getClassName(...)");
            if (StringsKt__StringsJVMKt.endsWith$default(className, "AlipayLogin", false, 2, (Object) null) && (activity = ActivityUtils.getActivitySafe(ContextHolder.getContext(), "com.eg.android.AlipayGphone.AlipayLogin")) != null) {
                RVLogger.w("ActivityTask", className + " not support multi task, redirect to default main task");
                if (activity == null) {
                    DexAOPEntry.android_content_Context_startActivity_proxy(activity, intent, bundle);
                    return;
                }
                Intrinsics.checkNotNullParameter(this, "activity");
                Intrinsics.checkNotNullParameter(intent, "intent");
                if (v.d(this) && (component = intent.getComponent()) != null) {
                    String className2 = component.getClassName();
                    Intrinsics.checkNotNullExpressionValue(className2, "getClassName(...)");
                    if (!StringsKt__StringsJVMKt.endsWith$default(className2, "CSPresentActivity", false, 2, (Object) null)) {
                        if (Intrinsics.areEqual(className2, "com.alipay.android.phone.xriver.bundlex.CSGAPushActivity")) {
                            if (!isFinishing()) {
                                Intrinsics.checkNotNull(this, "null cannot be cast to non-null type com.alipay.mobile.liteprocess.advice.OrphanTaskState");
                            }
                            RVLogger.w("ActivityTask", className2 + " should remain in main stack");
                            intent.addFlags(0x10000000);
                        }
                    } else {
                        String str = className2 + "$" + getClass().getSimpleName();
                        try {
                            Class.forName(str);
                            intent.setComponent(new ComponentName(component.getPackageName(), str));
                            RVLogger.w("ActivityTask", "replace target activity =>" + str);
                        } catch (Throwable th) {
                            RVLogger.w("ActivityTask", className2 + " not support multi task", th);
                        }
                    }
                }
                super/*android.content.ContextWrapper*/.startActivity(intent, bundle);
                return;
            }
        }
        activity = null;
        if (activity == null) {
        }
    }
}
