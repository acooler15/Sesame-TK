# 为原包中隐式引用已迁出类的文件补充 import（按行处理，稳健版）
param(
    [Parameter(Mandatory=$true)][string]$ScanPkg,     # 扫描的包（目录），如 fansirsqi.xposed.sesame.util
    [Parameter(Mandatory=$true)][hashtable]$ClassMap   # 类名 -> 新包全名
)

$ErrorActionPreference = "Stop"
$scanDir = Join-Path "e:\workspace\AndroidStudioProjects\Sesame-TK\app\src\main\java" ($ScanPkg.Replace('.', '\'))

Get-ChildItem -Path $scanDir -Filter *.kt -File | ForEach-Object {
    $file = $_.FullName
    $text = [System.IO.File]::ReadAllText($file)
    $needed = @()
    foreach ($cn in $ClassMap.Keys) {
        if ($text -match "\b$cn\b" -and $text -notmatch "(?m)^import\s+[\w.]+\.$cn\s*$") {
            $needed += "import $($ClassMap[$cn]).$cn"
        }
    }
    if ($needed.Count -gt 0) {
        $lines = $text -split "`r?`n"
        $pkgIdx = -1
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match "^package\s+") { $pkgIdx = $i; break }
        }
        if ($pkgIdx -ge 0) {
            $insert = @() + ($needed | Sort-Object)
            $newLines = $lines[0..$pkgIdx] + $insert + $lines[($pkgIdx+1)..($lines.Count-1)]
            [System.IO.File]::WriteAllText($file, ($newLines -join "`r`n"))
            Write-Host "已补充 import: $($_.Name) -> $($needed -join ', ')"
        }
    }
}
