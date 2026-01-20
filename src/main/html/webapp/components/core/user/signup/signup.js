import Control from 'can-control';
import $ from 'jquery';

import User from 'models/user';

import template from './signup.stache';


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
        const mail = $(this.element).find("[name='mail']");
        if (anonymous){
          mail.attr('disabled','disabled');
        } else {
          mail.removeAttr('disabled');
        }
      }
   },

  'submit': function(element, event) {
    event.preventDefault();

    const that = this;
    const user = new User();

    // anonymous radiobutton
    let anonymous = false;

    if (!this.emailRequired) {
      const anonymousControl = $(element).find("[name='anonymous']:checked");
      anonymous = (anonymousControl.val() == "1");
    }

    // username
    let username = $(element).find("[name='username']");
    const usernameError = user.checkUsername(username.val());
    this.updateControl(username, usernameError);

    // fullname
    const fullname = $(element).find("[name='full-name']");
    const fullnameError = user.checkName(fullname.val());
    this.updateControl(fullname, fullnameError);

    // mail
    const mail = $(element).find("[name='mail']");
    let mailError = undefined;

    if (!anonymous) {
      mailError = user.checkMail(mail.val());
      this.updateControl(mail, mailError);
    } else {
      this.updateControl(mail, undefined);
    }

    // password
    const newPassword = $(element).find("[name='new-password']");
    const confirmNewPassword = $(element).find("[name='confirm-new-password']");
    const passwordError = user.checkPassword(newPassword.val(), confirmNewPassword.val());
    this.updateControl(newPassword, passwordError);

    if (usernameError || fullnameError || mailError || passwordError) {
      return false;
    }

    $('#save').button('loading');

    $.ajax({
      url: "api/v2/users/register",
      type: "POST",
      data: $(element).find("#signon-form").serialize(),
      dataType: 'json',
      success: function(data) {
        if (data.success == true) {
          // shows success
          let message = "";
          if (!anonymous) {
            message = "Well done!</b> An email including the activation code has been sent to your address."
          } else {
            message = "<b>Well done!</b> Your account is now active. <a href=\"/\">Login now</a>."
          }

          $('#signon-form').hide();
          $('#success-message').html(message);
          $('#success-message').show();
        } else {
          // shows error msg
          username = $('#signon-form').find("[name='username']");
          that.updateControl(username, data.message);
          $('#save').button('reset');

        }
      },
      error: function(message) {
        alert('failure: ' + message);
        $('#save').button('reset');
      }
    });

  },

  updateControl: function(control, error) {
    if (error) {
      control.removeClass('is-valid');
      control.addClass('is-invalid');
      control.closest('.mb-3').find('.invalid-feedback').html(error);
    } else {
      control.removeClass('is-invalid');
      control.addClass('is-valid');
      control.closest('.mb-3').find('.invalid-feedback').html('');
    }
  }

});
