// 全局变量
let currentPage = 'main';
let userBalance = 1234.56;


// 页面初始化
document.addEventListener('DOMContentLoaded', function () {
    updateTime();
    setInterval(updateTime, 1000);

    // 模拟加载动画
    setTimeout(() => {
        document.body.classList.add('loaded');
    }, 500);
});

// 更新时间
function updateTime() {
    const now = new Date();
    const timeString = now.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
    const timeElement = document.querySelector('.time');
    if (timeElement) {
        timeElement.textContent = timeString;
    }
}

// 页面导航
function showPage(pageName) {
    // 更新底部导航状态
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });

    // 根据页面名称激活对应导航项
    const navItems = document.querySelectorAll('.nav-item');
    const pageIndex = {
        'main': 0,
        'bills': 1,
        'cards': 2,
        'profile': 3
    };

    if (navItems[pageIndex[pageName]]) {
        navItems[pageIndex[pageName]].classList.add('active');
    }

    currentPage = pageName;

    // 根据页面显示不同内容
    switch (pageName) {
        case 'main':
            goToIndex();
            break;
        case 'bills':
            goToBills();
            break;
        case 'cards':
            goToCards();
            break;
        case 'profile':
            goToProfile();
            break;
    }
}

// 显示主页
function goToIndex() {
    // 主页已经是默认显示的，这里可以添加刷新逻辑
    showToast('显示主页');
    window.location.href = 'index.html';
}

// 扫一扫功能
function goToScan() {
    showToast('正在打开扫一扫...');
    setTimeout(() => {
             AndroidInterface.startQrScannerActivity("qrScanCallback");
    }, 500);
}
function qrScanCallback(result) {
    console.log("扫描结果：", result);
    if (result) {
        showToast(result); // 调用 Toast
        parseCodeAndRedirect(result);
    }
}
/**
 * 调用解析二维码接口并根据结果跳转
 * @param {string} imageUrl - 二维码图片的URL
 */
function parseCodeAndRedirect(imageUrl) {
    // 获取用户token
    let token = '';
    try {
        const userDataStr = localStorage.getItem('userData');
        if (userDataStr) {
            const userData = JSON.parse(userDataStr);
            token = userData.token || '';
        }
    } catch (error) {
        console.error('获取token失败:', error);
        showToast('登录状态异常，无法解析二维码');
        return;
    }

    if (!token) {
        showToast('请先登录');
        return;
    }

    // 创建FormData对象
    const formData = new FormData();
    formData.append('imageUrl', imageUrl);

    // 调用接口
    fetch('http://graywolf.top:6031/payment/payment/parse-code', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'User-Agent': 'Apifox/1.0.0 (https://apifox.com)',
            'Accept': '*/*'
        },
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`接口请求失败: ${response.status}`);
        }
        return response.json();
    })
    .then(apiResult => {
        if (apiResult.code !== 200 || !apiResult.data) {
            throw new Error(apiResult.message || '解析二维码失败');
        }

        // 1. 提取接口返回的所有data字段
        const {
            targetId,
            targetName,
            targetType,
            bizCategory,
            userId,
            amount,
            discount,
            actualAmount
        } = apiResult.data;

        // 2. 构建URL参数（包含所有字段）
        const params = new URLSearchParams();
        params.append('targetId', targetId);
        params.append('merchant', targetName); // 保持merchant参数名兼容
        params.append('targetType', targetType);
        params.append('bizCategory', bizCategory);
        params.append('userId', userId);
        params.append('amount', amount);
        params.append('discount', discount);
        params.append('actualAmount', actualAmount);

        // 3. 跳转页面并携带所有参数
        window.location.href = `payment.html?${params.toString()}`;
    })
    .catch(error => {
        console.error('解析二维码过程出错:', error);
        showToast(error.message || '解析二维码失败，请重试');
    });
}
// 收款码功能
function goToReceive() {
    showToast('正在生成收款码...');
    setTimeout(() => {
        window.location.href = 'receive.html';
    }, 500);
}

// 出行码功能
function goToTransit() {
    showToast('正在打开出行码...');
    setTimeout(() => {
        window.location.href = 'transit.html';
    }, 500);
}

// 卡包功能
function goToCards() {
    showToast('正在打开卡包...');
    setTimeout(() => {
        window.location.href = 'cards.html';
    }, 500);
}

// 智能助手
function goToAssistant() {
    showToast('正在启动智能助手...');
    setTimeout(() => {

            AndroidInterface.startAssistantActivity();
    }, 500);
}

// 账单中心
function goToBills() {
    showToast('正在加载账单...');
    setTimeout(() => {
    // 1. 替换当前历史记录为一个空状态（移除当前页面的历史）
    history.replaceState(null, null, "");

    // 2. 跳转新页面（新页面会成为历史栈的唯一记录）
    window.location.href = 'bills.html';
    }, 500);
}

// 总资产
function goToAssets() {
    showToast('正在加载资产信息...');
    setTimeout(() => {
        window.location.href = 'assets.html';
    }, 500);
}

// 个人中心
function goToProfile() {
    showToast('正在打开个人中心...');
    setTimeout(() => {
        window.location.href = 'profile.html';
    }, 500);
}

