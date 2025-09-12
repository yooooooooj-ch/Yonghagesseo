/* ===========================================================
   오늘자 자녀 소비내역 랜덤 생성 (일부 자녀만 선택)
   -----------------------------------------------------------
   파라미터
   - p_select_rate        : 자녀 선택 확률(0~1). 예) 0.6 -> 약 60% 자녀만 생성 대상
   - p_min_tx_per_child   : 자녀 1명당 최소 생성 건수(0 이상 가능)
   - p_max_tx_per_child   : 자녀 1명당 최대 생성 건수(최소값 이상)
   - p_update_balance     : 'Y'면 ACCOUNTS.balance 차감, 기본 'N'
   - p_skip_if_exists     : 'Y'면 해당 자녀 계좌에 '오늘자' 소비가 이미 있으면 건너뜀
   주의
   - COMMIT/ROLLBACK은 호출자가 제어합니다. 이 프로시저는 COMMIT 하지 않습니다.
   - CONSUME.cons_no는 트리거/시퀀스 환경에 맞게 조정하십시오(여기선 생략 가정).
   =========================================================== */
CREATE OR REPLACE PROCEDURE add_random_consumes_today_opt (
    p_select_rate        IN NUMBER      DEFAULT 0.6,
    p_min_tx_per_child   IN PLS_INTEGER DEFAULT 0,
    p_max_tx_per_child   IN PLS_INTEGER DEFAULT 3,
    p_update_balance     IN VARCHAR2    DEFAULT 'N',
    p_skip_if_exists     IN VARCHAR2    DEFAULT 'N'
) IS
    v_today   DATE := TRUNC(SYSDATE);
    v_sel_cnt PLS_INTEGER := 0;
    v_rows    PLS_INTEGER := 0;

    -- 카테고리 가중치/금액/라벨: 질문자가 주신 스키마/확률에 맞춤(4:식비,5:교통,6:취미,2:의류,1:전자,3:게임)
    FUNCTION pick_cons_type RETURN NUMBER IS
        v_r NUMBER := DBMS_RANDOM.VALUE(0,100);
    BEGIN
        IF v_r < 35 THEN RETURN 4;      -- 식비
        ELSIF v_r < 55 THEN RETURN 5;   -- 교통
        ELSIF v_r < 80 THEN RETURN 6;   -- 취미/여가
        ELSIF v_r < 90 THEN RETURN 2;   -- 의류
        ELSIF v_r < 95 THEN RETURN 1;   -- 전자기기
        ELSE              RETURN 3;     -- 게임/콘텐츠
        END IF;
    END;

    FUNCTION amount_by_type(p_type NUMBER) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        CASE p_type
          WHEN 4 THEN v := TRUNC(DBMS_RANDOM.VALUE( 3000, 15001));   -- 식비
          WHEN 5 THEN v := TRUNC(DBMS_RANDOM.VALUE( 1000,  5001));   -- 교통
          WHEN 1 THEN v := TRUNC(DBMS_RANDOM.VALUE(50000, 150001));  -- 전자
          WHEN 2 THEN v := TRUNC(DBMS_RANDOM.VALUE(10000,  80001));  -- 의류
          WHEN 3 THEN v := TRUNC(DBMS_RANDOM.VALUE( 5000,  50001));  -- 게임
          WHEN 6 THEN v := TRUNC(DBMS_RANDOM.VALUE( 3000,  30001));  -- 취미/여가
          ELSE        v := TRUNC(DBMS_RANDOM.VALUE( 1000,  30001));  -- 기타
        END CASE;
        RETURN (v/100)*100; -- 100원 절삭
    END;

    FUNCTION desc_by_type(p_type NUMBER) RETURN VARCHAR2 IS
    BEGIN
        CASE p_type
          WHEN 4 THEN RETURN '식비';
          WHEN 5 THEN RETURN '교통비';
          WHEN 1 THEN RETURN '전자기기';
          WHEN 2 THEN RETURN '의류';
          WHEN 3 THEN RETURN '게임/콘텐츠';
          WHEN 6 THEN RETURN '취미/여가';
          ELSE        RETURN '기타';
        END CASE;
    END;

    FUNCTION random_time_today RETURN DATE IS
        secs PLS_INTEGER := TRUNC(DBMS_RANDOM.VALUE(0, 86400)); -- 0~86399
    BEGIN
        RETURN v_today + NUMTODSINTERVAL(secs, 'SECOND');
    END;
