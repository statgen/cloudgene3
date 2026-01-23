import Model from 'can-connect/can/model/model';

// TODO(Marc): Most likely this can be deleted (seems to be a Hadoop leftover that doesn't exist in the backend).
export default Model.extend({
  findOne: 'GET api/v2/admin/server/cluster',
}, {});
