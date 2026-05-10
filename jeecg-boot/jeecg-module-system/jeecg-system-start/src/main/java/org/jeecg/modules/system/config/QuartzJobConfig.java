package org.jeecg.modules.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.job.CryptoDataSyncJob;
import org.jeecg.modules.system.job.UsStockDataSyncJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz定时任务配置
 * 配置加密货币和美股数据同步任务
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzJobConfig {

    /**
     * 加密货币数据同步任务
     */
    @Bean
    public JobDetail cryptoDataSyncJobDetail() {
        return JobBuilder.newJob(CryptoDataSyncJob.class)
                .withIdentity("cryptoDataSyncJob", "DEFAULT")
                .storeDurably()
                .build();
    }

    /**
     * 每5分钟触发一次
     */
    @Bean
    public Trigger cryptoDataSyncTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 */5 * * * ?")
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .forJob(cryptoDataSyncJobDetail())
                .withIdentity("cryptoDataSyncTrigger", "DEFAULT")
                .withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * 美股数据同步任务
     */
    @Bean
    public JobDetail usStockDataSyncJobDetail() {
        return JobBuilder.newJob(UsStockDataSyncJob.class)
                .withIdentity("usStockDataSyncJob", "DEFAULT")
                .storeDurably()
                .build();
    }

    /**
     * 每5分钟触发一次
     */
    @Bean
    public Trigger usStockDataSyncTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 */5 * * * ?")
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .forJob(usStockDataSyncJobDetail())
                .withIdentity("usStockDataSyncTrigger", "DEFAULT")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
