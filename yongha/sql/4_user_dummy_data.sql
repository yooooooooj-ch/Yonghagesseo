DECLARE
  -- 규모
  c_num_parents   PLS_INTEGER := 374;
  c_num_children  PLS_INTEGER := 543;
  c_num_admins    PLS_INTEGER := 1;

  -- 소비 데이터
  c_months_back        PLS_INTEGER := 12;
  c_min_tx_per_month   PLS_INTEGER := 8;
  c_max_tx_per_month   PLS_INTEGER := 20;

  -- 변수
  v_parent_user_no   USERS.user_no%TYPE;
  v_child_user_no    USERS.user_no%TYPE;
  v_admin_user_no    USERS.user_no%TYPE;

  v_parent_acct_no   ACCOUNTS.account_no%TYPE;
  v_child_acct_no    ACCOUNTS.account_no%TYPE;

  v_goal_no          GOALHISTORY.goal_no%TYPE;

  -- INSERT용 값 변수들
  v_join_date   DATE;
  v_bday        DATE;
  v_bank        VARCHAR2(50);
  v_acct_id     VARCHAR2(50);
  v_goal_type   NUMBER;
  v_goal_amt    NUMBER;
  v_cons_type   NUMBER;
  v_cons_amt    NUMBER;
  v_cons_desc   VARCHAR2(100);
  v_cons_date   DATE;

  ----------------------------------------------------------------
  -- 커서 (계좌 있는 자녀)
  ----------------------------------------------------------------
  CURSOR cur_children IS
    SELECT u.user_no, a.account_no
      FROM USERS u
      JOIN ACCOUNTS a ON a.user_no = u.user_no
     WHERE u.user_type = 1;

  ----------------------------------------------------------------
  -- 함수들
  ----------------------------------------------------------------
  FUNCTION random_join_date RETURN DATE IS
  BEGIN
    RETURN TRUNC(SYSDATE) - TRUNC(DBMS_RANDOM.VALUE(0,365))
           + (TRUNC(DBMS_RANDOM.VALUE(0,24*60))/(24*60));
  END;

  FUNCTION random_birthday_parent RETURN DATE IS
  BEGIN
    RETURN ADD_MONTHS(DATE '1975-01-01', TRUNC(DBMS_RANDOM.VALUE(0, 16*12)))
           + TRUNC(DBMS_RANDOM.VALUE(0,28));
  END;

  FUNCTION random_birthday_child RETURN DATE IS
  BEGIN
    RETURN ADD_MONTHS(DATE '2007-01-01', TRUNC(DBMS_RANDOM.VALUE(0, 12*12)))
           + TRUNC(DBMS_RANDOM.VALUE(0,28));
  END;

  FUNCTION pick_bank RETURN VARCHAR2 IS
    v_r NUMBER := DBMS_RANDOM.VALUE(0,100);
  BEGIN
    IF v_r < 15 THEN RETURN '용하은행';
    ELSIF v_r < 30 THEN RETURN '식스뱅크';
    ELSIF v_r < 45 THEN RETURN '내일은행';
    ELSIF v_r < 60 THEN RETURN '다시봄은행';
    ELSIF v_r < 75 THEN RETURN '큐리뱅크';
    ELSIF v_r < 90 THEN RETURN '무드뱅크';
    ELSE               RETURN '딱돼은행';
    END IF;
  END;

  FUNCTION gen_parent_acnt_id(p_idx PLS_INTEGER) RETURN VARCHAR2 IS
  BEGIN
    RETURN '110-'||TO_CHAR(p_idx,'FM000')||'-'||TO_CHAR(p_idx+1000,'FM000000');
  END;

  FUNCTION gen_child_acnt_id(p_idx PLS_INTEGER) RETURN VARCHAR2 IS
  BEGIN
    RETURN '333-'||TO_CHAR(p_idx,'FM000')||'-'||TO_CHAR(p_idx+2000,'FM000000');
  END;

  FUNCTION pick_goal_type RETURN NUMBER IS
    v_r NUMBER := DBMS_RANDOM.VALUE(0,100);
  BEGIN
    IF v_r < 40 THEN RETURN 1;    -- 전자기기
    ELSIF v_r < 60 THEN RETURN 3; -- 게임
    ELSIF v_r < 75 THEN RETURN 2; -- 의류
    ELSIF v_r < 87 THEN RETURN 4; -- 여행
    ELSIF v_r < 93 THEN RETURN 5; -- 파티
    ELSE              RETURN 0;   -- 기타
    END IF;
  END;

  FUNCTION goal_amount RETURN NUMBER IS
  BEGIN
    RETURN (TRUNC(DBMS_RANDOM.VALUE(50000, 500001))/100)*100;
  END;

  -- 소비 카테고리 확률 재조정
  FUNCTION pick_cons_type RETURN NUMBER IS
    v_r NUMBER := DBMS_RANDOM.VALUE(0,100);
  BEGIN
    IF v_r < 35 THEN RETURN 4;       -- 식비
    ELSIF v_r < 55 THEN RETURN 5;    -- 교통
    ELSIF v_r < 80 THEN RETURN 6;    -- 취미/여가
    ELSIF v_r < 90 THEN RETURN 2;    -- 의류
    ELSIF v_r < 95 THEN RETURN 1;    -- 전자기기
    ELSE RETURN 3;                    -- 게임/콘텐츠
    END IF;
  END;

  FUNCTION amount_by_type(p_type NUMBER) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    CASE p_type
      WHEN 4 THEN v := TRUNC(DBMS_RANDOM.VALUE(3000,15001));     -- 식비
      WHEN 5 THEN v := TRUNC(DBMS_RANDOM.VALUE(1000,5001));      -- 교통
      WHEN 1 THEN v := TRUNC(DBMS_RANDOM.VALUE(50000,150001));   -- 전자
      WHEN 2 THEN v := TRUNC(DBMS_RANDOM.VALUE(10000,80001));    -- 의류
      WHEN 3 THEN v := TRUNC(DBMS_RANDOM.VALUE(5000,50001));     -- 게임
      ELSE        v := TRUNC(DBMS_RANDOM.VALUE(1000,30001));    -- 기타
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

  FUNCTION random_date_in_month(p_base DATE, p_months_ago NUMBER, p_cons_type NUMBER) RETURN DATE IS
    v_first   DATE   := TRUNC(ADD_MONTHS(p_base, -p_months_ago), 'MM');
    v_last    DATE   := LEAST(LAST_DAY(v_first), TRUNC(SYSDATE));
    v_days    PLS_INTEGER := (v_last - v_first) + 1;
    v_off_day PLS_INTEGER;
    v_off_min PLS_INTEGER := TRUNC(DBMS_RANDOM.VALUE(0, 1440));
  BEGIN
    IF p_cons_type IN (1,3,2) THEN
        -- 전자기기/게임/의류: 월급일 전후 집중
        v_off_day := TRUNC(DBMS_RANDOM.VALUE(25, LEAST(v_days,28)));
    ELSE
        -- 식비/교통/취미/기타: 주말/주중 혼합
        LOOP
          v_off_day := TRUNC(DBMS_RANDOM.VALUE(0,v_days));
          EXIT;  -- 단순 랜덤 분포
        END LOOP;
    END IF;
    RETURN v_first + v_off_day + (v_off_min / 1440);
  END;

