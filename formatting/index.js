const express = require('express');
const markdown = require('markdown-it');
const katex = require('katex');
const app = express();
const appName = 'Particify Formatting Service for ARSnova';
const port = process.env.SERVER_PORT || 3020;

app.get('/', (req, res) => {
  res.send($appName);
});

app.listen(port, () => {
  console.log(`${appName} listening at port ${port}.`);
});
