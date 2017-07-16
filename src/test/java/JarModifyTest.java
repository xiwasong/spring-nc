import cn.hn.java.summer.springnc.ControllerGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Created by xw2sy on 2017-07-16.
 */
public class JarModifyTest {

    @Test
    public void testModifyJar() throws IOException {
        String sourceJar="E:\\git_repo\\my\\springnc-test\\target\\springnc-test-1.0-SNAPSHOT.jar";
        File jarFile=new File(sourceJar);
        JarFile orignJar=new JarFile(jarFile);
        String targetName="BOOT-INF/classes/cn/hn/java/summer/springnctest/service";
        System.out.println(orignJar.getEntry(targetName));
        Enumeration<JarEntry> entryEnumeration= orignJar.entries();
        List<JarEntry> jarEntryList=new ArrayList<JarEntry>();
        BufferedInputStream bufferedInputStream = null;
        Map<String,byte[]> jarBytes=new HashMap<String, byte[]>();
        while (entryEnumeration.hasMoreElements()){
            JarEntry jarEntry= entryEnumeration.nextElement();
            bufferedInputStream = new BufferedInputStream(orignJar.getInputStream(jarEntry));
            int len = bufferedInputStream.available();
            byte[] bytes = new byte[len];
            bufferedInputStream.read(bytes);
            bufferedInputStream.close();
            jarBytes.put(jarEntry.getName(),bytes);
        }
        orignJar.close();
        
        JarOutputStream jos=new JarOutputStream(new FileOutputStream(jarFile));
        for(String key : jarBytes.keySet()){
            jarBytes.get(key);
            JarEntry jarEntry=new JarEntry(key);
            jos.putNextEntry(jarEntry);
            jos.write(jarBytes.get(key));
        }
        jos.putNextEntry(new JarEntry(targetName+"/test.txt"));
        jos.write("123".getBytes());
        jos.close();
    }

    @Test
    public void testJarFile(){
        String jarClassPath="file:/E:/git_repo/my/springnc-test/target/springnc-test-1.0-SNAPSHOT.jar!/BOOT-INF/classes!/";
        String[] jarPaths=jarClassPath.split("!");
        String jarFilePath=jarPaths[0];
        if(jarFilePath.startsWith("file:/")){
            jarFilePath=jarFilePath.substring("file:/".length());
        }
        File jarFile=new File(jarFilePath);
        Assert.assertTrue(jarFile.exists());
    }

    @Test
    public void testWirteClasIntoJarFile() throws IOException {
        //String jarClassPath="file:/E:/git_repo/my/springnc-test/target/springnc-test-1.0-SNAPSHOT.jar!/BOOT-INF/classes!/";
        String jarClassPath="file:/E:/git_repo/my/springnc-test/target/springnc-test-1.0-SNAPSHOT/springnc-test-1.0-SNAPSHOT.jar!/cn/hn/java/summer/springnctest/service/";
        File classFile=new File("E:\\git_repo\\my\\springnc-test\\target\\classes\\cn\\hn\\java\\summer\\springnctest\\service\\IHelloController.class");
        FileInputStream fis =new FileInputStream(classFile);
        byte[] bytes=new byte[fis.available()];
        fis.read(bytes);
        fis.close();
        ControllerGenerator.writeClassIntoJarFile(jarClassPath,"cn.hn.java.summer.springnctest.service","IHelloController",bytes);
    }
}
