<#
PowerShell script para recrear la base de datos MotoresBits.
- Detiene el proceso que ocupe el puerto 8080 (opcional).
- Realiza backup opcional con mysqldump si está disponible.
- Si el cliente `mysql` está en PATH lo usa para DROP/CREATE/IMPORT.
- Si no, busca un contenedor Docker llamado `motores_bits` y usa `docker exec`.

USO:
1) Abrir PowerShell como Administrador en la raíz del repo (C:\MDAI\Motores-Bits)
2) Ejecutar: .\scripts\recreate_db.ps1

El script pedirá confirmación y la contraseña root de MariaDB.
#>

Param()

Write-Host "--- Script: recreate_db.ps1 ---" -ForegroundColor Cyan

# Confirmación
$confirm = Read-Host "Esto eliminará y recreará la base de datos 'MotoresBits'. Escribe YES para confirmar"
if ($confirm -ne 'YES') { Write-Host "Abortado por el usuario." -ForegroundColor Yellow; exit }

# Pedir contraseña root (oculta)
$pw = Read-Host "Introduce la contraseña de root para MariaDB (se ocultará)" -AsSecureString
$pwdPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($pw))

# Parar proceso que use puerto 8080
Write-Host "Buscando proceso en puerto 8080..."
$tcp = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($tcp) {
    $pid = $tcp.OwningProcess
    Write-Host "Encontrado PID: $pid -> matando proceso..." -ForegroundColor Yellow
    try { Stop-Process -Id $pid -Force -ErrorAction Stop; Write-Host "Proceso $pid detenido." -ForegroundColor Green } catch { Write-Host "No se pudo detener PID $pid: $_" -ForegroundColor Red }
} else { Write-Host "Puerto 8080 libre." -ForegroundColor Green }

# Rutas
$schemaFile = Join-Path -Path (Get-Location) -ChildPath "src\main\resources\db\schema.sql"
if (-not (Test-Path $schemaFile)) { Write-Host "No se encuentra $schemaFile" -ForegroundColor Red; exit }

# Detectar mysql client
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
$mysqldumpCmd = Get-Command mysqldump -ErrorAction SilentlyContinue
$useDocker = $false

if ($mysqlCmd) {
    Write-Host "Cliente 'mysql' detectado: usando cliente local para recrear la DB." -ForegroundColor Green
    # Backup si mysqldump existe
    if ($mysqldumpCmd) {
        $backupFile = Join-Path -Path (Get-Location) -ChildPath "backup_MotoresBits_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
        Write-Host "Haciendo backup en $backupFile ..."
        & mysqldump -u root -p$pwdPlain MotoresBits > $backupFile
        if ($LASTEXITCODE -eq 0) { Write-Host "Backup completado: $backupFile" -ForegroundColor Green } else { Write-Host "Warning: backup falló o no existía la BD." -ForegroundColor Yellow }
    }

    Write-Host "Drop + Create DB..."
    & mysql -u root -p$pwdPlain -e "DROP DATABASE IF EXISTS MotoresBits; CREATE DATABASE MotoresBits CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) { Write-Host "Error al crear la base de datos con mysql client." -ForegroundColor Red; exit }

    Write-Host "Importando $schemaFile ..."
    & mysql -u root -p$pwdPlain MotoresBits < $schemaFile
    if ($LASTEXITCODE -ne 0) { Write-Host "Error durante la importación del schema." -ForegroundColor Red; exit }

    Write-Host "Importación completada." -ForegroundColor Green

} else {
    Write-Host "Cliente 'mysql' no encontrado en PATH. Intentando Docker..." -ForegroundColor Yellow
    # Buscar contenedor motores_bits
    $containerId = (& docker ps -a --filter "name=motores_bits" --format "{{.ID}}" 2>$null) -join ""
    if ($containerId) {
        Write-Host "Contenedor motores_bits encontrado ($containerId). Usando Docker exec." -ForegroundColor Green
        # Drop+Create dentro del contenedor
        & docker exec -i motores_bits mysql -u root -p$pwdPlain -e "DROP DATABASE IF EXISTS MotoresBits; CREATE DATABASE MotoresBits CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        if ($LASTEXITCODE -ne 0) { Write-Host "Error al crear DB dentro del contenedor." -ForegroundColor Red; exit }
        Write-Host "Copiando schema al contenedor..."
        & docker cp $schemaFile motores_bits:/schema.sql
        Write-Host "Importando en contenedor..."
        & docker exec -i motores_bits sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" MotoresBits < /schema.sql'
        if ($LASTEXITCODE -ne 0) { Write-Host "Error importando schema en contenedor." -ForegroundColor Red; exit }
        & docker exec -i motores_bits rm /schema.sql
        Write-Host "Importación en Docker completada." -ForegroundColor Green
    } else {
        Write-Host "No se encontró cliente mysql ni contenedor Docker 'motores_bits'. Instala el cliente MySQL/MariaDB o crea/lanzar el contenedor Docker recomendado." -ForegroundColor Red
        Write-Host "Comandos sugeridos para crear contenedor:
  docker run -d --name motores_bits -e MYSQL_ROOT_PASSWORD=proyectomdai -e MYSQL_DATABASE=MotoresBits -p 3306:3306 mariadb:latest" -ForegroundColor Yellow
        exit
    }
}

# Limpieza de la variable de contraseña
$pwdPlain = $null
Write-Host "Operación finalizada. Arranca la aplicación con: .\mvnw.cmd spring-boot:run  (o java -jar target\Motores-Bits-0.0.1-SNAPSHOT.jar)" -ForegroundColor Cyan

