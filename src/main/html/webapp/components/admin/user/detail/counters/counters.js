import $ from 'jquery';
import Control from 'can-control';

import UserCounter from 'models/user-counter';
import template from './counters.stache';

export default Control.extend({
  init: function (element, options) {
    UserCounter.findOne(
      { username: options.username },
      (response) => {
        const counterList = [];
        const counterMap = response.attr('counters');
        counterMap.each((value, key) => counterList.push({ key, value: value.toLocaleString() }));

        $(element).html(template({ counters: counterList }));
        $(element).fadeIn();
      },
    );
  },
});
