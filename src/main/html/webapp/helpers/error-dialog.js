import bootbox from 'bootbox';

export default function (title, response) {
  let error;

  if (response.responseJSON) {
    error = {
      statusText: response.status,
      responseText: response.responseJSON.message,
    };
  } else {
    error = {
      statusText: response.status,
      responseText: response.responseText,
    };
  }

  bootbox.alert({
    title: '<span class="text-danger">Error: ' + title + '</span>',
    message: error.responseText,
  });
}
