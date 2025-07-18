package com.example.camelpractice;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FilterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // 로그 메시지 필터링 API
        rest("/api/")
            .post("/log")
            .to("direct:logFilter");

        // 로그 필터링 메인 라우팅
        from("direct:logFilter")
            .convertBodyTo(String.class)
            .log("Log message received: ${body}")
            .process(exchange -> {
                String logMessage = exchange.getIn().getBody(String.class);
                if (logMessage == null) {
                    logMessage = "Log message is null";
                }

                // 로그 레벨 결정
                String logLevel = "INFO";   // 기본값
                String upperMessage = logMessage.toUpperCase();

                if (upperMessage.contains("ERROR") || upperMessage.contains("FATAL")) {
                    logLevel = "ERROR";
                } else if (upperMessage.contains("WARN") || upperMessage.contains("WARNING")) {
                    logLevel = "WARN";
                } else if (upperMessage.contains("DEBUG")) {
                    logLevel = "DEBUG";
                }

                exchange.getIn().setHeader("logLevel", logLevel);
            })
            .log("Log level determined: ${header.logLevel}")
            .filter(header("logLevel").in("ERROR", "WARN"))     // ★핵심★ FILTER 사용!
            .log("IMPORTANT LOG DETECTED! Processing...")
            .to("direct:processImportantLog");

        // 중요한 로그 처리
        from("direct:processImportantLog")
            .log("Processing important log...")
            .process(exchange -> {
                String logMessage = exchange.getIn().getBody(String.class);
                String logLevel = exchange.getIn().getHeader("logLevel", String.class);

                String response = String.format(
                        "{\"alert\": true, \"level\": \"%s\". \"action\": \"ADMIN_NOTIFICATION_SENT\", \"timestamp\": \"%s\"}",
                        logLevel,
                        logMessage.replace("\"", "'"),
                        java.time.LocalDateTime.now()
                );

                exchange.getIn().setBody(response);
            })
            .setHeader("Content-Type", constant("application/json; charset=UTF-8"))
            .log("Alert sent: ${body}");

        // 권한 체크 API
        rest("/api")
            .post("/admin/action")
            .to("direct:adminFilter");

        // 관리자 권한 필터
        from("direct:adminFilter")
            .log("Admin action requested: ${body}")
            .convertBodyTo(String.class)
            .process(exchange -> {
                String requestBody = exchange.getIn().getBody(String.class);

                // 간단한 권한 체크 (실제로는 JWT 토큰 등을 사용)
                String userRole = "USER";   // 기본값
                if (requestBody.contains("admin_key=secret123")) {
                    userRole = "ADMIN";
                }

                exchange.getIn().setHeader("userRole", userRole);
            })
            .log("User role : ${header.userRole}")
            // 관리자만 통과하는 필터
            .filter(header("userRole").isEqualTo("ADMIN"))
            .log("Admin access granted.")
            .to("direct:adminAction");

        // 관리자 액션 처리
        from("direct:adminAction")
            .process(exchange -> {
                String action = exchange.getIn().getBody(String.class);
                String response = String.format(
                    "{\"status\": \"SUCCESS\", \"message\": \"Admin action executed\", \"action\": \"%s\", \"timestamp\": \"%s\"}",
                    action.replace("\"", "'"),
                    java.time.LocalDateTime.now()
                );
                exchange.getIn().setBody(response);
            })
            .setHeader("Content-Type", constant("application/json; charset=UTF-8"));
    }
}
