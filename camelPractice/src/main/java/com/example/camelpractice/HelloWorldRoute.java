package com.example.camelpractice;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class HelloWorldRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // 5초마다 "Hello World" 메시지를 생성하고 로그로 출력
        from("timer:hello?period=5000")
                .setBody(constant("Hello World from Apache Camel!"))
                .log("메시지: ${body}")
                .to("log:hello-world");
    }
}