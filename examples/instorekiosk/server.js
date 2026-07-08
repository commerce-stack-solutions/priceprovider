const express = require('express');
const path = require('path');
const app = express();
const port = 3002;

// Serve static files from the Flutter web build directory
app.use(express.static(path.join(__dirname, 'build/web')));

// Fallback to index.html for SPA routing
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build/web', 'index.html'));
});

app.listen(port, () => {
  console.log(`In-Store Kiosk demo listening at http://localhost:${port}`);
});
