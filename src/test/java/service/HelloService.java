package service;

import bean.User;
import org.springframework.stereotype.Service;

/**
 * Created by xw2sy on 2017-07-15.
 */
@Service
public class HelloService implements IHello {

    public String say(User user) {
        System.out.println(user.getName());
        return "hello "+user.getName();
    }

    public String say(String msg) {
        return "hello "+msg;
    }

    public String say(String name, int age) {
        return "name:"+name+" age:"+age;
    }

    public String say2(String name) {
        return "name:"+name;
    }

    public String info(String id) {
        return "info id:"+id;
    }
}
