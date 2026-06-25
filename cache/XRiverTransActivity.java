//
// Decompiled by Jadx - 1208ms
//
package com.alipay.mobile.nebulax.xriver.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.alibaba.ariver.app.api.App;
import com.alibaba.ariver.kernel.common.RVProxy;
import com.alibaba.ariver.kernel.common.service.RVConfigService;
import com.alibaba.ariver.kernel.common.utils.BundleUtils;
import com.alibaba.ariver.kernel.common.utils.ExecutorUtils;
import com.alibaba.ariver.kernel.common.utils.JSONUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.ariver.resource.api.models.AppInfoModel;
import com.alibaba.ariver.resource.api.models.AppModel;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.xriver.android.node.CRVApp;
import com.alibaba.xriver.android.ui.XRiverActivityHelper;
import com.alibaba.xriver.android.utils.RVConfigServiceUtil;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.app.Activity_onCreate_androidosBundle_stub;
import com.alipay.dexaop.stub.android.app.Activity_onDestroy__stub;
import com.alipay.dexaop.stub.android.view.Window;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.antui.basic.AUCircleImageView;
import com.alipay.mobile.antui.basic.AUTextView;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.nebula.provider.TaConfigProvider;
import com.alipay.mobile.nebula.util.H5DimensionUtil;
import com.alipay.mobile.nebula.util.H5ImageUtil;
import com.alipay.mobile.nebula.util.H5StatusBarUtils;
import com.alipay.mobile.nebula.util.XriverH5Utils;
import com.alipay.mobile.nebulaappproxy.tinymenu.TinyBlurMenu;
import com.alipay.mobile.nebulaappproxy.utils.TinyAppLoggerUtils;
import com.alipay.mobile.nebulacore.util.graphics.TinyAppImageUtils;
import com.alipay.mobile.nebulaintegration.obfuscated.a4;
import com.alipay.mobile.nebulaintegration.obfuscated.aa;
import com.alipay.mobile.nebulaintegration.obfuscated.j5;
import com.alipay.mobile.nebulaintegration.obfuscated.m8;
import com.alipay.mobile.nebulaintegration.obfuscated.n8;
import com.alipay.mobile.nebulaintegration.obfuscated.o8;
import com.alipay.mobile.nebulaintegration.obfuscated.q1;
import com.alipay.mobile.nebulaintegration.obfuscated.u0;
import com.alipay.mobile.nebulaintegration.obfuscated.w8;
import com.alipay.mobile.nebulaintegration.obfuscated.x8;
import com.alipay.mobile.nebulax.engine.api.H5ConfigUtil;
import com.alipay.mobile.nebulax.integration.api.HalfScreenNavUtils;
import com.alipay.mobile.nebulax.integration.base.api.INebulaPage;
import com.alipay.mobile.nebulax.integration.base.halfscreen.HalfscreenUtils;
import com.alipay.mobile.nebulax.integration.base.halfscreen.view.slidinguppanel.SlidingUpPanelLayout;
import com.alipay.mobile.nebulax.integration.base.view.navigation.MYNavigationBar;
import com.alipay.mobile.nebulax.integration.base.view.navigation.legacy.LegacyRightButtonView;
import com.alipay.mobile.nebulax.integration.base.view.navigation.util.NavigationBarUtils;
import com.alipay.mobile.nebulax.integration.mpaas.R;
import com.alipay.mobile.nebulax.xriver.activity.XRiverActivity;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import x.n.o0;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public class XRiverTransActivity extends XRiverActivity {
    public static final h a;
    public static ChangeQuickRedirect 支;

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Lite1 extends LiteBase implements Activity_onCreate_androidosBundle_stub {
        public static ChangeQuickRedirect 支;

        public Lite1() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "onCreate NebulaTransActivity$Lite1");
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void onCreate(Bundle bundle) {
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
            if (getClass() != Lite1.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Lite1.class, this, bundle2);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Lite2 extends LiteBase implements Activity_onCreate_androidosBundle_stub {
        public static ChangeQuickRedirect 支;

        public Lite2() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "onCreate NebulaTransActivity$Lite2");
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void onCreate(Bundle bundle) {
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
            if (getClass() != Lite2.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Lite2.class, this, bundle2);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Lite3 extends LiteBase implements Activity_onCreate_androidosBundle_stub {
        public static ChangeQuickRedirect 支;

        public Lite3() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "onCreate NebulaTransActivity$Lite3");
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void onCreate(Bundle bundle) {
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
            if (getClass() != Lite3.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Lite3.class, this, bundle2);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Lite4 extends LiteBase implements Activity_onCreate_androidosBundle_stub {
        public static ChangeQuickRedirect 支;

        public Lite4() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "onCreate NebulaTransActivity$Lite4");
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void onCreate(Bundle bundle) {
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
            if (getClass() != Lite4.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Lite4.class, this, bundle2);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Lite5 extends LiteBase implements Activity_onCreate_androidosBundle_stub {
        public static ChangeQuickRedirect 支;

        public Lite5() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "onCreate NebulaTransActivity$Lite5");
        }

        @Override
        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        @Override
        public void onCreate(Bundle bundle) {
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
            if (getClass() != Lite5.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Lite5.class, this, bundle2);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class LiteBase extends XRiverActivity.XRiverLiteBase implements Activity_onCreate_androidosBundle_stub, Activity_onDestroy__stub, Window.Callback_onWindowFocusChanged_boolean_stub {
        public static final int b = 0;
        public static ChangeQuickRedirect 支;
        public boolean a;

        public LiteBase() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = false;
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            LiteBase liteBase;
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                liteBase = this;
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, liteBase, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                liteBase = this;
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            XRiverTransActivity.f(this, ((XRiverActivity) liteBase).mXRiverActivityHelper);
        }

        private void __onDestroy_stub_private() {
            CRVApp cRVApp;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "4", Void.TYPE).isSupported) {
                return;
            }
            super.onDestroy();
            XRiverActivityHelper xRiverActivityHelper = ((XRiverActivity) this).mXRiverActivityHelper;
            CRVApp cRVApp2 = null;
            if (xRiverActivityHelper != null) {
                cRVApp = xRiverActivityHelper.getApp();
            } else {
                cRVApp = null;
            }
            if (cRVApp != null && isHalfScreenApp()) {
                u0 u0Var = m8.a;
                u0Var.d(hashCode());
                XRiverActivityHelper xRiverActivityHelper2 = ((XRiverActivity) this).mXRiverActivityHelper;
                if (xRiverActivityHelper2 != null) {
                    cRVApp2 = xRiverActivityHelper2.getApp();
                }
                u0Var.c(cRVApp2.getAppId());
            }
        }

        private void __onWindowFocusChanged_stub_private(boolean z) {
            LiteBase liteBase;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                liteBase = this;
                if (PatchProxy.proxy(Boolean.valueOf(z), liteBase, changeQuickRedirect, "6", Boolean.TYPE, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                liteBase = this;
            }
            XRiverTransActivity.super.onWindowFocusChanged(z);
            if (!liteBase.a && z && isHalfScreenApp()) {
                liteBase.a = true;
                j jVar = new j(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(jVar);
                ExecutorUtils.runOnMain(jVar, 200L);
            }
        }

        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        public void __onDestroy_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                __onDestroy_stub_private();
            }
        }

        public void __onWindowFocusChanged_stub(boolean z) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "5", Boolean.TYPE, Void.TYPE).isSupported) {
                __onWindowFocusChanged_stub_private(z);
            }
        }

        public void f() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "7", Void.TYPE).isSupported) {
                return;
            }
            XRiverTransActivity.super.finish();
        }

        /* JADX WARN: Multi-variable type inference failed */
        public void finish() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "8", Void.TYPE).isSupported) {
                return;
            }
            if (isHalfScreenApp() && !isFinishing()) {
                if (ExecutorUtils.isMainThread()) {
                    XRiverTransActivity.e(this);
                    return;
                }
                i iVar = new i(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(iVar);
                ExecutorUtils.runOnMain(iVar);
                return;
            }
            XRiverTransActivity.super.finish();
        }

        public int getContentViewId(String str, Bundle bundle) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, bundle, String.class, Bundle.class, Integer.TYPE}, this, changeQuickRedirect, "9");
                if (proxy.isSupported) {
                    return ((Integer) proxy.result).intValue();
                }
            }
            if (isHalfScreenApp()) {
                return R.layout.layout_nebulax_trans_and_half;
            }
            return XRiverTransActivity.super.getContentViewId(str, bundle);
        }

        public void onCreate(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "10", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            if (getClass() != LiteBase.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(LiteBase.class, this, bundle2);
            }
        }

        public void onDestroy() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "11", Void.TYPE).isSupported) {
                if (getClass() != LiteBase.class) {
                    __onDestroy_stub_private();
                } else {
                    DexAOPEntry.android_app_Activity_onDestroy_proxy(LiteBase.class, this);
                }
            }
        }

        public void onWindowFocusChanged(boolean z) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "12", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
            if (getClass() != LiteBase.class) {
                __onWindowFocusChanged_stub_private(z);
            } else {
                DexAOPEntry.android_view_Window_Callback_onWindowFocusChanged_proxy(LiteBase.class, this, z);
            }
        }
    }

    @MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
    public static class Main extends XRiverActivity implements Activity_onCreate_androidosBundle_stub, Activity_onDestroy__stub, Window.Callback_onWindowFocusChanged_boolean_stub {
        public static final int b = 0;
        public static ChangeQuickRedirect 支;
        public boolean a;

        public Main() {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
                proxy.afterSuper(this);
            } else {
                this.a = false;
            }
        }

        private void __onCreate_stub_private(Bundle bundle) {
            Main main;
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                main = this;
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, main, changeQuickRedirect, "2", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                main = this;
                bundle2 = bundle;
            }
            super.onCreate(bundle2);
            XRiverTransActivity.f(this, ((XRiverActivity) main).mXRiverActivityHelper);
        }

        private void __onDestroy_stub_private() {
            CRVApp cRVApp;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "4", Void.TYPE).isSupported) {
                return;
            }
            super.onDestroy();
            XRiverActivityHelper xRiverActivityHelper = ((XRiverActivity) this).mXRiverActivityHelper;
            CRVApp cRVApp2 = null;
            if (xRiverActivityHelper != null) {
                cRVApp = xRiverActivityHelper.getApp();
            } else {
                cRVApp = null;
            }
            if (cRVApp != null && isHalfScreenApp()) {
                u0 u0Var = m8.a;
                u0Var.d(hashCode());
                XRiverActivityHelper xRiverActivityHelper2 = ((XRiverActivity) this).mXRiverActivityHelper;
                if (xRiverActivityHelper2 != null) {
                    cRVApp2 = xRiverActivityHelper2.getApp();
                }
                u0Var.c(cRVApp2.getAppId());
            }
        }

        private void __onWindowFocusChanged_stub_private(boolean z) {
            Main main;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                main = this;
                if (PatchProxy.proxy(Boolean.valueOf(z), main, changeQuickRedirect, "6", Boolean.TYPE, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                main = this;
            }
            super.onWindowFocusChanged(z);
            if (!main.a && z && isHalfScreenApp()) {
                main.a = true;
                l lVar = new l(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(lVar);
                ExecutorUtils.runOnMain(lVar, 200L);
            }
        }

        public void __onCreate_stub(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "1", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            __onCreate_stub_private(bundle2);
        }

        public void __onDestroy_stub() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "3", Void.TYPE).isSupported) {
                __onDestroy_stub_private();
            }
        }

        public void __onWindowFocusChanged_stub(boolean z) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "5", Boolean.TYPE, Void.TYPE).isSupported) {
                __onWindowFocusChanged_stub_private(z);
            }
        }

        public void e() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "7", Void.TYPE).isSupported) {
                return;
            }
            super.finish();
        }

        /* JADX WARN: Multi-variable type inference failed */
        public void finish() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "8", Void.TYPE).isSupported) {
                return;
            }
            if (isHalfScreenApp() && !isFinishing()) {
                if (ExecutorUtils.isMainThread()) {
                    XRiverTransActivity.e(this);
                    return;
                }
                k kVar = new k(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(kVar);
                ExecutorUtils.runOnMain(kVar);
                return;
            }
            super.finish();
        }

        public int getContentViewId(String str, Bundle bundle) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                PatchProxyResult proxy = PatchProxy.proxy(new Object[]{str, bundle, String.class, Bundle.class, Integer.TYPE}, this, changeQuickRedirect, "9");
                if (proxy.isSupported) {
                    return ((Integer) proxy.result).intValue();
                }
            }
            if (isHalfScreenApp()) {
                return R.layout.layout_nebulax_trans_and_half;
            }
            return super.getContentViewId(str, bundle);
        }

        public void onCreate(Bundle bundle) {
            Bundle bundle2;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null) {
                bundle2 = bundle;
                if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "10", Bundle.class, Void.TYPE).isSupported) {
                    return;
                }
            } else {
                bundle2 = bundle;
            }
            if (getClass() != Main.class) {
                __onCreate_stub_private(bundle2);
            } else {
                DexAOPEntry.android_app_Activity_onCreate_proxy(Main.class, this, bundle2);
            }
        }

        public void onDestroy() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "11", Void.TYPE).isSupported) {
                if (getClass() != Main.class) {
                    __onDestroy_stub_private();
                } else {
                    DexAOPEntry.android_app_Activity_onDestroy_proxy(Main.class, this);
                }
            }
        }

        public void onWindowFocusChanged(boolean z) {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "12", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
            if (getClass() != Main.class) {
                __onWindowFocusChanged_stub_private(z);
            } else {
                DexAOPEntry.android_view_Window_Callback_onWindowFocusChanged_proxy(Main.class, this, z);
            }
        }
    }

    static {
        SparseArray sparseArray = new SparseArray();
        sparseArray.put(1, Lite1.class);
        sparseArray.put(2, Lite2.class);
        sparseArray.put(3, Lite3.class);
        sparseArray.put(4, Lite4.class);
        sparseArray.put(5, Lite5.class);
        a = sparseArray;
    }

    public XRiverTransActivity() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "1")) != null) {
            proxy.afterSuper(this);
        }
    }

    public static void e(XRiverActivity xRiverActivity) {
        XRiverActivity xRiverActivity2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            xRiverActivity2 = xRiverActivity;
            if (PatchProxy.proxy(xRiverActivity2, (Object) null, changeQuickRedirect, "2", XRiverActivity.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            xRiverActivity2 = xRiverActivity;
        }
        if (xRiverActivity2.findViewById(R.id.nebulax_wrapper_view) == null) {
            if (xRiverActivity2 instanceof Main) {
                ((Main) xRiverActivity2).e();
                return;
            } else {
                if (xRiverActivity2 instanceof LiteBase) {
                    ((LiteBase) xRiverActivity2).f();
                    return;
                }
                return;
            }
        }
        xRiverActivity2.findViewById(R.id.nebulax_root_view).setPanelState(SlidingUpPanelLayout.PanelState.b);
    }

    /* JADX WARN: Removed duplicated region for block: B:104:0x0425  */
    /* JADX WARN: Removed duplicated region for block: B:106:0x040b  */
    /* JADX WARN: Removed duplicated region for block: B:111:0x02c1  */
    /* JADX WARN: Removed duplicated region for block: B:154:0x028a  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x03db  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x04c3  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x04ea  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x04fa  */
    /* JADX WARN: Removed duplicated region for block: B:76:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:77:0x04d6  */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0420  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void f(XRiverActivity xRiverActivity, XRiverActivityHelper xRiverActivityHelper) {
        App app;
        boolean z;
        boolean z2;
        boolean z3;
        boolean shouldPresentHalfScreenServiceInfo;
        String str;
        String str2;
        AppInfoModel appInfoModel;
        String appId;
        boolean z4;
        AUTextView findViewById;
        App app2;
        AUCircleImageView findViewById2;
        String str3;
        View findViewById3;
        View findViewById4;
        boolean z5;
        boolean z6;
        App app3;
        int intValue;
        int i;
        String appId2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if ((changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{xRiverActivity, xRiverActivityHelper, XRiverActivity.class, XRiverActivityHelper.class, Void.TYPE}, (Object) null, changeQuickRedirect, "3").isSupported) && !xRiverActivity.isFinishing()) {
            String stringExtra = xRiverActivity.getIntent().getStringExtra("transLandscape");
            if (!TextUtils.isEmpty(stringExtra) && "landscape".equalsIgnoreCase(stringExtra)) {
                xRiverActivity.setRequestedOrientation(0);
                NavigationBarUtils.setCutoutModeShortEdges(xRiverActivity.getWindow());
            } else {
                xRiverActivity.setRequestedOrientation(1);
            }
            View findViewById5 = xRiverActivity.findViewById(R.id.nebulax_root_view);
            if (findViewById5 != null) {
                findViewById5.setBackgroundColor(0);
            }
            View findViewById6 = xRiverActivity.findViewById(R.id.fragment_container);
            if (findViewById6 != null) {
                findViewById6.setBackgroundColor(0);
            }
            Bundle extras = xRiverActivity.getIntent().getExtras();
            if (extras != null) {
                try {
                    if (XriverH5Utils.getBoolean(extras, "transAnimate", false)) {
                        H5StatusBarUtils.setTransparentColor(xRiverActivity, 0x33000000);
                    }
                } catch (Exception e) {
                    RVLogger.e("NebulaX.AriverInt:XRiverTransActivity", e);
                }
            }
            if (xRiverActivityHelper != null && xRiverActivity.isHalfScreenApp()) {
                H5StatusBarUtils.setTransparentColor(xRiverActivity, 0);
                App app4 = xRiverActivityHelper.getApp();
                if (app4 != null) {
                    HalfscreenUtils.setHalfScreenModeFromStartParam(app4);
                    x8.n(xRiverActivity, (ViewGroup) null, app4);
                    x8.i(xRiverActivity, (ViewGroup) null, app4, new g(app4));
                    u0 u0Var = m8.a;
                    String appId3 = app4.getAppId();
                    if (appId3 != null) {
                        app = (App) ((HashMap) u0Var.b).get(appId3);
                    } else {
                        u0Var.getClass();
                        app = null;
                    }
                    if (app != null) {
                        if (!app.isExited()) {
                            if (!RVConfigServiceUtil.isAppIdInJSONObject(JSONUtils.parseObject(H5ConfigUtil.getConfigForAB("ta_half_screen_right_button", "a14.b62")), "ta_half_screen_right_button", app.getAppId(), (String) null)) {
                                RVLogger.d("HalfscreenUIUtils", "handleTinyRightButton switch is false");
                            } else {
                                Bundle startParams = app4.getStartParams();
                                HalfScreenNavUtils.HalfScreenBGStyle halfScreenBGStyle = HalfScreenNavUtils.HalfScreenBGStyle.GAUSS_BG;
                                if (BundleUtils.getInt(startParams, "halfscreenStyle", halfScreenBGStyle.getStyle()) != halfScreenBGStyle.getStyle()) {
                                    RVLogger.d("HalfscreenUIUtils", "handleTinyRightButton is not GAUSS background");
                                } else {
                                    INebulaPage activePage = app.getActivePage();
                                    MYNavigationBar navigationBarOfPage = NavigationBarUtils.getNavigationBarOfPage(activePage);
                                    if ((activePage instanceof INebulaPage) && (navigationBarOfPage instanceof MYNavigationBar)) {
                                        MYNavigationBar mYNavigationBar = navigationBarOfPage;
                                        if (!mYNavigationBar.needTinyPopMenu()) {
                                            RVLogger.d("HalfscreenUIUtils", "handleTinyRightButton is not useApNavigationBar");
                                        } else {
                                            TinyBlurMenu tinyBlurMenu = (TinyBlurMenu) activePage.getExtra("POP_MENU_EXTRA_KEY");
                                            FrameLayout frameLayout = (FrameLayout) x8.b(xRiverActivity, (ViewGroup) null, R.id.halfscreen_right_btn_container);
                                            if (tinyBlurMenu == null || frameLayout == null) {
                                                app3 = app;
                                                RVLogger.d("HalfscreenUIUtils", "handleTinyRightButton pop menu is null");
                                            } else {
                                                View legacyRightButtonView = new LegacyRightButtonView(xRiverActivity, false);
                                                legacyRightButtonView.attachAppId(app.getAppId());
                                                legacyRightButtonView.setHalfScreenRightButton();
                                                legacyRightButtonView.initViews(xRiverActivity, NavigationBarUtils.convertNavigationBarThemeToTitleBarTheme(mYNavigationBar.getCurrentTheme()), mYNavigationBar.getCurrentTitleTransparent());
                                                legacyRightButtonView.setCloseButtonOnClickListener(new w8(new WeakReference(app), new WeakReference(tinyBlurMenu), xRiverActivity));
                                                HashMap hashMap = new HashMap();
                                                hashMap.put("appId", app4.getAppId());
                                                hashMap.put("hostAppId", app.getAppId());
                                                app3 = app;
                                                TinyAppLoggerUtils.markUepExposeAndClick(legacyRightButtonView.getCloseButton(), "a192.b111870.c383554.d633058", hashMap, false);
                                                tinyBlurMenu.createHalfScreenMenu(xRiverActivity);
                                                WeakReference weakReference = new WeakReference(tinyBlurMenu);
                                                int hashCode = xRiverActivity.hashCode();
                                                u0Var.getClass();
                                                if (weakReference.get() != null) {
                                                    ((HashMap) u0Var.c).put(Integer.valueOf(hashCode), weakReference);
                                                }
                                                legacyRightButtonView.setOptionMenuOnClickListener(new n8(weakReference, hashCode));
                                                TinyAppLoggerUtils.markUepExposeAndClick(legacyRightButtonView.getOptionMenu(), "a192.b111870.c383554.d633076", hashMap, false);
                                                legacyRightButtonView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                                                if (!aa.h()) {
                                                    RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset not use new large screen adapt, return 0");
                                                } else if (xRiverActivity.isResizeable()) {
                                                    RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset isResizeable true, return 0");
                                                } else {
                                                    if (aa.d != null) {
                                                        RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset cached offset: " + aa.d);
                                                        intValue = aa.d.intValue();
                                                    } else if (aa.i(xRiverActivity) == 1.0f) {
                                                        aa.d = 0;
                                                        RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset layoutRatio == 1, return 0");
                                                        intValue = aa.d.intValue();
                                                    } else {
                                                        Display defaultDisplay = xRiverActivity.getWindowManager().getDefaultDisplay();
                                                        if (defaultDisplay.getWidth() != defaultDisplay.getMode().getPhysicalWidth() && r7 / r5 < 0.6d) {
                                                            aa.d = 0;
                                                            RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset isInMagicMode, return 0");
                                                            intValue = aa.d.intValue();
                                                        } else {
                                                            aa.d = Integer.valueOf(aa.c(xRiverActivity));
                                                            RVLogger.d("NebulaX.AriverInt:LargeScreenUtils", "getWrapperViewHorizontalOffset calculated offset: " + aa.d);
                                                            intValue = aa.d.intValue();
                                                        }
                                                    }
                                                    if (intValue != 0) {
                                                        ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                                                        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                                                            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                                                            i = 0;
                                                            int dip2px = H5DimensionUtil.dip2px(xRiverActivity, 6.0f) + Math.max(intValue, 0);
                                                            marginLayoutParams.rightMargin = dip2px;
                                                            o0.c(marginLayoutParams, dip2px);
                                                            frameLayout.setLayoutParams(marginLayoutParams);
                                                            frameLayout.setVisibility(i);
                                                            frameLayout.addView(legacyRightButtonView);
                                                        }
                                                    }
                                                    i = 0;
                                                    frameLayout.setVisibility(i);
                                                    frameLayout.addView(legacyRightButtonView);
                                                }
                                                intValue = 0;
                                                if (intValue != 0) {
                                                }
                                                i = 0;
                                                frameLayout.setVisibility(i);
                                                frameLayout.addView(legacyRightButtonView);
                                            }
                                            appId2 = app3.getAppId();
                                            if (!TextUtils.isEmpty(appId2)) {
                                                if (!RVConfigServiceUtil.isAppIdInJSONObject(JSONUtils.parseObject(H5ConfigUtil.getConfigForAB("ta_half_screen_channel_title", "a14.b62")), "ta_half_screen_channel_title", appId2, (String) null)) {
                                                    RVLogger.d("HalfscreenUIUtils", "setHalfScreenChannelTitle switch is false");
                                                } else {
                                                    Bundle startParams2 = app4.getStartParams();
                                                    HalfScreenNavUtils.HalfScreenBGStyle halfScreenBGStyle2 = HalfScreenNavUtils.HalfScreenBGStyle.GAUSS_BG;
                                                    if (BundleUtils.getInt(startParams2, "halfscreenStyle", halfScreenBGStyle2.getStyle()) != halfScreenBGStyle2.getStyle()) {
                                                        RVLogger.d("HalfscreenUIUtils", "setHalfScreenChannelTitle is not GAUSS background");
                                                    } else {
                                                        TaConfigProvider taConfigProvider = (TaConfigProvider) XriverH5Utils.getProvider(TaConfigProvider.class.getName());
                                                        if (taConfigProvider != null) {
                                                            String tinyAppConfig = taConfigProvider.getTinyAppConfig(appId2, "halfScreenChannel");
                                                            RVLogger.d("HalfscreenUIUtils", "setHalfScreenChannelTitle taConfig halfScreenChannelTitle: " + tinyAppConfig);
                                                            if (!TextUtils.isEmpty(tinyAppConfig)) {
                                                                JSONObject parseObject = JSONUtils.parseObject(tinyAppConfig);
                                                                String string = JSONUtils.getString(parseObject, "imageUrl");
                                                                String string2 = JSONUtils.getString(parseObject, "actionUrl");
                                                                ImageView imageView = (ImageView) x8.b(xRiverActivity, (ViewGroup) null, R.id.halfscreen_channel_image);
                                                                if (imageView != null && !TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                                                                    z = false;
                                                                    imageView.setVisibility(0);
                                                                    imageView.setOnClickListener(new j5(1, string2));
                                                                    TinyAppImageUtils.loadImage(string, appId2, new o8(imageView, 0));
                                                                    HashMap hashMap2 = new HashMap();
                                                                    hashMap2.put("appId", app4.getAppId());
                                                                    hashMap2.put("hostAppId", appId2);
                                                                    TinyAppLoggerUtils.markUepExposeAndClick(imageView, "a192.b111870.c383554.d633043", hashMap2, false);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            z = false;
                                        }
                                    }
                                }
                            }
                        }
                        app3 = app;
                        appId2 = app3.getAppId();
                        if (!TextUtils.isEmpty(appId2)) {
                        }
                        z = false;
                    } else {
                        z = false;
                    }
                    if ("yes".equalsIgnoreCase(((RVConfigService) RVProxy.get(RVConfigService.class)).getConfigWithProcessCache("h5_halfScreen_title_fix", "yes"))) {
                        z2 = xRiverActivity.isTinyApp();
                        RVLogger.d("NebulaX.AriverInt:XRiverTransActivity", "handle halfScreen, isTiny = " + z2);
                    } else {
                        z2 = true;
                    }
                    if (z2) {
                        if (app4.isTinyApp()) {
                            AppModel appModel = (AppModel) app4.getData(AppModel.class);
                            if (appModel != null) {
                                z6 = "NO".equalsIgnoreCase(JSONUtils.getString(appModel.getExtendInfos(), "usePresetPopmenu"));
                            } else {
                                z6 = z;
                            }
                            z5 = !z6;
                        } else {
                            z5 = z;
                        }
                        if (z5) {
                            z3 = true;
                            shouldPresentHalfScreenServiceInfo = HalfscreenUtils.shouldPresentHalfScreenServiceInfo(app4);
                            String str4 = "";
                            if (shouldPresentHalfScreenServiceInfo) {
                                str = "";
                                str2 = str;
                            } else {
                                JSONObject parseObject2 = JSONUtils.parseObject(BundleUtils.getString(app4.getStartParams(), "freeWindowServiceInfo", ""));
                                if (parseObject2 == null) {
                                    str = "";
                                    str2 = str;
                                } else {
                                    str2 = parseObject2.getString("name");
                                    str = parseObject2.getString("icon");
                                }
                                if (!z3 && (str2.isEmpty() || str.isEmpty())) {
                                    shouldPresentHalfScreenServiceInfo = z;
                                }
                            }
                            if (z3 && !shouldPresentHalfScreenServiceInfo) {
                                app2 = app4;
                            } else {
                                appInfoModel = (AppInfoModel) app4.getData(AppInfoModel.class);
                                if (appInfoModel == null) {
                                    appId = appInfoModel.getAppId();
                                } else {
                                    appId = app4.getAppId();
                                }
                                if (z3 && shouldPresentHalfScreenServiceInfo) {
                                    if (appInfoModel != null) {
                                        z = true;
                                    }
                                    z4 = z;
                                } else {
                                    z4 = true;
                                }
                                if (appInfoModel != null && !TextUtils.isEmpty(appInfoModel.getName())) {
                                    str2 = appInfoModel.getName();
                                }
                                if (appInfoModel != null && !TextUtils.isEmpty(appInfoModel.getLogo())) {
                                    str = appInfoModel.getLogo();
                                }
                                if (!TextUtils.isEmpty(str) && (findViewById2 = xRiverActivity.findViewById(R.id.tiny_half_app_icon)) != null) {
                                    H5ImageUtil.loadImage(str, appId, new a4(findViewById2, 1));
                                }
                                findViewById = xRiverActivity.findViewById(R.id.tiny_half_app_name);
                                app2 = app4;
                                x8.m(xRiverActivity, app2, findViewById, str2, xRiverActivity.getResources().getString(R.string.half_app_provide_service), xRiverActivity.getResources().getDrawable(R.drawable.halfscreen_arrow));
                                if (findViewById != null && z4) {
                                    findViewById.setOnClickListener(new q1(app2, appId, 4));
                                }
                            }
                            x8.h(xRiverActivity, (ViewGroup) null, app2);
                            View.OnClickListener j5Var = new j5(6, xRiverActivity);
                            View.OnClickListener q1Var = new q1(xRiverActivity, app2, 5);
                            HashMap hashMap3 = new HashMap();
                            hashMap3.put("appId", app2.getAppId());
                            if (app2.getSceneParams() != null) {
                                str3 = "";
                            } else {
                                String string3 = app2.getSceneParams().getString("sourcePageUrl", "");
                                str3 = app2.getSceneParams().getString("sourceAppId", "");
                                str4 = string3;
                            }
                            hashMap3.put("sourcePageUrl", str4);
                            hashMap3.put("sourceAppId", str3);
                            TinyAppLoggerUtils.markSpmExpose(xRiverActivity, "a192.b111870.c383554", hashMap3);
                            findViewById3 = xRiverActivity.findViewById(R.id.tiny_half_close);
                            if (findViewById3 != null) {
                                findViewById3.setOnClickListener(q1Var);
                                TinyAppLoggerUtils.markSpmExpose(xRiverActivity, "a192.b111870.c383554.d500375", hashMap3);
                            }
                            findViewById4 = xRiverActivity.findViewById(R.id.halfscreen_mask_view);
                            if (findViewById4 == null) {
                                findViewById4.setOnClickListener(j5Var);
                                return;
                            }
                            return;
                        }
                    }
                    z3 = z;
                    shouldPresentHalfScreenServiceInfo = HalfscreenUtils.shouldPresentHalfScreenServiceInfo(app4);
                    String str42 = "";
                    if (shouldPresentHalfScreenServiceInfo) {
                    }
                    if (z3) {
                    }
                    appInfoModel = (AppInfoModel) app4.getData(AppInfoModel.class);
                    if (appInfoModel == null) {
                    }
                    if (z3) {
                    }
                    z4 = true;
                    if (appInfoModel != null) {
                        str2 = appInfoModel.getName();
                    }
                    if (appInfoModel != null) {
                        str = appInfoModel.getLogo();
                    }
                    if (!TextUtils.isEmpty(str)) {
                        H5ImageUtil.loadImage(str, appId, new a4(findViewById2, 1));
                    }
                    findViewById = xRiverActivity.findViewById(R.id.tiny_half_app_name);
                    app2 = app4;
                    x8.m(xRiverActivity, app2, findViewById, str2, xRiverActivity.getResources().getString(R.string.half_app_provide_service), xRiverActivity.getResources().getDrawable(R.drawable.halfscreen_arrow));
                    if (findViewById != null) {
                        findViewById.setOnClickListener(new q1(app2, appId, 4));
                    }
                    x8.h(xRiverActivity, (ViewGroup) null, app2);
                    View.OnClickListener j5Var2 = new j5(6, xRiverActivity);
                    View.OnClickListener q1Var2 = new q1(xRiverActivity, app2, 5);
                    HashMap hashMap32 = new HashMap();
                    hashMap32.put("appId", app2.getAppId());
                    if (app2.getSceneParams() != null) {
                    }
                    hashMap32.put("sourcePageUrl", str42);
                    hashMap32.put("sourceAppId", str3);
                    TinyAppLoggerUtils.markSpmExpose(xRiverActivity, "a192.b111870.c383554", hashMap32);
                    findViewById3 = xRiverActivity.findViewById(R.id.tiny_half_close);
                    if (findViewById3 != null) {
                    }
                    findViewById4 = xRiverActivity.findViewById(R.id.halfscreen_mask_view);
                    if (findViewById4 == null) {
                    }
                }
            }
        }
    }
}
