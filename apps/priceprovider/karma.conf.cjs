module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage')
    ],
    client: {
      clearContext: false,
      jasmine: {
        random: false
      }
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: false,
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu', '--headless=new']
      }
    },
    singleRun: true,
    browserDisconnectTolerance: 2,
    browserNoActivityTimeout: 120000,
    browserDisconnectTimeout: 10000,
    captureTimeout: 120000,
    retryLimit: 0
  });
};
