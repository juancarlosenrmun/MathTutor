/** @brief Presenta conteos agregados y elimina los datos al salir de la vista docente. */
export function mountTeacherStatistics(document, client) {
  const button = document.querySelector('#teacher-load');
  const status = document.querySelector('#teacher-status');
  const results = document.querySelector('#teacher-results');
  const region = document.querySelector('#teacher-table');
  let generation = 0, identity;

  function sync(view) {
    const current = view === 'espacio' && ['ADMIN', 'PROFESOR'].includes(client.profile?.role)
      ? client.profile.email : null;
    if (identity === current) return;
    identity = current;
    generation++;
    results.replaceChildren();
    region.hidden = true;
    button.disabled = false;
    status.textContent = '';
  }

  button.addEventListener('click', async () => {
    const current = ++generation;
    button.disabled = true;
    try {
      const rows = await (await client.request('/api/teacher/statistics')).json();
      if (current !== generation) return;
      results.replaceChildren(...rows.map(summary => {
        const row = document.createElement('tr');
        const columns = [
          [summary.module], [summary.topic ?? 'Sin tema'], [summary.total, 'stats-total'],
          [summary.started, 'stats-activity'], [summary.active, 'stats-activity'],
          [summary.completed, 'stats-completed'], [summary.unfinished, 'stats-activity'],
          [summary.abandoned, 'stats-deleted'], [summary.early, 'stats-progress'],
          [summary.nearSolution, 'stats-progress'], [summary.unmeasured, 'stats-progress'],
        ];
        for (const [value, style = ''] of columns) {
          const cell = document.createElement('td');
          cell.className = style;
          cell.textContent = String(value);
          row.append(cell);
        }
        return row;
      }));
      region.hidden = rows.length === 0;
      status.textContent = rows.length ? 'Estadísticas actualizadas.' : 'Todavía no hay ejercicios registrados.';
    } catch (error) {
      if (current === generation) status.textContent = error.message;
    } finally {
      if (current === generation) button.disabled = false;
    }
  });
  return { sync };
}
