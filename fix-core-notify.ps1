# 对 core/ 各子包执行 fix-imports（7.7 notify）
$classMap = @{
    Notify    = 'fansirsqi.xposed.sesame.core.notify'
    ToastUtil = 'fansirsqi.xposed.sesame.core.notify'
}
foreach ($p in @('core.json', 'core.log', 'core.reflect', 'core.util', 'core.threads')) {
    & 'e:\workspace\AndroidStudioProjects\Sesame-TK\fix-imports.ps1' -ScanPkg "fansirsqi.xposed.sesame.$p" -ClassMap $classMap
}
