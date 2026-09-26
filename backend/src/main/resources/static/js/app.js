// Toasts succès/erreur (.agents-messages .alert) : disparaissent tout seuls après quelques
// secondes au lieu de rester affichés indéfiniment dans la page.
document.querySelectorAll('.agents-messages .alert').forEach(function (el, i) {
  setTimeout(function () {
    el.classList.add('alert-fade-out');
    el.addEventListener('transitionend', function () { el.remove(); }, { once: true });
  }, 2000 + i * 300);
});

// Confirmation stylée avant une action sensible (ex: retirer/restaurer un ménage) —
// remplace window.confirm() par une vraie modale. Se déclenche sur tout
// <form class="js-confirm-form">, y compris ceux ajoutés dynamiquement (recherche en direct),
// grâce à la délégation d'événement. Le titre, le libellé du bouton et son détail viennent des
// attributs data-confirm-title / data-confirm-label / data-confirm-detail du formulaire (avec
// des valeurs par défaut pour le retrait d'un ménage, cas historique) ; data-confirm-variant ('danger' par
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
    titleEl.textContent = form.dataset.confirmTitle || 'Voulez-vous retirer ce ménage ?';
    messageEl.textContent = form.dataset.confirmDetail || '';
    okBtn.textContent = form.dataset.confirmLabel || 'Retirer';
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

// Rechargement sans retour en haut de page : la position de défilement est mémorisée en
// quittant une page et rétablie si l'on revient sur la même page (filtre, pagination,
// recherche validée, retour après enregistrement ou retrait d'un ménage...).
(function () {
  var KEY = 'dashboard-scroll';
  window.addEventListener('pagehide', function () {
    try {
      sessionStorage.setItem(KEY, JSON.stringify({ path: location.pathname, y: window.scrollY }));
    } catch (e) { /* stockage indisponible : on garde le comportement par défaut */ }
  });
  var saved = null;
  try {
    saved = JSON.parse(sessionStorage.getItem(KEY) || 'null');
    sessionStorage.removeItem(KEY);
  } catch (e) { saved = null; }
  if (saved && saved.path === location.pathname && saved.y > 0) {
    if ('scrollRestoration' in history) history.scrollRestoration = 'manual';
    var restore = function () { window.scrollTo(0, saved.y); };
    restore();
    // La carte et le graphique changent la hauteur de la page en se dessinant.
    window.addEventListener('load', restore);
  }
})();

// Formulaire de modification d'un ménage : le nombre de membres (chef compris, obligatoire)
// limite les fiches membres qu'on peut ajouter à (nombre - 1). Au-delà, on demande de modifier
// d'abord ce nombre. Les fiches ajoutées peuvent rester vides.
(function () {
  function rows(form) {
    return Array.prototype.slice.call(form.querySelectorAll('.js-members-table tbody tr'));
  }

  // Les champs sont nommés membres[i].xxx : on renumérote après chaque retrait pour que les
  // indices restent contigus.
  function renumber(form) {
    rows(form).forEach(function (row, i) {
      row.querySelectorAll('[name^="membres["]').forEach(function (field) {
        field.name = field.name.replace(/^membres\[\d+\]/, 'membres[' + i + ']');
        if (field.id) field.id = field.name.replace(/[\[\].]/g, '');
      });
    });
  }

  function addRow(form) {
    var template = form.querySelector('.js-member-row-template');
    var tbody = form.querySelector('.js-members-table tbody');
    tbody.insertAdjacentHTML('beforeend', template.innerHTML.replace(/__INDEX__/g, String(rows(form).length)));
  }

  function maxMembres(form) {
    return parseInt(form.dataset.maxMembres, 10) || 14;
  }

  function showLimit(form, message) {
    var el = form.querySelector('.js-member-limit');
    if (!el) return;
    el.textContent = message || '';
    el.hidden = !message;
  }

  function tryAddMember(form) {
    var total = parseInt(form.querySelector('.js-nombre-membres').value, 10);
    if (!(total >= 1)) {
      showLimit(form, 'Saisissez d\'abord le nombre de membres du ménage (chef compris).');
      return;
    }
    if (1 + rows(form).length >= Math.min(total, maxMembres(form) + 1)) {
      showLimit(form, 'Le ménage compte ' + total + ' membre(s), chef compris. Pour ajouter un autre membre, '
        + 'modifiez d\'abord le nombre de membres.');
      return;
    }
    showLimit(form, '');
    addRow(form);
  }

  document.addEventListener('click', function (e) {
    var form = e.target.closest('.js-household-edit-form');
    if (!form) return;
    if (e.target.closest('.js-add-member')) {
      tryAddMember(form);
    } else if (e.target.closest('.js-remove-member')) {
      e.target.closest('tr').remove();
      renumber(form);
      showLimit(form, '');
    } else if (e.target.closest('.js-cancel-edit')) {
      var modal = form.closest('.edit-modal');
      if (modal) {
        modal.hidden = true;
        modal.querySelector('.edit-modal-body').innerHTML = '';
      } else {
        window.location.href = form.dataset.cancelUrl || '/dashboard';
      }
    }
  });

  document.addEventListener('input', function (e) {
    if (e.target.classList.contains('js-nombre-membres')) {
      showLimit(e.target.closest('.js-household-edit-form'), '');
    }
  });
})();

// Modification depuis le tableau des ménages : le formulaire s'ouvre dans une fenêtre par-dessus
// la liste, sans quitter la page. Après enregistrement, retour sur la même liste (mêmes filtres,
// même position).
(function () {
  var modal = document.getElementById('edit-modal');
  if (!modal) return;
  var body = modal.querySelector('.edit-modal-body');

  function close() {
    modal.hidden = true;
    body.innerHTML = '';
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.js-edit-household');
    if (!btn) return;
    e.preventDefault();
    var returnTo = location.pathname + location.search;
    fetch('/dashboard/households/' + btn.dataset.id + '/edit-form?returnTo=' + encodeURIComponent(returnTo))
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        // Modification refusée (ex. ménage complet pour un superviseur) : le serveur renvoie vers
        // la liste avec un message, qu'on affiche en rechargeant la page.
        if (res.redirected) {
          window.location.href = res.url;
          return null;
        }
        return res.text();
      })
      .then(function (html) {
        if (html === null) return;
        body.innerHTML = html;
        modal.hidden = false;
        body.scrollTop = 0;
      })
      .catch(function () {
        // Fenêtre indisponible : on se rabat sur la page de modification complète.
        window.location.href = '/dashboard/households/' + btn.dataset.id + '/edit?returnTo=' + encodeURIComponent(returnTo);
      });
  });

  modal.querySelector('.edit-modal-close').addEventListener('click', close);
  document.addEventListener('keydown', function (e) {
    var confirmOpen = document.getElementById('confirm-modal');
    if (e.key === 'Escape' && !modal.hidden && (!confirmOpen || confirmOpen.hidden)) close();
  });
})();
