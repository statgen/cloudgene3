const stealTools = require('steal-tools');
const fs = require('fs');
const path = require('path');

/**
 * Move all direct children of `srcDir` into `dstDir`, and delete `dstDir`.
 *
 * @param {string} srcDir Move files from
 * @param {string} dstDir Move files to
 */
const moveDir = (srcDir, dstDir) => {
  console.log(`Moving and deleting dir: ${srcDir} -> ${dstDir}`);

  srcDir = path.resolve(srcDir);
  dstDir = path.resolve(dstDir);

  const srcFiles = fs.readdirSync(srcDir);

  for (const file of srcFiles) {
    console.log(`Moving ${file}`);

    const srcFile = path.join(srcDir, file);
    const dstFile = path.join(dstDir, file);

    fs.renameSync(srcFile, dstFile);
  }

  fs.rmdirSync(srcDir);
};

// ======== Use StealJS to build the front-end ======== //

fs.rmSync('dist/', { recursive: true, force: true });

stealTools.build(
  {
    main: ['cloudgene/index', 'cloudgene/admin'],
  },
  {
    bundleSteal: true,
    bundleAssets: {
      infer: true,
      glob: ['public/**/*'],
    },
  },
).then(() => { // (on success)
  // ======== Flatten dist ======== //

  moveDir('dist/public', 'dist/');
});
