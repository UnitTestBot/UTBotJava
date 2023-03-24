:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -eu

SCRIPT_VERSION=dotnet-cmd-v2
COMPANY_DIR="UtBot"
TARGET_DIR="${TEMPDIR:-$HOME/.local/share}/$COMPANY_DIR/dotnet-cmd"
KEEP_ROSETTA2=false
export DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=true
export DOTNET_SKIP_FIRST_TIME_EXPERIENCE=true
export DOTNET_CLI_TELEMETRY_OPTOUT=true

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

retry_on_error () {
  local n="$1"
  shift

  for i in $(seq 2 "$n"); do
    "$@" 2>&1 && return || echo "WARNING: Command '$1' returned non-zero exit status $?, try again"
  done
  "$@"
}

is_linux_musl () {
  (ldd --version 2>&1 || true) | grep -q musl
}

case $(uname) in
Darwin)
  DOTNET_ARCH=$(uname -m)
  if ! $KEEP_ROSETTA2 && [ "$(sysctl -n sysctl.proc_translated 2>/dev/null || true)" = "1" ]; then
    DOTNET_ARCH=arm64
  fi
  case $DOTNET_ARCH in
  x86_64)
    DOTNET_HASH_URL=cf3e1c73-a9a9-4e08-8607-8f9edae5f3f2/40a021a98a6b6e430a1f170037735f6f
    DOTNET_FILE_NAME=dotnet-sdk-6.0.301-osx-x64
    ;;
  arm64)
    DOTNET_HASH_URL=3859fff3-f8a9-4e05-87cd-bd6db75833f5/64ec1099d45f85d14099da3c1f92a5c3
    DOTNET_FILE_NAME=dotnet-sdk-6.0.301-osx-arm64
    ;;
  *) echo "Unknown architecture $DOTNET_ARCH" >&2; exit 1;;
  esac;;
Linux)
  DOTNET_ARCH=$(linux$(getconf LONG_BIT) uname -m)
  case $DOTNET_ARCH in
  x86_64)
    if is_linux_musl; then
      DOTNET_HASH_URL=206aebda-126f-484f-af02-051a17c1ec54/2ec559cb69cec83ffa64dba5441a1b2d
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-musl-x64
    else
      DOTNET_HASH_URL=77d472e5-194c-421e-992d-e4ca1d08e6cc/56c61ac303ddf1b12026151f4f000a2b
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-x64
    fi
    ;;
  aarch64)
    if is_linux_musl; then
      DOTNET_HASH_URL=4bd2399a-e0e9-43a6-9767-ac15dd430b1c/3dd4307a1ce811e31943d80eee638bc1
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-musl-arm64
    else
      DOTNET_HASH_URL=06c4ee8e-bf2c-4e46-ab1c-e14dd72311c1/f7bc6c9677eaccadd1d0e76c55d361ea
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-arm64
    fi
    ;;
  armv7l | armv8l)
    if is_linux_musl; then
      DOTNET_HASH_URL=952c468c-ac70-46b0-9274-4cb9c270950c/f0cd4c8392158547c2fa38674bfd56fd
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-musl-arm
    else
      DOTNET_HASH_URL=a218e3b9-941b-43be-bfb1-615862777457/80954de34ab68729981ed372a8d25b46
      DOTNET_FILE_NAME=dotnet-sdk-6.0.301-linux-arm
    fi
    ;;
  *) echo "Unknown architecture $DOTNET_ARCH" >&2; exit 1;;
  esac;;
*) echo "Unknown platform: $(uname)" >&2; exit 1;;
esac

DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/$DOTNET_HASH_URL/$DOTNET_FILE_NAME.tar.gz
DOTNET_TARGET_DIR=$TARGET_DIR/$DOTNET_FILE_NAME-$SCRIPT_VERSION
DOTNET_TEMP_FILE=$TARGET_DIR/dotnet-sdk-temp.tar.gz

if grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
  # Everything is up-to-date in $DOTNET_TARGET_DIR, do nothing
  true
