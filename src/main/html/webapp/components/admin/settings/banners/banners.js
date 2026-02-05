import Control from 'can-control';
import domData from 'can-util/dom/data/data';
import $ from 'jquery';
import bootbox from 'bootbox';

import ErrorPage from 'helpers/error-page';
import showErrorDialog from 'helpers/error-dialog';

import Banner from 'models/banner';
import template from './banners.stache';
import popupTemplate from './popup.stache';

const connectTester = () => {
  /** @type HTMLSelectElement */
  const typeElement = document.getElementById('add-type');
  const row = document.getElementById('add-row');

  typeElement.addEventListener('change', () => {
    switch (typeElement.value) {
      case 'warning':
        row.classList.add('table-warning');
        row.classList.remove('table-danger');
        break;
      case 'danger':
        row.classList.add('table-danger');
        row.classList.remove('table-warning');
        break;
      default:
        console.error('Message type not recognized: ', typeElement.value);
    }
  });
};

const disableInvalidButtons = () => {
  $('.up-btn').first().prop('disabled', true);
  $('.down-btn').last().prop('disabled', true);
};

export default Control.extend({

  'init': (element) => {
    Banner.findAll({},
      (banners) => {
        $(element).html(template({
          banners: banners,
        }));

        connectTester();
        disableInvalidButtons();

        $('#content').fadeIn();
      },
      (response) => {
        new ErrorPage(element, response);
      });
  },

  '.up-btn click': (element) => {
    const currentRow = $(element).closest('tr');
    const currentBanner = domData.get.call(currentRow[0], 'banner');

    const previousRow = currentRow.prev('tr');
    const previousBanner = (!previousRow) ? undefined : domData.get.call(previousRow[0], 'banner');

    if (!previousBanner) {
      console.error('No previous banner; can\'t go up.');
      return;
    }

    Banner.swap(
      currentBanner, previousBanner,
      () => { window.location.reload(); }, // success
      (response) => { console.error('Failed to swap: ', response); }, // error
    );
  },

  '.down-btn click': (element) => {
    const currentRow = $(element).closest('tr');
    const currentBanner = domData.get.call(currentRow[0], 'banner');

    const nextRow = currentRow.next('tr');
    const nextBanner = (!nextRow) ? undefined : domData.get.call(nextRow[0], 'banner');

    if (!nextBanner) {
      console.error('No next banner; can\'t go down.');
      return;
    }

    Banner.swap(
      currentBanner, nextBanner,
      () => { window.location.reload(); }, // success
      (response) => { console.error('Failed to swap: ', response); }, // error
    );
  },

  '.edit-btn click': (element) => {
    const row = $(element).closest('tr');
    const banner = domData.get.call(row[0], 'banner');

    const oldType = banner.attr('type');
    const oldMessage = banner.attr('message');

    const contents = popupTemplate({
      type: oldType,
      message: oldMessage,
    });

    console.log('.edit-btn click :: contents = ', contents);

    bootbox.confirm({
      title: 'Modify Message',
      message: contents,
      callback: (result) => {
        if (result) {
          const newType = $('#popup-type').val();
          const newMessage = $('#popup-message').val();

          console.log('.edit-btn click :: ', { newType, newMessage });

          banner.attr('type', newType);
          banner.attr('message', newMessage);

          console.log('.edit-btn click :: banner = ', banner);

          banner.save(
            () => {}, // success
            (response) => { // error
              showErrorDialog('Banner update failed', response);
            },
          );
        }
      },
    });
  },

  '.delete-btn click': (el) => {
    const row = $(el).closest('tr');
    const banner = domData.get.call(row[0], 'banner');

    banner.destroy(
      () => {
        window.location.reload();
      }, // success
      (response) => { // error
        showErrorDialog('Banner not deleted', response);
      },
    );
  },

  '#add-submit click': () => {
    /** @type HTMLSelectElement */
    const typeElement = document.getElementById('add-type');
    const type = typeElement.value;

    // Type is always valid (dropdown defaults to warning).

    /** @type HTMLInputElement */
    const messageElement = document.getElementById('add-message');
    const messageFeedback = document.getElementById('add-message-feedback');
    const message = messageElement.value;

    // Message must be non-blank
    if (!message || !message.trim()) {
      const errorText = 'Message must be non-null.';

      messageElement.setCustomValidity(errorText);
      messageElement.classList.add('is-invalid');
      messageElement.classList.remove('is-valid');

      messageFeedback.textContent = errorText;

      return;
    } else {
      messageElement.setCustomValidity('');
      messageElement.classList.add('is-valid');
      messageElement.classList.remove('is-invalid');

      messageFeedback.textContent = '';
    }

    console.log({ type, message });

    $.ajax({
      url: 'api/v2/admin/banner',
      type: 'POST',
      data: { type, message },
      contentType: 'application/json',
      success: () => {
        window.location.reload();
      },
      error: (response) => {
        showErrorDialog('Banner not added', response);
      },
    });
  },
});
