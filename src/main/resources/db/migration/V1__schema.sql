CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  document_id VARCHAR(32) NOT NULL UNIQUE,
  registration_id VARCHAR(40) NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash CHAR(60) NOT NULL,
  role VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE workers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  document_id VARCHAR(32) NOT NULL UNIQUE,
  email VARCHAR(190) NULL,
  phone VARCHAR(40) NULL,
  gross_salary DECIMAL(15,2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_workers_salary CHECK (gross_salary >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_workers (
  user_id BIGINT NOT NULL,
  worker_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, worker_id),
  CONSTRAINT fk_user_workers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_workers_worker FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  worker_id BIGINT NOT NULL,
  description VARCHAR(120) NOT NULL,
  percentage DECIMAL(7,4) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_rates_worker_description UNIQUE (worker_id, description),
  CONSTRAINT fk_rates_worker FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
  CONSTRAINT chk_rates_percentage CHECK (percentage >= 0 AND percentage <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE receipts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  worker_id BIGINT NOT NULL,
  period_date DATE NOT NULL,
  gross_amount DECIMAL(15,2) NOT NULL,
  deductions_amount DECIMAL(15,2) NOT NULL,
  net_amount DECIMAL(15,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_receipts_worker_period UNIQUE (worker_id, period_date),
  CONSTRAINT fk_receipts_worker FOREIGN KEY (worker_id) REFERENCES workers(id),
  CONSTRAINT chk_receipts_amounts CHECK (gross_amount >= 0 AND deductions_amount >= 0 AND net_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE receipt_lines (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receipt_id BIGINT NOT NULL,
  description VARCHAR(120) NOT NULL,
  percentage DECIMAL(7,4) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  CONSTRAINT fk_receipt_lines_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
