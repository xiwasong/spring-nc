package cn.hn.java.summer.springnc;

import cn.hn.java.summer.springnc.annotation.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * generate controller by service interface
 * Created by xw2sy on 2017-07-15.
 */
public class ControllerGenerator {

    private static final Log logger= LogFactory.getLog(ControllerGenerator.class);

    private static final String AUTO_CONTROLLER_NAME="Controller";

    private static final String PREFIX_FILE = "file:/";

    private static final Map<String,String> ANNOTATION_NAME_MAPPING=new HashMap<String, String>();

    static {
        ANNOTATION_NAME_MAPPING.put("Controller", "org.springframework.stereotype.Controller");
        ANNOTATION_NAME_MAPPING.put("RestController", "org.springframework.web.bind.annotation.RestController");
        ANNOTATION_NAME_MAPPING.put("RequestMapping", "org.springframework.web.bind.annotation.RequestMapping");
        ANNOTATION_NAME_MAPPING.put("DeleteMapping", "org.springframework.web.bind.annotation.DeleteMapping");
        ANNOTATION_NAME_MAPPING.put("GetMapping", "org.springframework.web.bind.annotation.GetMapping");
        ANNOTATION_NAME_MAPPING.put("PatchMapping", "org.springframework.web.bind.annotation.PatchMapping");
        ANNOTATION_NAME_MAPPING.put("PostMapping", "org.springframework.web.bind.annotation.PostMapping");
        ANNOTATION_NAME_MAPPING.put("PutMapping", "org.springframework.web.bind.annotation.PutMapping");
    }

    private Class interfaceCls;
    private Class serviceCls;
    private ClassPool classPool;
    private ConstPool constPool;
    private CtClass controllerCtCls;

    public ControllerGenerator(Class interfaceCls, Class serviceCls){
        this.interfaceCls=interfaceCls;
        this.serviceCls=serviceCls;
    }

    /**
     * generate controller by service interface
     * @return controller class
     */
    public Class create(){
        //get package from service interface class
        String controllerPackage=interfaceCls.getPackage().getName();
        if(logger.isDebugEnabled()) {
            logger.debug("start to generate controller for:" + interfaceCls.getName());
        }
        return createController(controllerPackage);
    }

