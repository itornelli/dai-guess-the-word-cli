create extension if not exists pgcrypto;

alter table players
add column if not exists token uuid not null default gen_random_uuid();

alter table players
add constraint uk_players_token unique (token);

