import cn.hn.java.summer.springnc.AutoControllerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by xw2sy on 2017-07-16.
 */

@SpringBootApplication(scanBasePackages = "service")
public class Application {

    public static void main(String[] args) {
        AutoControllerApplication.run(Application.class);
        SpringApplication.run(Application.class, args);
    }
}
