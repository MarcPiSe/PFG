Param(
    [switch]$i,
    [switch]$u,
    [switch]$d,
    [switch]$r,
    [switch]$b,
    [switch]$h,
    [string]$up,
    [string]$pp,
    [string]$dp,
    [string]$ur,
    [string]$pr,
    [string]$ea,
    [string]$pa
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ComposeDir = Join-Path $ScriptDir 'server'
$StorageDir = Join-Path $ScriptDir 'storage_data'
$DesktopDir = Join-Path $ScriptDir 'desktop'
$EnvFile = Join-Path $ScriptDir '.env'

# Function to save environment variables to .env file
function Save-EnvVars {
    $envContent = @"
POSTGRES_USER=$($env:POSTGRES_USER)
POSTGRES_PASSWORD=$($env:POSTGRES_PASSWORD)
POSTGRES_DB=$($env:POSTGRES_DB)
RABBITMQ_DEFAULT_USER=$($env:RABBITMQ_DEFAULT_USER)
RABBITMQ_DEFAULT_PASS=$($env:RABBITMQ_DEFAULT_PASS)
PGADMIN_DEFAULT_EMAIL=$($env:PGADMIN_DEFAULT_EMAIL)
PGADMIN_DEFAULT_PASSWORD=$($env:PGADMIN_DEFAULT_PASSWORD)
"@
    
    Set-Content -Path $EnvFile -Value $envContent
    
    Write-Host ""
    Write-Host "IMPORTANT: Configuration saved to .env file" -ForegroundColor Yellow
    Write-Host "   Do NOT delete this file as it contains your custom credentials." -ForegroundColor Yellow
    Write-Host "   Future restarts and updates will use these values automatically." -ForegroundColor Yellow
    Write-Host ""
}

function Load-EnvVars {
    if (Test-Path $EnvFile) {
        Get-Content $EnvFile | ForEach-Object {
            if ($_ -match '^([^=]+)=(.*)$') {
                [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
            }
        }
        Write-Host "Loaded existing configuration from .env file"
    }
}

function Set-DefaultVars {
    $env:POSTGRES_USER = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "admin" }
    $env:POSTGRES_PASSWORD = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "admin" }
    $env:POSTGRES_DB = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { "mydb" }
    $env:RABBITMQ_DEFAULT_USER = if ($env:RABBITMQ_DEFAULT_USER) { $env:RABBITMQ_DEFAULT_USER } else { "admin" }
    $env:RABBITMQ_DEFAULT_PASS = if ($env:RABBITMQ_DEFAULT_PASS) { $env:RABBITMQ_DEFAULT_PASS } else { "admin" }
    $env:PGADMIN_DEFAULT_EMAIL = if ($env:PGADMIN_DEFAULT_EMAIL) { $env:PGADMIN_DEFAULT_EMAIL } else { "admin@admin.com" }
    $env:PGADMIN_DEFAULT_PASSWORD = if ($env:PGADMIN_DEFAULT_PASSWORD) { $env:PGADMIN_DEFAULT_PASSWORD } else { "admin" }
}

function Show-Usage {
    Write-Host "Usage: setup.ps1 [-i|-u|-d|-r|-b|-h] [credential options]"
    Write-Host "  -i  install Docker, build code and start system adding super admin (required)"
    Write-Host "  -u  start system"
    Write-Host "  -d  stop system"
    Write-Host "  -r  stop system and remove all data"
    Write-Host "  -b  update: rebuild services and restart containers"
    Write-Host "  -h  show this help"
    Write-Host ""
    Write-Host "Credential options (only valid with -i):"
    Write-Host "  -up <user>      PostgreSQL username"
    Write-Host "  -pp <password>  PostgreSQL password"
    Write-Host "  -dp <database>  PostgreSQL database name"
    Write-Host "  -ur <user>      RabbitMQ username"
    Write-Host "  -pr <password>  RabbitMQ password"
    Write-Host "  -ea <email>     pgAdmin email"
    Write-Host "  -pa <password>  pgAdmin password"
    Write-Host ""
    Write-Host "Example: setup.ps1 -i -up myuser -pp mypass -dp mydb"
}

function Install-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "Docker not found. Installing..."
        if (Get-Command winget -ErrorAction SilentlyContinue) {
            winget install -e --id Docker.DockerDesktop -h
        } elseif (Get-Command choco -ErrorAction SilentlyContinue) {
            choco install docker-desktop -y
        } else {
            Write-Host "Please install Docker Desktop manually from https://www.docker.com and re-run the script."
            exit 1
        }
    }
}

function Install-NodeAndPnpm {
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
        Write-Host "Installing Node.js..."
        if (Get-Command winget -ErrorAction SilentlyContinue) {
            winget install OpenJS.NodeJS
        } elseif (Get-Command choco -ErrorAction SilentlyContinue) {
            choco install nodejs -y
        } else {
            Write-Host "Please install Node.js manually from https://nodejs.org and re-run the script."
            exit 1
        }
        # Refresh environment
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    }
    
    if (-not (Get-Command pnpm -ErrorAction SilentlyContinue)) {
        Write-Host "Installing pnpm..."
        npm install -g pnpm
    }
}

