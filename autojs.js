
var config = {
 // ——————————|||【 屏幕解锁——上滑操作 】|||——————————\\
    上滑时长基数: 115, // 毫秒ms，滑动时长从这个基数开始添加增长算法 
    上滑起始位置: 0,  // 0=从屏幕底部开始，1=从随机位置开始
    上滑次数: 3,    // [1~5]次，0=默认5次。减少上滑次数能缩短解锁时间，但随之增大了失败率
// ----→ 解锁密码(解锁方式：1=图案解锁, 2=数字(或混合)密码，0=其它)
    解锁方式: 2,             // 解锁方式(下面二选一)
    锁屏数字密码: "0227",  // 数字(或混合)密码（解锁方式=2时生效），长度必须>=4位
    锁屏图案坐标: [
        [805,1479],     // 图案解锁坐标（解锁方式=1时生效）
        [537,1479],    // 坐标：[x, y], 逗号是“英文”逗号
        [274,1747],   // 注意遵循原格式，最后一行末尾没逗号。
        [274,2016]
    ],
    输出密码: 0,   // 0=关闭，1=开启（解锁过程中，日志打印密码），若非调试，不建议开启，以防密码泄露  
}
// 程序最大运行时间，超过该时间会强制停止(ms)。  3分钟
global.maxRuntime = (config && config.运行超时时间 || 3) * 60 * 1000;
// 设备信息
const dwidth = device.width;
const dheight = device.height;
//------------ 业务逻辑开始 ----------//

setScaleBaseX(1080);
setScaleBaseY(2400);


// 无障碍锁屏
function autoLockScreen() {
    // 无障碍服务调用系统锁屏
    try {
        // 尝试标准方式（Android 8.0+）
        auto.service.performGlobalAction(
            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        );
    } catch (e) {
        // 反射调用（兼容低版本）
        const ACTION_LOCK_SCREEN = 8; // GLOBAL_ACTION_LOCK_SCREEN 的常量值 = 8
        auto.service.performGlobalAction(ACTION_LOCK_SCREEN);
    }
}

// 点击中心坐标
function clickCenter(obj) {
    try {
        if (obj) {
            if (typeof obj === 'string') {
                obj = content(obj);
            }

            if (obj instanceof UiSelector) {
                obj = obj.findOne(2000);
            }

            if (obj && (obj instanceof UiObject)) {
                if (obj.show())
                    wait(() => false, 1000);
                let x = obj.bounds().centerX();
                let y = obj.bounds().centerY();
                //log(x,y)
                if (x > 0 && y > 0) {
                    let result = click(x, y);
                    wait(() => false, 300);

                    return result;
                }
            }
        }
    } catch (e) {}

    return false;
}

// [0-n]，不重复随机排列，返回数组，包含n
function getRandomNumbers(n) {
    let numbers = Array.from({
        length: n + 1
    }, (_, i) => i);
    let result = [];
    while (numbers.length > 0) {
        let randomIndex = Math.floor(Math.random() * numbers.length);
        let randomNumber = numbers.splice(randomIndex, 1)[0];
        result.push(randomNumber);
    }
    return result;
}

// 点亮屏幕
function screenOn() {
    //屏幕点亮

    // device.wakeUpIfNeeded();
    // device.wakeUp();

    let m = 20;
    while (!device.isScreenOn() && m--) {
        // 设备激活
        device.wakeUpIfNeeded();
        device.wakeUp();
        wait(() => false, 500);
    }
    //亮屏
    device.keepScreenDim(3*60*1000);
    wait(() => false, 500);
}

// 调用 Android KeyguardManager 检查锁屏状态
var KeyguardManager = context.getSystemService(context.KEYGUARD_SERVICE);
var isLocked = () => KeyguardManager.isKeyguardLocked(); // 是否锁屏
var isSecure = () => KeyguardManager.isKeyguardSecure(); // 是否安全锁屏（如密码、指纹）


// 多次上滑
function swipesUp(swipeCount, n) {
    swipeCount = Math.min(swipeCount, 5);
    let arr = getRandomNumbers(4);

    let durationBase = (config && config.上滑时长基数) || 115;
    log("上滑时长基数：" + durationBase)

    for (let p = 0; p < swipeCount; p++) {
        let i = config.上滑起始位置 ? arr[p] : p;
        let startY = dheight * (0.96 - 0.15 * i);
        let baseEndY = dheight * (0.65 - 0.15 * i);
        let baseDistance = startY - baseEndY;
        let endY = baseEndY;

        if (n < swipeCount - 1) {
            let distanceMultiplier = 1 + 0.1 * (swipeCount - 1 - n);
            let adaptiveMultiplier = distanceMultiplier * (1 - i * 0.02);
            let actualDistance = baseDistance * adaptiveMultiplier;
            endY = startY - actualDistance;
        }
        if (endY < 0) endY = 0;


        let duration = (durationBase + 10 * Math.pow(-1, p)) + p * 50 + (4 - n) * 50;
        duration = Math.max(duration, durationBase);

        console.warn(`--→ 第 ${p+1} 次上滑`)
        log(`位置： ${i}:${(0.96 - 0.15 * i).toFixed(2)}:(${Math.round(startY)}→${Math.round(endY)})`)
        log(`滑动时长： ${duration}`)

        swipe(
            dwidth * (4 + Math.pow(-1, i + n)) / 8,
            startY,
            dwidth * (4.5 + Math.pow(-1, i + n)) / 8,
            endY,
            duration
        );
        //  wait(() => false, 500 + (3 - n) * 50);
        //  if (p < 1) wait(() => false, 500);

        // 有安全加密
        if (isSecure) {
            // 有加密的情况下，才有解密页面
            if (wait(() => (
                    contentStartsWith('紧急').exists() ||
                    content('返回').exists()
                ), 1)) {
                log(`上滑成功！`)
                log(`需要密码解锁才能进桌面……`)
                wait(() => false, 1000);
                return;

            }
        } else {
            if (!isLocked()) {
                log(`上滑成功！`);
                log(`已经成功进入桌面……`)
                return;
            }
        }


    }
    console.warn(`————————————→ `)
    log("上滑结束！");
    wait(() => false, 1000);
}


