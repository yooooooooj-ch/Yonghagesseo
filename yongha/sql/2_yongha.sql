
COMMIT;

--------------------------------------------------
------------------- 테이블 ------------------------
--------------------------------------------------
-- 테이블 제거
DROP TABLE Banner;
DROP TABLE Parentuser;
DROP TABLE Childuser;
DROP TABLE GoalHistory;
DROP TABLE Consume;
DROP TABLE Family;
DROP TABLE Transfer;
DROP TABLE AutoTransfer;
DROP TABLE Accounts;
DROP TABLE Users;


-- 테이블 생성
-- 사용자 테이블
CREATE TABLE Users (
    user_no     NUMBER PRIMARY KEY,
    user_id     VARCHAR2(20) NOT NULL UNIQUE,
    password    VARCHAR2(100) NOT NULL,
    user_name   VARCHAR2(50) NOT NULL,
    profile     NUMBER,
    birthday    DATE NOT NULL,
    email       VARCHAR2(100),
    address     VARCHAR2(200),
    tel         VARCHAR2(20),
    user_type   NUMBER NOT NULL,
    fcm_token   VARCHAR2(255),
    join_date   DATE DEFAULT SYSDATE
);


-- 계좌 테이블
CREATE TABLE Accounts (
    account_no   NUMBER PRIMARY KEY,  -- 계좌의 고정 ID
    user_no      NUMBER UNIQUE,       -- 사용자당 1계좌
    account_id   VARCHAR2(20) UNIQUE, -- 계좌번호 (변경 가능)
    balance      NUMBER DEFAULT 0 CHECK (balance >= 0),
    bank_name    VARCHAR2(50) NOT NULL,
    CONSTRAINT fk_account_user 
        FOREIGN KEY (user_no) REFERENCES Users(user_no) 
        ON DELETE CASCADE
);


-- Transfer 테이블
CREATE TABLE Transfer (
    trans_no       NUMBER PRIMARY KEY,
    from_account_no NUMBER,
    to_account_no   NUMBER,
    amount          NUMBER NOT NULL CHECK (amount >= 0),
    trans_date      DATE DEFAULT SYSDATE NOT NULL,
    trans_desc      VARCHAR2(200),
    CONSTRAINT fk_transfer_from FOREIGN KEY (from_account_no) REFERENCES Accounts(account_no) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_to   FOREIGN KEY (to_account_no)   REFERENCES Accounts(account_no) ON DELETE CASCADE
);


-- 자동이체 
CREATE TABLE AutoTransfer (
    auto_trans_no   NUMBER PRIMARY KEY,
    from_account_no NUMBER,
    to_account_no   NUMBER,
    amount          NUMBER(20, 0) DEFAULT 0 CHECK (amount >= 0) NOT NULL,
    trans_cycle     NUMBER DEFAULT 1 NOT NULL,
    last_trans_date DATE,
    CONSTRAINT fk_auto_from_account 
        FOREIGN KEY (from_account_no) REFERENCES Accounts(account_no) 
        ON DELETE CASCADE,
    CONSTRAINT fk_auto_to_account 
        FOREIGN KEY (to_account_no) REFERENCES Accounts(account_no) 
        ON DELETE CASCADE
);

-- 가족관계 (PK: 시퀀스 기반, FK는 SET NULL)
CREATE TABLE Family (
    family_no   NUMBER PRIMARY KEY,
    parent_no   NUMBER,
    child_no    NUMBER,
    CONSTRAINT fk_family_parent 
        FOREIGN KEY (parent_no) REFERENCES Users(user_no) 
        ON DELETE SET NULL,
    CONSTRAINT fk_family_child  
        FOREIGN KEY (child_no)  REFERENCES Users(user_no) 
        ON DELETE SET NULL
);

-- 소비내역
CREATE TABLE Consume (
    cons_no     NUMBER PRIMARY KEY,
    account_no  NUMBER,
    amount      NUMBER NOT NULL CHECK (amount >= 0),
    cons_desc   VARCHAR2(200),
    cons_type   NUMBER, -- 0: 기타 / 1: 전자기기 / 2: 의류 / 3: 게임 / 4: 식비 / 5: 교통비
    cons_date   DATE DEFAULT SYSDATE,
    used_point NUMBER DEFAULT 0 CHECK (used_point >= 0),
    CONSTRAINT fk_consume_account FOREIGN KEY (account_no) REFERENCES Accounts(account_no) ON DELETE CASCADE
);


-- 목표내역
CREATE TABLE GoalHistory (
    goal_no        NUMBER        PRIMARY KEY,
    child_no       NUMBER        NOT NULL,   -- 목표 소유자(자녀)
    goal_type      NUMBER,       -- 0: 기타 / 1: 전자기기 / 2: 의류 / 3: 게임
    goal_name      VARCHAR2(100) NOT NULL,
    target_amount  NUMBER(20,0)  NOT NULL CHECK (target_amount > 0),
    start_date     DATE          DEFAULT SYSDATE NOT NULL,
    end_date       DATE,
    achieved       NUMBER(1)     DEFAULT 0 NOT NULL CHECK (achieved IN (0, 1)),

    CONSTRAINT fk_goal_child
        FOREIGN KEY (child_no) REFERENCES Users(user_no)
        ON DELETE CASCADE
);


