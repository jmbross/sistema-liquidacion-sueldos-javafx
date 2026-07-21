[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& .\mvnw.cmd --batch-mode --no-transfer-progress clean verify
if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }

$version = '1.0.0-portfolio'
$stage = Join-Path $root "target/release/payroll-modernization-$version"
$assets = Join-Path $root 'target/release-assets'
Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $assets -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stage, $assets | Out-Null

Copy-Item "target/payroll-modernization-$version.jar" $stage
Copy-Item target/lib $stage -Recurse
Copy-Item README.md, LICENSE, SECURITY.md, .env.example $stage

$runCmd = @(
    '@echo off'
    'setlocal'
    'cd /d "%~dp0"'
    'java --module-path "lib" --add-modules javafx.controls,javafx.swing -cp "payroll-modernization-1.0.0-portfolio.jar;lib/*" com.jmbross.payroll.Launcher'
) -join "`r`n"
$runCmd | Set-Content (Join-Path $stage 'run.cmd') -Encoding ascii

$runSh = @(
    '#!/usr/bin/env sh'
    'cd -- "$(dirname -- "$0")"'
    'exec java --module-path "lib" --add-modules javafx.controls,javafx.swing -cp "payroll-modernization-1.0.0-portfolio.jar:lib/*" com.jmbross.payroll.Launcher'
) -join "`n"
$runSh | Set-Content (Join-Path $stage 'run.sh') -Encoding utf8NoBOM

$packageReadme = @(
    '# Portable portfolio package'
    ''
    'Requires JDK 21 and a reachable MySQL 8 database already migrated by the source distribution.'
    'Set DB_HOST, DB_PORT, DB_NAME, DB_USER, and DB_PASSWORD, then run run.cmd on Windows or run.sh on Unix.'
    ''
    'This is not a native installer or a production payroll product.'
) -join "`n"
$packageReadme | Set-Content (Join-Path $stage 'PACKAGE_README.md') -Encoding utf8NoBOM

$zip = Join-Path $assets "payroll-modernization-$version.zip"
Compress-Archive -Path $stage -DestinationPath $zip -CompressionLevel Optimal
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $zip).Hash.ToLowerInvariant()
"$hash  $(Split-Path -Leaf $zip)" | Set-Content (Join-Path $assets 'SHA256SUMS.txt') -Encoding ascii
Write-Host "Created $zip"
