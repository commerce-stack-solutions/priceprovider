/**
 * Message type enum matching backend MessageType
 */
export enum MessageType {
  INFO = 'INFO',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  DANGER = 'DANGER',
  SUCCESS = 'SUCCESS',
  DEBUG = 'DEBUG'
}

/**
 * Message interface matching backend Message structure.
 * Uses message-key with parameters for i18n translation.
 */
export interface Message {
  type: MessageType;
  'message-key': string; // Translation key for i18n (required)
  parameters?: { [key: string]: string }; // Dynamic parameters for message interpolation
  'status-code'?: number;
  fields?: string[]; // Field names associated with the message
}

/**
 * Error response structure matching backend ErrorResponse
 */
export interface ErrorResponse {
  $messages: Message[];
}
