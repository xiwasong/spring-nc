package service;

import bean.User;
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
