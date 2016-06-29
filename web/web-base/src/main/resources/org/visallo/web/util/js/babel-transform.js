/**
 * Rebuild the custom ./babel.js if changing plugins/presets
 *
 * https://github.com/v5analytics/babel-standalone
 */
(function() {
    var result = Babel.transform(input, {
      "sourceMap": ${SOURCEMAP},
      "presets": ["es2015"],
      "plugins": ["transform-react-jsx", "transform-react-display-name"]
    });

    if (result && result.map) {
        result.map.sources = [source];
    }

    return {
        sourceMap: JSON.stringify(result.map),
        code: result.code
    }
})();