BEGIN
    -- 파라미터 보정
    IF p_max_tx_per_child < p_min_tx_per_child THEN
        RAISE_APPLICATION_ERROR(-20001, 'p_max_tx_per_child < p_min_tx_per_child');
    END IF;

    -- 확률(0~1) 보정
    DECLARE
        v_rate NUMBER := LEAST(GREATEST(NVL(p_select_rate,0), 0), 1);
    BEGIN
        FOR c IN (
            SELECT u.user_no, MIN(a.account_no) AS account_no
              FROM USERS u
              JOIN ACCOUNTS a ON a.user_no = u.user_no
             WHERE u.user_type = 1
             GROUP BY u.user_no
        ) LOOP
            -- 일부 자녀만 선택 (선택 안되면 그 자녀는 오늘 0건 생성)
            IF DBMS_RANDOM.VALUE(0,1) > v_rate THEN
                CONTINUE;
            END IF;

            -- 오늘자 소비 이미 있으면 스킵 옵션
            IF p_skip_if_exists = 'Y' THEN
                DECLARE v_exist NUMBER;
                BEGIN
                    SELECT COUNT(*) INTO v_exist
                      FROM CONSUME
                     WHERE account_no = c.account_no
                       AND TRUNC(cons_date) = v_today;
                    IF v_exist > 0 THEN
                        CONTINUE;
                    END IF;
                END;
            END IF;

            v_sel_cnt := v_sel_cnt + 1;

            DECLARE
                v_cnt  PLS_INTEGER := TRUNC(DBMS_RANDOM.VALUE(p_min_tx_per_child, p_max_tx_per_child + 1));
                v_type NUMBER;
                v_amt  NUMBER;
                v_desc VARCHAR2(100);
                v_dt   DATE;
            BEGIN
                IF v_cnt > 0 THEN
                    FOR i IN 1..v_cnt LOOP
                        v_type := pick_cons_type;
                        v_amt  := amount_by_type(v_type);
                        v_desc := desc_by_type(v_type);
                        v_dt   := random_time_today;

                        INSERT INTO CONSUME (account_no, amount, cons_desc, cons_type, cons_date, used_point)
                        VALUES (c.account_no, v_amt, v_desc, v_type, v_dt, 0);

                        IF p_update_balance = 'Y' THEN
                            UPDATE ACCOUNTS
                               SET balance = balance - v_amt
                             WHERE account_no = c.account_no;
                        END IF;

                        v_rows := v_rows + 1;
                    END LOOP;
                END IF;
            END;
        END LOOP;
    END;

    DBMS_OUTPUT.PUT_LINE('Selected children : ' || v_sel_cnt);
    DBMS_OUTPUT.PUT_LINE('Rows inserted     : ' || v_rows);
    DBMS_OUTPUT.PUT_LINE('For date          : ' || TO_CHAR(v_today, 'YYYY-MM-DD'));
    -- COMMIT 하지 않음.
END;
/


SET SERVEROUTPUT ON;

-- 약 50% 자녀만 대상, 0~3건 생성, 잔액 차감 안함, 오늘 기록 있으면 스킵하지 않음
BEGIN
  add_random_consumes_today_opt(0.5, 0, 3, 'N', 'N');
END;
/

-- 전 자녀 중 약 70% 대상, 1~4건 생성, 잔액 차감, 오늘 기록 있으면 스킵
BEGIN
  add_random_consumes_today_opt(0.7, 1, 4, 'Y', 'Y');
END;
/



