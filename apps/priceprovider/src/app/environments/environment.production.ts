export const environment = {
  apiBaseUrl: 'https://api.example.com/',
  oidc: {
    issuerUri: 'https://auth.example.com/realms/priceprovider',
    clientId: 'priceprovider-app',
    scope: 'openid profile email',
    requireHttps: true
  }
};
