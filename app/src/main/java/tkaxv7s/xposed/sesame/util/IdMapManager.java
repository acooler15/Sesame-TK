package tkaxv7s.xposed.sesame.util;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

public class IdMapManager {
    private static final String TAG = IdMapManager.class.getSimpleName();
    private static List<String> idMapList;
    public static void register(String className){
        if (idMapList == null) {
            idMapList = new ArrayList<String>();
        }
        if(!idMapList.contains(className)){
            Log.i(TAG, className + " register");
            idMapList.add(className);
        }
    }

    public static void resetIdMap(){
        if (idMapList != null) {
            Log.i(TAG,String.valueOf(idMapList.size()));
            for(int i=0;i<idMapList.size();i++){
                try {
                    String className = idMapList.get(i);
                    Class clazz = Class.forName(className);
                    Method method = clazz.getMethod("reset");
                    if (method!=null) {
                        method.invoke(null);
                        Log.i(TAG, className + " reset");
                    }
                } catch(Exception e) {
                    Log.printStackTrace(TAG, e);
                }
            }
        }
    }
}
