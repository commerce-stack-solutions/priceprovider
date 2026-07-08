/**
 * Simple Express server to serve the shop frontend demo.
 * Serves static files from the current directory.
 */
const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Serve static files
app.use(express.static(path.join(__dirname)));

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Shop Frontend Demo running at http://localhost:${PORT}`);
  console.log(`Make sure Keycloak is running at http://localhost:8081`);
  console.log(`Make sure Price Provider Service is running at http://localhost:8080`);
});
