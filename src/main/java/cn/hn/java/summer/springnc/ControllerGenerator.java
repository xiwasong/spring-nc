package cn.hn.java.summer.springnc;

import cn.hn.java.summer.springnc.annotation.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
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
import java.lang.reflect.Parameter;
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

    //private static final Log logger= LogFactory.getLog(ControllerGenerator.class);

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

    /**
     * generate controller by service interface
     * @param interfaceCls interface class
     * @param serviceCls service class which implements from interfaceCls
     * @return controller class
     */
    public static Class createController(Class interfaceCls, Class serviceCls){
        //get package from service interface class
        String controllerPackage=interfaceCls.getPackage().getName();
        //if(logger.isDebugEnabled()) {
        //    logger.debug("start to generate controller for:" + interfaceCls.getName());
        //}
        System.out.print("start to generate controller for:" + interfaceCls.getName());
        return createController(controllerPackage,interfaceCls,serviceCls);
    }

    /**
     * check interface controller class exists
     * @param interfaceCls controller's interface
     * @return boolean
     */
    public static boolean controllerClassExists(Class interfaceCls){
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
     * @param interfaceCls interface class
     * @param serviceCls service class which implements from interfaceCls
     * @return controller class
     */
    private static Class createController(String controllerPackage, Class interfaceCls, Class serviceCls){
        try {
            ClassPool pool = ClassPool.getDefault();
            String className=interfaceCls.getSimpleName()+AUTO_CONTROLLER_NAME;
            String clsFullName=controllerPackage +"."+ className;
            try {
                return Class.forName(clsFullName);
            }catch (ClassNotFoundException e){
                //need create class
            }

            CtClass controllerCls = pool.makeClass(clsFullName);
            //init class's annotations
            initClassAnnotationsAttribute(controllerCls,interfaceCls);

            //set interface
            CtClass serviceInterface = pool.get(interfaceCls.getName());
            controllerCls.setInterfaces(new CtClass[]{serviceInterface});

            //init autowired service field
            initAutowiredField(controllerCls,serviceCls);

            //add interface implements methods to controller class
            makeInterfaceMethods(interfaceCls,serviceCls,controllerCls);

            //save class file to running directory
            saveClassFile(controllerCls,serviceCls,controllerPackage,className);

            //load class
            Class cls= controllerCls.toClass(Thread.currentThread().getContextClassLoader(),null);
            //if(logger.isDebugEnabled()){
            //    logger.debug(cls.getName()+" been generated!");
            //}

            return cls;
        }catch (Exception e){
            //logger.error("generate controller failed!!!!",e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * save class bytecode to file (directory or jar)
     * @param controllerCls controller ct class
     * @param serviceCls service class
     * @param controllerPackage controller package
     * @param className controller class name
     */
    private static void saveClassFile(CtClass controllerCls, Class serviceCls, String controllerPackage, String className){
        URL rootRes=AutoControllerApplication.class.getClassLoader().getResource("");
        if(rootRes==null){
            //try again
            rootRes=serviceCls.getResource("");
        }
        if(rootRes!=null) {
            if("jar".equals(rootRes.getProtocol())){
                //write into jar file
                try {
                    writeClassIntoJarFile(rootRes.getPath(), controllerPackage, className, controllerCls.toBytecode());
                }catch (Exception e){
                    //ignore
                }
            }else {
                try {
                    controllerCls.writeFile(rootRes.getPath());
                } catch (Exception e) {
                    //ignore
                    //if (logger.isDebugEnabled()) {
                    //    logger.error("write class file error!", e);
                    //}
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
    public static void writeClassIntoJarFile(String sourceJarFile,String classPackage,String className, byte[] codeBytes){
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
            //logger.error(sourceJarFile+" is not exists!!!!!");
            System.out.print(sourceJarFile+" is not exists!!!!!");
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
            //logger.error("write class bytes into jar file error!!!!",e);
            e.printStackTrace();
        }
    }

    /**
     * init target class annotation attributes
     * @param targetCls target class
     * @param interfaceCls target class's interface
     */
    private static void initClassAnnotationsAttribute(CtClass targetCls, Class interfaceCls){
        ClassFile classFile= targetCls.getClassFile();
        ConstPool pool=classFile.getConstPool();

        //use raw annotations make AnnotationsAttribute
        java.lang.annotation.Annotation[] annotations= interfaceCls.getAnnotations();
        AnnotationsAttribute attribute= makeAnnotationsAttribute(pool,annotations);

        //use auto mapping controller
        if(interfaceCls.getAnnotation(AutoController.class)!=null){
            AnnotationInfo annInfo=new AnnotationInfo(RestController.class);
            Annotation annotation= makeAnnotation(pool,annInfo);
            attribute.addAnnotation(annotation);
        }

        classFile.addAttribute(attribute);
    }

    /**
     * init autowired service field
     * @param controllerCls controller ct class
     * @param serviceCls service class
     */
    private static void initAutowiredField(CtClass controllerCls, Class serviceCls) throws CannotCompileException {
        //add service field
        CtField serviceField =CtField.make("private "+serviceCls.getName()+" service;",controllerCls);
        //create autowired annotation
        ConstPool constPool= controllerCls.getClassFile().getConstPool();
        AnnotationsAttribute annAttr=new AnnotationsAttribute(constPool,AnnotationsAttribute.visibleTag);
        Annotation autowired=new Annotation(Autowired.class.getName(),constPool);
        annAttr.addAnnotation(autowired);
        serviceField.getFieldInfo().addAttribute(annAttr);
        controllerCls.addField(serviceField);
    }

    /**
     * make an AnnotationsAttribute
     * @param pool ConstPool
     * @param annotations annotations
     * @return AnnotationsAttribute
     */
    private static AnnotationsAttribute makeAnnotationsAttribute(ConstPool pool, java.lang.annotation.Annotation[] annotations){
        //create Annotations Attribute instance
        AnnotationsAttribute annAttr=new AnnotationsAttribute(pool,AnnotationsAttribute.visibleTag);
        //fetch all annotations ,add to Attribute
        for(java.lang.annotation.Annotation ann : annotations){
            Annotation annotation=makeAnnotation(pool,ann);
            //addMemberValue first then addAnnotation
            annAttr.addAnnotation(annotation);
        }

        return annAttr;
    }

    /**
     * make bytecode annotation by raw annotation
     * @param pool ConstPool
     * @param ann raw annotation
     * @return bytecode annotation
     */
    private static Annotation makeAnnotation(ConstPool pool,java.lang.annotation.Annotation ann){
        //get annotation's all field names and values
        Map<String,Object> memberValues=getAnnotationValues(ann);
        //annotation info
        AnnotationInfo annInfo=new AnnotationInfo();
        annInfo.setAnnotationClass(ann.annotationType());
        annInfo.setValues(memberValues);

        //use annotation info make bytecode annotation
        return makeAnnotation(pool,annInfo);

    }

    /**
     * make bytecode annotation by raw annotation
     * @param pool ConstPool
     * @param annInfo an annotation info
     * @return bytecode annotation
     */
    private static Annotation makeAnnotation(ConstPool pool,AnnotationInfo annInfo){
        String simpleTypeName=annInfo.getName();
        //use default mapped name
        String typeName=ANNOTATION_NAME_MAPPING.get(simpleTypeName);
        if(typeName==null){
            typeName=annInfo.getAnnotationClass().getName();
        }
        Annotation annotation=new Annotation(typeName,pool);
        if(annInfo.getValues()!=null) {
            Set<String> keys = annInfo.getValues().keySet();
            for (String key : keys) {
                Object value = annInfo.getValues().get(key);
                //use value make a MemberValue
                MemberValue memberValue = makeMemberValue(value, pool);
                annotation.addMemberValue(key, memberValue);
            }
        }
        return annotation;
    }

    /**
     * use value make a MemberValue
     * @param value source value object
     * @param pool const pool
     * @return MemberValue
     */
    private static MemberValue makeMemberValue(Object value,ConstPool pool){
        MemberValue memberValue=null;
        if(value instanceof String){
            memberValue=new StringMemberValue(value.toString(),pool);
        }else if(value instanceof Boolean){
            memberValue=new BooleanMemberValue((Boolean)value, pool);
        }else if(value.getClass().isArray()){
            int len=Array.getLength(value);
            ArrayMemberValue arrayMemberValue=new ArrayMemberValue(pool);
            MemberValue[] memberValues=new MemberValue[len];
            for(int i=0;i<len;i++){
                memberValues[i]=makeMemberValue(Array.get(value,i),pool);
            }
            arrayMemberValue.setValue(memberValues);
            memberValue=arrayMemberValue;
        }
        return memberValue;
    }

    /**
     * add interface implements methods to target class
     * @param interfaceCls interface calss
     * @param targetCls target ct class
     */
    private static void  makeInterfaceMethods(Class interfaceCls, Class serviceCls, CtClass targetCls){
        ClassPool pool = ClassPool.getDefault();
        Method[] methods= interfaceCls.getDeclaredMethods();
        for(Method mtd: methods){
            Class[] paraTypes= mtd.getParameterTypes();
            CtClass[] ctClasses=new CtClass[paraTypes.length];
            try {
                for(int i=0;i<paraTypes.length;i++){
                    ctClasses[i]=pool.get(paraTypes[i].getName());
                }
                CtMethod ctMethod = new CtMethod(pool.get(mtd.getReturnType().getName()),mtd.getName(),ctClasses,targetCls);
                ctMethod.setBody("return this.service." + mtd.getName() + "($$);");
                //auto mapping tag
                boolean autoMapping=interfaceCls.getAnnotation(AutoController.class)!=null;
                //init method annotations
                initMethodAnnotationsAttribute(targetCls,serviceCls,mtd,ctMethod,autoMapping);

                targetCls.addMethod(ctMethod);
            }catch (Exception e){
                //logger.error("create interface method "+mtd.getName()+" failed!!",e);
                e.printStackTrace();
            }
        }
    }

    /**
     * get method parameters's names
     * @param serviceCls service class
     * @param method rwa method
     * @return parameter names
     */
    private static String[] getMethodParameterNames(Class serviceCls, Method method){
        String[] paramNames=new String[0];
        try {
            ClassPool pool = ClassPool.getDefault();
            method = serviceCls.getDeclaredMethod(method.getName(), method.getParameterTypes());
            CtMethod ctMethod= pool.getMethod(serviceCls.getName(),method.getName());
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
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
     * @param targetCls target class
     * @param  method raw method
     * @param  ctMethod target method
     * @param autoMapping auto mapping method as an action
     */
    private static void initMethodAnnotationsAttribute(CtClass targetCls,Class serviceCls, Method method, CtMethod ctMethod, boolean autoMapping){
        ConstPool pool=targetCls.getClassFile().getConstPool();

        //make method's raw annotations
        java.lang.annotation.Annotation[] annotations= method.getAnnotations();
        AnnotationsAttribute attribute= makeAnnotationsAttribute(pool,annotations);

        //need auto mapping
        if(autoMapping && (method.getAnnotation(ExcludeMapping.class)==null && method.getAnnotation(RequestMapping.class)==null && method.getAnnotation(GetMapping.class)==null && method.getAnnotation(PostMapping.class)==null && method.getAnnotation(DeleteMapping.class)==null && method.getAnnotation(PutMapping.class)==null && method.getAnnotation(PatchMapping.class)==null )){
            AnnotationInfo annInfo=new AnnotationInfo(org.springframework.web.bind.annotation.RequestMapping.class);
            //use method name as mapping path
            annInfo.addValue("value",new String[]{"/"+method.getName()});
            Annotation annotation= makeAnnotation(pool,annInfo);
            attribute.addAnnotation(annotation);
        }

        //add annotation attribute
        ctMethod.getMethodInfo().addAttribute(attribute);

        //init Method Parameter's Annotations
        initMethodParameterAnnotations(pool,serviceCls,method,ctMethod);
    }

    /**
     * init Method Parameter's Annotations
     * @param pool ConstPool
     * @param serviceCls service class
     * @param method Method
     * @param ctMethod CtMethod
     */
    private static void initMethodParameterAnnotations(ConstPool pool,Class serviceCls, Method method, CtMethod ctMethod){
        //get method parameters's variable names
        String[] paramNames= getMethodParameterNames(serviceCls,method);
        //noinspection Since15
        Parameter[] parameters= method.getParameters();
        Annotation[][] allAnnotations=new Annotation[parameters.length][];
        for(int i=0; i<parameters.length; i++){
            //one parameter's annotations
            List<Annotation> paraAnnList=new ArrayList<Annotation>();
            //noinspection Since15
            Parameter para=parameters[i];
            //if parameter's type is primitive or is in java.lang package,need add RequestParam annotations
            //noinspection Since15
            if(
                    (((Class)para.getParameterizedType()).isPrimitive() || para.getParameterizedType().getTypeName().startsWith("java.lang.")) &&
                            // and no RequestParam annotation
                            //noinspection Since15
                            para.getAnnotation(RequestParam.class)==null && para.getAnnotation(PathVariable.class)==null
                    ){
                //create Annotations Attribute instance
                Annotation ann=new Annotation(RequestParam.class.getName(),pool);
                ann.addMemberValue("value",new StringMemberValue(paramNames[i],pool));
                ann.addMemberValue("required",new BooleanMemberValue(false,pool));
                paraAnnList.add(ann);
            }
            //fetch exits raw annotations, generate parameter's bytecode annotations
            //noinspection Since15
            java.lang.annotation.Annotation[] paraRawAnns= para.getAnnotations();
            for(java.lang.annotation.Annotation ann : paraRawAnns){
                paraAnnList.add(makeAnnotation(pool,ann));
            }
            allAnnotations[i]=paraAnnList.toArray(new Annotation[]{});
        }

        //add ParameterAnnotationsAttribute to methodInfo
        ParameterAnnotationsAttribute parameterAnnotationsAttribute = new ParameterAnnotationsAttribute(pool, ParameterAnnotationsAttribute.visibleTag);
        parameterAnnotationsAttribute.setAnnotations(allAnnotations);
        ctMethod.getMethodInfo().addAttribute(parameterAnnotationsAttribute);
    }

    /**
     * get AnnotationValues
     * @param ann annotations
     * @return AnnotationValues
     */
    private static Map<String,Object> getAnnotationValues(java.lang.annotation.Annotation ann){
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
}
