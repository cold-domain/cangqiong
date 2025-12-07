package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Mytask {

    //每5秒钟执行
    //@Scheduled(cron = "0/5 * * * * *")
    public void task(){
        log.info("定时任务执行了");
    }
}
