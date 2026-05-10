package org.jeecg.modules.system.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.service.UsStockMarketService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 报表中心 - 美股行情 API
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/test/usstock")
@RequiredArgsConstructor
public class UsStockReportController {

    private final JdbcTemplate jdbcTemplate;
    private final UsStockMarketService usStockMarketService;

    /**
     * 获取美股行情数据
     */
    @GetMapping("/list")
    public Result<List<UsStockVO>> list() {
        String sql = "SELECT symbol, name, sector, close_price as closePrice, " +
                "prev_close as prevClose, `change`, change_pct as changePct, " +
                "open_price as openPrice, high_price as highPrice, low_price as lowPrice, " +
                "volume, market_cap as marketCap, trade_date as tradeDate " +
                "FROM us_stock_weekly ORDER BY change_pct DESC";

        List<UsStockVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UsStockVO.class));
        return Result.OK(list);
    }

    /**
     * 手动同步美股数据
     */
    @PostMapping("/sync")
    public Result<String> sync() {
        log.info("手动触发美股数据同步");
        try {
            usStockMarketService.syncToDatabase();
            return Result.OK("数据同步成功");
        } catch (Exception e) {
            log.error("数据同步失败: {}", e.getMessage(), e);
            return Result.error("数据同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据（直接生成，不经过数据库）
     */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> realtime() {
        List<Map<String, Object>> data = usStockMarketService.fetchRealTimeData();
        return Result.OK(data);
    }

    /**
     * 美股数据 VO
     */
    @Data
    public static class UsStockVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String symbol;
        private String name;
        private String sector;
        private BigDecimal closePrice;
        private BigDecimal prevClose;
        private BigDecimal change;
        private BigDecimal changePct;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private Long volume;
        private String marketCap;
        private String tradeDate;
    }
}
