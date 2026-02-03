import Control from 'can-control';
import $ from 'jquery';
import Raphael from 'raphael/raphael';
import Morris from 'morris.js/morris.js';

import Counter from 'models/counter';
import JobValue from 'models/job-value';
import template from './dashboard.stache';

export default Control.extend({

  'init': function (element) {
    $(element).hide();
    const that = this;

    window.Raphael = Raphael;

    JobValue.findAll({}, function (values) {
      Counter.findOne({}, function (counter) {
        $(element).html(template({
          counter: counter,
          values: values,
        }));
        $(element).fadeIn();

        $.getJSON('api/v2/admin/server/statistics', {
          days: 1,
        }, function (mydata) {
          $('#new_users').html(mydata[0].users - mydata[mydata.length - 1].users);
          $('#total_users').html(mydata[0].users);

          $('#new_jobs').html(mydata[0].completeJobs - mydata[mydata.length - 1].completeJobs);
          $('#total_jobs').html(mydata[0].completeJobs);

          that.options.running = Morris.Area({
            element: 'morris-area-chart',
            data: mydata,
            xkey: 'timestamp',
            ykeys: ['runningJobs', 'waitingJobs'],
            labels: ['Running Jobs', 'Waiting Jobs'],
            pointSize: 0,
            hideHover: 'always',
            smooth: 'false',
            resize: true,
          });
        });
      }, function () {
        // Counter.findOne() error handler (noop)
      });
    }, function () {
      // JobValue.findAll() error handler (noop)
    });
  },

  '#day_combo change': function () {
    const days = $('#day_combo').val();
    const that = this;
    $.getJSON('api/v2/admin/server/statistics', {
      days: days,
    }, function (mydata) {
      $('#new_users').html(mydata[0].users - mydata[mydata.length - 1].users);
      $('#total_users').html(mydata[0].users);

      $('#new_jobs').html(mydata[0].completeJobs - mydata[mydata.length - 1].completeJobs);
      $('#total_jobs').html(mydata[0].completeJobs);

      that.options.running.setData(mydata);
    });
  },
});