//解锁
function unLock() {
    //  screenOn();
    if (!isLocked()) return;

    console.info("-----→");
    log("设备已锁定！！！");
    log("启动解锁程序……");

    console.info(">>>>>>>→设备解锁←<<<<<<<")

    log("开始解锁设备……");

    let swipeCount = (config && config.上滑次数) || 5;

    //解锁
    let n = 4;
    while (isLocked() && n--) {
        screenOn();
        wait(() => false, 1000);
        // 上滑
        swipesUp(swipeCount, n);

        // 有安全加密
        if (isSecure) {
            // 有加密的情况下，才有解密页面
            if (!wait(() => (
                    contentStartsWith('紧急').exists() ||
                    content('返回').exists()
                ), 3)) {
                console.error('上滑失败，重试！')
                if (n < 3) {
                    console.error('可以尝试修改配置：')
                    console.error('{上滑起始位置: ' + config.上滑起始位置 + '}')

                    if (n === 2) {
                        if (!config.上滑起始位置)
                            config.上滑起始位置 = 1
                        else
                            config.上滑起始位置 = 0;

                        console.error('尝试自动修改配置：')
                        console.error('{上滑起始位置: ' + config.上滑起始位置 + '}')
                    }
                }
                wait(() => false, 1000);
                // 锁屏
                autoLockScreen();
                wait(() => false, 1500);
                // 点亮屏幕
                // screenOn();
                continue;
            }

            content('输入密码').exists() && clickCenter('输入密码');

            if (config.解锁方式 === 1) {
                log("→图案解锁");
                let password = config.锁屏图案坐标;
                if (config.输出密码) {
                    log("坐标：");
                    password.forEach((coord, index) => {
                        console.error(`第${index+1}个坐标：[${coord[0]}, ${coord[1]}]`);
                    });
                }
                gesture(600, config.锁屏图案坐标);
            }
            if (config.解锁方式 === 2) {
                let password = config.锁屏数字密码;
                if (typeof password !== 'string') {
                    console.error('密码格式错误！');
                    console.error('密码开始和结束，必须有英文双引号！');
                    abnormalInterrupt = 0;
                    wait(() => false, 2000);
                    exit();
                    wait(() => false, 2000);
                }

                passwrd = String(password).trim();

                if (password.length < 4) {
                    console.error('密码长度必须>=4位！');
                    abnormalInterrupt = 0;
                    wait(() => false, 2000);
                    exit();
                    wait(() => false, 2000);
                }

                if (textContains('混合').exists() ||
                    contentContains('空格').exists() ||
                    contentContains('回车').exists()) {
                    log("→数字密码(混合密码)解锁");
                    let b = 20;
                    while (clickCenter('删除') && b--);
                } else {
                    log("→数字密码解锁");
                }

                for (let i = 0; i < password.length; i++) {
                    if (config.输出密码) {
                        console.error(`第${i+1}个密码字符：[${password[i]}]`);
                    }
                    let num = content(password[i]).findOne(1000);
                    if (!clickCenter(num)) {
                        console.error('[' + password[i] + '] 点击失败!')
                    };
                    wait(() => false, 300);
                }
                if (textContains('混合').exists() ||
                    contentContains('空格').exists() ||
                    contentContains('回车').exists()) {
                    clickCenter(desc('回车').findOne(1000));
                }
            }
            wait(() => false, 666);
        }

        // //去桌面
        // for (let i = 0; i < 3; i++) {
        //     toHome();
        //     wait(() => false, 300);
        // }


        wait(() => false, 300);
        if (isLocked()) {
            let k = 20;
            while (!clickCenter('返回') &&
                clickCenter('删除') &&
                k--);
            console.error('解锁失败，重试！')
        }

    }
    if (isLocked()) {
        console.error("屏幕解锁失败！！！");
        if (config && config.通知提醒)
            notice(String('出错了！(' + nowDate().substr(5, 14) + ')'), String('屏幕解锁失败了！'));

        abnormalInterrupt = 0;
        wait(() => false, 2000);
        exit();
        wait(() => false, 2000);
    }
    log("屏幕解锁成功！！！(∗❛ั∀❛ั∗)✧*。");



    return;
}





function main() {
    if (autojs.isRootAvailable()) {
        console.info("已获取root权限！");
    } else {
        console.error("未获取root权限！");
        exit();
    }
    sleep(1000);

    // 脚本执行期间防止息屏锁屏
    device.keepScreenOn();

    console.info("开始解锁");
    unLock();
    // 先回到桌面再启动支付宝
    home();
    sleep(500);
    // 反复尝试启动支付宝，直到确认当前前台是目标包名
    var maxRetries = 3;
    var retryInterval = 1500; // ms
    for (var i = 0; i < maxRetries; i++) {
        console.info("启动支付宝 (第" + (i + 1) + "次)");
        launch("com.eg.android.AlipayGphone");
        sleep(retryInterval);
        if (currentPackage() === "com.eg.android.AlipayGphone") {
            console.info("已确认支付宝在前台");
            break;
        }
        if (i < maxRetries - 1) {
            console.warn("未检测到支付宝在前台，重试...");
        }
    }
    device.cancelKeepingAwake();
    exit();
}

main()