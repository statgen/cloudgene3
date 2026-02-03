import Control from 'can-control';
import $ from 'jquery';

import template from './login.stache';

export default Control.extend({

  init: function (element, options) {
    $(element).hide();
    $(element).html(template({
      oauth: options.appState.attr('oauth'),
    }));
    $(element).fadeIn();
  },

  submit: function (element, event) {
    event.preventDefault();

    const password = $(element).find('[name="password"]');

    $.ajax({
      url: 'login',
      type: 'POST',
      data: $(element).find('#signin-form').serialize(),
      dataType: 'json',
      success: function (response) {
        const dataToken = {
          csrf: response.csrf,
          token: response.access_token,
        };
        localStorage.setItem('cloudgene', JSON.stringify(dataToken));

        window.location = './';
      },
      error: function (response) {
        password.addClass('is-invalid');
        $('#invalid-password').html(response.responseJSON.message);
      },
    });
  },
});