function Install-Rust {
    if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
        Write-Host "Installing Rust..."
        if (Get-Command winget -ErrorAction SilentlyContinue) {
            winget install Rustlang.Rustup
        } else {
            # Download and install rustup
            $rustupUrl = "https://win.rustup.rs/x86_64"
            $rustupPath = "$env:TEMP\rustup-init.exe"
            Invoke-WebRequest -Uri $rustupUrl -OutFile $rustupPath
            & $rustupPath -y
            Remove-Item $rustupPath
        }
        # Refresh environment
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    }
}

function Build-Projects {
    Write-Host "Building backend services..."
    Get-ChildItem (Join-Path $ScriptDir 'server') -Directory | ForEach-Object {
        if (Test-Path (Join-Path $_ 'pom.xml')) {
            Push-Location $_
            ./mvnw clean install -DskipTests
            Pop-Location
        }
    }
    
    Write-Host "Building web client..."
    Push-Location (Join-Path $ScriptDir 'ui-new')
    npm install
    Pop-Location
}

function Build-TauriApp {
    Write-Host "Building Tauri desktop application..."
    if (Test-Path $DesktopDir) {
        try {
            Push-Location $DesktopDir
            pnpm install
            
            # Try to build for multiple targets if possible
            Write-Host "Attempting to build for multiple platforms..."
            pnpm tauri build --verbose
            
            # Attempt cross-compilation for other platforms (if toolchain is available)
            if (Get-Command rustup -ErrorAction SilentlyContinue) {
                Write-Host "Checking for additional target toolchains..."
                
                # Try to add common targets (will skip if already installed or not available)
                rustup target add x86_64-unknown-linux-gnu 2>$null
                rustup target add x86_64-apple-darwin 2>$null
                rustup target add aarch64-apple-darwin 2>$null
                
                # Attempt cross-compilation (limited support on Windows)
                Write-Host "Note: Cross-compilation from Windows has limited support"
                Write-Host "For full multi-platform builds, consider using Linux or CI/CD"
            }
            
            Write-Host "Tauri installers generated successfully" -ForegroundColor Green
            
            # Create installers directory if it doesn't exist
            $InstallersDir = Join-Path $ScriptDir 'installers'
            if (-not (Test-Path $InstallersDir)) {
                New-Item -ItemType Directory -Path $InstallersDir -Force | Out-Null
            }
            
            # Move installers to the installers directory
            # Search for all bundle directories (including cross-compiled targets)
            $BundleDirs = Get-ChildItem -Path (Join-Path $DesktopDir 'src-tauri\target') -Recurse -Directory -Name "bundle" -ErrorAction SilentlyContinue
            
            if ($BundleDirs.Count -gt 0) {
                # Clear previous installers
                Get-ChildItem -Path $InstallersDir -File | Remove-Item -Force
                
                # Copy all installer files from all bundle directories
                foreach ($bundleRelPath in $BundleDirs) {
                    $bundlePath = Join-Path $DesktopDir "src-tauri\target\$bundleRelPath"
                    
                    $installerTypes = @('msi', 'nsis', 'deb', 'appimage', 'dmg', 'macos')
                    foreach ($type in $installerTypes) {
                        $typeDir = Join-Path $bundlePath $type
                        if (Test-Path $typeDir) {
                            Get-ChildItem -Path $typeDir -File | Copy-Item -Destination $InstallersDir -Force
                        }
                    }
                }
                
                Write-Host "   Installers moved to: $InstallersDir\" -ForegroundColor Green
                Write-Host "   Available installers:" -ForegroundColor Green
                $installers = Get-ChildItem -Path $InstallersDir -File
                if ($installers.Count -gt 0) {
                    foreach ($installer in $installers) {
                        Write-Host "     $($installer.Name)" -ForegroundColor Green
                    }
                } else {
                    Write-Host "     No installers found" -ForegroundColor Yellow
                }
                
                # Show platform-specific notes
                Write-Host ""
                Write-Host "   Platform notes:" -ForegroundColor Cyan
                Write-Host "     - Current platform: Windows" -ForegroundColor Cyan
                Write-Host "     - Native Windows installers (.msi, .exe) generated" -ForegroundColor Cyan
                Write-Host "     - Cross-compilation from Windows has limitations" -ForegroundColor Cyan
                Write-Host "     - For Linux/macOS installers, build on respective platforms or use CI/CD" -ForegroundColor Cyan
            } else {
                Write-Host "   Warning: No bundle directories found" -ForegroundColor Yellow
            }
        }
        catch {
            Write-Host "Failed to build Tauri application: $_" -ForegroundColor Red
        }
        finally {
            Pop-Location
        }
    } else {
        Write-Host "Desktop directory not found, skipping Tauri build" -ForegroundColor Yellow
    }
}

