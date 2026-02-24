-- 0.0.1

CREATE TABLE `user` (
    id                 INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username           VARCHAR(100) NOT NULL,
    password           VARCHAR(100) NOT NULL,
    full_name          VARCHAR(100) NULL,
    mail               VARCHAR(100) NULL,
    role               VARCHAR(100) NULL,
    aws_key            VARCHAR(200) NULL,
    aws_secret_key     VARCHAR(200) NULL,
    save_keys          BOOLEAN,
    export_to_s3       BOOLEAN,
    s3_bucket          VARCHAR(200) NULL,
    export_input_to_s3 BOOLEAN,
    activation_code    VARCHAR(200) NULL,
    active             BOOLEAN
);

CREATE TABLE job (
    id         VARCHAR(100) NOT NULL PRIMARY KEY,
    state      INTEGER NOT NULL,
    start_time BIGINT NOT NULL,
    end_time   BIGINT NOT NULL,
    name       VARCHAR(300),
    s3_url     VARCHAR(300),
    type       INTEGER,
    user_id    INTEGER NOT NULL REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE parameter (
    id       INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    `value`  VARCHAR(200) NOT NULL,
    type     VARCHAR(25) NOT NULL,
    format   VARCHAR(25) NULL,
    input    BOOLEAN,
    download BOOLEAN,
    variable VARCHAR(100) NOT NULL,
    job_id   VARCHAR(100) NOT NULL REFERENCES job(id) ON DELETE CASCADE
);

CREATE TABLE steps (
    id         INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    state      INTEGER NOT NULL,
    name       VARCHAR(300),
    start_time BIGINT NOT NULL,
    end_time   BIGINT NOT NULL,
    job_id     VARCHAR(100) NOT NULL
);

CREATE TABLE log_messages (
    id      INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    time    BIGINT NOT NULL,
    type    INTEGER NOT NULL,
    message VARCHAR(1000),
    step_id INTEGER NOT NULL REFERENCES steps(id) ON DELETE CASCADE
);


-- 1.0.3

CREATE TABLE downloads (
    id           INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parameter_id VARCHAR(200) NOT NULL,
    job_id       VARCHAR(100) NOT NULL,
    name         VARCHAR(200) NULL,
    path         VARCHAR(200) NULL,
    hash         VARCHAR(200) NULL,
    count        INTEGER NOT NULL,
    size         VARCHAR(200) NULL
);

-- 1.0.5

CREATE TABLE cache_entries (
    id             INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    signature      VARCHAR(300),
    used           INTEGER NOT NULL,
    last_used_on   BIGINT NOT NULL,
    created_on     BIGINT NOT NULL,
    execution_time BIGINT NOT NULL,
    size           BIGINT NOT NULL,
    user_id        VARCHAR(100) NULL,
    job_id         VARCHAR(100) NULL,
    output         VARCHAR(1000)
);

-- 1.0.7

CREATE TABLE counters (
    id      INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(300),
    job_id  VARCHAR(100) NULL,
    `value` BIGINT NOT NULL
);


-- 1.9.0

CREATE TABLE counters_history (
    id         INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    time_stamp BIGINT NOT NULL,
    name       VARCHAR(300),
    `value`    BIGINT NOT NULL
);

-- 1.9.1

CREATE TABLE html_snippets (
    id    INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `key` VARCHAR(300),
    text  VARCHAR(1000)
);

-- 1.9.2

ALTER TABLE job ADD COLUMN deleted_on BIGINT NULL DEFAULT NULL;

-- 1.9.3

ALTER TABLE parameter ADD COLUMN admin_only BOOLEAN NOT NULL DEFAULT false;

-- 1.9.6

ALTER TABLE job ADD COLUMN application VARCHAR(300) NULL DEFAULT NULL;

-- 1.9.8

ALTER TABLE job ADD COLUMN application_id VARCHAR(300) NULL DEFAULT NULL;

-- 1.16.0

ALTER TABLE `user` ADD COLUMN api_token VARCHAR(300) NULL DEFAULT NULL;

-- 1.19.0

ALTER TABLE `user` ADD COLUMN login_attempts INTEGER NULL DEFAULT 0;
ALTER TABLE `user` ADD COLUMN locked_until   TIMESTAMP NULL DEFAULT NULL;
ALTER TABLE `user` ADD COLUMN last_login     TIMESTAMP NULL DEFAULT NULL;

-- 1.26.0

ALTER TABLE job ADD COLUMN submitted_on     BIGINT NOT NULL DEFAULT 0;
ALTER TABLE job ADD COLUMN finished_on      BIGINT NOT NULL DEFAULT 0;
ALTER TABLE job ADD COLUMN setup_start_time BIGINT NOT NULL DEFAULT 0;
ALTER TABLE job ADD COLUMN setup_end_time   BIGINT NOT NULL DEFAULT 0;

-- 2.0.0-rc3

ALTER TABLE downloads MODIFY parameter_id INTEGER;

CREATE INDEX idx_downloads_parameter_id ON downloads(parameter_id);
CREATE INDEX idx_parameter_job_id       ON parameter(job_id,input);
CREATE INDEX idx_steps_job_id           ON steps(job_id);
CREATE INDEX idx_log_messages_step_id   ON log_messages(step_id);
CREATE INDEX idx_job_user_id            ON job(user_id,state);

-- 2.0.0

ALTER TABLE log_messages MODIFY COLUMN message TEXT;

-- 2.3.4

ALTER TABLE html_snippets MODIFY COLUMN text VARCHAR(8000);

-- 2.3.7

ALTER TABLE job ADD COLUMN user_agent VARCHAR(400);

-- 2.6.0

ALTER TABLE `user` ADD COLUMN api_token_expires_on TIMESTAMP NULL DEFAULT NULL;

-- 2.8.1

CREATE INDEX idx_downloads_hash ON downloads(hash);
CREATE INDEX idx_downloads_path ON downloads(path);
CREATE INDEX idx_user_username  ON `user`(username);
CREATE INDEX idx_user_mail      ON `user`(mail);
CREATE INDEX idx_user_fullname  ON `user`(full_name);

-- 3.0.0-beta5

ALTER TABLE `parameter` ADD COLUMN hash VARCHAR(300) NULL DEFAULT NULL;

-- 3.0.0-rc2

CREATE TABLE job_values (
    id      INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(300),
    job_id  VARCHAR(100) NULL,
    `value` VARCHAR(300) NOT NULL
);

-- 3.0.1

ALTER TABLE parameter MODIFY `value` VARCHAR(200) NULL;

-- 3.1.4-statgen.12

CREATE TABLE banners (
    type     ENUM('warning', 'danger') NOT NULL,
    message  VARCHAR(1000) NOT NULL,
    position INTEGER NOT NULL,
    id       INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY
);

-- 3.1.4-statgen.13

ALTER TABLE job ADD COLUMN completion_state ENUM('submitted', 'running', 'complete', 'retired') NULL DEFAULT NULL;
ALTER TABLE job ADD COLUMN success_state ENUM('pending', 'succeeded', 'failed', 'canceled') NULL DEFAULT NULL;
ALTER TABLE job ADD COLUMN notification_state ENUM('pending', 'notified_completion', 'sent_retirement_reminder', 'notified_retirement') NULL DEFAULT NULL;
