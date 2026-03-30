export const environment = {
  apiBaseUrl: 'http://localhost:8080/',
  oidc: {
    issuerUri: 'http://localhost:8081/realms/priceprovider',
    clientId: 'priceprovider-app',
    scope: 'openid profile email',
    requireHttps: false
  }
};
