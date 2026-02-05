import $ from 'jquery';
import 'bootstrap';
import 'bootstrap/dist/css/bootstrap.css';
import 'components/core/layout/layout.css';
import '@fortawesome/fontawesome-free/css/all.css';
import 'can-map-define';

import ErrorPage from 'helpers/error-page';
import RouterControl from 'helpers/router';
import { addBeforeSendHook } from 'helpers/before-send';

import Server from 'models/server';

import LayoutControl from 'components/admin/layout/';
import DashboardControl from 'components/admin/dashboard/';
import UserListControl from 'components/admin/user/list/';
import UserDetailControl from 'components/admin/user/detail/';
import JobListControl from 'components/admin/job/list/';
import JobDetailControl from 'components/core/job/detail/';
import AppListControl from 'components/admin/app/list/';
import AppSettingsControl from 'components/admin/app/settings/';
import SettingsGeneralControl from 'components/admin/settings/general/';
import SettingsNextflowControl from 'components/admin/settings/nextflow/';
import SettingsServerControl from 'components/admin/settings/server/';
import SettingsMailControl from 'components/admin/settings/mail/';
import SettingsTemplatesControl from 'components/admin/settings/templates/';
import SettingsBannersControl from 'components/admin/settings/banners/';
import SettingsLogsControl from 'components/admin/settings/logs/';

$(document.links).filter(function () {
  return this.hostname !== window.location.hostname;
}).attr('target', '_blank');

const routes = [{
  path: '',
  control: DashboardControl,
  options: {
    login: false,
  },
  guard: adminGuard,
}, {
  path: 'pages/admin-home',
  control: DashboardControl,
  options: {
    login: false,
  },
  guard: adminGuard,
}, {
  path: 'pages/jobs',
  control: JobListControl,
  guard: adminGuard,
}, {
  path: 'pages/users',
  control: UserListControl,
  options: {
    page: 1,
  },
  guard: adminGuard,
}, {
  path: 'pages/users/pages/{page}',
  control: UserListControl,
  guard: adminGuard,
}, {
  path: 'pages/users/search/{query}',
  control: UserListControl,
  guard: adminGuard,
}, {
  path: 'pages/users/{user}',
  control: UserDetailControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-apps',
  control: AppListControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-apps/{app}',
  control: AppSettingsControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-server',
  control: SettingsServerControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-settings-general',
  control: SettingsGeneralControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-settings-nextflow',
  control: SettingsNextflowControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-settings-mail',
  control: SettingsMailControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-settings-templates',
  control: SettingsTemplatesControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-settings-banners',
  control: SettingsBannersControl,
  guard: adminGuard,
}, {
  path: 'pages/admin-logs',
  control: SettingsLogsControl,
  guard: adminGuard,
}, {
  path: 'jobs/{job}',
  control: JobDetailControl,
  guard: adminGuard,
}, {
  path: 'jobs/{job}/{tab}',
  control: JobDetailControl,
  guard: adminGuard,
}];

function adminGuard(appState) {
  if (appState.attr('loggedIn')) {
    return appState.attr('user').attr('admin');
  } else {
    return false;
  }
}

addBeforeSendHook();

Server.findOne({}, function (server) {
  new LayoutControl('#main', {
    appState: server,
  });

  new RouterControl('#content', {
    routes: routes,
    appState: server,
    forbidden: {
      control: ErrorPage,
      options: {
        status: '403',
        responseText: 'Oops, you are not allowed to view this content.',
      },
    },
  });
});
