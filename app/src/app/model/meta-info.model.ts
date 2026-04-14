export interface MetaInfo {
  identityFields?: string[];
  mandatoryFields?: string[];
  referenceKeyFields?: string[];
  enumValues?: { [key: string]: string[] };
}
