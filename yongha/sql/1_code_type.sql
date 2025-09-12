DROP TABLE Code;
DROP TABLE Code_Type;

DROP SEQUENCE seq_code_no;
ALTER SEQUENCE seq_code_no RESTART START WITH 1;

-- 1) CODE_TYPE 먼저 만들고 즉시 시드 삽입
CREATE TABLE CODE_TYPE (
    type_name   VARCHAR2(30) PRIMARY KEY,
    type_desc   VARCHAR2(200),
    is_active   CHAR(1) DEFAULT 'Y' CHECK (is_active IN ('Y','N'))
);

INSERT INTO CODE_TYPE(type_name, type_desc, is_active) VALUES ('PROFILE',   '프로필 이미지 코드', 'Y');
INSERT INTO CODE_TYPE(type_name, type_desc, is_active) VALUES ('GOAL_TYPE', '목표 종류 코드',     'Y');
INSERT INTO CODE_TYPE(type_name, type_desc, is_active) VALUES ('CONS_TYPE', '소비 종류 코드',     'Y');
INSERT INTO CODE_TYPE(type_name, type_desc, is_active) VALUES ('USER_TYPE', '사용자 역할 코드',   'Y');

COMMIT;  -- 명시적 커밋(심리적 안전장치)





-- 2) CODE 생성 + 시드
CREATE TABLE CODE (
    code_no     NUMBER PRIMARY KEY,
    type_name   VARCHAR2(30) NOT NULL,
    code_value  NUMBER       NOT NULL,
    code_label  VARCHAR2(100) NOT NULL,
    code_desc   VARCHAR2(4000),
    sort_order  NUMBER DEFAULT 0 NOT NULL,
    is_active   CHAR(1) DEFAULT 'Y' CHECK (is_active IN ('Y','N')),
    CONSTRAINT uk_code UNIQUE (type_name, code_value),
    CONSTRAINT fk_code_type FOREIGN KEY (type_name) REFERENCES CODE_TYPE(type_name) ON DELETE CASCADE
);

CREATE SEQUENCE seq_code_no START WITH 1 INCREMENT BY 1 NOCACHE;


-- PROFILE
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'PROFILE', 0, '핑크용',  '핑크 아바타', 0, 'Y');

INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'PROFILE', 1, '파란용', '파랑 아바타', 1, 'Y');

INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'PROFILE', 2, '초록용', '초록 아바타', 2, 'Y');

INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'PROFILE', 3, '노란용', '노랑 아바타', 3, 'Y');

-- GOAL_TYPE
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'GOAL_TYPE', 0, '기타',     '📦', 0, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'GOAL_TYPE', 1, '전자기기', '💻', 1, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'GOAL_TYPE', 2, '의류',     '👕', 2, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'GOAL_TYPE', 3, '게임',     '🎮', 3, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'GOAL_TYPE', 4, '여행',     '✈️️', 4, 'Y');

-- CONS_TYPE
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 0, '기타',    '📦', 0, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 1, '전자기기', '💻', 1, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 2, '의류',    '👕', 2, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 3, '게임',    '🎮', 3, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 4, '식비',    '🍜', 4, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 5, '교통비',  '🚍', 5, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'CONS_TYPE', 6, '취미/여가','🎮', 6, 'Y');

-- USER_TYPE
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'USER_TYPE', 0, '부모',   NULL, 0, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'USER_TYPE', 1, '자녀',   NULL, 1, 'Y');
INSERT INTO CODE (code_no, type_name, code_value, code_label, code_desc, sort_order, is_active)
VALUES (SEQ_CODE_NO.NEXTVAL, 'USER_TYPE', 2, '관리자', NULL, 2, 'Y');

COMMIT;