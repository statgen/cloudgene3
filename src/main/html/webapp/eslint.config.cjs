// eslint.config.js
const { defineConfig } = require("eslint/config");
const js = require("@eslint/js");
const globals = require("globals");
const importPlugin = require("eslint-plugin-import");

module.exports = defineConfig([
  // “eslint:recommended” equivalent in ESLint v9 flat config
  js.configs.recommended,

  // Global ignores (replaces .eslintignore in flat-config world)
  {
    ignores: ["node_modules/**", "dist/**", "build/**", "**/*.min.js"],
  },

  // Your project-specific settings/rules
  {
    files: ["**/*.js"],
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: "module",
      globals: {
        ...globals.browser,

        // If your app uses these as globals (common with Steal/global-format libs):
        $: "readonly",
        jQuery: "readonly",
        Raphael: "readonly",
        bootbox: "readonly",
      },
    },

    ...(importPlugin
      ? { plugins: { import: importPlugin } }
      : {}),

    rules: {
      "block-scoped-var": "error",
      "no-unused-vars": "error",

      // Modernize safely:
      "no-var": "error",
      "prefer-const": "warn",

      // Useful bug-catchers:
      "no-undef": "error",
      "no-redeclare": "error",
      "no-use-before-define": ["error", { functions: false, classes: true, variables: true }],
      "eqeqeq": ["error", "smart"],
      "no-implied-eval": "error",
    },
  },
]);
