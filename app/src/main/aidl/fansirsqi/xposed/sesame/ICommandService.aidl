package fansirsqi.xposed.sesame;

import android.os.Bundle;
import fansirsqi.xposed.sesame.ICallback;
import fansirsqi.xposed.sesame.IStatusListener;

interface ICommandService {
    void executeCommand(String command, ICallback callback);
    void requestUnlock(in Bundle options, ICallback callback);
    void registerListener(IStatusListener listener);
    void unregisterListener(IStatusListener listener);
    // 借道模块无障碍服务检索指定应用窗口内的文本节点（sealed 官方通道，可下钻 WebView H5 虚拟树）。
    // 返回 JSON: {"found":true,"text":"...","left":0,"top":0,"right":0,"bottom":0}；未命中 {"found":false}；异常 {"found":false,"error":"..."}
    String findNodeByText(String packageName, String keyword);
}
