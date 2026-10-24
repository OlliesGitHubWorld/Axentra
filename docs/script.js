// ═══════════════════════════════════════════
// Theme toggle (dark/light)
// ═══════════════════════════════════════════

const savedTheme = localStorage.getItem('axentra-theme');
if (savedTheme === 'light') {
    document.documentElement.setAttribute('data-theme', 'light');
} else {
    document.documentElement.removeAttribute('data-theme');
}

document.querySelectorAll('.theme-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
        const isLight = document.documentElement.getAttribute('data-theme') === 'light';
        if (isLight) {
            document.documentElement.removeAttribute('data-theme');
            localStorage.setItem('axentra-theme', 'dark');
        } else {
            document.documentElement.setAttribute('data-theme', 'light');
            localStorage.setItem('axentra-theme', 'light');
        }
    });
});

// ═══════════════════════════════════════════
// Mobile nav toggle
// ═══════════════════════════════════════════

document.querySelector('.nav-toggle').addEventListener('click', () => {
    document.querySelector('.nav-links').classList.toggle('active');
});

document.querySelectorAll('.nav-links a').forEach(link => {
    link.addEventListener('click', () => {
        document.querySelector('.nav-links').classList.remove('active');
    });
});

// ═══════════════════════════════════════════
// Smooth scrolling for anchor links
// ═══════════════════════════════════════════

function smoothScrollTo(targetY, duration = 1200) {
    const startY = window.scrollY;
    const diff = targetY - startY;
    const startTime = performance.now();

    // Two-phase: 90% in first 0.2s, remaining 10% over 2.2s
    const snapTime = 200; // ms for the fast phase

    function step(now) {
        const elapsed = now - startTime;

        let value;
        if (elapsed < snapTime) {
            // Phase 1: 0% to 90% in 200ms (ease-out)
            const t = elapsed / snapTime;
            const eased = 1 - Math.pow(1 - t, 2);
            value = eased * 0.8;
        } else {
            // Phase 2: 80% to 100% over remaining time (ease-out slow crawl)
            const t = Math.min((elapsed - snapTime) / (duration - snapTime), 1);
            const eased = 1 - Math.pow(1 - t, 3);
            value = 0.8 + eased * 0.2;
        }

        window.scrollTo(0, startY + diff * value);

        if (elapsed < duration) {
            requestAnimationFrame(step);
        } else {
            window.scrollTo(0, startY + diff);
        }
    }

    requestAnimationFrame(step);
}

document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    // Skip wiki sidebar and mobile nav links (handled by wiki.js)
    if (anchor.closest('.sidebar') || anchor.closest('.wiki-mobile-nav')) return;

    anchor.addEventListener('click', (e) => {
        const targetId = anchor.getAttribute('href');
        if (targetId === '#') return;
        const target = document.querySelector(targetId);
        if (!target) return;

        e.preventDefault();
        const navHeight = document.querySelector('.nav').offsetHeight;
        const targetPos = target.getBoundingClientRect().top + window.scrollY - navHeight - 16;

        smoothScrollTo(targetPos, 1200);
    });
});

// ═══════════════════════════════════════════
// Command tabs with slide animation
// ═══════════════════════════════════════════

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const currentActive = document.querySelector('.tab-content.active');
        const nextTab = document.getElementById('tab-' + btn.dataset.tab);

        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        if (currentActive && currentActive !== nextTab) {
            currentActive.style.animation = 'tabOut 0.2s ease forwards';
            currentActive.addEventListener('animationend', function handler() {
                currentActive.classList.remove('active');
                currentActive.style.animation = '';
                currentActive.removeEventListener('animationend', handler);

                nextTab.classList.add('active');
                nextTab.style.animation = 'tabIn 0.3s ease forwards';
                nextTab.addEventListener('animationend', function handler2() {
                    nextTab.style.animation = '';
                    nextTab.removeEventListener('animationend', handler2);
                });
            });
        } else {
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            nextTab.classList.add('active');
        }
    });
});

