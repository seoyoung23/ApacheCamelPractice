package com.example.camelpractice.testFile;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class FileDebugRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // 디버깅 전용 파일 테스트 라우트
        rest("/debug")
                .post("/file-test")
                .to("direct:debugFileTest");

        from("direct:debugFileTest")
                .routeId("file-debug-route")
                .log("=== 파일 디버깅 시작 ===")
                .process(exchange -> {
                    String body = exchange.getIn().getBody(String.class);
                    System.out.println("📝 받은 데이터: " + body);

                    // 1. 권한 확인
                    String testPath = "C:/camel-temp";
                    System.out.println("🔍 테스트 경로: " + testPath);

                    // 2. 디렉토리 생성 가능한지 직접 확인
                    try {
                        File testDir = new File(testPath);
                        if (!testDir.exists()) {
                            boolean created = testDir.mkdirs();
                            System.out.println("📁 디렉토리 생성 시도: " + created);
                            if (created) {
                                System.out.println("✅ 디렉토리 생성 성공: " + testPath);
                            } else {
                                System.out.println("❌ 디렉토리 생성 실패: " + testPath);
                            }
                        } else {
                            System.out.println("📁 디렉토리 이미 존재: " + testPath);
                        }

                        // 3. 쓰기 권한 확인
                        System.out.println("✍️ 쓰기 권한: " + testDir.canWrite());
                        System.out.println("📖 읽기 권한: " + testDir.canRead());

                    } catch (Exception e) {
                        System.err.println("❌ 디렉토리 처리 중 오류: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // 4. Camel 헤더 설정
                    exchange.getIn().setHeader("CamelFileName", "debug-test-" + System.currentTimeMillis() + ".txt");
                    exchange.getIn().setBody("디버그 테스트 내용: " + body + "\n생성 시간: " + java.time.LocalDateTime.now());
                })
                .log("🚀 Camel File Component로 전송 중...")
                .to("file:C:/camel-temp?autoCreate=true")
                .log("✅ Camel File Component 전송 완료")
                .process(exchange -> {
                    // 5. 실제 파일 생성 확인
                    File dir = new File("C:/camel-temp");
                    if (dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles();
                        System.out.println("📋 생성된 파일 수: " + (files != null ? files.length : 0));
                        if (files != null && files.length > 0) {
                            for (File file : files) {
                                System.out.println("📄 파일: " + file.getName() + " (크기: " + file.length() + " bytes)");
                            }
                        }
                    } else {
                        System.out.println("❌ 디렉토리가 여전히 존재하지 않음");
                    }
                })
                .setBody(constant("파일 디버깅 완료! 로그를 확인하세요."));
    }
}