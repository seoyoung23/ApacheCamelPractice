package com.example.camelpractice;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class FileProcessRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // input 폴더의 파일을 읽어서 내용을 변환한 후 output 폴더로 복사
        from("file:input?noop=true")
                .log("파일 처리 시작: ${header.CamelFileName}")
                .process(exchange -> {
                    String originalContent = exchange.getIn().getBody(String.class);
                    String modifiedContent = "=== 처리된 파일 ===\n" + originalContent + "\n=== 처리 완료 ===";
                    exchange.getIn().setBody(modifiedContent);
                })
                .log("파일 처리 완료: ${header.CamelFileName}")
                .to("file:output");
    }
}