import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import $ from 'jquery';
import bootbox from 'bootbox';

import ErrorPage from 'helpers/error-page';
import Template from 'models/template';

import template from './templates.stache';
import showErrorDialog from 'helpers/error-dialog';


export default Control.extend({

  "init": function(element, options) {
    Template.findAll({},
      function(templates) {
        $(element).html(template({
          templates: templates,
        }));
        $("#content").fadeIn();
      },
      function(response) {
        new ErrorPage(element, response);
      });
  },

  '.edit-btn click': function(el, ev) {

    const tr = $(el).closest('tr');
    const template = domData.get.call(tr[0], 'template');
    const oldText = template.attr('text');
    bootbox.confirm({
      title: template.attr('key'),
      message: '<form><textarea class="form-control span5" id="message" rows="10" name="message" width="30" height="20">' + oldText + '</textarea></form>',
      callback: function(result) {
        if (result) {
          const text = $('#message').val();
          template.attr('text', text);
          template.save(
            function(data) {},
            function(response) {
              showErrorDialog("Operation failed", response);
            },
          );
        }
      }
    });
  }

});
