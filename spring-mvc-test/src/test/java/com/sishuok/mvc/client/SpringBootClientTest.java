package com.sishuok.mvc.client;

import com.sishuok.mvc.controller.UserRestController;
import com.sishuok.mvc.entity.User;
import com.sishuok.mvc.service.UserService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * <p>User: Zhang Kaitao
 * <p>Date: 13-12-29
 * <p>Version: 1.0
 * 通过SpringApplication.run启动一个服务器，
 * 然后Config.xml是Spring的Java配置方式，
 * 此处只加载了UserRestController及其依赖UserService，
 * 对于UserService可以通过如Mockito进行模拟/也可以注入真实的实现，
 * Mockito请参考《单元测试系列之2：模拟利器Mockito》。
 * 可以通过EmbeddedServletContainerFactory子类指定使用哪个内嵌的web容器（目前支持：jetty/tomcat）。
 这种方式的优点就是速度比内嵌Jetty容器速度快，
 但是还是不够快且还需要启动一个服务器（开一个端口），
 因此Spring 3.2提供了模拟Server的方式进行测试。
 即服务器是通过Mock技术模拟的而不是真的启动一个服务器。
 上述两种方式对于如果服务还不存在的情况也是无法测试的，因此Mock Server进行测试时最好的选择。

 */
public class SpringBootClientTest extends AbstractClientTest {

    private static ApplicationContext ctx;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ctx = SpringApplication.run(Config.class); //启动服务器 加载Config指定的组件
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SpringApplication.exit(ctx);//退出服务器
    }


    @Test
    public void testFindById() throws Exception {
        ResponseEntity<User> entity = restTemplate.getForEntity(baseUri + "/{id}", User.class, 1L);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertThat(entity.getHeaders().getContentType().toString(), containsString(MediaType.APPLICATION_JSON_VALUE));
        assertThat(entity.getBody(), hasProperty("name", is("zhang")));
    }


    @Test
    public void testSaveWithJson() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("zhang");

        String uri = baseUri;
        String createdLocation = baseUri + "/" + 1;

        restTemplate.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter()));
        ResponseEntity<User> responseEntity = restTemplate.postForEntity(uri, user, User.class);

        assertEquals(createdLocation, responseEntity.getHeaders().get("Location").get(0));
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(user, responseEntity.getBody());
    }


    @Test
    public void testSaveWithXML() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("zhang");

        String uri = baseUri;
        String createdLocation = baseUri + "/" + 1;

        restTemplate.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(new Jaxb2RootElementHttpMessageConverter()));
        ResponseEntity<User> responseEntity = restTemplate.postForEntity(uri, user, User.class);

        assertEquals(createdLocation, responseEntity.getHeaders().get("Location").get(0));
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        assertEquals(user, responseEntity.getBody());
    }

    @Test
    public void testUpdate() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("zhang");

        String uri = baseUri + "/{id}";

        restTemplate.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter()));
        ResponseEntity responseEntity = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(user), (Class) null, user.getId());

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    public void testDelete() throws Exception {
        String uri = baseUri + "/{id}";
        Long id = 1L;

        ResponseEntity responseEntity = restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity.EMPTY, (Class) null, id);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }


    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public EmbeddedServletContainerFactory servletContainer() {
            return new JettyEmbeddedServletContainerFactory();
        }

        @Bean
        public UserRestController userController() {
            return new UserRestController(userService());
        }

        @Bean
        public UserService userService() {
            //Mockito请参考 http://stamen.iteye.com/blog/1470066
            UserService userService = Mockito.mock(UserService.class);
            User user = new User();
            user.setId(1L);
            user.setName("zhang");
           // OngoingStubbing<User> u=Mockito.when(userService.findById(Mockito.any(Long.class)));
            Mockito.when(userService.findById(Mockito.any(Long.class))).thenReturn(user);
            return userService;
//            return new UserServiceImpl(); //此处也可以返回真实的UserService实现
        }
    }

}
