import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by xw2sy on 2017-07-16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes=Application.class)
public class SpringMvcTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testRequestParameter(){
        Assert.assertEquals("hello world",restTemplate.getForObject("/say?name=world",String.class));
    }

    @Test
    public void testRequestNamedParameter(){
        Assert.assertEquals("name:money age:100",restTemplate.getForObject("/say2?name=money&age=100",String.class));
    }

    @Test
    public void testPathVariable(){
        Assert.assertEquals("info id:123",restTemplate.getForObject("/info/123",String.class));
    }
}
