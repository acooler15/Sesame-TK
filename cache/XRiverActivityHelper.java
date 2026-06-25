//
// Decompiled by Jadx - 566ms
//
package com.alibaba.xriver.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import com.alibaba.ariver.app.api.App;
import com.alibaba.ariver.app.api.activity.StartClientBundle;
import com.alibaba.ariver.app.api.point.activity.ActivityHelperOnCreateFinishedPoint;
import com.alibaba.ariver.app.api.point.activity.ActivityOnNewIntentPoint;
import com.alibaba.ariver.app.api.ui.fragment.IFragmentManager;
import com.alibaba.ariver.app.ipc.ClientMsgReceiver;
import com.alibaba.ariver.kernel.api.IIpcChannel;
import com.alibaba.ariver.kernel.api.extension.ExtensionPoint;
import com.alibaba.ariver.kernel.common.RVProxy;
import com.alibaba.ariver.kernel.common.service.RVConfigService;
import com.alibaba.ariver.kernel.common.utils.BundleUtils;
import com.alibaba.ariver.kernel.common.utils.ExecutorUtils;
import com.alibaba.ariver.kernel.common.utils.ProcessUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.ariver.kernel.ipc.IpcChannelManager;
import com.alibaba.ariver.kernel.ipc.IpcMessage;
import com.alibaba.xriver.android.node.CRVApp;
import com.alibaba.xriver.android.resource.CRVAppModelUtils;
import com.alibaba.xriver.android.utils.CRVParamMap;
import com.alipay.dexaop.DexAOPCenter;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.java.lang.Runnable_run__stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.framework.MpaasClassInfo;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public class XRiverActivityHelper extends XRiverLifecycleHelperBase {
    public static ChangeQuickRedirect 支;
    public ChaosReport a;

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public XRiverActivityHelper(App app, IFragmentManager iFragmentManager, FragmentActivity fragmentActivity) {
        super((App) r1[0], (IFragmentManager) r1[1], (FragmentActivity) r1[2]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", (objArr = new Object[]{app, iFragmentManager, fragmentActivity}))) != null) {
            proxy.afterSuper(this);
        } else {
            super(app, iFragmentManager, fragmentActivity);
            this.a = null;
        }
    }

    public static App createAppAndStart(String str, long j, int i, byte[] bArr, Bundle bundle, Bundle bundle2, Bundle bundle3) {
        long transferAppModelToNative;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, Long.valueOf(j), Integer.valueOf(i), bArr, bundle, bundle2, bundle3, String.class, Long.TYPE, Integer.TYPE, byte[].class, Bundle.class, Bundle.class, Bundle.class, App.class}, (Object) null, changeQuickRedirect, "1");
            if (proxy.isSupported) {
                return (App) proxy.result;
            }
        }
        if (bArr == null) {
            transferAppModelToNative = -1;
        } else {
            transferAppModelToNative = CRVAppModelUtils.transferAppModelToNative(bArr);
        }
        return nativeCreateAppAndStart(str, j, i, transferAppModelToNative, CRVParamMap.bundleToParamMap(bundle), CRVParamMap.bundleToParamMap(bundle2), CRVParamMap.bundleToParamMap(bundle3));
    }

    public static native App nativeCreateAppAndStart(String str, long j, int i, long j2, long j3, long j4, long j5);

    public void onCreate() {
        CRVApp cRVApp;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) || (cRVApp = ((XRiverLifecycleHelperBase) this).mCRVApp) == null) {
            return;
        }
        cRVApp.start();
        if (ProcessUtils.isMainProcess() && ((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("xriver_main_proc_client_channel", true)) {
            IpcChannelManager.getInstance().registerClientChannel(((XRiverLifecycleHelperBase) this).mCRVApp.getStartToken(), new IIpcChannel.Stub(this) {
                public static ChangeQuickRedirect 支;
                public final XRiverActivityHelper a;

                @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
                public class 1 implements Runnable_run__stub, Runnable {
                    public static ChangeQuickRedirect 支;
                    public final IpcMessage a;

                    public 1(IpcMessage ipcMessage) {
                        ConstructorCode proxy;
                        ChangeQuickRedirect changeQuickRedirect = 支;
                        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{ipcMessage})) != null) {
                            proxy.afterSuper(this);
                        } else {
                            this.a = ipcMessage;
                        }
                    }

                    private void __run_stub_private() {
                        ChangeQuickRedirect changeQuickRedirect = 支;
                        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                            return;
                        }
                        ClientMsgReceiver.getInstance().handleMessage(this.a);
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
                        this.a = (XRiverActivityHelper) proxy.getFieldValue(0);
                        proxy.afterSuper(this);
                    } else {
                        this.a = this;
                    }
                }

                public boolean isFinishing() {
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null) {
                        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect2, "1", Boolean.TYPE);
                        if (proxy.isSupported) {
                            return ((Boolean) proxy.result).booleanValue();
                        }
                    }
                    XRiverActivityHelper xRiverActivityHelper = this.a;
                    CRVApp cRVApp2 = ((XRiverLifecycleHelperBase) xRiverActivityHelper).mCRVApp;
                    if (cRVApp2 != null && !cRVApp2.isExited() && !((XRiverLifecycleHelperBase) xRiverActivityHelper).mActivity.isFinishing()) {
                        return false;
                    }
                    return true;
                }

                public void sendMessage(IpcMessage ipcMessage) {
                    IpcMessage ipcMessage2;
                    ChangeQuickRedirect changeQuickRedirect2 = 支;
                    if (changeQuickRedirect2 != null) {
                        ipcMessage2 = ipcMessage;
                        if (PatchProxy.proxy(ipcMessage2, this, changeQuickRedirect2, "2", IpcMessage.class, Void.TYPE).isSupported) {
                            return;
                        }
                    } else {
                        ipcMessage2 = ipcMessage;
                    }
                    XRiverActivityHelper$1$1 r7 = new XRiverActivityHelper$1$1(ipcMessage2);
                    DexAOPEntry.java_lang_Runnable_newInstance_Created(r7);
                    ExecutorUtils.runOnMain(r7);
                }
            });
        }
        StartClientBundle startClientBundle = new StartClientBundle();
        startClientBundle.appId = ((XRiverLifecycleHelperBase) this).mCRVApp.getAppId();
        startClientBundle.startParams = ((XRiverLifecycleHelperBase) this).mCRVApp.getStartParams();
        startClientBundle.sceneParams = ((XRiverLifecycleHelperBase) this).mCRVApp.getSceneParams();
        ExtensionPoint.as(ActivityHelperOnCreateFinishedPoint.class).node(((XRiverLifecycleHelperBase) this).mCRVApp).create().onActivityHelperOnCreateFinished(((XRiverLifecycleHelperBase) this).mCRVApp, ((XRiverLifecycleHelperBase) this).mActivity, startClientBundle);
    }

    public void onNewIntent(Intent intent) {
        XRiverActivityHelper xRiverActivityHelper;
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivityHelper = this;
            intent2 = intent;
            if (PatchProxy.proxy(intent2, xRiverActivityHelper, changeQuickRedirect, "4", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivityHelper = this;
            intent2 = intent;
        }
        RVLogger.d("XRIVER:Android:XRiverActivityHelper", "onNewIntent with intent: " + intent2);
        ExtensionPoint.as(ActivityOnNewIntentPoint.class).node(((XRiverLifecycleHelperBase) xRiverActivityHelper).mCRVApp).create().onNewIntent(((XRiverLifecycleHelperBase) xRiverActivityHelper).mCRVApp, ((XRiverLifecycleHelperBase) xRiverActivityHelper).mActivity, intent2);
        Bundle extras = intent2.getExtras();
        if (extras != null && !intent2.getBooleanExtra("IS_LITE_MOVE_TASK", false)) {
            Bundle bundle = (Bundle) BundleUtils.getParcelable(extras, "startParams");
            Bundle bundle2 = (Bundle) BundleUtils.getParcelable(extras, "sceneParams");
            if (((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigBoolean("ta_disable_dup_start", false) && bundle.containsKey("RESTORE_APPID") && !TextUtils.isEmpty(bundle.getString("RESTORE_APPID")) && bundle.containsKey("TrackStartupId") && ((XRiverLifecycleHelperBase) xRiverActivityHelper).mActivity.getIntent() != null) {
                String string = ((Bundle) BundleUtils.getParcelable(((XRiverLifecycleHelperBase) xRiverActivityHelper).mActivity.getIntent().getExtras(), "startParams")).getString("TrackStartupId");
                String string2 = bundle.getString("TrackStartupId");
                if (!TextUtils.isEmpty(string) && TextUtils.equals(string, string2)) {
                    RVLogger.d("XRIVER:Android:XRiverActivityHelper", "onNewIntent exit with duplicate start!");
                    return;
                }
            }
            if (bundle != null) {
                ((XRiverLifecycleHelperBase) xRiverActivityHelper).mCRVApp.restart(bundle, bundle2);
            }
            if (xRiverActivityHelper.a == null) {
                xRiverActivityHelper.a = new ChaosReport();
            }
            xRiverActivityHelper.a.reportIfAppIdMissMatched(((XRiverLifecycleHelperBase) xRiverActivityHelper).mCRVApp, extras);
        }
    }
}
