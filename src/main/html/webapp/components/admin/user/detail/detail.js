import $ from 'jquery';
import Control from 'can-control';
import bootbox from 'bootbox';

import showErrorDialog from 'helpers/error-dialog';
import ErrorPage from 'helpers/error-page';

import User from 'models/user';
import Group from 'models/group';

import JobTable from 'components/admin/job/list/table/';
import template from './detail.stache';

export default Control.extend({

  'init': function(element, options) {
    this.user = null;

    User.findOne(
      { user: options.user },
      (user) => {
        this.user = user;

        $(element).html(template({
          user: user,
        }));
        $(element).fadeIn();

        new JobTable('#job-list', { user: user.username });
      }
    );
  },

  '#delete-user-btn click': function(el, ev) {
    const user = this.user;

    bootbox.confirm({
      title: 'Delete User',
      message: "Are you sure you want to delete <b>" + user.attr('username') + "</b>?",
      callback: function(result) {
        if (result) {
          user.destroy(
            function(data) {
              window.location.hash = "#!pages/users";
            },
            function(response) {
              showErrorDialog("User not deleted", response);
          });
        }
      },
    });
  },

  '.edit-role-btn click': function(el, ev) {
    const user = this.user;
    const element = this.element;

    Group.findAll({},
      function(groups) {

        const roles = user.attr('role').split(',');

        let options = '';
        groups.forEach(function(group, index) {
          if ($.inArray(group.attr('name'), roles) >= 0) {
            options += '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" checked />';
          } else {
            options += '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" />';
          }
          options += ' <b>' + group.attr('name') + '</b><br><small class="text-muted">Access to: ' + group.attr('apps').join(', ') + '</small></label><br>';
        });

        bootbox.confirm({
          title: 'Edit User Roles',
          message: '<form id="role-form">' + options + '</form>',
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
                function(data) {},
                function(response) {
                  showErrorDialog("User not deleted", response);
                }
              );
            }
          }
        });
      },
      function(response) {
        new ErrorPage(element, response);
      });
  },

});
