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
        // Listes province/ville/commune du formulaire chargé (cf. initGeo plus bas).
        if (window.initGeo) window.initGeo(body);
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

// Listes déroulantes enchaînées province -> ville -> commune (formulaire de modification d'un
// ménage, création des zones). Dans un conteneur .js-geo :
// - .js-geo-province / .js-geo-ville / .js-geo-commune : les listes affichées ;
// - .js-geo-*-value : champs cachés réellement envoyés (name="province", "ville", "commune"),
//   déjà remplis par le serveur avec la valeur actuelle ;
// - .js-geo-ville-other / .js-geo-commune-other : saisie libre, via l'option "Autre" (la liste de
//   référence n'est pas exhaustive) ou affichée d'office quand la liste est vide (ville sans
//   commune connue, ville saisie librement) ;
// - .js-geo-multi : à la place de .js-geo-commune, choix de plusieurs communes (zones) — liste
//   .js-geo-commune-pick + bouton .js-geo-commune-add ; chaque commune ajoutée devient une
//   étiquette dans .js-geo-communes-chosen (champ caché name="communes").
// Les données viennent de /dashboard/geo (mêmes listes que les apps de saisie), chargées une fois.
(function () {
  var AUTRE = '__autre__';
  var OTHER_LABEL = 'Autre (saisie libre)';
  var geoPromise = null;

  function loadGeo() {
    if (!geoPromise) {
      geoPromise = fetch('/dashboard/geo').then(function (res) { return res.json(); });
    }
    return geoPromise;
  }

  // Comparaison sans casse ni accents : "Kinshasa", "KINSHASA" et "KINSHASA " sont la même ville.
  function key(value) {
    return (value || '').normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toUpperCase();
  }

  function option(value, label, selected) {
    var opt = document.createElement('option');
    opt.value = value;
    opt.textContent = label;
    opt.selected = !!selected;
    return opt;
  }

  // Affiche la liste ou la saisie libre. Une liste masquée ne doit pas rester "required" (le
  // navigateur bloquerait l'envoi sur un champ invisible).
  function showOther(select, other, useOther, listEmpty) {
    if (select.dataset.required === undefined) select.dataset.required = select.required ? 'true' : 'false';
    select.hidden = listEmpty;
    select.required = !listEmpty && select.dataset.required === 'true';
    other.hidden = !useOther;
    other.required = useOther && select.dataset.required === 'true';
  }

  // Remplit une liste. Liste vide : saisie libre affichée directement. Valeur actuelle absente de
  // la liste : option "Autre" choisie, avec la saisie libre pré-remplie.
  function fill(select, items, current, other) {
    select.innerHTML = '';
    select.appendChild(option('', '—', !current));
    var found = false;
    items.forEach(function (label) {
      var match = !!current && key(label) === key(current);
      found = found || match;
      select.appendChild(option(label.toUpperCase(), label, match));
    });
    if (!other) return;
    var listEmpty = items.length === 0;
    var isOther = listEmpty || (!!current && !found);
    select.appendChild(option(AUTRE, OTHER_LABEL, isOther && !listEmpty));
    showOther(select, other, isOther, listEmpty);
    other.value = isOther ? (current || '') : '';
  }

  function setup(box, geo) {
    var provinceSel = box.querySelector('.js-geo-province');
    var villeSel = box.querySelector('.js-geo-ville');
    var communeSel = box.querySelector('.js-geo-commune');
    var provinceVal = box.querySelector('.js-geo-province-value');
    var villeVal = box.querySelector('.js-geo-ville-value');
    var communeVal = box.querySelector('.js-geo-commune-value');
    var villeOther = box.querySelector('.js-geo-ville-other');
    var communeOther = box.querySelector('.js-geo-commune-other');
    var multi = box.querySelector('.js-geo-multi');
    var pickSel = multi && multi.querySelector('.js-geo-commune-pick');
    var pickOther = multi && multi.querySelector('.js-geo-commune-pick-other');
    var chosen = multi && multi.querySelector('.js-geo-communes-chosen');

    function villeEntry(name) {
      return geo.villes.find(function (v) { return key(v.name) === key(name); }) || null;
    }

    function villesFor(province) {
      return geo.villes
        .filter(function (v) { return !province || key(v.province) === key(province); })
        .map(function (v) { return v.name; });
    }

    function communesFor(ville) {
      var entry = villeEntry(ville);
      return entry ? entry.communes : [];
    }

    function renderCommunes() {
      var communes = communesFor(villeVal.value);
      if (communeSel) {
        fill(communeSel, communes, communeVal.value, communeOther);
      }
      if (multi) {
        fill(pickSel, communes, '', pickOther);
      }
    }

    function renderVilles() {
      fill(villeSel, villesFor(provinceVal.value), villeVal.value, villeOther);
      renderCommunes();
    }

    // --- zones : communes ajoutées une à une, affichées en étiquettes
    function addChosen(value) {
      var name = (value || '').trim().toUpperCase();
      if (!name) return;
      var exists = Array.prototype.some.call(chosen.querySelectorAll('input'), function (input) {
        return key(input.value) === key(name);
      });
      if (exists) return;
      var tag = document.createElement('span');
      tag.className = 'geo-tag';
      tag.appendChild(document.createTextNode(name));
      var hidden = document.createElement('input');
      hidden.type = 'hidden';
      hidden.name = 'communes';
      hidden.value = name;
      var remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'geo-tag-remove';
      remove.setAttribute('aria-label', 'Retirer ' + name);
      remove.textContent = '×';
      remove.addEventListener('click', function () { tag.remove(); });
      tag.appendChild(hidden);
      tag.appendChild(remove);
      chosen.appendChild(tag);
    }

    function addPicked() {
      var fromList = !pickSel.hidden && pickSel.value && pickSel.value !== AUTRE;
      addChosen(fromList ? pickSel.value : pickOther.value);
      pickSel.value = '';
      pickOther.value = '';
      if (!pickSel.hidden) showOther(pickSel, pickOther, false, false);
    }

    fill(provinceSel, geo.provinces, provinceVal.value, null);
    renderVilles();

    provinceSel.addEventListener('change', function () {
      provinceVal.value = provinceSel.value;
      villeVal.value = '';
      if (communeVal) communeVal.value = '';
      if (chosen) chosen.innerHTML = '';
      renderVilles();
    });

    villeSel.addEventListener('change', function () {
      var isOther = villeSel.value === AUTRE;
      showOther(villeSel, villeOther, isOther, false);
      villeVal.value = isOther ? villeOther.value.trim().toUpperCase() : villeSel.value;
      if (isOther) villeOther.focus();
      // Une ville de la liste impose sa province (utile si aucune province n'était choisie).
      var entry = isOther ? null : villeEntry(villeSel.value);
      if (entry && key(entry.province) !== key(provinceVal.value)) {
        provinceVal.value = entry.province;
        fill(provinceSel, geo.provinces, provinceVal.value, null);
        fill(villeSel, villesFor(provinceVal.value), villeVal.value, villeOther);
      }
      if (communeVal) communeVal.value = '';
      if (chosen) chosen.innerHTML = '';
      renderCommunes();
    });

    villeOther.addEventListener('input', function () {
      villeVal.value = villeOther.value.trim().toUpperCase();
    });

    if (communeSel) {
      communeSel.addEventListener('change', function () {
        var isOther = communeSel.value === AUTRE;
        showOther(communeSel, communeOther, isOther, false);
        communeVal.value = isOther ? communeOther.value.trim().toUpperCase() : communeSel.value;
        if (isOther) communeOther.focus();
      });
      communeOther.addEventListener('input', function () {
        communeVal.value = communeOther.value.trim().toUpperCase();
      });
    }

    if (multi) {
      pickSel.addEventListener('change', function () {
        var isOther = pickSel.value === AUTRE;
        showOther(pickSel, pickOther, isOther, false);
        if (isOther) pickOther.focus();
        else addPicked();
      });
      multi.querySelector('.js-geo-commune-add').addEventListener('click', addPicked);
      // Entrée dans la saisie libre : ajoute la commune au lieu d'envoyer le formulaire.
      pickOther.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
          e.preventDefault();
          addPicked();
        }
      });
      // Au moins une commune avant d'envoyer (une commune tapée mais pas encore ajoutée compte).
      box.addEventListener('submit', function (e) {
        if (pickOther.value.trim()) addPicked();
        if (!chosen.querySelector('input')) {
          e.preventDefault();
          e.stopImmediatePropagation();
          (pickSel.hidden ? pickOther : pickSel).setCustomValidity('Ajoutez au moins une commune.');
          (pickSel.hidden ? pickOther : pickSel).reportValidity();
        }
      }, true);
      [pickSel, pickOther].forEach(function (el) {
        el.addEventListener('input', function () { el.setCustomValidity(''); });
        el.addEventListener('change', function () { el.setCustomValidity(''); });
      });
    }
  }

  function initGeo(root) {
    var boxes = (root || document).querySelectorAll('.js-geo:not([data-geo-ready])');
    if (!boxes.length) return;
    loadGeo().then(function (geo) {
      boxes.forEach(function (box) {
        box.dataset.geoReady = 'true';
        setup(box, geo);
      });
    });
  }

  window.initGeo = initGeo;
  initGeo(document);
})();

