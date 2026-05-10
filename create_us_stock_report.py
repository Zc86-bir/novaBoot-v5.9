import sys; sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:9080/jeecg-boot/jmreport"
TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwiY2xpZW50VHlwZSI6IlBDIiwiZXhwIjoxNzc4NjI1ODM4fQ.QrIP8c9Cw8wPDazJGsBGXEeO-9PCnKdD5OER0-NvJZ4"

import requests
import json

session = requests.Session()
session.headers.update({"X-Access-Token": TOKEN, "Content-Type": "application/json"})

def req(method, path, data=None):
    url = BASE_URL + path
    if method == "GET":
        r = session.get(url)
    else:
        r = session.post(url, json=data)
    result = r.json()
    if not result.get("success"):
        raise Exception(f"{path} 失败: {result}")
    return result

# 1. 获取数据源
ds_result = req("GET", "/initDataSource")
ds_list = ds_result.get("result", [])
db_source = None
for ds in ds_list:
    if "积木" in ds.get("name", ""):
        db_source = ds["id"]
        break
if not db_source and ds_list:
    db_source = ds_list[0]["id"]
if not db_source:
    raise Exception("没有可用的数据源")
print(f"数据源: {db_source}")

# 2. 生成报表ID
import time, random
def gen_id():
    ts = str(int(time.time() * 1000))
    return "8" + ts[1:] + "".join([str(random.randint(0,9)) for _ in range(16-len(ts))])

report_id = gen_id()
print(f"报表ID: {report_id}")

# 3. 定义SQL和字段
sql = "SELECT symbol, name, sector, close_price, prev_close, `change`, change_pct, open_price, high_price, low_price, volume, market_cap, trade_date FROM us_stock_weekly ORDER BY change_pct DESC"

field_list = []
field_map = {
    "symbol": ("股票代码", "String"),
    "name": ("股票名称", "String"),
    "sector": ("行业板块", "String"),
    "close_price": ("收盘价", "BigDecimal"),
    "prev_close": ("前日收盘", "BigDecimal"),
    "change": ("涨跌额", "BigDecimal"),
    "change_pct": ("涨跌幅(%)", "BigDecimal"),
    "open_price": ("开盘价", "BigDecimal"),
    "high_price": ("最高价", "BigDecimal"),
    "low_price": ("最低价", "BigDecimal"),
    "volume": ("成交量", "Long"),
    "market_cap": ("市值", "String"),
    "trade_date": ("交易日期", "Date"),
}

for i, (raw_name, (txt, tp)) in enumerate(field_map.items()):
    field_list.append({
        "fieldName": raw_name,
        "fieldText": txt,
        "fieldType": tp,
        "widgetType": "String" if tp == "String" else "String",
        "orderNum": i,
        "isShow": 1,
        "isQuery": 0,
        "searchFlag": 0,
        "extJson": "",
        "dictCode": "",
    })

save_db_payload = {
    "dbCode": "usStockDs",
    "dbChName": "美股涨跌数据",
    "dbType": "0",
    "dbSource": db_source,
    "dbDynSql": sql,
    "isList": "1",
    "isPage": "0",
    "fieldList": field_list,
    "reportId": report_id,
}

db_result = req("POST", "/saveDb", save_db_payload)
db_id = db_result["result"]["id"]
print(f"数据集ID: {db_id}")

# 5. 构建报表设计
designer = {
    "id": report_id,
    "reportName": "上周五美股主流股票涨跌行情",
    "reportType": "0",
    "theme": "blue",
    "rows": {"len": 300},
    "cols": {"0": {"width": 30}, "len": 200},
    "styles": [
        {"align": "center", "border": {"bottom": ["thin", "#d8d8d8"], "top": ["thin", "#d8d8d8"], "left": ["thin", "#d8d8d8"], "right": ["thin", "#d8d8d8"]}},
        {"align": "center", "valign": "middle", "border": {"bottom": ["thin", "#d8d8d8"], "top": ["thin", "#d8d8d8"], "left": ["thin", "#d8d8d8"], "right": ["thin", "#d8d8d8"]}},
        {"align": "center", "valign": "middle", "bgcolor": "#4472C4", "font": {"color": "#FFFFFF", "bold": True}, "border": {"bottom": ["thin", "#d8d8d8"], "top": ["thin", "#d8d8d8"], "left": ["thin", "#d8d8d8"], "right": ["thin", "#d8d8d8"]}},
        {"align": "center", "valign": "middle", "font": {"size": 16, "bold": True}, "border": {"bottom": ["thin", "#d8d8d8"], "top": ["thin", "#d8d8d8"], "left": ["thin", "#d8d8d8"], "right": ["thin", "#d8d8d8"]}},
        {"align": "center"},
    ],
    "merges": [],
    "chartList": [],
    "background": {"enabled": True, "color": "#f5f7fa"},
    "freeze": "A3",
}

# 行1: 标题行
row1_cells = {
    "0": {"text": "", "style": 4},
    "1": {"text": "上周五美股主流股票涨跌行情（2026-05-08）", "style": 3, "merge": [0, 12]},
}
designer["rows"]["1"] = {"height": 45, "cells": row1_cells}
designer["merges"].append("B1:N1")

# 行2: 表头行
headers = ["股票代码", "股票名称", "行业板块", "收盘价($)", "前日收盘($)", "涨跌额($)", "涨跌幅(%)", "开盘价($)", "最高价($)", "最低价($)", "成交量", "市值", "交易日期"]
row2_cells = {"0": {"text": "", "style": 4}}
for i, h in enumerate(headers):
    row2_cells[str(i + 1)] = {"text": h, "style": 2}
designer["rows"]["2"] = {"height": 30, "cells": row2_cells}

# 行3+: 数据行 (绑定 #{usStockDs.field})
for row_idx in range(3, 28):
    data_row = row_idx - 2  # 1-based offset from data
    cells = {"0": {"text": "", "style": 4}}
    bindings = [
        "symbol", "name", "sector", "close_price", "prev_close",
        "change", "change_pct", "open_price", "high_price", "low_price",
        "volume", "market_cap", "trade_date"
    ]
    for i, field in enumerate(bindings):
        col = i + 1
        # 涨跌幅列特殊处理
        if field == "change_pct":
            cells[str(col)] = {"text": f"#{{usStockDs.{field}}}%", "style": 1}
        elif field == "change":
            cells[str(col)] = {"text": f"#{{usStockDs.{field}}}", "style": 1}
        elif field == "volume":
            cells[str(col)] = {"text": f"#{{usStockDs.{field}}}", "style": 1}
        else:
            cells[str(col)] = {"text": f"#{{usStockDs.{field}}}", "style": 1}
    designer["rows"][str(row_idx)] = {"height": 25, "cells": cells}

designer["rows"]["len"] = 28

# 6. 保存报表
save_payload = {
    "id": report_id,
    "designerObj": designer,
    "rows": designer["rows"],
    "cols": designer["cols"],
    "styles": designer["styles"],
    "merges": designer["merges"],
    "chartList": designer["chartList"],
    "background": designer["background"],
    "freeze": designer["freeze"],
    "reportName": designer["reportName"],
    "reportType": designer["reportType"],
    "theme": designer["theme"],
}

result = req("POST", "/save", save_payload)
print(f"报表创建成功!")
print(f"设计器: http://localhost:9080/jeecg-boot/jmreport/index/{report_id}?token={TOKEN}&tenantId=1")
print(f"预览: http://localhost:9080/jeecg-boot/jmreport/view/{report_id}?token={TOKEN}&tenantId=1")
