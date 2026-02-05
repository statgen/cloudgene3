import $ from 'jquery';
import Control from 'can-control';
import stache from 'can-stache';

import Counter from 'models/counter';
import BannerDisplay from './banner-display/';

export default Control.extend({
  init: (element, options) => {
    $.get('static/home.stache',
      function (data) {
        const template = stache(data);

        Counter.findOne({},
          (counter) => { // success
            $(element).html(template({
              counter: counter,
              loggedIn: options.appState.loggedIn,
            }));

            new BannerDisplay('#banners', {});
          },
          () => { // error
            $(element).html(template({
              counter: undefined,
              loggedIn: options.loggedIn,
            }));
          },
        );
      },
    );
  },
});
