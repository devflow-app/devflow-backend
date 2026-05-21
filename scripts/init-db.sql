-- ============================================================
--  DevFlow — Database Initialization Script
--  Creates a separate database for each microservice
--  Executed automatically by Docker on first run
-- ============================================================

\c postgres;

-- Auth Service DB
CREATE DATABASE devflow_auth;
GRANT ALL PRIVILEGES ON DATABASE devflow_auth TO devflow;

-- Project Service DB
CREATE DATABASE devflow_projects;
GRANT ALL PRIVILEGES ON DATABASE devflow_projects TO devflow;

-- Notification Service DB
CREATE DATABASE devflow_notifications;
GRANT ALL PRIVILEGES ON DATABASE devflow_notifications TO devflow;

-- AI Service DB
CREATE DATABASE devflow_ai;
GRANT ALL PRIVILEGES ON DATABASE devflow_ai TO devflow;
