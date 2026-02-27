import $ from 'jquery';
import Control from 'can-control';

import UserCounter from 'models/user-counter';
import template from './counters.stache';

export default Control.extend({
  init: function (element, options) {
    UserCounter.findOne(
      { username: options.username },
      ({ counters }) => {
        $(element).html(template({ counters }));
        $(element).fadeIn();
      },
    );
  },
});
