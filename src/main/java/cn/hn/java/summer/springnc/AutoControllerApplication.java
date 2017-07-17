package cn.hn.java.summer.springnc;

import cn.hn.java.summer.springnc.annotation.AutoController;
import cn.hn.java.summer.springnc.annotation.Controller;
import cn.hn.java.summer.springnc.annotation.RestController;

import java.util.List;

/**
 * Created by xw2sy on 2017-07-16.
 */
public class AutoControllerApplication {

    public static void main(String[] args) throws ClassNotFoundException {
        run(Class.forName(args[0]));
    }

    public static void run(Class source){
        ClassPathScanner scanner=new ClassPathScanner(
            //set default include type filter
            new ClassPathScanner.IncludeTypeFilter() {
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

            },source);

        boolean needExit=false;
        boolean isDirectory=source.getResource(".")!=null;

        List<Class<?>> classList= scanner.scan();
        for(Class cls : classList){
            if(!cls.isInterface()){
                //find out controller interface and implement class
                Class[] interfaces= cls.getInterfaces();
                for(Class clsInterface : interfaces){
                    for(Class controllerCls : classList){
                        //generate controller
                        if(clsInterface==controllerCls){
                            //if not run as a jar and controller exists,don't exit
                            if(!isDirectory && !ControllerGenerator.controllerClassExists(controllerCls)){
                                needExit=true;
                            }
                            ControllerGenerator.createController(controllerCls,cls);
                            break;
                        }
                    }
                }
            }
        }

        if(needExit){
            System.exit(0);
        }
    }

}
