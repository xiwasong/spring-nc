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
