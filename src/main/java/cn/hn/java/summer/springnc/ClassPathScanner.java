package cn.hn.java.summer.springnc;

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
    private static final String JAR_URL_SEPERATOR = "!/";
    private static final String CLASS_FILE = ".class";
    private static final String SPRINGBOOT_ANN_NAME="org.springframework.boot.autoconfigure.SpringBootApplication";
    private String[] packageNames=null;
    private Class source=null;
    private IncludeTypeFilter includeTypeFilter;

    public ClassPathScanner(IncludeTypeFilter includeTypeFilter){
        this(includeTypeFilter,null);
    }

    public ClassPathScanner(IncludeTypeFilter includeTypeFilter, Class source){
        this.includeTypeFilter=includeTypeFilter;
        this.source=source;
        initPackageNames();
    }

    private void initPackageNames(){
        if(source==null){
            packageNames=new String[]{""};
            return;
        }
        Annotation[] annotations= source.getAnnotations();
        for(Annotation ann : annotations){
            //is SpringBootApplication annotation
            if(SPRINGBOOT_ANN_NAME.equals(ann.annotationType().getName())){
                try {
                    //get scanBasePackages
                    Method method = ann.getClass().getDeclaredMethod("scanBasePackages");
                    Object annValue = method.invoke(ann);
                    int len= Array.getLength(annValue);
                    packageNames=new String[len];
                    for(int i=0;i< len;i++){
                        packageNames[i]=Array.get(annValue,i).toString();
                    }
                }catch (Exception e){
                    //ignore
                }
            }
        }
        if(packageNames==null || packageNames.length==0){
            packageNames=new String[]{""};
        }
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
        int end = file.indexOf(JAR_URL_SEPERATOR);
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

    public interface IncludeTypeFilter{
        boolean accept(Class cls);
    }
}
