/**
 * 企业官网主JavaScript文件
 * 包含交互功能和增强体验
 */

// 等待DOM加载完成
document.addEventListener('DOMContentLoaded', function() {
    console.log('网站加载完成');

    // 初始化所有功能
    initNavigation();
    initSmoothScroll();
    initServiceTabs();
    initAnimations();
    initContactForm();
});

/**
 * 导航功能初始化
 */
function initNavigation() {
    const navToggle = document.querySelector('.nav-toggle');
    const navMenu = document.querySelector('.nav-menu');

    if (navToggle && navMenu) {
        navToggle.addEventListener('click', function() {
            navMenu.classList.toggle('active');
            this.setAttribute('aria-expanded', navMenu.classList.contains('active'));
        });

        // 点击菜单项后关闭移动端菜单
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', function() {
                if (window.innerWidth < 768) {
                    navMenu.classList.remove('active');
                    navToggle.setAttribute('aria-expanded', 'false');
                }
            });
        });

        // 点击页面其他地方关闭菜单
        document.addEventListener('click', function(event) {
            if (!navMenu.contains(event.target) && !navToggle.contains(event.target)) {
                navMenu.classList.remove('active');
                navToggle.setAttribute('aria-expanded', 'false');
            }
        });
    }

    // 导航栏滚动效果
    window.addEventListener('scroll', function() {
        const navbar = document.querySelector('.navbar');
        if (window.scrollY > 100) {
            navbar.style.boxShadow = 'var(--shadow-md)';
            navbar.style.background = 'rgba(255, 255, 255, 0.98)';
        } else {
            navbar.style.boxShadow = 'var(--shadow-sm)';
            navbar.style.background = 'rgba(255, 255, 255, 0.95)';
        }
    });
}

/**
 * 平滑滚动初始化
 */
function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            const href = this.getAttribute('href');

            // 排除空链接和外部链接
            if (href === '#' || href.includes('http')) {
                return;
            }

            e.preventDefault();

            const targetElement = document.querySelector(href);
            if (targetElement) {
                const headerHeight = document.querySelector('.navbar').offsetHeight;
                const targetPosition = targetElement.offsetTop - headerHeight - 20;

                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });

                // 更新URL哈希（不滚动）
                history.pushState(null, null, href);
            }
        });
    });
}

/**
 * 服务标签切换功能
 */
function initServiceTabs() {
    const serviceTabs = document.querySelectorAll('.service-tab');
    const serviceDetails = document.querySelectorAll('.service-detail');

    if (serviceTabs.length > 0 && serviceDetails.length > 0) {
        serviceTabs.forEach(tab => {
            tab.addEventListener('click', function() {
                const tabId = this.getAttribute('data-tab');

                // 移除所有激活状态
                serviceTabs.forEach(t => t.classList.remove('active'));
                serviceDetails.forEach(d => d.classList.remove('active'));

                // 添加当前激活状态
                this.classList.add('active');
                const targetDetail = document.getElementById(tabId);
                if (targetDetail) {
                    targetDetail.classList.add('active');
                }
            });
        });
    }
}

/**
 * 动画效果初始化
 */
function initAnimations() {
    // 滚动动画 - 使用Intersection Observer API
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-in');
            }
        });
    }, observerOptions);

    // 观察需要动画的元素
    const animateElements = document.querySelectorAll('.feature-card, .product-card, .contact-card, .section-header');
    animateElements.forEach(el => {
        el.classList.add('animate-on-scroll');
        observer.observe(el);
    });

    // 添加CSS动画类
    const style = document.createElement('style');
    style.textContent = `
        .animate-on-scroll {
            opacity: 0;
            transform: translateY(30px);
            transition: opacity 0.6s ease, transform 0.6s ease;
        }

        .animate-on-scroll.animate-in {
            opacity: 1;
            transform: translateY(0);
        }
    `;
    document.head.appendChild(style);
}

/**
 * 联系表单功能（如果有的话）
 */
