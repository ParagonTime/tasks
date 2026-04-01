--liquibase formatted sql

--changeset developer:1
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

--changeset developer:2
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    assignee_id BIGINT,
    CONSTRAINT fk_tasks_user FOREIGN KEY (assignee_id) REFERENCES users (id)
);

--changeset developer:3
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);