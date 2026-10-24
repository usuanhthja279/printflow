const PUBLIC_REFERENCE_PREFIX = 'PF-';

export function printReference(publicId: string): string {
  return `${PUBLIC_REFERENCE_PREFIX}${publicId.replaceAll('-', '').slice(0, 8).toUpperCase()}`;
}

export function matchesPrintReference(query: string, publicId: string): boolean {
  const value = query.trim();
  const displayReference = /^PF-([a-f\d]{1,8})$/i.exec(value);
  const completePublicId = /^[a-f\d]{8}(?:-[a-f\d]{4}){3}-[a-f\d]{12}$/i.test(value);
  const normalizedQuery = displayReference?.[1]?.toUpperCase()
    || (completePublicId ? value.replaceAll('-', '').toUpperCase() : '');
  return normalizedQuery.length > 0 && publicId.replaceAll('-', '').toUpperCase().includes(normalizedQuery);
}

export function matchesQueueSearch(query: string, publicId: string, fields: string[]): boolean {
  const text = query.trim();
  if (!text) return true;
  return matchesPrintReference(text, publicId)
    || fields.some(field => field.toLocaleLowerCase().includes(text.toLocaleLowerCase()));
}
