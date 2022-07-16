const express = require('express');
const bodyParser = require('body-parser');
const markdownIt = require('markdown-it');
const markdownItLinkAttributes = require('markdown-it-link-attributes');
const katex = require('katex');
const markdownItKatex = require('@iktakahiro/markdown-it-katex');
const markdownItPrism = require('markdown-it-prism');

const app = express();
const appName = 'Particify Formatting Service for ARSnova';
const port = process.env.SERVER_PORT || 3020;
const defaultMdOpts = { breaks: true, linkify: true };
const linkOpts = { attrs: { target: '_blank', rel: 'nofollow noreferrer' } };
const markdown = markdownIt('zero', defaultMdOpts);
const defaultMdFeatureset = 'simple';
const markdownFeaturesets = {
  minimum: [
    'newline',
    'entity'
  ],
  simple: [
    'escape',
    'newline',
    'entity',
    'emphasis',
    'strikethrough',
    'backticks',
    'blockquote',
    'list',
    'table',
    'code',
    'fence',
    'hr',
    'linkify',
    'link',
    'image'
  ],
  extended: [
    'escape',
    'newline',
    'entity',
    'emphasis',
    'strikethrough',
    'backticks',
    'blockquote',
    'list',
    'table',
    'code',
    'fence',
    'hr',
    'linkify',
    'link',
    'image',

    'heading'
  ],
  math: [
    'math_inline',
    'math_block'
  ]
};

app.use(bodyParser.json())
markdown.use(markdownItLinkAttributes, linkOpts);
markdown.use(markdownItPrism);
markdown.use(markdownItKatex);

// Store reference because it will be overriden during reconfiguration
const highlight = markdown.options.highlight;

app.get('/', (req, res) => {
  res.send(appName);
});

app.post('/render', (req, res) => {
  if (!req.body || !req.body.text) {
    res.status(400).send({ error: 'Value for \'text\' is missing.' });

    return;
  }
  let text = req.body.text;
  const options = req.body.options;

  text = render(text, options);

  res.send({ html: text });
});

app.post('/render-multiple', (req, res) => {
  if (!req.body || !(req.body instanceof Array)) {
    res.status(400).send({ error: 'Value of type Array expected.' });

    return;
  }
  let error;
  const texts = req.body.map(item => {
    if (!item.text) {
      error = true;
      return { error: 'Value for \'text\' is missing.' };
    }
    return { html: render(item.text, item.options) };
  });

  res.status(error ? 400 : 200).send(texts);
});

app.listen(port, () => {
  console.log(`${appName} listening at port ${port}.`);
});

function render(text, options) {
  if (options) {
    const mdFeatureset = (options.markdownFeatureset || defaultMdFeatureset).toLowerCase();
    let mdOpts = defaultMdOpts;
    let mdFeatures = options.markdown
      ? markdownFeaturesets[mdFeatureset] || markdownFeaturesets[defaultMdFeatureset]
      : markdownFeaturesets.minimum;
    if (options.latex) {
      mdFeatures = mdFeatures.concat(markdownFeaturesets.math);
    }
    mdOpts = Object.assign(
      defaultMdOpts,
      { highlight: (options.syntaxHighlighting ?? true) ? highlight : () => '' });
    text = configureMarkdown(markdown, mdOpts, mdFeatures).render(text);
  }

  return text;
}

function configureMarkdown(renderer, options, features) {
  return renderer.configure('zero').set(options).enable(features);
}
