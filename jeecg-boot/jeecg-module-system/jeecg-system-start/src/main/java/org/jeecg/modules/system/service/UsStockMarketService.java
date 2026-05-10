package org.jeecg.modules.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 美股市场数据服务
 * 模拟实时美股市场数据更新
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@Service
public class UsStockMarketService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 美股代码列表 (80只主流股票)
    private static final List<String> STOCK_SYMBOLS = Arrays.asList(
        "AAPL", "MSFT", "GOOGL", "META", "NVDA", "TSLA", "AMD", "INTC", "QCOM", "CRM",
        "ORCL", "ADBE", "IBM", "NOW", "SNOW", "JPM", "BAC", "WFC", "GS", "MS",
        "BLK", "SCHW", "C", "USB", "PNC", "AXP", "V", "JNJ", "UNH", "PFE",
        "MRK", "ABT", "TMO", "AMGN", "BMY", "GILD", "REGN", "AMZN", "KO", "PEP",
        "MCD", "SBUX", "NKE", "DIS", "COST", "WMT", "TGT", "BA", "CAT", "GE",
        "HON", "UPS", "FDX", "RTX", "LMT", "NOC", "MMM", "XOM", "CVX", "COP",
        "SLB", "EOG", "PSX", "VLO", "OXY", "VZ", "T", "TMUS", "CMCSA", "CHTR",
        "BRK-B", "SPGI", "MA", "CME", "ICE", "NDAQ", "PLTR", "COIN", "SHOP", "UBER"
    );

    // 行业映射
    private static final Map<String, String> SECTOR_MAP = new HashMap<>();
    static {
        SECTOR_MAP.put("AAPL", "Technology"); SECTOR_MAP.put("MSFT", "Technology");
        SECTOR_MAP.put("GOOGL", "Technology"); SECTOR_MAP.put("META", "Technology");
        SECTOR_MAP.put("NVDA", "Technology"); SECTOR_MAP.put("TSLA", "Technology");
        SECTOR_MAP.put("AMD", "Technology"); SECTOR_MAP.put("INTC", "Technology");
        SECTOR_MAP.put("QCOM", "Technology"); SECTOR_MAP.put("CRM", "Technology");
        SECTOR_MAP.put("ORCL", "Technology"); SECTOR_MAP.put("ADBE", "Technology");
        SECTOR_MAP.put("IBM", "Technology"); SECTOR_MAP.put("NOW", "Technology");
        SECTOR_MAP.put("SNOW", "Technology"); SECTOR_MAP.put("PLTR", "Technology");
        SECTOR_MAP.put("SHOP", "Technology"); SECTOR_MAP.put("UBER", "Technology");

        SECTOR_MAP.put("JPM", "Finance"); SECTOR_MAP.put("BAC", "Finance");
        SECTOR_MAP.put("WFC", "Finance"); SECTOR_MAP.put("GS", "Finance");
        SECTOR_MAP.put("MS", "Finance"); SECTOR_MAP.put("BLK", "Finance");
        SECTOR_MAP.put("SCHW", "Finance"); SECTOR_MAP.put("C", "Finance");
        SECTOR_MAP.put("USB", "Finance"); SECTOR_MAP.put("PNC", "Finance");
        SECTOR_MAP.put("AXP", "Finance"); SECTOR_MAP.put("V", "Finance");
        SECTOR_MAP.put("BRK-B", "Finance"); SECTOR_MAP.put("SPGI", "Finance");
        SECTOR_MAP.put("MA", "Finance"); SECTOR_MAP.put("CME", "Finance");
        SECTOR_MAP.put("ICE", "Finance"); SECTOR_MAP.put("NDAQ", "Finance");
        SECTOR_MAP.put("COIN", "Finance");

        SECTOR_MAP.put("JNJ", "Healthcare"); SECTOR_MAP.put("UNH", "Healthcare");
        SECTOR_MAP.put("PFE", "Healthcare"); SECTOR_MAP.put("MRK", "Healthcare");
        SECTOR_MAP.put("ABT", "Healthcare"); SECTOR_MAP.put("TMO", "Healthcare");
        SECTOR_MAP.put("AMGN", "Healthcare"); SECTOR_MAP.put("BMY", "Healthcare");
        SECTOR_MAP.put("GILD", "Healthcare"); SECTOR_MAP.put("REGN", "Healthcare");

        SECTOR_MAP.put("AMZN", "Consumer"); SECTOR_MAP.put("KO", "Consumer");
        SECTOR_MAP.put("PEP", "Consumer"); SECTOR_MAP.put("MCD", "Consumer");
        SECTOR_MAP.put("SBUX", "Consumer"); SECTOR_MAP.put("NKE", "Consumer");
        SECTOR_MAP.put("DIS", "Consumer"); SECTOR_MAP.put("COST", "Consumer");
        SECTOR_MAP.put("WMT", "Consumer"); SECTOR_MAP.put("TGT", "Consumer");

        SECTOR_MAP.put("BA", "Industrial"); SECTOR_MAP.put("CAT", "Industrial");
        SECTOR_MAP.put("GE", "Industrial"); SECTOR_MAP.put("HON", "Industrial");
        SECTOR_MAP.put("UPS", "Industrial"); SECTOR_MAP.put("FDX", "Industrial");
        SECTOR_MAP.put("RTX", "Industrial"); SECTOR_MAP.put("LMT", "Industrial");
        SECTOR_MAP.put("NOC", "Industrial"); SECTOR_MAP.put("MMM", "Industrial");

        SECTOR_MAP.put("XOM", "Energy"); SECTOR_MAP.put("CVX", "Energy");
        SECTOR_MAP.put("COP", "Energy"); SECTOR_MAP.put("SLB", "Energy");
        SECTOR_MAP.put("EOG", "Energy"); SECTOR_MAP.put("PSX", "Energy");
        SECTOR_MAP.put("VLO", "Energy"); SECTOR_MAP.put("OXY", "Energy");

        SECTOR_MAP.put("VZ", "Telecom"); SECTOR_MAP.put("T", "Telecom");
        SECTOR_MAP.put("TMUS", "Telecom"); SECTOR_MAP.put("CMCSA", "Telecom");
        SECTOR_MAP.put("CHTR", "Telecom");
    }

    // 基准价格 (模拟真实价格)
    private static final Map<String, BigDecimal> BASE_PRICES = new HashMap<>();
    static {
        BASE_PRICES.put("AAPL", new BigDecimal("195.35"));
        BASE_PRICES.put("MSFT", new BigDecimal("415.20"));
        BASE_PRICES.put("GOOGL", new BigDecimal("172.50"));
        BASE_PRICES.put("META", new BigDecimal("485.60"));
        BASE_PRICES.put("NVDA", new BigDecimal("895.50"));
        BASE_PRICES.put("TSLA", new BigDecimal("178.50"));
        BASE_PRICES.put("AMD", new BigDecimal("158.20"));
        BASE_PRICES.put("INTC", new BigDecimal("31.50"));
        BASE_PRICES.put("QCOM", new BigDecimal("215.80"));
        BASE_PRICES.put("CRM", new BigDecimal("275.50"));
        BASE_PRICES.put("ORCL", new BigDecimal("125.30"));
        BASE_PRICES.put("ADBE", new BigDecimal("575.20"));
        BASE_PRICES.put("IBM", new BigDecimal("215.50"));
        BASE_PRICES.put("NOW", new BigDecimal("785.30"));
        BASE_PRICES.put("SNOW", new BigDecimal("165.80"));
        BASE_PRICES.put("JPM", new BigDecimal("198.50"));
        BASE_PRICES.put("BAC", new BigDecimal("38.50"));
        BASE_PRICES.put("WFC", new BigDecimal("62.30"));
        BASE_PRICES.put("GS", new BigDecimal("458.50"));
        BASE_PRICES.put("MS", new BigDecimal("98.50"));
        BASE_PRICES.put("BLK", new BigDecimal("825.30"));
        BASE_PRICES.put("SCHW", new BigDecimal("72.50"));
        BASE_PRICES.put("C", new BigDecimal("58.20"));
        BASE_PRICES.put("USB", new BigDecimal("42.80"));
        BASE_PRICES.put("PNC", new BigDecimal("165.30"));
        BASE_PRICES.put("AXP", new BigDecimal("225.50"));
        BASE_PRICES.put("V", new BigDecimal("285.30"));
        BASE_PRICES.put("JNJ", new BigDecimal("158.50"));
        BASE_PRICES.put("UNH", new BigDecimal("525.30"));
        BASE_PRICES.put("PFE", new BigDecimal("28.50"));
        BASE_PRICES.put("MRK", new BigDecimal("125.80"));
        BASE_PRICES.put("ABT", new BigDecimal("108.50"));
        BASE_PRICES.put("TMO", new BigDecimal("558.30"));
        BASE_PRICES.put("AMGN", new BigDecimal("315.50"));
        BASE_PRICES.put("BMY", new BigDecimal("58.30"));
        BASE_PRICES.put("GILD", new BigDecimal("85.50"));
        BASE_PRICES.put("REGN", new BigDecimal("985.30"));
        BASE_PRICES.put("AMZN", new BigDecimal("185.50"));
        BASE_PRICES.put("KO", new BigDecimal("62.50"));
        BASE_PRICES.put("PEP", new BigDecimal("175.30"));
        BASE_PRICES.put("MCD", new BigDecimal("285.50"));
        BASE_PRICES.put("SBUX", new BigDecimal("78.50"));
        BASE_PRICES.put("NKE", new BigDecimal("85.30"));
        BASE_PRICES.put("DIS", new BigDecimal("112.50"));
        BASE_PRICES.put("COST", new BigDecimal("875.30"));
        BASE_PRICES.put("WMT", new BigDecimal("165.50"));
        BASE_PRICES.put("TGT", new BigDecimal("158.30"));
        BASE_PRICES.put("BA", new BigDecimal("185.50"));
        BASE_PRICES.put("CAT", new BigDecimal("358.30"));
        BASE_PRICES.put("GE", new BigDecimal("185.50"));
        BASE_PRICES.put("HON", new BigDecimal("215.30"));
        BASE_PRICES.put("UPS", new BigDecimal("142.50"));
        BASE_PRICES.put("FDX", new BigDecimal("285.30"));
        BASE_PRICES.put("RTX", new BigDecimal("125.50"));
        BASE_PRICES.put("LMT", new BigDecimal("458.30"));
        BASE_PRICES.put("NOC", new BigDecimal("475.50"));
        BASE_PRICES.put("MMM", new BigDecimal("98.50"));
        BASE_PRICES.put("XOM", new BigDecimal("118.50"));
        BASE_PRICES.put("CVX", new BigDecimal("158.30"));
        BASE_PRICES.put("COP", new BigDecimal("115.50"));
        BASE_PRICES.put("SLB", new BigDecimal("52.30"));
        BASE_PRICES.put("EOG", new BigDecimal("128.50"));
        BASE_PRICES.put("PSX", new BigDecimal("142.30"));
        BASE_PRICES.put("VLO", new BigDecimal("145.50"));
        BASE_PRICES.put("OXY", new BigDecimal("58.30"));
        BASE_PRICES.put("VZ", new BigDecimal("42.50"));
        BASE_PRICES.put("T", new BigDecimal("18.50"));
        BASE_PRICES.put("TMUS", new BigDecimal("185.30"));
        BASE_PRICES.put("CMCSA", new BigDecimal("38.50"));
        BASE_PRICES.put("CHTR", new BigDecimal("358.30"));
        BASE_PRICES.put("BRK-B", new BigDecimal("458.50"));
        BASE_PRICES.put("SPGI", new BigDecimal("525.30"));
        BASE_PRICES.put("MA", new BigDecimal("485.50"));
        BASE_PRICES.put("CME", new BigDecimal("225.30"));
        BASE_PRICES.put("ICE", new BigDecimal("138.50"));
        BASE_PRICES.put("NDAQ", new BigDecimal("72.50"));
        BASE_PRICES.put("PLTR", new BigDecimal("85.50"));
        BASE_PRICES.put("COIN", new BigDecimal("225.30"));
        BASE_PRICES.put("SHOP", new BigDecimal("78.50"));
        BASE_PRICES.put("UBER", new BigDecimal("72.30"));
    }

    // 股票名称
    private static final Map<String, String> STOCK_NAMES = new HashMap<>();
    static {
        STOCK_NAMES.put("AAPL", "Apple Inc."); STOCK_NAMES.put("MSFT", "Microsoft Corp.");
        STOCK_NAMES.put("GOOGL", "Alphabet Inc."); STOCK_NAMES.put("META", "Meta Platforms");
        STOCK_NAMES.put("NVDA", "NVIDIA Corp."); STOCK_NAMES.put("TSLA", "Tesla Inc.");
        STOCK_NAMES.put("AMD", "AMD Inc."); STOCK_NAMES.put("INTC", "Intel Corp.");
        STOCK_NAMES.put("QCOM", "Qualcomm Inc."); STOCK_NAMES.put("CRM", "Salesforce Inc.");
        STOCK_NAMES.put("ORCL", "Oracle Corp."); STOCK_NAMES.put("ADBE", "Adobe Inc.");
        STOCK_NAMES.put("IBM", "IBM Corp."); STOCK_NAMES.put("NOW", "ServiceNow Inc.");
        STOCK_NAMES.put("SNOW", "Snowflake Inc."); STOCK_NAMES.put("JPM", "JPMorgan Chase");
        STOCK_NAMES.put("BAC", "Bank of America"); STOCK_NAMES.put("WFC", "Wells Fargo");
        STOCK_NAMES.put("GS", "Goldman Sachs"); STOCK_NAMES.put("MS", "Morgan Stanley");
        STOCK_NAMES.put("BLK", "BlackRock Inc."); STOCK_NAMES.put("SCHW", "Charles Schwab");
        STOCK_NAMES.put("C", "Citigroup Inc."); STOCK_NAMES.put("USB", "US Bancorp");
        STOCK_NAMES.put("PNC", "PNC Financial"); STOCK_NAMES.put("AXP", "American Express");
        STOCK_NAMES.put("V", "Visa Inc."); STOCK_NAMES.put("JNJ", "Johnson & Johnson");
        STOCK_NAMES.put("UNH", "UnitedHealth"); STOCK_NAMES.put("PFE", "Pfizer Inc.");
        STOCK_NAMES.put("MRK", "Merck & Co."); STOCK_NAMES.put("ABT", "Abbott Labs");
        STOCK_NAMES.put("TMO", "Thermo Fisher"); STOCK_NAMES.put("AMGN", "Amgen Inc.");
        STOCK_NAMES.put("BMY", "Bristol Myers"); STOCK_NAMES.put("GILD", "Gilead Sciences");
        STOCK_NAMES.put("REGN", "Regeneron"); STOCK_NAMES.put("AMZN", "Amazon Inc.");
        STOCK_NAMES.put("KO", "Coca-Cola Co."); STOCK_NAMES.put("PEP", "PepsiCo Inc.");
        STOCK_NAMES.put("MCD", "McDonald Corp."); STOCK_NAMES.put("SBUX", "Starbucks Corp.");
        STOCK_NAMES.put("NKE", "Nike Inc."); STOCK_NAMES.put("DIS", "Disney Inc.");
        STOCK_NAMES.put("COST", "Costco Inc."); STOCK_NAMES.put("WMT", "Walmart Inc.");
        STOCK_NAMES.put("TGT", "Target Corp."); STOCK_NAMES.put("BA", "Boeing Co.");
        STOCK_NAMES.put("CAT", "Caterpillar Inc."); STOCK_NAMES.put("GE", "GE Aerospace");
        STOCK_NAMES.put("HON", "Honeywell Intl"); STOCK_NAMES.put("UPS", "UPS Inc.");
        STOCK_NAMES.put("FDX", "FedEx Corp."); STOCK_NAMES.put("RTX", "RTX Corp.");
        STOCK_NAMES.put("LMT", "Lockheed Martin"); STOCK_NAMES.put("NOC", "Northrop Grumm");
        STOCK_NAMES.put("MMM", "3M Company"); STOCK_NAMES.put("XOM", "Exxon Mobil");
        STOCK_NAMES.put("CVX", "Chevron Corp."); STOCK_NAMES.put("COP", "ConocoPhillips");
        STOCK_NAMES.put("SLB", "Schlumberger"); STOCK_NAMES.put("EOG", "EOG Resources");
        STOCK_NAMES.put("PSX", "Phillips 66"); STOCK_NAMES.put("VLO", "Valero Energy");
        STOCK_NAMES.put("OXY", "Occidental"); STOCK_NAMES.put("VZ", "Verizon Comms");
        STOCK_NAMES.put("T", "AT&T Inc."); STOCK_NAMES.put("TMUS", "T-Mobile US");
        STOCK_NAMES.put("CMCSA", "Comcast Corp."); STOCK_NAMES.put("CHTR", "Charter Comm");
        STOCK_NAMES.put("BRK-B", "Berkshire Hath"); STOCK_NAMES.put("SPGI", "S&P Global");
        STOCK_NAMES.put("MA", "Mastercard Inc."); STOCK_NAMES.put("CME", "CME Group");
        STOCK_NAMES.put("ICE", "Intercont Exch"); STOCK_NAMES.put("NDAQ", "Nasdaq Inc.");
        STOCK_NAMES.put("PLTR", "Palantir Tech"); STOCK_NAMES.put("COIN", "Coinbase Glob");
        STOCK_NAMES.put("SHOP", "Shopify Inc."); STOCK_NAMES.put("UBER", "Uber Tech Inc");
    }

    // 市值
    private static final Map<String, String> MARKET_CAPS = new HashMap<>();
    static {
        MARKET_CAPS.put("AAPL", "2.9T"); MARKET_CAPS.put("MSFT", "3.1T");
        MARKET_CAPS.put("GOOGL", "1.8T"); MARKET_CAPS.put("META", "1.2T");
        MARKET_CAPS.put("NVDA", "2.2T"); MARKET_CAPS.put("TSLA", "560B");
        MARKET_CAPS.put("AMD", "260B"); MARKET_CAPS.put("INTC", "140B");
        MARKET_CAPS.put("QCOM", "190B"); MARKET_CAPS.put("CRM", "270B");
        MARKET_CAPS.put("ORCL", "360B"); MARKET_CAPS.put("ADBE", "250B");
        MARKET_CAPS.put("IBM", "200B"); MARKET_CAPS.put("NOW", "180B");
        MARKET_CAPS.put("SNOW", "55B"); MARKET_CAPS.put("JPM", "580B");
        MARKET_CAPS.put("BAC", "310B"); MARKET_CAPS.put("WFC", "250B");
        MARKET_CAPS.put("GS", "150B"); MARKET_CAPS.put("MS", "160B");
        MARKET_CAPS.put("BLK", "125B"); MARKET_CAPS.put("SCHW", "140B");
        MARKET_CAPS.put("C", "110B"); MARKET_CAPS.put("USB", "70B");
        MARKET_CAPS.put("PNC", "65B"); MARKET_CAPS.put("AXP", "75B");
        MARKET_CAPS.put("V", "580B"); MARKET_CAPS.put("JNJ", "380B");
        MARKET_CAPS.put("UNH", "480B"); MARKET_CAPS.put("PFE", "160B");
        MARKET_CAPS.put("MRK", "320B"); MARKET_CAPS.put("ABT", "190B");
        MARKET_CAPS.put("TMO", "210B"); MARKET_CAPS.put("AMGN", "170B");
        MARKET_CAPS.put("BMY", "120B"); MARKET_CAPS.put("GILD", "85B");
        MARKET_CAPS.put("REGN", "110B"); MARKET_CAPS.put("AMZN", "1.9T");
        MARKET_CAPS.put("KO", "270B"); MARKET_CAPS.put("PEP", "240B");
        MARKET_CAPS.put("MCD", "210B"); MARKET_CAPS.put("SBUX", "90B");
        MARKET_CAPS.put("NKE", "130B"); MARKET_CAPS.put("DIS", "200B");
        MARKET_CAPS.put("COST", "380B"); MARKET_CAPS.put("WMT", "450B");
        MARKET_CAPS.put("TGT", "75B"); MARKET_CAPS.put("BA", "110B");
        MARKET_CAPS.put("CAT", "180B"); MARKET_CAPS.put("GE", "200B");
        MARKET_CAPS.put("HON", "140B"); MARKET_CAPS.put("UPS", "120B");
        MARKET_CAPS.put("FDX", "72B"); MARKET_CAPS.put("RTX", "160B");
        MARKET_CAPS.put("LMT", "110B"); MARKET_CAPS.put("NOC", "115B");
        MARKET_CAPS.put("MMM", "55B"); MARKET_CAPS.put("XOM", "500B");
        MARKET_CAPS.put("CVX", "290B"); MARKET_CAPS.put("COP", "130B");
        MARKET_CAPS.put("SLB", "75B"); MARKET_CAPS.put("EOG", "72B");
        MARKET_CAPS.put("PSX", "65B"); MARKET_CAPS.put("VLO", "50B");
        MARKET_CAPS.put("OXY", "55B"); MARKET_CAPS.put("VZ", "180B");
        MARKET_CAPS.put("T", "130B"); MARKET_CAPS.put("TMUS", "210B");
        MARKET_CAPS.put("CMCSA", "150B"); MARKET_CAPS.put("CHTR", "55B");
        MARKET_CAPS.put("BRK-B", "880B"); MARKET_CAPS.put("SPGI", "150B");
        MARKET_CAPS.put("MA", "450B"); MARKET_CAPS.put("CME", "80B");
        MARKET_CAPS.put("ICE", "80B"); MARKET_CAPS.put("NDAQ", "40B");
        MARKET_CAPS.put("PLTR", "180B"); MARKET_CAPS.put("COIN", "55B");
        MARKET_CAPS.put("SHOP", "100B"); MARKET_CAPS.put("UBER", "150B");
    }

    /**
     * 生成实时美股数据
     */
    public List<Map<String, Object>> fetchRealTimeData() {
        List<Map<String, Object>> result = new ArrayList<>();
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Random random = new Random();

        for (String symbol : STOCK_SYMBOLS) {
            BigDecimal basePrice = BASE_PRICES.getOrDefault(symbol, new BigDecimal("100.00"));

            // 模拟价格波动 (-3% 到 +3%)
            double fluctuation = (random.nextDouble() - 0.5) * 0.06;
            BigDecimal closePrice = basePrice.multiply(BigDecimal.valueOf(1 + fluctuation))
                    .setScale(2, RoundingMode.HALF_UP);

            // 前收盘价 (基准价格)
            BigDecimal prevClose = basePrice;

            // 涨跌额
            BigDecimal change = closePrice.subtract(prevClose).setScale(2, RoundingMode.HALF_UP);

            // 涨跌幅
            BigDecimal changePct = change.divide(prevClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            // 开盘、最高、最低价
            BigDecimal openPrice = basePrice.multiply(BigDecimal.valueOf(1 + (random.nextDouble() - 0.5) * 0.02))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal highPrice = closePrice.max(openPrice).multiply(BigDecimal.valueOf(1 + random.nextDouble() * 0.015))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lowPrice = closePrice.min(openPrice).multiply(BigDecimal.valueOf(1 - random.nextDouble() * 0.015))
                    .setScale(2, RoundingMode.HALF_UP);

            // 成交量 (100万到1亿)
            long volume = 1000000 + (long)(random.nextDouble() * 99000000);

            Map<String, Object> data = new HashMap<>();
            data.put("symbol", symbol);
            data.put("name", STOCK_NAMES.getOrDefault(symbol, symbol));
            data.put("sector", SECTOR_MAP.getOrDefault(symbol, "Other"));
            data.put("closePrice", closePrice);
            data.put("prevClose", prevClose);
            data.put("change", change);
            data.put("changePct", changePct);
            data.put("openPrice", openPrice);
            data.put("highPrice", highPrice);
            data.put("lowPrice", lowPrice);
            data.put("volume", volume);
            data.put("marketCap", MARKET_CAPS.getOrDefault(symbol, "100B"));
            data.put("tradeDate", today);

            result.add(data);
        }

        return result;
    }

    /**
     * 同步数据到数据库
     */
    public void syncToDatabase() {
        List<Map<String, Object>> data = fetchRealTimeData();

        if (data.isEmpty()) {
            log.warn("未生成美股数据，跳过同步");
            return;
        }

        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 先删除今天的旧数据
        jdbcTemplate.update("DELETE FROM us_stock_weekly WHERE trade_date = ?", today);

        // 批量插入新数据
        String sql = "INSERT INTO us_stock_weekly (symbol, name, sector, close_price, prev_close, " +
                "`change`, change_pct, open_price, high_price, low_price, volume, market_cap, trade_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, item) -> {
            ps.setString(1, (String) item.get("symbol"));
            ps.setString(2, (String) item.get("name"));
            ps.setString(3, (String) item.get("sector"));
            ps.setBigDecimal(4, (BigDecimal) item.get("closePrice"));
            ps.setBigDecimal(5, (BigDecimal) item.get("prevClose"));
            ps.setBigDecimal(6, (BigDecimal) item.get("change"));
            ps.setBigDecimal(7, (BigDecimal) item.get("changePct"));
            ps.setBigDecimal(8, (BigDecimal) item.get("openPrice"));
            ps.setBigDecimal(9, (BigDecimal) item.get("highPrice"));
            ps.setBigDecimal(10, (BigDecimal) item.get("lowPrice"));
            ps.setLong(11, (Long) item.get("volume"));
            ps.setString(12, (String) item.get("marketCap"));
            ps.setString(13, (String) item.get("tradeDate"));
        });

        log.info("成功同步 {} 条美股数据到数据库", data.size());
    }
}