    /**
     * check interface controller class exists
     * @param interfaceCls controller's interface
     * @return boolean
     */
    public boolean controllerClassExists(Class interfaceCls){
        String className=interfaceCls.getSimpleName()+AUTO_CONTROLLER_NAME;
        String controllerPackage=interfaceCls.getPackage().getName();
        String clsFullName=controllerPackage +"."+ className;
        try {
            return Class.forName(clsFullName)!=null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * generate controller by service interface
     * @param controllerPackage set generated controller's package
     * @return controller class
     */
    private Class createController(String controllerPackage){
        try {
            ClassPool pool = ClassPool.getDefault();
            String className=interfaceCls.getSimpleName()+AUTO_CONTROLLER_NAME;
            String clsFullName=controllerPackage +"."+ className;
            try {
                return Class.forName(clsFullName);
            }catch (ClassNotFoundException e){
                //need create class
            }

            controllerCtCls = pool.makeClass(clsFullName);
            classPool=controllerCtCls.getClassPool();
            constPool=controllerCtCls.getClassFile().getConstPool();

            //init class's annotations
            initClassAnnotationsAttribute();

            //set interface
            CtClass serviceInterface = pool.get(interfaceCls.getName());
            controllerCtCls.setInterfaces(new CtClass[]{serviceInterface});

            //init autowired service field
            initAutowiredField();

            //add interface implements methods to controller class
            makeInterfaceMethods();

            //save class file to running directory
            if(logger.isDebugEnabled()) {
                saveClassFile(controllerPackage, className);
            }

            //load class
            Class cls= controllerCtCls.toClass(Thread.currentThread().getContextClassLoader(),null);
            if(logger.isInfoEnabled()){
                logger.debug(cls.getName()+" class generated!");
            }

            return cls;
        }catch (Exception e){
            logger.error("generate controller failed!!!!",e);
            return null;
        }
    }

    /**
     * init target class annotation attributes
     */
    private void initClassAnnotationsAttribute(){
        ClassFile classFile= controllerCtCls.getClassFile();

        //use raw annotations make AnnotationsAttribute
        java.lang.annotation.Annotation[] annotations= interfaceCls.getAnnotations();
        AnnotationsAttribute attribute= makeAnnotationsAttribute(annotations);

        //use auto mapping controller
        if(interfaceCls.getAnnotation(AutoController.class)!=null){
            AnnotationInfo annInfo=new AnnotationInfo(RestController.class);
            Annotation annotation= makeAnnotation(annInfo);
            attribute.addAnnotation(annotation);

            //add default request mapping
            if(interfaceCls.getAnnotation(RequestMapping.class)==null){
                annInfo=new AnnotationInfo(RequestMapping.class);
                annInfo.addValue("value",new String[]{"/"+interfaceCls.getSimpleName()});
                annotation= makeAnnotation(annInfo);
                attribute.addAnnotation(annotation);
            }
        }

        classFile.addAttribute(attribute);
    }

    /**
     * init autowired service field
     */
    private void initAutowiredField() throws CannotCompileException {
        //add service field
        CtField serviceField =CtField.make("private "+serviceCls.getName()+" service;",controllerCtCls);
        //create autowired annotation
        ConstPool constPool= controllerCtCls.getClassFile().getConstPool();
        AnnotationsAttribute annAttr=new AnnotationsAttribute(constPool,AnnotationsAttribute.visibleTag);
        Annotation autowired=new Annotation(Autowired.class.getName(),constPool);
        annAttr.addAnnotation(autowired);
        serviceField.getFieldInfo().addAttribute(annAttr);
        controllerCtCls.addField(serviceField);
    }

    /**
     * make an AnnotationsAttribute
     * @param annotations annotations
     * @return AnnotationsAttribute
     */
    private AnnotationsAttribute makeAnnotationsAttribute(java.lang.annotation.Annotation[] annotations){
        //create Annotations Attribute instance
        AnnotationsAttribute annAttr=new AnnotationsAttribute(constPool,AnnotationsAttribute.visibleTag);
        //fetch all annotations ,add to Attribute
        for(java.lang.annotation.Annotation ann : annotations){
            Annotation annotation=makeAnnotation(ann);
            //addMemberValue first then addAnnotation
            annAttr.addAnnotation(annotation);
        }

        return annAttr;
    }

    /**
     * make bytecode annotation by raw annotation
     * @param ann raw annotation
     * @return bytecode annotation
     */
    private Annotation makeAnnotation(java.lang.annotation.Annotation ann){
        //get annotation's all field names and values
        Map<String,Object> memberValues=getAnnotationValues(ann);
        //annotation info
        AnnotationInfo annInfo=new AnnotationInfo();
        annInfo.setAnnotationClass(ann.annotationType());
        annInfo.setValues(memberValues);

        //use annotation info make bytecode annotation
        return makeAnnotation(annInfo);

    }

    /**
     * make bytecode annotation by raw annotation
     * @param annInfo an annotation info
     * @return bytecode annotation
     */
    private Annotation makeAnnotation(AnnotationInfo annInfo){
        String simpleTypeName=annInfo.getName();
        //use default mapped name
        String typeName=ANNOTATION_NAME_MAPPING.get(simpleTypeName);
        if(typeName==null){
            typeName=annInfo.getAnnotationClass().getName();
        }
        Annotation annotation=new Annotation(typeName,constPool);
        if(annInfo.getValues()!=null) {
            Set<String> keys = annInfo.getValues().keySet();
            for (String key : keys) {
                Object value = annInfo.getValues().get(key);
                //use value make a MemberValue
                MemberValue memberValue = makeMemberValue(value);
                annotation.addMemberValue(key, memberValue);
            }
        }
        return annotation;
    }

    /**
     * use value make a MemberValue
     * @param value source value object
     * @return MemberValue
     */
    private MemberValue makeMemberValue(Object value){
        MemberValue memberValue=null;
        if(value instanceof String){
            memberValue=new StringMemberValue(value.toString(),constPool);
        }else if(value instanceof Boolean){
            memberValue=new BooleanMemberValue((Boolean)value, constPool);
        }else if(value instanceof Enum){
            //for enum type
            EnumMemberValue enumMemberValue=new EnumMemberValue(constPool);
            enumMemberValue.setType(((Enum)value).getClass().getName());
            enumMemberValue.setValue(((Enum)value).name());
            memberValue=enumMemberValue;
        }else if(value.getClass().isArray()){
            int len=Array.getLength(value);
            ArrayMemberValue arrayMemberValue=new ArrayMemberValue(constPool);
            MemberValue[] memberValues=new MemberValue[len];
            for(int i=0;i<len;i++){
                memberValues[i]=makeMemberValue(Array.get(value,i));
            }
            arrayMemberValue.setValue(memberValues);
            memberValue=arrayMemberValue;
        }
        return memberValue;
    }

    /**
     * add interface implements methods to target class
     */
    private void  makeInterfaceMethods(){
        Method[] methods= interfaceCls.getDeclaredMethods();
        for(Method mtd: methods){
            Class[] paraTypes= mtd.getParameterTypes();
            CtClass[] ctClasses=new CtClass[paraTypes.length];
            try {
                for(int i=0;i<paraTypes.length;i++){
                    ctClasses[i]=classPool.get(paraTypes[i].getName());
                }
                CtMethod ctMethod = new CtMethod(classPool.get(mtd.getReturnType().getName()),mtd.getName(),ctClasses,controllerCtCls);
                ctMethod.setBody("return this.service." + mtd.getName() + "($$);");
                //auto mapping tag
                boolean autoMapping=interfaceCls.getAnnotation(AutoController.class)!=null;
                //init method annotations
                initMethodAnnotationsAttribute(mtd,ctMethod,autoMapping);

                controllerCtCls.addMethod(ctMethod);
            }catch (Exception e){
                logger.error("create interface method "+mtd.getName()+" failed!!",e);
            }
        }
    }

    /**
     * get method parameters's names
     * @param method rwa method
     * @return parameter names
     */
    private String[] getMethodParameterNames(Method method){
        String[] paramNames=new String[0];
        try {
            //must use service class's implements method
            method=serviceCls.getDeclaredMethod(method.getName(),method.getParameterTypes());

            CtClass ctClass= classPool.getCtClass(serviceCls.getName());
            Class[] parClasses= method.getParameterTypes();
            //get parameter's ct class
            CtClass[] ctParaClz=new CtClass[parClasses.length];
            for(int i=0;i<parClasses.length;i++){
                ctParaClz[i]=classPool.getCtClass(parClasses[i].getName());
            }
            //get ct method
            CtMethod ctMethod= ctClass.getDeclaredMethod(method.getName(),ctParaClz);
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            //get Variable info
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (attr == null) {
                return new String[]{};
            }
            paramNames = new String[method.getParameterTypes().length];
            int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = attr.variableName(i + pos);
            }
        }catch (Exception e){
            //ignore
        }
        return paramNames;
    }


    /**
     * init target class method's annotation attributes
     * @param  method raw method
     * @param  ctMethod target method
     * @param autoMapping auto mapping method as an action
     */
    private void initMethodAnnotationsAttribute(Method method, CtMethod ctMethod, boolean autoMapping){
        //make method's raw annotations
        java.lang.annotation.Annotation[] annotations= method.getAnnotations();
        AnnotationsAttribute attribute= makeAnnotationsAttribute(annotations);

        //need auto mapping
        if(autoMapping && (method.getAnnotation(ExcludeMapping.class)==null && method.getAnnotation(RequestMapping.class)==null && method.getAnnotation(GetMapping.class)==null && method.getAnnotation(PostMapping.class)==null && method.getAnnotation(DeleteMapping.class)==null && method.getAnnotation(PutMapping.class)==null && method.getAnnotation(PatchMapping.class)==null )){
            AnnotationInfo annInfo=new AnnotationInfo(org.springframework.web.bind.annotation.RequestMapping.class);
            //use method name as mapping path
            annInfo.addValue("value",new String[]{"/"+method.getName()});
            Annotation annotation= makeAnnotation(annInfo);
            attribute.addAnnotation(annotation);
        }

        //add annotation attribute
        ctMethod.getMethodInfo().addAttribute(attribute);

        //init Method Parameter's Annotations
        initMethodParameterAnnotations(method,ctMethod);
    }

    /**
     * init Method Parameter's Annotations
     * @param method Method
     * @param ctMethod CtMethod
     */
    private void initMethodParameterAnnotations(Method method, CtMethod ctMethod){
        //get method parameters's variable names
        String[] paramNames= getMethodParameterNames(method);
        Class[] paramTypes= method.getParameterTypes();
        java.lang.annotation.Annotation[][] allParameterAnnotations= method.getParameterAnnotations();

        Annotation[][] allAnnotations=new Annotation[paramTypes.length][];
        for(int i=0; i<paramTypes.length; i++){
            //one parameter's annotations
            List<Annotation> paraAnnList=new ArrayList<Annotation>();
            java.lang.annotation.Annotation[] parameterAnnotations=allParameterAnnotations[i];
            //if parameter's type is primitive or is in java.lang package,need add RequestParam annotations
            if(
                (paramTypes[i].isPrimitive() || paramTypes[i].getName().startsWith("java.lang."))
            ){
                boolean noRequestParam=true;
                //Check that the parameter contains @RequestParam or @PathVariable
                for(java.lang.annotation.Annotation annotation :parameterAnnotations){
                    if(
                        annotation.annotationType()==RequestParam.class ||
                        annotation.annotationType()==PathVariable.class
                    ){
                        noRequestParam=false;
                        break;
                    }
                }
                if(noRequestParam) {
                    //create Annotations Attribute instance
                    Annotation ann = new Annotation(RequestParam.class.getName(), constPool);
                    ann.addMemberValue("value", new StringMemberValue(paramNames[i], constPool));
                    ann.addMemberValue("required", new BooleanMemberValue(false, constPool));
                    paraAnnList.add(ann);
                }
            }
            //fetch exits raw annotations, generate parameter's bytecode annotations
            for(java.lang.annotation.Annotation ann : parameterAnnotations){
                paraAnnList.add(makeAnnotation(ann));
            }
            allAnnotations[i]=paraAnnList.toArray(new Annotation[]{});
        }

        //add ParameterAnnotationsAttribute to methodInfo
        ParameterAnnotationsAttribute parameterAnnotationsAttribute = new ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag);
        parameterAnnotationsAttribute.setAnnotations(allAnnotations);
        ctMethod.getMethodInfo().addAttribute(parameterAnnotationsAttribute);
    }

