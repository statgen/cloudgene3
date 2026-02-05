import 'can-map-define';
import Model from 'can-connect/can/model/model';

export default Model.extend(
  // Static properties
  {
    findAll: 'GET api/v2/banner',
    findOne: 'GET api/v2/banner/{id}',
    create: 'POST api/v2/admin/banner',
    update: 'PUT api/v2/admin/banner/{id}',
    destroy: 'DELETE api/v2/admin/banner/{id}',

    /**
     * Swap the positions of `Banner` objects `first` and `second`.
     *
     * @param {Banner} first First banner to swap positions.
     * @param {Banner} second Second banner to swap positions.
     * @param {(data: any) => void} success jQuery `ajax()` callback on success.
     * @param {(response: any) => void} error jQuery `ajax()` callback on failure.
     */
    swap: (first, second, success, error) => {
      $.ajax({
        url: 'api/v2/admin/banner/swap',
        type: 'POST',
        data: { id1: first.id, id2: second.id },
        contentType: 'application/json',
        success,
        error,
      });
    },
  },

  // Instance properties
  {
    define: {
      alertClass: {
        get: function () {
          switch (this.attr('type')) {
            case 'warning':
              return 'alert alert-warning';
            case 'danger':
            default:
              return 'alert alert-danger';
          }
        },
      },

      rowClass: {
        get: function () {
          switch (this.attr('type')) {
            case 'warning':
              return 'table-warning';
            case 'danger':
            default:
              return 'table-danger';
          }
        },
      },
    },
  },
);
