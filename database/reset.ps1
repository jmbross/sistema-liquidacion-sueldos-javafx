$ErrorActionPreference = "Stop"
docker compose down --volumes
docker compose up -d --wait
Write-Host "Database reset. Flyway migrations run when the application starts or during integration tests."
