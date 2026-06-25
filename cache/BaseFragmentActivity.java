//
// Decompiled by Jadx - 937ms
//
package com.alipay.mobile.framework.app.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.alipay.dexaop.DexAOPCenter;
import com.alipay.dexaop.DexAOPEntry;
import com.alipay.dexaop.stub.android.app.Activity_onBackPressed__stub;
import com.alipay.dexaop.stub.android.app.Activity_onCreate_androidosBundle_stub;
import com.alipay.dexaop.stub.android.app.Activity_onDestroy__stub;
import com.alipay.dexaop.stub.android.app.Activity_onNewIntent_androidcontentIntent_stub;
import com.alipay.dexaop.stub.android.app.Activity_onPause__stub;
import com.alipay.dexaop.stub.android.app.Activity_onRequestPermissionsResult_int;
import com.alipay.dexaop.stub.android.app.Activity_onResume__stub;
import com.alipay.dexaop.stub.android.app.Activity_onSaveInstanceState_androidosBundle_stub;
import com.alipay.dexaop.stub.android.app.Activity_onStart__stub;
import com.alipay.dexaop.stub.android.app.Activity_onStop__stub;
import com.alipay.dexaop.stub.android.app.Activity_onUserInteraction__stub;
import com.alipay.dexaop.stub.android.app.Activity_onUserLeaveHint__stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.content.ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub;
import com.alipay.dexaop.stub.android.view.Window;
import com.alipay.dexaop.stub.java.lang.Runnable_run__stub;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.aspect.FrameworkPointcutExecution;
import com.alipay.mobile.common.logging.api.LoggerFactory;
import com.alipay.mobile.framework.LauncherApplicationAgent;
import com.alipay.mobile.framework.MicroApplicationContext;
import com.alipay.mobile.framework.MpaasClassInfo;
import com.alipay.mobile.framework.app.ActivityApplication;
import com.alipay.mobile.framework.loading.LoadingView;
import com.alipay.mobile.framework.memsaver.MemInfo;
import com.alipay.mobile.framework.memsaver.MemSaverController;
import com.alipay.mobile.framework.memsaver.MemSaverNativePageInfo;
import com.alipay.mobile.framework.memsaver.MemSaverPageListener;
import com.alipay.mobile.framework.permission.RequestPermissionsResultCallback;
import com.alipay.mobile.framework.service.ext.ExternalService;
import com.alipay.mobile.framework.stack.FrameworkAnimPlayerInfo;
import com.alipay.mobile.framework.util.ConfigUtils;
import com.alipay.mobile.quinox.utils.ContentResolvers;
import com.alipay.mobile.quinox.utils.SystemUtil;
import com.alipay.mobile.quinox.utils.TraceLogger;

@MpaasClassInfo(BundleName = "android-phone-mobilesdk-framework", ExportJarName = "api", Level = "framework", Product = "基础框架")
public abstract class BaseFragmentActivity extends QuinoxFragmentActivity implements Activity_onBackPressed__stub, Activity_onCreate_androidosBundle_stub, Activity_onDestroy__stub, Activity_onNewIntent_androidcontentIntent_stub, Activity_onPause__stub, Activity_onRequestPermissionsResult_int.ArjavalangString.Arint_stub, Activity_onResume__stub, Activity_onSaveInstanceState_androidosBundle_stub, Activity_onStart__stub, Activity_onStop__stub, Activity_onUserInteraction__stub, Activity_onUserLeaveHint__stub, ComponentCallbacks2_onConfigurationChanged_androidcontentresConfiguration_stub, ComponentCallbacks_onConfigurationChanged_androidcontentresConfiguration_stub, Window.Callback_dispatchKeyEvent_androidviewKeyEvent_stub, Window.Callback_dispatchTouchEvent_androidviewMotionEvent_stub, Window.Callback_onContentChanged__stub, Window.Callback_onWindowFocusChanged_boolean_stub, ActivityResponsable, ActivityTrackable, MemSaverPageListener {
    public static ChangeQuickRedirect 支;
    public final String TAG;
    public final String XRIVER_ACTIVITY;
    public boolean _mFinished;
    protected ActivityHelper mActivityHelper;
    protected ActivityApplication mApp;
    protected boolean mIsForeground;
    protected MicroApplicationContext mMicroApplicationContext;
    public boolean memRelease;
    public MemSaverNativePageInfo nativePageInfo;

    @MpaasClassInfo(BundleName = "android-phone-mobilesdk-framework", ExportJarName = "api", Level = "framework", Product = "基础框架")
    public class 1 implements Runnable_run__stub, Runnable {
        public static ChangeQuickRedirect 支;
        final BaseFragmentActivity this$0;
        final Handler val$handler;

        @MpaasClassInfo(BundleName = "android-phone-mobilesdk-framework", ExportJarName = "api", Level = "framework", Product = "基础框架")
        public class 1 implements Runnable_run__stub, Runnable {
            public static ChangeQuickRedirect 支;
            final 1 this$1;

