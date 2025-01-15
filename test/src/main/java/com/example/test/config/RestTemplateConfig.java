package com.example.test.config;

import org.springframework.aop.framework.ProxyConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {
//    private String PROXY_HOST = "10.207.156.52";
//    private int PROXY_PORT = 3128;

    @Bean
    public RestTemplate restTemplate() {
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
//        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//        requestFactory.setProxy(proxy);
//        return new RestTemplate(requestFactory);
        return new RestTemplate();
    }
}
