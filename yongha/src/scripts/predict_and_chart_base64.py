# -*- coding: utf-8 -*-
# 파일 인자 기반 처리: --in <json> --out <json>
# GUI 백엔드 사용 금지 (서버 환경)
import matplotlib
matplotlib.use("Agg")

import sys
import json
import argparse
import warnings
from datetime import datetime, timedelta
import io
import base64

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# 한글 폰트(환경에 맞게 조정)
from matplotlib import rcParams, font_manager as fm
import os
for ttf in [r"C:\Windows\Fonts\malgun.ttf", r"C:\Windows\Fonts\malgunbd.ttf"]:
    if os.path.exists(ttf): fm.fontManager.addfont(ttf)
rcParams["font.family"] = "Malgun Gothic"
rcParams["axes.unicode_minus"] = False

warnings.simplefilter("ignore", UserWarning)

import matplotlib as m, shutil
try: shutil.rmtree(m.get_cachedir())
except Exception: pass


# Prophet (미설치/오류 시 우회)
try:
    from prophet import Prophet
    HAS_PROPHET = True
except Exception as _:
    HAS_PROPHET = False

def run(payload: dict) -> dict:
    charts = {}
    results = []

    today = datetime.today().replace(hour=0, minute=0, second=0, microsecond=0)
    forecast_days = 90

    payload_rows = payload.get("payload", []) or []
    payload2_rows = payload.get("payload2", []) or []

    # payload2 dict: user_no -> info
    child_info = {}
    for item in payload2_rows:
        if not item or len(item) < 5:
            continue
        user_no = int(item[0])
        current_balance = float(item[1] or 0.0)
        daily_auto = item[2] or []
        target_amount = float(item[3] or 0.0)
        child_name = str(item[4])
        child_info[user_no] = {
            "current_balance": current_balance,
            "daily_auto": daily_auto,
            "target_amount": target_amount,
            "child_name": child_name
        }

    # payload group: user_no -> [(cat, amt, date)]
    user_groups = {}
    for row in payload_rows:
        if not row or len(row) < 4:
            continue
        user_no = int(row[0])
        cat = int(row[1]) if row[1] is not None else -1
        amt = float(row[2] or 0.0)
        try:
            ds = pd.to_datetime(row[3])
        except Exception:
            ds = pd.Timestamp(today)
        user_groups.setdefault(user_no, []).append((cat, amt, ds))

    idx = pd.date_range(today, today + timedelta(days=forecast_days - 1), freq="D")

    for user_no, records in user_groups.items():
        if user_no not in child_info:
            continue

        info = child_info[user_no]
        initial_balance = float(info["current_balance"] or 0.0)
        daily_auto_transfer = info["daily_auto"] or [0.0] * forecast_days
        # 길이 보정
        if len(daily_auto_transfer) < forecast_days:
            daily_auto_transfer = list(daily_auto_transfer) + [0.0] * (forecast_days - len(daily_auto_transfer))
        else:
            daily_auto_transfer = list(daily_auto_transfer[:forecast_days])

        target_amount = float(info["target_amount"] or 0.0)

        # 카테고리별 집계
        category_series = {}
        for cat, amt, ds in records:
            category_series.setdefault(cat, []).append((ds, amt))

        # 일별 총 소비 시리즈
        daily_consume_total = pd.Series(0.0, index=idx, dtype=float)

        for cat, data in category_series.items():
            try:
                df = pd.DataFrame(data, columns=['ds', 'y']).groupby('ds', as_index=True).sum()
                df = df.asfreq('D', fill_value=0.0)

                if cat in [4, 5] and HAS_PROPHET and not df.empty and df['y'].sum() > 0:
                    prophet_df = df.reset_index().rename(columns={'ds': 'ds', 'y': 'y'})
                    m = Prophet(daily_seasonality=True, weekly_seasonality=True, yearly_seasonality=False)
                    m.fit(prophet_df)
                    future = pd.DataFrame({'ds': idx})
                    forecast = m.predict(future)
                    pred = pd.Series(np.maximum(forecast['yhat'].values, 0.0), index=idx, dtype=float)

                elif cat in [1, 2, 3]:
                    mean_amt = float(df['y'].mean()) if not df.empty else 15000.0
                    purchase_prob = 0.08
                    rng = np.random.default_rng(seed=42 + int(cat))
                    pred_values = [float(rng.poisson(mean_amt * purchase_prob)) for _ in range(forecast_days)]
                    pred = pd.Series(pred_values, index=idx, dtype=float)

                else:
                    # 과거 패턴(최근값 반복) 혹은 0
                    if not df.empty:
                        # 최근 30일 평균을 기준으로 상수 예측
                        tail = df['y'].tail(30)
                        base = float(tail.mean()) if not tail.empty else float(df['y'].mean())
                    else:
                        base = 0.0
                    pred = pd.Series([base] * forecast_days, index=idx, dtype=float)

                daily_consume_total = daily_consume_total.add(pred, fill_value=0.0)
            except Exception as e:
                print(f"[Python] 예측 오류 (user {user_no}, cat {cat}): {e}", file=sys.stderr, flush=True)

        daily_autotransfer_total = pd.Series(daily_auto_transfer, index=idx, dtype=float)

        # 누적 잔액 계산
        cumulative_balance = pd.Series(index=idx, dtype=float)
        cumulative_balance.iloc[0] = initial_balance
        if forecast_days > 1:
            delta = (daily_autotransfer_total - daily_consume_total).iloc[1:].cumsum()
            cumulative_balance.iloc[1:] = initial_balance + delta.values
        
        child_name = info["child_name"]
        
        # 그래프
        plt.figure(figsize=(9, 4.5), dpi=120)  # 9in * 120dpi = 1080px
        plt.plot(cumulative_balance.index, cumulative_balance.values,
                 linestyle='-', color="blue", label='계좌 예상 잔액')

        if target_amount > 0:
            plt.hlines(y=target_amount, xmin=idx[0], xmax=idx[-1],
                       linestyles='--', color="red", label=f'목표금액 {target_amount:.0f}')

        achieved = cumulative_balance[cumulative_balance >= target_amount]
        if target_amount > 0 and not achieved.empty:
            d0 = achieved.index[0]
            plt.scatter(d0, target_amount, s=100, marker='*', zorder=5,
                        color="green", label=f'목표 달성 가능: {d0.strftime("%Y-%m-%d")}')

        ymax = max(float(np.nanmax(cumulative_balance.values)), target_amount) if len(cumulative_balance) else target_amount
        plt.ylim(0, ymax * 1.05 if ymax > 0 else 1.0)
        plt.xticks(rotation=45, ha='right')
        plt.ylabel("금액")
        plt.xlabel("날짜")
        plt.title(f"{child_name}의 계좌 예상 잔액")
        plt.ticklabel_format(style='plain', axis='y')
        plt.legend()
        plt.tight_layout()

        buf = io.BytesIO()
        plt.savefig(buf, format='png', bbox_inches='tight', pad_inches=0.1)
        plt.close()
        buf.seek(0)
        charts[f"{user_no}_cumulative_balance"] = base64.b64encode(buf.read()).decode('utf-8')

        results.append({
            "user_no": user_no,
            "target_amount": target_amount
        })

    return {"charts": charts, "predictions": results}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--in", dest="in_path")
    parser.add_argument("--out", dest="out_path")
    args = parser.parse_args()

    if not args.in_path or not args.out_path:
        print("[Python] --in/--out 인자를 지정하세요.", file=sys.stderr, flush=True)
        sys.exit(2)

    try:
        with open(args.in_path, "r", encoding="utf-8") as f:
            payload = json.load(f)
    except Exception as e:
        print(f"[Python] 입력 JSON 로드 실패: {e}", file=sys.stderr, flush=True)
        sys.exit(3)

    try:
        result = run(payload)
    except Exception as e:
        print(f"[Python] 실행 오류: {e}", file=sys.stderr, flush=True)
        sys.exit(4)

    try:
        with open(args.out_path, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False)
    except Exception as e:
        print(f"[Python] 출력 JSON 저장 실패: {e}", file=sys.stderr, flush=True)
        sys.exit(5)

if __name__ == "__main__":
    main()