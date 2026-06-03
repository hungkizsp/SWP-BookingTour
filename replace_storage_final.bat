@echo off
echo 🚀 Starting deep cleanup of localStorage...
echo.

set SEARCH_DIR=%~dp0

powershell -Command "Get-ChildItem -Path '%SEARCH_DIR%' -Recurse -Include *.html,*.js | ForEach-Object { $content = Get-Content $_.FullName; if ($content -match 'localStorage') { echo \"Fixing: $($_.FullName)\"; $content -replace 'localStorage', 'sessionStorage' | Set-Content $_.FullName -Encoding UTF8 } }"

echo.
echo ✅ Done! All references have been replaced.
echo ⚠️ PLEASE RESTART YOUR SERVER AND CLEAR BROWSER CACHE.
pause
