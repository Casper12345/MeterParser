CREATE TABLE meter_readings (
    id UUID DEFAULT gen_random_uuid() NOT NULL,
    nmi VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    consumption NUMERIC NOT NULL,
    CONSTRAINT meter_readings_pk PRIMARY KEY (id),
    CONSTRAINT meter_readings_unique_consumption UNIQUE (nmi, timestamp)
);
