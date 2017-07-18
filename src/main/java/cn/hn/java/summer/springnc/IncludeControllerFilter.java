package cn.hn.java.summer.springnc;

import cn.hn.java.summer.springnc.annotation.AutoController;
import cn.hn.java.summer.springnc.annotation.Controller;
import cn.hn.java.summer.springnc.annotation.RestController;

/**
 * Created by xw2sy on 2017-07-18.
 */
public class IncludeControllerFilter implements IncludeTypeFilter {

    public boolean accept(Class cls) {
        if(cls.isInterface()){
            return isControllerClass(cls);
        }else{
            Class[] interfaces= cls.getInterfaces();
            //find controller interface's implement classes
            for(Class interfaceCls : interfaces){
                if(isControllerClass(interfaceCls)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check need generate controller class
     * @param cls class
     * @return boolean
     */
    private boolean isControllerClass(Class cls){
        return
                cls.getAnnotation(org.springframework.stereotype.Controller.class)==null &&
                        cls.getAnnotation(org.springframework.web.bind.annotation.RestController.class)==null &&
                        (
                                cls.getAnnotation(Controller.class) != null ||
                                        cls.getAnnotation(RestController.class) != null ||
                                        cls.getAnnotation(AutoController.class)!=null
                        );
    }

}
