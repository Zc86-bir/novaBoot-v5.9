package org.jeecg.modules.system.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.service.CoinGeckoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * 加密货币数据同步定时任务
 * 每5分钟从CoinGecko API获取最新数据
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoDataSyncJob implements Job {

    private final CoinGeckoService coinGeckoService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行加密货币数据同步任务...");
        try {
            coinGeckoService.syncToDatabase();
            log.info("加密货币数据同步任务完成");
        } catch (Exception e) {
            log.error("加密货币数据同步失败: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
