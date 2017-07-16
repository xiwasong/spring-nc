package service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by xw2sy on 2017-07-16.
 */
@Controller
@RequestMapping("/")
public class OneController {

    @RequestMapping("/test")
    @ResponseBody
    public String test(@RequestParam("msg") String msg){
        return "manual "+msg;
    }

    @Autowired
    private HelloService service;

}
