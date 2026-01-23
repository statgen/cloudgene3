import Control from 'can-control';

import 'helpers/helpers';
import $ from 'jquery';
import bootbox from 'bootbox';
import showErrorDialog from 'helpers/error-dialog';

import ApplicationSettings from 'models/application-settings';
import template from './settings.stache';

export default Control.extend({

  init: function (element, options) {
    const that = this;

    ApplicationSettings.findOne({ id: options.app }, function (application) {
      that.application = application;
      $(element).html(template({
        application: application,

      }));
      $(element).fadeIn();
    });
  },

  submit: function (form, event) {
    event.preventDefault();

    const nextflowProfile = $('#nextflow-profile').val();
    const nextflowConfig = $('#nextflow-config').val();
    const nextflowWork = $('#nextflow-work').val();
    const nextflowEnv = $('#nextflow-env').val();

    this.application.attr('config').attr('nextflow.profile', nextflowProfile);
    this.application.attr('config').attr('nextflow.config', nextflowConfig);
    this.application.attr('config').attr('nextflow.work', nextflowWork);
    this.application.attr('config').attr('nextflow.env', nextflowEnv);

    this.application.save(
      function () {
        bootbox.alert('Application settings updated.');
      },
      function (response) {
        showErrorDialog('Operation failed', response);
      },
    );
  },
});
