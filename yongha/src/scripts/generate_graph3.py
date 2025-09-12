#!/usr/bin/env python
# coding: utf-8

import sys
import matplotlib.pyplot as plt
import io
import base64
from datetime import datetime, timedelta

# 한글 폰트 설정
plt.rc('font', family='Malgun Gothic')
plt.rcParams['axes.unicode_minus'] = False

def parse_input(input_str):
    """
    입력 문자열을 'YYYY-MM-DD,value;YYYY-MM-DD,value' 형식으로 받음
    비어 있으면 빈 리스트 반환
    """
    if not input_str:
        return []
    result = []
    for item in input_str.split(';'):
        if ',' in item:
            date_str, value_str = item.split(',')
            try:
                date = datetime.strptime(date_str, '%Y-%m-%d')
                value = int(value_str)
                result.append((date, value))
            except:
                continue
    return result

def generate_graph(data):
    # 오늘 기준 최근 7일
    today = datetime.today()
    days = [today - timedelta(days=i) for i in range(6, -1, -1)]
    
    # 데이터 딕셔너리화
    data_dict = {d.date(): v for d, v in data}
    values = [data_dict.get(d.date(), 0) for d in days]  # 데이터 없으면 0
    labels = [d.strftime('%m-%d') for d in days]

    # 막대 그래프
    plt.figure(figsize=(8, 4))
    plt.bar(labels, values, color='skyblue')
    plt.title("최근 7일 소비 현황")
    plt.xlabel("날짜")
    plt.ylabel("소비 금액")
    plt.tight_layout()

    # PNG → Base64
    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)
    return base64.b64encode(buf.read()).decode('utf-8')

if __name__ == "__main__":
    input_str = sys.argv[1] if len(sys.argv) > 1 else ""
    data = parse_input(input_str)
    graph_base64 = generate_graph(data)
    print(graph_base64)