// ═══════════════════════════════════════════
// Navbar scroll effect
// ═══════════════════════════════════════════

window.addEventListener('scroll', () => {
    const nav = document.querySelector('.nav');
    nav.classList.toggle('nav-scrolled', window.scrollY > 50);
});

// ═══════════════════════════════════════════
// Scroll reveal animations (staggered)
// ═══════════════════════════════════════════

const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('revealed');
            revealObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.08 });

document.querySelectorAll('.feature-card, .screenshot-card, .download-card, .command').forEach((el, i) => {
    // Calculate stagger delay within each group
    const parent = el.parentElement;
    const siblings = Array.from(parent.children).filter(c => c.classList.contains(el.classList[0]));
    const index = siblings.indexOf(el);
    el.style.setProperty('--reveal-delay', (index * 0.08) + 's');
    el.classList.add('reveal-element');
    revealObserver.observe(el);
});

// Section titles + subtitles
document.querySelectorAll('.section-title, .section-subtitle').forEach(el => {
    el.classList.add('reveal-element', 'reveal-fade');
    revealObserver.observe(el);
});

// ═══════════════════════════════════════════
// Hero entrance animation
// ═══════════════════════════════════════════

window.addEventListener('DOMContentLoaded', () => {
    const heroElements = [
        '.hero-badge',
        '.hero-title',
        '.hero-subtitle',
        '.hero-desc',
        '.hero-badges',
        '.hero-buttons',
        '.hero-stats'
    ];

    heroElements.forEach((selector, i) => {
        const el = document.querySelector(selector);
        if (!el) return;
        el.style.opacity = '0';
        el.style.transform = 'translateY(24px)';
        setTimeout(() => {
            el.style.transition = 'opacity 0.7s cubic-bezier(0.16, 1, 0.3, 1), transform 0.7s cubic-bezier(0.16, 1, 0.3, 1)';
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
        }, 150 + i * 100);
    });
});

// ═══════════════════════════════════════════
// Counter animation for stats
// ═══════════════════════════════════════════

function animateCounter(el, target, suffix = '') {
    const duration = 1500;
    const start = performance.now();

    function update(now) {
        const progress = Math.min((now - start) / duration, 1);
        // Ease out cubic
        const eased = 1 - Math.pow(1 - progress, 3);
        const value = Math.round(eased * target);
        el.textContent = value + suffix;
        if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
}

const statsObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const statNumbers = entry.target.querySelectorAll('.stat-number');
            statNumbers.forEach(num => {
                const text = num.textContent.trim();
                if (text === '60+') animateCounter(num, 60, '+');
                else if (text === '3') animateCounter(num, 3);
                else if (text === '0') animateCounter(num, 0);
            });
            statsObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.5 });

const heroStats = document.querySelector('.hero-stats');
if (heroStats) statsObserver.observe(heroStats);

// ═══════════════════════════════════════════
// 3D tilt effect on cards
// ═══════════════════════════════════════════

const tiltCards = document.querySelectorAll('.screenshot-card');

tiltCards.forEach(card => {
    card.addEventListener('mousemove', (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;

        const rotateX = ((y - centerY) / centerY) * -5;
        const rotateY = ((x - centerX) / centerX) * 5;

        card.style.transform = `perspective(800px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-4px)`;
    });

    card.addEventListener('mouseleave', () => {
        card.style.transform = 'perspective(800px) rotateX(0) rotateY(0) translateY(0)';
        card.style.transition = 'transform 0.5s ease';
    });

    card.addEventListener('mouseenter', () => {
        card.style.transition = 'transform 0.1s ease';
    });
});

// ═══════════════════════════════════════════
// Floating particles in hero
// ═══════════════════════════════════════════

