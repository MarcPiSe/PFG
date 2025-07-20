#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_DIR="$SCRIPT_DIR/server"
STORAGE_DIR="$SCRIPT_DIR/storage_data"
DESKTOP_DIR="$SCRIPT_DIR/desktop"
ENV_FILE="$SCRIPT_DIR/.env"

# Function to save environment variables to .env file
save_env_vars() {
  echo "POSTGRES_USER=$POSTGRES_USER" > "$ENV_FILE"
  echo "POSTGRES_PASSWORD=$POSTGRES_PASSWORD" >> "$ENV_FILE"
  echo "POSTGRES_DB=$POSTGRES_DB" >> "$ENV_FILE"
  echo "RABBITMQ_DEFAULT_USER=$RABBITMQ_DEFAULT_USER" >> "$ENV_FILE"
  echo "RABBITMQ_DEFAULT_PASS=$RABBITMQ_DEFAULT_PASS" >> "$ENV_FILE"
  echo "PGADMIN_DEFAULT_EMAIL=$PGADMIN_DEFAULT_EMAIL" >> "$ENV_FILE"
  echo "PGADMIN_DEFAULT_PASSWORD=$PGADMIN_DEFAULT_PASSWORD" >> "$ENV_FILE"
  
  echo ""
  echo " IMPORTANT: Configuration saved to .env file"
  echo "   Do NOT delete this file as it contains your custom credentials."
  echo "   Future restarts and updates will use these values automatically."
  echo ""
}

load_env_vars() {
  if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
    echo "Loaded existing configuration from .env file"
  fi
}

set_default_vars() {
  export POSTGRES_USER="${POSTGRES_USER:-admin}"
  export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-admin}"
  export POSTGRES_DB="${POSTGRES_DB:-mydb}"
  export RABBITMQ_DEFAULT_USER="${RABBITMQ_DEFAULT_USER:-admin}"
  export RABBITMQ_DEFAULT_PASS="${RABBITMQ_DEFAULT_PASS:-admin}"
  export PGADMIN_DEFAULT_EMAIL="${PGADMIN_DEFAULT_EMAIL:-admin@admin.com}"
  export PGADMIN_DEFAULT_PASSWORD="${PGADMIN_DEFAULT_PASSWORD:-admin}"
}

usage() {
  echo "Usage: $0 [-i|-u|-d|-r|-b|-h] [credential options]"
  echo "  -i  install Docker, build code and start system adding super admin (required)"
  echo "  -u  start system"
  echo "  -d  stop system"
  echo "  -r  stop system and remove all data"
  echo "  -b  update: rebuild services and restart containers"
  echo "  -h  show this help"
  echo ""
  echo "Credential options (only valid with -i):"
  echo "  -up <user>      PostgreSQL username"
  echo "  -pp <password>  PostgreSQL password"
  echo "  -dp <database>  PostgreSQL database name"
  echo "  -ur <user>      RabbitMQ username"
  echo "  -pr <password>  RabbitMQ password"
  echo "  -ea <email>     pgAdmin email"
  echo "  -pa <password>  pgAdmin password"
  echo ""
  echo "Example: $0 -i -up myuser -pp mypass -dp mydb"
}

install_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose apache2-utils uuid-runtime curl
    sudo systemctl enable docker || true
    sudo systemctl start docker || sudo service docker start
  fi
}

install_node() {
  if ! command -v node >/dev/null 2>&1; then
    echo "Installing Node.js..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
    sudo apt-get install -y nodejs
  fi
  
  if ! command -v pnpm >/dev/null 2>&1; then
    echo "Installing pnpm..."
    npm install -g pnpm
  fi
}

install_rust() {
  if ! command -v cargo >/dev/null 2>&1; then
    echo "Installing Rust..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source "$HOME/.cargo/env"
  fi
}