    /**
     * get AnnotationValues
     * @param ann annotations
     * @return AnnotationValues
     */
    private Map<String,Object> getAnnotationValues(java.lang.annotation.Annotation ann){
        Map<String,Object> memberValues=new HashMap<String, Object>();
        Method[] methods= ann.annotationType().getDeclaredMethods();
        for(Method mtd : methods){
            mtd.setAccessible(true);
            try {
                memberValues.put(mtd.getName(),mtd.invoke(ann));
            } catch (IllegalAccessException e) {
                //ignore
            } catch (InvocationTargetException e) {
                //ignore
            }
        }
        return memberValues;
    }


    /**
     * save class bytecode to file (directory or jar)
     * @param controllerPackage controller package
     * @param className controller class name
     */
    private void saveClassFile(String controllerPackage, String className){
        URL rootRes=ControllerGenerator.class.getClassLoader().getResource("");
        if(rootRes==null){
            //try again
            rootRes=serviceCls.getResource("");
        }
        if(rootRes!=null) {
            if("jar".equals(rootRes.getProtocol()) || "war".equals(rootRes.getProtocol())){
                //unnecessary
                //write into jar file
                //try {
                //    writeClassIntoJarFile(rootRes.getPath(), controllerPackage, className, controllerCls.toBytecode());
                //}catch (Exception e){
                //    //ignore
                //}
            }else {
                try {
                    controllerCtCls.writeFile(rootRes.getPath());
                } catch (Exception e) {
                    //ignore
                    if (logger.isDebugEnabled()) {
                        logger.error("write class file error!", e);
                    }
                }
            }
        }
    }

