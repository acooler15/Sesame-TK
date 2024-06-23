package tkaxv7s.xposed.sesame.util;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
public class IdMapManager {
    private static List<String> idMapList= new ArrayList<String>();
    public static void register(String className){
        if(!idMapList.contains(className)){
            idMapList.add(className);
        }
    }

    public static void resetIdMap(){
        for(int i=0;i<=idMapList.size();i++){
            try {
                String className = idMapList.get(i);
                Class clazz = Class.forName(className);
                Method method = clazz.getMethod("reset");
                if (method!=null) {
                    method.invoke(null);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
