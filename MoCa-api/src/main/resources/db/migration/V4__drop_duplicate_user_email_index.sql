-- users.email 컬럼의 UNIQUE 제약(users_email_key)과 중복되는 인덱스 제거
DROP INDEX IF EXISTS idx_user_email;
