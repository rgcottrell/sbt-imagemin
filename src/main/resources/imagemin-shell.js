/*global process, require */

var mkdirp = require("mkdirp"),
    path = require("path"),
    Imagemin = require("imagemin");

var SOURCE_FILE_MAPPINGS_ARG = 2;
var TARGET_ARG = 3;
var OPTIONS_ARG = 4;

var args = process.argv;
var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
var target = args[TARGET_ARG];
var options = JSON.parse(args[OPTIONS_ARG]) || {};

var interlaced = options.interlaced || true;
var optimizationLevel = options.optimizationLevel || 3;
var progressive = options.progressive || true;

mkdirp(target);

var results = [];
var problems = [];
sourceFileMappings.forEach(function(sourceFileMapping) {
  var input = sourceFileMapping[0];
  var output = path.join(target, sourceFileMapping[1]);

  try {
    var imagemin = new Imagemin()
      .src(input)
      .dest(output)
      .use(Imagemin.jpegtran({ progressive: progressive }))
      .use(Imagemin.gifsicle({ interlaced: interlaced }))
      .use(Imagemin.optipng({ optimizationLevel: optimizationLevel }))
      .use(Imagemin.svgo());

    imagemin.optimize(function(err, data) {
      if (err) {
        throw err;
      }
      results.push({
        source: input,
        result: {
          filesRead: [input],
          filesWritten: [output]
        }
      });
    });
  } catch (e) {
    problems.push({
        message: err,
        severity: "error",
        source: input
    });
    console.error(e);
  }
});

console.log("\u0010" + JSON.stringify({results: results, problems: problems}));