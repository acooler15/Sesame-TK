# 包迁移脚本：移动文件 + 更新 package 声明 + 更新全项目 import
param(
    [Parameter(Mandatory=$true)][string]$OldPkg,   # 如 fansirsqi.xposed.sesame.util
    [Parameter(Mandatory=$true)][string]$NewPkg,   # 如 fansirsqi.xposed.sesame.core.json
    [Parameter(Mandatory=$true)][string[]]$Files   # 文件名列表，如 JsonUtil.kt
)

$ErrorActionPreference = "Stop"
$root = "e:\workspace\AndroidStudioProjects\Sesame-TK\app\src\main"
$oldDir = Join-Path "e:\workspace\AndroidStudioProjects\Sesame-TK\app\src\main\java" ($OldPkg.Replace('.', '\'))
$newDir = Join-Path "e:\workspace\AndroidStudioProjects\Sesame-TK\app\src\main\java" ($NewPkg.Replace('.', '\'))

if (-not (Test-Path $newDir)) { New-Item -ItemType Directory -Path $newDir -Force | Out-Null }

$ClassNames = $Files | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) }

# 1. 移动文件并更新 package 声明
foreach ($f in $Files) {
    $src = Join-Path $oldDir $f
    $dst = Join-Path $newDir $f
    if (-not (Test-Path $src)) { Write-Error "源文件不存在: $src" }
    Move-Item -Path $src -Destination $dst -Force
    $content = [System.IO.File]::ReadAllText($dst)
    $content = $content -replace ("(?m)^package " + [regex]::Escape($OldPkg) + "\s*$"), ("package " + $NewPkg)
    [System.IO.File]::WriteAllText($dst, $content)
    Write-Host "已移动: $f -> $NewPkg"
}

# 2. 更新全项目 import（逐类精确替换，避免误伤子包如 util.maps）
$count = 0
Get-ChildItem -Path $root -Recurse -Include *.kt | ForEach-Object {
    $text = [System.IO.File]::ReadAllText($_.FullName)
    $newText = $text
    foreach ($cn in $ClassNames) {
        $pat = [regex]::Escape("$OldPkg.$cn") + '(?![A-Za-z0-9_])'
        $newText = $newText -replace $pat, "$NewPkg.$cn"
    }
    if ($newText -ne $text) {
        [System.IO.File]::WriteAllText($_.FullName, $newText)
        $count++
    }
}
Write-Host "已更新 import 的文件数: $count"
