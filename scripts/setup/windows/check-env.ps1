$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Line([string]$Label, [string]$Value) {
    '{0,-26} {1}' -f $Label, $Value
}

Write-Host '== OS / hardware =='
$os = Get-CimInstance Win32_OperatingSystem
Line 'OS' ("{0} (build {1})" -f $os.Caption, $os.BuildNumber)
$cs = Get-CimInstance Win32_ComputerSystem
Line 'RAM' ("{0:N1} GB" -f ($cs.TotalPhysicalMemory / 1GB))
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
Line 'CPU' ("{0} / {1}C{2}T" -f $cpu.Name.Trim(), $cpu.NumberOfCores, $cpu.NumberOfLogicalProcessors)

Write-Host '== disks =='
Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
    ForEach-Object { Line $_.DeviceID ("{0:N1} GB free / {1:N1} GB" -f ($_.FreeSpace/1GB), ($_.Size/1GB)) }

Write-Host '== tools =='
$java = Get-Command java -ErrorAction SilentlyContinue
Line 'java' $(if ($java) { $java.Source } else { 'NOT FOUND' })
if ($java) {
    $javaOut = cmd.exe /c "java -version 2>&1"
    Line 'java version' (($javaOut | Select-Object -First 1).Trim())
}
Line 'JAVA_HOME' $env:JAVA_HOME

$git = Get-Command git -ErrorAction SilentlyContinue
Line 'git' $(if ($git) { (& git --version 2>&1) } else { 'NOT FOUND' })

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
Line 'mvn' $(if ($mvn) { $mvn.Source } else { 'NOT FOUND (use mvnw)' })

$mysqld = 'E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysqld.exe'
Line 'mysqld' $(if (Test-Path $mysqld) { (& $mysqld --version 2>&1) } else { 'NOT FOUND' })

$ghCmd = Get-Command gh -ErrorAction SilentlyContinue
$ghPath = if ($ghCmd) { $ghCmd.Source } elseif (Test-Path 'E:\Develop\gh\bin\gh.exe') { 'E:\Develop\gh\bin\gh.exe' } else { $null }
Line 'gh' $(if ($ghPath) { (& $ghPath --version 2>&1 | Select-Object -First 1) } else { 'NOT FOUND' })

$node = Get-Command node -ErrorAction SilentlyContinue
Line 'node' $(if ($node) { (& node -v 2>&1) } else { 'NOT FOUND' })

$wx = Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*','HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*','HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -match '\u5fae\u4fe1\u5f00\u53d1\u8005\u5de5\u5177|WeChat DevTools' } |
    Select-Object -First 1
Line 'WeChat DevTools' $(if ($wx) { "$($wx.DisplayName) => $($wx.DisplayIcon)" } else { 'NOT FOUND' })

$wxCli = Get-ChildItem 'E:\Develop\wxDevelop' -Directory -ErrorAction SilentlyContinue |
    ForEach-Object { Join-Path $_.FullName 'cli.bat' } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1
Line 'DevTools CLI' $(if ($wxCli) { $wxCli } else { 'NOT FOUND' })

Write-Host '== Maven local repository =='
$settings = Join-Path $env:USERPROFILE '.m2\settings.xml'
if (Test-Path $settings) {
    try {
        Line 'settings.xml' $settings
        Line 'localRepository' ([xml](Get-Content $settings -Raw)).settings.localRepository
    } catch {
        Line 'settings.xml' "EXISTS but cannot parse: $settings"
    }
} else {
    Line 'settings.xml' 'NOT FOUND'
}

Write-Host '== MySQL status =='
$cnf = 'E:\Develop\MySQL\.secrets\client-root.cnf'
if (Test-Path $cnf) {
    & 'E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysqladmin.exe' --defaults-extra-file=$cnf ping 2>&1
} else {
    Line 'client-root.cnf' 'NOT FOUND'
}

Write-Host '== ports =='
foreach ($p in 3306, 8080, 80, 443) {
    $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    Line "port $p" $(if ($c) { "LISTEN pid=$($c.OwningProcess)" } else { 'free' })
}
