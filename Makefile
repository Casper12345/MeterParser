# Variables
COMPOSE_FILE = docker/docker-compose.yml
JAR_FILE = lib/flyway-assembly-0.1.0-SNAPSHOT.jar

up:
	@docker-compose -f $(COMPOSE_FILE) up -d
	@echo "Waiting for Postgres to be ready..."
	@sleep 2
	@until docker exec -it postgres_db pg_isready -U postgres; do \
		echo "Postgres is not ready yet. Retrying in 2 seconds..."; \
		sleep 2; \
	done
	@echo "Postgres is ready! Proceeding with migration..."
	@java -cp $(JAR_FILE) migrate.Main

docker-up:
	@docker-compose -f $(COMPOSE_FILE) up -d

docker-down:
	@docker-compose -f $(COMPOSE_FILE) down

docker-restart: docker-down docker-up

logs:
	@docker-compose -f $(COMPOSE_FILE) logs -f

clean:
	@docker-compose -f $(COMPOSE_FILE) down -v
	@rm -rf docker/postgres_data
