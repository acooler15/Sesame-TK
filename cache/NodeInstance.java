//
// Decompiled by Jadx - 623ms
//
package com.alibaba.ariver.app;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Process;
import android.support.annotation.Nullable;
import com.alibaba.ariver.kernel.api.extension.ExtensionManager;
import com.alibaba.ariver.kernel.api.node.DataNode;
import com.alibaba.ariver.kernel.api.node.Node;
import com.alibaba.ariver.kernel.api.node.ValueStore;
import com.alibaba.ariver.kernel.api.security.Accessor;
import com.alibaba.ariver.kernel.api.security.DefaultGroup;
import com.alibaba.ariver.kernel.api.security.Group;
import com.alibaba.ariver.kernel.api.security.Permission;
import com.alibaba.ariver.kernel.common.multiinstance.InstanceType;
import com.alibaba.ariver.kernel.common.multiinstance.MultiInstanceUtils;
import com.alibaba.ariver.kernel.common.utils.RVKernelUtils;
import com.alibaba.ariver.kernel.common.utils.RVLogger;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.instantrun.ChangeQuickRedirect;
import com.alipay.instantrun.ConstructorCode;
import com.alipay.instantrun.PatchProxy;
import com.alipay.instantrun.PatchProxyResult;
import com.alipay.mobile.framework.MpaasClassInfo;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@MpaasClassInfo(BundleName = "multiplatform-phone-xriver_integration", ExportJarName = "api", Level = "container", Product = "容器")
@SuppressLint({"ParcelCreator"})
public class NodeInstance implements DataNode, ValueStore {
    private static final String TAG = "AriverNodeInstance";
    protected static ExtensionManager sExtensionManager;
    private static int sNodeIdBase = Process.myPid() * 10000;
    private static int sNodeIdCounter = 1;
    public static ChangeQuickRedirect 支;
    private boolean mAlreadyFinalized;
    private final Stack<Node> mChildNodes;
    private final Map<Class, Object> mDataStore;
    private CountDownLatch mFinalizedLatch;
    private InstanceType mInstanceType;
    private long mNodeId;
    private Node mParentNode;
    private final Map<String, Object> mValueStoreMap;

