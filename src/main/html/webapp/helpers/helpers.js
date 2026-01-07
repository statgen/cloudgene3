import dateFormat from 'dateformat';
import stache from 'can-stache';
import AU from 'ansi_up';


stache.registerHelper('truncate', function(str, len) {
  if (str.length > len) {
    let truncated = str.substr(0, len + 1);

    while (truncated.length) {
      const ch = truncated.substr(-1);
      truncated = truncated.substr(0, -1);

      if (ch === ' ') {
        break;
      }
    }

    if (truncated == '') {
      truncated = str.substr(0, len);
    }

    return truncated + '...';
  }
  return str;
});

function renderTreeItem(jobId, items, level) {
  let html = '<ul class="folder ' + (level > 0 ? 'sub-folder' : 'root-folder') + '">';
  for (let i = 0; i < items.length; i++) {
    html += '<li>';
    if (items[i].folder == true) {
      html += '<i class="fas fa-angle-right folder-item text-muted fa-fw"></i>&nbsp;';
      html += '<span class="folder-item-text fa-fw"><i class="fas fa-folder text-muted"></i>&nbsp' + items[i].name + '</span>';
      html += renderTreeItem(jobId, items[i].childs, level + 1);
    } else {
      html += '<i class="far fa-file-alt text-muted fa-fw file-item-icon""></i>&nbsp;';
      html += '<a class="file-item" href="' + items[i].path + '" target="_blank">' + items[i].name + '</a>';
      html += '&nbsp;&nbsp;&nbsp;&nbsp;<span class="text-muted">(' + items[i].size + ")</span>";
    }
    html += "</li>";
  }
  html += "</ul>";
  return html;
}

stache.registerHelper('renderTree', function(jobId, item) {
  return renderTreeItem(jobId, item, 0);
});

stache.registerHelper('replaceNL', function(value, total) {
  return value.replaceAll('\n', '<br>');
});


stache.registerHelper('percentage', function(value, total) {
  return (value / total) * 100;
});

stache.registerHelper('floor', function(value) {
  return Math.floor(value);
});

stache.registerHelper('prettyTime', function(executionTime) {
  if (!executionTime || executionTime <= 0) {

    return '-';

  } else {

    const h = (Math.floor((executionTime / 1000) / 60 / 60));
    const m = ((Math.floor((executionTime / 1000) / 60)) % 60);

    return (h > 0 ? h + ' h ' : '') + (m > 0 ? m + ' min ' : '') +
      ((Math.floor(executionTime / 1000)) % 60) + ' sec';

  }
});

stache.registerHelper('prettyDate', function(unixTimestamp) {
  if (unixTimestamp > 0) {
    const dt = new Date(unixTimestamp);
    return dateFormat(dt, "default");
  } else {
    return '-';
  }
});

stache.registerHelper('ansiToHtml', function(txt) {
  const ansiUp = new AU();
  ansiUp.use_classes = true;
  return ansiUp.ansi_to_html(txt);
});

stache.registerHelper('isImage', function(str, options) {
  const image = str.endsWith('png') || str.endsWith('jpg') || str.endsWith('gif');
  if (image) {
    return options.fn();
  } else {
    return options.inverse();
  }
});

stache.registerHelper('isS3', function(str, options) {
  const s3 = str.startsWith('s3://');
  if (s3) {
    return options.fn();
  } else {
    return options.inverse();
  }
});

stache.registerHelper('isParamChecked', function(param, options) {
  const value = param.attr('value');
  let result = options.inverse();
  param.attr('values').each(function(item) {
    if (item.attr('key') === 'true') {
      if (item.attr('value') === value) {
        result = options.fn();
        return;
      } else {
        result = options.inverse();
        return;
      }
    }
  });
  return result;
});

stache.registerHelper('getParamTrueValue', function(param, options) {
  let result = '??';
  param.attr('values').each(function(item) {
    if (item.attr('key') === 'true') {
      result = item.attr('value');
      return;
    }
  });
  return result;
});

stache.registerHelper('getParamFalseValue', function(param, options) {
  let result = '??';
  param.attr('values').each(function(item) {
    if (item.attr('key') === 'false') {
      result = item.attr('value');
      return;
    }
  });
  return result;
});


stache.registerHelper('div', function(a, b, options) {
  if (a) {
    return Math.round(a / b * 10) / 10;
  } else {
    return 0;
  }
});
