package com.example.camelpractice;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class RestApiRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // REST API 설정
        restConfiguration()
                .component("servlet")
                .host("localhost")
                .port(8080);

        // GET /camel/api/hello 엔드포인트 (JSON 바인딩 없음)
        rest("/api")
                .get("/hello")
                .to("direct:hello");

        // POST /camel/api/message 엔드포인트 (JSON 바인딩 있음)
        rest("/api")
                .post("/message")
                .bindingMode(RestBindingMode.off)  // 개별적으로 바인딩 끄기
                .to("direct:processMessage");

        // 실제 처리 로직
        from("direct:hello")
                .log("GET /api/hello 요청 받음")
                .setBody(constant("{\"message\": \"안녕하세요! Apache Camel입니다.\"}"))
                .setHeader("Content-Type", constant("application/json"));

        from("direct:processMessage")
                .log("POST /api/message 요청 받음: ${body}")
                .process(exchange -> {
                    String inputMessage = exchange.getIn().getBody(String.class);
                    if (inputMessage == null || inputMessage.trim().isEmpty()) {
                        inputMessage = "빈 메시지";
                    }

                    // JSON 응답 생성
                    String response = String.format(
                            "{\"received\": \"%s\", \"processed\": \"%s\", \"timestamp\": \"%s\"}",
                            inputMessage.replace("\"", "\\\""),
                            inputMessage.toUpperCase().replace("\"", "\\\""),
                            java.time.LocalDateTime.now()
                    );

                    exchange.getIn().setBody(response);
                })
                .setHeader("Content-Type", constant("application/json"));
    }
}