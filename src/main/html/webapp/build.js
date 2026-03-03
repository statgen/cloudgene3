const stealTools = require('steal-tools');

stealTools.build(
  {
    main: ['cloudgene/index', 'cloudgene/admin'],
  },
  {
    bundleSteal: true,
    bundleAssets: {
      infer: true,
      glob: ['public/*'],
    },
  },
);
