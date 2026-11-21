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
// Demo tabs (See it in action)
// ═══════════════════════════════════════════

document.querySelectorAll('.demo-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.demo-btn').forEach(b => {
            b.classList.remove('active');
            b.setAttribute('aria-selected', 'false');
        });
        document.querySelectorAll('.demo-panel').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        btn.setAttribute('aria-selected', 'true');
        document.getElementById('demo-' + btn.dataset.demo).classList.add('active');
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
}, { passive: true });

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
        }, i * 60);
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
                // "110+" -> 110 with suffix "+"
                const match = num.textContent.trim().match(/^(\d+)(\D*)$/);
                if (match) animateCounter(num, parseInt(match[1], 10), match[2]);
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
    if (!hero || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const canvas = document.createElement('canvas');
    canvas.className = 'hero-particles';
    canvas.style.cssText = 'position:absolute;inset:0;pointer-events:none;z-index:0;';
    hero.insertBefore(canvas, hero.firstChild);

    const ctx = canvas.getContext('2d');
    let particles = [];
    let animId;
    let visible = true;

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
        const count = Math.min(Math.floor(canvas.width * canvas.height / 15000), window.innerWidth < 768 ? 25 : 60);
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

        if (visible) animId = requestAnimationFrame(draw);
    }

    init();
    draw();

    // Stop animating while the hero is off screen
    new IntersectionObserver(([entry]) => {
        if (entry.isIntersecting === visible) return;
        visible = entry.isIntersecting;
        if (visible) draw();
    }).observe(hero);

    window.addEventListener('resize', () => {
        // Mobile browsers fire resize when the address bar collapses
        if (hero.offsetWidth === canvas.width) return;
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

// ═══════════════════════════════════════════
// Live data: latest release + server count
// ═══════════════════════════════════════════

// sessionStorage can throw (private mode, blocked storage), so every access is guarded
function cacheGet(key) {
    try { return sessionStorage.getItem(key); } catch (e) { return null; }
}

function cacheSet(key, value) {
    try { sessionStorage.setItem(key, value); } catch (e) { /* ignore */ }
}

function fetchJson(url, timeoutMs) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    return fetch(url, { signal: controller.signal })
        .then(res => {
            if (!res.ok) throw new Error('HTTP ' + res.status);
            return res.json();
        })
        .finally(() => clearTimeout(timer));
}

// Hero badge: keeps the hardcoded text unless GitHub has a newer release
const releaseBadge = document.getElementById('release-badge');
if (releaseBadge) {
    // Compares "v1.8.2"-style versions number by number
    const isNewer = (tag, than) => {
        const a = (tag.match(/\d+/g) || []).map(Number);
        const b = (than.match(/\d+/g) || []).map(Number);
        for (let i = 0; i < Math.max(a.length, b.length); i++) {
            if ((a[i] || 0) !== (b[i] || 0)) return (a[i] || 0) > (b[i] || 0);
        }
        return false;
    };
    const applyRelease = (tag, name) => {
        const current = releaseBadge.textContent.trim().split(' ')[0];
        if (!tag || !isNewer(tag, current)) return;
        const title = (name || tag).replace(/^Axentra\s+/i, '');
        releaseBadge.textContent = title.includes(tag) ? title : tag + ' - ' + title;
    };

    const cached = cacheGet('axentra-release');
    if (cached) {
        try { const r = JSON.parse(cached); applyRelease(r.tag, r.name); } catch (e) { /* ignore */ }
    } else {
        fetchJson('https://api.github.com/repos/OlliesGitHubWorld/Axentra/releases/latest', 3000)
            .then(data => {
                cacheSet('axentra-release', JSON.stringify({ tag: data.tag_name, name: data.name }));
                applyRelease(data.tag_name, data.name);
            })
            .catch(() => { /* keep the fallback text */ });
    }
}

// "Used on X servers": latest point of the bStats servers chart, hidden when unavailable
const serverCount = document.getElementById('server-count');
if (serverCount) {
    const showCount = count => {
        if (!(count > 0)) return;
        serverCount.querySelector('strong').textContent = count.toLocaleString('en-US');
        serverCount.querySelector('.hero-usage-unit').textContent = count === 1 ? 'server' : 'servers';
        serverCount.hidden = false;
    };

    const cached = parseInt(cacheGet('axentra-servers'), 10);
    if (cached > 0) {
        showCount(cached);
    } else {
        fetchJson('https://bstats.org/api/v1/plugins/30019/charts/servers/data?maxElements=1', 4000)
            .then(points => {
                const last = Array.isArray(points) && points.length ? points[points.length - 1] : null;
                const count = last ? Number(last[1]) : 0;
                if (count > 0) cacheSet('axentra-servers', String(count));
                showCount(count);
            })
            .catch(() => { /* stays hidden */ });
    }
}

// ═══════════════════════════════════════════
// Screenshot lightbox
// ═══════════════════════════════════════════

const lightboxLinks = Array.from(document.querySelectorAll('.lightbox-link'));

if (lightboxLinks.length) {
    const box = document.createElement('div');
    box.className = 'lightbox';
    box.setAttribute('role', 'dialog');
    box.setAttribute('aria-modal', 'true');
    box.setAttribute('aria-label', 'Screenshot viewer');
    box.innerHTML =
        '<span class="lightbox-counter" aria-live="polite"></span>' +
        '<button class="lightbox-btn lightbox-close" aria-label="Close">&times;</button>' +
        '<button class="lightbox-btn lightbox-prev" aria-label="Previous screenshot">&lsaquo;</button>' +
        '<button class="lightbox-btn lightbox-next" aria-label="Next screenshot">&rsaquo;</button>' +
        '<figure class="lightbox-figure"><img class="lightbox-img" alt=""><figcaption class="lightbox-caption"></figcaption></figure>';
    document.body.appendChild(box);

    const img = box.querySelector('.lightbox-img');
    const caption = box.querySelector('.lightbox-caption');
    const counter = box.querySelector('.lightbox-counter');
    const closeBtn = box.querySelector('.lightbox-close');
    let current = 0;
    let lastFocus = null;

    function show(index) {
        current = (index + lightboxLinks.length) % lightboxLinks.length;
        const link = lightboxLinks[current];
        const thumb = link.querySelector('img');
        img.src = link.getAttribute('href');
        img.alt = thumb ? thumb.alt : '';
        caption.textContent = link.dataset.caption || '';
        counter.textContent = (current + 1) + ' / ' + lightboxLinks.length;
    }

    function open(index) {
        lastFocus = document.activeElement;
        show(index);
        box.classList.add('open');
        document.body.classList.add('lightbox-open');
        closeBtn.focus();
    }

    function close() {
        box.classList.remove('open');
        document.body.classList.remove('lightbox-open');
        if (lastFocus) lastFocus.focus();
    }

    lightboxLinks.forEach((link, i) => {
        link.addEventListener('click', e => {
            e.preventDefault();
            open(i);
        });
    });

    closeBtn.addEventListener('click', close);
    box.querySelector('.lightbox-prev').addEventListener('click', () => show(current - 1));
    box.querySelector('.lightbox-next').addEventListener('click', () => show(current + 1));

    // Tap outside the image closes
    box.addEventListener('click', e => {
        if (e.target === box || e.target.classList.contains('lightbox-figure')) close();
    });

    document.addEventListener('keydown', e => {
        if (!box.classList.contains('open')) return;
        if (e.key === 'Escape') close();
        else if (e.key === 'ArrowLeft') show(current - 1);
        else if (e.key === 'ArrowRight') show(current + 1);
        else if (e.key === 'Tab') {
            // Keep focus inside the viewer
            const buttons = Array.from(box.querySelectorAll('button'));
            const at = buttons.indexOf(document.activeElement);
            e.preventDefault();
            buttons[(at + (e.shiftKey ? -1 : 1) + buttons.length) % buttons.length].focus();
        }
    });

    // Swipe left/right to browse
    let touchX = null;
    let touchY = null;
    box.addEventListener('touchstart', e => {
        touchX = e.touches[0].clientX;
        touchY = e.touches[0].clientY;
    }, { passive: true });
    box.addEventListener('touchend', e => {
        if (touchX === null) return;
        const dx = e.changedTouches[0].clientX - touchX;
        const dy = e.changedTouches[0].clientY - touchY;
        if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) show(current + (dx < 0 ? 1 : -1));
        touchX = null;
    }, { passive: true });
}
