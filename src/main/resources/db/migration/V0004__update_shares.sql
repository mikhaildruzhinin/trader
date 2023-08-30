alter table trader.shares
alter column quantity drop not null,
alter column quantity drop default,
alter column schema_version set default 4;
-- skip 3 to align schema_version with migration number
