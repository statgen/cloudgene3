import Control from 'can-control';
import deparam from 'can-deparam';
import $ from 'jquery';
import bootbox from 'bootbox';

import ErrorPage from 'helpers/error-page';
import User from 'models/user';
import Template from 'models/template';
import UserToken from 'models/user-token';
import UserProfile from 'models/user-profile';

import template from './profile.stache';
import templateDeleteDialog from './dialogs/delete.stache';
import templateNewTokenDialog from './dialogs/new.stache'


export default Control.extend({

  "init": function(element, options) {

    this.emailRequired = options.appState.attr('emailRequired');
    const username = options.appState.attr('user').attr('username');

    $(element).hide();

    User.findOne({
      user: username
    }, function(user) {
      $(element).html(template({
        user: user,
        anonymousAccount: (!options.appState.attr('emailRequired')),
        emailProvided: (user.attr('mail') != "" && user.attr('mail') != undefined),
        userEmailDescription: options.appState.attr('userEmailDescription'),
        userWithoutEmailDescription: options.appState.attr('userWithoutEmailDescription')
      }));
      options.user = user;
      $(element).fadeIn();
    });
  },

  "#anonymous click" : function(){
    if (!this.emailRequired){
      const anonymousControl = $(this.element).find("[name='anonymous']");
      const anonymous = !anonymousControl.is(':checked');
      const mail = $(this.element).find("[name='mail']");

      if (anonymous){
        mail.attr('disabled','disabled');
      } else {
        mail.removeAttr('disabled');
      }
      mail.val("");
    }
  },

  'submit': function(element, event) {
    event.preventDefault();
    const user = new User();

    // fullname
    const fullname = $(element).find("[name='full-name']");
    const fullnameError = user.checkName(fullname.val());
    this.updateControl(fullname, fullnameError);

    let anonymous = false;
    if (!this.emailRequired){
      const anonymousControl = $(this.element).find("[name='anonymous']");
      anonymous = !anonymousControl.is(':checked');
    }

    // mail
    const mail = $(element).find("[name='mail']");
    if (!anonymous){
      const mailError = user.checkMail(mail.val());
      this.updateControl(mail, mailError);
    } else {
      this.updateControl(mail, undefined);
    }

    // password if password is not empty. else no password update on server side
    const newPassword = $(element).find("[name='new-password']");
    let newPasswordError = undefined;
    if (newPassword.val() !== "") {
      const confirmNewPassword = $(element).find("[name='confirm-new-password']");
      newPasswordError = user.checkPassword(newPassword.val(), confirmNewPassword.val());
      this.updateControl(confirmNewPassword, newPasswordError);
    }
    if (fullnameError || mailError || newPasswordError) {
      return false;
    }

    $.ajax({
      url: "api/v2/users/me/profile",
      type: "POST",
      data: $(element).find("#account-form").serialize(),
      dataType: 'json',
      success: function(data) {

        if (data.success == true) {

          // shows okey
          bootbox.alert(data.message);

        } else {
          // shows error
          bootbox.alert(data.message);

        }
      },
      error: function(response) {
        new ErrorPage(element, response);
      }
    });

  },

  '#create_token click': function() {

    //load template
    const that = this;
    Template.findOne({
      key: 'TERMS'
    }, function(template) {

      bootbox.confirm({
        title: 'Terms of Service',
        message: template.attr('text'),
        buttons: {
          confirm: {
            label: 'I Agree',
            className: 'btn-success'
          }
        },
        callback: function(result) {
          if (result) {

            const token_expiration = $('#token_expiration').val();

            const user = that.options.user;

            const userToken = new UserToken();
            userToken.attr('user', user.attr('username'));
            userToken.attr('expiration', token_expiration);


            userToken.save(function(responseText) {
              user.attr('hasApiToken', true);
              user.attr('apiTokenValid', true);
              user.attr('apiTokenMessage', "");
              bootbox.alert({
                title: 'New API Token',
                message: templateNewTokenDialog({
                  token: responseText.token,
                })
              });
            }, function(message) {
              bootbox.alert({
                title: 'New API Token',
                message: 'Error: ' + message,
              });
            });
          }
        }
      })
    });
  },

  '#revoke_token click': function() {

    const user = this.options.user;

    bootbox.confirm({
      title: 'Revoke API Token',
      message: "Are you sure you want to revoke your <b>API Token</b>? All your applications and scripts that are you using this API token have to be changed!",
      callback: function(result) {
        if (result) {
          const userToken = new UserToken();
          userToken.attr('user', user.attr('username'));
          userToken.attr('id', 'luki');
          userToken.destroy(function() {
            user.attr('hasApiToken', false);
            user.attr('apiTokenValid', true);
            user.attr('apiTokenMessage', "");
            bootbox.alert({
              title: 'Revoke API Token',
              message: 'Your token is now inactive.',
            });
          }, function(response) {
            bootbox.alert({
              title: 'API Token',
              message: 'Error: ' + response,
            });
          });
        }
      }
    });
  },

  updateControl: function(control, error) {
    if (error) {
      control.removeClass('is-valid');
      control.addClass('is-invalid');
      control.closest('.form-group').find('.invalid-feedback').html(error);
    } else {
      control.removeClass('is-invalid');
      control.addClass('is-valid');
      control.closest('.form-group').find('.invalid-feedback').html('');
    }
  },

  '#delete_account click': function() {

    const deleteAcountDialog = bootbox.dialog({
      title: 'Deleting Account',
      message: templateDeleteDialog(),
      buttons: {
        cancel: {
          label: "Cancel",
          class: "btn-default",
          callback: function() {}
        },
        ok: {
          label: "Delete Account",
          class: "btn-danger",
          callback: function() {

            // get form parameters
            const form = deleteAcountDialog.find("form");
            const values = deparam(form.serialize());

            // create delete request
            const userProfile = new UserProfile();
            userProfile.attr('user', values['username']);
            userProfile.attr('username', values['username']);
            userProfile.attr('password', values['password']);
            userProfile.attr('id', 'id');
            userProfile.destroy(function() {
              bootbox.alert({
                title: 'Account Deleted',
                message: 'Your account is now deleted.',
              });
              window.location.href = 'logout';
              return true;
            }, function(message) {
              const response = JSON.parse(message.responseText);
              bootbox.alert({
                title: 'Account NOT Deleted',
                message: 'Error: ' + response.message
              });
              return false;
            });
          }
        }
      }
    });
  }
});
