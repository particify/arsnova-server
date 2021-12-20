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
const linkOpts = { attrs: { target: '_blank', rel: 'noopener' } };
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
markdown.linkify.set({ target: '_blank' });

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
  let html = req.body.text;
  const options = req.body.options;

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
    html = configureMarkdown(markdown, mdOpts, mdFeatures).render(html);
  }

  res.send({ html: html });
});

app.listen(port, () => {
  console.log(`${appName} listening at port ${port}.`);
});

function configureMarkdown(renderer, options, features) {
  return renderer.configure('zero').set(options).enable(features);
}
