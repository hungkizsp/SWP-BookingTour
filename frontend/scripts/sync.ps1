# Sync Script: Frontend -> Backend Static
# Usage: ./scripts/sync.ps1

$FrontendRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$BackendStatic = Resolve-Path (Join-Path $FrontendRoot "..\\backend\\src\\main\\resources\\static")

Write-Host "Syncing Frontend to Backend..." -ForegroundColor Cyan
Write-Host "Source: $FrontendRoot"
Write-Host "Target: $BackendStatic"

# Ensure target exists
if (!(Test-Path $BackendStatic)) {
    New-Item -ItemType Directory -Force -Path $BackendStatic | Out-Null
}

# Directories to sync
$Folders = @("assets", "pages")

foreach ($Folder in $Folders) {
    $Src = Join-Path $FrontendRoot "static/$Folder"
    $Dst = Join-Path $BackendStatic $Folder
    
    if (Test-Path $Src) {
        Write-Host "Copying $Folder..."
        if (Test-Path $Dst) { Remove-Item -Recurse -Force $Dst }
        Copy-Item -Recurse -Force $Src $Dst
    }
}

Write-Host "Synchronization Complete!" -ForegroundColor Green
