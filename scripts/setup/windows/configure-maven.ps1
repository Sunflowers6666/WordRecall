$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$m2 = Join-Path $env:USERPROFILE '.m2'
$settings = Join-Path $m2 'settings.xml'
$repo = 'E:/Develop/Maven/repository'

New-Item -ItemType Directory -Force -Path $m2, 'E:\Develop\Maven\repository' | Out-Null

if (Test-Path $settings) {
    try {
        $xml = [xml](Get-Content $settings -Raw)
        $current = $xml.settings.localRepository
        if ($current -eq $repo) {
            Write-Host "already configured: $settings -> $current"
            exit 0
        }
        throw "settings.xml exists with localRepository '$current'; refusing to overwrite. Edit it manually or remove it first."
    } catch {
        if ($_.Exception.Message -like 'settings.xml exists*') { throw }
        throw "settings.xml exists but cannot be parsed: $settings. Refusing to overwrite."
    }
}

$content = @"
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <localRepository>$repo</localRepository>
</settings>
"@
[System.IO.File]::WriteAllText($settings, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "wrote $settings"

[xml]$check = Get-Content $settings -Raw
Write-Host ("localRepository=" + $check.settings.localRepository)
if ($check.settings.localRepository -ne $repo) { throw 'verification failed' }
