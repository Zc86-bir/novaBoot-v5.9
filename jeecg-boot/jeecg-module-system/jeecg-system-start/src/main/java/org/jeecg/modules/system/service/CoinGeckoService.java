package org.jeecg.modules.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CoinGecko API 服务
 * 用于获取真实加密货币市场数据
 *
 * @author jeecg
 * @since 2026-05-10
 */
@Slf4j
@Service
public class CoinGeckoService {

    private static final String API_BASE_URL = "https://api.coingecko.com/api/v3";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 支持的币种ID映射（CoinGecko ID -> 我们的symbol）
    private static final Map<String, String> COIN_ID_MAP = new LinkedHashMap<>();
    static {
        // Layer1
        COIN_ID_MAP.put("bitcoin", "BTC");
        COIN_ID_MAP.put("ethereum", "ETH");
        COIN_ID_MAP.put("solana", "SOL");
        COIN_ID_MAP.put("binancecoin", "BNB");
        COIN_ID_MAP.put("ripple", "XRP");
        COIN_ID_MAP.put("cardano", "ADA");
        COIN_ID_MAP.put("avalanche-2", "AVAX");
        COIN_ID_MAP.put("polkadot", "DOT");
        COIN_ID_MAP.put("near", "NEAR");
        COIN_ID_MAP.put("cosmos", "ATOM");
        COIN_ID_MAP.put("fantom", "FTM");
        COIN_ID_MAP.put("algorand", "ALGO");
        COIN_ID_MAP.put("internet-computer", "ICP");
        COIN_ID_MAP.put("filecoin", "FIL");
        COIN_ID_MAP.put("tron", "TRX");

        // DeFi
        COIN_ID_MAP.put("chainlink", "LINK");
        COIN_ID_MAP.put("uniswap", "UNI");
        COIN_ID_MAP.put("aave", "AAVE");
        COIN_ID_MAP.put("maker", "MKR");
        COIN_ID_MAP.put("curve-dao-token", "CRV");
        COIN_ID_MAP.put("compound-governance-token", "COMP");
        COIN_ID_MAP.put("sushi", "SUSHI");
        COIN_ID_MAP.put("lido-dao", "LDO");
        COIN_ID_MAP.put("havven", "SNX");
        COIN_ID_MAP.put("yearn-finance", "YFI");

        // Meme
        COIN_ID_MAP.put("dogecoin", "DOGE");
        COIN_ID_MAP.put("shiba-inu", "SHIB");
        COIN_ID_MAP.put("pepe", "PEPE");
        COIN_ID_MAP.put("floki", "FLOKI");
        COIN_ID_MAP.put("bonk", "BONK");
        COIN_ID_MAP.put("dogwifhat", "WIF");

        // Layer2
        COIN_ID_MAP.put("matic-network", "MATIC");
        COIN_ID_MAP.put("arbitrum", "ARB");
        COIN_ID_MAP.put("optimism", "OP");
        COIN_ID_MAP.put("immutable-x", "IMX");
        COIN_ID_MAP.put("loopring", "LRC");
        COIN_ID_MAP.put("starknet", "STRK");
        COIN_ID_MAP.put("litecoin", "LTC");
        COIN_ID_MAP.put("bitcoin-cash", "BCH");
        COIN_ID_MAP.put("eos", "EOS");
        COIN_ID_MAP.put("tezos", "XTZ");

        // AI
        COIN_ID_MAP.put("fetch-ai", "FET");
        COIN_ID_MAP.put("render-token", "RNDR");
        COIN_ID_MAP.put("bittensor", "TAO");
        COIN_ID_MAP.put("singularitynet", "AGIX");
        COIN_ID_MAP.put("ocean-protocol", "OCEAN");
    }

