package cn.hn.java.summer.springnc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.List;

/**
 * Created by xw2sy on 2017-07-18.
 */
@Configuration
public class AutoControllerConfiguration implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        ClassPathScanner scanner=new ClassPathScanner(
                //set default include type filter
                new IncludeControllerFilter(), getClass()
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