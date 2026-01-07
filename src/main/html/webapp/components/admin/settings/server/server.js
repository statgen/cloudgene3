import Control from 'can-control';
import $ from 'jquery';
import bootbox from 'bootbox';

import ErrorPage from 'helpers/error-page';
import Cluster from 'models/cluster';
import Template from 'models/template';

import template from './server.stache';
import showErrorDialog from 'helpers/error-dialog';


export default Control.extend({

  "init": function(element, options) {
    const that = this;
    $(element).hide();
    Cluster.findOne({},
      function(cluster) {
        that.cluster = cluster;
        $(element).html(template({
          cluster: cluster
        }));
        $(element).fadeIn();
      });

  },

  "#maintenance-enter-btn click": function() {
    const that = this;
    const element = this.element;

    Template.findOne({
      key: 'MAINTENANCE_MESSAGE'
    }, function(template) {

      const oldText = template.attr('text');
      bootbox.confirm({
        title: 'Maintenance Message',
        message: '<form><textarea class="form-control span5" id="message" rows="10" name="message" width="30" height="20">' + oldText + '</textarea></form>',
        callback: function(result) {
          if (result) {
            const text = $('#message').val();
            template.attr('text', text);
            template.save();

            $.get('api/v2/admin/server/maintenance/enter').then(function(data) {
              bootbox.alert(data);
              that.init(element, that.options);
            }, function(response) {
              showErrorDialog("Operation failed", response);
            });
          }
        }
      });

    }, function(response) {
      new ErrorPage(element, response);
    });

  },

  "#maintenance-exit-btn click": function() {
    const that = this;
    $.get('api/v2/admin/server/maintenance/exit').then(function(data) {
      bootbox.alert(data);
      that.init(that.element, that.options);
    }, function(response) {
      showErrorDialog("Operation failed", response);
    });
  },

  "#queue-block-btn click": function() {
    const that = this;
    $.get('api/v2/admin/server/queue/block').then(function(data) {
      bootbox.alert(data);
      that.init(that.element, that.options);
    }, function(response) {
      showErrorDialog("Operation failed", response);
    });
  },

  "#queue-open-btn click": function() {
    const that = this;
    $.get('api/v2/admin/server/queue/open').then(function(data) {
      bootbox.alert(data);
      that.init(that.element, that.options);
    }, function(response) {
      showErrorDialog("Operation failed", response);
    });
  },

  "#retire-btn click": function() {
    const that = this;
    $.get('api/v2/admin/jobs/retire').then(function(data) {
      bootbox.alert(data);
      that.init(that.element, that.options);
    }, function(response) {
      showErrorDialog("Operation failed", response);
    });
  }

});
