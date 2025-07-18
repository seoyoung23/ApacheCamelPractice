package com.example.camelpractice.routeTemplate;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component("routeTemplateDefinition") // Bean 이름 명시적 지정
public class RouteTemplateDefinition extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        System.out.println("=== 라우트 템플릿 정의 시작 ===");

        // 📌 템플릿 1: 로그 찍기 템플릿
        routeTemplate("log-template")
                .templateParameter("sourceUri")
                .templateParameter("logName")
                .from("{{sourceUri}}")
                .log("템플릿 로그: ${body}")
                .to("log:{{logName}}");
        System.out.println("✅ log-template 정의 완료");

        // 📌 템플릿 2: 파일 저장 템플릿 (디렉토리 생성 포함)
        routeTemplate("file-save-template")
                .templateParameter("inputUri")
                .templateParameter("outputDir")
                .from("{{inputUri}}")
                .log("템플릿 파일 저장: ${body}")
                .process(exchange -> {
                    // 템플릿 파라미터에서 디렉토리 경로 가져오기
                    String outputPath = exchange.getProperty("outputDir", String.class);
                    if (outputPath == null) {
                        outputPath = "C:/camel-temp"; // 기본값
                    }

                    // Java 코드로 디렉토리 생성
                    java.io.File targetDir = new java.io.File(outputPath);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        System.out.println("📁 템플릿 디렉토리 생성: " + outputPath + " → " + (created ? "성공" : "실패"));
                    } else {
                        System.out.println("📁 템플릿 디렉토리 이미 존재: " + outputPath);
                    }

                    // 파일명 설정
                    String fileName = "template-" + System.currentTimeMillis() + ".txt";
                    exchange.getIn().setHeader("CamelFileName", fileName);

                    System.out.println("📄 템플릿 저장 파일: " + outputPath + "/" + fileName);
                })
                .to("file:{{outputDir}}");
        System.out.println("✅ file-save-template 정의 완료");

        // 📌 템플릿 3: HTTP 호출 템플릿
        routeTemplate("http-call-template")
                .templateParameter("triggerUri")
                .templateParameter("targetUrl")
                .from("{{triggerUri}}")
                .log("HTTP 호출: ${body}")
                .setHeader("Content-Type", constant("application/json"))
                .to("http://{{targetUrl}}");
        System.out.println("✅ http-call-template 정의 완료");

        System.out.println("=== 모든 라우트 템플릿 정의 완료 ===");
    }
}