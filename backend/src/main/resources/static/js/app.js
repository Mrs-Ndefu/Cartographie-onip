// Bascule affiché/masqué pour tout champ mot de passe précédé d'un bouton .toggle-password
document.addEventListener('click', function (e) {
  const btn = e.target.closest('.toggle-password');
  if (!btn) return;
  const input = btn.previousElementSibling;
  if (!input || (input.type !== 'password' && input.type !== 'text')) return;
  const showing = input.type === 'text';
  input.type = showing ? 'password' : 'text';
  btn.textContent = showing ? '👁' : '🙈';
  btn.setAttribute('aria-label', showing ? 'Afficher le mot de passe' : 'Masquer le mot de passe');
});