    /**
     * write Class byte codes Into SpringBoot Jar file
     * @param sourceJarFile jar file
     * @param classPackage class package
     * @param className class name
     * @param codeBytes class byte codes
     */
    public void writeClassIntoJarFile(String sourceJarFile,String classPackage,String className, byte[] codeBytes){
        //String jarClassPath="file://xxx/springnc-test/target/springnc-test-1.0-SNAPSHOT.jar!/BOOT-INF/classes!/";
        String[] jarPaths=sourceJarFile.split("!/");
        //eg:/BOOT-INF/classes or class package path
        String baseClassDir=jarPaths[1];
        //trim end /
        if(baseClassDir.lastIndexOf("/")==baseClassDir.length()-1){
            baseClassDir=baseClassDir.substring(0,baseClassDir.length()-1);
        }
        String jarFilePath=jarPaths[0];
        if(jarFilePath.startsWith(PREFIX_FILE)){
            jarFilePath=jarFilePath.substring(PREFIX_FILE.length());
        }
        File jarFile=new File(jarFilePath);
        if(!jarFile.exists()){
            logger.error(sourceJarFile+" is not exists!!!!!");
            return;
        }
        try {
            String targetName = baseClassDir;
            String packagePath=classPackage.replace(".", "/");
            if(!baseClassDir.equals(packagePath)){
                targetName=baseClassDir + "/" + packagePath;
            }
            JarFile orignJar = new JarFile(jarFile);
            Enumeration<JarEntry> entryEnumeration = orignJar.entries();
            BufferedInputStream bufferedInputStream;
            Map<String, byte[]> jarBytes = new HashMap<String, byte[]>();
            while (entryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = entryEnumeration.nextElement();
                bufferedInputStream = new BufferedInputStream(orignJar.getInputStream(jarEntry));
                int len = bufferedInputStream.available();
                byte[] bytes = new byte[len];
                bufferedInputStream.read(bytes);
                bufferedInputStream.close();
                jarBytes.put(jarEntry.getName(), bytes);
            }
            orignJar.close();

            JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
            for (String key : jarBytes.keySet()) {
                jarBytes.get(key);
                JarEntry jarEntry = new JarEntry(key);
                jos.putNextEntry(jarEntry);
                jos.write(jarBytes.get(key));
            }
            jos.putNextEntry(new JarEntry(targetName +"/"+ className + ".class"));
            jos.write(codeBytes);
            jos.close();
        }catch (Exception e){
            logger.error("write class bytes into jar file error!!!!",e);
        }
    }
}