build_projects() {
  echo "Building backend services..."
  for pom in "$SCRIPT_DIR"/server/*/pom.xml; do
    svc_dir=$(dirname "$pom")
    (cd "$svc_dir" && ./mvnw clean install -DskipTests)
  done
  
  echo "Building web client..."
  (cd "$SCRIPT_DIR/ui-new" && npm install)
}

build_tauri_app() {
  echo "Building Tauri desktop application..."
  if [ -d "$DESKTOP_DIR" ]; then
    (cd "$DESKTOP_DIR" && pnpm install)
    
    # Try to build for multiple targets if possible
    echo "Attempting to build for multiple platforms..."
    (cd "$DESKTOP_DIR" && pnpm tauri build --verbose)
    
    # Attempt cross-compilation for other platforms (if toolchain is available)
    if command -v rustup >/dev/null 2>&1; then
      echo "Checking for additional target toolchains..."
      
      # Try to add common targets (will skip if already installed or not available)
      rustup target add x86_64-pc-windows-msvc 2>/dev/null || true
      rustup target add x86_64-apple-darwin 2>/dev/null || true
      rustup target add aarch64-apple-darwin 2>/dev/null || true
      
      # Only attempt cross-compilation if we're on Linux (most flexible platform)
      if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Attempting cross-compilation for Windows..."
        (cd "$DESKTOP_DIR" && pnpm tauri build --target x86_64-pc-windows-msvc --verbose 2>/dev/null || echo "   Cross-compilation for Windows not available")
      fi
    fi
    
    if [ $? -eq 0 ]; then
      echo "Tauri installers generated successfully"
      
      INSTALLERS_DIR="$SCRIPT_DIR/installers"
      mkdir -p "$INSTALLERS_DIR"
      
      BUNDLE_DIR="$DESKTOP_DIR/src-tauri/target/release/bundle"
      if [ -d "$BUNDLE_DIR" ]; then
        rm -rf "$INSTALLERS_DIR"/*
        
        # Copy all installer files from all possible target directories
        find "$DESKTOP_DIR/src-tauri/target" -name "bundle" -type d | while read bundle_path; do
          if [ -d "$bundle_path/deb" ]; then
            cp -r "$bundle_path/deb/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
          if [ -d "$bundle_path/appimage" ]; then
            cp -r "$bundle_path/appimage/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
          if [ -d "$bundle_path/msi" ]; then
            cp -r "$bundle_path/msi/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
          if [ -d "$bundle_path/nsis" ]; then
            cp -r "$bundle_path/nsis/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
          if [ -d "$bundle_path/dmg" ]; then
            cp -r "$bundle_path/dmg/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
          if [ -d "$bundle_path/macos" ]; then
            cp -r "$bundle_path/macos/"* "$INSTALLERS_DIR/" 2>/dev/null || true
          fi
        done
        
        echo "   Installers moved to: $INSTALLERS_DIR/"
        echo "   Available installers:"
        if [ "$(ls -A "$INSTALLERS_DIR" 2>/dev/null)" ]; then
          ls -la "$INSTALLERS_DIR/" 2>/dev/null | grep -v "^total" | grep -v "^d" | awk '{print "     " $NF}'
        else
          echo "     No installers found"
        fi
        
        # Show platform-specific notes
        echo ""
        echo "   Platform notes:"
        echo "     - Current platform: $(uname -s)"
        echo "     - Native installers generated for this platform"
        if [[ "$OSTYPE" != "linux-gnu"* ]]; then
          echo "     - Cross-compilation requires Linux with additional toolchains"
          echo "     - For full multi-platform support, run build on Linux with:"
          echo "       sudo apt install gcc-mingw-w64 and configure Windows cross-compilation"
        fi
      else
        echo "   Warning: Bundle directory not found"
      fi
    else
      echo "Failed to build Tauri application"
    fi
  else
    echo "Desktop directory not found, skipping Tauri build"
  fi
}

start_system() {
  load_env_vars
  set_default_vars
  (cd "$COMPOSE_DIR" && sudo docker-compose up -d)
}

update_system() {
  stop_system
  build_projects
  build_tauri_app
  load_env_vars
  set_default_vars
  (cd "$COMPOSE_DIR" && sudo docker-compose up -d --build)
}

stop_system() {
  (cd "$COMPOSE_DIR" && sudo docker-compose down)
}

wait_for_login() {
  until [ "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{\"username\":\"'$USERNAME'\",\"password\":\"'$PASSWORD'\"}' http://localhost:8762/users/auth/login)" = "200" ]; do
    echo "Waiting for application to be ready..."
    sleep 5
  done
  echo "Superuser login succeeded."
}

add_superadmin() {
  if [ -z "$USERNAME" ]; then
    read -p "Superuser username: " USERNAME
  fi
  if [ -z "$PASSWORD" ]; then
    read -s -p "Superuser password: " PASSWORD
    echo
  fi
  HASH=$(htpasswd -bnBC 10 "" "$PASSWORD" | tr -d ':\n')
  USER_ID=$(uuidgen)
  ELEMENT_ID=$(uuidgen)
  FOLDER_ID=$(uuidgen)
  ACCESS_ID=$(uuidgen)
  SNAPSHOT_ID=$(uuidgen)
  SNAPSHOT_ELEMENT_ID=$(uuidgen)

  EXISTS=$(docker exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT 1 FROM user_entity WHERE role='2' LIMIT 1;")
  if [ "$EXISTS" = "1" ]; then
    echo "Super admin already exists. Skipping creation."
    return
  fi

  docker exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<SQL
INSERT INTO user_entity (id, username, password, role) VALUES ('$USER_ID', '$USERNAME', '$HASH', '2');
INSERT INTO user_info (id, email, first_name, last_name, created_date, last_modified_date) VALUES ('$USER_ID', '${USERNAME}@example.com', 'Super', 'User', NOW(), NOW());
INSERT INTO element_entity (id, is_folder) VALUES ('$ELEMENT_ID', true);
INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES ('$FOLDER_ID', 'root', '$USER_ID', NULL, '$ELEMENT_ID', NOW(), NOW(), false, false);
INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES ('$ACCESS_ID', '$USER_ID', '$ELEMENT_ID', 'ADMIN');
INSERT INTO snapshot_entity (id, user_id) VALUES ('$SNAPSHOT_ID', '$USER_ID');
INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES ('$SNAPSHOT_ELEMENT_ID', '$ELEMENT_ID', '$SNAPSHOT_ID', NULL, 'folder', '/', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '/');
SQL
}

remove_data() {
  stop_system
  read -p "This will delete all database and file data. Are you sure? [y/N] " confirm
  if [[ $confirm =~ ^[yY]$ ]]; then
    (cd "$COMPOSE_DIR" && sudo docker-compose down -v)
    sudo rm -rf "$STORAGE_DIR"
  else
    echo "Aborted."
  fi
}

if [ $# -eq 0 ]; then
  usage
  exit 1
fi

INSTALL_MODE=false
SAVE_CONFIG=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -i)
      INSTALL_MODE=true
      SAVE_CONFIG=true
      shift
      ;;
    -u)
      start_system
      exit 0
      ;;
    -d)
      stop_system
      exit 0
      ;;
    -r)
      remove_data
      exit 0
      ;;
    -b)
      update_system
      exit 0
      ;;
    -up)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      POSTGRES_USER="$2"
      shift 2
      ;;
    -pp)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      POSTGRES_PASSWORD="$2"
      shift 2
      ;;
    -dp)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      POSTGRES_DB="$2"
      shift 2
      ;;
    -ur)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      RABBITMQ_DEFAULT_USER="$2"
      shift 2
      ;;
    -pr)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      RABBITMQ_DEFAULT_PASS="$2"
      shift 2
      ;;
    -ea)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      PGADMIN_DEFAULT_EMAIL="$2"
      shift 2
      ;;
    -pa)
      if [ "$INSTALL_MODE" = false ]; then
        echo "Error: Credential options can only be used with -i"
        exit 1
      fi
      PGADMIN_DEFAULT_PASSWORD="$2"
      shift 2
      ;;
    -h|*)
      usage
      exit 0
      ;;
  esac
done

if [ "$INSTALL_MODE" = true ]; then
  set_default_vars
  if [ "$SAVE_CONFIG" = true ]; then
    save_env_vars
  fi
  install_docker
  install_node
  install_rust
  build_projects
  build_tauri_app
  start_system
  add_superadmin
  wait_for_login
fi