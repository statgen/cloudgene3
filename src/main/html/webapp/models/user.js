import Model from 'can-connect/can/model/model';

export default Model.extend({
  findOne: 'GET api/v2/users/{user}/profile',
  destroy: 'POST api/v2/admin/users/{username}/delete',
  update: 'POST api/v2/admin/users/changegroup',
  findAll: 'GET api/v2/admin/users',
}, {

  define: {

    'readOnly': {
      get: function() {
        return this.attr('username') === 'admin' || this.attr('username') === 'public';
      }
    }
  },

  'checkPassword': function(password, confirm_password) {
    if (!password) {
      return 'Please provide a password.';
    }

    if (!confirm_password) {
      return 'Please confirm your password.';
    }

    if (password !== confirm_password) {
      return 'Please make sure the passwords match.';
    }

    if (password.length < 14) {
      return 'Password must contain at least 14 characters.';
    }

    let re = /[0-9]/;
    if (!re.test(password)) {
      return 'Password must contain at least one number: 0-9';
    }

    re = /[a-z]/;
    if (!re.test(password)) {
      return 'Password must contain at least one lowercase letter: a-z';
    }

    re = /[A-Z]/;
    if (!re.test(password)) {
      return 'Password must contain at least one uppercase letter: A-Z';
    }

    re = /[!"#$%&'()*+,-./:;<=>?@[\]\\^_`{|}~]/;
    if (!re.test(password)) {
      return 'Password must contain at least one special character: !"#$%&\'()*+,-./:;<=>?@[]\\^_`{|}~';
    }
  },

  'checkUsername': function(username) {
    if (!username) {
      return 'The username is required.';
    }

    if (username.length < 4 || username.length > 16) {
      return 'The username must contain between 4 and 16 characters.';
    }

    const pattern = new RegExp(/^[a-z][a-z0-9_]+[a-z0-9]$/);
    if (!pattern.test(username)) {
      return 'Username can only contain lowercase letters a-z, digits 0-9, and underscores _. It must start with a lowercase letter, and cannot end in an underscore.';
    }
  },

  'checkName': function(name) {
    if (!name) {
      return 'The full name is required.';
    }
  },

  'checkMail': function(mail) {
    if (!mail) {
      return 'E-Mail is required.';
    }

    const pattern = new RegExp(
      // eslint-disable-next-line
      /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i);
    if (!pattern.test(mail)) {
      return 'Please enter a valid mail address.';
    }
  }
});
