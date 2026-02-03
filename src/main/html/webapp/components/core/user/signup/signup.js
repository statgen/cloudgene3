import Control from 'can-control';
import $ from 'jquery';

import User from 'models/user';
import template from './signup.stache';

const DUMMY_USER = new User();

/**
 * Updates the provided input and its feedback field based on the provided error.
 *
 * @param {HTMLInputElement} input The form control to update.
 * @param {HTMLElement} feedback The Bootstrap feedback field for this input.
 * @param {string | undefined} error An error string if validation failed, otherwise a falsy value.
 */
function updateControl(input, feedback, error) {
  error = error || '';

  input.setCustomValidity(error);

  input.classList.toggle('is-valid', !error);
  input.classList.toggle('is-invalid', !!error);

  feedback.textContent = error;
}

/**
 * @typedef {Object} ValidationResult
 * @property {string} value
 * @property {string | undefined} error
 */

/**
 * @returns {ValidationResult}
 */
function validateUsername() {
  const input = document.getElementById('username');
  const feedback = document.getElementById('username-feedback');

  const value = input.value;
  const error = DUMMY_USER.checkUsername(value);

  updateControl(input, feedback, error);
  return { value, error };
}

/**
 * @returns {ValidationResult}
 */
function validateFullName() {
  const input = document.getElementById('full-name');
  const feedback = document.getElementById('full-name-feedback');

  const value = input.value;
  const error = DUMMY_USER.checkName(value);

  updateControl(input, feedback, error);
  return { value, error };
}

/**
 * @param {boolean} emailRequired
 * @returns {boolean}
 */
function isAnonymous(emailRequired) {
  if (emailRequired) return false;

  /** @type {HTMLInputElement} */
  const anonymousInput = document.querySelector('[name="anonymous"]:checked');

  return anonymousInput.value === '1';
}

/**
 * @param {boolean} emailRequired
 * @returns {ValidationResult}
 */
function validateMail(emailRequired) {
  const input = document.getElementById('mail');
  const feedback = document.getElementById('mail-feedback');

  const value = input.value;
  const anonymous = isAnonymous(emailRequired);
  const error = anonymous ? undefined : DUMMY_USER.checkMail(value);

  updateControl(input, feedback, error);
  return { value, error };
}

/**
 * @returns {string | undefined}
 */
function validatePassword() {
  const passwordInput = document.getElementById('password');
  const confirmInput = document.getElementById('confirm-password');
  const feedback = document.getElementById('password-feedback');

  const password = passwordInput.value;
  const confirm = confirmInput.value;
  const error = DUMMY_USER.checkPassword(password, confirm);

  updateControl(passwordInput, feedback, error);
  return { value: password, error: error };
}

export default Control.extend({

  'init': function (element, options) {
    this.emailRequired = options.appState.attr('emailRequired');

    $(element).hide();
    $(element).html(template({
      emailRequired: options.appState.attr('emailRequired'),
      userEmailDescription: options.appState.attr('userEmailDescription'),
      userWithoutEmailDescription: options.appState.attr('userWithoutEmailDescription'),
    }));

    document.getElementById('username').addEventListener('blur', validateUsername);
    document.getElementById('full-name').addEventListener('blur', validateFullName);
    document.getElementById('mail').addEventListener('blur', () => validateMail(this.emailRequired));
    document.getElementById('password').addEventListener('blur', validatePassword);
    document.getElementById('confirm-password').addEventListener('blur', validatePassword);

    $(element).fadeIn();
  },

  '#optional-mail-accept click': function () {
    this.updateEmailControl();
  },

  '#optional-mail-reject click': function () {
    this.updateEmailControl();
  },

  'updateEmailControl': function () {
    if (!this.emailRequired) {
      const mailInput = document.getElementById('mail');
      mailInput.disabled = isAnonymous(this.emailRequired);
    }
  },

  'submit': function (element, event) {
    event.preventDefault();

    const emailRequired = this.emailRequired;
    const anonymous = isAnonymous(emailRequired);

    const { error: usernameError } = validateUsername();
    const { error: fullNameError } = validateFullName();
    const { error: mailError } = validateMail(emailRequired);
    const { error: passwordError } = validatePassword();

    if (usernameError || fullNameError || mailError || passwordError) {
      return false;
    }

    $('#save').button('loading');

    $.ajax({
      url: 'api/v2/users/register',
      type: 'POST',
      data: $(element).find('#signup-form').serialize(),
      dataType: 'json',
      success: function (data) {
        if (data.success) {
          let message = '';
          if (!anonymous) {
            message = '<b>Well done!</b> An email including the activation code has been sent to your address.';
          } else {
            message = '<b>Well done!</b> Your account is now active. <a href="/">Login now</a>.';
          }

          $('#signup-form').hide();
          $('#success-message').html(message);
          $('#success-message').removeClass('d-none');
        } else {
          updateControl(document.getElementById('username'), document.getElementById('username-feedback'), data.message);
          $('#save').button('reset');
        }
      },
      error: function (message) {
        alert('failure: ' + message);
        $('#save').button('reset');
      },
    });
  },
});
