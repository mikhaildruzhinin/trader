create table if not exists trader.candles (
	id bigserial not null primary key,
	figi char(12) not null,
	open numeric(15, 9),
	close numeric(15, 9),
	high numeric(15, 9),
	low numeric(15, 9),
	time timestamp,
	deleted_flg bool not null default false,
	load_dttm timestamp not null default now()
);
