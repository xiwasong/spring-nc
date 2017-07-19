# spring-nc
spring-nc=spring no controller, a spring mvc extension,  auto generate controller from service interface .

spring-nc 是一个spring mvc的扩展，它通过service的接口生成对应的controller类，并注入对应接口的service实现类将controller请求映射到实现类的方法，并作为bean注册到spring容器中。   
而自动生成的controller和普通的controller别无二致，仅需要在接口类上应用@Controller、@RequestMapping等注解，减少了开发工作量让开发更便捷，同时它完全与普通的controller并存。   
此外spring-nc还提供了自动映射功能，只需在接口上应用@AutoController注解，你便可以通过"/接口名/方法名"直接访问service方法。

github:https://github.com/xiwasong/spring-nc   
oschina:http://git.oschina.net/xiwa/spring-nc  
qq群:233391281  

# useage:
使用方法：

### 1. declare an interface:
定义service接口，并在接口上按以往的方式应用Controller相关注解。

```java
package cn.hn.java.summer.springnctest.service;


import cn.hn.java.summer.springnc.annotation.GetMapping;
import cn.hn.java.summer.springnc.annotation.RequestMapping;
import cn.hn.java.summer.springnc.annotation.RestController;
import cn.hn.java.summer.springnctest.bean.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by xw2sy on 2017-07-15.
 */
@RestController
@RequestMapping("/")
public interface IHello {

    @ResponseBody
    @RequestMapping("/say")
    String say(User user);

    @ResponseBody
    @RequestMapping("/say2")
    String say2(String name, int age);

    @GetMapping("/info/{id}")
    String info(@PathVariable("id") String id);
}

```
注意：除了@Controller、@RestController、@RequestMapping、@PutMapping、@PostMapping、@PatchMapping、@GetMapping和@DeleteMapping这8个注解需要用spring-nc中的注解代替，其它任何注解都可用原有的。

### 2. create a service implements the interface:
创建service接口实现类，无需特别处理。

```java
package cn.hn.java.summer.springnctest.service;

import cn.hn.java.summer.springnctest.bean.User;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Created by xw2sy on 2017-07-15.
 */
@Service
public class HelloService implements IHello {

    public String say(User user) {
        System.out.println(user.getName());
        return "hello "+user.getName();
    }

    public String say2(String name, int age) {
        return "name:"+name+" age:"+age;
    }

    public String say2(String name) {
        return "name:"+name;
    }

    public String info(@PathVariable("id") String id) {
        return "info id:"+id;
    }
}

```

### 3. apply AutoControllerConfiguration on application:
在应用程序启动类上应用AutoControllerConfiguration配置，使用@ImportAutoConfiguration(AutoControllerConfiguration.class)或直接继承AutoControllerConfiguration。

```java
package cn.hn.java.summer.springnctest;

import cn.hn.java.summer.springnc.AutoControllerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xw2sy on 2017-07-16.
 */

@SpringBootApplication(scanBasePackages = "cn.hn.java.summer.springnctest.service")
//@ImportAutoConfiguration(AutoControllerConfiguration.class)
// or extends AutoControllerConfiguration
public class Application extends AutoControllerConfiguration{

    public static void main(String[] args) {
        AutoControllerApplication.run(Application.class);
        SpringApplication.run(Application.class, args);
    }
}

```


### 4. get /say?name=world:
output: hello world

### 5. example: [https://github.com/xiwasong/springnc-test](https://github.com/xiwasong/springnc-test)

# auto mapping support: 
自动映射支持，如下在接口上应用@AutoController注解，对应的接口方法将会被映射到"/IAutoMapping/方法名"上。  
如果有不想要被自动映射的方法，可使用@ExcludeMapping注解将它排除。  
当然你也可以同时混用@RequestMapping等映射注解。

```java
package service;

import cn.hn.java.summer.springnc.annotation.AutoController;
import cn.hn.java.summer.springnc.annotation.ExcludeMapping;
import cn.hn.java.summer.springnc.annotation.RequestMapping;

/**
 * Created by xw2sy on 2017-07-17.
 */
@AutoController
public interface IAutoMapping {

    //mapped to "/one"
    String one();

    @ExcludeMapping
    String two();

    //mapped to "/three"
    String three(String msg);

    @RequestMapping("/imFour")
    String four();
}

```
mapping result:
```java
2017-07-19 02:39:43.311  INFO 6296 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/IAutoMapping/one]}" onto public java.lang.String service.IAutoMappingController.one()
2017-07-19 02:39:43.311  INFO 6296 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/IAutoMapping/imFour]}" onto public java.lang.String service.IAutoMappingController.four()
2017-07-19 02:39:43.311  INFO 6296 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/IAutoMapping/three]}" onto public java.lang.String service.IAutoMappingController.three(java.lang.String)
```
