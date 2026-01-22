import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import $ from 'jquery';
import md5 from 'md5';
import bootbox from 'bootbox';

import ErrorPage from 'helpers/error-page';
import User from 'models/user';
import Group from 'models/group';

import template from './list.stache';
import showErrorDialog from 'helpers/error-dialog';

export default Control.extend({

  'init': function(element, options) {
    let params = {};
    if (options.query) {
      params = {
        query: options.query
      }
    } else {
      params = {
        page: options.page
      }
    }

    User.findAll(
      params,
      function(users) {
        $(element).html(template({
          users: users,
          md5: md5,
          query: options.query
        }));
        $(element).fadeIn();
      },
      function(response) {
        new ErrorPage(element, response);
      });
  },

  '.delete-user-btn click': function(el) {
    const tr = $(el).closest('tr');
    const user = domData.get.call(tr[0], 'user');

    bootbox.confirm({
      title: 'Delete User',
      message: 'Are you sure you want to delete <b>' + user.attr('username') + '</b>?',
      callback: function(result) {
        if (result) {
          user.destroy(
            function() {},
            function(response) {
              showErrorDialog('User not deleted', response);
            },
          );
        }
      },
    });
  },

  '.edit-role-btn click': function(el) {
    const tr = $(el).closest('tr');
    const user = domData.get.call(tr[0], 'user');
    const element = this.element;

    Group.findAll({},
      function(groups) {

        const roles = user.attr('role').split(',');

        let options = '';
        groups.forEach(function(group) {
          if ($.inArray(group.attr('name'), roles) >= 0) {
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" checked />';
            //options = options + '<option selected>' + group.attr('name') + '</option>';
          } else {
            //options = options + '<option>' + group.attr('name') + '</option>';
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" />';
          }
          options = options + ' <b>' + group.attr('name') + '</b><br><small class="text-muted">Access to: ' + group.attr('apps').join(', ') + '</small></label><br>';
        });

        bootbox.confirm({
          title: 'Edit User Roles',
          message:
            '<p>Editing user roles for user: <b>' + user.attr('username') + '</b></p>' +
            '<form id="role-form">' + options + '</form>',
          callback: function(result) {
            if (result) {

              const boxes = $('#role-form input:checkbox');
              const checked = [];
              for (let i = 0; boxes[i]; ++i) {
                if (boxes[i].checked) {
                  checked.push(boxes[i].value);
                }
              }

              const text = checked.join(',');
              user.attr('role', text);
              user.save(
                function() {},
                function(response) {
                  showErrorDialog('User not deleted', response);
                });
            }
          }
        });
      },
      function(response) {
        new ErrorPage(element, response);
      });
  },

  'submit': function() {
    event.preventDefault();

    const query = $(this.element).find('#query');
    if (query.val() !== '') {
      window.location.href = '#!pages/users/search/' + query.val();
    } else {
      window.location.href = '#!pages/users';
    }
  },
});
