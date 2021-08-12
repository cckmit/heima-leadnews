package com.heima.jobs.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class XxlJobConfig {

    @Value("${xxljob.admin.addresses}")
    private String adminAddresses;

    @Value("${xxljob.executor.appname}")
    private String appName;

    @Value("${xxljob.executor.port}")
    private int port;
    @Value("${xxljob.executor.logPath}")
    private String logPath;
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("正在调用xxljob组件进行分布式任务调度→");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appName);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogRetentionDays(30);
        xxlJobSpringExecutor.setLogPath(logPath);
        log.info("xxljob组件调用正常。。。返回xxljob执行器→ {}",xxlJobSpringExecutor.toString());
        return xxlJobSpringExecutor;
    }
}
