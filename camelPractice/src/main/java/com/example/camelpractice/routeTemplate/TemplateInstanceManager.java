package com.example.camelpractice.routeTemplate;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TemplateInstanceManager {

    @Autowired
    private CamelContext camelContext;

    @EventListener(ApplicationReadyEvent.class)
    public void createTemplateInstances() {
        System.out.println("=== 템플릿 인스턴스 생성 시작 ===");

        try {
            // 잠시 대기
            Thread.sleep(2000);

            // 기존 기본 라우트들을 제거하고 템플릿 기반 라우트로 교체
            replaceWithTemplateRoutes();

            System.out.println("=== 템플릿 인스턴스 생성 완료 ===");

        } catch (Exception e) {
            System.err.println("❌ 템플릿 생성 실패, 기본 라우트 유지: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceWithTemplateRoutes() throws Exception {

        try {
            // 1단계: 기존 기본 라우트들 중지 및 제거
            removeDefaultRoutes();

            // 2단계: 템플릿 방식으로 새 라우트 생성
            if (tryTemplateApproach()) {
                System.out.println("✅ 템플릿 방식으로 라우트 교체 성공");
            } else {
                // 3단계: 템플릿 실패 시 개선된 직접 라우트 생성
                createImprovedDirectRoutes();
                System.out.println("✅ 개선된 직접 라우트로 교체 성공");
            }

        } catch (Exception e) {
            System.err.println("라우트 교체 실패: " + e.getMessage());
            throw e;
        }
    }

    private void removeDefaultRoutes() throws Exception {
        // 기본 라우트들을 찾아서 제거
        String[] defaultRouteIds = {"log-template-default", "file-template-default", "http-template-default"};

        for (String routeId : defaultRouteIds) {
            try {
                Route route = camelContext.getRoute(routeId);
                if (route != null) {
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                    System.out.println("🗑️ 기본 라우트 제거: " + routeId);
                }
            } catch (Exception e) {
                System.out.println("⚠️ 라우트 제거 실패 (무시): " + routeId + " - " + e.getMessage());
            }
        }
    }

    private boolean tryTemplateApproach() {
        try {
            System.out.println("🔧 템플릿 방식 시도 중...");

            // 로그 템플릿 인스턴스 생성
            TemplatedRouteBuilder.builder(camelContext, "log-template")
                    .routeId("log-template-instance")
                    .parameter("sourceUri", "direct:logTemplate")
                    .parameter("logName", "MyLogTemplate")
                    .add();
            System.out.println("✅ 로그 템플릿 인스턴스 생성");

            // 파일 템플릿 인스턴스 생성
            TemplatedRouteBuilder.builder(camelContext, "file-save-template")
                    .routeId("file-template-instance")
                    .parameter("inputUri", "direct:fileTemplate")
                    .parameter("outputDir", "C:/camel-temp")
                    .add();
            System.out.println("✅ 파일 템플릿 인스턴스 생성");

            // HTTP 템플릿 인스턴스 생성
            TemplatedRouteBuilder.builder(camelContext, "http-call-template")
                    .routeId("http-template-instance")
                    .parameter("triggerUri", "direct:httpTemplate")
                    .parameter("targetUrl", "httpbin.org/post")
                    .add();
            System.out.println("✅ HTTP 템플릿 인스턴스 생성");

            return true;

        } catch (Exception e) {
            System.err.println("템플릿 방식 실패: " + e.getMessage());
            return false;
        }
    }

    private void createImprovedDirectRoutes() throws Exception {
        System.out.println("🔧 개선된 직접 라우트 생성 중...");

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                // 로그 라우트 (개선된 버전)
                from("direct:logTemplate")
                        .routeId("log-template-improved")
                        .log("개선된 템플릿 로그: ${body}")
                        .process(exchange -> {
                            // 추가 로직 가능
                            String body = exchange.getIn().getBody(String.class);
                            System.out.println("🔍 로그 처리 완료: " + body);
                        })
                        .to("log:MyLogTemplate?level=INFO&showHeaders=true");

                // 파일 저장 라우트 (개선된 버전)
                from("direct:fileTemplate")
                        .routeId("file-template-improved")
                        .log("개선된 파일 저장: ${body}")
                        .process(exchange -> {
                            // 현재 작업 디렉토리 출력
                            String currentDir = System.getProperty("user.dir");
                            System.out.println("🔍 현재 작업 디렉토리: " + currentDir);
                            System.out.println("🔍 파일 저장 예정 경로: C:/temp/camel-test");

                            String content = exchange.getIn().getBody(String.class);
                            String enhanced = "=== Camel File Processing ===\n" +
                                    "Timestamp: " + java.time.LocalDateTime.now() + "\n" +
                                    "Content: " + content + "\n" +
                                    "=== End ===";
                            exchange.getIn().setBody(enhanced);
                        })
                        .setHeader("CamelFileName", simple("improved-${date:now:yyyyMMdd-HHmmss}.txt"))
                        .to("file:C:/temp/camel-test?autoCreate=true")
                        .log("✅ 파일 저장 완료!");

                // HTTP 호출 라우트 (개선된 버전)
                from("direct:httpTemplate")
                        .routeId("http-template-improved")
                        .log("개선된 HTTP 호출: ${body}")
                        .setHeader("Content-Type", constant("application/json"))
                        .setHeader("User-Agent", constant("Apache-Camel-Improved/3.18.8"))
                        .setHeader("X-Custom-Header", constant("CamelTemplate"))
                        .to("http://httpbin.org/post");
            }
        });
    }

    // 디버깅용 메서드 - 안전한 버전
    public void printCurrentRoutes() {
        System.out.println("=== 현재 활성 라우트 ===");
        try {
            for (Route route : camelContext.getRoutes()) {
                System.out.println("Route: " + route.getId() +
                        " | Endpoint: " + route.getEndpoint().getEndpointUri());
            }
            System.out.println("총 라우트 수: " + camelContext.getRoutes().size());
        } catch (Exception e) {
            System.err.println("라우트 정보 출력 중 오류: " + e.getMessage());
        }
    }
}