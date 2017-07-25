package cn.hn.java.summer.springnc;

import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by xw2sy on 2017-07-16.
 */
public class ClassPathScanner {

    private static final String PROTOCOL_FILE = "file";
    private static final String PROTOCOL_JAR = "jar";
    private static final String PREFIX_FILE = "file:";
    private static final String JAR_URL_SEPARATOR = "!/";
    private static final String CLASS_FILE = ".class";
    private static final String SPRINGBOOT_ANN_NAME="org.springframework.boot.autoconfigure.SpringBootApplication";
    private String[] packageNames=new String[]{};
    private Class[] sources=null;
    private IncludeTypeFilter includeTypeFilter;

    public ClassPathScanner(IncludeTypeFilter includeTypeFilter){
        this(includeTypeFilter,new String[]{},null);
    }


    public ClassPathScanner(IncludeTypeFilter includeTypeFilter,String[] packageNames){
        this.includeTypeFilter=includeTypeFilter;
        this.packageNames=packageNames;
        initPackageNames();
    }

    public ClassPathScanner(IncludeTypeFilter includeTypeFilter, Class[] sources){
        this.includeTypeFilter=includeTypeFilter;
        this.sources=sources;
        initPackageNames();
    }

    public ClassPathScanner(IncludeTypeFilter includeTypeFilter,String[] packageNames, Class[] sources){
        this.includeTypeFilter=includeTypeFilter;
        this.packageNames=packageNames;
        this.sources=sources;
        initPackageNames();
    }

    /**
     * init packageNames variable
     * get package names from sources class's annotation 'scanBasePackages' field
     */
    private void initPackageNames(){
        Assert.notNull(packageNames,"packageNames cannot be empty!");

        if(sources==null || sources.length==0){
            return;
        }
        List<String> packageNameList=new ArrayList<>();
        for (Class source : sources) {
            packageNameList.addAll(getAnnotationPackageNames(source));
        }
        //add exist package names
        for(String name : packageNames){
            packageNameList.add(name);
        }
        packageNames=packageNameList.toArray(new String[]{});
    }

    /**
     * get scanBasePackages value from source class annotation
     * @param source source class
     * @return package names
     */
    private List<String> getAnnotationPackageNames(Class source){
        List<String> packageNames=new ArrayList<>();
        Annotation[] annotations= source.getAnnotations();
        for(Annotation ann : annotations){
            //is SpringBootApplication annotation
            if(SPRINGBOOT_ANN_NAME.equals(ann.annotationType().getName())){
                try {
                    //get scanBasePackages
                    Method method = ann.getClass().getDeclaredMethod("scanBasePackages");
                    Object annValue = method.invoke(ann);
                    int len= Array.getLength(annValue);
                    for(int i=0;i< len;i++){
                        packageNames.add(Array.get(annValue,i).toString());
                    }
                }catch (Exception e){
                    //ignore
                }
            }
        }
        return packageNames;
    }

    public List<Class<?>> scan(){
        List<Class<?>> classList=new ArrayList<Class<?>>();
        for(String packageName : packageNames){
            classList.addAll(scan(packageName));
        }
        return classList;
    }

    private List<Class<?>> scan(String packageName) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        Enumeration<URL> en = null;
        try {
            en = getClass().getClassLoader().getResources(dotToPath(packageName));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        assert en != null;
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            if (PROTOCOL_FILE.equals(url.getProtocol())) {
                File root = new File(url.getFile());
                findInDirectory(list, root, root, packageName);
            }
            else if (PROTOCOL_JAR.equals(url.getProtocol())) {
                findInJar(list, getJarFile(url), packageName);
            }
        }
        return list;
    }

    private File getJarFile(URL url) {
        String file = url.getFile();
        if (file.startsWith(PREFIX_FILE))
            file = file.substring(PREFIX_FILE.length());
        int end = file.indexOf(JAR_URL_SEPARATOR);
        if (end!=(-1))
            file = file.substring(0, end);
        return new File(file);
    }

    private void findInJar(List<Class<?>> results, File file, String packageName) {
        JarFile jarFile = null;
        String packagePath = dotToPath(packageName) + "/";
        try {
            jarFile = new JarFile(file);
            Enumeration<JarEntry> en = jarFile.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                String name = je.getName();
                if (name.contains(packagePath) && name.endsWith(CLASS_FILE)) {
                    String className = name.substring(name.indexOf(packagePath), name.length() - CLASS_FILE.length());
                    add(results, pathToDot(className));
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            if (jarFile!=null) {
                try {
                    jarFile.close();
                }
                catch(IOException ignored) {}
            }
        }
    }

    private void findInDirectory(List<Class<?>> results, File rootDir, File dir, String packageName) {
        File[] files = dir.listFiles();
        String rootPath = rootDir.getPath();
        assert files != null;
        for (File file : files) {
            if (file.isFile()) {
                String classFileName = file.getPath();
                if (classFileName.endsWith(CLASS_FILE)) {
                    String className = classFileName.substring(rootPath.length() - packageName.length(), classFileName.length() - CLASS_FILE.length());
                    add(results, pathToDot(className));
                }
            }
            else if (file.isDirectory()) {
                findInDirectory(results, rootDir, file, packageName);
            }
        }
    }

    private void add(List<Class<?>> results, String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        }
        catch(ClassNotFoundException e) {
            return;
        }
        if(includeTypeFilter.accept(clazz)) {
            results.add(clazz);
        }
    }

    private String dotToPath(String s) {
        return s.replace('.', '/');
    }

    private String pathToDot(String s) {
        return s.replace('/', '.').replace('\\', '.').replaceAll("^\\.","");
    }

}
