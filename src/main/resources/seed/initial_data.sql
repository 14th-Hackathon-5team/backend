-- 배포 초기 공통 데이터 시드 스크립트
-- Spring Boot가 자동 실행하지 않는 파일입니다. 배포 시 DB에 1회 수동으로 실행해주세요.
-- (재시작마다 자동 실행되면 중복 삽입되므로 data.sql로 두지 않았습니다.)
--
-- 주의: 이 스크립트는 category 컬럼에 TOPIK_APPLICATION/TOPIK_EXAM 값을 사용합니다.
-- 반드시 alter_category_enum.sql을 먼저 실행해 category ENUM을 변경한 뒤 이 스크립트를 실행하세요.
--
-- TOPIK 일정 출처: topik.go.kr 공식 회차 안내표 (2026.08 기준, 한국 접수/응시 기준)
-- 이미 접수가 마감된 회차는 시험일이 지나지 않은 것만 포함했습니다.
-- 가이드 본문(content)은 초안이며, 배포 전 팀 검수가 필요합니다.

-- ============================
-- 캘린더: TOPIK 공통 일정
-- ============================

-- 제108회 PBT (접수마감, 시험일만 등록)
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제108회 PBT 시험일', 'TOPIK_EXAM', '2026-10-18', NULL, true,
        '제108회 TOPIK PBT 시험일입니다. 성적 발표는 12월 10일 예정입니다.', 'https://www.topik.go.kr');

-- 제15회 IBT
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제15회 IBT 접수기간', 'TOPIK_APPLICATION', '2026-08-18', '2026-08-24', true,
        '제15회 TOPIK IBT 접수기간입니다.', 'https://www.topik.go.kr');
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제15회 IBT 시험일', 'TOPIK_EXAM', '2026-10-24', NULL, true,
        '제15회 TOPIK IBT 시험일입니다. 성적 발표는 11월 13일 예정입니다.', 'https://www.topik.go.kr');

-- 제109회 PBT
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제109회 PBT 접수기간', 'TOPIK_APPLICATION', '2026-09-01', '2026-09-07', true,
        '제109회 TOPIK PBT 접수기간입니다.', 'https://www.topik.go.kr');
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제109회 PBT 시험일', 'TOPIK_EXAM', '2026-11-15', NULL, true,
        '제109회 TOPIK PBT 시험일입니다. 성적 발표는 12월 22일 예정입니다.', 'https://www.topik.go.kr');

-- 제16회 IBT
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제16회 IBT 접수기간', 'TOPIK_APPLICATION', '2026-09-15', '2026-09-21', true,
        '제16회 TOPIK IBT 접수기간입니다.', 'https://www.topik.go.kr');
INSERT INTO calendar_events (user_id, title, category, start_date, end_date, is_global, description, related_link)
VALUES (NULL, 'TOPIK 제16회 IBT 시험일', 'TOPIK_EXAM', '2026-11-28', NULL, true,
        '제16회 TOPIK IBT 시험일입니다. 성적 발표는 12월 18일 예정입니다.', 'https://www.topik.go.kr');

-- ============================
-- 세부정보: 카테고리별 가이드 (초안, 배포 전 검수 필요)
-- ============================

INSERT INTO guides (category, title, content, reference_url)
VALUES ('VISA', 'D-2/D-4 비자 안내',
        'D-2는 학위과정 유학생, D-4는 어학연수 등 일반연수 대상 체류자격입니다. 체류 가능 범위와 체류기간 상한이 비자 종류별로 다르니, 본인의 비자 종류를 확인하고 체류기간을 관리하세요.',
        'https://www.hikorea.go.kr');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('VISA', '외국인등록증 발급 절차',
        '90일 초과 체류 예정인 외국인은 입국 후 90일 이내에 외국인등록을 해야 합니다. 준비 서류와 발급 절차는 하이코리아에서 확인할 수 있습니다.',
        'https://www.hikorea.go.kr');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('TOPIK_APPLICATION', 'TOPIK 접수 방법 안내',
        'TOPIK 접수는 topik.go.kr에서 회차별 접수기간에 진행됩니다. 인기 시험장은 접수 시작 당일 조기 마감될 수 있으니 접수 첫날 신청을 권장합니다.',
        'https://www.topik.go.kr');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('TOPIK_EXAM', 'TOPIK 시험 당일 안내',
        '시험 당일 신분증과 수험표를 반드시 지참해야 하며, 입실 시간을 준수해야 합니다. 자세한 유의사항은 TOPIK 공식 홈페이지에서 확인하세요.',
        'https://www.topik.go.kr');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('LEGAL', '외국인등록 안내',
        '90일을 초과하여 체류할 외국인은 입국일로부터 90일 이내에 외국인등록을 해야 합니다. 미등록 시 불이익이 있을 수 있습니다.',
        'https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1029733341');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('LEGAL', '체류지 변경신고 안내',
        '이사 등으로 체류지가 바뀐 경우, 변경된 날부터 15일 이내에 체류지 변경신고를 해야 합니다. 신고하지 않으면 불이익을 받을 수 있습니다.',
        'https://www.law.go.kr/lsLinkCommonInfo.do?lsJoLnkSeq=1031822293');

INSERT INTO guides (category, title, content, reference_url)
VALUES ('ACADEMIC', '외국인 특별전형 기본 요건',
        '외국인 특별전형 지원을 위해서는 부모 국적, 고교 졸업 요건 등을 충족해야 합니다. 대학별로 세부 요건과 제출서류가 다르니 지원하려는 학교의 모집요강을 꼭 확인하세요.',
        'https://www.studyinkorea.go.kr');
