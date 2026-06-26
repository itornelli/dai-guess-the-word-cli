--liquibase formatted sql

--changeset wurdal:001-create-players-table
CREATE TABLE players (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    games_won INT NOT NULL DEFAULT 0
);
