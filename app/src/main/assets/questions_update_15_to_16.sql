BEGIN;

ALTER TABLE topic ADD COLUMN version INT DEFAULT 1;

# topic_exam_settings

COMMIT;