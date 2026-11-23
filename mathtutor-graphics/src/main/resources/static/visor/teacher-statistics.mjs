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
        for (const value of [summary.module, summary.topic ?? 'Sin tema', summary.started, summary.active,
          summary.completed, summary.abandoned, summary.abandonedEarly, summary.abandonedNearSolution]) {
          const cell = document.createElement('td');
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
