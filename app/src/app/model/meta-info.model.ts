export interface FieldMetadata {
  name: string;
  type: 'Number' | 'Enum' | 'LocalizedString' | 'Reference' | 'Set<Reference>' | 'String' | 'DateTime' | 'Boolean';
  readOnly?: boolean;
  precision?: number;
}

export interface MetaInfo {
  identityFields?: string[];
  mandatoryFields?: string[];
  referenceKeyFields?: string[];
  enumValues?: { [key: string]: string[] };
  fields?: FieldMetadata[];
}
