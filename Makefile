# Local Spark Development Environment Makefile
# This Makefile helps you set up a local Spark environment for plugin development

# Configuration
SPARK_VERSION := 3.5.0
SPARK_HOME := ./spark-$(SPARK_VERSION)-bin-hadoop3
SPARK_DOWNLOAD_URL := https://archive.apache.org/dist/spark/spark-$(SPARK_VERSION)/spark-$(SPARK_VERSION)-bin-hadoop3.tgz
SPARK_TARBALL := spark-$(SPARK_VERSION)-bin-hadoop3.tgz

# Plugin configuration
PLUGIN_NAME := spark-god-plugin
PLUGIN_VERSION := 1.0.0
PLUGIN_DIR := src/$(PLUGIN_NAME)
UI_NAME := spark-god-ui
UI_DIR := src/$(UI_NAME)
PLUGIN_JAR := $(PLUGIN_DIR)/target/scala-2.12/$(PLUGIN_NAME)_2.12-$(PLUGIN_VERSION).jar
TEST_APP_JAR := src/test-app/target/scala-2.12/test-app_2.12-1.0.0.jar

DATAFLINT_VERSION := 0.4.2

# Default target
.PHONY: help
help:
	@echo "Local Spark Development Environment"
	@echo "=================================="
	@echo ""
	@echo "Setup commands:"
	@echo "  make download-spark    - Download Spark $(SPARK_VERSION)"
	@echo "  make install-spark     - Extract and setup Spark"
	@echo "  make setup             - Complete setup (download + install)"
	@echo ""
	@echo "Spark commands:"
	@echo "  make start-master      - Start Spark master"
	@echo "  make start-worker      - Start Spark worker"
	@echo "  make start-history     - Start Spark History Server"
	@echo "  make stop-spark        - Stop all Spark processes"
	@echo "  make spark-status      - Check Spark processes"
	@echo ""
	@echo "Plugin commands:"
	@echo "  make plugin-build      - Build the SparkGod plugin"
	@echo "  make plugin-copy       - Copy plugin to Spark jars directory"
	@echo "  make plugin-test       - Test plugin with sample job"
	@echo "  make plugin-clean      - Clean plugin build files"
	@echo ""
	@echo "Development commands:"
	@echo "  make sample-job        - Run a sample Spark job"
	@echo "  make clean             - Clean all files"
	@echo "  make logs              - Show Spark logs"

# Setup targets
.PHONY: download-spark
download-spark:
	@echo "Downloading Spark $(SPARK_VERSION)..."
	@if [ ! -f "$(SPARK_TARBALL)" ]; then \
		curl -L -o "$(SPARK_TARBALL)" "$(SPARK_DOWNLOAD_URL)"; \
	else \
		echo "Spark tarball already exists, skipping download."; \
	fi

download-dataflint:
	curl -L https://repo1.maven.org/maven2/io/dataflint/spark_2.12/${DATAFLINT_VERSION}/spark_2.12-${DATAFLINT_VERSION}.jar -o dataflint.jar

.PHONY: install-spark
install-spark:
	@echo "Installing Spark..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		tar -xzf "$(SPARK_TARBALL)"; \
		echo "Spark installed to $(SPARK_HOME)"; \
	else \
		echo "Spark already installed at $(SPARK_HOME)"; \
	fi
	@echo "Setting up environment..."
	@echo 'export SPARK_HOME=$(PWD)/$(SPARK_HOME)' > .env
	@echo 'export PATH=$$SPARK_HOME/bin:$$PATH' >> .env
	@echo "Environment file created: .env"
	@echo "To use Spark, run: source .env"

.PHONY: setup
setup: download-spark install-spark
	@echo "Setup complete!"
	@echo "Next steps:"
	@echo "1. Run: source .env"
	@echo "2. Run: make start-master"
	@echo "3. Run: make start-worker"
	@echo "4. Run: make plugin-build"
	@echo "5. Run: make plugin-copy"
	@echo "6. Run: make plugin-test"

ui-init:
	cd src && npm create vite@latest $(UI_NAME) -- --template react-ts
	cd $(UI_DIR) && npm install
	cd $(UI_DIR) && npm install tailwindcss @tailwindcss/vite zustand

ui-test:
	cd $(UI_DIR) && npm run dev

ui-build:
	cd $(UI_DIR) && npm run build

# Spark management targets
.PHONY: start-master
start-master:
	@echo "Starting Spark master..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		echo "Error: Spark not installed. Run 'make setup' first."; \
		exit 1; \
	fi
	@export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
	$(SPARK_HOME)/sbin/start-master.sh
	@echo "Spark master started at http://localhost:8080"

.PHONY: start-worker
start-worker:
	@echo "Starting Spark worker..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		echo "Error: Spark not installed. Run 'make setup' first."; \
		exit 1; \
	fi
	@export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
	$(SPARK_HOME)/sbin/start-slave.sh spark://localhost:7077
	@echo "Spark worker started"

.PHONY: start-history
start-history:
	@echo "Starting Spark History Server..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		echo "Error: Spark not installed. Run 'make setup' first."; \
		exit 1; \
	fi
	@mkdir -p /tmp/spark-events
	@export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
	$(SPARK_HOME)/sbin/start-history-server.sh
	@echo "Spark History Server started at http://localhost:18080"

stop-history:
	@echo "Stopping Spark History Server..."
	@if [ -d "$(SPARK_HOME)" ]; then \
		export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
		$(SPARK_HOME)/sbin/stop-history-server.sh; \
	fi
	@echo "Spark History Server stopped"

