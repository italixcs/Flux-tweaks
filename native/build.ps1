param([Parameter(Mandatory=$true)][string]$JavaHome)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build\native'
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
$vswhere=Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
$installation=& $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if(-not $installation){throw 'Install Visual Studio C++ Build Tools and the Windows SDK to build the Spotify bridge.'}
& (Join-Path $installation 'Common7\Tools\Launch-VsDevShell.ps1') -Arch amd64 -HostArch amd64 -SkipAutomaticLocation
Push-Location $buildRoot
try {
    & cl.exe /nologo /std:c++20 /EHsc /LD /O2 /MT /DWIN32_LEAN_AND_MEAN /DNOMINMAX "/I$JavaHome\include" "/I$JavaHome\include\win32" (Join-Path $PSScriptRoot 'media.cpp') /Fefluxmedia.dll windowsapp.lib ole32.lib
    if($LASTEXITCODE -ne 0){throw 'Native media bridge compilation failed.'}
} finally {Pop-Location}
