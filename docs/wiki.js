// ═══════════════════════════════════════════
// Sidebar + mobile nav active link tracking
// ═══════════════════════════════════════════

const sections = document.querySelectorAll('.wiki-section');
const sidebarLinks = document.querySelectorAll('.sidebar-link');
const mobileNavLinks = document.querySelectorAll('.wiki-mobile-nav a');

const sectionObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const id = entry.target.id;

            sidebarLinks.forEach(link => {
                link.classList.toggle('active', link.getAttribute('href') === '#' + id);
            });

            mobileNavLinks.forEach(link => {
                const isActive = link.getAttribute('href') === '#' + id;
                link.classList.toggle('active', isActive);
                if (isActive) {
                    link.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
                }
            });
        }
    });
}, { threshold: 0.1, rootMargin: '-80px 0px -60% 0px' });

sections.forEach(section => sectionObserver.observe(section));

// ═══════════════════════════════════════════
// Wiki search
// ═══════════════════════════════════════════

const searchInput = document.getElementById('wiki-search');
if (searchInput) {
    // Build search index from all wiki content
    const searchableSections = [];
    document.querySelectorAll('.wiki-section').forEach(section => {
        const headings = section.querySelectorAll('h1, h2, h3');
        headings.forEach(h => {
            searchableSections.push({
                text: h.textContent.toLowerCase(),
                element: h,
                section: section
            });
        });
        // Also index table cells with code
        section.querySelectorAll('td code').forEach(code => {
            searchableSections.push({
                text: code.textContent.toLowerCase(),
                element: code.closest('tr') || code,
                section: section
            });
        });
    });

    // Create results dropdown
    const resultsDiv = document.createElement('div');
    resultsDiv.className = 'search-results';
    searchInput.parentElement.appendChild(resultsDiv);

    let debounceTimer;
    searchInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const query = searchInput.value.toLowerCase().trim();
            resultsDiv.innerHTML = '';

            if (query.length < 2) {
                resultsDiv.classList.remove('active');
                return;
            }

            const matches = searchableSections
                .filter(item => item.text.includes(query))
                .slice(0, 8);

            if (matches.length === 0) {
                resultsDiv.innerHTML = '<div class="search-no-result">No results found</div>';
                resultsDiv.classList.add('active');
                return;
            }

            const seen = new Set();
            matches.forEach(match => {
                const text = match.element.textContent;
                if (seen.has(text)) return;
                seen.add(text);

                const item = document.createElement('a');
                item.className = 'search-result-item';
                item.textContent = text;
                item.href = '#';
                item.addEventListener('click', (e) => {
                    e.preventDefault();
                    match.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    // Flash highlight
                    match.element.classList.add('search-highlight');
                    setTimeout(() => match.element.classList.remove('search-highlight'), 2000);
                    resultsDiv.classList.remove('active');
                    searchInput.value = '';
                });
                resultsDiv.appendChild(item);
            });

            resultsDiv.classList.add('active');
        }, 150);
    });

    // Close on click outside
    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !resultsDiv.contains(e.target)) {
            resultsDiv.classList.remove('active');
        }
    });

    // Keyboard shortcut: Ctrl+K or / to focus search
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey && e.key === 'k') || (e.key === '/' && document.activeElement.tagName !== 'INPUT')) {
            e.preventDefault();
            searchInput.focus();
        }
        if (e.key === 'Escape') {
            searchInput.blur();
            resultsDiv.classList.remove('active');
        }
    });
}

// ═══════════════════════════════════════════
// Copy button on code blocks
// ═══════════════════════════════════════════

document.querySelectorAll('.wiki-code-block').forEach(block => {
    const btn = document.createElement('button');
    btn.className = 'code-copy-btn';
    btn.textContent = 'Copy';
    btn.addEventListener('click', () => {
        const code = block.querySelector('code').textContent;
        navigator.clipboard.writeText(code).then(() => {
            btn.textContent = 'Copied!';
            btn.classList.add('copied');
            setTimeout(() => {
                btn.textContent = 'Copy';
                btn.classList.remove('copied');
            }, 2000);
        });
    });
    block.style.position = 'relative';
    block.appendChild(btn);
});

// ═══════════════════════════════════════════
// Back to top button
// ═══════════════════════════════════════════

const backToTop = document.createElement('button');
backToTop.className = 'back-to-top';
backToTop.innerHTML = '&#8593;';
backToTop.setAttribute('aria-label', 'Back to top');
document.body.appendChild(backToTop);

window.addEventListener('scroll', () => {
    backToTop.classList.toggle('visible', window.scrollY > 400);
}, { passive: true });

backToTop.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
});
