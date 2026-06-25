//
// Decompiled by Jadx - 379ms
//
package com.alipay.mobile.nebulax.integration.base.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.alibaba.ariver.app.NodeInstance;
import com.alibaba.ariver.kernel.api.node.Node;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.mobile.framework.MpaasClassInfo;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
public abstract class ICRVApp extends NodeInstance implements INebulaApp, Parcelable {
    public static ChangeQuickRedirect 支;

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ICRVApp(Parcel parcel) {
        super((Parcel) r1[0]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "0", (objArr = new Object[]{parcel}))) == null) {
            super(parcel);
        } else {
            proxy.afterSuper(this);
        }
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ICRVApp(Node node) {
        super((Node) r1[0]);
        Object[] objArr;
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null || (proxy = PatchProxy.proxy(changeQuickRedirect, "1", (objArr = new Object[]{node}))) == null) {
            super(node);
        } else {
            proxy.afterSuper(this);
        }
    }
}
