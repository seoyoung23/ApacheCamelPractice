package com.example.camelpractice;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ContentBasedRouterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // REST 설정
        restConfiguration()
                .component("servlet")
                .host("localhost")
                .port(8080);

        // REST 엔드포인트: 주문 처리
        rest("/api")
            .post("/order")
            .to("direct:processOrder");

        // 주문 처리 메인 route - 안전한 방법
        from("direct:processOrder")
                .log("주문 접수 시작")
                .convertBodyTo(String.class)
                .log("주문 접수: ${body}")
                .process(exchange -> {
                    String message = exchange.getIn().getBody(String.class);
                    if (message == null) {
                        message = "empty message";
                    }

                    String messageType = "NORMAL";
                    String lowerMessage = message.toLowerCase();

                    if (lowerMessage.contains("vip")) {
                        messageType = "VIP";
                    } else if (lowerMessage.contains("urgent") || lowerMessage.contains("긴급")) {
                        messageType = "URGENT";
                    }

                    exchange.getIn().setHeader("messageType", messageType);
//                    exchange.getIn().setHeader("originalMessage", message);   // 이 부분에서 인코딩 에러 발생
                    exchange.getIn().setBody(message); // body 다시 설정
                })
                .log("메시지 타입: ${header.messageType}")
                .choice()
                .when(header("messageType").isEqualTo("VIP"))
                .log("VIP 고객 주문 처리")
                .to("direct:vipOrder")
                .when(header("messageType").isEqualTo("URGENT"))
                .log("긴급 주문 처리")
                .to("direct:urgentOrder")
                .otherwise()
                .log("일반 주문 처리")
                .to("direct:normalOrder")
                .end();

        // VIP 주문 처리
        from("direct:vipOrder")
                .log("VIP 혜택 적용 중...")
                .process(exchange -> {
                    String originalMessage = exchange.getIn().getBody(String.class);
                    // 안전한 JSON 생성 (따옴표 문제 해결)
                    String safeMessage = originalMessage.replace("\"", "'");
                    String response = "{\"status\": \"VIP 처리 완료\", \"message\": \"" +
                            safeMessage + "\", \"discount\": \"20%\", \"priority\": \"높음\"}";
                    exchange.getIn().setBody(response);
                })
                .setHeader("Content-Type", constant("application/json"));

        // 긴급 주문 처리
        from("direct:urgentOrder")
                .log("긴급 처리 프로세스 시작...")
                .process(exchange -> {
                    String originalMessage = exchange.getIn().getBody(String.class);
                    String safeMessage = originalMessage.replace("\"", "'");
                    String response = "{\"status\": \"긴급 처리 완료\", \"message\": \"" +
                            safeMessage + "\", \"processing_time\": \"1시간 이내\"}";
                    exchange.getIn().setBody(response);
                })
                .setHeader("Content-Type", constant("application/json"));

        // 일반 주문 처리
        from("direct:normalOrder")
                .log("일반 처리 프로세스 시작...")
                .process(exchange -> {
                    String originalMessage = exchange.getIn().getBody(String.class);
                    String safeMessage = originalMessage.replace("\"", "'");
                    String response = "{\"status\": \"일반 처리 완료\", \"message\": \"" +
                            safeMessage + "\", \"processing_time\": \"24시간 이내\"}";
                    exchange.getIn().setBody(response);
                })
                .setHeader("Content-Type", constant("application/json"));
    }
}