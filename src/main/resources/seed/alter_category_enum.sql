-- 배포용 스키마 변경 스크립트 (수동 1회 실행)
-- Spring Boot가 자동 실행하지 않는 파일입니다. 배포 시 DB에 1회 수동으로 실행해주세요.
--
-- 목적: calendar_events.category / guides.category의 MySQL native ENUM 허용값에서
-- TOPIK을 TOPIK_APPLICATION, TOPIK_EXAM으로 교체합니다.
-- (EventCategory.TOPIK / GuideCategory.TOPIK은 코드에서 이미 TOPIK_APPLICATION, TOPIK_EXAM으로 분리되었습니다.)
--
-- 작성 시점 기준 운영 DB의 두 테이블 모두 데이터 0건으로 확인되어, 기존 TOPIK 값을 가진
-- 데이터를 변환하는 UPDATE 문은 포함하지 않았습니다. 만약 실제 배포 시점에 category = 'TOPIK'인
-- 행이 존재한다면, 이 스크립트 실행 전에 해당 행을 TOPIK_APPLICATION/TOPIK_EXAM 중 무엇으로
-- 옮길지 먼저 결정하고 별도의 UPDATE를 진행한 뒤 이 스크립트를 실행해야 합니다.
--
-- 배포 순서:
--   1) 이 스크립트(alter_category_enum.sql) 실행
--   2) initial_data.sql 실행
--   3) 애플리케이션 재배포/재시작

ALTER TABLE calendar_events
    MODIFY COLUMN category
    ENUM('ACADEMIC','LEGAL','TOPIK_APPLICATION','TOPIK_EXAM','VISA')
    NOT NULL;

ALTER TABLE guides
    MODIFY COLUMN category
    ENUM('ACADEMIC','LEGAL','TOPIK_APPLICATION','TOPIK_EXAM','VISA')
    NOT NULL;