function Start-System {
    Load-EnvVars
    Set-DefaultVars
    Push-Location $ComposeDir
    docker-compose up -d
    Pop-Location
}

function Update-System {
    Stop-System
    Build-Projects
    Build-TauriApp
    Load-EnvVars
    Set-DefaultVars
    Push-Location $ComposeDir
    docker-compose up -d --build
    Pop-Location
}

function Stop-System {
    Push-Location $ComposeDir
    docker-compose down
    Pop-Location
}

function Wait-For-Login {
    $body = @{username=$Username; password=$Password} | ConvertTo-Json
    do {
        try {
            $response = Invoke-WebRequest -Uri 'http://localhost:8762/users/auth/login' -Method Post -Body $body -ContentType 'application/json'
            $ok = $response.StatusCode -eq 200
        } catch {
            $ok = $false
        }
        if (-not $ok) {
            Write-Host "Waiting for application to be ready..."
            Start-Sleep -Seconds 5
        }
    } until ($ok)
    Write-Host "Superuser login succeeded."
}

function Add-SuperAdmin {
    if (-not $Username) { $Username = Read-Host 'Superuser username' }
    if (-not $Password) { $Password = Read-Host 'Superuser password' }
    $exists = docker exec postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -tAc "SELECT 1 FROM user_entity WHERE role='2' LIMIT 1;"
    if ($exists.Trim() -eq '1') {
        Write-Host "Super admin already exists. Skipping creation."
        return
    }
    $hash = docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 '' $Password | % { $_.Split(':')[-1].Trim() }
    $userId = [guid]::NewGuid()
    $elementId = [guid]::NewGuid()
    $folderId = [guid]::NewGuid()
    $accessId = [guid]::NewGuid()
    $snapshotId = [guid]::NewGuid()
    $snapshotElementId = [guid]::NewGuid()

    $commands = @(
        "INSERT INTO user_entity (id, username, password, role) VALUES ('$userId', '$Username', '$hash', '2');",
        "INSERT INTO user_info (id, email, first_name, last_name, created_date, last_modified_date) VALUES ('$userId', '${Username}@example.com', 'Super', 'User', NOW(), NOW());",
        "INSERT INTO element_entity (id, is_folder) VALUES ('$elementId', true);",
        "INSERT INTO folder_entity (id, name, user_id, parent_id, element_id_id, creation_date, last_modification, shared, deleted) VALUES ('$folderId', 'root', '$userId', NULL, '$elementId', NOW(), NOW(), false, false);",
        "INSERT INTO access_rule (id, user_id, element_id, access_type) VALUES ('$accessId', '$userId', '$elementId', 'ADMIN');",
        "INSERT INTO snapshot_entity (id, user_id) VALUES ('$snapshotId', '$userId');",
        "INSERT INTO snapshot_element_entity (id, element_id, snapshot_id, parent_id, type, name, hash, path) VALUES ('$snapshotElementId', '$elementId', '$snapshotId', NULL, 'folder', '/', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '/');"
    )
    foreach ($cmd in $commands) {
        docker exec postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "$cmd"
    }
}

function Remove-Data {
    Stop-System
    $confirm = Read-Host 'This will delete all database and file data. Are you sure? [y/N]'
    if ($confirm -match '^[yY]$') {
        Push-Location $ComposeDir
        docker-compose down -v
        Pop-Location
        if (Test-Path $StorageDir) { Remove-Item -Recurse -Force $StorageDir }
    } else {
        Write-Host 'Aborted.'
    }
}

# Validate credential options are only used with -i
if (($up -or $pp -or $dp -or $ur -or $pr -or $ea -or $pa) -and -not $i) {
    Write-Host "Error: Credential options can only be used with -i" -ForegroundColor Red
    exit 1
}

if ($h -or (-not ($i -or $u -or $d -or $r -or $b))) {
    Show-Usage
    exit
}

if ($i) {
    # Apply credential parameters if provided
    if ($up) { $env:POSTGRES_USER = $up }
    if ($pp) { $env:POSTGRES_PASSWORD = $pp }
    if ($dp) { $env:POSTGRES_DB = $dp }
    if ($ur) { $env:RABBITMQ_DEFAULT_USER = $ur }
    if ($pr) { $env:RABBITMQ_DEFAULT_PASS = $pr }
    if ($ea) { $env:PGADMIN_DEFAULT_EMAIL = $ea }
    if ($pa) { $env:PGADMIN_DEFAULT_PASSWORD = $pa }
    
    Set-DefaultVars
    Save-EnvVars
    Install-Docker
    Install-NodeAndPnpm
    Install-Rust
    Build-Projects
    Build-TauriApp
    Start-System
    Add-SuperAdmin
    Wait-For-Login
}
if ($u) { Start-System }
if ($d) { Stop-System }
if ($r) { Remove-Data }
if ($b) { Update-System }