// Formulaire "Créer un compte" (agents.html) : en choisissant un nom ou un email suggéré par la
// liste déroulante filtrée (datalist, cf. staff-names-list / staff-emails-list), remplit aussi
// l'autre champ — pour éviter une faute de frappe qui ferait échouer la vérification de
// correspondance avec le personnel importé (cf. AgentService.createAgentFromStaffList).
(function () {
  var form = document.getElementById('create-agent-form');
  if (!form) return;

  var nameInput = document.getElementById('create-fullname');
  var emailInput = document.getElementById('create-username');
  var nameOptions = document.querySelectorAll('#staff-names-list option');
  var emailOptions = document.querySelectorAll('#staff-emails-list option');

  // Les deux datalist sont rendues dans le même ordre à partir de la même liste côté serveur
  // (cf. AgentAdminController#list) : l'option à l'index i de l'une correspond à celle de l'autre.
  var nameToEmail = {};
  var emailToName = {};
  nameOptions.forEach(function (opt, i) {
    var email = emailOptions[i] ? emailOptions[i].value : null;
    if (email) {
      nameToEmail[opt.value] = email;
      emailToName[email] = opt.value;
    }
  });

  nameInput.addEventListener('input', function () {
    var email = nameToEmail[nameInput.value];
    if (email) emailInput.value = email;
  });
  emailInput.addEventListener('input', function () {
    var name = emailToName[emailInput.value];
    if (name) nameInput.value = name;
  });
})();
