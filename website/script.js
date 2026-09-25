document.getElementById('year').textContent = new Date().getFullYear();

const navToggle = document.getElementById('navToggle');
const nav = document.getElementById('nav');

navToggle.addEventListener('click', () => {
  nav.classList.toggle('open');
});

nav.querySelectorAll('a').forEach((link) => {
  link.addEventListener('click', () => nav.classList.remove('open'));
});

const WHATSAPP_NUMBER = '573216905058';

document.getElementById('contactForm').addEventListener('submit', (e) => {
  e.preventDefault();
  const nombre = document.getElementById('nombre').value.trim();
  const telefono = document.getElementById('telefono').value.trim();
  const email = document.getElementById('email').value.trim();
  const mensaje = document.getElementById('mensaje').value.trim();

  const lines = [
    `Hola, mi nombre es ${nombre}.`,
    `Teléfono: ${telefono}`,
    email ? `Correo: ${email}` : null,
    `Mensaje: ${mensaje}`,
  ].filter(Boolean);

  const text = encodeURIComponent(lines.join('\n'));
  window.open(`https://wa.me/${WHATSAPP_NUMBER}?text=${text}`, '_blank', 'noopener');
});
