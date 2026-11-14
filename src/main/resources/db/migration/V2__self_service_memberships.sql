-- Existing deployments must explicitly baseline the reviewed V1 schema before V2.
-- MySQL DDL commits implicitly: backup and preflight are required before deployment.
ALTER TABLE users MODIFY fk_company_id BIGINT NULL,
    ADD COLUMN membership_active BIT(1) NOT NULL DEFAULT b'0';
ALTER TABLE user_sessions MODIFY company_id BIGINT NULL;

-- Preserve former global authority assignments as historical evidence.
CREATE TABLE legacy_admin_assignments (
    user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, company_id BIGINT NULL,
    old_role_name VARCHAR(20) NOT NULL, migrated_at DATETIME(6) NOT NULL,
    PRIMARY KEY(user_id,role_id)
) ENGINE=InnoDB;
INSERT INTO legacy_admin_assignments
SELECT ur.user_id,ur.role_id,u.fk_company_id,r.name,UTC_TIMESTAMP(6)
FROM user_roles ur JOIN roles r ON r.id=ur.role_id JOIN users u ON u.id=ur.user_id
WHERE r.name='ADMIN';
CREATE TABLE legacy_admin_roles AS SELECT * FROM roles WHERE name='ADMIN';
INSERT IGNORE INTO roles(name) VALUES ('WORKER'),('EMPLOYER');
INSERT IGNORE INTO user_roles(user_id,role_id)
SELECT a.user_id,r.id FROM legacy_admin_assignments a JOIN roles r
ON r.name=IF(a.company_id IS NULL,'WORKER','EMPLOYER');
DELETE ur FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='ADMIN';
DELETE FROM roles WHERE name='ADMIN';
ALTER TABLE roles MODIFY name ENUM('EMPLOYER','WORKER') NOT NULL;

-- Existing assigned users retain membership; roleless accounts must request approval.
UPDATE users u SET membership_active=IF(u.fk_company_id IS NOT NULL AND EXISTS
    (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id),b'1',b'0'),
    security_version=security_version+1;
INSERT INTO user_roles(user_id,role_id)
SELECT u.id,r.id FROM users u JOIN roles r ON r.name='WORKER'
WHERE NOT EXISTS(SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id);
UPDATE user_sessions SET revoked_at=COALESCE(revoked_at,UTC_TIMESTAMP(6));

CREATE UNIQUE INDEX uq_company_name ON companies(name);
CREATE TABLE membership_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL, company_id BIGINT NOT NULL,
    status ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED') NOT NULL,
    created_at DATETIME(6) NOT NULL, decided_at DATETIME(6) NULL, decided_by BIGINT NULL,
    pending_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN applicant_id ELSE NULL END) STORED,
    CONSTRAINT fk_request_applicant FOREIGN KEY(applicant_id) REFERENCES users(id),
    CONSTRAINT fk_request_company FOREIGN KEY(company_id) REFERENCES companies(id),
    UNIQUE KEY uq_pending_applicant(pending_user_id),
    INDEX idx_membership_company(company_id,status)
) ENGINE=InnoDB;
CREATE TABLE membership_audits (
    id BINARY(16) NOT NULL PRIMARY KEY, actor_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL, company_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL, occurred_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE operator_recovery_records (
    id BINARY(16) NOT NULL PRIMARY KEY, target_user_id BIGINT NOT NULL,
    operator_name VARCHAR(120) NOT NULL, action VARCHAR(40) NOT NULL,
    occurred_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
