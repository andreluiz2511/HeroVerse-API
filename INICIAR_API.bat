@echo off
setlocal
cd /d "%~dp0"
echo ===============================================
echo  HeroVerse - Iniciar API com Maven Wrapper
echo ===============================================
 echo.
where java >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH.
    echo Configure um JDK 21 ou superior no seu computador.
    pause
    exit /b 1
)
call "%~dp0mvnw.cmd" spring-boot:run
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel iniciar a API.
    echo Confira as mensagens acima e se o JDK esta configurado.
    pause
    exit /b 1
)
endlocal
