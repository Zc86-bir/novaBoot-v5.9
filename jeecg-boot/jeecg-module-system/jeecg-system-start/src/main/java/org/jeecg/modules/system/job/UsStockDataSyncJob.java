package org.jeecg.modules.system.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.service.UsStockMarketService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * 美股数据同步定时任务
 * 每5分钟生成模拟实时美股数据
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsStockDataSyncJob implements Job {

    private final UsStockMarketService usStockMarketService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行美股数据同步任务...");
        try {
            usStockMarketService.syncToDatabase();
            log.info("美股数据同步任务完成");
        } catch (Exception e) {
            log.error("美股数据同步失败: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