function createParticles() {
    const hero = document.querySelector('.hero');
    if (!hero) return;

    const canvas = document.createElement('canvas');
    canvas.className = 'hero-particles';
    canvas.style.cssText = 'position:absolute;inset:0;pointer-events:none;z-index:0;';
    hero.insertBefore(canvas, hero.firstChild);

    const ctx = canvas.getContext('2d');
    let particles = [];
    let animId;

    function resize() {
        canvas.width = hero.offsetWidth;
        canvas.height = hero.offsetHeight;
    }

    function createParticle() {
        return {
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            size: Math.random() * 2 + 0.5,
            speedX: (Math.random() - 0.5) * 0.3,
            speedY: (Math.random() - 0.5) * 0.3,
            opacity: Math.random() * 0.4 + 0.1,
            pulse: Math.random() * Math.PI * 2
        };
    }

    function init() {
        resize();
        const count = Math.min(Math.floor(canvas.width * canvas.height / 15000), 60);
        particles = Array.from({ length: count }, createParticle);
    }

    function getAccentColor() {
        const hex = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();
        const r = parseInt(hex.slice(1, 3), 16) || 0;
        const g = parseInt(hex.slice(3, 5), 16) || 188;
        const b = parseInt(hex.slice(5, 7), 16) || 212;
        return { r, g, b };
    }

    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        const { r, g, b } = getAccentColor();

        particles.forEach(p => {
            p.x += p.speedX;
            p.y += p.speedY;
            p.pulse += 0.01;

            // Wrap around
            if (p.x < 0) p.x = canvas.width;
            if (p.x > canvas.width) p.x = 0;
            if (p.y < 0) p.y = canvas.height;
            if (p.y > canvas.height) p.y = 0;

            const glow = p.opacity * (0.7 + 0.3 * Math.sin(p.pulse));
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(${r}, ${g}, ${b}, ${glow})`;
            ctx.fill();
        });

        // Draw connections between nearby particles
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 120) {
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = `rgba(${r}, ${g}, ${b}, ${0.06 * (1 - dist / 120)})`;
                    ctx.lineWidth = 0.5;
                    ctx.stroke();
                }
            }
        }

        animId = requestAnimationFrame(draw);
    }

    init();
    draw();

    window.addEventListener('resize', () => {
        cancelAnimationFrame(animId);
        init();
        draw();
    });
}

createParticles();

// ═══════════════════════════════════════════
// Mouse glow effect on hero
// ═══════════════════════════════════════════

const hero = document.querySelector('.hero');
if (hero) {
    const glow = document.createElement('div');
    glow.className = 'mouse-glow';
    hero.appendChild(glow);

    hero.addEventListener('mousemove', (e) => {
        const rect = hero.getBoundingClientRect();
        glow.style.left = (e.clientX - rect.left) + 'px';
        glow.style.top = (e.clientY - rect.top) + 'px';
        glow.style.opacity = '1';
    });

    hero.addEventListener('mouseleave', () => {
        glow.style.opacity = '0';
    });
}

// ═══════════════════════════════════════════
// Smooth parallax on hero background
// ═══════════════════════════════════════════

const heroBg = document.querySelector('.hero-bg');
if (heroBg) {
    window.addEventListener('scroll', () => {
        const scrolled = window.scrollY;
        heroBg.style.transform = `translateY(${scrolled * 0.3}px)`;
    }, { passive: true });
}

// ═══════════════════════════════════════════
// Magnetic buttons
// ═══════════════════════════════════════════

document.querySelectorAll('.btn').forEach(btn => {
    btn.addEventListener('mousemove', (e) => {
        const rect = btn.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        btn.style.transform = `translate(${x * 0.15}px, ${y * 0.15}px)`;
    });

    btn.addEventListener('mouseleave', () => {
        btn.style.transform = '';
        btn.style.transition = 'transform 0.4s ease';
    });

    btn.addEventListener('mouseenter', () => {
        btn.style.transition = 'transform 0.1s ease';
    });
});
