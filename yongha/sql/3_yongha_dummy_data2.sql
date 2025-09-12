-- 더미 데이터 생성 (Oracle PL/SQL) - 2025-08-21
DECLARE
  v_parent1   Users.user_no%TYPE;
  v_parent2   Users.user_no%TYPE;
  v_admin1    Users.user_no%TYPE;
  v_child1    Users.user_no%TYPE;
  v_child2    Users.user_no%TYPE;
  v_child3    Users.user_no%TYPE;
  v_child4    Users.user_no%TYPE;

  v_acc_p1    Accounts.account_no%TYPE;
  v_acc_p2    Accounts.account_no%TYPE;
  v_acc_c1    Accounts.account_no%TYPE;
  v_acc_c2    Accounts.account_no%TYPE;
  v_acc_c3    Accounts.account_no%TYPE;

  v_g_c1_active  GoalHistory.goal_no%TYPE;
  v_g_c1_done    GoalHistory.goal_no%TYPE;
  v_g_c2_active  GoalHistory.goal_no%TYPE;
  v_g_c3_done    GoalHistory.goal_no%TYPE;
BEGIN
  -------------------------------------------------------------------
  -- Users (부모 2, 자녀 4, 관리자 1)
  -- profile/user_type는 CODE 테이블( PROFILE:0~3 / USER_TYPE: 0=부모,1=자녀,2=관리자 )과 일치해야 함
  -------------------------------------------------------------------
  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('parent01','pw123','부모01', 1, DATE '1985-01-15', 'parent01@example.com', '010-1111-1111', 0)
  RETURNING user_no INTO v_parent1;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('parent02','pw123','부모02', 2, DATE '1983-06-10', 'parent02@example.com', '010-2222-2222', 0)
  RETURNING user_no INTO v_parent2;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('child01','pw123','자녀01', 0, DATE '2012-04-02', 'child01@example.com', '010-3001-0001', 1)
  RETURNING user_no INTO v_child1;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('child02','pw123','자녀02', 3, DATE '2013-08-21', 'child02@example.com', '010-3002-0002', 1)
  RETURNING user_no INTO v_child2;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('child03','pw123','자녀03', 2, DATE '2014-12-11', 'child03@example.com', '010-3003-0003', 1)
  RETURNING user_no INTO v_child3;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('child04','pw123','자녀04', 1, DATE '2015-03-27', 'child04@example.com', '010-3004-0004', 1)
  RETURNING user_no INTO v_child4;

  INSERT INTO Users(user_id, password, user_name, profile, birthday, email, tel, user_type)
  VALUES ('admin01','pw123','관리자01', 0, DATE '1980-01-01', 'admin01@example.com', '010-9999-9999', 2)
  RETURNING user_no INTO v_admin1;

  -------------------------------------------------------------------
  -- Parentuser (부모 부가정보) – 선택 사항
  -------------------------------------------------------------------
  INSERT INTO Parentuser(parent_no, mail_cycle) VALUES (v_parent1, 0);
  INSERT INTO Parentuser(parent_no, mail_cycle) VALUES (v_parent2, 2);

  -------------------------------------------------------------------
  -- Accounts (부모 2, 자녀 3에게 계좌 부여)
  -------------------------------------------------------------------
  INSERT INTO Accounts(user_no, account_id, balance, bank_name)
  VALUES (v_parent1, '110-0001-000001', 1000000, 'Shinhan')
  RETURNING account_no INTO v_acc_p1;

  INSERT INTO Accounts(user_no, account_id, balance, bank_name)
  VALUES (v_parent2, '110-0002-000002',  800000, 'Kookmin')
  RETURNING account_no INTO v_acc_p2;

  INSERT INTO Accounts(user_no, account_id, balance, bank_name)
  VALUES (v_child1, '333-1001-000003',   50000, 'Woori')
  RETURNING account_no INTO v_acc_c1;

  INSERT INTO Accounts(user_no, account_id, balance, bank_name)
  VALUES (v_child2, '333-1002-000004',   30000, 'Hana')
  RETURNING account_no INTO v_acc_c2;

  INSERT INTO Accounts(user_no, account_id, balance, bank_name)
  VALUES (v_child3, '333-1003-000005',       0, 'Shinhan')
  RETURNING account_no INTO v_acc_c3;

  -------------------------------------------------------------------
  -- Family (부모-자녀 매핑)  ※ FK ON DELETE SET NULL
  -------------------------------------------------------------------
  INSERT INTO Family(parent_no, child_no) VALUES (v_parent1, v_child1);
  INSERT INTO Family(parent_no, child_no) VALUES (v_parent1, v_child2);
  INSERT INTO Family(parent_no, child_no) VALUES (v_parent2, v_child3);
  INSERT INTO Family(parent_no, child_no) VALUES (v_parent2, v_child4);

  -------------------------------------------------------------------
  -- GoalHistory (자녀별 진행/완료 목표) 
  -- goal_type: GOAL_TYPE 코드(0=기타,1=전자기기,2=의류,3=게임,4=여행)
  -------------------------------------------------------------------
  -- child01: 진행 중 1건, 과거 완료 1건
  INSERT INTO GoalHistory(child_no, goal_type, goal_name, target_amount, start_date, achieved)
  VALUES (v_child1, 1, '닌텐도 스위치', 300000, SYSDATE-15, 0)
  RETURNING goal_no INTO v_g_c1_active;

  INSERT INTO GoalHistory(child_no, goal_type, goal_name, target_amount, start_date, end_date, achieved)
  VALUES (v_child1, 4, '강릉 여행', 50000, SYSDATE-90, SYSDATE-60, 0)
  RETURNING goal_no INTO v_g_c1_done;

  -- child02: 진행 중 1건
  INSERT INTO GoalHistory(child_no, goal_type, goal_name, target_amount, start_date, achieved)
  VALUES (v_child2, 0, '자전거', 200000, SYSDATE-10, 0)
  RETURNING goal_no INTO v_g_c2_active;

  -- child03: 과거 완료 1건(현재 목표 없음)
  INSERT INTO GoalHistory(child_no, goal_type, goal_name, target_amount, start_date, end_date, achieved)
  VALUES (v_child3, 2, '운동화', 70000, SYSDATE-80, SYSDATE-50, 0)
  RETURNING goal_no INTO v_g_c3_done;

  -------------------------------------------------------------------
  -- Childuser (자녀 1명당 1행, 현재 진행 목표 매핑 + 포인트 초기값)
  -------------------------------------------------------------------
  INSERT INTO Childuser(child_no, current_goal_no, point) VALUES (v_child1, v_g_c1_active, 0);
  INSERT INTO Childuser(child_no, current_goal_no, point) VALUES (v_child2, v_g_c2_active, 0);
  INSERT INTO Childuser(child_no, current_goal_no, point) VALUES (v_child3, NULL, 0);
  INSERT INTO Childuser(child_no, current_goal_no, point) VALUES (v_child4, NULL, 0);

  -------------------------------------------------------------------
  -- 완료 처리(트리거로 포인트 적립 발생: 목표금액의 3% → FLOOR)
  -- child01의 '강릉 여행', child03의 '운동화' 완료
  -------------------------------------------------------------------
  UPDATE GoalHistory SET achieved = 1, end_date = SYSDATE-60 WHERE goal_no = v_g_c1_done;
  UPDATE GoalHistory SET achieved = 1, end_date = SYSDATE-50 WHERE goal_no = v_g_c3_done;

  -------------------------------------------------------------------
  -- Transfer (부모 → 자녀 이체)
  -------------------------------------------------------------------
  INSERT INTO Transfer(from_account_no, to_account_no, amount, trans_desc)
  VALUES (v_acc_p1, v_acc_c1, 20000, '용돈 이체 - child01');

  INSERT INTO Transfer(from_account_no, to_account_no, amount, trans_desc)
  VALUES (v_acc_p1, v_acc_c2, 15000, '용돈 이체 - child02');

  INSERT INTO Transfer(from_account_no, to_account_no, amount, trans_desc)
  VALUES (v_acc_p2, v_acc_c3, 10000, '용돈 이체 - child03');

  -------------------------------------------------------------------
  -- AutoTransfer (부모 → 자녀 자동이체 예시)  trans_cycle 단위는 프로젝트 규칙에 따름
  -------------------------------------------------------------------
  INSERT INTO AutoTransfer(from_account_no, to_account_no, amount, trans_cycle, last_trans_date)
  VALUES (v_acc_p1, v_acc_c1, 50000, 30, SYSDATE-1);

  -------------------------------------------------------------------
  -- Consume (자녀 계좌 소비)
  -- cons_type: CONS_TYPE 코드(0=기타,1=전자기기,2=의류,3=게임,4=식비,5=교통비)
  -------------------------------------------------------------------
  INSERT INTO Consume(account_no, amount, cons_desc, cons_type, cons_date, used_point)
  VALUES (v_acc_c1, 12000, '점심 식비', 4, SYSDATE-1, 0);

  INSERT INTO Consume(account_no, amount, cons_desc, cons_type, cons_date, used_point)
  VALUES (v_acc_c1,  5000, '버스 교통비', 5, SYSDATE-1, 0);

  INSERT INTO Consume(account_no, amount, cons_desc, cons_type, cons_date, used_point)
  VALUES (v_acc_c2, 15000, '휴대폰 액세서리', 1, SYSDATE-2, 0);

  COMMIT;
END;
/
