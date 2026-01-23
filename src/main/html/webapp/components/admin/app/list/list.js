import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import canRoute from 'can-route';
import $ from 'jquery';
import bootbox from 'bootbox';

import 'helpers/helpers';
import showErrorDialog from 'helpers/error-dialog';
import ErrorPage from 'helpers/error-page';

import Application from 'models/application';
import Group from 'models/group';

import template from './list.stache';
import templateInstallGithub from './install-github/install-github.stache';
import templateInstallUrl from './install-url/install-url.stache';

export default Control.extend({

  'init': function (element) {
    Application.findAll({}, function (applications) {
      const grouped = {};
      applications.forEach(function (item) {
        let category = item.category;
        if (category === undefined) {
          category = 'Application';
        }
        if (!grouped[category]) {
          grouped[category] = [];
        }
        grouped[category].push(item);
      });

      const categories = Object.keys(grouped).map((category) => {
        return {
          name: category,
          applications: grouped[category].map((app) => {
            return app;
          }),
        };
      });

      categories.sort(function (a, b) {
        // Place "Application" at the beginning
        if (a.name.toLowerCase() === 'application') return -1;
        if (b.name.toLowerCase() === 'application') return 1;

        // Compare other categories alphabetically
        return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
      });

      $(element).html(template({
        categories: categories,
      }));
      $(element).fadeIn();
    });
  },

  '#install-app-url-btn click': function () {
    bootbox.confirm({
      title: 'Install App from URL',
      message: templateInstallUrl(),
      callback: function (result) {
        if (result) {
          const url = $('#url').val();
          const app = new Application();
          app.attr('url', url);

          const waitingDialog = bootbox.dialog({
            title: 'Install application',
            message:
              '<p>Please wait while the application is configured.</p>'
              + '<div class="progress progress-striped active">'
              + '    <div id="waiting-progress" class="bar" style="width: 100%;"></div>'
              + '</div>',
            show: false,
          });

          waitingDialog.on('shown.bs.modal', function () {
            app.save(
              function () {
                waitingDialog.modal('hide');
                bootbox.alert({
                  title: 'Congratulations',
                  message: '<p>The application installation was successful.</p>',
                  callback: function () {
                    const router = canRoute.router;
                    router.reload();
                  },
                });
              },
              function (response) {
                waitingDialog.modal('hide');
                showErrorDialog('Operation failed', response);
              },
            );
          });

          waitingDialog.modal('show');
        }
      },
    });
  },

  '#install-app-github-btn click': function () {
    bootbox.confirm({
      title: 'Install App from GitHub repository',
      message: templateInstallGithub(),
      callback: function (result) {
        if (result) {
          const url = 'github://' + $('#url').val();
          const app = new Application();
          app.attr('url', url);

          const waitingDialog = bootbox.dialog({
            title: 'Installing Application',
            message:
              '<p>Please wait while the application is configured.</p>'
              + '<div class="progress progress-striped active">'
              + '<div id="waiting-progress" class="bar" style="width: 100%;"></div>'
              + '</div>',
            show: false,
          });

          waitingDialog.on(
            'shown.bs.modal',
            function () {
              app.save(
                function () {
                  waitingDialog.modal('hide');
                  bootbox.alert({
                    title: 'Congratulations',
                    message: '<p>The application installation was successful.</p>',
                    callback: function () {
                      const router = canRoute.router;
                      router.reload();
                    },
                  });
                },
                function (response) {
                  waitingDialog.modal('hide');
                  showErrorDialog('Operation failed', response);
                },
              );
            },
          );

          waitingDialog.modal('show');
        }
      },
    });
  },

  '#reload-apps-btn click': function () {
    const element = this.element;

    Application.findAll({
      reload: 'true',
    }, function (applications) {
      const grouped = {};
      applications.forEach(function (item) {
        let category = item.category;
        if (category === undefined) {
          category = 'Application';
        }
        if (!grouped[category]) {
          grouped[category] = [];
        }
        grouped[category].push(item);
      });

      const categories = Object.keys(grouped).map((category) => {
        return {
          name: category,
          applications: grouped[category].map((app) => {
            return app;
          }),
        };
      });

      categories.sort(function (a, b) {
        // Place "Application" at the beginning
        if (a.name.toLowerCase() === 'application') return -1;
        if (b.name.toLowerCase() === 'application') return 1;

        // Compare other categories alphabetically
        return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
      });

      $(element).html(template({
        categories: categories,
      }));
      $('#content').fadeIn();
    });
  },

  '.enable-disable-btn click': function (el) {
    const card = $(el).closest('tr');
    const application = domData.get.call(card[0], 'application');

    const enabled = !application.attr('enabled');
    bootbox.confirm({
      title: (enabled ? 'Enable' : 'Disable') + ' Application',
      message: 'Are you sure you want to ' + (enabled ? 'enable' : 'disable') + ' <b>' + application.attr('id') + '</b>?',
      callback: function (result) {
        if (result) {
          application.attr('enabled', enabled);

          const waitingDialog = bootbox.dialog({
            title: (enabled ? 'Enabling...' : 'Disabling...'),
            message:
              '<p>Please wait while the application is configured.</p>'
              + '<div class="progress progress-striped active">'
              + '<div id="waiting-progress" class="bar" style="width: 100%;"></div>'
              + '</div>',
            show: false,
          });
          waitingDialog.on(
            'shown.bs.modal',
            function () {
              application.save(
                function () {
                  waitingDialog.modal('hide');
                  bootbox.alert({
                    title: 'Congratulations',
                    message: '<p>The application has been successfully ' + (enabled ? 'enabled' : 'disabled') + '.</p>',
                  });
                },
                function (response) {
                  waitingDialog.modal('hide');
                  showErrorDialog('Operation failed', response);
                },
              );
            },
          );
          waitingDialog.modal('show');
        }
      },
    });
  },

  '.delete-app-btn click': function (el) {
    const card = $(el).closest('tr');
    const application = domData.get.call(card[0], 'application');

    bootbox.confirm({
      title: 'Delete Application',
      message: 'Are you sure you want to delete <b>' + application.attr('id') + '</b>?',
      callback: function (result) {
        if (result) {
          const waitingDialog = bootbox.dialog({
            title: 'Uninstalling...',
            message:
              '<p>Please wait while the application is configured.</p>'
              + '<div class="progress progress-striped active">'
              + '<div id="waiting-progress" class="bar" style="width: 100%;"></div>'
              + '</div>',
            show: false,
          });

          waitingDialog.on(
            'shown.bs.modal',
            function () {
              application.destroy(
                function () {
                  waitingDialog.modal('hide');
                  bootbox.alert({
                    title: 'Congratulations',
                    message: '<p>The application has been successfully removed.</p>',
                  });
                },
                function (response) {
                  waitingDialog.modal('hide');
                  showErrorDialog('Operation failed', response);
                },
              );
            },
          );

          waitingDialog.modal('show');
        }
      },
    });
  },

  '.edit-permission-btn click': function (el) {
    const card = $(el).closest('tr');
    const application = domData.get.call(card[0], 'application');
    const element = this.element;

    Group.findAll({},
      function (groups) {
        const roles = application.attr('permission').split(',');

        let options = '';
        groups.forEach(function (group) {
          if ($.inArray(group.attr('name'), roles) >= 0) {
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" checked />';
          } else {
            options = options + '<label class="checkbox"><input type="checkbox" name="role-select" value="' + group.attr('name') + '" />';
          }
          options = options + ' <b>' + group.attr('name') + '</b></label><br>';
        });

        // Add input field for creating a new group
        const newGroupSection = `
          <hr>
          <label for="new-group-name">New Group Name:</label>
          <input type="text" id="new-group-name" class="form-control" placeholder="Enter new group name">
        `;

        bootbox.confirm({
          title: 'Edit Permission for ' + application.attr('name'),
          message: '<form id="role-form">' + options + newGroupSection + '</form>',
          callback: function (result) {
            if (result) {
              const boxes = $('#role-form input:checkbox');
              const checked = [];
              for (let i = 0; boxes[i]; ++i) {
                if (boxes[i].checked) {
                  checked.push(boxes[i].value);
                }
              }

              const newGroupName = $('#new-group-name').val().trim();
              if (newGroupName) {
                checked.push(newGroupName);
              }

              const text = checked.join(',');
              application.attr('permission', text);
              application.save(
                function () {},
                function (response) {
                  showErrorDialog('Operation failed', response);
                },
              );
            }
          },
        });
      },
      function (response) {
        new ErrorPage(element, response);
      });
  },

  '.view-source-btn click': function (el) {
    const card = $(el).closest('tr');
    const application = domData.get.call(card[0], 'application');
    bootbox.alert({
      title: 'View Source',
      message: '<div style="overflow: auto; height: 600px; width: 100%"><h5>File</h5><p>' + application.attr('filename') + '</p>' + '<h5>Source</h5><small><p><pre><code>' + application.attr('source') + '</code></pre></small></p></div>',
      className: 'w-100',
    });
  },
});
