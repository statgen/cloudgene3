import Control from 'can-control';

import Banner from 'models/banner';
import template from './banner-display.stache';

export default Control.extend({
  init: (element) => {
    Banner.findAll(
      {}, // No idea
      (banners) => {
        $(element).html(template({ banners }));
        $(element).fadeIn();
      },
    );
  },
});
