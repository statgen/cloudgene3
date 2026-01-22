import $ from 'jquery';
import Control from 'can-control';

import './results.css';


export default Control.extend({

  'init': function() {
    $('#btn-copy').tooltip();
  },

  '#btn-copy click': function(el) {
    const copyTest = document.queryCommandSupported('copy');
    const elOriginalText = $(el).attr('data-original-title');
    const copyTextArea = $('#curl');

    if (copyTest === true) {
      copyTextArea.select();
      try {
        const successful = document.execCommand('copy');
        const msg = successful ? 'Copied!' : 'Whoops, not copied!';
        $(el).attr('data-original-title', msg).tooltip('show');
      } catch (error) {
        console.log('Unable to copy', error);
      }
      $(el).attr('data-original-title', elOriginalText);
    } else {
      // Fallback if browser doesn't support .execCommand('copy')
      window.prompt('Copy to clipboard: Ctrl+C or Command+C, Enter', copyTextArea.val());
    }
  }
});
