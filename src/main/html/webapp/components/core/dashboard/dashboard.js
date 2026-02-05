import $ from 'jquery';
import Control from 'can-control';

import Counter from 'models/counter';
import BannerDisplay from './banner-display/';
import template from './dashboard.stache';

export default Control.extend({
  init: (element, options) => {
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
});
