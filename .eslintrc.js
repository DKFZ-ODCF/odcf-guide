module.exports = {
    "env": {
        "browser": true,
        "jquery": true
    },
    "extends": "eslint:recommended", // do not use recommended rules while evaluation
    "parserOptions": {
        "ecmaVersion": "latest",
        "sourceType": "module"
    },
    "rules": {
        "semi": ["error", "always"],
        "indent": ["error", 4, { "SwitchCase": 1, "MemberExpression": 1, "ignoreComments": true }],
        "no-else-return": "error",
        "consistent-return": "warn",
        "space-unary-ops" : "warn",
        "no-undef": "warn",
        "no-unused-vars": "warn",
        "no-useless-escape": "warn",
    }
}