else
while true; do  # Note(k15tfu): for goto
  mkdir -p "$TARGET_DIR"

  LOCK_FILE="$TARGET_DIR/.dotnet-cmd-lock.pid"
  TMP_LOCK_FILE="$TARGET_DIR/.tmp.$$.pid"
  echo $$ >"$TMP_LOCK_FILE"

  while ! ln "$TMP_LOCK_FILE" "$LOCK_FILE" 2>/dev/null; do
    LOCK_OWNER=$(cat "$LOCK_FILE" 2>/dev/null || true)
    while [ -n "$LOCK_OWNER" ] && ps -p $LOCK_OWNER >/dev/null; do
      warn "Waiting for the process $LOCK_OWNER to finish bootstrap dotnet.cmd"
      sleep 1
      LOCK_OWNER=$(cat "$LOCK_FILE" 2>/dev/null || true)

      # Hurry up, bootstrap is ready..
      if grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
        break 3  # Note(k15tfu): goto out of the outer if-else block.
      fi
    done

    if [ -n "$LOCK_OWNER" ] && grep -q -x $LOCK_OWNER "$LOCK_FILE" 2>/dev/null; then
      die "ERROR: The lock file $LOCK_FILE still exists on disk after the owner process $LOCK_OWNER exited"
    fi
  done

  trap "rm -f \"$LOCK_FILE\"" EXIT
  rm "$TMP_LOCK_FILE"

  if ! grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
    warn "Downloading $DOTNET_URL to $DOTNET_TEMP_FILE"

    rm -f "$DOTNET_TEMP_FILE"
    if command -v curl >/dev/null 2>&1; then
      if [ -t 1 ]; then CURL_PROGRESS="--progress-bar"; else CURL_PROGRESS="--silent --show-error"; fi
      retry_on_error 5 curl -L $CURL_PROGRESS --output "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
    elif command -v wget >/dev/null 2>&1; then
      if [ -t 1 ]; then WGET_PROGRESS=""; else WGET_PROGRESS="-nv"; fi
      retry_on_error 5 wget $WGET_PROGRESS -O "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
    else
      die "ERROR: Please install wget or curl"
    fi

    warn "Extracting $DOTNET_TEMP_FILE to $DOTNET_TARGET_DIR"
    rm -rf "$DOTNET_TARGET_DIR"
    mkdir -p "$DOTNET_TARGET_DIR"

    tar -x -f "$DOTNET_TEMP_FILE" -C "$DOTNET_TARGET_DIR"
    rm -f "$DOTNET_TEMP_FILE"

    echo "$DOTNET_URL" >"$DOTNET_TARGET_DIR/.flag"
  fi

  rm "$LOCK_FILE"
  break
done
fi

if [ ! -x "$DOTNET_TARGET_DIR/dotnet" ]; then
  die "Unable to find dotnet under $DOTNET_TARGET_DIR"
fi

exec "$DOTNET_TARGET_DIR/dotnet" "$@"

:CMDSCRIPT

setlocal
set SCRIPT_VERSION=v2
set COMPANY_NAME=UtBot
set TARGET_DIR=%LOCALAPPDATA%\%COMPANY_NAME%\dotnet-cmd\

for /f "tokens=3 delims= " %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v "PROCESSOR_ARCHITECTURE"') do set ARCH=%%a

if "%ARCH%"=="ARM64" (
    set DOTNET_HASH_URL=656c8345-6661-409e-871d-00ca93cec542/cae3dcdc5c668c0e0abcf12d838348f1
    set DOTNET_FILE_NAME=dotnet-sdk-6.0.301-win-arm64
) else (

if "%ARCH%"=="AMD64" (
    set DOTNET_HASH_URL=333eba0c-3242-48f3-a923-fdac5f219f77/342a4595101e3b4616360a7666459236
    set DOTNET_FILE_NAME=dotnet-sdk-6.0.301-win-x64
) else (

if "%ARCH%"=="x86" (
    set DOTNET_HASH_URL=0a9cabcb-cb52-4f5e-bb79-1298f9ff9e22/c306c5cc940a9bb9a39ffe6619a255e6
    set DOTNET_FILE_NAME=dotnet-sdk-6.0.301-win-x86
) else (

echo Unknown Windows architecture
goto fail

)))

set DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/%DOTNET_HASH_URL%/%DOTNET_FILE_NAME%.zip
set DOTNET_TARGET_DIR=%TARGET_DIR%%DOTNET_FILE_NAME%-%SCRIPT_VERSION%\
set DOTNET_TEMP_FILE=%TARGET_DIR%dotnet-sdk-temp.zip
set DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=true
set DOTNET_SKIP_FIRST_TIME_EXPERIENCE=true
set DOTNET_CLI_TELEMETRY_OPTOUT=true

set POWERSHELL=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe

if not exist "%DOTNET_TARGET_DIR%.flag" goto downloadAndExtractDotNet

set /p CURRENT_FLAG=<"%DOTNET_TARGET_DIR%.flag"
if "%CURRENT_FLAG%" == "%DOTNET_URL%" goto continueWithDotNet

:downloadAndExtractDotNet

set DOWNLOAD_AND_EXTRACT_DOTNET_PS1= ^
Set-StrictMode -Version 3.0; ^
$ErrorActionPreference = 'Stop'; ^
 ^
$createdNew = $false; ^
$lock = New-Object System.Threading.Mutex($true, 'Global\dotnet-cmd-lock', [ref]$createdNew); ^
if (-not $createdNew) { ^
    Write-Host 'Waiting for the other process to finish bootstrap dotnet.cmd'; ^
    [void]$lock.WaitOne(); ^
} ^
 ^
try { ^
    if ((Get-Content '%DOTNET_TARGET_DIR%.flag' -ErrorAction Ignore) -ne '%DOTNET_URL%') { ^
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; ^
        Write-Host 'Downloading %DOTNET_URL% to %DOTNET_TEMP_FILE%'; ^
        [void](New-Item '%TARGET_DIR%' -ItemType Directory -Force); ^
        (New-Object Net.WebClient).DownloadFile('%DOTNET_URL%', '%DOTNET_TEMP_FILE%'); ^
 ^
        Write-Host 'Extracting %DOTNET_TEMP_FILE% to %DOTNET_TARGET_DIR%'; ^
        if (Test-Path '%DOTNET_TARGET_DIR%') { ^
            Remove-Item '%DOTNET_TARGET_DIR%' -Recurse; ^
        } ^
        Add-Type -A 'System.IO.Compression.FileSystem'; ^
        [IO.Compression.ZipFile]::ExtractToDirectory('%DOTNET_TEMP_FILE%', '%DOTNET_TARGET_DIR%'); ^
        Remove-Item '%DOTNET_TEMP_FILE%'; ^
 ^
        Set-Content '%DOTNET_TARGET_DIR%.flag' -Value '%DOTNET_URL%'; ^
    } ^
} ^
finally { ^
    $lock.ReleaseMutex(); ^
}

"%POWERSHELL%" -nologo -noprofile -Command %DOWNLOAD_AND_EXTRACT_DOTNET_PS1%
if errorlevel 1 goto fail

:continueWithDotNet

if not exist "%DOTNET_TARGET_DIR%\dotnet.exe" (
  echo Unable to find dotnet.exe under %DOTNET_TARGET_DIR%
  goto fail
)

REM Prevent globally installed .NET Core from leaking into this runtime's lookup
SET DOTNET_MULTILEVEL_LOOKUP=0
SET DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=false

for /f "tokens=2 delims=:." %%c in ('chcp') do set /a PREV_CODE_PAGE=%%c
call "%DOTNET_TARGET_DIR%\dotnet.exe" %*
set /a DOTNET_EXIT_CODE=%ERRORLEVEL%
chcp %PREV_CODE_PAGE% >nul

exit /B %DOTNET_EXIT_CODE%
endlocal

:fail
echo "FAIL"
exit /b 1