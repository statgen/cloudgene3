import $ from 'jquery';
import 'bootstrap';
import 'bootstrap/dist/css/bootstrap.css';
import '@fortawesome/fontawesome-free/css/all.css';

import RouterControl from 'helpers/router';
import ErrorPage from 'helpers/error-page';
import { addBeforeSendHook } from 'helpers/before-send';

import Server from 'models/server';

import LayoutControl from 'components/core/layout/';
import DashboardControl from 'components/core/dashboard/';
import AboutControl from 'components/core/about/';
import ContactControl from 'components/core/contact/';
import UserLoginControl from 'components/core/user/login/';
import UserLogoutControl from 'components/core/user/logout/';
import UserSignupControl from 'components/core/user/signup/';
import UserActivateControl from 'components/core/user/activate/';
import UserPasswordRecoveryControl from 'components/core/user/password-recovery/';
import UserPasswordResetControl from 'components/core/user/password-reset/';
import UserProfileControl from 'components/core/user/profile/';
import JobListControl from 'components/core/job/list/';
import JobDetailControl from 'components/core/job/detail/';
import SubmitJobControl from 'components/core/job/submit/';

// open all external link in new tab
$(document.links).filter(function () {
  return this.hostname !== window.location.hostname;
}).attr('target', '_blank');

const routes = [{
  path: '',
  control: DashboardControl,
  classes: 'fullsize-container',
}, {
  path: 'pages/home',
  control: DashboardControl,
  classes: 'fullsize-container',
}, {
  path: 'pages/about',
  control: AboutControl,
}, {
  path: 'pages/contact',
  control: ContactControl,
}, {
  path: 'pages/login',
  control: UserLoginControl,
}, {
  path: 'pages/logout',
  control: UserLogoutControl,
}, {
  path: 'activate/{user}/{key}',
  control: UserActivateControl,
}, {
  path: 'recovery/{user}/{key}',
  control: UserPasswordRecoveryControl,
}, {
  path: 'pages/register',
  control: UserSignupControl,
}, {
  path: 'pages/reset-password',
  control: UserPasswordResetControl,
}, {
  path: 'pages/profile',
  control: UserProfileControl,
  guard: loggedInGuard,
}, {
  path: 'pages/jobs',
  control: JobListControl,
  options: {
    page: 1,
  },
  classes: 'fullsize-container',
  guard: loggedInGuard,
}, {
  path: 'pages/jobs/{page}',
  control: JobListControl,
  classes: 'fullsize-container',
  guard: loggedInGuard,
}, {
  path: 'jobs/{job}',
  control: JobDetailControl,
  classes: 'fullsize-container',
  guard: loggedInGuard,
}, {
  path: 'jobs/{job}/{tab}',
  control: JobDetailControl,
  classes: 'fullsize-container',
  guard: loggedInGuard,
}, {
  path: 'run/{app}',
  control: SubmitJobControl,
  classes: 'fullsize-container',
  guard: loggedInGuard,
}];

function loggedInGuard(appState) {
  return appState.attr('loggedIn');
}

addBeforeSendHook();

Server.findOne({}, function (server) {
  new LayoutControl('#main', {
    server: server,
  });

  new RouterControl('#content', {
    routes: routes,
    appState: server,
    classes: 'container page my-5 p-5',
    forbidden: {
      control: ErrorPage,
      options: {
        status: '401',
        responseText: 'Oops, you need to <a href="#!pages/login">login</a> to view this content.',
      },
    },
  });
});
