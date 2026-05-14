@echo off
:: ============================================================
:: TicketFlow - Script de inicio (Windows)
:: ============================================================
:: Usa el JDK bundled de IntelliJ IDEA y Node.js portable.
:: No requiere ninguna instalacion adicional del sistema.
::
:: Requisitos:
::   - IntelliJ IDEA 2025.3.4 instalado (JDK bundled)
::   - Maven y Node.js ya descargados en este proyecto
:: ============================================================

:: Ruta al JDK de IntelliJ
set JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

:: Ruta al Maven local del proyecto
set MVN_EXE=%~dp0maven\apache-maven-3.9.6\bin\mvn.cmd

:: Ruta al Node.js portable del proyecto
set NODE_DIR=%~dp0node\node-v20.15.0-win-x64
set PATH=%NODE_DIR%;%PATH%

echo ============================================
echo   TicketFlow - Iniciando sistema
echo ============================================
echo.

:: Verificaciones
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK no encontrado: %JAVA_HOME%
    pause & exit /b 1
)
if not exist "%MVN_EXE%" (
    echo [ERROR] Maven no encontrado: %MVN_EXE%
    pause & exit /b 1
)
if not exist "%NODE_DIR%\node.exe" (
    echo [ERROR] Node.js no encontrado: %NODE_DIR%
    pause & exit /b 1
)

echo [OK] Java: %JAVA_HOME%
echo [OK] Maven: %MVN_EXE%
echo [OK] Node.js: %NODE_DIR%
echo.

:: Instalar dependencias del frontend si no existen
if not exist "%~dp0frontend\node_modules" (
    echo [1/3] Instalando dependencias del frontend...
    cd /d "%~dp0frontend"
    npm install
    echo [OK] Dependencias instaladas
) else (
    echo [1/3] Dependencias del frontend ya instaladas
)

echo.
echo [2/3] Iniciando Backend (Spring Boot)...
start "TicketFlow Backend :8080" cmd /k "set JAVA_HOME=%JAVA_HOME% && set PATH=%JAVA_HOME%\bin;%PATH% && cd /d "%~dp0backend" && "%MVN_EXE%" spring-boot:run"

echo Esperando que el backend inicie (12 segundos)...
timeout /t 12 /nobreak >nul

echo [3/3] Iniciando Frontend (React + Vite)...
start "TicketFlow Frontend :5173" cmd /k "set PATH=%NODE_DIR%;%PATH% && cd /d "%~dp0frontend" && npm run dev"

echo.
echo ============================================
echo   Sistema listo!
echo   Backend:  http://localhost:8080/api/health
echo   Frontend: http://localhost:5173
echo ============================================
echo.

timeout /t 4 /nobreak >nul
start http://localhost:5173
