//
// Decompiled by Jadx - 471ms
//
package com.alipay.mobile.framework.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.app.Activity_onCreate_androidosBundle_stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.content.ContextWrapper_attachBaseContext_androidcontentContext_stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.aspect.FrameworkPointcutExecution;
import com.alipay.mobile.framework.LauncherApplicationAgent;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.framework.exception.StartActivityRecord;
import com.alipay.mobile.framework.util.ActivityCrashHelper;
import com.alipay.mobile.quinox.screen.OrientationHelper;
import com.alipay.mobile.quinox.utils.TraceLogger;

@MpaasClassInfo(BundleName = "android-phone-mobilesdk-framework", ExportJarName = "api", Level = "framework", Product = "基础框架")
public class QuinoxFragmentActivity extends FragmentActivity implements Activity_onCreate_androidosBundle_stub, ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub, ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub, ContextWrapper_attachBaseContext_androidcontentContext_stub {
    public static final String TAG = "QuinoxFragmentActivity";
    public static ChangeQuickRedirect 支;
    public final ActivityCrashHelper activityCrashHelper;
    public volatile boolean hasLogRes;

    public QuinoxFragmentActivity() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
            proxy.afterSuper(this);
        } else {
            this.activityCrashHelper = new ActivityCrashHelper();
            this.hasLogRes = false;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
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
        super/*android.content.ContextWrapper*/.attachBaseContext(context2);
        TraceLogger.w(TAG, getClass().getSimpleName() + " replaceResources().");
        replaceResources(context2);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onConfigurationChanged_stub_private(Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "4", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            configuration2 = configuration;
        }
        super.onConfigurationChanged(configuration2);
        if (enableOrientationAdaptation()) {
            setRequestedOrientation(OrientationHelper.canRotate(configuration2) ? -1 : 1);
        } else {
            applyScreenOrientation(configuration2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onCreate_stub_private(Bundle bundle) {
        QuinoxFragmentActivity quinoxFragmentActivity;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            quinoxFragmentActivity = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, quinoxFragmentActivity, changeQuickRedirect, "6", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            quinoxFragmentActivity = this;
            bundle2 = bundle;
        }
        quinoxFragmentActivity.activityCrashHelper.beforeOnCreateCheck(this);
        try {
            super.onCreate(bundle2);
            if (enableOrientationAdaptation()) {
                setRequestedOrientation(OrientationHelper.canRotate(getResources().getConfiguration()) ? -1 : 1);
            } else {
                applyScreenOrientation(getResources().getConfiguration());
            }
            quinoxFragmentActivity.activityCrashHelper.afterOnCreate(this);
        } catch (Exception e) {
            quinoxFragmentActivity.activityCrashHelper.reportNotWork(this, e);
            throw e;
        }
    }

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

    public void __onConfigurationChanged_stub(Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "3", Configuration.class, Void.TYPE).isSupported) {
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
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "5", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        __onCreate_stub_private(bundle2);
    }

    public void _startActivityForResult(Intent intent, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{intent, Integer.valueOf(i), Intent.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "7").isSupported) {
            TraceLogger.w(TAG, new StartActivityRecord("startActivityForResult(intent=" + intent + ", code=" + i + ")"));
            super.startActivityForResult(intent, i);
        }
    }

    public void applyScreenOrientation(Configuration configuration) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(configuration, this, changeQuickRedirect, "8", Configuration.class, Void.TYPE).isSupported;
        }
    }

    public void attachBaseContext(Context context) {
        Context context2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            context2 = context;
            if (PatchProxy.proxy(context2, this, changeQuickRedirect, "9", Context.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            context2 = context;
        }
        if (getClass() != QuinoxFragmentActivity.class) {
            __attachBaseContext_stub_private(context2);
        } else {
            DexAOPEntry.android_content_ContextWrapper_attachBaseContext_proxy(QuinoxFragmentActivity.class, this, context2);
        }
    }

    public boolean enableOrientationAdaptation() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return false;
        }
        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "10", Boolean.TYPE);
        if (proxy.isSupported) {
            return ((Boolean) proxy.result).booleanValue();
        }
        return false;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public ApplicationInfo getApplicationInfo() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "11", ApplicationInfo.class);
            if (proxy.isSupported) {
                return (ApplicationInfo) proxy.result;
            }
        }
        return this.activityCrashHelper.getApplicationInfo(super/*android.content.ContextWrapper*/.getApplicationInfo());
    }

    public ClassLoader getClassLoader() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "12", ClassLoader.class);
            if (proxy.isSupported) {
                return (ClassLoader) proxy.result;
            }
        }
        return getClass().getClassLoader();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public Resources getResources() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "13", Resources.class);
            if (proxy.isSupported) {
                return (Resources) proxy.result;
            }
        }
        Resources resources = super/*android.content.ContextWrapper*/.getResources();
        if (!this.hasLogRes) {
            TraceLogger.i(TAG, "getResources:" + resources);
            this.hasLogRes = true;
        }
        return resources;
    }

    public void onConfigurationChanged(@NonNull Configuration configuration) {
        Configuration configuration2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            configuration2 = configuration;
            if (PatchProxy.proxy(configuration2, this, changeQuickRedirect, "14", Configuration.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            configuration2 = configuration;
        }
        if (getClass() != QuinoxFragmentActivity.class) {
            __onConfigurationChanged_stub_private(configuration2);
        } else {
            DexAOPEntry.android_content_ComponentCallbacks2_onConfigurationChanged_proxy(QuinoxFragmentActivity.class, this, configuration2);
        }
    }

    public void onCreate(@Nullable Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "15", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        if (getClass() != QuinoxFragmentActivity.class) {
            __onCreate_stub_private(bundle2);
        } else {
            DexAOPEntry.android_app_Activity_onCreate_proxy(QuinoxFragmentActivity.class, this, bundle2);
        }
    }

    public void replaceResources(Context context) {
        Context context2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            context2 = context;
            if (PatchProxy.proxy(context2, this, changeQuickRedirect, "16", Context.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            context2 = context;
        }
        if (ActivityConstants.replace) {
            LauncherApplicationAgent.getmBundleContext().replaceResources(context2, getClass().getName(), ActivityConstants.bundleNames);
            return;
        }
        if (!ActivityConstants.judged) {
            if ("com.eg.android.AlipayGphone".equals(context2.getPackageName())) {
                ActivityConstants.replace = true;
            }
            ActivityConstants.judged = true;
        }
        LauncherApplicationAgent.getmBundleContext().replaceResources(context2, getClass().getName(), new String[0]);
    }

    public void startActivity(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "17", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        Object[] objArr = {intent2};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivity(Intent)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivity(Intent)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            TransitionHelper.setStartWithActivityContext(intent2, false);
            _startActivityForResult(intent2, -1);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivity(Intent)", this, objArr);
    }

    public void startActivityForResult(Intent intent, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{intent, Integer.valueOf(i), Intent.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "18").isSupported) {
            Object[] objArr = {intent, Integer.valueOf(i)};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivityForResult(Intent, int)", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivityForResult(Intent, int)", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                TransitionHelper.setStartWithActivityContext(intent, false);
                _startActivityForResult(intent, i);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.startActivityForResult(Intent, int)", this, objArr);
        }
    }
}
