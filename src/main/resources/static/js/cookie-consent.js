/* Guard: previene doble inicialización si el fragmento se incluye más de una vez */
if (window._mardiqueCC) {
  /* ya inicializado; exponer API si aún no existe y salir */
  if (!window.MardiqueCookies) {
    window.MardiqueCookies = { openPreferences: function(){}, acceptAll: function(){}, rejectNonEssential: function(){}, getConsent: function(){ return null; } };
  }
} else {
window._mardiqueCC = true;

/**
 * cookie-consent.js — Mardique Cookie Consent Module
 *
 * Categorías gestionadas:
 *   necessary     – siempre activas (sesión Spring, CSRF)
 *   personalization – Google Fonts
 *   external        – Google Maps embed
 *
 * Almacenamiento: localStorage (key: STORAGE_KEY)
 * Versión de política: POLICY_VERSION — cambiarla fuerza re-consentimiento.
 */
(function () {
  'use strict';

  /* ── Configuración ──────────────────────────────────────────────── */
  var POLICY_VERSION = '1.0';
  var STORAGE_KEY    = 'mardique_cc';

  /* ── Helpers de persistencia ────────────────────────────────────── */
  function loadConsent() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      var data = JSON.parse(raw);
      if (!data || data.version !== POLICY_VERSION) return null;
      return data;
    } catch (e) {
      return null;
    }
  }

  function saveConsent(categories) {
    var data = {
      version:   POLICY_VERSION,
      timestamp: new Date().toISOString(),
      categories: {
        necessary:       true,
        personalization: !!categories.personalization,
        external:        !!categories.external
      }
    };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch (e) {}
    return data;
  }

  /* ── Activación de recursos opcionales ─────────────────────────── */

  /**
   * Habilita fuentes de Google Fonts.
   * Los <link> tienen data-cc-href en lugar de href para bloquear la carga.
   */
  function activateFonts() {
    var links = document.querySelectorAll('link[data-cc-category="personalization"]');
    for (var i = 0; i < links.length; i++) {
      var link = links[i];
      if (link.dataset.ccHref && !link.getAttribute('href')) {
        link.setAttribute('href', link.dataset.ccHref);
      }
    }
  }

  /**
   * Sustituye los placeholders de Google Maps por el <iframe> real.
   */
  function activateMaps() {
    var placeholders = document.querySelectorAll('.cc-map-placeholder[data-cc-category="external"]');
    for (var i = 0; i < placeholders.length; i++) {
      var ph = placeholders[i];
      var src = ph.getAttribute('data-cc-src');
      if (!src) continue;

      var iframe = document.createElement('iframe');
      iframe.src              = src;
      iframe.allowFullscreen  = true;
      iframe.loading          = 'lazy';
      iframe.title            = 'Ubicación Sociedad Portuaria Mardique';
      iframe.style.width      = '100%';
      iframe.style.height     = '100%';
      iframe.style.minHeight  = '300px';
      iframe.style.border     = '0';
      iframe.style.borderRadius = '10px';

      /* Copiar clases que no sean del sistema cc- */
      var classesToCopy = [];
      ph.classList.forEach(function (cls) {
        if (!cls.startsWith('cc-')) classesToCopy.push(cls);
      });
      classesToCopy.forEach(function (cls) { iframe.classList.add(cls); });

      ph.parentNode.replaceChild(iframe, ph);
    }
  }

  /* ── Banner ─────────────────────────────────────────────────────── */
  function showBanner() {
    var banner = document.getElementById('cc-banner');
    if (!banner) return;
    banner.removeAttribute('hidden');
    banner.setAttribute('aria-hidden', 'false');
    requestAnimationFrame(function () {
      requestAnimationFrame(function () {
        banner.classList.add('cc-visible');
      });
    });
  }

  function hideBanner() {
    var banner = document.getElementById('cc-banner');
    if (!banner) return;
    banner.classList.remove('cc-visible');
    banner.setAttribute('aria-hidden', 'true');
    banner.addEventListener('transitionend', function handler() {
      banner.setAttribute('hidden', '');
      banner.removeEventListener('transitionend', handler);
    });
  }

  /* ── Modal ──────────────────────────────────────────────────────── */
  var _lastFocusedBeforeModal = null;

  function openModal() {
    var modal = document.getElementById('cc-modal');
    if (!modal) return;

    _lastFocusedBeforeModal = document.activeElement;

    /* Sincroniza toggles con preferencias guardadas o defaults */
    var current = loadConsent();
    var cats    = current ? current.categories : { personalization: false, external: false };
    setToggle('cc-toggle-personalization', cats.personalization);
    setToggle('cc-toggle-external',        cats.external);

    hideBanner();
    modal.removeAttribute('hidden');
    modal.setAttribute('aria-hidden', 'false');
    document.body.classList.add('cc-modal-open');

    /* Foco al primer elemento interactivo */
    var focusable = modal.querySelector('[data-cc-action="close-modal"]');
    if (focusable) focusable.focus();
  }

  function closeModal() {
    var modal = document.getElementById('cc-modal');
    if (!modal) return;
    modal.setAttribute('hidden', '');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('cc-modal-open');

    /* Restituir foco */
    if (_lastFocusedBeforeModal && _lastFocusedBeforeModal.focus) {
      _lastFocusedBeforeModal.focus();
    }

    /* Si aún no hay consentimiento mostrar el banner */
    if (!loadConsent()) showBanner();
  }

  /* Cierre silencioso (ya hay consentimiento guardado) */
  function closeModalSilent() {
    var modal = document.getElementById('cc-modal');
    if (!modal) return;
    modal.setAttribute('hidden', '');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('cc-modal-open');

    if (_lastFocusedBeforeModal && _lastFocusedBeforeModal.focus) {
      _lastFocusedBeforeModal.focus();
    }
  }

  function setToggle(id, value) {
    var toggle = document.getElementById(id);
    if (!toggle) return;
    toggle.checked = !!value;
    toggle.setAttribute('aria-checked', String(!!value));
  }

  /* ── Acciones principales ───────────────────────────────────────── */
  function acceptAll() {
    saveConsent({ personalization: true, external: true });
    activateFonts();
    activateMaps();
    hideBanner();
    closeModalSilent();
  }

  function rejectNonEssential() {
    saveConsent({ personalization: false, external: false });
    hideBanner();
    closeModalSilent();
  }

  function savePreferences() {
    var perso = document.getElementById('cc-toggle-personalization');
    var ext   = document.getElementById('cc-toggle-external');
    var cats  = {
      personalization: perso ? perso.checked : false,
      external:        ext   ? ext.checked   : false
    };
    saveConsent(cats);
    if (cats.personalization) activateFonts();
    if (cats.external)        activateMaps();
    closeModalSilent();
  }

  /* ── Trampa de foco en el modal ─────────────────────────────────── */
  function trapFocus(e) {
    var modal = document.getElementById('cc-modal');
    if (!modal || modal.hidden) return;
    var focusableSelectors = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
    var focusableEls = modal.querySelectorAll(focusableSelectors);
    var first = focusableEls[0];
    var last  = focusableEls[focusableEls.length - 1];
    if (e.key !== 'Tab') return;
    if (e.shiftKey) {
      if (document.activeElement === first) { e.preventDefault(); last.focus(); }
    } else {
      if (document.activeElement === last)  { e.preventDefault(); first.focus(); }
    }
  }

  /* ── Carga dinámica en el placeholder ──────────────────────────── */
  function loadSingleExternal(el) {
    var ph = el.closest('.cc-map-placeholder');
    if (!ph) return;
    /* Actualiza consentimiento: mantiene personalization, activa external */
    var current = loadConsent();
    var cats    = current ? current.categories : { personalization: false };
    saveConsent({ personalization: !!cats.personalization, external: true });
    activateMaps();
    hideBanner();
  }

  /* ── Cableado de eventos ────────────────────────────────────────── */
  function wireEvents() {
    /* Delegación de clics en data-cc-action */
    document.addEventListener('click', function (e) {
      var target = e.target && e.target.closest('[data-cc-action]');
      if (!target) return;
      e.preventDefault();
      var action = target.getAttribute('data-cc-action');
      switch (action) {
        case 'accept-all':         acceptAll();             break;
        case 'reject':             rejectNonEssential();    break;
        case 'open-modal':         openModal();             break;
        case 'close-modal':        closeModal();            break;
        case 'save-preferences':   savePreferences();       break;
        case 'load-external':      loadSingleExternal(target); break;
        default: break;
      }
    });

    /* Escape cierra el modal */
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        var modal = document.getElementById('cc-modal');
        if (modal && !modal.hidden) closeModal();
      }
      trapFocus(e);
    });

    /* Sincroniza aria-checked con el estado del toggle */
    document.addEventListener('change', function (e) {
      if (e.target && e.target.classList.contains('cc-toggle')) {
        e.target.setAttribute('aria-checked', String(e.target.checked));
      }
    });
  }

  /* ── Inicialización ─────────────────────────────────────────────── */
  function init() {
    var consent = loadConsent();
    wireEvents();

    if (consent) {
      /* Aplica preferencias guardadas */
      if (consent.categories.personalization) activateFonts();
      if (consent.categories.external)        activateMaps();
    } else {
      /* Primera visita o política nueva: mostrar banner */
      showBanner();
    }
  }

  /* ── API pública ────────────────────────────────────────────────── */
  window.MardiqueCookies = {
    /** Reabre el panel de preferencias desde cualquier enlace del sitio */
    openPreferences:    function () { openModal(); },
    acceptAll:          function () { acceptAll(); },
    rejectNonEssential: function () { rejectNonEssential(); },
    /** Devuelve el objeto de consentimiento actual o null si no hay ninguno */
    getConsent:         function () { return loadConsent(); },
    /**
     * Permite cambiar la versión de la política desde la consola o tests.
     * Se usa para forzar que el banner vuelva a aparecer al actualizar la política.
     */
    _getPolicyVersion:  function () { return POLICY_VERSION; }
  };

  /* Arrancar cuando el DOM esté listo */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
} /* fin del guard window._mardiqueCC */