stop-master:
	@echo "Stopping Spark Master..."
	@if [ -d "$(SPARK_HOME)" ]; then \
		export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
		$(SPARK_HOME)/sbin/stop-master.sh; \
	fi
	@echo "Spark Master stopped"

stop-worker:
	@echo "Stopping Spark Worker..."
	@if [ -d "$(SPARK_HOME)" ]; then \
		export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
		$(SPARK_HOME)/sbin/stop-slave.sh; \
	fi
	@echo "Spark Worker stopped"

.PHONY: stop-spark
stop-spark:
	@echo "Stopping Spark processes..."
	@if [ -d "$(SPARK_HOME)" ]; then \
		export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
		$(SPARK_HOME)/sbin/stop-all.sh; \
	fi
	@echo "Spark processes stopped"

.PHONY: spark-status
spark-status:
	@ps aux | grep spark | grep -v -e grep -e spark-status || echo "No Spark processes running"

ui-copy: ui-build
	rm -rf $(PLUGIN_DIR)/src/main/resources/ui/
	mkdir -p $(PLUGIN_DIR)/src/main/resources/ui/
	cp -R $(UI_DIR)/dist/* $(PLUGIN_DIR)/src/main/resources/ui/

# Plugin targets
.PHONY: plugin-build
plugin-build: ui-copy
	@echo "Building SparkGod plugin..."
	@if [ ! -f "build.sbt" ]; then \
		echo "Error: build.sbt not found in root directory."; \
		exit 1; \
	fi
	sbt sparkGodPlugin/clean sparkGodPlugin/compile sparkGodPlugin/package testApp/clean testApp/compile testApp/package
	@echo "Plugin built: $(PLUGIN_JAR)"

.PHONY: plugin-copy
plugin-copy:
	@echo "Copying plugin to Spark jars directory..."
	@if [ ! -f "$(PLUGIN_JAR)" ]; then \
		echo "Error: Plugin JAR not found. Run 'make plugin-build' first."; \
		exit 1; \
	fi
	@cp "$(PLUGIN_JAR)" "$(SPARK_HOME)/jars/"
	@echo "Plugin copied to $(SPARK_HOME)/jars/"

.PHONY: plugin-test
plugin-test:
	@echo "Testing plugin with sample job..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		echo "Error: Spark not installed. Run 'make setup' first."; \
		exit 1; \
	fi
	@export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
	$(SPARK_HOME)/bin/spark-submit \
		--class com.example.spark.TestApplication \
		--master spark://localhost:7077 \
		--conf spark.plugins=org.apache.spark.SparkGodPlugin \
		--conf spark.eventLog.enabled=true \
		--conf spark.eventLog.dir=file:///tmp/spark-events \
		$(TEST_APP_JAR)

.PHONY: plugin-clean
plugin-clean:
	@echo "Cleaning plugin build files..."
	@sbt sparkGodPlugin/clean testApp/clean
	@rm -rf $(PLUGIN_DIR)/target/
	@rm -rf src/test-app/target/
	@echo "Plugin build files cleaned"

# Development targets
.PHONY: sample-job
sample-job:
	@echo "Running sample Spark job..."
	@if [ ! -d "$(SPARK_HOME)" ]; then \
		echo "Error: Spark not installed. Run 'make setup' first."; \
		exit 1; \
	fi
	@export SPARK_HOME=$(PWD)/$(SPARK_HOME) && \
	$(SPARK_HOME)/bin/spark-submit \
		--class org.apache.spark.examples.SparkPi \
		--master spark://localhost:7077 \
		--conf spark.eventLog.enabled=true \
		--packages io.dataflint:spark_2.12:$(DATAFLINT_VERSION) \
		--conf spark.plugins=io.dataflint.spark.SparkDataflintPlugin \
		--conf spark.eventLog.dir=file:///tmp/spark-events \
		$(SPARK_HOME)/examples/jars/spark-examples_2.12-$(SPARK_VERSION).jar 10

.PHONY: logs
logs:
	@echo "Spark logs:"
	@if [ -d "$(SPARK_HOME)/logs" ]; then \
		ls -la "$(SPARK_HOME)/logs/"; \
		echo ""; \
		echo "Latest master log:"; \
		tail -20 "$(SPARK_HOME)/logs/spark-*-org.apache.spark.deploy.master.Master-*.out" 2>/dev/null || echo "No master log found"; \
		echo ""; \
		echo "Latest worker log:"; \
		tail -20 "$(SPARK_HOME)/logs/spark-*-org.apache.spark.deploy.worker.Worker-*.out" 2>/dev/null || echo "No worker log found"; \
	else \
		echo "No logs directory found"; \
	fi

.PHONY: clean
clean: stop-spark plugin-clean
	@echo "Cleaning all files..."
	@rm -rf "$(SPARK_HOME)"
	@rm -f "$(SPARK_TARBALL)"
	@rm -f .env
	@rm -rf events
	@echo "All files cleaned"

# Quick start target
.PHONY: quick-start
quick-start: setup
	@echo "Quick start complete!"
	@echo ""
	@echo "To start development:"
	@echo "1. source .env"
	@echo "2. make start-master"
	@echo "3. make start-worker"
	@echo "4. make start-history"
	@echo ""
	@echo "Then open:"
	@echo "- Spark Master UI: http://localhost:8080"
	@echo "- Spark History Server: http://localhost:18080"