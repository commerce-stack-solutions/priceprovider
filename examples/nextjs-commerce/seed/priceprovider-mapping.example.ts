/**
 * Example helper for a Next.js Commerce data provider.
 *
 * It maps commerce product + quantity input to the Price Provider public API path
 * exposed through Tyk:
 *   /commerce/prices/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}
 */

export async function fetchPrice(args: {
  channelId: string;
  countryIsoKey: string;
  priceType: string;
  pricedResourceId: string;
  quantity: number;
  unit: string;
  currency: string;
  accessToken?: string;
}) {
  const baseUrl = process.env.PRICES_API_BASE_URL ?? process.env.NEXT_PUBLIC_PRICES_API_BASE_URL;
  if (!baseUrl) {
    throw new Error('PRICES_API_BASE_URL (or NEXT_PUBLIC_PRICES_API_BASE_URL) is not set');
  }

  const { channelId, countryIsoKey, priceType, pricedResourceId, quantity, unit, currency, accessToken } = args;
  const url = new URL(
    `${baseUrl}/${channelId}/${countryIsoKey}/pricerows/${priceType}/of/${pricedResourceId}`
  );
  url.searchParams.set('quantity', String(quantity));
  url.searchParams.set('unit', unit);
  url.searchParams.set('currency', currency);

  const response = await fetch(url, {
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
    cache: 'no-store'
  });

  if (!response.ok) {
    throw new Error(`Price Provider request failed: ${response.status}`);
  }

  return response.json();
}
