@echo off
rem --------------------------------------------------------------
rem  replace_localstorage_to_sessionstorage.bat
rem  --------------------------------------------------------------
rem  Description:
rem    - Recursively scans the frontend folder.
rem    - Replaces every occurrence of "localStorage" with "sessionStorage".
rem    - Applies to .html, .js, and .css files.
rem    - Writes changes directly back to the original files.
rem --------------------------------------------------------------

rem Set the base directory to the frontend folder (relative to this script).
set "BASE_DIR=%~dp0frontend"

rem ==== Process HTML files ==================================================
for /r "%BASE_DIR%" %%F in (*.html) do (
    echo [HTML]  Processing: %%F
    powershell -Command "(Get-Content -Raw -Encoding UTF8 \"%%F\") -replace 'localStorage', 'sessionStorage' | Set-Content -Encoding UTF8 \"%%F\""
)

rem ==== Process JS files ====================================================
for /r "%BASE_DIR%" %%F in (*.js) do (
    echo [JS]    Processing: %%F
    powershell -Command "(Get-Content -Raw -Encoding UTF8 \"%%F\") -replace 'localStorage', 'sessionStorage' | Set-Content -Encoding UTF8 \"%%F\""
)

rem ==== Process CSS files ===================================================
for /r "%BASE_DIR%" %%F in (*.css) do (
    echo [CSS]   Processing: %%F
    powershell -Command "(Get-Content -Raw -Encoding UTF8 \"%%F\") -replace 'localStorage', 'sessionStorage' | Set-Content -Encoding UTF8 \"%%F\""
)

echo --------------------------------------------------------------
echo ✅  Completed! All localStorage references have been replaced with sessionStorage.
echo --------------------------------------------------------------
pause
