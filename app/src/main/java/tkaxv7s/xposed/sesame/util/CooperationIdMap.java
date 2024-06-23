package tkaxv7s.xposed.sesame.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CooperationIdMap {
    private static final String TAG = CooperationIdMap.class.getSimpleName();

    public static boolean shouldReload = false;

    private static Map<String, String> idMap;
    private static boolean hasChanged = false;

    {
        // 加载时向IdMapManager注册
        IdMapManager.register(CooperationIdMap.class.getName());
        Log.i(TAG,"register to IdMapManager");
    }

    public static void putIdMap(String key, String value) {
        if (key == null || key.isEmpty())
            return;
        if (getIdMap().containsKey(key)) {
            if (!getIdMap().get(key).equals(value)) {
                getIdMap().remove(key);
                getIdMap().put(key, value);
                hasChanged = true;
            }
        } else {
            getIdMap().put(key, value);
            hasChanged = true;
        }
    }

    public static void removeIdMap(String key) {
        if (key == null || key.isEmpty())
            return;
        if (getIdMap().containsKey(key)) {
            getIdMap().remove(key);
            hasChanged = true;
        }
    }

    public static void saveIdMap() {
        if (hasChanged) {
            StringBuilder sb = new StringBuilder();
            Set<Map.Entry<String, String>> idSet = getIdMap().entrySet();
            for (Map.Entry<String, String> entry : idSet) {
                sb.append(entry.getKey());
                sb.append(':');
                sb.append(entry.getValue());
                sb.append('\n');
            }
            hasChanged = !FileUtil.write2File(sb.toString(), FileUtil.getCooperationIdMapFile());
        }
    }

    public static Map<String, String> getIdMap() {
        if (idMap == null || shouldReload) {
            shouldReload = false;
            idMap = new TreeMap<>();
            String str = FileUtil.readFromFile(FileUtil.getCooperationIdMapFile());
            if (str != null && str.length() > 0) {
                try {
                    String[] idSet = str.split("\n");
                    for (String s : idSet) {
                        // Log.i(TAG, s);
                        int ind = s.indexOf(":");
                        idMap.put(s.substring(0, ind), s.substring(ind + 1));
                    }
                } catch (Throwable t) {
                    Log.printStackTrace(TAG, t);
                    idMap.clear();
                }
            }
        }
        return idMap;
    }

    public static void reset(){
        if (idMap != null && !shouldReload) {
            saveIdMap();
        }
        idMap = null;
        Log.i(TAG,"reset");
    }

}
