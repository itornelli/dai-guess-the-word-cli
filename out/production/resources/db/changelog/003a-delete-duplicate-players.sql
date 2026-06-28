delete from players
where id not in (
    select min(id) from players group by name
);

