package service;


import bean.User;
import cn.hn.java.summer.springnc.annotation.GetMapping;
import cn.hn.java.summer.springnc.annotation.RequestMapping;
import cn.hn.java.summer.springnc.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @GetMapping("/say1")
    String say(String msg);

    @RequestMapping(value="/say2",method = RequestMethod.GET)
    String say(String name, int age);

    @GetMapping("/info/{id}")
    String info(@PathVariable("id") String id);
}
