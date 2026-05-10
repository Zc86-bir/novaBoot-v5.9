package org.jeecg.modules.system.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.service.CoinGeckoService;
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
 * 报表中心 - 加密货币行情 API
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@RestController
@RequestMapping("/test/crypto")
@RequiredArgsConstructor
public class CryptoReportController {

    private final JdbcTemplate jdbcTemplate;
    private final CoinGeckoService coinGeckoService;

    /**
     * 获取加密货币行情数据
     */
    @GetMapping("/list")
    public Result<List<CryptoVO>> list() {
        String sql = "SELECT symbol, name, price, price_btc as priceBtc, " +
                "change_24h as change24h, change_pct_24h as changePct24h, " +
                "change_7d as change7d, high_24h as high24h, low_24h as low24h, " +
                "volume_24h as volume24h, market_cap as marketCap, market_cap_rank as marketCapRank, " +
                "circulating_supply as circulatingSupply, total_supply as totalSupply, " +
                "max_supply as maxSupply, trade_date as tradeDate " +
                "FROM crypto_weekly ORDER BY market_cap_rank ASC";

        List<CryptoVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(CryptoVO.class));
        return Result.OK(list);
    }

    /**
     * 手动同步加密货币数据
     */
    @PostMapping("/sync")
    public Result<String> sync() {
        log.info("手动触发加密货币数据同步");
        try {
            coinGeckoService.syncToDatabase();
            return Result.OK("数据同步成功");
        } catch (Exception e) {
            log.error("数据同步失败: {}", e.getMessage(), e);
            return Result.error("数据同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据（直接从API，不经过数据库）
     */
    @GetMapping("/realtime")
    public Result<List<Map<String, Object>>> realtime() {
        List<Map<String, Object>> data = coinGeckoService.fetchRealTimeData();
        return Result.OK(data);
    }

    /**
     * 加密货币数据 VO
     */
    @Data
    public static class CryptoVO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal priceBtc;
        private BigDecimal change24h;
        private BigDecimal changePct24h;
        private BigDecimal change7d;
        private BigDecimal high24h;
        private BigDecimal low24h;
        private BigDecimal volume24h;
        private BigDecimal marketCap;
        private Integer marketCapRank;
        private BigDecimal circulatingSupply;
        private BigDecimal totalSupply;
        private BigDecimal maxSupply;
        private String tradeDate;
    }
}