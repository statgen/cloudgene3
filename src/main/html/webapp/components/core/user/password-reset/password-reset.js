import Control from 'can-control';
import $ from 'jquery';

import template from './password-reset.stache';

export default Control.extend({

  init: function (element) {
    $(element).hide();
    $(element).html(template());
    $(element).fadeIn();
  },

  submit: function (element, event) {
    event.preventDefault();

    $.ajax({
      url: 'api/v2/users/reset',
      type: 'POST',
      data: $(element).find('#reset-form').serialize(),
      dataType: 'json',
      success: function (data) {
        if (data.success) {
          // show okey
          $('#reset-page').hide();
          $('#success-message').html(data.message);
          $('#success-message').toggleClass('d-none', false);
        } else {
          // shows error
          $('#username-input').addClass('is-invalid');
          $('#username-feedback').html(data.message);
        }
      },
      error: function (message) {
        alert('failure: ' + message);
      },
    });
  },
});
