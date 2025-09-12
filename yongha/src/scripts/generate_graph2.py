#!/usr/bin/env python
# coding: utf-8

import sys
import matplotlib.pyplot as plt
import base64
from io import BytesIO

# 한글 폰트 설정
plt.rc('font', family='Malgun Gothic')
plt.rcParams['axes.unicode_minus'] = False

# 부모, 자녀 수 받기
parents = int(sys.argv[1])
children = int(sys.argv[2])

labels = ['부모', '자녀']
sizes = [parents, children]
colors = ['skyblue', 'lightgreen']

plt.figure(figsize=(4,4))
plt.pie(sizes, labels=labels, autopct='%1.1f%%', colors=colors, startangle=90)
plt.axis('equal')

# 이미지 → base64 변환
buf = BytesIO()
plt.savefig(buf, format='png')
buf.seek(0)
print(base64.b64encode(buf.read()).decode('utf-8'))
