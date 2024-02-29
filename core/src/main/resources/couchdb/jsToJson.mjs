/**
 * Run this script with Node.js to convert design document JS modules to JSON.
 */

import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

let dir = path.dirname(fileURLToPath(import.meta.url));
let files = await fs.readdir(dir);

files.forEach(async (filename) => {
  if (!filename.endsWith('.design.mjs')) {
    return;
  }
  let designDoc = (await import('./' + filename)).designDoc;
  if (!designDoc) {
    console.warn(filename + ' does not contain a design doc.');
    return;
  }
  console.log('Generating CouchDB JSON design doc from ' + filename + '.');
  let jsonDoc = jsToJson(designDoc);
  fs.writeFile(filename.replace(/\.mjs$/, '.json'), JSON.stringify(jsonDoc));
});

/**
 * Transforms JS map functions of the passed object to strings. JS functions are
 * not valid JSON. Additionally, redundant indentation is removed from the
 * function string.
 */
function jsToJson(designDoc) {
  var views = designDoc.views;
  Object.keys(views).forEach(function (viewName) {
    views[viewName].map = views[viewName].map.toString().replace(/\n\t{3}(\t*)/g, function (m, p1) {
      return "\n" + p1.replace(/\t/g, "  ");
    });
  });

  return designDoc;
}
