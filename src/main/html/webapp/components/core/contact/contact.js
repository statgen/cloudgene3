import $ from 'jquery';
import Control from 'can-control';

import template from './contact.stache';

export default Control.extend({
  init: function (element) {
    $(element).html(template());
  },
});
