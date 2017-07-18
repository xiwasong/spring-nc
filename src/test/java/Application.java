import cn.hn.java.summer.springnc.AutoControllerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xw2sy on 2017-07-16.
 */

@SpringBootApplication(scanBasePackages = "service")
//extends AutoControllerConfiguration or
//@ImportAutoConfiguration(AutoControllerConfiguration.class)
public class Application extends AutoControllerConfiguration{

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
