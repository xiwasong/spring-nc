package service;

import org.springframework.stereotype.Service;

/**
 * Created by xw2sy on 2017-07-17.
 */
@Service
public class AutoMappingSerivce implements IAutoMapping {

    public String one() {
        return "one";
    }

    public String two() {
        return "two";
    }

    public String three(String msg) {
        return "three "+msg;
    }

    public String four() {
        return "four";
    }
}
