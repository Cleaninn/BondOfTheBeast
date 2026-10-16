(() => {
  const get = id => document.getElementById(id);
  const dialog = get('shot-dialog');
  get('page-content').addEventListener('click', event => {
    const button = event.target.closest('.shot-open');
    if (!button) return;
    const original = button.querySelector('img');
    get('shot-image').src = original.currentSrc || original.src;
    get('shot-image').alt = original.alt;
    get('shot-caption').textContent = button.closest('figure').querySelector('figcaption')?.textContent || original.alt;
    dialog.showModal();
  });
  get('shot-close').addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
})();
