#!/usr/bin/env python
# coding: utf-8

import sys
import io
import base64
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

plt.rc('font', family='Malgun Gothic')
plt.rcParams['axes.unicode_minus'] = False

try:
    if len(sys.argv) != 2:
        print("Usage: python generate_graph.py <data>")
        sys.exit(1)

    data_str = sys.argv[1]
    items = data_str.split(';')
    dates = []
    amounts = []

    for item in items:
        d, a = item.split(',')
        dates.append(d)
        amounts.append(float(a))

    # 누적합 계산
    cum_amounts = []
    total = 0
    for a in amounts:
        total += a
        cum_amounts.append(total)

    plt.figure(figsize=(10,5))
    plt.plot(dates, cum_amounts, marker='o')
    plt.xticks(rotation=45)
    plt.xlabel('Date')
    plt.ylabel('Cumulative Amount')
    plt.title('모든 계좌의 누적 이체 금액')
    plt.tight_layout()

    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    buf.seek(0)
    print(base64.b64encode(buf.read()).decode('utf-8'))

except Exception as e:
    print("Error: {}".format(e))
    sys.exit(1)