    /**
     * 获取币种名称映射
     */
    private static final Map<String, String> COIN_NAME_MAP = new HashMap<>();
    static {
        COIN_NAME_MAP.put("bitcoin", "Bitcoin");
        COIN_NAME_MAP.put("ethereum", "Ethereum");
        COIN_NAME_MAP.put("solana", "Solana");
        COIN_NAME_MAP.put("binancecoin", "BNB");
        COIN_NAME_MAP.put("ripple", "XRP");
        COIN_NAME_MAP.put("cardano", "Cardano");
        COIN_NAME_MAP.put("avalanche-2", "Avalanche");
        COIN_NAME_MAP.put("polkadot", "Polkadot");
        COIN_NAME_MAP.put("near", "NEAR Protocol");
        COIN_NAME_MAP.put("cosmos", "Cosmos");
        COIN_NAME_MAP.put("fantom", "Fantom");
        COIN_NAME_MAP.put("algorand", "Algorand");
        COIN_NAME_MAP.put("internet-computer", "Internet Computer");
        COIN_NAME_MAP.put("filecoin", "Filecoin");
        COIN_NAME_MAP.put("tron", "TRON");
        COIN_NAME_MAP.put("chainlink", "Chainlink");
        COIN_NAME_MAP.put("uniswap", "Uniswap");
        COIN_NAME_MAP.put("aave", "Aave");
        COIN_NAME_MAP.put("maker", "Maker");
        COIN_NAME_MAP.put("curve-dao-token", "Curve DAO");
        COIN_NAME_MAP.put("compound-governance-token", "Compound");
        COIN_NAME_MAP.put("sushi", "SushiSwap");
        COIN_NAME_MAP.put("lido-dao", "Lido DAO");
        COIN_NAME_MAP.put("havven", "Synthetix");
        COIN_NAME_MAP.put("yearn-finance", "yearn.finance");
        COIN_NAME_MAP.put("dogecoin", "Dogecoin");
        COIN_NAME_MAP.put("shiba-inu", "Shiba Inu");
        COIN_NAME_MAP.put("pepe", "Pepe");
        COIN_NAME_MAP.put("floki", "Floki Inu");
        COIN_NAME_MAP.put("bonk", "Bonk");
        COIN_NAME_MAP.put("dogwifhat", "dogwifhat");
        COIN_NAME_MAP.put("matic-network", "Polygon");
        COIN_NAME_MAP.put("arbitrum", "Arbitrum");
        COIN_NAME_MAP.put("optimism", "Optimism");
        COIN_NAME_MAP.put("immutable-x", "Immutable X");
        COIN_ID_MAP.put("loopring", "Loopring");
        COIN_NAME_MAP.put("starknet", "Starknet");
        COIN_NAME_MAP.put("loopring", "Loopring");
        COIN_NAME_MAP.put("litecoin", "Litecoin");
        COIN_NAME_MAP.put("bitcoin-cash", "Bitcoin Cash");
        COIN_NAME_MAP.put("eos", "EOS");
        COIN_NAME_MAP.put("tezos", "Tezos");
        COIN_NAME_MAP.put("fetch-ai", "Fetch.ai");
        COIN_NAME_MAP.put("render-token", "Render");
        COIN_NAME_MAP.put("bittensor", "Bittensor");
        COIN_NAME_MAP.put("singularitynet", "SingularityNET");
        COIN_NAME_MAP.put("ocean-protocol", "Ocean Protocol");
    }

