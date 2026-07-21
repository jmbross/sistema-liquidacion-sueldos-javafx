-- All records are deliberately fictitious and reserved for local demonstration.
INSERT INTO users (first_name, last_name, document_id, registration_id, email, password_hash, role) VALUES
('Admin', 'Demo', 'DEMO-ADMIN-001', 'DEMO-MAT-001', 'admin@demo.local', '$2a$12$1qPbrrvGG4HlV/bKyADv2e9Jk2GdozaZZK6AdOFhPY.HJiRuSZVDa', 'ADMIN'),
('Usuario', 'Demo', 'DEMO-USER-001', 'DEMO-MAT-002', 'usuario@demo.local', '$2a$12$dUQqD0GBfk/AmwpkPrYhiuDxnBXTGwBce0uam8DVNRRXr5FBfQqKC', 'USER');

INSERT INTO workers (first_name, last_name, document_id, email, phone, gross_salary) VALUES
('Empleado', 'Ejemplo Uno', 'DEMO-WORKER-001', 'empleado@demo.local', '000-000-0001', 1250000.00),
('Persona', 'Ejemplo Dos', 'DEMO-WORKER-002', 'persona@demo.local', '000-000-0002', 980000.00);

INSERT INTO user_workers (user_id, worker_id)
SELECT u.id, w.id FROM users u CROSS JOIN workers w WHERE u.email = 'usuario@demo.local' AND w.document_id = 'DEMO-WORKER-001';

INSERT INTO rates (worker_id, description, percentage)
SELECT id, 'Retirement demo', 11.0000 FROM workers
UNION ALL SELECT id, 'Health insurance demo', 3.0000 FROM workers;
