package cn.hn.java.summer.springnc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xw2sy on 2017-07-18.
 */
@Configuration
public class NoControllerConfig implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    /**
     * When you need to manually create NoControllerConfig,
     * you can specify the scanned package through this property,
     * such as through the XML configuration file.
     *  Multiple package names are separated by ','.
     */
    private String scanBasePackages="";

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    public void setScanBasePackages(String scanBasePackages) {
        this.scanBasePackages = scanBasePackages;
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        List<Class> sourceClz=new ArrayList<>();

        if(registry instanceof DefaultListableBeanFactory) {
            Class enableAutoConfiguration=null;
            try {
                enableAutoConfiguration=Class.forName("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
            } catch (ClassNotFoundException e) {
                //ignore
            }
            //find @EnableAutoConfiguration or @SpringBootApplication annotated bean classes
            if(enableAutoConfiguration!=null){
                Map<String, Object> applicationCls = ((DefaultListableBeanFactory) registry).getBeansWithAnnotation(enableAutoConfiguration);
                for(Object bootObj : applicationCls.values()){
                    sourceClz.add(bootObj.getClass());
                }
            }
        }

        //add self class in use 'extend NoControllerConfig' case
        sourceClz.add(getClass());
        //trim spaces
        scanBasePackages=scanBasePackages.replaceAll("\\s+","");

        ClassPathScanner scanner=new ClassPathScanner(
                //set default include type filter
                new IncludeControllerFilter(),
                scanBasePackages.split(","),
                sourceClz.toArray(new Class[]{})
        );

        List<Class<?>> classList= scanner.scan();
        for(Class cls : classList){
            if(!cls.isInterface()){
                //find out controller interface and implement class
                Class[] interfaces= cls.getInterfaces();
                for(Class clsInterface : interfaces){
                    for(Class controllerCls : classList){
                        //generate controller
                        if(clsInterface==controllerCls){
                            //create
                            Class beanClass=new ControllerGenerator(controllerCls,cls).create();
                            //register
                            registerBean(registry, beanClass);
                            break;
                        }
                    }
                }
            }
        }

    }

    private void registerBean(BeanDefinitionRegistry registry, Class<?> beanClass){
        AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(beanClass);
        ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(beanDefinition);
        beanDefinition.setScope(scopeMetadata.getScopeName());
        String beanName =this.beanNameGenerator.generateBeanName(beanDefinition, registry);
        AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDefinition);
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

}