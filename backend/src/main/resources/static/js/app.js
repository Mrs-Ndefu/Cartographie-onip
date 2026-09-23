// Toasts succès/erreur (.agents-messages .alert) : disparaissent tout seuls après quelques
// secondes au lieu de rester affichés indéfiniment dans la page.
document.querySelectorAll('.agents-messages .alert').forEach(function (el, i) {
  setTimeout(function () {
    el.classList.add('alert-fade-out');
    el.addEventListener('transitionend', function () { el.remove(); }, { once: true });
  }, 2000 + i * 300);
});

// Confirmation stylée avant une action irréversible (ex: archiver/restaurer un ménage) —
// remplace window.confirm() par une vraie modale. Se déclenche sur tout
// <form class="js-confirm-form">, y compris ceux ajoutés dynamiquement (recherche en direct),
// grâce à la délégation d'événement. Le titre, le libellé du bouton et son détail viennent des
// attributs data-confirm-title / data-confirm-label / data-confirm-detail du formulaire (avec
// des valeurs par défaut pour l'archivage, cas historique) ; data-confirm-variant ('danger' par
// défaut, ou 'success') choisit la couleur du bouton de confirmation.
(function () {
  var modal = document.getElementById('confirm-modal');
  if (!modal) return;

  var titleEl = modal.querySelector('.confirm-title');
  var messageEl = modal.querySelector('.confirm-message');
  var cancelBtn = modal.querySelector('.confirm-cancel');
  var okBtn = modal.querySelector('.confirm-ok');
  var pendingForm = null;

  function open(form) {
    pendingForm = form;
    titleEl.textContent = form.dataset.confirmTitle || 'Voulez-vous archiver ce ménage ?';
    messageEl.textContent = form.dataset.confirmDetail || '';
    okBtn.textContent = form.dataset.confirmLabel || 'Archiver';
    var success = form.dataset.confirmVariant === 'success';
    okBtn.classList.toggle('btn-success', success);
    okBtn.classList.toggle('btn-danger', !success);
    modal.hidden = false;
    okBtn.focus();
  }

  function close() {
    modal.hidden = true;
    pendingForm = null;
  }

  document.addEventListener('submit', function (e) {
    var form = e.target.closest('.js-confirm-form');
    if (!form || form.dataset.confirmed === 'true') return;
    e.preventDefault();
    open(form);
  });

  cancelBtn.addEventListener('click', close);
  modal.addEventListener('click', function (e) {
    if (e.target === modal) close();
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !modal.hidden) close();
  });

  okBtn.addEventListener('click', function () {
    if (!pendingForm) return;
    pendingForm.dataset.confirmed = 'true';
    modal.hidden = true;
    pendingForm.submit();
  });
})();

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