function initContactForm() {
    const contactForm = document.getElementById('contact-form');
    if (contactForm) {
        contactForm.addEventListener('submit', function(e) {
            e.preventDefault();

            // 获取表单数据
            const formData = new FormData(this);
            const data = Object.fromEntries(formData);

            // 简单验证
            if (!data.name || !data.email || !data.message) {
                showNotification('请填写所有必填字段', 'error');
                return;
            }

            // 模拟提交
            showNotification('消息发送中...', 'info');

            // 这里可以添加实际的表单提交逻辑
            setTimeout(() => {
                showNotification('消息发送成功！我们将尽快回复您。', 'success');
                contactForm.reset();
            }, 1500);
        });
    }

    // 邮件联系按钮点击统计（可选）
    const mailButtons = document.querySelectorAll('a[href*="mail.163.com"]');
    mailButtons.forEach(button => {
        button.addEventListener('click', function() {
            console.log('用户点击了邮件联系按钮');
            // 这里可以添加分析代码
        });
    });
}

/**
 * 显示通知消息
 */
function showNotification(message, type = 'info') {
    // 移除现有的通知
    const existingNotification = document.querySelector('.notification');
    if (existingNotification) {
        existingNotification.remove();
    }

    // 创建新通知
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'error' ? 'fa-exclamation-circle' : 'fa-info-circle'}"></i>
            <span>${message}</span>
        </div>
        <button class="notification-close"><i class="fas fa-times"></i></button>
    `;

    // 添加到页面
    document.body.appendChild(notification);

    // 添加样式
    if (!document.querySelector('#notification-styles')) {
        const styles = document.createElement('style');
        styles.id = 'notification-styles';
        styles.textContent = `
            .notification {
                position: fixed;
                top: 20px;
                right: 20px;
                background: white;
                border-radius: var(--radius-md);
                box-shadow: var(--shadow-lg);
                padding: 1rem 1.5rem;
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 1rem;
                z-index: 9999;
                animation: slideIn 0.3s ease;
                max-width: 400px;
                border-left: 4px solid var(--primary-color);
            }

            .notification-success {
                border-left-color: #10b981;
            }

            .notification-error {
                border-left-color: #ef4444;
            }

            .notification-info {
                border-left-color: var(--primary-color);
            }

            .notification-content {
                display: flex;
                align-items: center;
                gap: 0.75rem;
            }

            .notification-content i {
                font-size: 1.25rem;
            }

            .notification-success .notification-content i {
                color: #10b981;
            }

            .notification-error .notification-content i {
                color: #ef4444;
            }

            .notification-info .notification-content i {
                color: var(--primary-color);
            }

            .notification-close {
                background: none;
                border: none;
                color: var(--gray-500);
                cursor: pointer;
                font-size: 1rem;
                padding: 0.25rem;
                border-radius: var(--radius-sm);
                transition: all 0.2s ease;
            }

            .notification-close:hover {
                background: var(--gray-100);
                color: var(--gray-700);
            }

            @keyframes slideIn {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
        `;
        document.head.appendChild(styles);
    }

    // 自动消失
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        notification.style.transform = 'translateX(100%)';
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 5000);

    // 关闭按钮
    const closeButton = notification.querySelector('.notification-close');
    closeButton.addEventListener('click', () => {
        notification.style.animation = 'slideOut 0.3s ease';
        notification.style.transform = 'translateX(100%)';
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    });

    // 添加滑出动画
    if (!document.querySelector('#slideOut-animation')) {
        const slideOut = document.createElement('style');
        slideOut.id = 'slideOut-animation';
        slideOut.textContent = `
            @keyframes slideOut {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(100%);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(slideOut);
    }
}

/**
 * 当前年份更新
 */
function updateCurrentYear() {
    const yearElements = document.querySelectorAll('.current-year');
    const currentYear = new Date().getFullYear();
    yearElements.forEach(el => {
        el.textContent = currentYear;
    });
}

// 初始化年份
updateCurrentYear();

/**
 * 性能优化：延迟加载非关键资源
 */
function lazyLoadResources() {
    // 图片懒加载
    const images = document.querySelectorAll('img[data-src]');
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.removeAttribute('data-src');
                    imageObserver.unobserve(img);
                }
            });
        });
        images.forEach(img => imageObserver.observe(img));
    }
}

// 页面完全加载后执行延迟加载
window.addEventListener('load', lazyLoadResources);

/**
 * 控制台欢迎信息
 */
console.log('%c👋 欢迎访问企业官网！', 'color: #4361ee; font-size: 16px; font-weight: bold;');
console.log('%c🚀 网站使用现代技术构建，支持响应式设计和交互动画。', 'color: #7209b7; font-size: 14px;');