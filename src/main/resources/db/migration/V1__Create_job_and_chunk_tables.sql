-- Create job table
CREATE TABLE job (
    id VARCHAR(36) PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    target JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
