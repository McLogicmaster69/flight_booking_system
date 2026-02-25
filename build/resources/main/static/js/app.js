// Minimal, framework-free UI helpers for the template pages.

window.appNavToggle = function () {
  const nav = document.getElementById('mobileNav');
  const btn = document.querySelector('.nav-toggle');
  if (!nav || !btn) return;

  const isHidden = nav.hasAttribute('hidden');
  if (isHidden) {
    nav.removeAttribute('hidden');
    btn.setAttribute('aria-expanded', 'true');
  } else {
    nav.setAttribute('hidden', '');
    btn.setAttribute('aria-expanded', 'false');
  }
};

window.appOpenModal = function (id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.removeAttribute('hidden');
  modal.setAttribute('aria-hidden', 'false');
  const focusable = modal.querySelector('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
  if (focusable) focusable.focus();
};

window.appCloseModal = function (id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.setAttribute('hidden', '');
  modal.setAttribute('aria-hidden', 'true');
};
