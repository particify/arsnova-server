# ARSnova 2

This project really brings you *two* different versions of ARSnova: `arsnova-js` (ARSnova 2) and `arsnova-legacy-js` (ARSnova 1).

The first one is currently under heavy development and is not ready for production use. The second one is the tried-and-true ARSnova for your mobile device. However, `arsnova-legacy-js` will not receive any major updates and is nearing its end of life. It will be superseded by `arsnova-js`.

## Getting started

Both versions of ARSnova will be deployed alongside each other, so you get to choose which one you would like to use. By default, `arsnova-legacy-js` is served via `index.html` and optionally via `developer.html`. If you want to get your hands dirty, you should open `dojo-index.html` and try out the redesigned ARSnova 2. It will work on any major browser instead of being for Webkit browsers only.

## Deployment

You will need to do some configuration work upfront. Currently, you need to create a file named `config.properties` inside the `src/main/webapp` folder. The easiest way of doing this is to copy the provided `config.properties.example` and to rename it accordingly. These defaults should get you started.