    /**
     * 获取实时加密货币数据
     */
    public List<Map<String, Object>> fetchRealTimeData() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            String ids = String.join(",", COIN_ID_MAP.keySet());
            String url = API_BASE_URL + "/coins/markets?vs_currency=usd&ids=" + ids +
                    "&order=market_cap_desc&per_page=100&page=1&sparkline=false&price_change_percentage=24h,7d";

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("CoinGecko API请求失败: {}", response.code());
                    return result;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                int rank = 1;
                for (JsonNode coin : root) {
                    String id = coin.get("id").asText();
                    String symbol = COIN_ID_MAP.getOrDefault(id, coin.get("symbol").asText().toUpperCase());
                    String name = COIN_NAME_MAP.getOrDefault(id, coin.get("name").asText());

                    Map<String, Object> data = new HashMap<>();
                    data.put("symbol", symbol);
                    data.put("name", name);
                    data.put("price", BigDecimal.valueOf(coin.get("current_price").asDouble(0)));
                    data.put("priceBtc", BigDecimal.valueOf(coin.get("current_price").asDouble(0) / getBtcPrice(root)));
                    data.put("change24h", BigDecimal.valueOf(coin.get("price_change_24h").asDouble(0)));
                    data.put("changePct24h", BigDecimal.valueOf(coin.get("price_change_percentage_24h").asDouble(0)));
                    data.put("change7d", BigDecimal.valueOf(coin.has("price_change_percentage_7d_in_currency") ?
                            coin.get("price_change_percentage_7d_in_currency").asDouble(0) : 0));
                    data.put("high24h", BigDecimal.valueOf(coin.get("high_24h").asDouble(0)));
                    data.put("low24h", BigDecimal.valueOf(coin.get("low_24h").asDouble(0)));
                    data.put("volume24h", BigDecimal.valueOf(coin.get("total_volume").asDouble(0)));
                    data.put("marketCap", BigDecimal.valueOf(coin.get("market_cap").asDouble(0)));
                    data.put("marketCapRank", rank++);
                    data.put("circulatingSupply", BigDecimal.valueOf(coin.get("circulating_supply").asDouble(0)));
                    data.put("totalSupply", BigDecimal.valueOf(coin.get("total_supply").asDouble(0)));
                    data.put("maxSupply", coin.has("max_supply") && !coin.get("max_supply").isNull() ?
                            BigDecimal.valueOf(coin.get("max_supply").asDouble(0)) : null);
                    data.put("tradeDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

                    result.add(data);
                }
            }
        } catch (Exception e) {
            log.error("获取CoinGecko数据失败: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 从返回数据中获取BTC价格
     */
    private double getBtcPrice(JsonNode root) {
        for (JsonNode coin : root) {
            if ("bitcoin".equals(coin.get("id").asText())) {
                return coin.get("current_price").asDouble(1);
            }
        }
        return 1;
    }

    /**
     * 同步数据到数据库
     */
    public void syncToDatabase() {
        List<Map<String, Object>> data = fetchRealTimeData();

        if (data.isEmpty()) {
            log.warn("未获取到CoinGecko数据，跳过同步");
            return;
        }

        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 先删除今天的旧数据
        jdbcTemplate.update("DELETE FROM crypto_weekly WHERE trade_date = ?", today);

        // 批量插入新数据
        String sql = "INSERT INTO crypto_weekly (symbol, name, price, price_btc, change_24h, change_pct_24h, " +
                "change_7d, high_24h, low_24h, volume_24h, market_cap, market_cap_rank, circulating_supply, " +
                "total_supply, max_supply, trade_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, item) -> {
            ps.setString(1, (String) item.get("symbol"));
            ps.setString(2, (String) item.get("name"));
            ps.setBigDecimal(3, (BigDecimal) item.get("price"));
            ps.setBigDecimal(4, (BigDecimal) item.get("priceBtc"));
            ps.setBigDecimal(5, (BigDecimal) item.get("change24h"));
            ps.setBigDecimal(6, (BigDecimal) item.get("changePct24h"));
            ps.setBigDecimal(7, (BigDecimal) item.get("change7d"));
            ps.setBigDecimal(8, (BigDecimal) item.get("high24h"));
            ps.setBigDecimal(9, (BigDecimal) item.get("low24h"));
            ps.setBigDecimal(10, (BigDecimal) item.get("volume24h"));
            ps.setBigDecimal(11, (BigDecimal) item.get("marketCap"));
            ps.setInt(12, (Integer) item.get("marketCapRank"));
            ps.setBigDecimal(13, (BigDecimal) item.get("circulatingSupply"));
            ps.setBigDecimal(14, (BigDecimal) item.get("totalSupply"));
            ps.setBigDecimal(15, (BigDecimal) item.get("maxSupply"));
            ps.setString(16, (String) item.get("tradeDate"));
        });

        log.info("成功同步 {} 条加密货币数据到数据库", data.size());
    }
}
