alter table trader.shares
alter column figi type char(12),
alter column schema_version set default 2;