// 充值
function goToRecharge() {
    showToast('正在打开充值页面...');
    setTimeout(() => {
        window.location.href = 'recharge.html';
    }, 500);
}

// 提现
function goToWithdraw() {
    showToast('正在打开提现页面...');
    setTimeout(() => {
        window.location.href = 'withdraw.html';
    }, 500);
}

// 交易记录
function goToHistory() {
    showToast('正在加载交易记录...');
    setTimeout(() => {
        window.location.href = 'transaction_records.html';
    }, 500);
}

// 身份审核
function goToIdentityVerification() {
    showToast('正在打开身份审核页面...');
    setTimeout(() => {
        window.location.href = 'identity_verification.html';
    }, 500);
}

// 收款记录
function goToReceiveHistory() {
    showToast('正在加载收款记录...');
    setTimeout(() => {
        window.location.href = 'receive_history.html';
    }, 500);
}

// 出行记录
function goToTransitRecords() {
    showToast('正在加载出行记录...');
    setTimeout(() => {
        window.location.href = 'transit_records.html';
    }, 500);
}

// 消息通知
function showNotifications() {
    showToast('暂无新消息');
}

// 关闭模态框
function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// 显示提示信息
function showToast(message, duration = 2000) {
    // 移除已存在的toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }

    // 创建新的toast
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;

    // 添加样式
    toast.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        font-size: 14px;
        z-index: 10000;
        animation: toastFadeIn 0.3s ease;
    `;

    // 添加动画样式
    const style = document.createElement('style');
    style.textContent = `
        @keyframes toastFadeIn {
            from {
                opacity: 0;
                transform: translate(-50%, -50%) scale(0.8);
            }
            to {
                opacity: 1;
                transform: translate(-50%, -50%) scale(1);
            }
        }
        @keyframes toastFadeOut {
            from {
                opacity: 1;
                transform: translate(-50%, -50%) scale(1);
            }
            to {
                opacity: 0;
                transform: translate(-50%, -50%) scale(0.8);
            }
        }
    `;

    if (!document.querySelector('#toast-styles')) {
        style.id = 'toast-styles';
        document.head.appendChild(style);
    }

    document.body.appendChild(toast);

    // 自动移除
    setTimeout(() => {
        toast.style.animation = 'toastFadeOut 0.3s ease';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }, duration);
}

// 模拟数据更新
function updateBalance(newBalance) {
    userBalance = newBalance;
    const balanceElement = document.querySelector('.balance-amount');
    if (balanceElement) {
        balanceElement.textContent = `¥ ${userBalance.toLocaleString('zh-CN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
    }
}

// 返回上一页
function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = 'index.html';
    }
}

// 页面加载动画
function showLoading() {
    const loading = document.createElement('div');
    loading.className = 'loading-overlay';
    loading.innerHTML = `
        <div class="loading-content">
            <div class="loading"></div>
            <p>加载中...</p>
        </div>
    `;

    loading.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(255, 255, 255, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    `;

    const content = loading.querySelector('.loading-content');
    content.style.cssText = `
        text-align: center;
        color: #333;
    `;

    const text = loading.querySelector('p');
    text.style.cssText = `
        margin-top: 16px;
        font-size: 14px;
    `;

    document.body.appendChild(loading);

    return loading;
}

function hideLoading(loadingElement) {
    if (loadingElement && loadingElement.parentNode) {
        loadingElement.remove();
    }
}

// 工具函数：格式化金额
function formatCurrency(amount) {
    return `¥ ${amount.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;
}

// 工具函数：格式化时间
function formatTime(date) {
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 工具函数：生成随机ID
function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

// 本地存储工具
const Storage = {
    set: function (key, value) {
        try {
            sessionStorage.setItem(key, JSON.stringify(value));
        } catch (e) {
            console.error('存储失败:', e);
        }
    },

    get: function (key, defaultValue = null) {
        try {
            const item = sessionStorage.getItem(key);
            return item ? JSON.parse(item) : defaultValue;
        } catch (e) {
            console.error('读取失败:', e);
            return defaultValue;
        }
    },

    remove: function (key) {
        try {
            sessionStorage.removeItem(key);
        } catch (e) {
            console.error('删除失败:', e);
        }
    },

    // 添加收款记录
    addReceiveRecord: function (record) {
        try {
            // 获取现有记录
            const records = this.get('receiveRecords', []);

            // 添加新记录到开头
            records.unshift(record);

            // 保存回本地存储
            this.set('receiveRecords', records);

            return true;
        } catch (e) {
            console.error('添加收款记录失败:', e);
            return false;
        }
    },

    // 获取最近收款记录
    getRecentReceiveRecords: function (count = 3) {
        try {
            const records = this.get('receiveRecords', []);
            return records.slice(0, count);
        } catch (e) {
            console.error('获取最近收款记录失败:', e);
            return [];
        }
    }
};

// 初始化用户数据
function initUserData() {
    const userData = Storage.get('userData', {
        balance: 1234.56,

        username: '用户',
        avatar: 'icons/a-29-gerenxinxi.png'
    });

    userBalance = userData.balance;


    updateBalance(userBalance);
}

// 页面可见性变化处理
document.addEventListener('visibilitychange', function () {
    if (!document.hidden) {
        // 页面重新可见时刷新数据
        updateTime();
        initUserData();
    }
});

// 初始化
initUserData();