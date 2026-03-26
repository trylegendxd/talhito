// ─── Menu Toggle ───────────────────────────────────────────────
const menuToggle = document.getElementById('menuToggle');
const navLinks = document.getElementById('navLinks');

if (menuToggle && navLinks) {
  menuToggle.addEventListener('click', () => {
    navLinks.classList.toggle('open');
  });
  navLinks.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => navLinks.classList.remove('open'));
  });
}

// ─── Formulário de Encomenda ───────────────────────────────────
const orderForm = document.getElementById('orderForm');
const formMessage = document.getElementById('formMessage');

if (orderForm) {
  orderForm.addEventListener('submit', (event) => {
    event.preventDefault();

    const nome = document.getElementById('nome').value.trim();
    const telefone = document.getElementById('telefone').value.trim();
    const produto = document.getElementById('produto').value.trim();

    if (!nome || !telefone || !produto) {
      formMessage.textContent = '⚠️ Por favor preencha os campos obrigatórios antes de enviar.';
      formMessage.style.color = '#a31616';
      return;
    }

    formMessage.innerHTML = '✅ <strong>Pedido recebido!</strong> Entraremos em contacto em breve para confirmar a sua encomenda.';
    formMessage.style.color = '#1f6b1f';
    orderForm.reset();

    // Scroll suave para a mensagem
    formMessage.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  });
}

// ─── Catálogo — Filtros e Ordenação ───────────────────────────
const categoryFilters = document.getElementById('categoryFilters');
const filterPromo = document.getElementById('filterPromo');
const sortSelect = document.getElementById('sortSelect');
const productsGrid = document.getElementById('productsGrid');
const emptyState = document.getElementById('emptyState');

let activeCategory = 'todos';

function getProducts() {
  return Array.from(document.querySelectorAll('.product-card'));
}

function applyFilters() {
  const products = getProducts();
  const onlyPromo = filterPromo && filterPromo.checked;
  const sortValue = sortSelect ? sortSelect.value : 'default';

  // Filtrar
  let visible = products.filter(p => {
    const cat = p.dataset.cat || '';
    const isPromo = p.dataset.promo === 'true';

    const matchesCat = activeCategory === 'todos' || cat === activeCategory;
    const matchesPromo = !onlyPromo || isPromo;

    return matchesCat && matchesPromo;
  });

  // Esconder todos
  products.forEach(p => {
    p.style.display = 'none';
    p.classList.remove('fade-in');
  });

  // Ordenar
  if (sortValue === 'preco-asc') {
    visible.sort((a, b) => parseFloat(a.dataset.price) - parseFloat(b.dataset.price));
  } else if (sortValue === 'preco-desc') {
    visible.sort((a, b) => parseFloat(b.dataset.price) - parseFloat(a.dataset.price));
  } else if (sortValue === 'nome') {
    visible.sort((a, b) => (a.dataset.name || '').localeCompare(b.dataset.name || '', 'pt'));
  }

  // Mostrar e reordenar no DOM
  if (visible.length === 0) {
    emptyState && (emptyState.style.display = 'block');
  } else {
    emptyState && (emptyState.style.display = 'none');
    visible.forEach((p, i) => {
      p.style.display = '';
      // Reordenar no grid
      productsGrid.appendChild(p);
      // Animação escalonada
      setTimeout(() => p.classList.add('fade-in'), i * 40);
    });
  }
}

// Botões de categoria
if (categoryFilters) {
  categoryFilters.querySelectorAll('.filter-tag').forEach(btn => {
    btn.addEventListener('click', () => {
      categoryFilters.querySelectorAll('.filter-tag').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeCategory = btn.dataset.cat;
      applyFilters();
    });
  });
}

if (filterPromo) filterPromo.addEventListener('change', applyFilters);
if (sortSelect) sortSelect.addEventListener('change', applyFilters);

// Inicializar com animação
window.addEventListener('DOMContentLoaded', () => {
  getProducts().forEach((p, i) => {
    setTimeout(() => p.classList.add('fade-in'), i * 50);
  });
});

// Função global para reset (usada no botão do empty state)
function resetFilters() {
  activeCategory = 'todos';
  if (filterPromo) filterPromo.checked = false;
  if (sortSelect) sortSelect.value = 'default';
  if (categoryFilters) {
    categoryFilters.querySelectorAll('.filter-tag').forEach(b => b.classList.remove('active'));
    const todoBtn = categoryFilters.querySelector('[data-cat="todos"]');
    if (todoBtn) todoBtn.classList.add('active');
  }
  applyFilters();
}

// ─── Botões "Encomendar" no catálogo → preenche campo produto ──
document.addEventListener('click', (e) => {
  const btn = e.target.closest('a[href="#encomenda"]');
  if (!btn) return;

  // Encontrar o nome do produto no card pai
  const card = btn.closest('.product-card');
  if (!card) return;
  const productName = card.dataset.name || '';

  // Aguardar scroll e preencher o campo produto
  setTimeout(() => {
    const produtoInput = document.getElementById('produto');
    if (produtoInput && productName) {
      if (!produtoInput.value) {
        produtoInput.value = productName;
      }
      produtoInput.focus();
    }
  }, 500);
});
