import 'can-map-define';
import Model from 'can-connect/can/model/model';

const STATE_DEAD                         = -1;
const STATE_WAITING                      =  1;
const STATE_RUNNING                      =  2;
const STATE_EXPORTING                    =  3;
const STATE_SUCCESS                      =  4;
const STATE_FAILED                       =  5;
const STATE_CANCELED                     =  6;
const STATE_RETIRED                      =  7;
const STATE_SUCESS_AND_NOTIFICATION_SEND =  8;
const STATE_FAILED_AND_NOTIFICATION_SEND =  9;

export default Model.extend({
  findAll: 'GET api/v2/jobs',
  findOne: 'GET api/v2/jobs/{id}/status',
  destroy: 'DELETE api/v2/jobs/{id}',
}, {

  'syncTime': function () {
    if (this.attr('startTime') > 0 && this.attr('endTime') === 0) {
      this.attr('endTime', this.attr('currentTime'));
    } else {
      // TODO(Marc): This else branch is likely doing nothing. Delete and check!
      this.attr('endTime', this.attr('endTime'));
    }
  },

  define: {
    'longName': {
      get: function () {
        return this.attr('name');
      }
    },

    'executionTime': {
      get: function () {
        const start = this.attr('startTime');
        const end = this.attr('endTime');
        const current = this.attr('currentTime');

        if (start === 0 && end === 0) {
          return undefined;
        }

        let executionTime;
        if (start > 0 && end === 0) {
          executionTime = current - start;
        } else {
          executionTime = end - start;
        }

        if (executionTime <= 0) {
          return undefined;
        } else {
          return executionTime;
        }
      }
    },

    'stateAsText': {
      get: function () {
        switch (this.attr('state')) {
          case STATE_DEAD:
            return 'Pending';
          case STATE_WAITING:
            return 'Waiting';
          case STATE_RUNNING:
            return 'Running';
          case STATE_EXPORTING:
            return 'Exporting Data';
          case STATE_SUCCESS:
          case STATE_SUCESS_AND_NOTIFICATION_SEND:
            return 'Complete';
          case STATE_CANCELED:
            return 'Canceled';
          case STATE_RETIRED:
            return 'Retired';
          case STATE_FAILED:
          case STATE_FAILED_AND_NOTIFICATION_SEND:
          default:
            return 'Error';
        }
      }
    },

    'stateAsClass': {
      get: function () {
        switch (this.attr('state')) {
          case STATE_DEAD:
            return 'dark';
          case STATE_WAITING:
          case STATE_RETIRED:
            return 'secondary';
          case STATE_RUNNING:
          case STATE_EXPORTING:
            return 'primary';
          case STATE_SUCCESS:
          case STATE_SUCESS_AND_NOTIFICATION_SEND:
            return 'success';
          case STATE_FAILED:
          case STATE_CANCELED:
          case STATE_FAILED_AND_NOTIFICATION_SEND:
          default:
            return 'danger';
        }
      }
    },

    'stateAsImage': {
      get: function () {
        switch (this.attr('state')) {
          case STATE_DEAD:
            return "fas fa-moon";
          case STATE_WAITING:
            if (this.attr('setupRunning')) {
              return 'fas fa-cog fa-spin';
            } else {
              return 'far fa-pause-circle';
            }
          case STATE_RUNNING:
          case STATE_EXPORTING:
            return "fas fa-circle-notch fa-spin";
          case  STATE_SUCCESS:
          case STATE_SUCESS_AND_NOTIFICATION_SEND:
            return "fas fa-check";
          case STATE_FAILED:
          case STATE_FAILED_AND_NOTIFICATION_SEND:
            return "fas fa-exclamation";
          case STATE_CANCELED:
            return "fas fa-times";
          case STATE_RETIRED:
            return "fas fa-archive";
          default:
            return "fas fa-triangle-exclamation";
        }
      }
    },

    'isInQueue': {
      get: function () {
        return this.attr('state') === STATE_WAITING && this.attr('positionInQueue') !== -1;
      }
    },

    'isPending': {
      get: function () {
        return this.attr('state') === STATE_DEAD;
      }
    },

    'isRetired': {
      get: function () {
        return this.attr('state') === STATE_RETIRED;
      }
    },

    'isRunning': {
      get: function () {
        return (
          this.attr('state') === STATE_RUNNING ||
          this.attr('state') === STATE_EXPORTING
        );
      }
    },

    'willBeRetired': {
      // NOTE(Marc): Semantically closer to "has the user been notified of impending job retirement?"
      get: function () {
        return (
          this.attr('state') === STATE_SUCESS_AND_NOTIFICATION_SEND ||
          this.attr('state') === STATE_FAILED_AND_NOTIFICATION_SEND
        );
      }
    },

    'canResetCounters': {
      get: function () {
        // NOTE(Marc): Semantically closer to "has the job finished? (including errors)"
        return this.attr('state') > STATE_EXPORTING;
      }
    },

    'canSendRetireNotification': {
      get: function () {
        // NOTE(Marc): Equivalent to canResetCounters() && willBeRetired()
        return (
          this.attr('state') > STATE_EXPORTING &&
          this.attr('state') !== STATE_SUCESS_AND_NOTIFICATION_SEND &&
          this.attr('state') !== STATE_FAILED_AND_NOTIFICATION_SEND
        );
      }
    },

    'canIncreaseRetireDate': {
      get: function () {
        // NOTE(Marc): Identical to willBeRetired().
        return (
          this.attr('state') === STATE_SUCESS_AND_NOTIFICATION_SEND ||
          this.attr('state') === STATE_FAILED_AND_NOTIFICATION_SEND
        );
      }
    },

    'canShowLog': {
      get: function () {
        // NOTE(Marc): Double-negation !! casts to boolean based on truthy-ness (length > 0 strings are truthy).
        //             So this is better described as "are there logs?"
        return !!this.attr('logs');
      }
    },

    'canCancel': {
      get: function () {
        // NOTE(Marc): Semantically, "is this waiting or running?"
        return (
          this.attr('state') <= STATE_EXPORTING &&
          this.attr('state') !== STATE_DEAD
        );
      }
    },

    'canRetireJob': {
      get: function () {
        // NOTE(Marc): Original logic was wrong. This is taken from backend JobService.archive(job)
        return (
          this.attr('state') === STATE_SUCCESS ||
          this.attr('state') === STATE_FAILED ||
          this.attr('state') === STATE_CANCELED
        );
      }
    },

    'canDelete': {
      get: function () {
        // NOTE(Marc): Equivalent to !canCancel()
        return (
          this.attr('state') > STATE_EXPORTING ||
          this.attr('state') === STATE_DEAD
        );
      }
    }
  }
});
