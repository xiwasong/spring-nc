# spring-nc
spring-nc=spring no controller, a spring mvc extension,  auto generate controller from interface .

github:https://github.com/xiwasong/spring-nc   
oschina:http://git.oschina.net/xiwa/spring-nc  

# useage:

### 1. declare an interface:

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

### 2. create a service implements the interface:

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

### 3. run application:

```java
package cn.hn.java.summer.springnctest;

import cn.hn.java.summer.springnc.AutoControllerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xw2sy on 2017-07-16.
 */

@SpringBootApplication(scanBasePackages = "cn.hn.java.summer.springnctest.service")
public class Application {

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
2017-07-17 16:40:59.234  INFO 10368 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/imFour]}" onto public java.lang.String service.IAutoMappingController.four()
2017-07-17 16:40:59.235  INFO 10368 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/three]}" onto public java.lang.String service.IAutoMappingController.three(java.lang.String)
2017-07-17 16:40:59.235  INFO 10368 --- [           main] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped "{[/one]}" onto public java.lang.String service.IAutoMappingController.one()
```