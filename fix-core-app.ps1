# Task 7.10 隐式引用修复：util、util/maps、core/* 子包
$classMap = @{
    AssetUtil        = 'fansirsqi.xposed.sesame.core.app'
    Files            = 'fansirsqi.xposed.sesame.core.app'
    IconManager      = 'fansirsqi.xposed.sesame.core.app'
    ModuleStatus     = 'fansirsqi.xposed.sesame.core.app'
    NetworkUtils     = 'fansirsqi.xposed.sesame.core.app'
    StatusManager    = 'fansirsqi.xposed.sesame.core.app'
    SwipeUtil        = 'fansirsqi.xposed.sesame.core.app'
    UnlockUtil       = 'fansirsqi.xposed.sesame.core.app'
    WakeLockManager  = 'fansirsqi.xposed.sesame.core.app'
    CommandUtil      = 'fansirsqi.xposed.sesame.core.app'
    FansirsqiUtil    = 'fansirsqi.xposed.sesame.core.app'
    DirectoryWatcher = 'fansirsqi.xposed.sesame.core.app'
    defaultBlacklist = 'fansirsqi.xposed.sesame.core.app'
    TaskBlacklist    = 'fansirsqi.xposed.sesame.core.app'
}
$packages = @(
    'util', 'util.maps',
    'core.json', 'core.log', 'core.reflect', 'core.util',
    'core.threads', 'core.notify', 'core.permission', 'core.store'
)
foreach ($p in $packages) {
    & 'e:\workspace\AndroidStudioProjects\Sesame-TK\fix-imports.ps1' -ScanPkg "fansirsqi.xposed.sesame.$p" -ClassMap $classMap
}
