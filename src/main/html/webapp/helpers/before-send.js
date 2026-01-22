import $ from 'jquery';

export function getLocalCloudgeneData() {
  let csrf = undefined;
  let token = undefined;

  if (localStorage.getItem('cloudgene')) {
    try {
      // get data
      const data = JSON.parse(localStorage.getItem('cloudgene'));
      csrf = data.csrf;
      token = data.token;
    } catch (error) {
      console.log('Failed to parse local Cloudgene data', error);
    }

    return { csrf, token };
  }
}

export function addBeforeSendHook() {
  $.ajaxPrefilter(function (options, originalOptions) {
    if (!options.beforeSend) {
      options.beforeSend = function (xhr) {
        const { csrf, token } = getLocalCloudgeneData();
        xhr.setRequestHeader('X-CSRF-Token', csrf);
        xhr.setRequestHeader('X-Auth-Token', token);
      }
    }

    //canjs has an bug while sending data in json format: data is not in json format, so we need to fix it convert it manually to JSON
    if (options.processData &&
      /^application\/json((\+|;).+)?$/i.test(options.contentType) &&
      /^(post|put|delete)$/i.test(options.type)
    ) {
      options.data = JSON.stringify(originalOptions.data);
      options.processData = false;
    }
  });
}