-- 자녀 정보 (목표, 포인트)
-- “자녀 1명당 1행” 보장: PK=child_no
CREATE TABLE Childuser (
  child_no         NUMBER       PRIMARY KEY,
  current_goal_no  NUMBER       NULL,        -- 현재 진행 목표(없으면 NULL)
  point            NUMBER(20,0) DEFAULT 0 NOT NULL,

  CONSTRAINT fk_childuser_child
    FOREIGN KEY (child_no) REFERENCES Users(user_no)
    ON DELETE CASCADE,

  -- goal 삭제 시 현재 목표 참조만 NULL로
  CONSTRAINT fk_childuser_current_goal
    FOREIGN KEY (current_goal_no) REFERENCES GoalHistory(goal_no)
    ON DELETE SET NULL
);

-- 부모 정보 (이메일 수신 주기)
CREATE TABLE Parentuser (
    parent_no   NUMBER  PRIMARY KEY,
    mail_cycle  NUMBER  DEFAULT 1, -- 0: 매일 / 1: 일주일 / 2: 한달
    last_send_date  DATE DEFAULT SYSDATE,
    
    CONSTRAINT fk_parentuser_parent
        FOREIGN KEY (parent_no) REFERENCES Users(user_no)
        ON DELETE CASCADE
);


-- 이벤트 배너
CREATE TABLE Banner (
    banner_no       NUMBER          PRIMARY KEY,               -- 시퀀스로 생성되는 배너번호
    img_path        VARCHAR2(200),                             -- 이미지 경로 (NULL 허용)
    banner_index    NUMBER          DEFAULT 0    NOT NULL,     -- 배너 인덱스 (0~9 범위 제한은 별도 제약 필요)
    start_date      DATE            DEFAULT SYSDATE NOT NULL,  -- 시작날짜 (기본: 오늘)
    end_date        DATE            DEFAULT (SYSDATE + 7) NOT NULL -- 종료날짜 (기본: 7일 뒤)
);


-- code 테이블이랑 각 타입 연결
-- users 테이블
-- 타입명 상수 컬럼 추가: 기본값 + NOT NULL + 해당 값만 허용
ALTER TABLE Users ADD profile_type_name VARCHAR2(30) DEFAULT 'PROFILE'   NOT NULL;
ALTER TABLE Users ADD user_type_name    VARCHAR2(30) DEFAULT 'USER_TYPE' NOT NULL;

ALTER TABLE Users ADD CONSTRAINT chk_profile_type_name CHECK (profile_type_name = 'PROFILE');
ALTER TABLE Users ADD CONSTRAINT chk_user_type_name    CHECK (user_type_name    = 'USER_TYPE');

-- 복합 FK 연결: (타입명, 숫자값)
ALTER TABLE Users ADD CONSTRAINT fk_users_profile_code
  FOREIGN KEY (profile_type_name, profile)
  REFERENCES CODE (type_name, code_value);

ALTER TABLE Users ADD CONSTRAINT fk_users_user_type_code
  FOREIGN KEY (user_type_name, user_type)
  REFERENCES CODE (type_name, code_value);


-- consume 테이블
ALTER TABLE Consume ADD cons_type_name VARCHAR2(30) DEFAULT 'CONS_TYPE' NOT NULL;
ALTER TABLE Consume ADD CONSTRAINT chk_cons_type_name CHECK (cons_type_name = 'CONS_TYPE');

ALTER TABLE Consume ADD CONSTRAINT fk_consume_cons_type
  FOREIGN KEY (cons_type_name, cons_type)
  REFERENCES CODE(type_name, code_value);
  
-- goal history 테이블
ALTER TABLE GoalHistory ADD goal_type_name VARCHAR2(30) DEFAULT 'GOAL_TYPE' NOT NULL;
ALTER TABLE GoalHistory ADD CONSTRAINT chk_goal_type_name CHECK (goal_type_name = 'GOAL_TYPE');

ALTER TABLE GoalHistory ADD CONSTRAINT fk_goalhistory_goal_type
  FOREIGN KEY (goal_type_name, goal_type)
  REFERENCES CODE(type_name, code_value);








-- 테이블 초기화
DELETE FROM Banner;
DELETE FROM GoalHistory;
DELETE FROM Consume;
DELETE FROM Parentuser;
DELETE FROM Childuser;
DELETE FROM Family;
DELETE FROM Transfer;
DELETE FROM AutoTransfer;
DELETE FROM Accounts;
DELETE FROM Users;


