import $ from 'jquery';
import Control from 'can-control';
import stache from 'can-stache';

import Counter from 'models/counter';

export default Control.extend({
  'init': function(element, options) {
    $.get('static/home.stache',
      function(data) {
        const template = stache(data);

        Counter.findOne({},
          function(counter) {
            $(element).html(template({
              counter: counter,
              loggedIn: options.appState.loggedIn
            }));
          },
          function() {
            $(element).html(template({
              counter: undefined,
              loggedIn: options.loggedIn
            }));
          }
        );
      }
    );
  }
});
