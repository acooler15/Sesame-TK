# 对 core/ 各子包执行 fix-imports
$classMap = @{
    GlobalThreadPools = 'fansirsqi.xposed.sesame.core.threads'
    CoroutineUtils    = 'fansirsqi.xposed.sesame.core.threads'
}
foreach ($p in @('core.json', 'core.log', 'core.reflect', 'core.util')) {
    & 'e:\workspace\AndroidStudioProjects\Sesame-TK\fix-imports.ps1' -ScanPkg "fansirsqi.xposed.sesame.$p" -ClassMap $classMap
}
