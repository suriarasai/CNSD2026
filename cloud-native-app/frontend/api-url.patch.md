# frontend/src/App.jsx — the only source change needed in LAB 1

Replace:

    const API_URL = 'http://localhost:3001/tasks';

With:

    const API_URL = '/api/tasks';

Why: `http://localhost:3001` is the *developer's laptop*, hard-coded into a
JavaScript bundle that will be downloaded by a browser somewhere else entirely.
A relative path lets the gateway decide where the API lives, which is what makes
LAB 4 (`--scale backend=3`) possible without rebuilding the frontend image.
