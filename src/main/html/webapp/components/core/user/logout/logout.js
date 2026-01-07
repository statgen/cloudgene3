import Control from 'can-control';

export default Control.extend({
  "init": function(element, options) {
    localStorage.removeItem("cloudgene");
    window.location = './';
  }
});
