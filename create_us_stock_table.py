import sys; sys.stdout.reconfigure(encoding='utf-8')
import pymysql

conn = pymysql.connect(
    host='127.0.0.1',
    port=3306,
    user='root',
    password='tBYdA6c@S4T9RmW#^ZKR',
    database='jeecg-boot',
    charset='utf8mb4'
)

cursor = conn.cursor()

# 创建表
cursor.execute("""
DROP TABLE IF EXISTS `us_stock_weekly`
""")

cursor.execute("""
CREATE TABLE `us_stock_weekly` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `symbol` varchar(20) NOT NULL COMMENT '股票代码',
  `name` varchar(50) NOT NULL COMMENT '股票名称',
  `sector` varchar(50) DEFAULT NULL COMMENT '行业板块',
  `close_price` decimal(10,2) NOT NULL COMMENT '周五收盘价(美元)',
  `prev_close` decimal(10,2) NOT NULL COMMENT '前日收盘价(美元)',
  `change` decimal(10,2) NOT NULL COMMENT '涨跌额(美元)',
  `change_pct` decimal(6,2) NOT NULL COMMENT '涨跌幅(%)',
  `open_price` decimal(10,2) DEFAULT NULL COMMENT '开盘价(美元)',
  `high_price` decimal(10,2) DEFAULT NULL COMMENT '最高价(美元)',
  `low_price` decimal(10,2) DEFAULT NULL COMMENT '最低价(美元)',
  `volume` bigint(20) DEFAULT NULL COMMENT '成交量',
  `market_cap` varchar(20) DEFAULT NULL COMMENT '市值',
  `trade_date` date NOT NULL COMMENT '交易日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美股每周涨跌数据'
""")

# 插入数据
stocks = [
    ('AAPL','苹果公司','科技',198.52,195.30,3.22,1.65,196.10,199.80,195.50,68520000,'3.08T','2026-05-08'),
    ('MSFT','微软','科技',432.18,428.50,3.68,0.86,429.00,433.50,427.20,25430000,'3.21T','2026-05-08'),
    ('GOOGL','谷歌','科技',185.60,182.40,3.20,1.75,183.10,186.20,182.00,28760000,'2.30T','2026-05-08'),
    ('AMZN','亚马逊','消费',195.80,193.20,2.60,1.35,193.80,196.50,192.80,52340000,'2.03T','2026-05-08'),
    ('NVDA','英伟达','科技',142.35,138.90,3.45,2.48,139.50,143.20,138.50,485620000,'3.50T','2026-05-08'),
    ('META','Meta Platforms','科技',525.40,530.20,-4.80,-0.91,528.00,532.10,523.50,18920000,'1.34T','2026-05-08'),
    ('TSLA','特斯拉','汽车',285.60,278.40,7.20,2.59,280.00,288.50,277.80,125680000,'910B','2026-05-08'),
    ('BRK.B','伯克希尔哈撒韦','金融',485.20,483.80,1.40,0.29,484.00,486.50,482.80,3850000,'1.08T','2026-05-08'),
    ('JPM','摩根大通','金融',248.35,245.60,2.75,1.12,246.00,249.20,245.00,12450000,'710B','2026-05-08'),
    ('V','Visa','金融',312.80,310.50,2.30,0.74,311.00,313.50,309.80,8520000,'635B','2026-05-08'),
    ('JNJ','强生','医疗',158.42,159.80,-1.38,-0.86,159.50,160.20,157.80,9860000,'382B','2026-05-08'),
    ('WMT','沃尔玛','消费',92.35,90.80,1.55,1.71,91.00,92.80,90.50,18420000,'748B','2026-05-08'),
    ('PG','宝洁','消费',172.50,171.20,1.30,0.76,171.50,173.00,170.80,7650000,'405B','2026-05-08'),
    ('MA','万事达','金融',532.60,528.90,3.70,0.70,529.50,534.00,527.80,3280000,'498B','2026-05-08'),
    ('HD','家得宝','消费',398.20,395.50,2.70,0.68,396.00,399.50,394.80,4560000,'395B','2026-05-08'),
    ('DIS','迪士尼','消费',112.80,110.50,2.30,2.08,111.00,113.50,110.20,15280000,'206B','2026-05-08'),
    ('NFLX','奈飞','科技',985.40,972.30,13.10,1.35,975.00,988.50,970.20,5820000,'422B','2026-05-08'),
    ('AMD','超威半导体','科技',145.80,142.50,3.30,2.32,143.20,146.80,142.00,68920000,'236B','2026-05-08'),
    ('CRM','赛富时','科技',298.50,295.80,2.70,0.91,296.50,300.20,295.00,8450000,'286B','2026-05-08'),
    ('INTC','英特尔','科技',28.45,29.20,-0.75,-2.57,29.00,29.50,28.20,78520000,'121B','2026-05-08'),
    ('BA','波音','工业',185.20,188.50,-3.30,-1.75,187.80,189.50,184.50,9520000,'113B','2026-05-08'),
    ('KO','可口可乐','消费',63.80,63.20,0.60,0.95,63.30,64.00,63.10,15680000,'275B','2026-05-08'),
    ('PFE','辉瑞','医疗',26.35,26.80,-0.45,-1.68,26.70,27.00,26.20,38520000,'149B','2026-05-08'),
    ('XOM','埃克森美孚','能源',112.50,110.80,1.70,1.53,111.00,113.20,110.50,18260000,'452B','2026-05-08'),
    ('CVX','雪佛龙','能源',158.90,156.50,2.40,1.53,157.00,159.50,156.20,9820000,'288B','2026-05-08'),
]

cursor.executemany("""
INSERT INTO `us_stock_weekly` (symbol, name, sector, close_price, prev_close, `change`, change_pct, open_price, high_price, low_price, volume, market_cap, trade_date)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
""", stocks)

conn.commit()

# 验证
cursor.execute("SELECT COUNT(*) as cnt FROM us_stock_weekly")
cnt = cursor.fetchone()[0]
print(f"数据插入成功，共 {cnt} 条记录")

cursor.execute("SELECT symbol, name, close_price, `change`, change_pct FROM us_stock_weekly ORDER BY change_pct DESC LIMIT 5")
for row in cursor.fetchall():
    print(f"  {row[0]} {row[1]} 收盘={row[2]} 涨跌额={row[3]} 涨跌幅={row[4]}%")

cursor.close()
conn.close()
print("\nMySQL表创建完成")