-- 테이블 확인
SELECT * FROM Users ORDER BY user_no desc;
SELECT * FROM Accounts;
SELECT * FROM AutoTransfer;
SELECT * FROM Transfer ORDER BY trans_no DESC;
SELECT * FROM Family ORDER BY family_no DESC;
SELECT * FROM Childuser;
SELECT * FROM Parentuser;
SELECT * FROM Consume ORDER BY cons_no DESC;
SELECT * FROM GoalHistory ORDER BY goal_no DESC;
SELECT * FROM Banner;


--------------------------------------------------
------------------- 시퀀스 ------------------------
--------------------------------------------------
-- 트리거 제거
DROP TRIGGER trg_user_no;
DROP TRIGGER trg_account_no;
DROP TRIGGER trg_trans_no;
DROP TRIGGER trg_auto_trans_no;
DROP TRIGGER trg_family_no;
DROP TRIGGER trg_cons_no;
DROP TRIGGER trg_goal_no;
DROP TRIGGER trg_banner_no;
DROP TRIGGER trg_goal_auto_complete;
DROP TRIGGER trg_goalhistory_reward;


-- 시퀀스 제거
DROP SEQUENCE seq_user_no;
DROP SEQUENCE seq_account_no;
DROP SEQUENCE seq_trans_no;
DROP SEQUENCE seq_auto_trans_no;
DROP SEQUENCE seq_family_no;
DROP SEQUENCE seq_cons_no;
DROP SEQUENCE seq_goal_no;
DROP SEQUENCE seq_banner_no;



-- 시퀀스생성
CREATE SEQUENCE seq_user_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_account_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_trans_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_auto_trans_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_family_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_cons_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_goal_no INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_banner_no START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 트리거 생성
CREATE OR REPLACE TRIGGER trg_user_no
BEFORE INSERT ON Users
FOR EACH ROW
BEGIN
    SELECT seq_user_no.NEXTVAL INTO :NEW.user_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_account_no
BEFORE INSERT ON Accounts
FOR EACH ROW
BEGIN
    SELECT seq_account_no.NEXTVAL INTO :NEW.account_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_trans_no
BEFORE INSERT ON Transfer
FOR EACH ROW
BEGIN
    SELECT seq_trans_no.NEXTVAL INTO :NEW.trans_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_auto_trans_no
BEFORE INSERT ON AutoTransfer
FOR EACH ROW
BEGIN
    SELECT seq_auto_trans_no.NEXTVAL INTO :NEW.auto_trans_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_family_no
BEFORE INSERT ON Family
FOR EACH ROW
BEGIN
    SELECT seq_family_no.NEXTVAL INTO :NEW.family_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_cons_no
BEFORE INSERT ON Consume
FOR EACH ROW
BEGIN
    SELECT seq_cons_no.NEXTVAL INTO :NEW.cons_no FROM dual;
END;
/

CREATE OR REPLACE TRIGGER trg_goal_no
BEFORE INSERT ON GoalHistory
FOR EACH ROW
BEGIN
    SELECT seq_goal_no.NEXTVAL INTO :NEW.goal_no FROM dual;
END;
/


CREATE OR REPLACE TRIGGER trg_banner_no
BEFORE INSERT ON Banner
FOR EACH ROW
BEGIN
    SELECT seq_banner_no.NEXTVAL INTO :NEW.banner_no FROM dual;
END;
/

-- 목표 자동 달성 트리거 (Accounts.balance 증가시에만 동작)
CREATE OR REPLACE TRIGGER trg_goal_auto_complete
AFTER UPDATE OF balance ON Accounts
FOR EACH ROW
BEGIN
  -- 해당 child의 진행 중 목표 중 (achieved=0) && (잔액>=목표액) → ready_to_complete=1
  UPDATE GoalHistory gh
     SET gh.achieved = 1,
         gh.end_date = SYSDATE 
   WHERE gh.child_no = :NEW.user_no
     AND gh.achieved = 0
     AND gh.target_amount <= :NEW.balance;
END;
/




-- 목표 달성시 포인트 자동적립 트리거
CREATE OR REPLACE TRIGGER trg_goalhistory_reward
AFTER UPDATE OF achieved ON GoalHistory
FOR EACH ROW
BEGIN
  -- 0 → 1로 변경되는 순간에만 적립
  IF NVL(:OLD.achieved,0) = 0 AND :NEW.achieved = 1 THEN
    UPDATE Childuser
       SET point = NVL(point,0) + FLOOR(:NEW.target_amount * 0.03)
     WHERE child_no = :NEW.child_no;
  END IF;
END;
/



-- 시퀀스 초기화
ALTER SEQUENCE seq_user_no RESTART START WITH 1;
ALTER SEQUENCE seq_trans_no RESTART START WITH 1;
ALTER SEQUENCE seq_auto_trans_no RESTART START WITH 1;
ALTER SEQUENCE seq_cons_no RESTART START WITH 1;
ALTER SEQUENCE seq_goal_no RESTART START WITH 1;
ALTER SEQUENCE seq_banner_no RESTART START WITH 1;




