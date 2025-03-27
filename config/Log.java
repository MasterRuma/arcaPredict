package com.application.arcagambling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Log {

    public void method() {

        log.info("info 레벨 로그");
        log.warn("warn 레벨 로그");
        log.error("error 레벨 로그");

    }

}
