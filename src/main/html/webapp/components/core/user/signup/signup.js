import Control from 'can-control';
import $ from 'jquery';

import User from 'models/user';

import template from './signup.stache';

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
    input.reportValidity();

    input.classList.toggle('is-valid', !error);
    input.classList.toggle('is-invalid', !!error);

    feedback.textContent = error;
  }

export default Control.extend({

  "init": function(element, options) {
    this.emailRequired = options.appState.attr('emailRequired');
    $(element).hide();
    $(element).html(template({
      emailRequired: options.appState.attr('emailRequired'),
      userEmailDescription: options.appState.attr('userEmailDescription'),
      userWithoutEmailDescription: options.appState.attr('userWithoutEmailDescription')
    }));
    $(element).fadeIn();
  },


  "#anonymous1 click" : function() {
    this.updateEmailControl();
  },

  "#anonymous2 click" : function() {
    this.updateEmailControl();
  },

  "updateEmailControl": function() {
      if (!this.emailRequired){
        const anonymousControl = $(this.element).find("[name='anonymous']:checked");
        const anonymous = (anonymousControl.val() == "1");
        if (anonymous){
          this.mail.attr('disabled','disabled');
        } else {
          this.mail.removeAttr('disabled');
        }
      }
   },

  'submit': function(element, event) {
    event.preventDefault();

    const username = document.getElementById("username");
    const usernameFeedback = document.getElementById("username-feedback");

    const fullName = document.getElementById("full-name");
    const fullNameFeedback = document.getElementById("full-name-feedback");

    const mail = document.getElementById("mail");
    const mailFeedback = document.getElementById("mail-feedback");

    const password = document.getElementById("password");
    const passwordFeedback = document.getElementById("password-feedback");

    const confirmPassword = document.getElementById("confirm-password");

    const user = new User();

    // anonymous radiobutton
    let anonymous = false;

    if (!this.emailRequired) {
      const anonymousControl = $(element).find("[name='anonymous']:checked"); // TODO(Marc): Also incorporate this one!
      anonymous = (anonymousControl.val() == "1");
    }

    // username
    const usernameError = user.checkUsername(username.value);
    updateControl(username, usernameFeedback, usernameError);

    // fullname
    const fullnameError = user.checkName(fullName.value);
    updateControl(fullName, fullNameFeedback, fullnameError);

    // mail
    let mailError = undefined;

    if (!anonymous) {
      mailError = user.checkMail(mail.value);
      updateControl(mail, mailFeedback, mailError);
    } else {
      updateControl(mail, mailFeedback, undefined);
    }

    // password
    const passwordError = user.checkPassword(password.value, confirmPassword.value);
    updateControl(password, passwordFeedback, passwordError);

    if (usernameError || fullnameError || mailError || passwordError) {
      return false;
    }

    $('#save').button('loading');

    $.ajax({
      url: 'api/v2/users/register',
      type: 'POST',
      data: $(element).find('#signon-form').serialize(),
      dataType: 'json',
      success: function(data) {
        if (data.success) {
          let message = '';
          if (!anonymous) {
            message = '<b>Well done!</b> An email including the activation code has been sent to your address.'
          } else {
            message = '<b>Well done!</b> Your account is now active. <a href="/">Login now</a>.'
          }

          $('#signon-form').hide();
          $('#success-message').html(message);
          $('#success-message').show();
        } else {
          updateControl(username, usernameFeedback, data.message);
          $('#save').button('reset');
        }
      },
      error: function(message) {
        alert('failure: ' + message);
        $('#save').button('reset');
      }
    });

  },
});