            public 1(1 r4) {
                ConstructorCode proxy;
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{r4})) == null) {
                    this.this$1 = r4;
                } else {
                    this.this$1 = (1) proxy.getFieldValue(0);
                    proxy.afterSuper(this);
                }
            }

            private void __run_stub_private() {
                ChangeQuickRedirect changeQuickRedirect = 支;
                if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                    TraceLogger.i(BaseFragmentActivity.access$000(this.this$1.this$0), "do finish after delay");
                    BaseFragmentActivity.access$101(this.this$1.this$0);
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

        public 1(BaseFragmentActivity baseFragmentActivity, Handler handler) {
            ConstructorCode proxy;
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0", new Object[]{baseFragmentActivity, handler})) == null) {
                this.this$0 = baseFragmentActivity;
                this.val$handler = handler;
            } else {
                this.this$0 = (BaseFragmentActivity) proxy.getFieldValue(0);
                this.val$handler = (Handler) proxy.getFieldValue(1);
                proxy.afterSuper(this);
            }
        }

        private void __run_stub_private() {
            ChangeQuickRedirect changeQuickRedirect = 支;
            if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "2", Void.TYPE).isSupported) {
                this.this$0.getWindow().setWindowAnimations(0);
                Handler handler = this.val$handler;
                BaseFragmentActivity$1$1 r1 = new BaseFragmentActivity$1$1(this);
                DexAOPEntry.java_lang_Runnable_newInstance_Created(r1);
                DexAOPEntry.hanlerPostProxy(handler, r1);
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

    public BaseFragmentActivity() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "0")) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.TAG = "FW:" + getClass().getSimpleName();
        this.XRIVER_ACTIVITY = "com.alipay.mobile.nebulax.xriver.activity.XRiverActivity";
        this.memRelease = false;
        this.mIsForeground = false;
        this._mFinished = false;
    }

    private boolean __dispatchKeyEvent_stub_private(KeyEvent keyEvent) {
        KeyEvent keyEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            keyEvent2 = keyEvent;
            PatchProxyResult proxy = PatchProxy.proxy(keyEvent2, this, changeQuickRedirect, "2", KeyEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            keyEvent2 = keyEvent;
        }
        Object[] objArr = {keyEvent2};
        FrameworkPointcutExecution.onExecutionBefore("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchKeyEvent(KeyEvent)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchKeyEvent(KeyEvent)", this, objArr);
        boolean _dispatchKeyEvent = (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) ? _dispatchKeyEvent(keyEvent2) : false;
        FrameworkPointcutExecution.onExecutionAfter("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchKeyEvent(KeyEvent)", this, objArr);
        return _dispatchKeyEvent;
    }

    private boolean __dispatchTouchEvent_stub_private(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, this, changeQuickRedirect, "4", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            motionEvent2 = motionEvent;
        }
        Object[] objArr = {motionEvent2};
        FrameworkPointcutExecution.onExecutionBefore("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchTouchEvent(MotionEvent)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchTouchEvent(MotionEvent)", this, objArr);
        boolean _dispatchTouchEvent = (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) ? _dispatchTouchEvent(motionEvent2) : false;
        FrameworkPointcutExecution.onExecutionAfter("boolean com.alipay.mobile.framework.app.ui.BaseFragmentActivity.dispatchTouchEvent(MotionEvent)", this, objArr);
        return _dispatchTouchEvent;
    }

    private void __onBackPressed_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "6", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onBackPressed()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onBackPressed()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onBackPressed();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onBackPressed()", this, objArr);
        }
    }

    private void __onConfigurationChanged_stub_private(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    private void __onContentChanged_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "10", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onContentChanged()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onContentChanged()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onContentChanged();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onContentChanged()", this, objArr);
        }
    }

    private void __onCreate_stub_private(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "12", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        Object[] objArr = {bundle2};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onCreate(Bundle)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onCreate(Bundle)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            _onCreate(bundle2);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onCreate(Bundle)", this, objArr);
    }

    private void __onDestroy_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "14", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onDestroy()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onDestroy()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onDestroy();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onDestroy()", this, objArr);
        }
    }

    private void __onNewIntent_stub_private(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "16", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        Object[] objArr = {intent2};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onNewIntent(Intent)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onNewIntent(Intent)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            _onNewIntent(intent2);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onNewIntent(Intent)", this, objArr);
    }

    private void __onPause_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "18", Void.TYPE).isSupported) {
            this.mIsForeground = false;
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onPause()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onPause()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onPause();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onPause()", this, objArr);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onRequestPermissionsResult_stub_private(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "20").isSupported) {
            Object[] objArr = {Integer.valueOf(i), strArr, iArr};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onRequestPermissionsResult(int,String[],int[])", this, objArr);
            super/*android.app.Activity*/.onRequestPermissionsResult(i, strArr, iArr);
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onRequestPermissionsResult(i, strArr, iArr);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onRequestPermissionsResult(int,String[],int[])", this, objArr);
        }
    }

    private void __onResume_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "22", Void.TYPE).isSupported) {
            this.mIsForeground = true;
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onResume()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onResume()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onResume();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onResume()", this, objArr);
        }
    }

    private void __onSaveInstanceState_stub_private(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "24", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        Object[] objArr = {bundle2};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onSaveInstanceState(Bundle)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onSaveInstanceState(Bundle)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            _onSaveInstanceState(bundle2);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onSaveInstanceState(Bundle)", this, objArr);
    }

    private void __onStart_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "26", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStart()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStart()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onStart();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStart()", this, objArr);
        }
    }

    private void __onStop_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "28", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStop()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStop()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onStop();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onStop()", this, objArr);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void __onUserInteraction_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "30", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onUserInteraction();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onUserInteraction();
            }
        }
    }

    private void __onUserLeaveHint_stub_private() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "32", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onUserLeaveHint()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onUserLeaveHint()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _onUserLeaveHint();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onUserLeaveHint()", this, objArr);
        }
    }

    private void __onWindowFocusChanged_stub_private(boolean z) {
        BaseFragmentActivity baseFragmentActivity;
        Bundle bundle;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), baseFragmentActivity, changeQuickRedirect, "34", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
        }
        ActivityApplication activityApplication = baseFragmentActivity.mApp;
        if (activityApplication == null || TextUtils.isEmpty(activityApplication.getAppId())) {
            bundle = null;
        } else {
            bundle = new Bundle();
            bundle.putString("appId", baseFragmentActivity.mApp.getAppId());
        }
        Object[] objArr = {Boolean.valueOf(z), bundle};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onWindowFocusChanged(boolean)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onWindowFocusChanged(boolean)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            _onWindowFocusChanged(z);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onWindowFocusChanged(boolean)", this, objArr);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private boolean _dispatchTouchEvent(MotionEvent motionEvent) {
        BaseFragmentActivity baseFragmentActivity;
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, baseFragmentActivity, changeQuickRedirect, "36", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            baseFragmentActivity = this;
            motionEvent2 = motionEvent;
        }
        ActivityHelper activityHelper = baseFragmentActivity.mActivityHelper;
        if (activityHelper != null) {
            activityHelper.dispatchOnTouchEvent(motionEvent2);
        }
        try {
            return super/*android.app.Activity*/.dispatchTouchEvent(motionEvent2);
        } catch (Throwable th) {
            TraceLogger.w(baseFragmentActivity.TAG, th);
            return false;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _finish() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "37", Void.TYPE).isSupported) {
            this._mFinished = true;
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.finish();
            }
            ActivityHelper activityHelper2 = this.mActivityHelper;
            if (activityHelper2 == null || !activityHelper2.isBehindTranslucentActivity() || SystemUtil.isOppoDevice() || Build.VERSION.SDK_INT < 25) {
                try {
                    super/*android.app.Activity*/.finish();
                    return;
                } catch (Throwable th) {
                    TraceLogger.w(this.TAG, th);
                    return;
                }
            }
            TraceLogger.w(this.TAG, "delay finish when behind translucent activity");
            Handler handler = new Handler(getMainLooper());
            BaseFragmentActivity$1 r1 = new BaseFragmentActivity$1(this, handler);
            DexAOPEntry.java_lang_Runnable_newInstance_Created(r1);
            DexAOPEntry.hanlerPostAtFrontOfQueueProxy(handler, r1);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onBackPressed() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "39", Void.TYPE).isSupported) {
            try {
                super/*android.app.Activity*/.onBackPressed();
                this.mApp.setIsPreventFromConfigChange(false);
            } catch (Exception unused) {
                finish();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onContentChanged() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "40", Void.TYPE).isSupported) {
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onContentChanged();
            }
            super/*android.app.Activity*/.onContentChanged();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onCreate(Bundle bundle) {
        BaseFragmentActivity baseFragmentActivity;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, baseFragmentActivity, changeQuickRedirect, "41", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            bundle2 = bundle;
        }
        super.onCreate(bundle2);
        TraceLogger.d("dynamicLoadToCheck", this + "/" + getApplicationContext() + "/" + Thread.currentThread().getContextClassLoader() + "/" + getClassLoader());
        ActivityHelper activityHelper = new ActivityHelper(this, bundle2);
        baseFragmentActivity.mActivityHelper = activityHelper;
        baseFragmentActivity.mApp = activityHelper.getApp();
        baseFragmentActivity.mMicroApplicationContext = baseFragmentActivity.mActivityHelper.getMicroApplicationContext();
        if (TextUtils.equals(Build.DEVICE, "M040")) {
            getWindow().getDecorView().setLayerType(1, null);
        }
        initAndAddNativePageInfo();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onDestroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "42", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onDestroy();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onDestroy();
            }
            if (this.nativePageInfo != null) {
                try {
                    MemSaverController.getInstance().getMemSaverNativeHandler().removePageInfo(this.nativePageInfo.getPageId());
                } catch (Throwable th) {
                    TraceLogger.e(this.TAG, th);
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onNewIntent(Intent intent) {
        BaseFragmentActivity baseFragmentActivity;
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            intent2 = intent;
            if (PatchProxy.proxy(intent2, baseFragmentActivity, changeQuickRedirect, "43", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            intent2 = intent;
        }
        super/*android.app.Activity*/.onNewIntent(intent2);
        ActivityHelper activityHelper = baseFragmentActivity.mActivityHelper;
        if (activityHelper != null) {
            activityHelper.onNewIntent(intent2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onPause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "44", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onPause();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onPause();
            }
        }
    }

    private void _onReady(Bundle bundle) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(bundle, this, changeQuickRedirect, "45", Bundle.class, Void.TYPE).isSupported;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onResume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "46", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onResume();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onResume();
            }
            _restoreFromAppTrimMemory();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onSaveInstanceState(Bundle bundle) {
        BaseFragmentActivity baseFragmentActivity;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, baseFragmentActivity, changeQuickRedirect, "47", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            bundle2 = bundle;
        }
        super/*android.app.Activity*/.onSaveInstanceState(bundle2);
        ActivityHelper activityHelper = baseFragmentActivity.mActivityHelper;
        if (activityHelper != null) {
            activityHelper.onSaveInstanceState(bundle2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onStart() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "48", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onStart();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onStart();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onStop() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "49", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onStop();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onStop();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onUserLeaveHint() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "50", Void.TYPE).isSupported) {
            super/*android.app.Activity*/.onUserLeaveHint();
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.onUserLeaveHint();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void _onWindowFocusChanged(boolean z) {
        BaseFragmentActivity baseFragmentActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            if (PatchProxy.proxy(Boolean.valueOf(z), baseFragmentActivity, changeQuickRedirect, "51", Boolean.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
        }
        super/*android.app.Activity*/.onWindowFocusChanged(z);
        ActivityHelper activityHelper = baseFragmentActivity.mActivityHelper;
        if (activityHelper != null) {
            activityHelper.onWindowFocusChanged(z);
        }
    }

    @Deprecated
    private void _toast(String str, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, Integer.valueOf(i), String.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "53").isSupported) {
            this.mActivityHelper.toast(str, i);
        }
    }

    public static String access$000(BaseFragmentActivity baseFragmentActivity) {
        BaseFragmentActivity baseFragmentActivity2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity2 = baseFragmentActivity;
            PatchProxyResult proxy = PatchProxy.proxy(baseFragmentActivity2, (Object) null, changeQuickRedirect, "54", BaseFragmentActivity.class, String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        } else {
            baseFragmentActivity2 = baseFragmentActivity;
        }
        return baseFragmentActivity2.TAG;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0 */
    /* JADX WARN: Type inference failed for: r0v1, types: [android.app.Activity] */
    /* JADX WARN: Type inference failed for: r0v3 */
    public static void access$101(BaseFragmentActivity baseFragmentActivity) {
        ?? r0;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            BaseFragmentActivity baseFragmentActivity2 = baseFragmentActivity;
            boolean z = PatchProxy.proxy(baseFragmentActivity2, (Object) null, changeQuickRedirect, "55", BaseFragmentActivity.class, Void.TYPE).isSupported;
            r0 = baseFragmentActivity2;
            if (z) {
                return;
            }
        } else {
            r0 = baseFragmentActivity;
        }
        super/*android.app.Activity*/.finish();
    }

    public boolean __dispatchKeyEvent_stub(KeyEvent keyEvent) {
        KeyEvent keyEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            keyEvent2 = keyEvent;
            PatchProxyResult proxy = PatchProxy.proxy(keyEvent2, this, changeQuickRedirect, "1", KeyEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            keyEvent2 = keyEvent;
        }
        return __dispatchKeyEvent_stub_private(keyEvent2);
    }

    public boolean __dispatchTouchEvent_stub(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, this, changeQuickRedirect, "3", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            motionEvent2 = motionEvent;
        }
        return __dispatchTouchEvent_stub_private(motionEvent2);
    }

    public void __onBackPressed_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "5", Void.TYPE).isSupported) {
            __onBackPressed_stub_private();
        }
    }

    public void __onConfigurationChanged_stub(Configuration configuration) {
        __onConfigurationChanged_stub_private(configuration);
    }

    public void __onContentChanged_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "9", Void.TYPE).isSupported) {
            __onContentChanged_stub_private();
        }
    }

    public void __onCreate_stub(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "11", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        __onCreate_stub_private(bundle2);
    }

    public void __onDestroy_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "13", Void.TYPE).isSupported) {
            __onDestroy_stub_private();
        }
    }

    public void __onNewIntent_stub(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "15", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        __onNewIntent_stub_private(intent2);
    }

    public void __onPause_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "17", Void.TYPE).isSupported) {
            __onPause_stub_private();
        }
    }

    public void __onRequestPermissionsResult_stub(int i, String[] strArr, int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "19").isSupported) {
            __onRequestPermissionsResult_stub_private(i, strArr, iArr);
        }
    }

    public void __onResume_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "21", Void.TYPE).isSupported) {
            __onResume_stub_private();
        }
    }

    public void __onSaveInstanceState_stub(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "23", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        __onSaveInstanceState_stub_private(bundle2);
    }

    public void __onStart_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "25", Void.TYPE).isSupported) {
            __onStart_stub_private();
        }
    }

    public void __onStop_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "27", Void.TYPE).isSupported) {
            __onStop_stub_private();
        }
    }

    public void __onUserInteraction_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "29", Void.TYPE).isSupported) {
            __onUserInteraction_stub_private();
        }
    }

    public void __onUserLeaveHint_stub() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "31", Void.TYPE).isSupported) {
            __onUserLeaveHint_stub_private();
        }
    }

    public void __onWindowFocusChanged_stub(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "33", Boolean.TYPE, Void.TYPE).isSupported) {
            __onWindowFocusChanged_stub_private(z);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean _dispatchKeyEvent(KeyEvent keyEvent) {
        BaseFragmentActivity baseFragmentActivity;
        KeyEvent keyEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            keyEvent2 = keyEvent;
            PatchProxyResult proxy = PatchProxy.proxy(keyEvent2, baseFragmentActivity, changeQuickRedirect, "35", KeyEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            baseFragmentActivity = this;
            keyEvent2 = keyEvent;
        }
        try {
            return super/*android.app.Activity*/.dispatchKeyEvent(keyEvent2);
        } catch (Throwable th) {
            TraceLogger.w(baseFragmentActivity.TAG, th);
            return false;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean _moveTaskToBack(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "38", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return super/*android.app.Activity*/.moveTaskToBack(z);
    }

    public void _restoreFromAppTrimMemory() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "52", Void.TYPE).isSupported) {
            TraceLogger.d(this.TAG, "_restoreFromAppTrimMemory memRelease=" + this.memRelease);
            if (this.memRelease) {
                this.memRelease = false;
                restoreFromAppTrimMemory();
            }
        }
    }

    public void addLoadingView(LoadingView loadingView) {
        BaseFragmentActivity baseFragmentActivity;
        LoadingView loadingView2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            loadingView2 = loadingView;
            if (PatchProxy.proxy(loadingView2, baseFragmentActivity, changeQuickRedirect, "56", LoadingView.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            loadingView2 = loadingView;
        }
        ActivityHelper activityHelper = baseFragmentActivity.mActivityHelper;
        if (activityHelper != null) {
            activityHelper.addLoadingView(loadingView2);
        }
    }

    @Deprecated
    public void alert(String str, String str2, String str3, DialogInterface.OnClickListener onClickListener, String str4, DialogInterface.OnClickListener onClickListener2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, str2, str3, onClickListener, str4, onClickListener2, String.class, String.class, String.class, DialogInterface.OnClickListener.class, String.class, DialogInterface.OnClickListener.class, Void.TYPE}, this, changeQuickRedirect, "57").isSupported) {
            Object[] objArr = {str, str2, str3, onClickListener, str4, onClickListener2};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener)", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener)", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                this.mActivityHelper.alert(str, str2, str3, onClickListener, str4, onClickListener2);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener)", this, objArr);
        }
    }

    public void dismissProgressDialog() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "60", Void.TYPE).isSupported) {
            this.mActivityHelper.dismissProgressDialog();
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        KeyEvent keyEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            keyEvent2 = keyEvent;
            PatchProxyResult proxy = PatchProxy.proxy(keyEvent2, this, changeQuickRedirect, "61", KeyEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            keyEvent2 = keyEvent;
        }
        return getClass() != BaseFragmentActivity.class ? __dispatchKeyEvent_stub_private(keyEvent2) : DexAOPEntry.android_view_Window_Callback_dispatchKeyEvent_proxy(BaseFragmentActivity.class, this, keyEvent2);
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        MotionEvent motionEvent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            motionEvent2 = motionEvent;
            PatchProxyResult proxy = PatchProxy.proxy(motionEvent2, this, changeQuickRedirect, "62", MotionEvent.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            motionEvent2 = motionEvent;
        }
        return getClass() != BaseFragmentActivity.class ? __dispatchTouchEvent_stub_private(motionEvent2) : DexAOPEntry.android_view_Window_Callback_dispatchTouchEvent_proxy(BaseFragmentActivity.class, this, motionEvent2);
    }

    public <T> T findServiceByInterface(String str) {
        BaseFragmentActivity baseFragmentActivity;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, baseFragmentActivity, changeQuickRedirect, "63", String.class, Object.class);
            if (proxy.isSupported) {
                return (T) proxy.result;
            }
        } else {
            baseFragmentActivity = this;
            str2 = str;
        }
        return (T) baseFragmentActivity.mActivityHelper.findServiceByInterface(str2);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public View findViewById(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "64", Integer.TYPE, View.class);
            if (proxy.isSupported) {
                return (View) proxy.result;
            }
        }
        return super/*android.app.Activity*/.findViewById(i);
    }

    public void finish() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "65", Void.TYPE).isSupported) {
            Object[] objArr = new Object[0];
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.finish()", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.finish()", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _finish();
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.finish()", this, objArr);
            LoggerFactory.getTraceLogger().warn("Instrumentation", "finishCalled, record caller stack", new Throwable());
        }
    }

    public ActivityApplication getActivityApplication() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "66", ActivityApplication.class);
            if (proxy.isSupported) {
                return (ActivityApplication) proxy.result;
            }
        }
        return this.mApp;
    }

    public String getActivityTrackId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "67", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        return getClass().getName();
    }

    public String getAppTrackId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "68", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        ActivityApplication activityApplication = this.mApp;
        if (activityApplication != null) {
            return activityApplication.getAppId();
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo() {
        return super.getApplicationInfo();
    }

    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public ContentResolver getContentResolver() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "71", ContentResolver.class);
            if (proxy.isSupported) {
                return (ContentResolver) proxy.result;
            }
        }
        ContentResolver contentResolver = super/*android.content.ContextWrapper*/.getContentResolver();
        ContentResolvers.fixTargetSdkInParallel(contentResolver);
        return contentResolver;
    }

    public <T extends ExternalService> T getExtServiceByInterface(String str) {
        BaseFragmentActivity baseFragmentActivity;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, baseFragmentActivity, changeQuickRedirect, "72", String.class, ExternalService.class);
            if (proxy.isSupported) {
                return (T) proxy.result;
            }
        } else {
            baseFragmentActivity = this;
            str2 = str;
        }
        return (T) baseFragmentActivity.mActivityHelper.getExtServiceByInterface(str2);
    }

    public Resources getResources() {
        return super.getResources();
    }

    public String getSourceTrackId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "74", String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        }
        ActivityApplication activityApplication = this.mApp;
        if (activityApplication != null) {
            return activityApplication.getSourceId();
        }
        return null;
    }

    public void initAndAddNativePageInfo() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "75", Void.TYPE).isSupported) {
            this.nativePageInfo = new MemSaverNativePageInfo(2, getClass().getName(), 1, this);
            MemSaverController.getInstance().getMemSaverNativeHandler().addPageInfo(this.nativePageInfo);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean isFinishing() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "76", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        if (this._mFinished) {
            return true;
        }
        return super/*android.app.Activity*/.isFinishing();
    }

    public boolean isForeground() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "77", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        return this.mIsForeground;
    }

    public boolean moveTaskToBack(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "78", Boolean.TYPE, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        Object[] objArr = new Object[0];
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.moveTaskToBack(boolean)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.moveTaskToBack(boolean)", this, objArr);
        boolean _moveTaskToBack = (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) ? _moveTaskToBack(z) : true;
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.moveTaskToBack(boolean)", this, objArr);
        return _moveTaskToBack;
    }

    public void onAppTrimMemory(MemInfo memInfo) {
        BaseFragmentActivity baseFragmentActivity;
        MemInfo memInfo2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            memInfo2 = memInfo;
            if (PatchProxy.proxy(memInfo2, baseFragmentActivity, changeQuickRedirect, "79", MemInfo.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            memInfo2 = memInfo;
        }
        TraceLogger.d(baseFragmentActivity.TAG, "onAppTrimMemory memRelease=" + baseFragmentActivity.memRelease);
        if (baseFragmentActivity.memRelease) {
            return;
        }
        if (baseFragmentActivity == LauncherApplicationAgent.getTopActivity()) {
            TraceLogger.d(baseFragmentActivity.TAG, "return onAppTrimMemory onTopActivity");
        } else {
            baseFragmentActivity.memRelease = true;
            onAppTrimMemoryEvent(memInfo2);
        }
    }

    public void onAppTrimMemoryEvent(MemInfo memInfo) {
        BaseFragmentActivity baseFragmentActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            if (PatchProxy.proxy(memInfo, baseFragmentActivity, changeQuickRedirect, "80", MemInfo.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
        }
        TraceLogger.d(baseFragmentActivity.TAG, "onAppTrimMemoryEvent");
    }

    public void onBackPressed() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "81", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onBackPressed_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onBackPressed_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onConfigurationChanged(@NonNull Configuration configuration) {
        if (getClass() != BaseFragmentActivity.class) {
            __onConfigurationChanged_stub_private(configuration);
        } else {
            DexAOPEntry.android_content_ComponentCallbacks2_onConfigurationChanged_proxy(BaseFragmentActivity.class, this, configuration);
        }
    }

    public void onContentChanged() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "83", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onContentChanged_stub_private();
            } else {
                DexAOPEntry.android_view_Window_Callback_onContentChanged_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    @SuppressLint({"NewApi"})
    public void onCreate(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "84", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        if (getClass() != BaseFragmentActivity.class) {
            __onCreate_stub_private(bundle2);
        } else {
            DexAOPEntry.android_app_Activity_onCreate_proxy(BaseFragmentActivity.class, this, bundle2);
        }
    }

    public void onDestroy() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "85", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onDestroy_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onDestroy_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onNewIntent(Intent intent) {
        Intent intent2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            intent2 = intent;
            if (PatchProxy.proxy(intent2, this, changeQuickRedirect, "86", Intent.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            intent2 = intent;
        }
        if (getClass() != BaseFragmentActivity.class) {
            __onNewIntent_stub_private(intent2);
        } else {
            DexAOPEntry.android_app_Activity_onNewIntent_proxy(BaseFragmentActivity.class, this, intent2);
        }
    }

    public void onPause() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "87", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onPause_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onPause_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:16:0x0033, code lost:
    
        if (r0.containsKey("appId") == false) goto L17;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onReady(Bundle bundle) {
        BaseFragmentActivity baseFragmentActivity;
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, baseFragmentActivity, changeQuickRedirect, "88", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            bundle2 = bundle;
        }
        ActivityApplication activityApplication = baseFragmentActivity.mApp;
        if (activityApplication != null && !TextUtils.isEmpty(activityApplication.getAppId())) {
            if (bundle2 == null) {
                bundle2 = new Bundle();
            }
            bundle2.putString("appId", baseFragmentActivity.mApp.getAppId());
        }
        Bundle bundle3 = bundle2;
        Object[] objArr = {bundle3};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onReady(Bundle)", this, objArr);
        Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onReady(Bundle)", this, objArr);
        if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
            _onReady(bundle3);
        }
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.onReady(Bundle)", this, objArr);
    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), strArr, iArr, Integer.TYPE, String[].class, int[].class, Void.TYPE}, this, changeQuickRedirect, "89").isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onRequestPermissionsResult_stub_private(i, strArr, iArr);
            } else {
                DexAOPEntry.android_app_Activity_onRequestPermissionsResult_proxy(BaseFragmentActivity.class, this, i, strArr, iArr);
            }
        }
    }

    public void onResume() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "90", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onResume_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onResume_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        Bundle bundle2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            bundle2 = bundle;
            if (PatchProxy.proxy(bundle2, this, changeQuickRedirect, "91", Bundle.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            bundle2 = bundle;
        }
        if (getClass() != BaseFragmentActivity.class) {
            __onSaveInstanceState_stub_private(bundle2);
        } else {
            DexAOPEntry.android_app_Activity_onSaveInstanceState_proxy(BaseFragmentActivity.class, this, bundle2);
        }
    }

    public void onStart() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "92", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onStart_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onStart_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onStop() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "93", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onStop_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onStop_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onUserInteraction() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "94", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onUserInteraction_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onUserInteraction_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onUserLeaveHint() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "95", Void.TYPE).isSupported) {
            if (getClass() != BaseFragmentActivity.class) {
                __onUserLeaveHint_stub_private();
            } else {
                DexAOPEntry.android_app_Activity_onUserLeaveHint_proxy(BaseFragmentActivity.class, this);
            }
        }
    }

    public void onWindowFocusChanged(boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Boolean.valueOf(z), this, changeQuickRedirect, "96", Boolean.TYPE, Void.TYPE).isSupported) {
            return;
        }
        if (getClass() != BaseFragmentActivity.class) {
            __onWindowFocusChanged_stub_private(z);
        } else {
            DexAOPEntry.android_view_Window_Callback_onWindowFocusChanged_proxy(BaseFragmentActivity.class, this, z);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void overridePendingTransition(int i, int i2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.TYPE, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "97").isSupported) {
            if ("1".equals(ConfigUtils.getConfigSafe("ig_backToLauncher_animation_optimization", ""))) {
                String name = getClass().getName();
                FrameworkAnimPlayerInfo animationConf = LauncherApplicationAgent.getInstance().getMicroApplicationContext().getAnimationConf("20000002");
                if (name.startsWith("com.alipay.mobile.nebulax.xriver.activity.XRiverActivity") && animationConf != null && animationConf.isPrepared2()) {
                    super/*android.app.Activity*/.overridePendingTransition(0, 0);
                    return;
                }
            }
            super/*android.app.Activity*/.overridePendingTransition(i, i2);
        }
    }

    @TargetApi(23)
    public void requestPermissions(String[] strArr, int i, RequestPermissionsResultCallback requestPermissionsResultCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{strArr, Integer.valueOf(i), requestPermissionsResultCallback, String[].class, Integer.TYPE, RequestPermissionsResultCallback.class, Void.TYPE}, this, changeQuickRedirect, "98").isSupported) {
            ActivityHelper activityHelper = this.mActivityHelper;
            if (activityHelper != null) {
                activityHelper.requestPermissions(strArr, i, requestPermissionsResultCallback);
            }
            DexAOPEntry.android_app_Activity_requestPermissions_proxy(this, strArr, i);
        }
    }

    public void restoreFromAppTrimMemory() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(this, changeQuickRedirect, "99", Void.TYPE).isSupported) {
            TraceLogger.d(this.TAG, "restoreFromAppTrimMemory");
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setContentView(int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(Integer.valueOf(i), this, changeQuickRedirect, "100", Integer.TYPE, Void.TYPE).isSupported) {
            return;
        }
        Object[] objArr = {Integer.valueOf(i)};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(int layoutResID)", this, objArr);
        super/*android.app.Activity*/.setContentView(i);
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(int layoutResID)", this, objArr);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setRequestedOrientation(int i) {
        BaseFragmentActivity baseFragmentActivity;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            if (PatchProxy.proxy(Integer.valueOf(i), baseFragmentActivity, changeQuickRedirect, "103", Integer.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
        }
        try {
            super/*android.app.Activity*/.setRequestedOrientation(i);
        } catch (Throwable th) {
            TraceLogger.w(baseFragmentActivity.TAG, th);
        }
    }

    public void showProgressDialog(String str) {
        BaseFragmentActivity baseFragmentActivity;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            baseFragmentActivity = this;
            str2 = str;
            if (PatchProxy.proxy(str2, baseFragmentActivity, changeQuickRedirect, "104", String.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            baseFragmentActivity = this;
            str2 = str;
        }
        baseFragmentActivity.mActivityHelper.showProgressDialog(str2);
    }

    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    public void startActivityForResult(Intent intent, int i) {
        super.startActivityForResult(intent, i);
    }

    public boolean startLoading() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "108", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        ActivityHelper activityHelper = this.mActivityHelper;
        if (activityHelper != null) {
            return activityHelper.startLoading();
        }
        return false;
    }

    public boolean stopLoading() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "109", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        ActivityHelper activityHelper = this.mActivityHelper;
        if (activityHelper != null) {
            return activityHelper.stopLoading();
        }
        return false;
    }

    @Deprecated
    public void toast(String str, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, Integer.valueOf(i), String.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "110").isSupported) {
            Object[] objArr = {str, Integer.valueOf(i)};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.toast(String, int)", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.toast(String, int)", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                _toast(str, i);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.toast(String, int)", this, objArr);
        }
    }

    @Deprecated
    public void alert(String str, String str2, String str3, DialogInterface.OnClickListener onClickListener, String str4, DialogInterface.OnClickListener onClickListener2, Boolean bool) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, str2, str3, onClickListener, str4, onClickListener2, bool, String.class, String.class, String.class, DialogInterface.OnClickListener.class, String.class, DialogInterface.OnClickListener.class, Boolean.class, Void.TYPE}, this, changeQuickRedirect, "58").isSupported) {
            Object[] objArr = {str, str2, str3, onClickListener, str4, onClickListener2, bool};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean)", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean)", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                this.mActivityHelper.alert(str, str2, str3, onClickListener, str4, onClickListener2, bool);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean)", this, objArr);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setContentView(View view) {
        View view2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            view2 = view;
            if (PatchProxy.proxy(view2, this, changeQuickRedirect, "101", View.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            view2 = view;
        }
        Object[] objArr = {view2};
        FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(View view)", this, objArr);
        super/*android.app.Activity*/.setContentView(view2);
        FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(View view)", this, objArr);
    }

    public void showProgressDialog(String str, boolean z, DialogInterface.OnCancelListener onCancelListener) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, Boolean.valueOf(z), onCancelListener, String.class, Boolean.TYPE, DialogInterface.OnCancelListener.class, Void.TYPE}, this, changeQuickRedirect, "105").isSupported) {
            this.mActivityHelper.showProgressDialog(str, z, onCancelListener);
        }
    }

    @Deprecated
    public void alert(String str, String str2, String str3, DialogInterface.OnClickListener onClickListener, String str4, DialogInterface.OnClickListener onClickListener2, Boolean bool, Boolean bool2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{str, str2, str3, onClickListener, str4, onClickListener2, bool, bool2, String.class, String.class, String.class, DialogInterface.OnClickListener.class, String.class, DialogInterface.OnClickListener.class, Boolean.class, Boolean.class, Void.TYPE}, this, changeQuickRedirect, "59").isSupported) {
            Object[] objArr = {str, str2, str3, onClickListener, str4, onClickListener2, bool, bool2};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean,Boolean)", this, objArr);
            Pair onExecutionAround = FrameworkPointcutExecution.onExecutionAround("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean,Boolean)", this, objArr);
            if (onExecutionAround == null || !((Boolean) onExecutionAround.first).booleanValue()) {
                this.mActivityHelper.alert(str, str2, str3, onClickListener, str4, onClickListener2, bool, bool2);
            }
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.alert(String,String,String,DialogInterface.OnClickListener,String,DialogInterface.OnClickListener,Boolean,Boolean)", this, objArr);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || !PatchProxy.proxy(new Object[]{view, layoutParams, View.class, ViewGroup.LayoutParams.class, Void.TYPE}, this, changeQuickRedirect, "102").isSupported) {
            Object[] objArr = {view, layoutParams};
            FrameworkPointcutExecution.onExecutionBefore("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(View view, ViewGroup.LayoutParams params)", this, objArr);
            super/*android.app.Activity*/.setContentView(view, layoutParams);
            FrameworkPointcutExecution.onExecutionAfter("void com.alipay.mobile.framework.app.ui.BaseFragmentActivity.setContentView(View view, ViewGroup.LayoutParams params)", this, objArr);
        }
    }
}
