#!/bin/bash
export DB_HOST="localhost"
export DB_PORT=5432
export DB_NAME="meter_readings"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export FILE_LOCATION=csv
java -cp lib/meter_parser-assembly-0.1.0-SNAPSHOT.jar app.Main