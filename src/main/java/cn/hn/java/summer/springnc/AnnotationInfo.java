package cn.hn.java.summer.springnc;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xw2sy on 2017-07-17.
 */
public class AnnotationInfo {

    private Class annotationClass;

    private String name;

    private Map<String,Object> values=new HashMap<String,Object>();

    public AnnotationInfo(){}

    public AnnotationInfo(Class annotationClass){
        this.annotationClass=annotationClass;
    }

    public Class getAnnotationClass() {
        return annotationClass;
    }

    public void setAnnotationClass(Class annotationClass) {
        this.annotationClass = annotationClass;
    }

    public String getName() {
        if(name==null){
            name=annotationClass.getSimpleName();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public AnnotationInfo addValue(String name, Object value){
        this.values.put(name,value);
        return this;
    }
}
