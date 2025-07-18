package com.example.camelpractice.routeTemplate;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TemplateUsageRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        System.out.println("=== REST 및 라우트 설정 시작 ===");

        // REST 설정
        restConfiguration()
                .component("servlet")
                .contextPath("/")  // 루트 경로 설정
                .host("localhost")
                .port(8080);

        // REST 엔드포인트
        rest("/api")
                .post("/log-test")
                .to("direct:logTest")
                .post("/file-test")
                .to("direct:fileTest")
                .post("/http-test")
                .to("direct:httpTest");

        // REST 처리 라우트들
        from("direct:logTest")
                .log("로그 테스트 시작")
                .to("direct:logTemplate");

        from("direct:fileTest")
                .log("파일 테스트 시작")
                .setBody(constant("파일에 저장할 내용: " + System.currentTimeMillis()))
                .to("direct:fileTemplate");

        from("direct:httpTest")
                .log("HTTP 테스트 시작")
                .setBody(constant("{\"message\": \"Hello from template\"}"))
                .to("direct:httpTemplate");

        // 🔥 핵심: 여기서 미리 기본 구현을 제공
        // 나중에 TemplateInstanceManager가 이것들을 덮어쓸 수 있음
        from("direct:logTemplate")
                .routeId("log-template-default")
                .log("기본 로그 처리: ${body}")
                .to("log:DefaultLogTemplate");

        from("direct:fileTemplate")
                .routeId("file-template-default")
                .log("기본 파일 처리: ${body}")
                .process(exchange -> {
                    // Java 코드로 디렉토리 생성
                    String targetPath = "C:/camel-temp";
                    java.io.File targetDir = new java.io.File(targetPath);

                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        System.out.println("📁 디렉토리 생성: " + targetPath + " → " + (created ? "성공" : "실패"));
                    } else {
                        System.out.println("📁 디렉토리 이미 존재: " + targetPath);
                    }

                    // 파일명 설정
                    String fileName = "default-" + System.currentTimeMillis() + ".txt";
                    exchange.getIn().setHeader("CamelFileName", fileName);

                    System.out.println("📄 저장할 파일: " + targetPath + "/" + fileName);
                })
                .to("file:C:/camel-temp");

        from("direct:httpTemplate")
                .routeId("http-template-default")
                .log("기본 HTTP 처리: ${body}")
                .setHeader("Content-Type", constant("application/json"))
                .to("http://httpbin.org/post");

        System.out.println("=== REST 및 기본 라우트 설정 완료 ===");
    }
}