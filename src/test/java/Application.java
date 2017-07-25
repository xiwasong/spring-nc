import cn.hn.java.summer.springnc.EnableNoController;
import cn.hn.java.summer.springnc.NoControllerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xw2sy on 2017-07-16.
 */

@SpringBootApplication(scanBasePackages = "service")
@EnableNoController
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
