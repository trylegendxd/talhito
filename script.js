const menuToggle = document.getElementById('menuToggle');
const navLinks = document.getElementById('navLinks');
const orderForm = document.getElementById('orderForm');
const formMessage = document.getElementById('formMessage');

if (menuToggle && navLinks) {
  menuToggle.addEventListener('click', () => {
    navLinks.classList.toggle('open');
  });

  navLinks.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => {
      navLinks.classList.remove('open');
    });
  });
}

if (orderForm) {
  orderForm.addEventListener('submit', (event) => {
    event.preventDefault();

    const nome = document.getElementById('nome').value.trim();
    const telefone = document.getElementById('telefone').value.trim();
    const produto = document.getElementById('produto').value.trim();

    if (!nome || !telefone || !produto) {
      formMessage.textContent = 'Preenche os campos obrigatórios antes de enviar.';
      formMessage.style.color = '#a31616';
      return;
    }

    formMessage.textContent = 'Pedido registado localmente. No futuro este formulário pode ser ligado a email ou base de dados.';
    formMessage.style.color = '#1f1a19';
    orderForm.reset();
  });
}