    public NodeInstance() {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "1")) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.mChildNodes = new Stack<>();
        this.mDataStore = new ConcurrentHashMap();
        this.mValueStoreMap = new ConcurrentHashMap();
        this.mAlreadyFinalized = false;
        this.mFinalizedLatch = new CountDownLatch(1);
        this.mNodeId = generateNodeId();
    }

    public static void bindExtensionManager(ExtensionManager extensionManager) {
        ExtensionManager extensionManager2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            extensionManager2 = extensionManager;
            if (PatchProxy.proxy(extensionManager2, (Object) null, changeQuickRedirect, "4", ExtensionManager.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            extensionManager2 = extensionManager;
        }
        sExtensionManager = extensionManager2;
    }

    public static long generateNodeId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy((Object) null, changeQuickRedirect, "8", Long.TYPE);
            if (proxy.isSupported) {
                return ((Long) proxy.result).longValue();
            }
        }
        int i = sNodeIdBase;
        sNodeIdCounter = sNodeIdCounter + 1;
        return i + r1;
    }

    public static ExtensionManager getBindedExtensionManager() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy((Object) null, changeQuickRedirect, "9", ExtensionManager.class);
            if (proxy.isSupported) {
                return (ExtensionManager) proxy.result;
            }
        }
        return sExtensionManager;
    }

    public <T extends Node> T bubbleFindNode(Class<T> cls) {
        Class<T> cls2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cls2 = cls;
            PatchProxyResult proxy = PatchProxy.proxy(cls2, this, changeQuickRedirect, "5", Class.class, Node.class);
            if (proxy.isSupported) {
                return (T) proxy.result;
            }
        } else {
            cls2 = cls;
        }
        for (NodeInstance nodeInstance = this; nodeInstance != null; nodeInstance = nodeInstance.getParentNode()) {
            if (cls2.isAssignableFrom(nodeInstance.getClass())) {
                return nodeInstance;
            }
        }
        return null;
    }

    public boolean containsKey(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "6", String.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        return nodeInstance.mValueStoreMap.containsKey(str2);
    }

    public int describeContents() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return 0;
        }
        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "7", Integer.TYPE);
        if (proxy.isSupported) {
            return ((Integer) proxy.result).intValue();
        }
        return 0;
    }

    public boolean getBooleanValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "10", String.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return ((Boolean) obj).booleanValue();
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return false;
            }
        }
        return false;
    }

    public Node getChild(long j) {
        NodeInstance nodeInstance;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            PatchProxyResult proxy = PatchProxy.proxy(Long.valueOf(j), nodeInstance, changeQuickRedirect, "11", Long.TYPE, Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        } else {
            nodeInstance = this;
        }
        synchronized (nodeInstance.mChildNodes) {
            try {
                Iterator<Node> it = nodeInstance.mChildNodes.iterator();
                while (it.hasNext()) {
                    Node next = it.next();
                    if (j == next.getNodeId()) {
                        return next;
                    }
                }
                return null;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public Node getChildAt(int i) {
        NodeInstance nodeInstance;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            PatchProxyResult proxy = PatchProxy.proxy(Integer.valueOf(i), nodeInstance, changeQuickRedirect, "12", Integer.TYPE, Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        } else {
            nodeInstance = this;
        }
        if (i >= 0 && nodeInstance.mChildNodes.size() != 0 && i < nodeInstance.mChildNodes.size()) {
            return nodeInstance.mChildNodes.get(i);
        }
        return null;
    }

    public int getChildCount() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "13", Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        }
        return this.mChildNodes.size();
    }

    public Stack<Node> getChildNodes() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "14", Stack.class);
            if (proxy.isSupported) {
                return (Stack) proxy.result;
            }
        }
        return this.mChildNodes;
    }

    public <T> T getData(Class<T> cls, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(new Object[]{cls, Boolean.valueOf(z), Class.class, Boolean.TYPE, Object.class}, this, changeQuickRedirect, "16");
            if (proxy.isSupported) {
                return (T) proxy.result;
            }
        }
        T t = (T) this.mDataStore.get(cls);
        if (t == null && z) {
            try {
                t = cls.newInstance();
                this.mDataStore.put(cls, t);
                return t;
            } catch (Throwable th) {
                RVLogger.w(TAG, "getData Exception", th);
            }
        }
        return t;
    }

    public Group getGroup() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "17", Group.class);
            if (proxy.isSupported) {
                return (Group) proxy.result;
            }
        }
        return DefaultGroup.INTERNAL;
    }

    public int getIndexOfChild(Node node) {
        NodeInstance nodeInstance;
        Node node2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            node2 = node;
            PatchProxyResult proxy = PatchProxy.proxy(node2, nodeInstance, changeQuickRedirect, "18", Node.class, Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        } else {
            nodeInstance = this;
            node2 = node;
        }
        if (node2 == null) {
            return -1;
        }
        synchronized (nodeInstance.mChildNodes) {
            try {
                int size = nodeInstance.mChildNodes.size();
                for (int i = 0; i < size; i++) {
                    if (node2 == nodeInstance.mChildNodes.get(i)) {
                        return i;
                    }
                }
                return -1;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public InstanceType getInstanceType() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "19", InstanceType.class);
            if (proxy.isSupported) {
                return (InstanceType) proxy.result;
            }
        }
        return this.mInstanceType;
    }

    public int getIntValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "20", String.class, Integer.TYPE);
            if (proxy.isSupported) {
                return ((Integer) proxy.result).intValue();
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return ((Integer) obj).intValue();
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return 0;
            }
        }
        return 0;
    }

    public JSONArray getJsonArrayValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "21", String.class, JSONArray.class);
            if (proxy.isSupported) {
                return (JSONArray) proxy.result;
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return (JSONArray) obj;
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return null;
            }
        }
        return null;
    }

    public JSONObject getJsonValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "22", String.class, JSONObject.class);
            if (proxy.isSupported) {
                return (JSONObject) proxy.result;
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return (JSONObject) obj;
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return null;
            }
        }
        return null;
    }

    public long getLongValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "23", String.class, Long.TYPE);
            if (proxy.isSupported) {
                return ((Long) proxy.result).longValue();
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return ((Long) obj).longValue();
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return 0L;
            }
        }
        return 0L;
    }

    public long getNodeId() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "24", Long.TYPE);
            if (proxy.isSupported) {
                return ((Long) proxy.result).longValue();
            }
        }
        return this.mNodeId;
    }

    public Node getParentNode() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "25", Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        }
        return this.mParentNode;
    }

    public String getStringValue(String str) {
        NodeInstance nodeInstance;
        String str2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            str2 = str;
            PatchProxyResult proxy = PatchProxy.proxy(str2, nodeInstance, changeQuickRedirect, "26", String.class, String.class);
            if (proxy.isSupported) {
                return (String) proxy.result;
            }
        } else {
            nodeInstance = this;
            str2 = str;
        }
        Object obj = nodeInstance.mValueStoreMap.get(str2);
        if (obj != null) {
            try {
                return (String) obj;
            } catch (Throwable th) {
                RVLogger.e(TAG, "getStringValue error!", th);
                return null;
            }
        }
        return null;
    }

    public void inquiry(List<? extends Permission> list, Accessor.InquiryCallback inquiryCallback) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            boolean z = PatchProxy.proxy(new Object[]{list, inquiryCallback, List.class, Accessor.InquiryCallback.class, Void.TYPE}, this, changeQuickRedirect, "27").isSupported;
        }
    }

    public boolean isChildless() {
        boolean empty;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "28", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        synchronized (this.mChildNodes) {
            empty = this.mChildNodes.empty();
        }
        return empty;
    }

    public synchronized void onFinalized() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "29", Void.TYPE).isSupported) {
            return;
        }
        synchronized (this) {
            try {
                ExtensionManager extensionManager = sExtensionManager;
                if (extensionManager != null) {
                    extensionManager.exitNode(this);
                }
                this.mParentNode = null;
                this.mValueStoreMap.clear();
                this.mValueStoreMap.put("IS_FINALIZED", Boolean.TRUE);
                this.mAlreadyFinalized = true;
                this.mFinalizedLatch.countDown();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public synchronized void onInitialized() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(this, changeQuickRedirect, "30", Void.TYPE).isSupported) {
            return;
        }
        synchronized (this) {
            ExtensionManager extensionManager = sExtensionManager;
            if (extensionManager != null) {
                extensionManager.enterNode(this);
            }
        }
    }

    public Node peekChild() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "31", Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        }
        synchronized (this.mChildNodes) {
            try {
                if (this.mChildNodes.empty()) {
                    return null;
                }
                return this.mChildNodes.peek();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public Node popChild() {
        Node pop;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "32", Node.class);
            if (proxy.isSupported) {
                return (Node) proxy.result;
            }
        }
        synchronized (this.mChildNodes) {
            pop = this.mChildNodes.pop();
        }
        return pop;
    }

    public void pushChild(Node node) {
        NodeInstance nodeInstance;
        Node node2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            node2 = node;
            if (PatchProxy.proxy(node2, nodeInstance, changeQuickRedirect, "33", Node.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            nodeInstance = this;
            node2 = node;
        }
        if (node2 == null) {
            return;
        }
        synchronized (nodeInstance.mChildNodes) {
            node2.setParentNode(this);
            nodeInstance.mChildNodes.push(node2);
        }
    }

    public void putBooleanValue(String str, boolean z) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, Boolean.valueOf(z), String.class, Boolean.TYPE, Void.TYPE}, this, changeQuickRedirect, "34").isSupported) {
            return;
        }
        this.mValueStoreMap.put(str, Boolean.valueOf(z));
    }

    public void putIntValue(String str, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, Integer.valueOf(i), String.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "35").isSupported) {
            return;
        }
        this.mValueStoreMap.put(str, Integer.valueOf(i));
    }

    public void putJsonArrayValue(String str, JSONArray jSONArray) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, jSONArray, String.class, JSONArray.class, Void.TYPE}, this, changeQuickRedirect, "36").isSupported) {
            return;
        }
        this.mValueStoreMap.put(str, jSONArray);
    }

    public void putJsonValue(String str, JSONObject jSONObject) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, jSONObject, String.class, JSONObject.class, Void.TYPE}, this, changeQuickRedirect, "37").isSupported) {
            return;
        }
        this.mValueStoreMap.put(str, jSONObject);
    }

    public void putLongValue(String str, long j) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, Long.valueOf(j), String.class, Long.TYPE, Void.TYPE}, this, changeQuickRedirect, "38").isSupported) {
            return;
        }
        this.mValueStoreMap.put(str, Long.valueOf(j));
    }

    public void putStringValue(String str, String str2) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{str, str2, String.class, String.class, Void.TYPE}, this, changeQuickRedirect, "39").isSupported) {
            return;
        }
        if (str2 == null) {
            this.mValueStoreMap.remove(str);
        } else {
            this.mValueStoreMap.put(str, str2);
        }
    }

    public boolean removeChild(Node node) {
        NodeInstance nodeInstance;
        Node node2;
        boolean remove;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            node2 = node;
            PatchProxyResult proxy = PatchProxy.proxy(node2, nodeInstance, changeQuickRedirect, "40", Node.class, Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        } else {
            nodeInstance = this;
            node2 = node;
        }
        synchronized (nodeInstance.mChildNodes) {
            remove = nodeInstance.mChildNodes.remove(node2);
        }
        return remove;
    }

    public <T> void setData(Class<T> cls, T t) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{cls, t, Class.class, Object.class, Void.TYPE}, this, changeQuickRedirect, "41").isSupported) {
            return;
        }
        if (t == null) {
            this.mDataStore.remove(cls);
            return;
        }
        if (RVKernelUtils.isDebug()) {
            RVLogger.d(TAG, "setData [" + cls + "] value: " + t);
        }
        this.mDataStore.put(cls, t);
    }

    public void setInstanceType(InstanceType instanceType) {
        NodeInstance nodeInstance;
        InstanceType instanceType2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            instanceType2 = instanceType;
            if (PatchProxy.proxy(instanceType2, nodeInstance, changeQuickRedirect, "42", InstanceType.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            nodeInstance = this;
            instanceType2 = instanceType;
        }
        nodeInstance.mInstanceType = instanceType2;
    }

    public void setNodeId(long j) {
        NodeInstance nodeInstance;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            if (PatchProxy.proxy(Long.valueOf(j), nodeInstance, changeQuickRedirect, "43", Long.TYPE, Void.TYPE).isSupported) {
                return;
            }
        } else {
            nodeInstance = this;
        }
        nodeInstance.mNodeId = j;
    }

    public void setParentNode(Node node) {
        NodeInstance nodeInstance;
        Node node2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            nodeInstance = this;
            node2 = node;
            if (PatchProxy.proxy(node2, nodeInstance, changeQuickRedirect, "44", Node.class, Void.TYPE).isSupported) {
                return;
            }
        } else {
            nodeInstance = this;
            node2 = node;
        }
        nodeInstance.mParentNode = node2;
    }

    public List<Permission> usePermissions() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect == null) {
            return null;
        }
        PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "45", List.class);
        if (proxy.isSupported) {
            return (List) proxy.result;
        }
        return null;
    }

    public boolean waitOnFinalized() {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            PatchProxyResult proxy = PatchProxy.proxy(this, changeQuickRedirect, "46", Boolean.TYPE);
            if (proxy.isSupported) {
                return ((Boolean) proxy.result).booleanValue();
            }
        }
        try {
            this.mFinalizedLatch.await();
            return this.mAlreadyFinalized;
        } catch (InterruptedException unused) {
            return false;
        }
    }

    public void writeToParcel(Parcel parcel, int i) {
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && PatchProxy.proxy(new Object[]{parcel, Integer.valueOf(i), Parcel.class, Integer.TYPE, Void.TYPE}, this, changeQuickRedirect, "47").isSupported) {
            return;
        }
        parcel.writeLong(this.mNodeId);
        parcel.writeParcelable(this.mParentNode, 0);
        InstanceType instanceType = this.mInstanceType;
        if (instanceType == null) {
            parcel.writeInt(-1);
        } else {
            parcel.writeInt(instanceType.ordinal());
        }
    }

    @Nullable
    public <T> T getData(Class<T> cls) {
        Class<T> cls2;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null) {
            cls2 = cls;
            PatchProxyResult proxy = PatchProxy.proxy(cls2, this, changeQuickRedirect, "15", Class.class, Object.class);
            if (proxy.isSupported) {
                return (T) proxy.result;
            }
        } else {
            cls2 = cls;
        }
        return (T) getData(cls2, false);
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public NodeInstance(Node node) {
        this();
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "3", new Object[]{node})) != null) {
            proxy.afterSuper(this);
        } else {
            this();
            this.mParentNode = node;
            this.mInstanceType = node.getInstanceType() == null ? MultiInstanceUtils.getDefaultInstanceType() : node.getInstanceType();
            onInitialized();
        }
    }

    public NodeInstance(Parcel parcel) {
        ConstructorCode proxy;
        ChangeQuickRedirect changeQuickRedirect = 支;
        if (changeQuickRedirect != null && (proxy = PatchProxy.proxy(changeQuickRedirect, "2", new Object[]{parcel})) != null) {
            proxy.afterSuper(this);
            return;
        }
        this.mChildNodes = new Stack<>();
        this.mDataStore = new ConcurrentHashMap();
        this.mValueStoreMap = new ConcurrentHashMap();
        this.mAlreadyFinalized = false;
        this.mFinalizedLatch = new CountDownLatch(1);
        this.mNodeId = parcel.readLong();
        this.mParentNode = parcel.readParcelable(NodeInstance.class.getClassLoader());
        int readInt = parcel.readInt();
        if (readInt >= 0) {
            this.mInstanceType = InstanceType.values()[readInt];
        } else {
            this.mInstanceType = MultiInstanceUtils.getDefaultInstanceType();
        }
    }
}