BEGIN
  DBMS_RANDOM.SEED(TO_NUMBER(TO_CHAR(SYSTIMESTAMP,'SSSSS')));

  ------------------------------------------------------------------
  -- 부모 생성 + 계좌
  ------------------------------------------------------------------
  FOR i IN 1..c_num_parents LOOP
    v_join_date := random_join_date;
    v_bday      := random_birthday_parent;
    v_bank      := pick_bank;
    v_acct_id   := gen_parent_acnt_id(i);

    INSERT INTO USERS (user_id, password, user_name, profile, birthday, email, tel, user_type, join_date)
    VALUES ('parent'||TO_CHAR(i,'FM000'),'pw123','부모'||i,MOD(i,4),
            v_bday,'parent'||i||'@example.com',
            '010-1'||TO_CHAR(i,'FM000000'),0,v_join_date)
    RETURNING user_no INTO v_parent_user_no;

    INSERT INTO ACCOUNTS (user_no, account_id, balance, bank_name)
    VALUES (v_parent_user_no, v_acct_id,
            TRUNC(DBMS_RANDOM.VALUE(500000, 2000001)), v_bank)
    RETURNING account_no INTO v_parent_acct_no;
  END LOOP;

  ------------------------------------------------------------------
  -- 자녀 생성 + 계좌 + 목표 + Childuser(point=0)
  ------------------------------------------------------------------
  FOR i IN 1..c_num_children LOOP
    v_join_date := random_join_date;
    v_bday      := random_birthday_child;
    v_bank      := pick_bank;
    v_acct_id   := gen_child_acnt_id(i);

    INSERT INTO USERS (user_id, password, user_name, profile, birthday, email, tel, user_type, join_date)
    VALUES ('child'||TO_CHAR(i,'FM000'),'pw123','자녀'||i,MOD(i,4),
            v_bday,'child'||i||'@example.com',
            '010-2'||TO_CHAR(i,'FM000000'),1,v_join_date)
    RETURNING user_no INTO v_child_user_no;

    INSERT INTO ACCOUNTS (user_no, account_id, balance, bank_name)
    VALUES (v_child_user_no, v_acct_id,
            TRUNC(DBMS_RANDOM.VALUE(0, 100001)), v_bank)
    RETURNING account_no INTO v_child_acct_no;

    v_goal_type := pick_goal_type;
    v_goal_amt  := goal_amount;
    INSERT INTO GOALHISTORY (child_no, goal_type, goal_name, target_amount, start_date, end_date, achieved)
    VALUES (v_child_user_no, v_goal_type, '현재목표-'||i, v_goal_amt,
            SYSDATE - TRUNC(DBMS_RANDOM.VALUE(5,180)), SYSDATE + TRUNC(DBMS_RANDOM.VALUE(5,180)), 0)
    RETURNING goal_no INTO v_goal_no;

    INSERT INTO CHILDUSER (child_no, current_goal_no, point)
    VALUES (v_child_user_no, v_goal_no, 0);

    FOR k IN 1 .. TRUNC(DBMS_RANDOM.VALUE(1,3)) LOOP
      v_goal_type := pick_goal_type;
      v_goal_amt  := goal_amount;
      INSERT INTO GOALHISTORY (child_no, goal_type, goal_name, target_amount,
                               start_date, end_date, achieved)
      VALUES (v_child_user_no, v_goal_type, '완료목표-'||i||'-'||k, v_goal_amt,
              SYSDATE - TRUNC(DBMS_RANDOM.VALUE(60,300)),
              SYSDATE - TRUNC(DBMS_RANDOM.VALUE(1,59)), 1);
    END LOOP;
  END LOOP;

  ------------------------------------------------------------------
  -- 관리자 생성
  ------------------------------------------------------------------
  FOR i IN 1..c_num_admins LOOP
    v_join_date := random_join_date;
    INSERT INTO USERS (user_id, password, user_name, profile, birthday, email, tel, user_type, join_date)
    VALUES ('admin'||TO_CHAR(i,'FM00'),'pw123','관리자'||i,0,
            DATE '1980-01-01' + (i-1),'admin'||i||'@example.com',
            '010-9'||TO_CHAR(i,'FM000000'),2,v_join_date)
    RETURNING user_no INTO v_admin_user_no;
  END LOOP;

  ------------------------------------------------------------------
  -- Family 매핑
  ------------------------------------------------------------------
  FOR i IN 1..c_num_children LOOP
    SELECT user_no INTO v_child_user_no
      FROM USERS WHERE user_id = 'child'||TO_CHAR(i,'FM000');

    SELECT user_no INTO v_parent_user_no
      FROM (SELECT user_no FROM USERS WHERE user_type = 0 ORDER BY DBMS_RANDOM.VALUE)
     WHERE ROWNUM = 1;

    INSERT INTO FAMILY(parent_no, child_no) VALUES (v_parent_user_no, v_child_user_no);
  END LOOP;

  ------------------------------------------------------------------
  -- 부모→자녀 이체 / 자동이체
  ------------------------------------------------------------------
  FOR i IN 1..c_num_children LOOP
    SELECT a.account_no INTO v_child_acct_no
      FROM ACCOUNTS a
     WHERE a.user_no = (SELECT user_no FROM USERS WHERE user_id = 'child'||TO_CHAR(i,'FM000'));

    SELECT a2.account_no INTO v_parent_acct_no
      FROM ACCOUNTS a2
     WHERE a2.user_no = (
       SELECT f.parent_no FROM FAMILY f
        WHERE f.child_no = (SELECT user_no FROM USERS WHERE user_id = 'child'||TO_CHAR(i,'FM000'))
        FETCH FIRST 1 ROWS ONLY
     );

    INSERT INTO TRANSFER(from_account_no, to_account_no, amount, trans_desc, trans_date)
    VALUES (v_parent_acct_no, v_child_acct_no,
            TRUNC(DBMS_RANDOM.VALUE(5000,50001)),
            '용돈 이체',
            SYSDATE - TRUNC(DBMS_RANDOM.VALUE(1,30)));

    INSERT INTO AUTOTRANSFER(from_account_no, to_account_no, amount, trans_cycle, last_trans_date)
    VALUES (v_parent_acct_no, v_child_acct_no,
            TRUNC(DBMS_RANDOM.VALUE(50000,500001)), 30,
            SYSDATE - TRUNC(DBMS_RANDOM.VALUE(1,30)));
  END LOOP;

  ------------------------------------------------------------------
  -- 자녀 소비 (최근 12개월, used_point=0)
  ------------------------------------------------------------------
  FOR c IN cur_children LOOP
    FOR m IN REVERSE 0 .. (c_months_back - 1) LOOP
      DECLARE
        v_tx_count PLS_INTEGER := TRUNC(DBMS_RANDOM.VALUE(c_min_tx_per_month, c_max_tx_per_month + 1));
      BEGIN
        FOR k IN 1 .. v_tx_count LOOP
          v_cons_type := pick_cons_type;
          v_cons_amt  := amount_by_type(v_cons_type);
          v_cons_desc := desc_by_type(v_cons_type);
          v_cons_date := random_date_in_month(SYSDATE, m, v_cons_type);
          IF v_cons_date > SYSDATE THEN
              v_cons_date := TRUNC(SYSDATE) + (DBMS_RANDOM.VALUE(0, 1440) / 1440);
          END IF;
          INSERT INTO CONSUME(account_no, amount, cons_desc, cons_type, cons_date, used_point)
          VALUES (c.account_no, v_cons_amt, v_cons_desc, v_cons_type, v_cons_date, 0);
        END LOOP;
      END;
    END LOOP;
  END LOOP;

  COMMIT;
END;
/