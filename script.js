// ─── Menu Toggle ───────────────────────────────────────────────
const menuToggle = document.getElementById('menuToggle');
const navLinks = document.getElementById('navLinks');

if (menuToggle && navLinks) {
  menuToggle.addEventListener('click', () => {
    navLinks.classList.toggle('open');
  });

  navLinks.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => navLinks.classList.remove('open'));
  });
}

// ─── Formulário de Encomenda ───────────────────────────────────
const orderForm = document.getElementById('orderForm');
const formMessage = document.getElementById('formMessage');

if (orderForm) {
  orderForm.addEventListener('submit', (event) => {
    event.preventDefault();

    const nome = document.getElementById('nome')?.value.trim() || '';
    const telefone = document.getElementById('telefone')?.value.trim() || '';
    const produto = document.getElementById('produto')?.value.trim() || '';

    if (!nome || !telefone || !produto) {
      formMessage.textContent = '⚠️ Por favor preencha os campos obrigatórios antes de enviar.';
      formMessage.style.color = '#a31616';
      return;
    }

    formMessage.innerHTML =
      '✅ <strong>Pedido recebido!</strong> Entraremos em contacto em breve para confirmar a sua encomenda.';
    formMessage.style.color = '#1f6b1f';

    orderForm.reset();

    formMessage.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest'
    });
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
  const onlyPromo = filterPromo ? filterPromo.checked : false;
  const sortValue = sortSelect ? sortSelect.value : 'default';

  let visible = products.filter((product) => {
    const cat = product.dataset.cat || '';
    const isPromo = product.dataset.promo === 'true';

    const matchesCategory = activeCategory === 'todos' || cat === activeCategory;
    const matchesPromo = !onlyPromo || isPromo;

    return matchesCategory && matchesPromo;
  });

  products.forEach((product) => {
    product.style.display = 'none';
    product.classList.remove('fade-in');
  });

  if (sortValue === 'preco-asc') {
    visible.sort((a, b) => parseFloat(a.dataset.price || 0) - parseFloat(b.dataset.price || 0));
  } else if (sortValue === 'preco-desc') {
    visible.sort((a, b) => parseFloat(b.dataset.price || 0) - parseFloat(a.dataset.price || 0));
  } else if (sortValue === 'nome') {
    visible.sort((a, b) =>
      (a.dataset.name || '').localeCompare((b.dataset.name || ''), 'pt')
    );
  }

  if (visible.length === 0) {
    if (emptyState) emptyState.style.display = 'block';
  } else {
    if (emptyState) emptyState.style.display = 'none';

    visible.forEach((product, index) => {
      product.style.display = '';
      if (productsGrid) productsGrid.appendChild(product);

      setTimeout(() => {
        product.classList.add('fade-in');
      }, index * 40);
    });
  }
}

// Botões de categoria
if (categoryFilters) {
  categoryFilters.querySelectorAll('.filter-tag').forEach((button) => {
    button.addEventListener('click', () => {
      categoryFilters.querySelectorAll('.filter-tag').forEach((btn) => {
        btn.classList.remove('active');
      });

      button.classList.add('active');
      activeCategory = button.dataset.cat || 'todos';
      applyFilters();
    });
  });
}

if (filterPromo) {
  filterPromo.addEventListener('change', applyFilters);
}

if (sortSelect) {
  sortSelect.addEventListener('change', applyFilters);
}

// Inicializar com animação
window.addEventListener('DOMContentLoaded', () => {
  getProducts().forEach((product, index) => {
    setTimeout(() => {
      product.classList.add('fade-in');
    }, index * 50);
  });
});

// Função global para reset
function resetFilters() {
  activeCategory = 'todos';

  if (filterPromo) filterPromo.checked = false;
  if (sortSelect) sortSelect.value = 'default';

  if (categoryFilters) {
    categoryFilters.querySelectorAll('.filter-tag').forEach((btn) => {
      btn.classList.remove('active');
    });

    const allButton = categoryFilters.querySelector('[data-cat="todos"]');
    if (allButton) allButton.classList.add('active');
  }

  applyFilters();
}

// Deixar disponível globalmente caso o botão use onclick="resetFilters()"
window.resetFilters = resetFilters;

// ─── Botões "Encomendar" no catálogo ──────────────────────────
document.addEventListener('click', (event) => {
  const button = event.target.closest('a[href="#encomenda"]');
  if (!button) return;

  const card = button.closest('.product-card');
  if (!card) return;

  const productName = card.dataset.name || '';

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
