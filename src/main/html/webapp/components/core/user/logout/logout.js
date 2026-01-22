import Control from 'can-control';

export default Control.extend({
  'init': function() {
    localStorage.removeItem('cloudgene');
    window.location = './';
  }
});
