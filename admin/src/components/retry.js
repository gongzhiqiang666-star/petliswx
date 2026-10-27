export function bindRetry(container, retry) {
  container.addEventListener('click', (event) => {
    if (event.target.closest('[data-action="retry"]')) retry();
  });
}
